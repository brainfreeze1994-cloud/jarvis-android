package com.jarvis.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

public class FloatingBubbleService extends Service {

    public static final String ACTION_STOP   = "com.jarvis.ai.STOP_BUBBLE";
    public static final String CHANNEL_ID    = "henry_bubble";

    private WindowManager wm;
    private View          bubbleView;
    private WindowManager.LayoutParams params;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf(); return START_NOT_STICKY;
        }
        startForeground(8888, buildNotification());
        showBubble();
        return START_STICKY;
    }

    private void showBubble() {
        if (bubbleView != null) return;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Build a simple bubble view programmatically
        TextView tv = new TextView(this);
        tv.setText("H");
        tv.setTextSize(20f);
        tv.setTextColor(0xFFC9A84C);
        tv.setBackgroundColor(0xEE0D0D0D);
        tv.setPadding(28, 20, 28, 20);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20; params.y = 300;

        bubbleView = tv;

        // Tap → open HENRY
        tv.setOnClickListener(v -> {
            Intent open = new Intent(this, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(open);
        });

        // Long-press → dismiss bubble
        tv.setOnLongClickListener(v -> { stopSelf(); return true; });

        // Drag
        tv.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY; float initTouchX, initTouchY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = params.x; initY = params.y;
                        initTouchX = e.getRawX(); initTouchY = e.getRawY();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initX + (int)(e.getRawX() - initTouchX);
                        params.y = initY + (int)(e.getRawY() - initTouchY);
                        wm.updateViewLayout(bubbleView, params);
                        return true;
                }
                return false;
            }
        });

        try { wm.addView(bubbleView, params); } catch (Exception ignored) {}
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "H.E.N.R.Y Bubble", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class), flags);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("H.E.N.R.Y is watching, sir")
            .setContentText("Tap to open • Long-press bubble to dismiss")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        super.onDestroy();
        if (bubbleView != null && wm != null) {
            try { wm.removeView(bubbleView); } catch (Exception ignored) {}
            bubbleView = null;
        }
    }
}
