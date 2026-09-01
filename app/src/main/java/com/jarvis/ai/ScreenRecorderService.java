package com.jarvis.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecorderService extends Service {

    private static final String TAG = "ScreenRecorderService";
    public static final String CHANNEL_ID = "henry_screen_recorder_channel";
    public static final int NOTIFICATION_ID = 9001;

    public static final String ACTION_START = "com.jarvis.ai.START_RECORDING";
    public static final String ACTION_STOP = "com.jarvis.ai.STOP_RECORDING";
    public static final String ACTION_PAUSE = "com.jarvis.ai.PAUSE_RECORDING";
    public static final String ACTION_RESUME = "com.jarvis.ai.RESUME_RECORDING";
    public static final String ACTION_STATE_CHANGED = "com.jarvis.ai.SCREEN_RECORD_STATE_CHANGED";

    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    public static final String EXTRA_RESULT_DATA = "extra_result_data";
    public static final String EXTRA_ENABLE_MIC = "extra_enable_mic";

    public static boolean isRecording = false;
    public static boolean isPaused = false;
    public static String currentRecordingPath = null;
    public static long currentStartTimeMs = 0;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;

    private WindowManager windowManager;
    private View floatingControlsView;
    private WindowManager.LayoutParams floatingParams;

    private TextView tvTimer;
    private TextView tvRecDot;
    private TextView tvExpandedTimer;
    private TextView btnPauseFloating;
    private TextView btnStopFloating;
    private TextView btnMicToggle;
    private LinearLayout layoutCollapsed;
    private LinearLayout layoutExpanded;
    private boolean isOverlayExpanded = false;

    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private int screenDensity = 320;
    private boolean recordMic = true;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTimeMs = 0;
    private long pausedDurationMs = 0;
    private long pauseStartTimeMs = 0;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording && !isPaused) {
                long elapsed = System.currentTimeMillis() - startTimeMs - pausedDurationMs;
                int seconds = (int) (elapsed / 1000) % 60;
                int minutes = (int) ((elapsed / (1000 * 60)) % 60);
                String timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds);
                if (tvTimer != null) {
                    tvTimer.setText(timeStr);
                }
                if (tvExpandedTimer != null) {
                    tvExpandedTimer.setText(timeStr);
                }
                // Blink REC dot
                if (tvRecDot != null) {
                    tvRecDot.setVisibility((seconds % 2 == 0) ? View.VISIBLE : View.INVISIBLE);
                }
            }
            if (isRecording) {
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopRecording();
            return START_NOT_STICKY;
        } else if (ACTION_PAUSE.equals(action)) {
            pauseRecording();
            return START_STICKY;
        } else if (ACTION_RESUME.equals(action)) {
            resumeRecording();
            return START_STICKY;
        } else if (ACTION_START.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            recordMic = intent.getBooleanExtra(EXTRA_ENABLE_MIC, true);

            if (resultCode != 0 && resultData != null) {
                startForegroundServiceCompat();
                startRecording(resultCode, resultData);
            } else {
                Log.e(TAG, "Missing projection result data");
                stopSelf();
            }
        }

        return START_STICKY;
    }

    private void startForegroundServiceCompat() {
        Notification notification = buildNotification("Recording screen in progress...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            if (recordMic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            }
            startForeground(NOTIFICATION_ID, notification, type);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "HENRY Screen Recorder",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows active screen recording controls and status");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String content) {
        Intent stopIntent = new Intent(this, ScreenRecorderService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pStop = PendingIntent.getService(this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pOpen = PendingIntent.getActivity(this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔴 H·E·N·R·Y Screen Recorder")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pOpen)
            .addAction(android.R.drawable.ic_media_pause, "Stop & Save", pStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void startRecording(int resultCode, Intent resultData) {
        try {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            screenWidth = (metrics.widthPixels / 2) * 2;
            screenHeight = (metrics.heightPixels / 2) * 2;
            screenDensity = metrics.densityDpi;

            File videoDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HENRY_Recordings");
            if (!videoDir.exists()) {
                videoDir.mkdirs();
            }
            if (!videoDir.exists()) {
                videoDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                if (videoDir != null && !videoDir.exists()) videoDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File outputFile = new File(videoDir, "HENRY_REC_" + timestamp + ".mp4");
            currentRecordingPath = outputFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            if (recordMic) {
                try {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                } catch (Exception e) {
                    Log.w(TAG, "Could not set audio source mic: " + e.getMessage());
                    recordMic = false;
                }
            }
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(currentRecordingPath);
            mediaRecorder.setVideoSize(screenWidth, screenHeight);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            if (recordMic) {
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setAudioSamplingRate(44100);
                mediaRecorder.setAudioEncodingBitRate(128000);
            }
            mediaRecorder.setVideoEncodingBitRate(6 * 1024 * 1024); // 6 Mbps
            mediaRecorder.setVideoFrameRate(30);

            mediaRecorder.prepare();

            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
            if (mediaProjection == null) {
                Toast.makeText(this, "Failed to obtain screen projection permission.", Toast.LENGTH_SHORT).show();
                stopSelf();
                return;
            }

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "HENRY_ScreenDisplay",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null,
                null
            );

            mediaRecorder.start();
            isRecording = true;
            isPaused = false;
            startTimeMs = System.currentTimeMillis();
            currentStartTimeMs = startTimeMs;
            pausedDurationMs = 0;

            if (recordMic) {
                try {
                    Intent micServiceIntent = new Intent(this, MicAudioRecordService.class);
                    micServiceIntent.setAction(MicAudioRecordService.ACTION_START);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(micServiceIntent);
                    } else {
                        startService(micServiceIntent);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not start MicAudioRecordService: " + e.getMessage());
                }
            }

            timerHandler.post(timerRunnable);
            showFloatingControls();
            broadcastStateChanged();

            Toast.makeText(this, "🔴 Screen recording started!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen recorder: " + e.getMessage(), e);
            Toast.makeText(this, "Screen record start failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            cleanup();
            stopSelf();
        }
    }

    private void broadcastStateChanged() {
        Intent stateIntent = new Intent(ACTION_STATE_CHANGED);
        stateIntent.putExtra("isRecording", isRecording);
        stateIntent.putExtra("isPaused", isPaused);
        sendBroadcast(stateIntent);
    }

    private void pauseRecording() {
        if (!isRecording || isPaused) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null) {
            try {
                mediaRecorder.pause();
                isPaused = true;
                pauseStartTimeMs = System.currentTimeMillis();
                updateOverlayUi();
                broadcastStateChanged();
                try {
                    Intent micPause = new Intent(this, MicAudioRecordService.class);
                    micPause.setAction(MicAudioRecordService.ACTION_PAUSE);
                    startService(micPause);
                } catch (Exception ignored) {}
                Toast.makeText(this, "Recording paused", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Pause failed: " + e.getMessage());
            }
        }
    }

    private void resumeRecording() {
        if (!isRecording || !isPaused) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null) {
            try {
                mediaRecorder.resume();
                isPaused = false;
                pausedDurationMs += (System.currentTimeMillis() - pauseStartTimeMs);
                updateOverlayUi();
                broadcastStateChanged();
                try {
                    Intent micResume = new Intent(this, MicAudioRecordService.class);
                    micResume.setAction(MicAudioRecordService.ACTION_RESUME);
                    startService(micResume);
                } catch (Exception ignored) {}
                Toast.makeText(this, "Recording resumed", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Resume failed: " + e.getMessage());
            }
        }
    }

    private void updateOverlayUi() {
        if (btnPauseFloating != null) {
            btnPauseFloating.setText(isPaused ? "▶ Resume" : "⏸ Pause");
        }
    }

    private void stopRecording() {
        if (!isRecording) {
            cleanup();
            stopSelf();
            return;
        }

        try {
            isRecording = false;
            isPaused = false;
            timerHandler.removeCallbacks(timerRunnable);
            broadcastStateChanged();

            try {
                Intent micStop = new Intent(this, MicAudioRecordService.class);
                micStop.setAction(MicAudioRecordService.ACTION_STOP);
                startService(micStop);
            } catch (Exception ignored) {}

            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (Exception ignored) {}
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }

            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }

            removeFloatingControls();

            if (currentRecordingPath != null) {
                File file = new File(currentRecordingPath);
                if (file.exists() && file.length() > 0) {
                    // Index file into Android MediaStore so Gallery & TikTok/YouTube see it
                    MediaScannerConnection.scanFile(this, new String[]{currentRecordingPath}, new String[]{"video/mp4"}, (path, uri) -> {
                        Log.d(TAG, "Scanned video file: " + path + " -> " + uri);
                    });

                    // Launch Social Preview & AI Caption Studio Activity
                    Intent studioIntent = new Intent(this, ScreenRecordResultActivity.class);
                    studioIntent.putExtra(ScreenRecordResultActivity.EXTRA_VIDEO_PATH, currentRecordingPath);
                    studioIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(studioIntent);
                } else {
                    Toast.makeText(this, "Recording ended. (0 bytes captured)", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping screen record: " + e.getMessage(), e);
        } finally {
            cleanup();
            stopSelf();
        }
    }

    private void showFloatingControls() {
        if (floatingControlsView != null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return;
        }

        try {
            FrameLayout container = new FrameLayout(this);

            // ── 1. Collapsed Floating Action Capsule (Mini FAB Mode) ──
            layoutCollapsed = new LinearLayout(this);
            layoutCollapsed.setOrientation(LinearLayout.HORIZONTAL);
            layoutCollapsed.setGravity(Gravity.CENTER_VERTICAL);
            layoutCollapsed.setPadding(dp(12), dp(8), dp(12), dp(8));

            GradientDrawable bgCollapsed = new GradientDrawable();
            bgCollapsed.setColor(0xF0071322);
            bgCollapsed.setCornerRadius(dp(24));
            bgCollapsed.setStroke(dp(1.5f), 0xFF00FFCC);
            layoutCollapsed.setBackground(bgCollapsed);

            // Red dot
            tvRecDot = new TextView(this);
            tvRecDot.setText("🔴");
            tvRecDot.setTextSize(12f);
            tvRecDot.setPadding(0, 0, dp(6), 0);
            layoutCollapsed.addView(tvRecDot);

            // Timer
            tvTimer = new TextView(this);
            tvTimer.setText("00:00");
            tvTimer.setTextColor(0xFF00FFCC);
            tvTimer.setTextSize(13f);
            tvTimer.setTypeface(Typeface.DEFAULT_BOLD);
            tvTimer.setPadding(0, 0, dp(8), 0);
            layoutCollapsed.addView(tvTimer);

            // Quick Stop Button in Collapsed Mode
            TextView btnQuickStop = new TextView(this);
            btnQuickStop.setText("⏹");
            btnQuickStop.setTextColor(0xFFFF3366);
            btnQuickStop.setTextSize(14f);
            btnQuickStop.setPadding(dp(4), dp(2), dp(6), dp(2));
            btnQuickStop.setOnClickListener(v -> stopRecording());
            layoutCollapsed.addView(btnQuickStop);

            // Expand Arrow
            TextView tvExpand = new TextView(this);
            tvExpand.setText("⚙️");
            tvExpand.setTextSize(13f);
            tvExpand.setPadding(dp(4), dp(2), 0, dp(2));
            layoutCollapsed.addView(tvExpand);

            // Tap collapsed capsule to toggle expanded overlay control panel
            layoutCollapsed.setOnClickListener(v -> toggleOverlayPanel());

            // ── 2. Expanded Floating Overlay Control Panel ──
            layoutExpanded = new LinearLayout(this);
            layoutExpanded.setOrientation(LinearLayout.VERTICAL);
            layoutExpanded.setPadding(dp(14), dp(12), dp(14), dp(12));
            layoutExpanded.setVisibility(View.GONE);

            GradientDrawable bgExpanded = new GradientDrawable();
            bgExpanded.setColor(0xF4071322);
            bgExpanded.setCornerRadius(dp(18));
            bgExpanded.setStroke(dp(1.5f), 0xFF00D4FF);
            layoutExpanded.setBackground(bgExpanded);

            // Row 1: Header + Timer + Collapse Button
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvHeader = new TextView(this);
            tvHeader.setText("🔴 1080p HD RECORDING");
            tvHeader.setTextColor(0xFF00FFCC);
            tvHeader.setTextSize(12f);
            tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams lpHeader = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            row1.addView(tvHeader, lpHeader);

            tvExpandedTimer = new TextView(this);
            tvExpandedTimer.setText("00:00");
            tvExpandedTimer.setTextColor(0xFF00D4FF);
            tvExpandedTimer.setTextSize(13f);
            tvExpandedTimer.setTypeface(Typeface.DEFAULT_BOLD);
            tvExpandedTimer.setPadding(dp(8), dp(2), dp(8), dp(2));
            GradientDrawable timerBg = new GradientDrawable();
            timerBg.setColor(0xFF14243B);
            timerBg.setCornerRadius(dp(8));
            tvExpandedTimer.setBackground(timerBg);
            row1.addView(tvExpandedTimer);

            TextView btnCollapse = new TextView(this);
            btnCollapse.setText(" ✕ ");
            btnCollapse.setTextColor(0xFF88A8D0);
            btnCollapse.setTextSize(14f);
            btnCollapse.setPadding(dp(8), dp(4), 0, dp(4));
            btnCollapse.setOnClickListener(v -> toggleOverlayPanel());
            row1.addView(btnCollapse);

            layoutExpanded.addView(row1);

            // Row 2: Action Buttons (Pause/Resume & Stop)
            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setGravity(Gravity.CENTER_VERTICAL);
            row2.setPadding(0, dp(10), 0, 0);

            btnPauseFloating = new TextView(this);
            btnPauseFloating.setText(isPaused ? "▶ Resume" : "⏸ Pause");
            btnPauseFloating.setTextColor(0xFF00D4FF);
            btnPauseFloating.setTextSize(12f);
            btnPauseFloating.setTypeface(Typeface.DEFAULT_BOLD);
            btnPauseFloating.setGravity(Gravity.CENTER);
            btnPauseFloating.setPadding(dp(12), dp(8), dp(12), dp(8));
            GradientDrawable pauseBg = new GradientDrawable();
            pauseBg.setColor(0xFF132845);
            pauseBg.setCornerRadius(dp(10));
            pauseBg.setStroke(dp(1), 0xFF00D4FF);
            btnPauseFloating.setBackground(pauseBg);
            btnPauseFloating.setOnClickListener(v -> {
                if (isPaused) resumeRecording();
                else pauseRecording();
            });
            LinearLayout.LayoutParams lpPause = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            lpPause.setMargins(0, 0, dp(6), 0);
            row2.addView(btnPauseFloating, lpPause);

            btnStopFloating = new TextView(this);
            btnStopFloating.setText("⏹ Stop & Studio");
            btnStopFloating.setTextColor(0xFFFFFFFF);
            btnStopFloating.setTextSize(12f);
            btnStopFloating.setTypeface(Typeface.DEFAULT_BOLD);
            btnStopFloating.setGravity(Gravity.CENTER);
            btnStopFloating.setPadding(dp(12), dp(8), dp(12), dp(8));
            GradientDrawable stopBg = new GradientDrawable();
            stopBg.setColor(0xFFFF2255);
            stopBg.setCornerRadius(dp(10));
            btnStopFloating.setBackground(stopBg);
            btnStopFloating.setOnClickListener(v -> stopRecording());
            LinearLayout.LayoutParams lpStop = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f);
            row2.addView(btnStopFloating, lpStop);

            layoutExpanded.addView(row2);

            // Row 3: Quick Action Chips (Social Studio + Mic Info)
            LinearLayout row3 = new LinearLayout(this);
            row3.setOrientation(LinearLayout.HORIZONTAL);
            row3.setGravity(Gravity.CENTER_VERTICAL);
            row3.setPadding(0, dp(8), 0, 0);

            TextView btnStudioShortcut = new TextView(this);
            btnStudioShortcut.setText("🎬 TikTok / YouTube Studio");
            btnStudioShortcut.setTextColor(0xFF00FFCC);
            btnStudioShortcut.setTextSize(11f);
            btnStudioShortcut.setGravity(Gravity.CENTER);
            btnStudioShortcut.setPadding(dp(8), dp(6), dp(8), dp(6));
            GradientDrawable studioBg = new GradientDrawable();
            studioBg.setColor(0xFF0C1D33);
            studioBg.setCornerRadius(dp(8));
            btnStudioShortcut.setBackground(studioBg);
            btnStudioShortcut.setOnClickListener(v -> {
                Intent studioIntent = new Intent(ScreenRecorderService.this, ScreenRecordResultActivity.class);
                studioIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(studioIntent);
            });
            LinearLayout.LayoutParams lpStudio = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            row3.addView(btnStudioShortcut, lpStudio);

            layoutExpanded.addView(row3);

            container.addView(layoutCollapsed);
            container.addView(layoutExpanded);

            int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

            floatingParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            floatingParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            floatingParams.x = 0;
            floatingParams.y = dp(60);

            // Drag touch listener for entire container
            container.setOnTouchListener(new View.OnTouchListener() {
                private int initialX, initialY;
                private float initialTouchX, initialTouchY;
                private boolean isDragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = floatingParams.x;
                            initialY = floatingParams.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            isDragging = false;
                            return false;
                        case MotionEvent.ACTION_MOVE:
                            float dx = Math.abs(event.getRawX() - initialTouchX);
                            float dy = Math.abs(event.getRawY() - initialTouchY);
                            if (dx > dp(6) || dy > dp(6)) {
                                isDragging = true;
                                floatingParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                                floatingParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                                if (floatingControlsView != null) {
                                    windowManager.updateViewLayout(floatingControlsView, floatingParams);
                                }
                                return true;
                            }
                            return false;
                        case MotionEvent.ACTION_UP:
                            return isDragging;
                    }
                    return false;
                }
            });

            floatingControlsView = container;
            windowManager.addView(floatingControlsView, floatingParams);
        } catch (Exception e) {
            Log.e(TAG, "Could not show floating screen record widget: " + e.getMessage());
        }
    }

    private void toggleOverlayPanel() {
        if (layoutCollapsed == null || layoutExpanded == null) return;
        isOverlayExpanded = !isOverlayExpanded;
        if (isOverlayExpanded) {
            layoutCollapsed.setVisibility(View.GONE);
            layoutExpanded.setVisibility(View.VISIBLE);
        } else {
            layoutExpanded.setVisibility(View.GONE);
            layoutCollapsed.setVisibility(View.VISIBLE);
        }
        if (floatingControlsView != null && windowManager != null) {
            windowManager.updateViewLayout(floatingControlsView, floatingParams);
        }
    }

    private void removeFloatingControls() {
        if (floatingControlsView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingControlsView);
            } catch (Exception ignored) {}
            floatingControlsView = null;
        }
    }

    private void cleanup() {
        isRecording = false;
        isPaused = false;
        timerHandler.removeCallbacks(timerRunnable);
        removeFloatingControls();
        try {
            stopForeground(true);
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int dp(float val) {
        return (int) (val * getResources().getDisplayMetrics().density + 0.5f);
    }
}
