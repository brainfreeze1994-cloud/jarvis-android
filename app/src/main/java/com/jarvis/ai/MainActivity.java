package com.jarvis.ai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.provider.CalendarContract;
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
    private static final int    PERM_CALL        = 102;
    private static final int    REQUEST_GALLERY  = 200;
    private static final int    REQUEST_CAMERA   = 201;
    private static final int    REQUEST_PDF      = 202;
    private static final String PREFS            = "jarvis_prefs";
    private static final String KEY_HIS          = "history_v2";
    private static final String KEY_VOICE        = "voice_choice";
    private static final String KEY_WAKE         = "wake_enabled";
    private static final String KEY_MUTE         = "tts_muted";
    private static final String KEY_SPEED        = "tts_speed";
    private static final String KEY_PERSONA      = "persona_mode";
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
    private String currentVoice   = VOICE_AMERICAN_MALE;

    // Persona modes
    private static final String PERSONA_FLIRTY       = "flirty";
    private static final String PERSONA_PROFESSIONAL = "professional";
    private static final String PERSONA_CASUAL       = "casual";
    private static final String PERSONA_TACTICAL     = "tactical";
    private String currentPersona = PERSONA_FLIRTY;

    // TTS speed (0=slow, 1=normal, 2=fast)
    private int     ttsSpeed    = 1;
    private boolean ttsMuted    = false;
    private boolean wakeEnabled = false;

    // UI
    private OrbView      orbView;
    private TextView     tvStatus, tvOrbHint, btnVoice;
    private RecyclerView recycler;
    private EditText     etInput;
    private ImageButton  btnMic, btnSend, btnClear, btnAttach;
    private ImageView    ivAttachPreview;
    private LinearLayout orbSection, chipsRow1, chipsRow2, chipsRow3;
    private NestedScrollView scrollMain;

    private final List<Message>     messages = new ArrayList<>();
    private final List<HistoryItem> history  = new ArrayList<>();
    private ChatAdapter adapter;
    private int typingPos = -1;

    private Uri    cameraImageUri;
    private String pendingImageBase64;
    private String pendingImageUriStr;
    private String pendingPdfText;      // PDF text waiting to be sent

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

    private String pendingCallAction = null;

    private BroadcastReceiver wakeReceiver;
    private BroadcastReceiver notifReceiver;

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
            new AlertDialog.Builder(this)
                .setTitle("HENRY Startup Error")
                .setMessage(android.util.Log.getStackTraceString(t))
                .setPositiveButton("OK", null).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null) return;
        if (WakeWordService.ACTION_WAKE_WORD.equals(intent.getAction())) {
            handleWakeWord();
        }
        // Widget mic button: open app and immediately start listening
        if (intent.getBooleanExtra("start_listening", false)) {
            mainHandler.postDelayed(this::startListening, 600);
        }
    }

    // ── startApp ──────────────────────────────────────────────────────────────
    private void startApp() {
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS).build();

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

        // Load prefs
        currentVoice   = getPrefs().getString(KEY_VOICE,   VOICE_AMERICAN_MALE);
        currentPersona = getPrefs().getString(KEY_PERSONA, PERSONA_FLIRTY);
        wakeEnabled    = getPrefs().getBoolean(KEY_WAKE,   false);
        ttsMuted       = getPrefs().getBoolean(KEY_MUTE,   false);
        ttsSpeed       = getPrefs().getInt(KEY_SPEED,      1);

        requestPerms();
        loadHistory();
        initNativeTts();
        updateVoiceButtonLabel();
        registerReceivers();

        if (wakeEnabled) startWakeService();

        // Button listeners
        if (orbView   != null) orbView.setOnClickListener(v -> toggleListening());
        if (btnMic    != null) btnMic.setOnClickListener(v -> toggleListening());
        if (btnSend   != null) btnSend.setOnClickListener(v -> sendText());
        if (btnClear  != null) btnClear.setOnClickListener(v -> showClearMenu());
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
            addJarvisMsg("Good day, sir. H.E.N.R.Y online. All systems nominal.");
            mainHandler.postDelayed(() ->
                speak("Good day, sir. H.E.N.R.Y online. All systems nominal.", "warm"), 1500);
        } else {
            hideWelcome();
        }

        // If launched from widget mic button
        if (getIntent() != null && getIntent().getBooleanExtra("start_listening", false)) {
            mainHandler.postDelayed(this::startListening, 800);
        }
    }

    private android.content.SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
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

    // ── Wake Word ─────────────────────────────────────────────────────────────
    private void startWakeService() {
        Intent i = new Intent(this, WakeWordService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(i);
        else
            startService(i);
    }

    private void stopWakeService() {
        Intent i = new Intent(this, WakeWordService.class);
        i.setAction(WakeWordService.ACTION_STOP);
        startService(i);
    }

    private void handleWakeWord() {
        if (currentState == OrbView.OrbState.THINKING ||
            currentState == OrbView.OrbState.LISTENING) return;
        if (isSpeaking) stopSpeaking();
        mainHandler.postDelayed(this::startListening, 300);
    }

    // ── Broadcast Receivers ───────────────────────────────────────────────────
    private void registerReceivers() {
        wakeReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) { handleWakeWord(); }
        };
        IntentFilter wf = new IntentFilter(WakeWordService.ACTION_WAKE_WORD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(wakeReceiver, wf, Context.RECEIVER_NOT_EXPORTED);
        else
            registerReceiver(wakeReceiver, wf);

        notifReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                String app  = intent.getStringExtra(NotificationService.EXTRA_APP);
                String text = intent.getStringExtra(NotificationService.EXTRA_TEXT);
                if (text == null || text.isEmpty()) return;
                String announce = "Notification from " + app + ": " + text;
                addJarvisMsg("[Notification] **" + app + ":** " + text);
                if (!ttsMuted) speak(announce, "neutral");
            }
        };
        IntentFilter nf = new IntentFilter(NotificationService.ACTION_NOTIFY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(notifReceiver, nf, Context.RECEIVER_NOT_EXPORTED);
        else
            registerReceiver(notifReceiver, nf);
    }

    // ── Native TTS ────────────────────────────────────────────────────────────
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
        if (isFrench) tts.setLanguage(Locale.FRENCH);
        else           tts.setLanguage(new Locale("en", "US"));
        tts.setPitch(isMale ? 0.75f : 1.05f);
        float[] rates = { 0.75f, 0.90f, 1.10f };
        tts.setSpeechRate(rates[Math.max(0, Math.min(2, ttsSpeed))]);
    }

    // ── Voice Picker ──────────────────────────────────────────────────────────
    private void showVoicePicker() {
        CharSequence[] options = {
            "🎙 Voice Accent",
            wakeEnabled ? "🟢 Wake Word: ON  (tap to disable)" : "⚫ Wake Word: OFF  (tap to enable)",
            ttsMuted    ? "🔇 Voice Muted  (tap to unmute)"   : "🔊 Voice Enabled  (tap to mute)",
            "⚡ Voice Speed: " + new String[]{"Slow","Normal","Fast"}[ttsSpeed],
            "🧠 Persona: " + capitalize(currentPersona),
            "📋 My Reminders",
            "📤 Export Chat",
            "🌐 Translate Last Reply"
        };
        new AlertDialog.Builder(this)
            .setTitle("◆ H.E.N.R.Y Settings")
            .setItems(options, (d, which) -> {
                switch (which) {
                    case 0: showVoiceAccentPicker(); break;
                    case 1: toggleWakeWord(); break;
                    case 2: toggleMute(); break;
                    case 3: showSpeedPicker(); break;
                    case 4: showPersonaPicker(); break;
                    case 5: showReminders(); break;
                    case 6: exportChat(); break;
                    case 7: translateLastReply(); break;
                }
            }).show();
    }

    private void showVoiceAccentPicker() {
        final String[] labels = {
            "🇬🇧 British Male ♂ (Ryan·Neural)",
            "🇬🇧 British Female ♀ (Sonia·Neural)",
            "🇺🇸 American Male ♂ (Guy·Neural)",
            "🇺🇸 American Female ♀ (Aria·Neural)",
            "🇵🇭 Filipino Male ♂ (Angelo·Neural)",
            "🇵🇭 Filipino Female ♀ (Blessica·Neural)",
            "🇫🇷 French Male ♂ (Henri·Neural)",
            "🇫🇷 French Female ♀ (Denise·Neural)"
        };
        final String[] voices = {
            VOICE_BRITISH_MALE, VOICE_BRITISH_FEMALE,
            VOICE_AMERICAN_MALE, VOICE_AMERICAN_FEMALE,
            VOICE_FILIPINO_MALE, VOICE_FILIPINO_FEMALE,
            VOICE_FRENCH_MALE, VOICE_FRENCH_FEMALE
        };
        int cur = 0;
        for (int i = 0; i < voices.length; i++) if (voices[i].equals(currentVoice)) { cur = i; break; }
        final int[] sel = { cur };
        new AlertDialog.Builder(this)
            .setTitle("◆ Voice Accent (Microsoft Neural)")
            .setSingleChoiceItems(labels, cur, (d, w) -> sel[0] = w)
            .setPositiveButton("Apply", (d, w) -> {
                currentVoice = voices[sel[0]];
                getPrefs().edit().putString(KEY_VOICE, currentVoice).apply();
                applyNativeVoice(currentVoice);
                updateVoiceButtonLabel();
                speak("H.E.N.R.Y online, sir.", "neutral");
                Toast.makeText(this, "Voice applied", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null).show();
    }

    private void toggleWakeWord() {
        wakeEnabled = !wakeEnabled;
        getPrefs().edit().putBoolean(KEY_WAKE, wakeEnabled).apply();
        if (wakeEnabled) {
            startWakeService();
            addJarvisMsg("[EMOTION:warm] Wake word enabled, sir. Say **\"Henry\"** anytime.");
            speak("Wake word enabled. Say Henry anytime, sir.", "warm");
        } else {
            stopWakeService();
            addJarvisMsg("[EMOTION:neutral] Wake word disabled, sir.");
            speak("Wake word disabled.", "neutral");
        }
        updateVoiceButtonLabel();
    }

    private void toggleMute() {
        ttsMuted = !ttsMuted;
        getPrefs().edit().putBoolean(KEY_MUTE, ttsMuted).apply();
        Toast.makeText(this, ttsMuted ? "Voice muted" : "Voice enabled", Toast.LENGTH_SHORT).show();
        if (!ttsMuted) speak("Voice enabled, sir.", "warm");
    }

    private void showSpeedPicker() {
        final String[] speeds = { "🐢 Slow", "▶ Normal", "⚡ Fast" };
        new AlertDialog.Builder(this)
            .setTitle("Voice Speed")
            .setSingleChoiceItems(speeds, ttsSpeed, null)
            .setPositiveButton("Apply", (d, w) -> {
                android.widget.ListView lv = ((AlertDialog) d).getListView();
                ttsSpeed = lv.getCheckedItemPosition();
                getPrefs().edit().putInt(KEY_SPEED, ttsSpeed).apply();
                applyNativeVoice(currentVoice);
                Toast.makeText(this, "Speed: " + speeds[ttsSpeed], Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null).show();
    }

    private void showPersonaPicker() {
        final String[] labels = { "😏 Flirty (default)", "👔 Professional", "😊 Casual", "🎯 Tactical" };
        final String[] personas = { PERSONA_FLIRTY, PERSONA_PROFESSIONAL, PERSONA_CASUAL, PERSONA_TACTICAL };
        int cur = 0;
        for (int i = 0; i < personas.length; i++) if (personas[i].equals(currentPersona)) { cur = i; break; }
        final int[] sel = { cur };
        new AlertDialog.Builder(this)
            .setTitle("◆ Persona Mode")
            .setSingleChoiceItems(labels, cur, (d, w) -> sel[0] = w)
            .setPositiveButton("Apply", (d, w) -> {
                currentPersona = personas[sel[0]];
                getPrefs().edit().putString(KEY_PERSONA, currentPersona).apply();
                history.add(new HistoryItem("user",
                    "[SYSTEM] Persona mode changed to: " + currentPersona +
                    ". Adjust your personality accordingly."));
                Toast.makeText(this, "Persona: " + capitalize(currentPersona), Toast.LENGTH_SHORT).show();
                speak("Persona set to " + currentPersona + " mode, sir.", "neutral");
            })
            .setNegativeButton("Cancel", null).show();
    }

    private void showReminders() {
        String list = ReminderManager.listReminders(this);
        String msg  = list != null ? stripEmotionTag(list) : "No upcoming reminders, sir.";
        new AlertDialog.Builder(this)
            .setTitle("◆ H.E.N.R.Y Reminders")
            .setMessage(msg)
            .setPositiveButton("OK", null).show();
    }

    // ── Translation ───────────────────────────────────────────────────────────
    private void translateLastReply() {
        // Find last JARVIS message
        String lastReply = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).type == Message.TYPE_JARVIS) {
                lastReply = messages.get(i).text;
                break;
            }
        }
        if (lastReply == null) {
            Toast.makeText(this, "No reply to translate", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] targetLangs = {
            "Filipino/Tagalog", "French", "Spanish", "Arabic",
            "Japanese", "Korean", "Chinese (Simplified)", "German"
        };
        final String[] langCodes = {
            "Filipino", "French", "Spanish", "Arabic",
            "Japanese", "Korean", "Chinese Simplified", "German"
        };
        final String replyToTranslate = lastReply;
        new AlertDialog.Builder(this)
            .setTitle("Translate to…")
            .setItems(targetLangs, (d, which) -> {
                String lang  = langCodes[which];
                String label = targetLangs[which];
                String prompt = "Translate this text to " + lang + " naturally and fluently. " +
                                "Output only the translation, no explanation:\n\n" + replyToTranslate;
                addJarvisMsg("Translating to " + label + "…");
                setState(OrbView.OrbState.THINKING);
                showTyping();

                List<HistoryItem> transHistory = new ArrayList<>();
                transHistory.add(new HistoryItem("user", prompt));
                JarvisApi.ask(transHistory, null, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imageUrl) {
                        mainHandler.post(() -> {
                            hideTyping();
                            String clean = stripEmotionTag(reply);
                            addJarvisMsg("**[" + label + "]** " + clean);
                            speak(clean, "neutral");
                            setState(OrbView.OrbState.IDLE);
                        });
                    }
                    @Override public void onError(String error) {
                        mainHandler.post(() -> {
                            hideTyping();
                            addJarvisMsg("Translation failed, sir: " + error);
                            setState(OrbView.OrbState.IDLE);
                        });
                    }
                });
            }).show();
    }

    // ── Export Chat ───────────────────────────────────────────────────────────
    private void exportChat() {
        if (messages.isEmpty()) {
            Toast.makeText(this, "No chat to export", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] exportOptions = { "📋 Share as Text", "💾 Save to File" };
        new AlertDialog.Builder(this)
            .setTitle("Export Chat")
            .setItems(exportOptions, (d, which) -> {
                String content = buildChatExportText();
                if (which == 0) {
                    // Share via system share sheet
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, content);
                    share.putExtra(Intent.EXTRA_SUBJECT, "H.E.N.R.Y Chat Export");
                    startActivity(Intent.createChooser(share, "Export chat via…"));
                } else {
                    // Save to file and share file URI
                    saveChatToFile(content);
                }
            }).show();
    }

    private String buildChatExportText() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════\n");
        sb.append("  H.E.N.R.Y Chat Export\n");
        sb.append("  Highly Enhanced Neural Reasoning for You\n");
        sb.append("  ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date())).append("\n");
        sb.append("═══════════════════════════════\n\n");
        for (Message m : messages) {
            if (m.type == Message.TYPE_USER)
                sb.append("YOU ▶  ").append(m.text).append("\n\n");
            else if (m.type == Message.TYPE_JARVIS)
                sb.append("HENRY ◆  ").append(m.text).append("\n\n");
        }
        sb.append("═══════════════════════════════\n");
        sb.append("Total messages: ").append(messages.size()).append("\n");
        return sb.toString();
    }

    private void saveChatToFile(String content) {
        new Thread(() -> {
            try {
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                                     "HENRY_chat_" + ts + ".txt");
                file.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(content.getBytes("UTF-8"));
                }
                Uri fileUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", file);
                mainHandler.post(() -> {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_STREAM, fileUri);
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share, "Save or share chat file…"));
                    Toast.makeText(this, "Saved: " + file.getName(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                    Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void updateVoiceButtonLabel() {
        if (btnVoice == null) return;
        String wake = wakeEnabled ? " 🟢" : "";
        switch (currentVoice) {
            case VOICE_BRITISH_MALE:    btnVoice.setText("🇬🇧♂" + wake); break;
            case VOICE_BRITISH_FEMALE:  btnVoice.setText("🇬🇧♀" + wake); break;
            case VOICE_AMERICAN_FEMALE: btnVoice.setText("🇺🇸♀" + wake); break;
            case VOICE_FILIPINO_MALE:   btnVoice.setText("🇵🇭♂" + wake); break;
            case VOICE_FILIPINO_FEMALE: btnVoice.setText("🇵🇭♀" + wake); break;
            case VOICE_FRENCH_MALE:     btnVoice.setText("🇫🇷♂" + wake); break;
            case VOICE_FRENCH_FEMALE:   btnVoice.setText("🇫🇷♀" + wake); break;
            default:                    btnVoice.setText("🇺🇸♂" + wake); break;
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
            .replaceAll("(?m)^#{1,6}\\s*", "")
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
            .replaceAll("\\[[^\\]]*\\]", "")
            .replaceAll("https?://\\S+", "")
            .replaceAll("[|^~`#@]", "")
            .replaceAll("(?m)^\\s*[-*+]\\s+", "")
            .replaceAll("[\\r\\n]+", " ")
            .replaceAll("\\s{2,}", " ")
            .trim();
    }

    // ── Speak ─────────────────────────────────────────────────────────────────
    private void speak(String text) { speak(text, "neutral"); }

    private void speak(final String rawText, final String emotion) {
        if (ttsMuted || rawText == null || rawText.trim().isEmpty()) return;
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
                    body.toString(), MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                    .url(SPEAK_URL).post(rb)
                    .addHeader("Content-Type", "application/json").build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.code() != 204 && resp.body() != null) {
                        byte[] audio = resp.body().bytes();
                        if (audio.length > 0) {
                            mainHandler.post(() -> playAudioBytes(audio, clean));
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {}
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
                speakNative(fallbackText); return true;
            });
            ttsPlayer.prepare();
            ttsPlayer.start();
        } catch (Exception e) { speakNative(fallbackText); }
    }

    private void speakNative(String clean) {
        if (!ttsReady || tts == null) {
            isSpeaking = false; setState(OrbView.OrbState.IDLE); return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
        else
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null);
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
            .setTitle("Attach")
            .setItems(new String[]{"📷 Take photo", "🖼 Choose image", "📄 Read PDF"}, (d, which) -> {
                if (which == 0)      openCamera();
                else if (which == 1) openGallery();
                else                 openPdf();
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
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Select image"), REQUEST_GALLERY);
    }

    private void openPdf() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("application/pdf");
        startActivityForResult(Intent.createChooser(i, "Select PDF"), REQUEST_PDF);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;

        if (req == REQUEST_PDF && data != null && data.getData() != null) {
            handlePdfAttachment(data.getData());
            return;
        }

        Uri uri = null;
        if (req == REQUEST_CAMERA && cameraImageUri != null) uri = cameraImageUri;
        else if (req == REQUEST_GALLERY && data != null)     uri = data.getData();
        if (uri != null) encodeImageAsync(uri);
    }

    // ── PDF Reading ───────────────────────────────────────────────────────────
    private void handlePdfAttachment(Uri pdfUri) {
        Toast.makeText(this, "Reading PDF…", Toast.LENGTH_SHORT).show();
        if (ivAttachPreview != null) ivAttachPreview.setVisibility(View.GONE);

        PdfReader.read(this, pdfUri, new PdfReader.Callback() {
            @Override public void onResult(String text, int pages) {
                mainHandler.post(() -> {
                    pendingPdfText = text;
                    pendingImageBase64 = null;
                    pendingImageUriStr = null;
                    if (ivAttachPreview != null) {
                        ivAttachPreview.setImageDrawable(null);
                        // Show a placeholder icon to indicate PDF is attached
                        ivAttachPreview.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(MainActivity.this,
                        "PDF read: " + pages + " page(s). Ask me anything about it.",
                        Toast.LENGTH_LONG).show();
                    if (etInput != null) etInput.setHint("Ask about the PDF…");
                });
            }
            @Override public void onError(String reason) {
                mainHandler.post(() ->
                    Toast.makeText(MainActivity.this, "PDF error: " + reason, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void clearAttachment() {
        pendingImageBase64 = null;
        pendingImageUriStr = null;
        pendingPdfText     = null;
        if (ivAttachPreview != null) {
            ivAttachPreview.setImageDrawable(null);
            ivAttachPreview.setVisibility(View.GONE);
        }
        if (etInput != null) etInput.setHint("Command HENRY…");
    }

    // ── Image encoding ────────────────────────────────────────────────────────
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
                    pendingPdfText     = null;
                    if (ivAttachPreview != null) {
                        ivAttachPreview.setImageURI(uri);
                        ivAttachPreview.setVisibility(View.VISIBLE);
                    }
                    if (etInput != null) etInput.setHint("Ask about the image…");
                    Toast.makeText(this, "Image attached", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Voice Input ───────────────────────────────────────────────────────────
    private void toggleListening() {
        if (isSpeaking) { stopSpeaking(); return; }
        if (isListening) stopListening(); else startListening();
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, PERM_CODE);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_LONG).show(); return;
        }
        if (speechRec != null) {
            try { speechRec.destroy(); } catch (Exception ignored) {}
        }
        speechRec = SpeechRecognizer.createSpeechRecognizer(this);
        speechRec.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                isListening = true;
                mainHandler.post(() -> {
                    setState(OrbView.OrbState.LISTENING);
                    if (tvOrbHint != null) tvOrbHint.setText("LISTENING — TAP TO STOP");
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
                        Toast.makeText(MainActivity.this, "Voice error (" + error + ")", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onResults(Bundle results) {
                isListening = false;
                mainHandler.post(() -> {
                    if (tvOrbHint != null) tvOrbHint.setText("WHAT CAN I DO FOR YOU, SIR?");
                });
                ArrayList<String> m = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
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
        boolean hasAttachment = pendingImageBase64 != null || pendingPdfText != null;
        if (text.isEmpty() && !hasAttachment) return;
        if (currentState == OrbView.OrbState.THINKING) return;
        if (text.isEmpty() && pendingImageBase64 != null)
            text = "Analyse this image and describe what you see in detail.";
        if (text.isEmpty() && pendingPdfText != null)
            text = "Summarise this document for me.";
        etInput.setText("");
        etInput.setHint("Command HENRY…");
        hideWelcome();
        askJarvis(text);
    }

    private void askJarvis(String userText) {
        // ── Reminder detection ────────────────────────────────────────────────
        String reminderReply = ReminderManager.trySchedule(this, userText);
        if (reminderReply != null) {
            history.add(new HistoryItem("user", userText));
            addUserMsg(userText);
            String clean = stripEmotionTag(reminderReply);
            history.add(new HistoryItem("model", clean));
            addJarvisMsg(clean);
            speak(clean, extractEmotion(reminderReply));
            saveHistory();
            return;
        }

        // ── "My reminders" query ──────────────────────────────────────────────
        if (userText.toLowerCase().contains("my reminder") ||
            userText.toLowerCase().contains("list reminder")) {
            String list = ReminderManager.listReminders(this);
            String reply = list != null ? list : "[EMOTION:warm]\nNo upcoming reminders, sir.";
            history.add(new HistoryItem("user", userText));
            addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            addJarvisMsg(clean);
            speak(clean, extractEmotion(reply));
            history.add(new HistoryItem("model", clean));
            saveHistory();
            return;
        }

        // ── Contacts / Calls / SMS ─────────────────────────────────────────────
        String contactCmd = ContactsHelper.parseContactCommand(userText);
        if (contactCmd != null) {
            handleContactCommand(contactCmd, userText);
            return;
        }

        // ── Calendar query ─────────────────────────────────────────────────────
        String calLower = userText.toLowerCase();
        if (calLower.contains("my schedule") || calLower.contains("my calendar")
            || calLower.contains("my event") || calLower.contains("what do i have")) {
            String calResult = readCalendar();
            if (calResult != null) {
                history.add(new HistoryItem("user", userText));
                addUserMsg(userText);
                history.add(new HistoryItem("model", calResult));
                addJarvisMsg(calResult);
                speak(calResult, "neutral");
                saveHistory();
                return;
            }
        }

        // ── PDF context injection ──────────────────────────────────────────────
        String effectiveUserText = userText;
        if (pendingPdfText != null) {
            effectiveUserText = "The following is a PDF document the user wants to discuss:\n\n"
                + pendingPdfText + "\n\n---\n\nUser question: " + userText;
            pendingPdfText = null;
            clearAttachment();
        }

        // ── Default: send to AI backend ────────────────────────────────────────
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

        // Build history for API call — inject PDF text in last user message if present
        List<HistoryItem> apiHistory = new ArrayList<>(history);
        if (!effectiveUserText.equals(userText) && !apiHistory.isEmpty()) {
            // Replace last user entry with the enriched version
            apiHistory.set(apiHistory.size() - 1, new HistoryItem("user", effectiveUserText));
        }

        JarvisApi.ask(apiHistory, imageB64, new JarvisApi.Callback() {
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

    // ── Contacts / Calls / SMS ────────────────────────────────────────────────
    private void handleContactCommand(String cmd, String userText) {
        history.add(new HistoryItem("user", userText));
        addUserMsg(userText);
        String[] parts = cmd.split(":", 3);
        String action = parts[0];
        String name   = parts.length > 1 ? parts[1].trim() : "";
        String body   = parts.length > 2 ? parts[2].trim() : "";

        String number = ContactsHelper.findNumber(this, name);
        if (number == null) {
            String reply = "I couldn't find " + name + " in your contacts, sir.";
            addJarvisMsg(reply); speak(reply, "concerned"); return;
        }

        if ("CALL".equals(action)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingCallAction = cmd;
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, PERM_CALL);
                return;
            }
            startActivity(ContactsHelper.callIntent(number));
            String reply = "Calling " + name + " now, sir.";
            addJarvisMsg(reply); speak(reply, "warm");
        } else {
            startActivity(ContactsHelper.smsIntent(number, body));
            String reply = "Opening message to " + name + ", sir.";
            addJarvisMsg(reply); speak(reply, "warm");
        }
        saveHistory();
    }

    // ── Calendar ──────────────────────────────────────────────────────────────
    private String readCalendar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) return null;
        try {
            ContentResolver cr = getContentResolver();
            long now      = System.currentTimeMillis();
            long tomorrow = now + 48 * 3600_000L;
            Uri uri = CalendarContract.Events.CONTENT_URI;
            String[] proj = {
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            };
            String sel = CalendarContract.Events.DTSTART + " >= ? AND "
                       + CalendarContract.Events.DTSTART + " <= ?";
            String[] args = { String.valueOf(now), String.valueOf(tomorrow) };
            try (Cursor c = cr.query(uri, proj, sel, args, CalendarContract.Events.DTSTART + " ASC")) {
                if (c == null || !c.moveToFirst()) return "No events in the next 48 hours, sir.";
                SimpleDateFormat sdf = new SimpleDateFormat("EEE h:mm a", Locale.US);
                StringBuilder sb = new StringBuilder("Your upcoming events, sir:\n\n");
                int count = 0;
                do {
                    String title = c.getString(0);
                    long   start = c.getLong(1);
                    sb.append("**").append(sdf.format(new Date(start))).append("** — ")
                      .append(title).append("\n");
                    count++;
                } while (c.moveToNext() && count < 10);
                return sb.toString().trim();
            }
        } catch (Exception e) { return null; }
    }

    // ── Clear menu ────────────────────────────────────────────────────────────
    private void showClearMenu() {
        new AlertDialog.Builder(this)
            .setTitle("Clear")
            .setItems(new String[]{"🗑 Clear conversation", "📤 Export chat first"}, (d, w) -> {
                if (w == 0) confirmClear(); else exportChat();
            }).show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
            .setTitle("Clear Memory")
            .setMessage("Wipe all conversation history?")
            .setPositiveButton("Clear", (d, w) -> {
                history.clear(); messages.clear();
                getPrefs().edit().remove(KEY_HIS).apply();
                adapter.notifyDataSetChanged();
                if (orbSection != null) orbSection.setVisibility(View.VISIBLE);
                if (chipsRow1  != null) chipsRow1.setVisibility(View.VISIBLE);
                if (chipsRow2  != null) chipsRow2.setVisibility(View.VISIBLE);
                if (chipsRow3  != null) chipsRow3.setVisibility(View.VISIBLE);
            })
            .setNegativeButton("Cancel", null).show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void addUserMsg(String text) {
        messages.add(new Message(Message.TYPE_USER, text));
        adapter.notifyItemInserted(messages.size() - 1); scrollToBottom();
    }
    private void addJarvisMsg(String text) {
        messages.add(new Message(Message.TYPE_JARVIS, text));
        adapter.notifyItemInserted(messages.size() - 1); scrollToBottom();
    }
    private void scrollToBottom() {
        recycler.post(() -> {
            if (scrollMain != null) scrollMain.post(() -> scrollMain.fullScroll(View.FOCUS_DOWN));
        });
    }
    private void showTyping() {
        messages.add(new Message(Message.TYPE_TYPING, ""));
        typingPos = messages.size() - 1;
        adapter.notifyItemInserted(typingPos); scrollToBottom();
    }
    private void hideTyping() {
        if (typingPos >= 0 && typingPos < messages.size()) {
            messages.remove(typingPos);
            adapter.notifyItemRemoved(typingPos); typingPos = -1;
        }
    }
    private void setState(OrbView.OrbState state) {
        currentState = state;
        if (orbView  != null) orbView.setState(state);
        if (tvStatus != null) {
            final String[] labels = {"STANDBY","LISTENING…","PROCESSING…","SPEAKING…","WAKE"};
            tvStatus.setText(labels[state.ordinal()]);
        }
    }
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Memory ────────────────────────────────────────────────────────────────
    private void saveHistory() {
        List<HistoryItem> toSave = history.size() > 80
            ? history.subList(history.size() - 80, history.size()) : history;
        getPrefs().edit().putString(KEY_HIS, gson.toJson(toSave)).apply();
    }
    private void loadHistory() {
        String json = getPrefs().getString(KEY_HIS, null);
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
            if (!messages.isEmpty()) { adapter.notifyDataSetChanged(); scrollToBottom(); }
        } catch (Exception ignored) {}
    }

    // ── Permissions ───────────────────────────────────────────────────────────
    private void requestPerms() {
        List<String> needed = new ArrayList<>();
        needed.add(Manifest.permission.RECORD_AUDIO);
        needed.add(Manifest.permission.CAMERA);
        needed.add(Manifest.permission.READ_CONTACTS);
        needed.add(Manifest.permission.READ_CALENDAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
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
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERM_CALL && pendingCallAction != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                handleContactCommand(pendingCallAction, "");
            }
            pendingCallAction = null;
        }
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
        try { unregisterReceiver(wakeReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(notifReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
