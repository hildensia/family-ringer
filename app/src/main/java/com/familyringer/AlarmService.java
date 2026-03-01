package com.familyringer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private int originalAlarmVolume = -1;
    private static final String NOTIF_CHANNEL = "alarm_service_channel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createServiceChannel();

        String message = intent != null ? intent.getStringExtra("message") : getString(R.string.default_alert_message);

        Notification notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(getString(R.string.notif_service_title))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();
        startForeground(1002, notification);

        SessionManager session = new SessionManager(this);

        // Save original volume, set to configured %
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int targetVolume = (int) (maxVolume * (session.getAlertVolume() / 100.0));
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0);

        // Resolve sound URI — use saved, or fall back to default notification sound
        String savedUri = session.getAlertSoundUri();
        Uri soundUri = savedUri != null
                ? Uri.parse(savedUri)
                : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Play sound
        try {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            AssetFileDescriptor afd =
                    getContentResolver().openAssetFileDescriptor(soundUri, "r");
            if (afd != null) {
                mediaPlayer.setDataSource(afd.getFileDescriptor(),
                        afd.getStartOffset(), afd.getLength());
                afd.close();
            } else {
                // fallback to default
                Uri fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                mediaPlayer.setDataSource(getApplicationContext(), fallback);
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            android.util.Log.e("AlarmService", "Failed to play alert sound", e);
        }

        // Vibrate
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] pattern = {0, 600, 300, 600, 300, 600};
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));

        // Stop after configured duration (0 = don't stop automatically)
        int duration = session.getAlertDuration();
        if (duration > 0) {
            new android.os.Handler().postDelayed(this::stopSelf, duration * 1000L);
        }

        return START_NOT_STICKY;
    }

    private void createServiceChannel() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(
                NOTIF_CHANNEL, getString(R.string.notif_service_channel), NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(ch);
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) vibrator.cancel();
        if (audioManager != null && originalAlarmVolume >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}