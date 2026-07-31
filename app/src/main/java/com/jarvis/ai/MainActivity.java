package com.jarvis.ai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int    PERM_CODE       = 101;
    private static final int    REQUEST_GALLERY  = 200;
    private static final int    REQUEST_CAMERA   = 201;
    private static final String PREFS            = "jarvis_prefs";
    private static final String KEY_HIS          = "history_v2";
    private static final String SPEAK_URL        = "https://jarvis-ai-seven-dun.vercel.app/api/speak";

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

    private MediaPlayer  mediaPlayer;
    private TextToSpeech androidTts;
    private boolean      ttsReady   = false;
    private boolean      isSpeaking = false;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build();

    private SpeechRecognizer speechRec;
    private boolean          isListening = false;

    private OrbView.OrbState currentState = OrbView.OrbState.IDLE;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson    gson        = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
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
                speak("Good day, sir. J.A.R.V.I.S online. All systems nominal. How may I assist you?"), 1500);
        }
    }

   // ── Android native TTS init — Google neural male voice ────────────────────
private void initTts() {
    // Force Google TTS engine for neural (non-robotic) voices
    androidTts = new TextToSpeech(this, status -> {
        if (status != TextToSpeech.SUCCESS) {
            // Google engine unavailable — fall back to default engine
            androidTts = new TextToSpeech(this, s2 -> {
                if (s2 == TextToSpeech.SUCCESS) setupTtsVoice();
            });
            return;
        }
        setupTtsVoice();
    }, "com.google.android.tts");
}

private void setupTtsVoice() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        android.speech.tts.Voice bestVoice = null;
        int bestScore = -1;

        for (android.speech.tts.Voice voice : androidTts.getVoices()) {
            String name    = voice.getName().toLowerCase();
            String lang    = voice.getLocale().getLanguage();
            String country = voice.getLocale().getCountry();

            if (!lang.equals("en")) continue;

            boolean isBritish = country.equals("GB");
            boolean isMale    = name.contains("male") || name.contains("#male")
                             || name.contains("-male") || name.contains("guy")
                             || name.contains("daniel") || name.contains("james")
                             || name.contains("george") || name.contains("oliver")
                             || name.contains("brian")  || name.contains("harry")
                             || name.contains("en-gb-x-gba") || name.contains("en-gb-x-gbb");
            boolean isNeural  = name.contains("neural") || name.contains("wavenet")
                             || name.contains("enhanced") || name.contains("local")
                             || voice.getQuality() >= android.speech.tts.Voice.QUALITY_HIGH;

            int score = 0;
            if (isMale)    score += 20;
            if (isBritish) score += 10;
            if (isNeural)  score += 5;

            if (score > bestScore) {
                bestScore = score;
                bestVoice = voice;
            }
        }

        if (bestVoice != null) {
            androidTts.setVoice(bestVoice);
        } else {
            androidTts.setLanguage(new Locale("en", "GB"));
        }
    } else {
        androidTts.setLanguage(new Locale("en", "GB"));
    }

    androidTts.setPitch(0.78f);       // Lower = deeper, manlier
    androidTts.setSpeechRate(0.90f);  // Slightly slower = more authoritative
    ttsReady = true;
}
    // ── Main speak entry point ────────────────────────────────────────────────
    private void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;

        String cleaned = text
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

        if (cleaned.isEmpty()) return;

        setState(OrbView.OrbState.SPEAKING);
        isSpeaking = true;
        stopMediaPlayer();

        List<String> chunks = splitSentences(cleaned);
        tryServerTts(chunks, 0, cleaned);
    }

    private List<String> splitSentences(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() > 280 && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(sentence).append(" ");
        }
        if (current.length() > 0) chunks.add(current.toString().trim());
        if (chunks.isEmpty()) chunks.add(text);
        return chunks;
    }

    private void tryServerTts(List<String> chunks, int index, String fullText) {
        if (!isSpeaking || index >= chunks.size()) {
            isSpeaking = false;
            mainHandler.post(() -> setState(OrbView.OrbState.IDLE));
            return;
        }
        final String chunk = chunks.get(index);
        new Thread(() -> {
            try {
                JSONObject jo = new JSONObject();
                jo.put("text", chunk);
                okhttp3.RequestBody reqBody = okhttp3.RequestBody.create(
                    jo.toString().getBytes("UTF-8"),
                    okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                Request req = new Request.Builder()
                    .url(SPEAK_URL)
                    .post(reqBody)
                    .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.code() == 200 && resp.body() != null) {
                        byte[] audio = resp.body().bytes();
                        if (audio.length > 500) {
                            File tmp = File.createTempFile("jarvis_" + index + "_", ".mp3", getCacheDir());
                            java.nio.file.Files.write(tmp.toPath(), audio);
                            mainHandler.post(() -> {
                                try {
                                    stopMediaPlayer();
                                    mediaPlayer = new MediaPlayer();
                                    mediaPlayer.setDataSource(tmp.getAbsolutePath());
                                    mediaPlayer.prepare();
                                    mediaPlayer.setPlaybackParams(
                                        mediaPlayer.getPlaybackParams().setPitch(0.85f));
                                    mediaPlayer.setOnCompletionListener(mp -> {
                                        mp.release();
                                        mediaPlayer = null;
                                        tmp.delete();
                                        tryServerTts(chunks, index + 1, fullText);
                                    });
                                    mediaPlayer.setOnErrorListener((mp, w, e) -> {
                                        mp.release();
                                        mediaPlayer = null;
                                        tmp.delete();
                                        tryServerTts(chunks, index + 1, fullText);
                                        return true;
                                    });
                                    mediaPlayer.start();
                                } catch (Exception e) {
                                    mainHandler.post(() -> fallbackTts(joinFrom(chunks, index)));
                                }
                            });
                            return;
                        }
                    }
                    // 204 or any non-200 → fall back to native TTS
                }
            } catch (Exception ignored) {}

            // Server failed — use Android native TTS for all remaining text
            String remaining = joinFrom(chunks, index);
            mainHandler.post(() -> fallbackTts(remaining));
        }).start();
    }

    private String joinFrom(List<String> chunks, int fromIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = fromIndex; i < chunks.size(); i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(chunks.get(i));
        }
        return sb.toString();
    }

    // ── Android native TTS fallback — unlimited, free, always works ──────────
    private void fallbackTts(String text) {
        if (!ttsReady || androidTts == null) {
            isSpeaking = false;
            setState(OrbView.OrbState.IDLE);
            return;
        }
        androidTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_fallback");
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (androidTts != null && androidTts.isSpeaking()) {
                    mainHandler.postDelayed(this, 400);
                } else {
                    isSpeaking = false;
                    setState(OrbView.OrbState.IDLE);
                }
            }
        }, 500);
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    private void stopSpeaking() {
        isSpeaking = false;
        stopMediaPlayer();
        if (androidTts != null && androidTts.isSpeaking()) androidTts.stop();
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
        else if (req == REQUEST_GALLERY && data != null)    uri = data.getData();
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
                    "user".equals(item.role) ? Message.TYPE_USER : Message.TYPE_JARVIS,
                    item.text));
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
                history.clear();
                messages.clear();
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
        if (androidTts != null) { androidTts.stop(); androidTts.shutdown(); }
        super.onDestroy();
    }
}
