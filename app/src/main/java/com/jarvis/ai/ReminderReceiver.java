package com.jarvis.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_LABEL = "reminder_label";
    public static final String CHANNEL_ID  = "henry_reminders";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String label = intent.getStringExtra(EXTRA_LABEL);
        if (label == null || label.isEmpty()) label = "Reminder";
        int notifId = intent.getIntExtra("notifId", 2000);
        createChannel(ctx);
        showNotification(ctx, label, notifId);
        speakReminder(ctx, "Sir, reminder: " + label);
    }

    private void showNotification(Context ctx, String label, int notifId) {
        PendingIntent pi = PendingIntent.getActivity(ctx, notifId,
            new Intent(ctx, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle("H.E.N.R.Y Reminder")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(new long[]{0, 500, 200, 500})
            .build();
        NotificationManager nm = (NotificationManager)
            ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(notifId, n);
    }

    private void speakReminder(Context ctx, String text) {
        TextToSpeech[] t = {null};
        t[0] = new TextToSpeech(ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                t[0].setLanguage(java.util.Locale.US);
                t[0].speak(text, TextToSpeech.QUEUE_FLUSH, null, "rem");
                new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(() -> { if (t[0] != null) { t[0].shutdown(); t[0] = null; } }, 8000);
            }
        });
    }

    private void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "HENRY Reminders", NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
