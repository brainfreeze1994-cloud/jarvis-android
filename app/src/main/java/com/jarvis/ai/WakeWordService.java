package com.jarvis.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class WakeWordService extends Service {

    public static final String ACTION_WAKE_WORD = "com.jarvis.ai.WAKE_WORD";
    public static final String ACTION_STOP      = "com.jarvis.ai.STOP_WAKE";
    private static final String CHANNEL_ID      = "henry_wake";
    private static final int    NOTIF_ID        = 1001;

    private SpeechRecognizer recognizer;
    private boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        createChannel();
        startForeground(NOTIF_ID, buildNotification());
        running = true;
        handler.postDelayed(this::startListening, 500);
        return START_STICKY;
    }

    private void startListening() {
        if (!running) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        if (recognizer != null) {
            try { recognizer.destroy(); } catch (Exception ignored) {}
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float r) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int t, Bundle b) {}
            @Override public void onPartialResults(Bundle partial) {
                ArrayList<String> r =
                    partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null) for (String s : r)
                    if (s != null && s.toLowerCase().contains("henry")) { trigger(); return; }
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> r =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null) for (String s : r)
                    if (s != null && s.toLowerCase().contains("henry")) { trigger(); return; }
                restartDelayed(400);
            }
            @Override public void onError(int error) { restartDelayed(1200); }
        });

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        try { recognizer.startListening(i); }
        catch (Exception e) { restartDelayed(2000); }
    }

    private void trigger() {
        Intent wake = new Intent(this, MainActivity.class);
        wake.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        wake.setAction(ACTION_WAKE_WORD);
        startActivity(wake);
        Intent bc = new Intent(ACTION_WAKE_WORD);
        bc.setPackage(getPackageName());
        sendBroadcast(bc);
        restartDelayed(7000);
    }

    private void restartDelayed(long ms) {
        handler.postDelayed(() -> { if (running) startListening(); }, ms);
    }

    @Override public void onDestroy() {
        running = false;
        if (recognizer != null) {
            try { recognizer.cancel(); recognizer.destroy(); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "HENRY Wake Word", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("H.E.N.R.Y — Listening")
            .setContentText("Say \"Henry\" to activate")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build();
    }
}
