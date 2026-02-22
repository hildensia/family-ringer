const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ─────────────────────────────────────────────────────────────────────────────
// Helper: verify the caller is authenticated
// ─────────────────────────────────────────────────────────────────────────────
function requireAuth(context) {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "You must be signed in to use this function."
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: verify the caller is a member of the group, returns the member doc
// ─────────────────────────────────────────────────────────────────────────────
async function requireGroupMember(groupId, uid) {
  const memberRef = db
    .collection("groups")
    .doc(groupId)
    .collection("members")
    .doc(uid);
  const memberSnap = await memberRef.get();
  if (!memberSnap.exists) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "You are not a member of this group."
    );
  }
  return memberSnap.data();
}

// ─────────────────────────────────────────────────────────────────────────────
// createGroup
//
// Called by the first device (parent) to create a new family group.
// Returns the groupId which is encoded into the QR code.
//
// Input:  { groupName: string }
// Output: { groupId: string }
// ─────────────────────────────────────────────────────────────────────────────
exports.createGroup = functions.runWith({ maxInstances: 2 }).https.onCall(async (data, context) => {
  requireAuth(context);

  // Cap total number of groups
  const groupCount = await db.collection('groups').count().get();
  if (groupCount.data().count >= 2) {
      throw new functions.https.HttpsError(
          'resource-exhausted',
          'Maximum number of groups reached.'
      );
  }

  const groupName = (data.groupName || "My Family").trim();
  if (groupName.length > 50) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Group name must be 50 characters or less."
    );
  }

  // Create the group document
  const groupRef = await db.collection("groups").add({
    name: groupName,
    createdBy: context.auth.uid,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  // Add the creator as a parent member
  const fcmToken = data.fcmToken || null;
  await groupRef.collection("members").doc(context.auth.uid).set({
    name: data.creatorName || "Parent",
    role: "parent",
    fcmToken: fcmToken,
    joinedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  functions.logger.info(`Group created: ${groupRef.id} by ${context.auth.uid}`);

  return { groupId: groupRef.id };
});

// ─────────────────────────────────────────────────────────────────────────────
// joinGroup
//
// Called by a device that scanned a QR code. Adds them to the group.
// Safe to call multiple times (idempotent) — updates token if already member.
//
// Input:  { groupId: string, name: string, role: "parent"|"child", fcmToken: string }
// Output: { groupId: string, groupName: string }
// ─────────────────────────────────────────────────────────────────────────────
exports.joinGroup = functions.runWith({ maxInstances: 2 }).https.onCall(async (data, context) => {
  requireAuth(context);

  const { groupId, name, role, fcmToken } = data;

  if (!groupId || !name || !role) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "groupId, name, and role are required."
    );
  }

  if (role !== "parent" && role !== "child") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Role must be 'parent' or 'child'."
    );
  }

  // Check the group exists
  const groupSnap = await db.collection("groups").doc(groupId).get();
  if (!groupSnap.exists) {
    throw new functions.https.HttpsError(
      "not-found",
      "Group not found. Check the QR code and try again."
    );
  }

  // Add/update member
  await db
    .collection("groups")
    .doc(groupId)
    .collection("members")
    .doc(context.auth.uid)
    .set(
      {
        name: name.trim(),
        role: role,
        fcmToken: fcmToken || null,
        joinedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }  // merge so re-joining just updates the token
    );

  functions.logger.info(`${context.auth.uid} joined group ${groupId} as ${role}`);

  return {
    groupId: groupId,
    groupName: groupSnap.data().name,
  };
});

// ─────────────────────────────────────────────────────────────────────────────
// registerToken
//
// Called whenever FCM refreshes a device token. Keeps tokens up to date
// so alerts always reach the device.
//
// Input:  { groupId: string, fcmToken: string }
// Output: { success: true }
// ─────────────────────────────────────────────────────────────────────────────
exports.registerToken = functions.runWith({ maxInstances: 2 }).https.onCall(async (data, context) => {
  requireAuth(context);

  const { groupId, fcmToken } = data;
  if (!groupId || !fcmToken) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "groupId and fcmToken are required."
    );
  }

  // Verify they're actually in this group before updating
  await requireGroupMember(groupId, context.auth.uid);

  await db
    .collection("groups")
    .doc(groupId)
    .collection("members")
    .doc(context.auth.uid)
    .update({ fcmToken: fcmToken });

  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// sendAlert
//
// Called by a parent device to send an alert to selected children.
// Validates the caller is a parent in the group before sending.
//
// Input:  { groupId: string, targetMemberIds: string[], message: string }
// Output: { sent: number, failed: number }
// ─────────────────────────────────────────────────────────────────────────────
exports.sendAlert = functions.runWith({ maxInstances: 2 }).https.onCall(async (data, context) => {
  requireAuth(context);

  const { groupId, targetMemberIds, message } = data;

  if (!groupId || !targetMemberIds || !message) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "groupId, targetMemberIds, and message are required."
    );
  }

  if (!Array.isArray(targetMemberIds) || targetMemberIds.length === 0) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "targetMemberIds must be a non-empty array."
    );
  }

  if (message.length > 200) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Message must be 200 characters or less."
    );
  }

  // Verify the caller is a PARENT in this group
  const callerData = await requireGroupMember(groupId, context.auth.uid);
  if (callerData.role !== "parent") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only parents can send alerts."
    );
  }

  // Fetch FCM tokens for all targeted members
  const membersRef = db
    .collection("groups")
    .doc(groupId)
    .collection("members");

  const tokenFetches = targetMemberIds.map((uid) =>
    membersRef.doc(uid).get()
  );
  const memberSnaps = await Promise.all(tokenFetches);

  const tokens = memberSnaps
    .filter((snap) => snap.exists && snap.data().fcmToken)
    .map((snap) => snap.data().fcmToken);

  if (tokens.length === 0) {
    throw new functions.https.HttpsError(
      "not-found",
      "No valid FCM tokens found for the selected members."
    );
  }

  // Send FCM messages — high priority data messages bypass silent mode
  const sendResults = await Promise.allSettled(
    tokens.map((token) =>
      messaging.send({
        token: token,
        data: {
          alert_message: message,
        },
        android: {
          priority: "high",
          ttl: 60000,  // 60 seconds
        },
      })
    )
  );

  const sent = sendResults.filter((r) => r.status === "fulfilled").length;
  const failed = sendResults.filter((r) => r.status === "rejected").length;

  functions.logger.info(
    `Alert sent for group ${groupId}: ${sent} succeeded, ${failed} failed`
  );

  return { sent, failed };
});
