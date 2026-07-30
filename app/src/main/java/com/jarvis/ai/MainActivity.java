package com.jarvis.ai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.speech.tts.Voice;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int    PERM_CODE      = 101;
    private static final int    REQUEST_GALLERY = 200;
    private static final int    REQUEST_CAMERA  = 201;
    private static final String PREFS           = "jarvis_prefs";
    private static final String KEY_HIS         = "history_v2";

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

    // ── TTS ──────────────────────────────────────────────────────────────────
    private TextToSpeech tts;
    private boolean      ttsReady = false;

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

        tts = new TextToSpeech(this, this);

        requestPermissions();
        loadHistory();

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
            String ts   = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File   dir  = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
        if (requestCode == REQUEST_CAMERA) {
            imageUri = cameraImageUri;
        } else if (requestCode == REQUEST_GALLERY && data != null) {
            imageUri = data.getData();
        }
        if (imageUri != null) encodeImageAsync(imageUri);
    }

    private void encodeImageAsync(Uri uri) {
        new Thread(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) throw new IOException("null stream");
                byte[] bytes = is.readAllBytes();
                if (bytes.length > 3_000_000) {
                    mainHandler.post(() ->
                        Toast.makeText(this, "Image too large (max ~3 MB)", Toast.LENGTH_SHORT).show());
                    return;
                }
                String b64 = "data:image/jpeg;base64,"
                    + Base64.encodeToString(bytes, Base64.NO_WRAP);
                mainHandler.post(() -> {
                    pendingImageBase64 = b64;
                    ivAttachPreview.setImageURI(uri);
                    ivAttachPreview.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Image attached — tap to remove", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                    Toast.makeText(this, "Failed to attach image", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void clearAttachment() {
        pendingImageBase64 = null;
        ivAttachPreview.setImageDrawable(null);
        ivAttachPreview.setVisibility(View.GONE);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TTS
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) return;

        int res = tts.setLanguage(Locale.UK);
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED)
            tts.setLanguage(Locale.US);

        Set<Voice> voices = tts.getVoices();
        Voice best = null;
        if (voices != null) {
            for (Voice v : voices) {
                String n = v.getName().toLowerCase();
                String l = v.getLocale().getLanguage() + "-" + v.getLocale().getCountry();
                boolean brit = l.equalsIgnoreCase("en-GB") || n.contains("en-gb");
                boolean male = n.contains("male") || n.contains("daniel") ||
                               n.contains("james") || n.contains("george") ||
                               !n.contains("female");
                if (brit && male && !v.isNetworkConnectionRequired()) { best = v; break; }
            }
            if (best == null) for (Voice v : voices) {
                String l = v.getLocale().getLanguage() + "-" + v.getLocale().getCountry();
                if (l.equalsIgnoreCase("en-GB") && !v.isNetworkConnectionRequired()) { best = v; break; }
            }
            if (best == null) for (Voice v : voices) {
                if (v.getLocale().getLanguage().equals("en") && !v.isNetworkConnectionRequired()) { best = v; break; }
            }
        }
        if (best != null) tts.setVoice(best);

        tts.setSpeechRate(0.90f);
        tts.setPitch(0.75f);

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) {
                mainHandler.post(() -> setState(OrbView.OrbState.IDLE));
            }
            @Override public void onError(String id) {
                mainHandler.post(() -> setState(OrbView.OrbState.IDLE));
            }
        });

        ttsReady = true;
    }

    private void speak(String text) {
        if (!ttsReady || tts == null) return;
        String plain = text
            .replaceAll("```[\\s\\S]*?```",           "code block.")
            .replaceAll("`([^`]+)`",                   "$1")
            .replaceAll("\\*\\*(.*?)\\*\\*",           "$1")
            .replaceAll("\\*(.*?)\\*",                 "$1")
            .replaceAll("#{1,6}\\s",                   "")
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)",  "$1")
            .replaceAll("(?m)^\\s*[-*+]\\s",           "")
            .replaceAll("(?m)^\\s*\\d+\\.\\s",         "")
            .trim();
        if (plain.length() > 800) plain = plain.substring(0, 800);
        setState(OrbView.OrbState.SPEAKING);
        tts.stop();
        tts.speak(plain, TextToSpeech.QUEUE_FLUSH, null, "JARVIS");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Speech Recognition
    // ════════════════════════════════════════════════════════════════════════
    private void toggleListening() {
        if (isListening) stopListening(); else startListening();
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tts != null) tts.stop();
        if (speechRec != null) speechRec.destroy();
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
                    if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                        error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        Toast.makeText(MainActivity.this, "Voice error — try again", Toast.LENGTH_SHORT).show();
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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,        "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,  false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,      1);
        speechRec.startListening(intent);
    }

    private void stopListening() {
        isListening = false;
        if (speechRec != null) speechRec.stopListening();
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
        if (text.isEmpty()) text = "Analyse this image and describe what you see.";
        etInput.setText("");
        askJarvis(text);
    }

    private void askJarvis(String userText) {
        history.add(new HistoryItem("user", userText));
        addUserMsg(userText);
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
                    String display = (imageUrl != null && !imageUrl.isEmpty())
                        ? reply + "\n\n[Image generated — open in browser to view]"
                        : reply;
                    addJarvisMsg(display);
                    saveHistory();
                    btnSend.setEnabled(true);
                    speak(reply);
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
        if (tts != null) tts.stop();
        if (speechRec != null) speechRec.stopListening();
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRec != null) { speechRec.destroy(); }
        super.onDestroy();
    }
}
