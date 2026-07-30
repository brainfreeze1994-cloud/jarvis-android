package com.jarvis.ai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int    PERM_CODE       = 101;
    private static final int    REQUEST_GALLERY  = 200;
    private static final int    REQUEST_CAMERA   = 201;
    private static final String PREFS            = "jarvis_prefs";
    private static final String KEY_HIS          = "history_v2";
   // StreamElements TTS — British Male Brian, free, no API key
    

    // ── Views ────────────────────────────────────────────────────────────────
    private OrbView      orbView;
    private TextView     tvStatus;
    private TextView     tvOrbHint;
    private RecyclerView recycler;
    private EditText     etInput;
    private ImageButton  btnMic;
    private ImageButton  btnSend;
    private ImageButton  btnClear;
    private ImageButton  btnAttach;
    private ImageView    ivAttachPreview;

    // ── Chat data ────────────────────────────────────────────────────────────
    private final List<Message>     messages = new ArrayList<>();
    private final List<HistoryItem> history  = new ArrayList<>();
    private ChatAdapter adapter;
    private int typingPos = -1;

    // ── Attachment ───────────────────────────────────────────────────────────
    private Uri    cameraImageUri;
    private String pendingImageBase64;
    private String pendingImageUriStr;

    // ── TTS ──────────────────────────────────────────────────────────────────
    private MediaPlayer   mediaPlayer;
    private boolean       isSpeaking = false;
    private TextToSpeech  androidTts;
    private boolean       ttsReady   = false;

    // ── OkHttp (shared) ──────────────────────────────────────────────────────
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    // ── Speech Recognition ───────────────────────────────────────────────────
    private SpeechRecognizer speechRec;
    private boolean          isListening = false;

    // ── State ────────────────────────────────────────────────────────────────
    private OrbView.OrbState currentState = OrbView.OrbState.IDLE;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson    gson        = new Gson();

    // ════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN        |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

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
        initAndroidTts();

        orbView.setOnClickListener(v -> toggleListening());
        btnMic.setOnClickListener(v -> toggleListening());
        btnSend.setOnClickListener(v -> sendText());
        btnClear.setOnClickListener(v -> confirmClear());
        btnAttach.setOnClickListener(v -> showAttachDialog());
        ivAttachPreview.setOnClickListener(v -> clearAttachment());
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendText(); return true; }
            return false;
        });

        if (history.isEmpty()) {
            addJarvisMsg("Good day, sir. J.A.R.V.I.S online. All systems nominal. How may I assist you?");
            speak("Good day, sir. J.A.R.V.I.S online. All systems nominal. How may I assist you?");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Attachment
    // ════════════════════════════════════════════════════════════════════════
    private void showAttachDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Attach image")
            .setItems(new String[]{"Take photo", "Choose from gallery"}, (d, which) -> {
                if (which == 0) openCamera();
                else            openGallery();
            })
            .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String ts    = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File   dir   = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File   photo = File.createTempFile("JARVIS_" + ts, ".jpg", dir);
            cameraImageUri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select image"), REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        Uri imageUri = null;
        if      (requestCode == REQUEST_CAMERA  && cameraImageUri != null) imageUri = cameraImageUri;
        else if (requestCode == REQUEST_GALLERY && data != null)            imageUri = data.getData();
        if (imageUri != null) encodeImageAsync(imageUri);
    }

    private void encodeImageAsync(Uri uri) {
        final String uriStr = uri.toString();
        new Thread(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) throw new IOException("Cannot open image stream");

                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp == null) throw new IOException("Cannot decode bitmap");

                int w = bmp.getWidth(), h = bmp.getHeight();
                int maxPx = 1024;
                if (w > maxPx || h > maxPx) {
                    float scale = Math.min((float) maxPx / w, (float) maxPx / h);
                    bmp = Bitmap.createScaledBitmap(bmp, (int)(w * scale), (int)(h * scale), true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] bytes = baos.toByteArray();

                String b64 = "data:image/jpeg;base64,"
                    + Base64.encodeToString(bytes, Base64.NO_WRAP);

                mainHandler.post(() -> {
                    pendingImageBase64 = b64;
                    pendingImageUriStr = uriStr;
                    ivAttachPreview.setImageURI(uri);
                    ivAttachPreview.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Image attached — tap to remove", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                    Toast.makeText(this, "Failed to attach image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void clearAttachment() {
        pendingImageBase64 = null;
        pendingImageUriStr = null;
        ivAttachPreview.setImageDrawable(null);
        ivAttachPreview.setVisibility(View.GONE);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Android native TTS — British male voice, deep pitch
    // ════════════════════════════════════════════════════════════════════════
    private void initAndroidTts() {
        androidTts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                androidTts.setPitch(0.60f);
                androidTts.setSpeechRate(0.88f);

                java.util.Set<Voice> voices = androidTts.getVoices();
                Voice best = null;
                if (voices != null) {
                    // Pass 1: named male en-GB
                    for (Voice v : voices) {
                        String n = v.getName().toLowerCase();
                        if ((n.contains("en-gb") || n.contains("en_gb")) &&
                            (n.contains("male") || n.contains("daniel") || n.contains("george") ||
                             n.contains("oliver") || n.contains("arthur") || n.contains("james"))) {
                            best = v; break;
                        }
                    }
                    // Pass 2: any en-GB
                    if (best == null) {
                        for (Voice v : voices) {
                            String n = v.getName().toLowerCase();
                            if (n.contains("en-gb") || n.contains("en_gb")) {
                                best = v; break;
                            }
                        }
                    }
                    // Pass 3: any male en-US
                    if (best == null) {
                        for (Voice v : voices) {
                            String n = v.getName().toLowerCase();
                            if ((n.contains("en-us") || n.contains("en_us")) && n.contains("male")) {
                                best = v; break;
                            }
                        }
                    }
                }
                if (best != null) {
                    androidTts.setVoice(best);
                } else {
                    androidTts.setLanguage(new java.util.Locale("en", "GB"));
                }
                ttsReady = true;
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Speak — Kokoro first, Android TTS fallback
    // ════════════════════════════════════════════════════════════════════════
    private void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;

        String plain = text
            .replaceAll("```[\\s\\S]*?```",           "code block.")
            .replaceAll("`([^`]+)`",                   "$1")
            .replaceAll("\\*\\*(.*?)\\*\\*",           "$1")
            .replaceAll("\\*(.*?)\\*",                 "$1")
            .replaceAll("#{1,6}\\s",                   "")
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)",  "$1")
            .replaceAll("(?m)^\\s*[-*+]\\s",           "")
            .replaceAll("(?m)^\\s*\\d+\\.\\s",         "")
            .replaceAll("\\n{3,}",                     "\n\n")
            .trim();

        if (plain.isEmpty()) return;

        setState(OrbView.OrbState.SPEAKING);
        isSpeaking = true;
        stopSpeaking();

        final String finalText = plain;
        new Thread(() -> {
            boolean kokoroOk = false;
            try {
                String encodedText = java.net.URLEncoder.encode(finalText, "UTF-8");
                    Request req = new Request.Builder()
                    .url("https://api.streamelements.com/kappa/v2/speech?voice=Brian&text=" + encodedText)
                    .get()
                   .build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        byte[] audio = resp.body().bytes();
                        if (audio.length > 100) {
                            String ct  = resp.header("Content-Type", "audio/wav");
                            String ext = (ct != null && (ct.contains("mp3") || ct.contains("mpeg"))) ? ".mp3" : ".wav";
                            File   tmp = File.createTempFile("jarvis_tts_", ext, getCacheDir());
                            java.nio.file.Files.write(tmp.toPath(), audio);

                            mainHandler.post(() -> {
                                try {
                                    mediaPlayer = new MediaPlayer();
                                    mediaPlayer.setDataSource(tmp.getAbsolutePath());
                                    mediaPlayer.prepare();
                                    mediaPlayer.setOnCompletionListener(mp -> {
                                        isSpeaking = false;
                                        setState(OrbView.OrbState.IDLE);
                                        mp.release();
                                        mediaPlayer = null;
                                        tmp.delete();
                                    });
                                    mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                                        isSpeaking = false;
                                        setState(OrbView.OrbState.IDLE);
                                        mp.release();
                                        mediaPlayer = null;
                                        tmp.delete();
                                        return true;
                                    });
                                    mediaPlayer.start();
                                } catch (Exception e) {
                                    speakWithAndroidTts(finalText);
                                }
                            });
                            kokoroOk = true;
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (!kokoroOk) {
                mainHandler.post(() -> speakWithAndroidTts(finalText));
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Android TTS fallback
    // ════════════════════════════════════════════════════════════════════════
    private void speakWithAndroidTts(String text) {
        if (androidTts == null || !ttsReady) {
            isSpeaking = false;
            setState(OrbView.OrbState.IDLE);
            return;
        }
        setState(OrbView.OrbState.SPEAKING);
        isSpeaking = true;
        androidTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utt");
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (androidTts != null && androidTts.isSpeaking()) {
                    mainHandler.postDelayed(this, 300);
                } else {
                    isSpeaking = false;
                    setState(OrbView.OrbState.IDLE);
                }
            }
        }, 300);
    }

    private void stopSpeaking() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (androidTts != null && androidTts.isSpeaking()) {
            androidTts.stop();
        }
        isSpeaking = false;
        setState(OrbView.OrbState.IDLE);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Speech Recognition
    // ════════════════════════════════════════════════════════════════════════
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
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show();
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
                        && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        Toast.makeText(MainActivity.this,
                            "Voice error (" + error + ") — tap to try again",
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onResults(Bundle results) {
                isListening = false;
                mainHandler.post(() -> tvOrbHint.setText("TAP TO SPEAK"));
                ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0).trim();
                    if (!text.isEmpty()) mainHandler.post(() -> askJarvis(text));
                } else {
                    mainHandler.post(() -> setState(OrbView.OrbState.IDLE));
                }
            }
            @Override public void onPartialResults(Bundle partial) {}
            @Override public void onEvent(int t, Bundle b) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,        Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,  false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,      1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        speechRec.startListening(intent);
    }

    private void stopListening() {
        isListening = false;
        if (speechRec != null) {
            try { speechRec.stopListening(); } catch (Exception ignored) {}
        }
        setState(OrbView.OrbState.IDLE);
        tvOrbHint.setText("TAP TO SPEAK");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Chat
    // ════════════════════════════════════════════════════════════════════════
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
            @Override
            public void onSuccess(String reply, String imageUrl) {
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
            @Override
            public void onError(String error) {
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

    // ════════════════════════════════════════════════════════════════════════
    //  Memory
    // ════════════════════════════════════════════════════════════════════════
    private void saveHistory() {
        List<HistoryItem> toSave = history.size() > 80
            ? history.subList(history.size() - 80, history.size()) : history;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString(KEY_HIS, gson.toJson(toSave))
            .apply();
    }

    private void loadHistory() {
        String json = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_HIS, null);
        if (json == null || json.isEmpty()) return;
        try {
            Type type = new TypeToken<List<HistoryItem>>(){}.getType();
            List<HistoryItem> saved = gson.fromJson(json, type);
            if (saved == null) return;
            history.addAll(saved);
            List<HistoryItem> visible = saved.size() > 20
                ? saved.subList(saved.size() - 20, saved.size()) : saved;
            for (HistoryItem item : visible) {
                int t = "user".equals(item.role) ? Message.TYPE_USER : Message.TYPE_JARVIS;
                messages.add(new Message(t, item.text));
            }
            if (!messages.isEmpty()) {
                adapter.notifyDataSetChanged();
                recycler.scrollToPosition(messages.size() - 1);
            }
        } catch (Exception ignored) {}
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
            .setTitle("Clear Memory")
            .setMessage("Wipe all conversation history and start fresh?")
            .setPositiveButton("Clear", (d, w) -> {
                history.clear();
                messages.clear();
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_HIS).apply();
                adapter.notifyDataSetChanged();
                addJarvisMsg("Memory wiped, sir. Starting fresh.");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  State
    // ════════════════════════════════════════════════════════════════════════
    private void setState(OrbView.OrbState state) {
        currentState = state;
        orbView.setState(state);
        final String[] labels = {"STANDBY", "LISTENING…", "PROCESSING…", "SPEAKING…", "WAKE"};
        tvStatus.setText(labels[state.ordinal()]);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Permissions
    // ════════════════════════════════════════════════════════════════════════
    private void requestPermissions() {
        List<String> needed = new ArrayList<>();
        needed.add(Manifest.permission.RECORD_AUDIO);
        needed.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        List<String> toReq = new ArrayList<>();
        for (String p : needed)
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                toReq.add(p);
        if (!toReq.isEmpty())
            ActivityCompat.requestPermissions(this, toReq.toArray(new String[0]), PERM_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Overrides
    // ════════════════════════════════════════════════════════════════════════
    @Override protected void onPause() {
        super.onPause();
        stopSpeaking();
        if (speechRec != null) {
            try { speechRec.stopListening(); } catch (Exception ignored) {}
        }
    }

    @Override protected void onDestroy() {
        stopSpeaking();
        if (speechRec != null) {
            try { speechRec.destroy(); } catch (Exception ignored) {}
        }
        if (androidTts != null) {
            androidTts.stop();
            androidTts.shutdown();
        }
        super.onDestroy();
    }
}
