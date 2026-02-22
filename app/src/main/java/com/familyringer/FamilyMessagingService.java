package com.familyringer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FamilyMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "family_ringer_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String message = remoteMessage.getData().get("alert_message");
        if (message == null) return;

        // Start alarm service (plays sound bypassing silent mode)
        Intent alarmIntent = new Intent(this, AlarmService.class);
        alarmIntent.putExtra("message", message);
        startForegroundService(alarmIntent);

        // Full-screen alert activity
        Intent alertIntent = new Intent(this, AlertActivity.class);
        alertIntent.putExtra("message", message);
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        createNotificationChannel();

        PendingIntent fullScreenIntent = PendingIntent.getActivity(
            this, 0, alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(getString(R.string.notif_alert_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, true)
            .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1001, builder.build());

        //startActivity(alertIntent);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // If the device is already set up, update the token in Firestore via Cloud Function
        SessionManager session = new SessionManager(this);
        if (session.isSetupComplete()) {
            CloudFunctions.registerToken(session.getGroupId(), token)
                .addOnFailureListener(e ->
                    android.util.Log.e("FCM", "Token refresh failed", e));
        }
    }

    private void createNotificationChannel() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Delete and recreate the channel to force importance update
        nm.deleteNotificationChannel(CHANNEL_ID);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, getString(R.string.notif_alert_channel), NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 200, 500});
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        AudioAttributes audio = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
        channel.setSound(null, null);
        nm.createNotificationChannel(channel);
    }
}
