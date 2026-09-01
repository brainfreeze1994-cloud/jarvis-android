package com.jarvis.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Foreground Service dedicated to capturing high-quality microphone audio
 * and writing it to a temporary file while screen recording or voice sessions are active.
 */
public class MicAudioRecordService extends Service {

    private static final String TAG = "MicAudioRecordService";
    public static final String CHANNEL_ID = "henry_mic_recorder_channel";
    public static final int NOTIFICATION_ID = 9002;

    public static final String ACTION_START = "com.jarvis.ai.START_MIC_AUDIO_RECORD";
    public static final String ACTION_STOP = "com.jarvis.ai.STOP_MIC_AUDIO_RECORD";
    public static final String ACTION_PAUSE = "com.jarvis.ai.PAUSE_MIC_AUDIO_RECORD";
    public static final String ACTION_RESUME = "com.jarvis.ai.RESUME_MIC_AUDIO_RECORD";
    public static final String ACTION_STATE_CHANGED = "com.jarvis.ai.MIC_AUDIO_STATE_CHANGED";

    public static final String EXTRA_OUTPUT_PATH = "extra_output_path";

    public static boolean isRecording = false;
    public static boolean isPaused = false;
    public static File currentTempAudioFile = null;
    public static String currentTempAudioPath = null;
    public static long startTimeMs = 0;
    private long pausedDurationMs = 0;
    private long pauseStartTimeMs = 0;

    private MediaRecorder mediaRecorder;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            String explicitPath = intent.getStringExtra(EXTRA_OUTPUT_PATH);
            startForegroundServiceCompat();
            startAudioRecording(explicitPath);
            return START_STICKY;
        } else if (ACTION_STOP.equals(action)) {
            stopAudioRecording();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        } else if (ACTION_PAUSE.equals(action)) {
            pauseAudioRecording();
            return START_STICKY;
        } else if (ACTION_RESUME.equals(action)) {
            resumeAudioRecording();
            return START_STICKY;
        }

        return START_STICKY;
    }

    private void startForegroundServiceCompat() {
        Notification notification = buildNotification("Recording microphone audio to temporary file...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int type = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            }
            if (type != 0) {
                startForeground(NOTIFICATION_ID, notification, type);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "HENRY Microphone Recorder",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Records microphone commentary into a temporary audio file");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String content) {
        Intent stopIntent = new Intent(this, MicAudioRecordService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pStop = PendingIntent.getService(this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pOpen = PendingIntent.getActivity(this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎙️ H·E·N·R·Y Microphone Audio Recording")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pOpen)
            .addAction(android.R.drawable.ic_media_pause, "Stop Audio", pStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void startAudioRecording(String explicitPath) {
        if (isRecording) {
            Log.d(TAG, "Audio recording already active");
            return;
        }

        try {
            if (explicitPath != null && !explicitPath.isEmpty()) {
                currentTempAudioFile = new File(explicitPath);
            } else {
                File cacheDir = getExternalCacheDir();
                if (cacheDir == null || !cacheDir.exists()) {
                    cacheDir = getCacheDir();
                }
                File audioTempDir = new File(cacheDir, "temp_mic_audio");
                if (!audioTempDir.exists()) {
                    audioTempDir.mkdirs();
                }
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                currentTempAudioFile = File.createTempFile("henry_mic_audio_" + timestamp + "_", ".m4a", audioTempDir);
            }

            currentTempAudioPath = currentTempAudioFile.getAbsolutePath();
            Log.i(TAG, "Recording microphone audio to temporary file: " + currentTempAudioPath);

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(currentTempAudioPath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioChannels(1); // Mono microphone channel

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            isPaused = false;
            startTimeMs = System.currentTimeMillis();
            pausedDurationMs = 0;
            broadcastStateChanged();

        } catch (Exception e) {
            Log.e(TAG, "Failed to start microphone audio recording: " + e.getMessage(), e);
            cleanup();
            stopForeground(true);
            stopSelf();
        }
    }

    private void pauseAudioRecording() {
        if (!isRecording || isPaused) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null) {
            try {
                mediaRecorder.pause();
                isPaused = true;
                pauseStartTimeMs = System.currentTimeMillis();
                broadcastStateChanged();
                Log.d(TAG, "Microphone audio recording paused");
            } catch (Exception e) {
                Log.e(TAG, "Pause audio failed: " + e.getMessage());
            }
        }
    }

    private void resumeAudioRecording() {
        if (!isRecording || !isPaused) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null) {
            try {
                mediaRecorder.resume();
                isPaused = false;
                pausedDurationMs += (System.currentTimeMillis() - pauseStartTimeMs);
                broadcastStateChanged();
                Log.d(TAG, "Microphone audio recording resumed");
            } catch (Exception e) {
                Log.e(TAG, "Resume audio failed: " + e.getMessage());
            }
        }
    }

    private void stopAudioRecording() {
        if (!isRecording) {
            cleanup();
            return;
        }

        try {
            isRecording = false;
            isPaused = false;
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (Exception e) {
                    Log.w(TAG, "MediaRecorder stop warning: " + e.getMessage());
                }
                try {
                    mediaRecorder.release();
                } catch (Exception ignored) {}
                mediaRecorder = null;
            }
            broadcastStateChanged();
            Log.i(TAG, "Microphone audio saved to temporary file: " + currentTempAudioPath);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping audio recorder: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void broadcastStateChanged() {
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        intent.putExtra("isRecording", isRecording);
        intent.putExtra("isPaused", isPaused);
        intent.putExtra("tempAudioPath", currentTempAudioPath);
        sendBroadcast(intent);
    }

    private void cleanup() {
        isRecording = false;
        isPaused = false;
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception ignored) {}
            mediaRecorder = null;
        }
    }

    @Override
    public void onDestroy() {
        stopAudioRecording();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static File getTempAudioFile() {
        return currentTempAudioFile;
    }

    public static String getTempAudioPath() {
        return currentTempAudioPath;
    }
}
