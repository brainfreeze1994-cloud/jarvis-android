package com.jarvis.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Car Mode — large buttons, voice-only, hands-free AI assistant.
 * Safe for driving: big tap zones, automatic listening, no keyboard.
 */
public class CarModeActivity extends AppCompatActivity {

    private static final int PERM_MIC = 501;

    private TextView     tvStatus, tvResponse, tvSpeed;
    private LinearLayout btnMic, btnNav, btnCall, btnMusic, btnExit;

    private SpeechRecognizer  speechRec;
    private TextToSpeech      tts;
    private boolean           ttsReady   = false;
    private boolean           isListening = false;
    private OkHttpClient      httpClient;
    private final Handler     handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on, fullscreen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();

        buildUI();

        // Init TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setPitch(0.85f); tts.setSpeechRate(0.9f);
                ttsReady = true;
                speak("Car mode activated. I'm ready, sir.");
            }
        }, "com.google.android.tts");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERM_MIC);
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF020C1B);
        root.setPadding(24, 48, 24, 24);

        // Title bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("H.E.N.R.Y  ◆  CAR MODE");
        title.setTextColor(0xFF00BEFF);
        title.setTextSize(18f);
        title.setLetterSpacing(0.2f);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topBar.addView(title);

        tvSpeed = new TextView(this);
        tvSpeed.setText("🚗 DRIVING");
        tvSpeed.setTextColor(0xFF004466);
        tvSpeed.setTextSize(12f);
        topBar.addView(tvSpeed);
        root.addView(topBar);

        // Status
        tvStatus = new TextView(this);
        tvStatus.setText("TAP MIC TO SPEAK");
        tvStatus.setTextColor(0xFF004466);
        tvStatus.setTextSize(11f);
        tvStatus.setLetterSpacing(0.1f);
        tvStatus.setPadding(0, 16, 0, 8);
        root.addView(tvStatus);

        // Divider
        View div = new View(this);
        div.setBackgroundColor(0xFF081830);
        div.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        root.addView(div);

        // MIC — giant button
        btnMic = makeCarBtn("🎤  SPEAK TO HENRY", 0xFF00BEFF, 0xFF020C1B, 120);
        btnMic.setOnClickListener(v -> toggleListening());
        root.addView(btnMic);

        // Response display
        tvResponse = new TextView(this);
        tvResponse.setText("Ready to assist, sir.");
        tvResponse.setTextColor(0xFF80DFFF);
        tvResponse.setTextSize(16f);
        tvResponse.setLineSpacing(4, 1.4f);
        tvResponse.setPadding(0, 16, 0, 16);
        root.addView(tvResponse);

        // Action buttons row
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        btnNav   = makeCarBtn("🗺\nNAVIGATE", 0xFF041828, 0xFF00BEFF, 80);
        btnCall  = makeCarBtn("📞\nCALL", 0xFF041828, 0xFF00BEFF, 80);
        btnMusic = makeCarBtn("🎵\nMUSIC", 0xFF041828, 0xFF00BEFF, 80);
        btnExit  = makeCarBtn("✕\nEXIT", 0xFF180404, 0xFFCC3030, 80);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(4, 0, 4, 0);
        btnNav.setLayoutParams(lp);
        btnCall.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnMusic.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnExit.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        btnNav.setOnClickListener(v   -> startListeningFor("navigate"));
        btnCall.setOnClickListener(v  -> startListeningFor("call"));
        btnMusic.setOnClickListener(v -> handleMusicToggle());
        btnExit.setOnClickListener(v  -> finish());

        actionRow.addView(btnNav);
        actionRow.addView(btnCall);
        actionRow.addView(btnMusic);
        actionRow.addView(btnExit);
        root.addView(actionRow);

        setContentView(root);
    }

    private LinearLayout makeCarBtn(String label, int bgColor, int textColor, int heightDp) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(android.view.Gravity.CENTER);
        btn.setBackgroundColor(bgColor);
        int h = (int)(heightDp * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, h);
        lp.setMargins(0, 16, 0, 0);
        btn.setLayoutParams(lp);
        btn.setPadding(16, 8, 16, 8);
        btn.setClickable(true); btn.setFocusable(true);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(textColor);
        tv.setTextSize(20f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setLetterSpacing(0.05f);
        btn.addView(tv);
        return btn;
    }

    private void toggleListening() {
        if (isListening) stopListening(); else startListening(null);
    }

    private void startListeningFor(String context) {
        tvStatus.setText("SAY: " + context.toUpperCase(Locale.US) + " DESTINATION…");
        startListening(context);
    }

    private void startListening(String context) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Mic permission required", Toast.LENGTH_SHORT).show(); return;
        }
        if (speechRec != null) { try { speechRec.destroy(); } catch (Exception ignored) {} }
        speechRec = SpeechRecognizer.createSpeechRecognizer(this);
        speechRec.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                isListening = true;
                runOnUiThread(() -> {
                    tvStatus.setText("LISTENING…");
                    tvStatus.setTextColor(0xFF00BEFF);
                    setBtnLabel(btnMic, "🔴  LISTENING…", 0xFF002244);
                });
            }
            @Override public void onEndOfSpeech() { isListening = false; }
            @Override public void onError(int error) {
                isListening = false;
                runOnUiThread(() -> {
                    tvStatus.setText("TAP MIC TO SPEAK");
                    tvStatus.setTextColor(0xFF004466);
                    setBtnLabel(btnMic, "🎤  SPEAK TO HENRY", 0xFF00BEFF);
                });
            }
            @Override public void onResults(Bundle results) {
                isListening = false;
                runOnUiThread(() -> {
                    setBtnLabel(btnMic, "🎤  SPEAK TO HENRY", 0xFF00BEFF);
                    tvStatus.setText("PROCESSING…");
                });
                ArrayList<String> m = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (m != null && !m.isEmpty()) {
                    String query = m.get(0).trim();
                    if ("navigate".equals(context)) {
                        String dest = NavigationHelper.navigate(CarModeActivity.this, query, false);
                        showResponse(dest.replaceAll("\\[EMOTION:\\w+\\]\\s*", ""));
                    } else if ("call".equals(context)) {
                        String num = ContactsHelper.findNumber(CarModeActivity.this, query);
                        if (num != null) {
                            Intent i = ContactsHelper.callIntent(num);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            showResponse("Calling " + query + ", sir.");
                        } else {
                            showResponse("Contact not found: " + query);
                        }
                    } else {
                        askHenry(query);
                    }
                }
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float r) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int t, Bundle b) {}
        });
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try { speechRec.startListening(i); } catch (Exception ignored) {}
    }

    private void stopListening() {
        if (speechRec != null) try { speechRec.stopListening(); } catch (Exception ignored) {}
        isListening = false;
    }

    private void askHenry(String query) {
        new Thread(() -> {
            try {
                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", "[CAR MODE - BRIEF REPLY ONLY] " + query);
                msgs.put(msg);
                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "brief");
                RequestBody rb = RequestBody.create(
                    body.toString(), MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                    .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                    .post(rb).addHeader("Content-Type", "application/json").build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) { showResponse("Network error, sir."); return; }
                    JSONObject j = new JSONObject(resp.body().string());
                    String reply = j.optString("reply", "").replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                    showResponse(reply);
                }
            } catch (Exception e) { showResponse("Error: " + e.getMessage()); }
        }).start();
    }

    private void showResponse(String text) {
        runOnUiThread(() -> {
            tvResponse.setText(text);
            tvStatus.setText("TAP MIC TO SPEAK");
            tvStatus.setTextColor(0xFF004466);
            speak(text);
        });
    }

    private void setBtnLabel(LinearLayout btn, String label, int color) {
        if (btn.getChildCount() > 0 && btn.getChildAt(0) instanceof TextView) {
            ((TextView) btn.getChildAt(0)).setText(label);
            btn.setBackgroundColor(color);
        }
    }

    private void handleMusicToggle() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am != null) am.dispatchMediaKeyEvent(
            new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        am.dispatchMediaKeyEvent(
            new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,
                android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        showResponse("Music toggled, sir.");
    }

    private void speak(String text) {
        if (!ttsReady || tts == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
    }

    @Override public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
    }

    @Override protected void onDestroy() {
        if (speechRec != null) try { speechRec.destroy(); } catch (Exception ignored) {}
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
