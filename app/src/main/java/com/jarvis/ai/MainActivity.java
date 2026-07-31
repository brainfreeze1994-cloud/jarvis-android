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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    private static final int    PERM_CODE       = 101;
    private static final int    REQUEST_GALLERY = 200;
    private static final int    REQUEST_CAMERA  = 201;
    private static final String PREFS           = "jarvis_prefs";
    private static final String KEY_HIS         = "history_v2";

    private OrbView      orbView;
    private TextView     tvStatus;
    private TextView     tvOrbHint;
    private RecyclerView recycler;
    private EditText     etInput;
    private ImageButton  btnMic, btnSend, btnClear, btnAttach;
    private ImageView    ivAttachPreview;

    private final List<Message>     messages = new ArrayList<>();
    private final List<HistoryItem> history  = new ArrayList<>();
    private ChatAdapter adapter;
    private int typingPos = -1;

    private Uri    cameraImageUri;
    private String pendingImageBase64;
    private String pendingImageUriStr;

    // TTS — Edge TTS (server, British Male Ryan) primary, Android native fallback
    private TextToSpeech tts;
    private boolean      ttsReady  = false;
    private boolean      isSpeaking = false;
    private MediaPlayer  ttsPlayer  = null;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build();

    private SpeechRecognizer speechRec;
    private boolean          isListening = false;

    private OrbView.OrbState currentState = OrbView.OrbState.IDLE;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson    gson        = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        recycler        = findViewById(R.id.recycler_chat);
        etInput         = findViewById(R.id.et_input);
        btnMic          = findViewById(R.id.btn_mic);
        btnSend         = findViewById(R.id.btn_send);
        btnClear        = findViewById(R.id.btn_clear);
        btnAttach       = findViewById(R.id.btn_attach);
        ivAttachPreview = findViewById(R.id.iv_attach_preview);

        adapter = new ChatAdapter(messages);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recycler.setLayoutManager(llm);
        recycler.setAdapter(adapter);

        requestPermissions();
        loadHistory();
        initTts();

        orbView.setOnClickListener(v -> toggleListening());
        btnMic.setOnClickListener(v -> toggleListening());
        btnSend.setOnClickListener(v -> sendText());
        btnClear.setOnClickListener(v -> confirmClear());
        btnAttach.setOnClickListener(v -> showAttachDialog());
        ivAttachPreview.setOnClickListener(v -> clearAttachment());
        etInput.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_SEND) { sendText(); return true; }
            return false;
        });

        if (history.isEmpty()) {
            addJarvisMsg("Good day, sir. J.A.R.V.I.S online. All systems nominal. How may I assist you?");
            mainHandler.postDelayed(() ->
                speak("Good day, sir. J.A.R.V.I.S online. All systems nominal. How may I assist you?"), 2000);
        }
    }

    // ── TTS init (Android native — fallback only) ─────────────────────────────
    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                pickBestMaleVoice();
            } else {
                tts = new TextToSpeech(this, s2 -> {
                    if (s2 == TextToSpeech.SUCCESS) pickBestMaleVoice();
                });
            }
        }, "com.google.android.tts");
    }

    private void pickBestMaleVoice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && tts.getVoices() != null) {
            android.speech.tts.Voice best = null;
            int bestScore = -1;
            for (android.speech.tts.Voice v : tts.getVoices()) {
                String name = v.getName().toLowerCase();
                if (!v.getLocale().getLanguage().equals("en")) continue;
                boolean isFemale = name.contains("female") || name.contains("woman")
                    || name.contains("girl") || name.contains("zira") || name.contains("hazel")
                    || name.contains("susan") || name.contains("kate") || name.contains("en-gb-x-gbc")
                    || name.contains("en-gb-x-gbd") || name.contains("samantha");
                if (isFemale) continue;
                boolean isBritish = v.getLocale().getCountry().equals("GB");
                boolean isMale = name.contains("male") || name.contains("daniel")
                    || name.contains("george") || name.contains("oliver")
                    || name.contains("harry") || name.contains("james")
                    || name.contains("en-gb-x-gba") || name.contains("en-gb-x-gbb");
                boolean isNeural = name.contains("neural") || name.contains("wavenet")
                    || name.contains("enhanced") || name.contains("local")
                    || v.getQuality() >= android.speech.tts.Voice.QUALITY_HIGH;
                int score = 0;
                if (isMale)    score += 30;
                if (isBritish) score += 15;
                if (isNeural)  score += 10;
                if (score > bestScore) { bestScore = score; best = v; }
            }
            if (best != null) tts.setVoice(best);
            else tts.setLanguage(new Locale("en", "GB"));
        } else {
            tts.setLanguage(new Locale("en", "GB"));
        }
        tts.setPitch(0.75f);
        tts.setSpeechRate(0.88f);
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
        ttsReady = true;
    }

    // ── Clean markdown for speech ─────────────────────────────────────────────
    private String cleanForTts(String text) {
        return text
            .replaceAll("```[\\s\\S]*?```", "code block.")
            .replaceAll("`([^`]+)`", "$1")
            .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
            .replaceAll("\\*(.*?)\\*", "$1")
            .replaceAll("#{1,6}\\s", "")
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
            .replaceAll("(?m)^\\s*[-*+]\\s", " ")
            .replaceAll("(?m)^\\s*\\d+\\.\\s", " ")
            .replaceAll("\\n+", " ")
            .trim();
    }

    // ── Speak — Edge TTS server first, Android native fallback ────────────────
    private void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String clean = cleanForTts(text);
        if (clean.isEmpty()) return;
        isSpeaking = true;
        setState(OrbView.OrbState.SPEAKING);

        new Thread(() -> {
            try {
                org.json.JSONObject body = new org.json.JSONObject();
                body.put("text", clean);
                okhttp3.RequestBody reqBody = okhttp3.RequestBody.create(
                    body.toString(), okhttp3.MediaType.parse("application/json"));
                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://jarvis-ai-seven-dun.vercel.app/api/speak")
                    .post(reqBody).build();
                try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                    if (response.code() == 200 && response.body() != null) {
                        byte[] audioBytes = response.body().bytes();
                        mainHandler.post(() -> playAudioBytes(audioBytes));
                        return;
                    }
                }
            } catch (Exception e) {
                android.util.Log.w("JARVIS_TTS", "Edge TTS failed: " + e.getMessage());
            }
            // Fallback to Android native TTS
            mainHandler.post(() -> speakNative(clean));
        }).start();
    }

    private void playAudioBytes(byte[] audioBytes) {
        try {
            if (ttsPlayer != null) { try { ttsPlayer.release(); } catch (Exception ignored) {} }
            java.io.File tmpFile = java.io.File.createTempFile("tts_", ".mp3", getCacheDir());
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpFile)) {
                fos.write(audioBytes);
            }
            ttsPlayer = new MediaPlayer();
            ttsPlayer.setDataSource(tmpFile.getAbsolutePath());
            ttsPlayer.setOnCompletionListener(mp -> {
                isSpeaking = false;
                setState(OrbView.OrbState.IDLE);
                mp.release();
                ttsPlayer = null;
                tmpFile.delete();
            });
            ttsPlayer.setOnErrorListener((mp, what, extra) -> {
                mp.release(); ttsPlayer = null; tmpFile.delete();
                speakNative(clean);
                return true;
            });
            ttsPlayer.prepare();
            ttsPlayer.start();
        } catch (Exception e) {
            android.util.Log.e("JARVIS_TTS", "Playback error: " + e.getMessage());
            speakNative(clean);
        }
    }

    private void speakNative(String clean) {
        if (!ttsReady || tts == null) return;
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
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File photo = File.createTempFile("JARVIS_" + ts, ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photo);
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
                int w = bmp.getWidth(), h = bmp.getHeight(), maxPx = 1024;
                if (w > maxPx || h > maxPx) {
                    float s = Math.min((float) maxPx / w, (float) maxPx / h);
                    bmp = Bitmap.createScaledBitmap(bmp, (int)(w * s), (int)(h * s), true);
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String b64 = "data:image/jpeg;base64,"
                    + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                mainHandler.post(() -> {
                    pendingImageBase64 = b64;
                    pendingImageUriStr = uri.toString();
                    ivAttachPreview.setImageURI(uri);
                    ivAttachPreview.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Image attached — tap to remove", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void clearAttachment() {
        pendingImageBase64 = null;
        pendingImageUriStr = null;
        ivAttachPreview.setImageDrawable(null);
        ivAttachPreview.setVisibility(View.GONE);
    }

    // ── Speech recognition ────────────────────────────────────────────────────
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
        if (speechRec != null) { try { speechRec.destroy(); } catch (Exception ignored) {} speechRec = null; }
        speechRec = SpeechRecognizer.createSpeechRecognizer(this);
        speechRec.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                isListening = true;
                mainHandler.post(() -> {
                    setState(OrbView.OrbState.LISTENING);
                    tvOrbHint.setText("LISTENING — TAP TO STOP");
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
                    tvOrbHint.setText("TAP TO SPEAK");
                    if (error != SpeechRecognizer.ERROR_NO_MATCH
                        && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
                        Toast.makeText(MainActivity.this,
                            "Voice error (" + error + ")", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onResults(Bundle results) {
                isListening = false;
                mainHandler.post(() -> tvOrbHint.setText("TAP TO SPEAK"));
                ArrayList<String> m = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (m != null && !m.isEmpty() && !m.get(0).trim().isEmpty())
                    mainHandler.post(() -> askJarvis(m.get(0).trim()));
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
        tvOrbHint.setText("TAP TO SPEAK");
    }

    // ── Chat ──────────────────────────────────────────────────────────────────
    private void sendText() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() && pendingImageBase64 == null) return;
        if (currentState == OrbView.OrbState.THINKING) return;
        if (text.isEmpty()) text = "Analyse this image and describe what you see in detail.";
        etInput.setText("");
        askJarvis(text);
    }

    private void askJarvis(String userText) {
        history.add(new HistoryItem("user", userText));
        addUserMsg(userText);
        if (pendingImageUriStr != null) {
            messages.add(new Message(Message.TYPE_IMAGE, null, pendingImageUriStr));
            adapter.notifyItemInserted(messages.size() - 1);
            recycler.scrollToPosition(messages.size() - 1);
        }
        saveHistory();
        setState(OrbView.OrbState.THINKING);
        showTyping();
        btnSend.setEnabled(false);
        String imageB64 = pendingImageBase64;
        clearAttachment();

        JarvisApi.ask(history, imageB64, new JarvisApi.Callback() {
            @Override public void onSuccess(String reply, String imageUrl) {
                mainHandler.post(() -> {
                    hideTyping();
                    history.add(new HistoryItem("model", reply));
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        messages.add(new Message(Message.TYPE_URL_IMAGE, reply, null, imageUrl));
                        adapter.notifyItemInserted(messages.size() - 1);
                        recycler.scrollToPosition(messages.size() - 1);
                        speak("Here is your generated image, sir.");
                    } else {
                        addJarvisMsg(reply);
                        speak(reply);
                    }
                    saveHistory();
                    btnSend.setEnabled(true);
                });
            }
            @Override public void onError(String error) {
                mainHandler.post(() -> {
                    hideTyping();
                    addJarvisMsg("My apologies, sir. I encountered an error: " + error);
                    btnSend.setEnabled(true);
                    setState(OrbView.OrbState.IDLE);
                });
            }
        });
    }

    private void addUserMsg(String text) {
        messages.add(new Message(Message.TYPE_USER, text));
        adapter.notifyItemInserted(messages.size() - 1);
        recycler.scrollToPosition(messages.size() - 1);
    }

    private void addJarvisMsg(String text) {
        messages.add(new Message(Message.TYPE_JARVIS, text));
        adapter.notifyItemInserted(messages.size() - 1);
        recycler.scrollToPosition(messages.size() - 1);
    }

    private void showTyping() {
        messages.add(new Message(Message.TYPE_TYPING, ""));
        typingPos = messages.size() - 1;
        adapter.notifyItemInserted(typingPos);
        recycler.scrollToPosition(typingPos);
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
                    "user".equals(item.role) ? Message.TYPE_USER : Message.TYPE_JARVIS, item.text));
            if (!messages.isEmpty()) {
                adapter.notifyDataSetChanged();
                recycler.scrollToPosition(messages.size() - 1);
            }
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
                addJarvisMsg("Memory wiped, sir. Starting fresh.");
            })
            .setNegativeButton("Cancel", null).show();
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private void setState(OrbView.OrbState state) {
        currentState = state;
        orbView.setState(state);
        final String[] labels = {"STANDBY", "LISTENING…", "PROCESSING…", "SPEAKING…", "WAKE"};
        tvStatus.setText(labels[state.ordinal()]);
    }

    // ── Permissions ───────────────────────────────────────────────────────────
    private void requestPermissions() {
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
