package com.jarvis.ai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int    PERM_CODE        = 101;
    private static final int    REQUEST_GALLERY  = 200;
    private static final int    REQUEST_CAMERA   = 201;
    private static final String PREFS            = "jarvis_prefs";
    private static final String KEY_HIS          = "history_v2";
    private static final String KEY_VOICE        = "voice_choice";
    private static final String CRASH_FILE       = "henry_crash.txt";
    private static final String SPEAK_URL        = "https://jarvis-ai-seven-dun.vercel.app/api/speak";

    // Voice choices
    private static final String VOICE_BRITISH_MALE    = "british_male";
    private static final String VOICE_BRITISH_FEMALE  = "british_female";
    private static final String VOICE_AMERICAN_MALE   = "american_male";
    private static final String VOICE_AMERICAN_FEMALE = "american_female";
    private static final String VOICE_FILIPINO_MALE   = "filipino_male";
    private static final String VOICE_FILIPINO_FEMALE = "filipino_female";
    private static final String VOICE_FRENCH_MALE     = "french_male";
    private static final String VOICE_FRENCH_FEMALE   = "french_female";
    private String currentVoice = VOICE_BRITISH_MALE;

    private OrbView          orbView;
    private TextView         tvStatus;
    private TextView         tvOrbHint;
    private TextView         btnVoice;
    private RecyclerView     recycler;
    private EditText         etInput;
    private ImageButton      btnMic, btnSend, btnClear, btnAttach;
    private ImageView        ivAttachPreview;
    private LinearLayout     orbSection;
    private LinearLayout     chipsRow1, chipsRow2, chipsRow3;
    private NestedScrollView scrollMain;

    private final List<Message>     messages = new ArrayList<>();
    private final List<HistoryItem> history  = new ArrayList<>();
    private ChatAdapter adapter;
    private int typingPos = -1;

    private Uri    cameraImageUri;
    private String pendingImageBase64;
    private String pendingImageUriStr;

    // TTS — Edge TTS (neural, via Vercel) + native TTS fallback
    private TextToSpeech tts;
    private boolean      ttsReady   = false;
    private boolean      isSpeaking = false;
    private MediaPlayer  ttsPlayer  = null;

    private OkHttpClient     httpClient;
    private SpeechRecognizer speechRec;
    private boolean          isListening = false;

    private OrbView.OrbState currentState = OrbView.OrbState.IDLE;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson    gson        = new Gson();

    // ── onCreate ──────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final java.io.File crashFile = new java.io.File(getFilesDir(), CRASH_FILE);
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            try {
                String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage()
                           + "\n\n" + android.util.Log.getStackTraceString(ex);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(crashFile, false);
                fos.write(msg.getBytes("UTF-8"));
                fos.flush(); fos.getFD().sync(); fos.close();
            } catch (Exception ignored) {}
            android.os.Process.killProcess(android.os.Process.myPid());
        });

        if (crashFile.exists()) {
            try {
                java.io.FileInputStream fis = new java.io.FileInputStream(crashFile);
                byte[] data = new byte[(int) crashFile.length()];
                fis.read(data); fis.close();
                String lastCrash = new String(data, "UTF-8");
                crashFile.delete();
                new AlertDialog.Builder(this)
                    .setTitle("HENRY Crash Report").setMessage(lastCrash)
                    .setPositiveButton("OK", null).show();
            } catch (Exception ignored) {}
        }

        try { startApp(); }
        catch (Throwable t) {
            String msg = t.getClass().getSimpleName() + ": " + t.getMessage()
                       + "\n\n" + android.util.Log.getStackTraceString(t);
            try {
                new AlertDialog.Builder(this)
                    .setTitle("HENRY Startup Error").setMessage(msg)
                    .setPositiveButton("OK", null).show();
            } catch (Exception ignored) {
                Toast.makeText(this, "Fatal: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // ── startApp ──────────────────────────────────────────────────────────────
    private void startApp() {
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        setContentView(R.layout.activity_main);

        orbView         = findViewById(R.id.orb_view);
        tvStatus        = findViewById(R.id.tv_status);
        tvOrbHint       = findViewById(R.id.tv_orb_hint);
        btnVoice        = findViewById(R.id.btn_voice);
        recycler        = findViewById(R.id.recycler_chat);
        etInput         = findViewById(R.id.et_input);
        btnMic          = findViewById(R.id.btn_mic);
        btnSend         = findViewById(R.id.btn_send);
        btnClear        = findViewById(R.id.btn_clear);
        btnAttach       = findViewById(R.id.btn_attach);
        ivAttachPreview = findViewById(R.id.iv_attach_preview);
        orbSection      = findViewById(R.id.orb_section);
        chipsRow1       = findViewById(R.id.chips_row1);
        chipsRow2       = findViewById(R.id.chips_row2);
        chipsRow3       = findViewById(R.id.chips_row3);
        scrollMain      = findViewById(R.id.scroll_main);

        adapter = new ChatAdapter(messages);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(false);
        recycler.setLayoutManager(llm);
        recycler.setAdapter(adapter);
        recycler.setNestedScrollingEnabled(false);

        requestPerms();
        loadHistory();
        currentVoice = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString(KEY_VOICE, VOICE_BRITISH_MALE);
        initNativeTts();
        updateVoiceButtonLabel();

        if (orbView   != null) orbView.setOnClickListener(v -> toggleListening());
        if (btnMic    != null) btnMic.setOnClickListener(v -> toggleListening());
        if (btnSend   != null) btnSend.setOnClickListener(v -> sendText());
        if (btnClear  != null) btnClear.setOnClickListener(v -> confirmClear());
        if (btnVoice  != null) btnVoice.setOnClickListener(v -> showVoicePicker());
        if (btnAttach != null) btnAttach.setOnClickListener(v -> showAttachDialog());
        if (ivAttachPreview != null) ivAttachPreview.setOnClickListener(v -> clearAttachment());
        if (etInput != null) {
            etInput.setOnEditorActionListener((v, id, e) -> {
                if (id == EditorInfo.IME_ACTION_SEND) { sendText(); return true; }
                return false;
            });
        }

        setupChip(R.id.chip1, "What's the weather in Dubai?");
        setupChip(R.id.chip2, "Generate image of a warrior");
        setupChip(R.id.chip3, "Write me a Python script");
        setupChip(R.id.chip4, "Tell me something fascinating");
        setupChip(R.id.chip5, "Latest tech news today");
        setupChip(R.id.chip6, "Explain quantum computing");

        if (history.isEmpty()) {
            addJarvisMsg("Good day, sir. H.E.N.R.Y online. All systems nominal. How may I assist you?");
            mainHandler.postDelayed(() ->
                speak("Good day, sir. H.E.N.R.Y online. All systems nominal. How may I assist you?", "warm"),
                1500);
        } else {
            hideWelcome();
        }
    }

    private void setupChip(int chipId, String text) {
        View chip = findViewById(chipId);
        if (chip != null) chip.setOnClickListener(v -> { hideWelcome(); askJarvis(text); });
    }

    private void hideWelcome() {
        if (orbSection != null) orbSection.setVisibility(View.GONE);
        if (chipsRow1  != null) chipsRow1.setVisibility(View.GONE);
        if (chipsRow2  != null) chipsRow2.setVisibility(View.GONE);
        if (chipsRow3  != null) chipsRow3.setVisibility(View.GONE);
    }

    // ── Native TTS (offline fallback) ─────────────────────────────────────────
    private void initNativeTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                applyNativeVoice(currentVoice);
                ttsReady = true;
            }
        }, "com.google.android.tts");
    }

    private void applyNativeVoice(String voice) {
        if (tts == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {}
                @Override public void onDone(String id) {
                    mainHandler.post(() -> { isSpeaking = false; setState(OrbView.OrbState.IDLE); });
                }
                @Override public void onError(String id) {
                    mainHandler.post(() -> { isSpeaking = false; setState(OrbView.OrbState.IDLE); });
                }
            });
        }
        boolean isFrench = voice.startsWith("french");
        boolean isMale   = voice.endsWith("_male");
        if (isFrench) {
            tts.setLanguage(Locale.FRENCH);
            tts.setPitch(isMale ? 0.78f : 1.10f);
        } else {
            tts.setLanguage(new Locale("en", "GB"));
            tts.setPitch(isMale ? 0.75f : 1.05f);
        }
        tts.setSpeechRate(isMale ? 0.90f : 0.93f);
    }

    // ── Voice Picker ──────────────────────────────────────────────────────────
    private void showVoicePicker() {
        final String[] labels = {
            "\uD83C\uDDEC\uD83C\uDDE7  British   Male   \u2642  (Ryan · Neural)",
            "\uD83C\uDDEC\uD83C\uDDE7  British   Female \u2640  (Sonia · Neural)",
            "\uD83C\uDDFA\uD83C\uDDF8  American  Male   \u2642  (Guy · Neural)",
            "\uD83C\uDDFA\uD83C\uDDF8  American  Female \u2640  (Aria · Neural)",
            "\uD83C\uDDF5\uD83C\uDDED  Filipino  Male   \u2642  (James · Neural)",
            "\uD83C\uDDF5\uD83C\uDDED  Filipino  Female \u2640  (Blessica · Neural)",
            "\uD83C\uDDEB\uD83C\uDDF7  French    Male   \u2642  (Henri · Neural)",
            "\uD83C\uDDEB\uD83C\uDDF7  French    Female \u2640  (Denise · Neural)"
        };
        final String[] voices = {
            VOICE_BRITISH_MALE,    VOICE_BRITISH_FEMALE,
            VOICE_AMERICAN_MALE,   VOICE_AMERICAN_FEMALE,
            VOICE_FILIPINO_MALE,   VOICE_FILIPINO_FEMALE,
            VOICE_FRENCH_MALE,     VOICE_FRENCH_FEMALE
        };
        final String[] btnTexts = {
            "\uD83C\uDDEC\uD83C\uDDE7 \u2642 VOICE",
            "\uD83C\uDDEC\uD83C\uDDE7 \u2640 VOICE",
            "\uD83C\uDDFA\uD83C\uDDF8 \u2642 VOICE",
            "\uD83C\uDDFA\uD83C\uDDF8 \u2640 VOICE",
            "\uD83C\uDDF5\uD83C\uDDED \u2642 VOICE",
            "\uD83C\uDDF5\uD83C\uDDED \u2640 VOICE",
            "\uD83C\uDDEB\uD83C\uDDF7 \u2642 VOICE",
            "\uD83C\uDDEB\uD83C\uDDF7 \u2640 VOICE"
        };

        int current = 0;
        for (int i = 0; i < voices.length; i++)
            if (voices[i].equals(currentVoice)) { current = i; break; }
        final int[] selected = { current };

        new AlertDialog.Builder(this)
            .setTitle("\u25C6  H.E.N.R.Y Voice  (Microsoft Neural)")
            .setSingleChoiceItems(labels, current, (d, which) -> selected[0] = which)
            .setPositiveButton("Apply", (d, w) -> {
                currentVoice = voices[selected[0]];
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_VOICE, currentVoice).apply();
                applyNativeVoice(currentVoice);
                if (btnVoice != null) btnVoice.setText(btnTexts[selected[0]]);
                speak("H.E.N.R.Y online, sir.", "neutral");
                Toast.makeText(this, "Neural voice applied", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateVoiceButtonLabel() {
        if (btnVoice == null) return;
        switch (currentVoice) {
            case VOICE_BRITISH_MALE:    btnVoice.setText("\uD83C\uDDEC\uD83C\uDDE7 \u2642 VOICE"); break;
            case VOICE_BRITISH_FEMALE:  btnVoice.setText("\uD83C\uDDEC\uD83C\uDDE7 \u2640 VOICE"); break;
            case VOICE_AMERICAN_FEMALE: btnVoice.setText("\uD83C\uDDFA\uD83C\uDDF8 \u2640 VOICE"); break;
            case VOICE_FILIPINO_MALE:   btnVoice.setText("\uD83C\uDDF5\uD83C\uDDED \u2642 VOICE"); break;
            case VOICE_FILIPINO_FEMALE: btnVoice.setText("\uD83C\uDDF5\uD83C\uDDED \u2640 VOICE"); break;
            case VOICE_FRENCH_MALE:     btnVoice.setText("\uD83C\uDDEB\uD83C\uDDF7 \u2642 VOICE"); break;
            case VOICE_FRENCH_FEMALE:   btnVoice.setText("\uD83C\uDDEB\uD83C\uDDF7 \u2640 VOICE"); break;
            default:                    btnVoice.setText("\uD83C\uDDEC\uD83C\uDDE7 \u2642 VOICE"); break;
        }
    }

    // ── Emotion ───────────────────────────────────────────────────────────────
    private String extractEmotion(String text) {
        if (text == null) return "neutral";
        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile("\\[EMOTION:(\\w+)\\]").matcher(text);
        return m.find() ? m.group(1).toLowerCase() : "neutral";
    }

    private String stripEmotionTag(String text) {
        if (text == null) return "";
        return text.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
    }

    private String cleanForTts(String text) {
        if (text == null) return "";
        return text
            .replaceAll("\\[EMOTION:\\w+\\]", "")
            .replaceAll("```[\\s\\S]*?```", "")
            .replaceAll("`([^`]+)`", "$1")
            .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
            .replaceAll("\\*(.*?)\\*", "$1")
            .replaceAll("__(.*?)__", "$1")
            .replaceAll("_(.*?)_", "$1")
            .replaceAll("(?m)^#{1,6}\\s*", "")
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
            .replaceAll("\\[[^\\]]*\\]", "")
            .replaceAll("\\{[^}]*\\}", "")
            .replaceAll("<[^>]*>", "")
            .replaceAll("https?://\\S+", "")
            .replaceAll("[|^~`#@]", "")
            .replaceAll("(?m)^\\s*[-*+]\\s+", "")
            .replaceAll("(?m)^\\s*\\d+[.)\\s]+", "")
            .replaceAll("-{2,}", "")
            .replaceAll("[\\r\\n]+", " ")
            .replaceAll("\\s{2,}", " ")
            .trim();
    }

    // ── Speak — Edge TTS via Vercel, fallback to native ───────────────────────
    private void speak(String text) { speak(text, "neutral"); }

    private void speak(final String rawText, final String emotion) {
        if (rawText == null || rawText.trim().isEmpty()) return;
        final String clean = cleanForTts(rawText);
        if (clean.isEmpty()) return;

        isSpeaking = true;
        setState(OrbView.OrbState.SPEAKING);

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("text", clean);
                body.put("voice", currentVoice);

                RequestBody rb = RequestBody.create(
                    body.toString(),
                    MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                    .url(SPEAK_URL).post(rb)
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.code() != 204
                            && resp.body() != null) {
                        byte[] audio = resp.body().bytes();
                        if (audio.length > 0) {
                            mainHandler.post(() -> playAudioBytes(audio, clean));
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Fallback: native TTS
            mainHandler.post(() -> speakNative(clean));
        }).start();
    }

    private void playAudioBytes(byte[] audioBytes, final String fallbackText) {
        try {
            if (ttsPlayer != null) {
                try { ttsPlayer.release(); } catch (Exception ignored) {}
                ttsPlayer = null;
            }
            File tmp = File.createTempFile("tts_", ".mp3", getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write(audioBytes); }

            ttsPlayer = new MediaPlayer();
            ttsPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            ttsPlayer.setDataSource(tmp.getAbsolutePath());
            ttsPlayer.setOnCompletionListener(mp -> {
                isSpeaking = false;
                setState(OrbView.OrbState.IDLE);
                mp.release(); ttsPlayer = null; tmp.delete();
            });
            ttsPlayer.setOnErrorListener((mp, what, extra) -> {
                mp.release(); ttsPlayer = null; tmp.delete();
                speakNative(fallbackText);
                return true;
            });
            ttsPlayer.prepare();
            ttsPlayer.start();
        } catch (Exception e) {
            speakNative(fallbackText);
        }
    }

    private void speakNative(String clean) {
        if (!ttsReady || tts == null) {
            isSpeaking = false;
            setState(OrbView.OrbState.IDLE);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
        } else {
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void stopSpeaking() {
        isSpeaking = false;
        if (ttsPlayer != null) {
            try { ttsPlayer.stop(); ttsPlayer.release(); } catch (Exception ignored) {}
            ttsPlayer = null;
        }
        if (tts != null && tts.isSpeaking()) tts.stop();
        setState(OrbView.OrbState.IDLE);
    }

    // ── Attachment ────────────────────────────────────────────────────────────
    private void showAttachDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Attach image")
            .setItems(new String[]{"Take photo", "Choose from gallery"}, (d, which) -> {
                if (which == 0) openCamera(); else openGallery();
            }).show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show(); return;
        }
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File photo = File.createTempFile("HENRY_" + ts, ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            cameraImageUri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Select image"), REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;
        Uri uri = null;
        if (req == REQUEST_CAMERA && cameraImageUri != null) uri = cameraImageUri;
        else if (req == REQUEST_GALLERY && data != null)     uri = data.getData();
        if (uri != null) encodeImageAsync(uri);
    }

    private void encodeImageAsync(Uri uri) {
        new Thread(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) throw new IOException("Cannot open stream");
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp == null) throw new IOException("Cannot decode bitmap");
                int w = bmp.getWidth(), h = bmp.getHeight(), maxPx = 768;
                if (w > maxPx || h > maxPx) {
                    float s = Math.min((float) maxPx / w, (float) maxPx / h);
                    bmp = Bitmap.createScaledBitmap(bmp, (int)(w*s), (int)(h*s), true);
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 72, baos);
                String b64 = "data:image/jpeg;base64,"
                    + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                mainHandler.post(() -> {
                    pendingImageBase64 = b64;
                    pendingImageUriStr = uri.toString();
                    if (ivAttachPreview != null) {
                        ivAttachPreview.setImageURI(uri);
                        ivAttachPreview.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(this, "Image attached — tap to remove", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void clearAttachment() {
        pendingImageBase64 = null; pendingImageUriStr = null;
        if (ivAttachPreview != null) {
            ivAttachPreview.setImageDrawable(null);
            ivAttachPreview.setVisibility(View.GONE);
        }
    }

    // ── Voice Input ───────────────────────────────────────────────────────────
    private void toggleListening() {
        if (isSpeaking) { stopSpeaking(); return; }
        if (isListening) stopListening(); else startListening();
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, PERM_CODE);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_LONG).show();
            return;
        }
        if (speechRec != null) {
            try { speechRec.destroy(); } catch (Exception ignored) {}
            speechRec = null;
        }
        speechRec = SpeechRecognizer.createSpeechRecognizer(this);
        speechRec.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                isListening = true;
                mainHandler.post(() -> {
                    setState(OrbView.OrbState.LISTENING);
                    if (tvOrbHint != null) tvOrbHint.setText("LISTENING \u2014 TAP TO STOP");
                });
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float r) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() { isListening = false; }
            @Override public void onError(int error) {
                isListening = false;
                mainHandler.post(() -> {
                    setState(OrbView.OrbState.IDLE);
                    if (tvOrbHint != null) tvOrbHint.setText("WHAT CAN I DO FOR YOU, SIR?");
                    if (error != SpeechRecognizer.ERROR_NO_MATCH
                        && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
                        Toast.makeText(MainActivity.this,
                            "Voice error (" + error + ")", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onResults(Bundle results) {
                isListening = false;
                mainHandler.post(() -> {
                    if (tvOrbHint != null) tvOrbHint.setText("WHAT CAN I DO FOR YOU, SIR?");
                });
                ArrayList<String> m =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (m != null && !m.isEmpty() && !m.get(0).trim().isEmpty())
                    mainHandler.post(() -> { hideWelcome(); askJarvis(m.get(0).trim()); });
                else
                    mainHandler.post(() -> setState(OrbView.OrbState.IDLE));
            }
            @Override public void onPartialResults(Bundle p) {}
            @Override public void onEvent(int t, Bundle b) {}
        });
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRec.startListening(i);
    }

    private void stopListening() {
        isListening = false;
        if (speechRec != null) try { speechRec.stopListening(); } catch (Exception ignored) {}
        setState(OrbView.OrbState.IDLE);
        if (tvOrbHint != null) tvOrbHint.setText("WHAT CAN I DO FOR YOU, SIR?");
    }

    // ── Chat ──────────────────────────────────────────────────────────────────
    private void sendText() {
        if (etInput == null) return;
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() && pendingImageBase64 == null) return;
        if (currentState == OrbView.OrbState.THINKING) return;
        if (text.isEmpty()) text = "Analyse this image and describe what you see in detail.";
        etInput.setText("");
        hideWelcome();
        askJarvis(text);
    }

    private void askJarvis(String userText) {
        history.add(new HistoryItem("user", userText));
        addUserMsg(userText);
        if (pendingImageUriStr != null) {
            messages.add(new Message(Message.TYPE_IMAGE, null, pendingImageUriStr));
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
        }
        saveHistory();
        setState(OrbView.OrbState.THINKING);
        showTyping();
        if (btnSend != null) btnSend.setEnabled(false);
        String imageB64 = pendingImageBase64;
        clearAttachment();

        JarvisApi.ask(history, imageB64, new JarvisApi.Callback() {
            @Override public void onSuccess(String reply, String imageUrl) {
                mainHandler.post(() -> {
                    hideTyping();
                    String emotion    = extractEmotion(reply);
                    String cleanReply = stripEmotionTag(reply);
                    history.add(new HistoryItem("model", cleanReply));
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        messages.add(new Message(Message.TYPE_URL_IMAGE, cleanReply, null, imageUrl));
                        adapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                        speak("Here is your generated image, sir.", "proud");
                    } else {
                        addJarvisMsg(cleanReply);
                        speak(cleanReply, emotion);
                    }
                    saveHistory();
                    if (btnSend != null) btnSend.setEnabled(true);
                });
            }
            @Override public void onError(String error) {
                mainHandler.post(() -> {
                    hideTyping();
                    addJarvisMsg("My apologies, sir. I encountered an error: " + error);
                    if (btnSend != null) btnSend.setEnabled(true);
                    setState(OrbView.OrbState.IDLE);
                });
            }
        });
    }

    private void addUserMsg(String text) {
        messages.add(new Message(Message.TYPE_USER, text));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void addJarvisMsg(String text) {
        messages.add(new Message(Message.TYPE_JARVIS, text));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        recycler.post(() -> {
            if (scrollMain != null)
                scrollMain.post(() -> scrollMain.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void showTyping() {
        messages.add(new Message(Message.TYPE_TYPING, ""));
        typingPos = messages.size() - 1;
        adapter.notifyItemInserted(typingPos);
        scrollToBottom();
    }

    private void hideTyping() {
        if (typingPos >= 0 && typingPos < messages.size()) {
            messages.remove(typingPos);
            adapter.notifyItemRemoved(typingPos);
            typingPos = -1;
        }
    }

    // ── Memory ────────────────────────────────────────────────────────────────
    private void saveHistory() {
        List<HistoryItem> toSave = history.size() > 80
            ? history.subList(history.size() - 80, history.size()) : history;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString(KEY_HIS, gson.toJson(toSave)).apply();
    }

    private void loadHistory() {
        String json = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_HIS, null);
        if (json == null || json.isEmpty()) return;
        try {
            Type type = new TypeToken<List<HistoryItem>>(){}.getType();
            List<HistoryItem> saved = gson.fromJson(json, type);
            if (saved == null) return;
            history.addAll(saved);
            List<HistoryItem> vis = saved.size() > 20
                ? saved.subList(saved.size() - 20, saved.size()) : saved;
            for (HistoryItem item : vis)
                messages.add(new Message(
                    "user".equals(item.role) ? Message.TYPE_USER : Message.TYPE_JARVIS,
                    item.text));
            if (!messages.isEmpty()) { adapter.notifyDataSetChanged(); scrollToBottom(); }
        } catch (Exception ignored) {}
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
            .setTitle("Clear Memory")
            .setMessage("Wipe all conversation history?")
            .setPositiveButton("Clear", (d, w) -> {
                history.clear(); messages.clear();
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_HIS).apply();
                adapter.notifyDataSetChanged();
                if (orbSection != null) orbSection.setVisibility(View.VISIBLE);
                if (chipsRow1  != null) chipsRow1.setVisibility(View.VISIBLE);
                if (chipsRow2  != null) chipsRow2.setVisibility(View.VISIBLE);
                if (chipsRow3  != null) chipsRow3.setVisibility(View.VISIBLE);
            })
            .setNegativeButton("Cancel", null).show();
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private void setState(OrbView.OrbState state) {
        currentState = state;
        if (orbView  != null) orbView.setState(state);
        if (tvStatus != null) {
            final String[] labels = {"STANDBY", "LISTENING\u2026", "PROCESSING\u2026", "SPEAKING\u2026", "WAKE"};
            tvStatus.setText(labels[state.ordinal()]);
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────
    private void requestPerms() {
        List<String> needed = new ArrayList<>();
        needed.add(Manifest.permission.RECORD_AUDIO);
        needed.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            needed.add(Manifest.permission.READ_MEDIA_IMAGES);
        else
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        List<String> toReq = new ArrayList<>();
        for (String p : needed)
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                toReq.add(p);
        if (!toReq.isEmpty())
            ActivityCompat.requestPermissions(this, toReq.toArray(new String[0]), PERM_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int c, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(c, p, r);
    }

    @Override protected void onPause() {
        super.onPause();
        stopSpeaking();
        if (speechRec != null) try { speechRec.stopListening(); } catch (Exception ignored) {}
    }

    @Override protected void onDestroy() {
        stopSpeaking();
        if (speechRec != null) try { speechRec.destroy(); } catch (Exception ignored) {}
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
