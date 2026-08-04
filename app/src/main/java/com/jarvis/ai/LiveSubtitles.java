package com.jarvis.ai;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Live Subtitles — real-time speech-to-text overlay captions.
 * Runs as foreground overlay; captions appear at the bottom of any screen.
 * "Start subtitles", "Turn on live captions", "Stop subtitles"
 */
public class LiveSubtitles extends Service {

    public static final String ACTION_START = "com.jarvis.ai.SUBTITLES_START";
    public static final String ACTION_STOP  = "com.jarvis.ai.SUBTITLES_STOP";

    private WindowManager     windowManager;
    private View              overlayView;
    private TextView          tvSubtitle;
    private SpeechRecognizer  speechRec;
    private boolean           active = false;

    private static boolean running = false;
    public static boolean isRunning() { return running; }

    public static boolean isSubtitleCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("live caption") || lower.contains("live subtitle") ||
               lower.contains("start subtitle") || lower.contains("stop subtitle") ||
               lower.contains("turn on caption") || lower.contains("turn off caption") ||
               lower.contains("real time caption") || lower.contains("realtime caption") ||
               lower.contains("show caption") || lower.contains("hide caption") ||
               lower.contains("transcribe live") || lower.contains("caption mode");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildOverlay();
        startRecognition();
    }

    private void buildOverlay() {
        tvSubtitle = new TextView(this);
        tvSubtitle.setTextColor(0xFFFFFFFF);
        tvSubtitle.setTextSize(16f);
        tvSubtitle.setPadding(24, 12, 24, 12);
        tvSubtitle.setBackgroundColor(0xCC000000);
        tvSubtitle.setMaxLines(3);

        int type = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 160;

        overlayView = tvSubtitle;
        windowManager.addView(overlayView, params);
    }

    private void startRecognition() {
        active = true;
        speechRec = SpeechRecognizer.createSpeechRecognizer(this);
        speechRec.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int e) { if (active) restartRecognition(); }
            @Override public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) updateSubtitle(r.get(0));
                if (active) restartRecognition();
            }
            @Override public void onPartialResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) updateSubtitle(r.get(0) + "…");
            }
            @Override public void onEvent(int t, Bundle b) {}
        });
        listen();
    }

    private void listen() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L);
        try { speechRec.startListening(i); } catch (Exception ignored) {}
    }

    private void restartRecognition() {
        try { Thread.sleep(300); } catch (Exception ignored) {}
        try { speechRec.startListening(buildIntent()); } catch (Exception ignored) {}
    }

    private Intent buildIntent() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        return i;
    }

    private void updateSubtitle(String text) {
        if (tvSubtitle != null) {
            tvSubtitle.post(() -> tvSubtitle.setText(text));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        active = false;
        running = false;
        if (speechRec != null) { try { speechRec.destroy(); } catch (Exception ignored) {} }
        if (overlayView != null && windowManager != null) {
            try { windowManager.removeView(overlayView); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
