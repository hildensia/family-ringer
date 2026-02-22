package com.familyringer;

import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around Firebase Cloud Functions calls.
 * All methods return Tasks so callers can chain .addOnSuccessListener etc.
 */
public class CloudFunctions {

    private static final String TAG = "CloudFunctions";
    private static final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    public static com.google.android.gms.tasks.Task<String> createGroup(
            String groupName, String creatorName, String fcmToken) {
        Map<String, Object> data = new HashMap<>();
        data.put("groupName", groupName);
        data.put("creatorName", creatorName);
        data.put("fcmToken", fcmToken);

        return functions.getHttpsCallable("createGroup")
            .call(data)
            .continueWith(task -> {
                Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                return (String) result.get("groupId");
            });
    }

    public static com.google.android.gms.tasks.Task<Map<String, Object>> joinGroup(
            String groupId, String name, String role, String fcmToken) {
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", groupId);
        data.put("name", name);
        data.put("role", role);
        data.put("fcmToken", fcmToken);

        return functions.getHttpsCallable("joinGroup")
            .call(data)
            .continueWith(task -> (Map<String, Object>) task.getResult().getData());
    }

    public static com.google.android.gms.tasks.Task<Void> registerToken(
            String groupId, String fcmToken) {
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", groupId);
        data.put("fcmToken", fcmToken);

        return functions.getHttpsCallable("registerToken")
            .call(data)
            .continueWith(task -> null);
    }

    public static com.google.android.gms.tasks.Task<Map<String, Object>> sendAlert(
            String groupId, List<String> targetMemberIds, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", groupId);
        data.put("targetMemberIds", targetMemberIds);
        data.put("message", message);

        return functions.getHttpsCallable("sendAlert")
            .call(data)
            .continueWith(task -> (Map<String, Object>) task.getResult().getData());
    }
}
