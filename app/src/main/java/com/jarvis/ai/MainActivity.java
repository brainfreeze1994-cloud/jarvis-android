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
import android.provider.Settings;
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
    private static final int    REQUEST_QR       = 203;
    private static final int    REQUEST_LOCATION = 204;
    private static final int    REQUEST_LIVE_CAM = 205;
    private static final int    REQUEST_DOC_SCAN = 206;
    private static final String PREFS            = "jarvis_prefs";
    private static final String KEY_HIS          = "history_v2";
    private static final String KEY_VOICE        = "voice_choice";
    private static final String KEY_WAKE         = "wake_enabled";
    private static final String KEY_MUTE         = "tts_muted";
    private static final String KEY_SPEED        = "tts_speed";
    private static final String KEY_PERSONA      = "persona_mode";
    private static final String KEY_RESP_MODE    = "response_mode";
    private static final String KEY_SCREEN_ON    = "screen_always_on";
    private static final String CRASH_FILE       = "henry_crash.txt";
    private static final String SPEAK_URL        = "https://jarvis-ai-seven-dun.vercel.app/api/speak";

    // Response mode
    private static final String MODE_BRIEF    = "brief";
    private static final String MODE_BALANCED = "balanced";
    private static final String MODE_DETAILED = "detailed";
    private String responseMode  = MODE_BALANCED;
    private boolean screenAlwaysOn = false;

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
    private String pendingDocScanQuestion; // question asked when scan launched

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

    // Active timer state
    private TextView tvTimerBadge = null;

    // User profile (loaded once, sent with every AI call)
    private UserProfile userProfile;

    // Floating bubble
    private static final String KEY_BUBBLE = "bubble_enabled";
    private boolean bubbleEnabled = false;

    // Battery guardian
    private Handler batteryHandler;
    private Runnable batteryChecker;

    // Fitness tracker
    private FitnessTracker fitnessTracker;

    // Sleep mode state
    private boolean sleepModeActive = false;

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
        if (intent.getBooleanExtra("start_listening", false)) {
            mainHandler.postDelayed(this::startListening, 600);
        }
        // Handle shared content from ShareReceiver
        String sharedText  = intent.getStringExtra("shared_text");
        String sharedImage = intent.getStringExtra("shared_image");
        if (sharedImage != null) {
            encodeImageAsync(android.net.Uri.parse(sharedImage));
            mainHandler.postDelayed(() -> {
                hideWelcome();
                String prompt = sharedText != null ? sharedText : "Analyse this image for me.";
                if (etInput != null) etInput.setText(prompt);
            }, 800);
        } else if (sharedText != null) {
            hideWelcome();
            final String txt = sharedText;
            mainHandler.post(() -> askJarvis("I'm sharing this with you: " + txt));
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
        currentVoice   = getPrefs().getString(KEY_VOICE,      VOICE_AMERICAN_MALE);
        currentPersona = getPrefs().getString(KEY_PERSONA,    PERSONA_FLIRTY);
        responseMode   = getPrefs().getString(KEY_RESP_MODE,  MODE_BALANCED);
        wakeEnabled    = getPrefs().getBoolean(KEY_WAKE,      false);
        ttsMuted       = getPrefs().getBoolean(KEY_MUTE,      false);
        ttsSpeed       = getPrefs().getInt(KEY_SPEED,         1);
        screenAlwaysOn = getPrefs().getBoolean(KEY_SCREEN_ON, false);
        applyScreenAlwaysOn();
        userProfile    = UserProfile.load(this);
        bubbleEnabled  = getPrefs().getBoolean(KEY_BUBBLE, false);
        if (bubbleEnabled) startBubble();

        requestPerms();
        loadHistory();
        initNativeTts();
        updateVoiceButtonLabel();
        registerReceivers();

        if (wakeEnabled) startWakeService();

        // Battery guardian — check every 2 minutes
        batteryHandler  = new Handler(Looper.getMainLooper());
        batteryChecker  = new Runnable() {
            @Override public void run() {
                BatteryGuardian.check(MainActivity.this, (level, threshold) -> {
                    String msg = "[EMOTION:concerned] Sir, battery is at " + level +
                        "%. I strongly recommend plugging in now.";
                    addJarvisMsg(stripEmotionTag(msg));
                    speak(msg, "concerned");
                });
                batteryHandler.postDelayed(this, 2 * 60 * 1000);
            }
        };
        batteryHandler.postDelayed(batteryChecker, 30 * 1000); // first check after 30s

        // Fitness tracker
        fitnessTracker = new FitnessTracker(this);
        fitnessTracker.start();

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

        setupChip(R.id.chip1, 0);
        setupChip(R.id.chip2, 1);
        setupChip(R.id.chip3, 2);
        setupChip(R.id.chip4, 3);
        setupChip(R.id.chip5, 4);
        setupChip(R.id.chip6, 5);

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

    private void setupChip(int chipId, int index) {
        View chip = findViewById(chipId);
        if (chip == null) return;
        // Update label
        String text = ChipPrefs.get(this, index);
        if (chip instanceof TextView) ((TextView) chip).setText(
            text.length() > 28 ? text.substring(0, 25) + "…" : text);
        // Tap → ask
        chip.setOnClickListener(v -> { hideWelcome(); askJarvis(ChipPrefs.get(this, index)); });
        // Long-press → edit
        chip.setOnLongClickListener(v -> {
            editChip(chipId, index); return true;
        });
    }

    private void editChip(int chipId, int index) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(ChipPrefs.get(this, index));
        input.setSingleLine(true);
        input.selectAll();
        new AlertDialog.Builder(this)
            .setTitle("Edit Suggestion")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String newText = input.getText().toString().trim();
                if (!newText.isEmpty()) {
                    ChipPrefs.set(this, index, newText);
                    setupChip(chipId, index); // refresh label
                    Toast.makeText(this, "Chip updated", Toast.LENGTH_SHORT).show();
                }
            })
            .setNeutralButton("Reset", (d, w) -> {
                ChipPrefs.reset(this, index);
                setupChip(chipId, index);
            })
            .setNegativeButton("Cancel", null).show();
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
        String modeLbl = MODE_BRIEF.equals(responseMode) ? "Brief"
                       : MODE_DETAILED.equals(responseMode) ? "Detailed" : "Balanced";
        CharSequence[] options = {
            "🎙 Voice Accent",
            wakeEnabled    ? "🟢 Wake Word: ON  (tap to disable)"  : "⚫ Wake Word: OFF  (tap to enable)",
            ttsMuted       ? "🔇 Voice Muted  (tap to unmute)"    : "🔊 Voice Enabled  (tap to mute)",
            "⚡ Voice Speed: " + new String[]{"Slow","Normal","Fast"}[ttsSpeed],
            "🧠 Persona: " + capitalize(currentPersona),
            "💬 Response Mode: " + modeLbl,
            screenAlwaysOn ? "💡 Screen Always-On: ON  (tap off)" : "💡 Screen Always-On: OFF  (tap on)",
            bubbleEnabled  ? "🫧 Floating Bubble: ON  (tap off)"  : "🫧 Floating Bubble: OFF  (tap on)",
            "☀️ Daily Digest — Full Morning Briefing",
            "📰 Headlines Only",
            "📋 My Reminders",
            "📝 My Notes",
            "🛒 Shopping List",
            "⚡ My Shortcuts",
            "🔍 Search Chat History",
            "👤 My Profile",
            "📤 Export Chat",
            "🌐 Translate Last Reply"
        };
        new AlertDialog.Builder(this)
            .setTitle("◆ H.E.N.R.Y Settings")
            .setItems(options, (d, which) -> {
                switch (which) {
                    case 0:  showVoiceAccentPicker(); break;
                    case 1:  toggleWakeWord(); break;
                    case 2:  toggleMute(); break;
                    case 3:  showSpeedPicker(); break;
                    case 4:  showPersonaPicker(); break;
                    case 5:  showResponseModePicker(); break;
                    case 6:  toggleScreenAlwaysOn(); break;
                    case 7:  toggleBubble(); break;
                    case 8:  showDailyDigest(); break;
                    case 9:  readNewsBriefing(); break;
                    case 10: showReminders(); break;
                    case 11: showNotes(); break;
                    case 12: showShoppingList(); break;
                    case 13: showShortcuts(); break;
                    case 14: showChatSearch(); break;
                    case 15: showProfileEditor(); break;
                    case 16: exportChat(); break;
                    case 17: translateLastReply(); break;
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

    private void showResponseModePicker() {
        final String[] labels  = { "⚡ Brief — 1-2 sentences max", "⚖ Balanced — default", "📖 Detailed — full explanations" };
        final String[] modes   = { MODE_BRIEF, MODE_BALANCED, MODE_DETAILED };
        int cur = 0;
        for (int i = 0; i < modes.length; i++) if (modes[i].equals(responseMode)) { cur = i; break; }
        final int[] sel = { cur };
        new AlertDialog.Builder(this)
            .setTitle("◆ Response Mode")
            .setSingleChoiceItems(labels, cur, (d, w) -> sel[0] = w)
            .setPositiveButton("Apply", (d, w) -> {
                responseMode = modes[sel[0]];
                getPrefs().edit().putString(KEY_RESP_MODE, responseMode).apply();
                String msg;
                switch (responseMode) {
                    case MODE_BRIEF:    msg = "Switching to brief mode, sir. Short and sharp."; break;
                    case MODE_DETAILED: msg = "Detailed mode, sir. I will hold nothing back."; break;
                    default:            msg = "Balanced mode restored, sir."; break;
                }
                Toast.makeText(this, "Mode: " + responseMode, Toast.LENGTH_SHORT).show();
                speak(msg, "neutral");
            })
            .setNegativeButton("Cancel", null).show();
    }

    // ── Screen Always-On ──────────────────────────────────────────────────────
    private void toggleScreenAlwaysOn() {
        screenAlwaysOn = !screenAlwaysOn;
        getPrefs().edit().putBoolean(KEY_SCREEN_ON, screenAlwaysOn).apply();
        applyScreenAlwaysOn();
        String msg = screenAlwaysOn
            ? "Screen will stay on while I'm with you, sir."
            : "Screen timeout restored, sir.";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        speak(msg, "neutral");
    }

    private void applyScreenAlwaysOn() {
        if (screenAlwaysOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // ── News Briefing ─────────────────────────────────────────────────────────
    private void readNewsBriefing() {
        addJarvisMsg("Fetching your morning briefing, sir…");
        setState(OrbView.OrbState.THINKING);
        NewsReader.fetch(new NewsReader.Callback() {
            @Override public void onResult(String formatted) {
                String clean = stripEmotionTag(formatted);
                String emotion = extractEmotion(formatted);
                addJarvisMsg(clean);
                speak(clean, emotion);
                setState(OrbView.OrbState.IDLE);
            }
            @Override public void onError(String reason) {
                addJarvisMsg(reason);
                speak(reason, "concerned");
                setState(OrbView.OrbState.IDLE);
            }
        });
    }

    // ── Voice Notes ───────────────────────────────────────────────────────────
    private void showNotes() {
        String notes = VoiceNotes.readAll(this);
        String clean = stripEmotionTag(notes);
        new AlertDialog.Builder(this)
            .setTitle("◆ H.E.N.R.Y Notes")
            .setMessage(clean)
            .setPositiveButton("OK", null)
            .setNegativeButton("Clear All", (d, w) -> {
                String reply = stripEmotionTag(VoiceNotes.deleteAll(this));
                Toast.makeText(this, reply, Toast.LENGTH_SHORT).show();
            }).show();
    }

    // ── User Profile Editor ───────────────────────────────────────────────────
    private void showProfileEditor() {
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        android.widget.EditText etName      = field(layout, "Your name",           userProfile.name);
        android.widget.EditText etCity      = field(layout, "Your city",           userProfile.city);
        android.widget.EditText etJob       = field(layout, "Your job/role",       userProfile.job);
        android.widget.EditText etInterests = field(layout, "Interests (hobbies, topics…)", userProfile.interests);
        android.widget.EditText etNickname  = field(layout, "What HENRY calls you (default: sir)", userProfile.nickname);

        new AlertDialog.Builder(this)
            .setTitle("◆ Your Profile")
            .setView(layout)
            .setPositiveButton("Save", (d, w) -> {
                userProfile.name      = etName.getText().toString().trim();
                userProfile.city      = etCity.getText().toString().trim();
                userProfile.job       = etJob.getText().toString().trim();
                userProfile.interests = etInterests.getText().toString().trim();
                userProfile.nickname  = etNickname.getText().toString().trim();
                if (userProfile.nickname.isEmpty()) userProfile.nickname = "sir";
                userProfile.save(this);
                String name = userProfile.name.isEmpty() ? "sir" : userProfile.name;
                String msg  = "Profile saved. Good to know you better, " + name + ".";
                Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
                speak(msg, "warm");
            })
            .setNegativeButton("Cancel", null).show();
    }

    private android.widget.EditText field(android.widget.LinearLayout parent, String hint, String value) {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint(hint);
        et.setText(value != null ? value : "");
        et.setSingleLine(true);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 16;
        et.setLayoutParams(lp);
        parent.addView(et);
        return et;
    }

    private void showReminders() {
        String list = ReminderManager.listReminders(this);
        String msg  = list != null ? stripEmotionTag(list) : "No upcoming reminders, sir.";
        new AlertDialog.Builder(this)
            .setTitle("◆ H.E.N.R.Y Reminders")
            .setMessage(msg)
            .setPositiveButton("OK", null).show();
    }

    // ── Floating Bubble ───────────────────────────────────────────────────────
    private void toggleBubble() {
        if (bubbleEnabled) {
            bubbleEnabled = false;
            getPrefs().edit().putBoolean(KEY_BUBBLE, false).apply();
            stopService(new Intent(this, FloatingBubbleService.class));
            speak("Bubble dismissed, sir.", "neutral");
        } else {
            // Need SYSTEM_ALERT_WINDOW permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !android.provider.Settings.canDrawOverlays(this)) {
                Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + getPackageName()));
                startActivity(i);
                Toast.makeText(this, "Grant 'Display over other apps' then try again", Toast.LENGTH_LONG).show();
                return;
            }
            bubbleEnabled = true;
            getPrefs().edit().putBoolean(KEY_BUBBLE, true).apply();
            startBubble();
            speak("Bubble activated, sir. I'll follow you everywhere.", "warm");
        }
    }

    private void startBubble() {
        Intent i = new Intent(this, FloatingBubbleService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
    }

    // ── Daily Digest ──────────────────────────────────────────────────────────
    private void showDailyDigest() {
        addJarvisMsg("Assembling your morning briefing, sir…");
        setState(OrbView.OrbState.THINKING);
        DailyDigest.build(this, digest -> {
            String clean   = stripEmotionTag(digest);
            String emotion = extractEmotion(digest);
            addJarvisMsg(clean);
            speak(clean, emotion);
            setState(OrbView.OrbState.IDLE);
        });
    }

    // ── Shopping List ─────────────────────────────────────────────────────────
    private void showShoppingList() {
        String content = stripEmotionTag(ShoppingList.readAll(this));
        new AlertDialog.Builder(this)
            .setTitle("◆ Shopping List")
            .setMessage(content.isEmpty() ? "Empty, sir." : content)
            .setPositiveButton("OK", null)
            .setNeutralButton("Share", (d, w) -> {
                Intent share = ShoppingList.shareIntent(this);
                if (share != null) startActivity(share);
                else Toast.makeText(this, "List is empty", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Clear All", (d, w) -> {
                ShoppingList.clearAll(this);
                Toast.makeText(this, "List cleared", Toast.LENGTH_SHORT).show();
            }).show();
    }

    // ── Custom Shortcuts ──────────────────────────────────────────────────────
    private void showShortcuts() {
        String list = stripEmotionTag(CustomShortcuts.listAll(this));
        new AlertDialog.Builder(this)
            .setTitle("◆ Voice Shortcuts")
            .setMessage(list)
            .setPositiveButton("Add New", (d, w) -> showAddShortcut())
            .setNeutralButton("Clear All", (d, w) -> {
                CustomShortcuts.clearAll(this);
                Toast.makeText(this, "Shortcuts cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null).show();
    }

    private void showAddShortcut() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);
        android.widget.EditText etTrigger = field(layout, "When I say… (e.g. good morning)", "");
        android.widget.EditText etAction  = field(layout, "Do / say… (e.g. tell me today's weather)", "");
        new AlertDialog.Builder(this)
            .setTitle("New Shortcut")
            .setView(layout)
            .setPositiveButton("Save", (d, w) -> {
                String trigger = etTrigger.getText().toString().trim();
                String action  = etAction.getText().toString().trim();
                if (!trigger.isEmpty() && !action.isEmpty()) {
                    String reply = stripEmotionTag(CustomShortcuts.add(this, trigger, action));
                    addJarvisMsg(reply); speak(reply, "excited");
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    // ── Chat Search ───────────────────────────────────────────────────────────
    private void showChatSearch() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Search keyword…");
        input.setPadding(48, 24, 48, 24);
        new AlertDialog.Builder(this)
            .setTitle("🔍 Search Conversation")
            .setView(input)
            .setPositiveButton("Search", (d, w) -> {
                String kw = input.getText().toString().trim();
                if (!kw.isEmpty()) {
                    String result = ChatSearch.search(history, kw);
                    String clean  = stripEmotionTag(result);
                    addJarvisMsg(clean);
                    speak("Here are the results for " + kw + ", sir.", "neutral");
                }
            })
            .setNegativeButton("Cancel", null).show();
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
                JarvisApi.ask(transHistory, null, MODE_DETAILED, userProfile, new JarvisApi.Callback() {
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
        // Apply emotion colour to orb
        if (orbView != null) orbView.setEmotion(emotion != null ? emotion : "neutral");

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

        // Live camera result
        if (req == REQUEST_LIVE_CAM && data != null) {
            String analysisResult = data.getStringExtra(LiveCameraActivity.EXTRA_RESULT);
            if (analysisResult != null && !analysisResult.isEmpty()) {
                addJarvisMsg(analysisResult);
                speak(analysisResult, "neutral");
                history.add(new HistoryItem("model", analysisResult));
                saveHistory();
            }
            return;
        }

        if (req == REQUEST_DOC_SCAN && data != null && data.getData() != null) {
            Uri scanUri = data.getData();
            String question = pendingDocScanQuestion != null ? pendingDocScanQuestion : "Summarise this document.";
            pendingDocScanQuestion = null;
            addJarvisMsg("Scanning document with ML Kit OCR…");
            setState(OrbView.OrbState.THINKING);
            DocumentScanner.scan(this, scanUri, new DocumentScanner.Callback() {
                @Override public void onResult(String text, int lineCount) {
                    mainHandler.post(() -> {
                        String intro = "**Scanned text (" + lineCount + " lines):**\n\n" + text;
                        addJarvisMsg(intro);
                        speak("Scan complete, sir. " + lineCount + " lines of text extracted.", "excited");
                        // Now ask HENRY about it
                        String prompt = "The following text was scanned from a document:\n\n" + text +
                            "\n\n---\n\n" + question;
                        setState(OrbView.OrbState.THINKING); showTyping();
                        List<HistoryItem> scanHist = new ArrayList<>();
                        scanHist.add(new HistoryItem("user", prompt));
                        JarvisApi.ask(scanHist, null, responseMode, userProfile, new JarvisApi.Callback() {
                            @Override public void onSuccess(String reply, String imageUrl) {
                                mainHandler.post(() -> {
                                    hideTyping();
                                    String clean   = stripEmotionTag(reply);
                                    String emotion = extractEmotion(reply);
                                    addJarvisMsg(clean); speak(clean, emotion);
                                    history.add(new HistoryItem("model", clean));
                                    saveHistory(); setState(OrbView.OrbState.IDLE);
                                });
                            }
                            @Override public void onError(String error) {
                                mainHandler.post(() -> { hideTyping(); setState(OrbView.OrbState.IDLE); });
                            }
                        });
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        addJarvisMsg(reason); speak(reason, "concerned");
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
        }

        if (req == REQUEST_PDF && data != null && data.getData() != null) {
            handlePdfAttachment(data.getData());
            return;
        }

        Uri uri = null;
        if (req == REQUEST_CAMERA && cameraImageUri != null) uri = cameraImageUri;
        else if (req == REQUEST_GALLERY && data != null)     uri = data.getData();
        if (uri != null) {
            // If doc scan was pending, use OCR path instead of image attachment
            if (pendingDocScanQuestion != null && req == REQUEST_CAMERA) {
                Uri finalUri = uri;
                String question = pendingDocScanQuestion;
                pendingDocScanQuestion = null;
                addJarvisMsg("Scanning document…");
                setState(OrbView.OrbState.THINKING);
                DocumentScanner.scan(this, finalUri, new DocumentScanner.Callback() {
                    @Override public void onResult(String text, int lineCount) {
                        mainHandler.post(() -> {
                            addJarvisMsg("**Scanned (" + lineCount + " lines):**\n\n" + text);
                            speak("Scan complete, " + lineCount + " lines extracted, sir.", "excited");
                            String prompt = "Scanned document text:\n\n" + text + "\n\n---\n\n" + question;
                            List<HistoryItem> h2 = new ArrayList<>();
                            h2.add(new HistoryItem("user", prompt));
                            showTyping();
                            JarvisApi.ask(h2, null, responseMode, userProfile, new JarvisApi.Callback() {
                                @Override public void onSuccess(String reply, String imageUrl) {
                                    mainHandler.post(() -> {
                                        hideTyping();
                                        String clean = stripEmotionTag(reply);
                                        addJarvisMsg(clean); speak(clean, extractEmotion(reply));
                                        history.add(new HistoryItem("model", clean));
                                        saveHistory(); setState(OrbView.OrbState.IDLE);
                                    });
                                }
                                @Override public void onError(String e) {
                                    mainHandler.post(() -> { hideTyping(); setState(OrbView.OrbState.IDLE); });
                                }
                            });
                        });
                    }
                    @Override public void onError(String reason) {
                        mainHandler.post(() -> {
                            addJarvisMsg(reason); speak(reason, "concerned");
                            setState(OrbView.OrbState.IDLE);
                        });
                    }
                });
            } else {
                encodeImageAsync(uri);
            }
        }
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

        String lower = userText.toLowerCase(java.util.Locale.US);

        // ── Custom Shortcuts (checked first — user-defined overrides everything) ──
        String shortcutAction = CustomShortcuts.match(this, userText);
        if (shortcutAction != null) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            // If action looks like a command, execute it; otherwise speak it
            if (shortcutAction.startsWith("ask:") || shortcutAction.startsWith("say:")) {
                String content = shortcutAction.substring(4).trim();
                history.add(new HistoryItem("model", content)); addJarvisMsg(content);
                speak(content, "neutral");
            } else {
                // Treat as a new query to HENRY
                askJarvis(shortcutAction); return;
            }
            saveHistory(); return;
        }

        // ── Chat Search ───────────────────────────────────────────────────────
        String searchKw = ChatSearch.parseSearchCommand(userText);
        if (searchKw != null) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String result = ChatSearch.search(history, searchKw);
            String clean  = stripEmotionTag(result);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak("Here's what I found, sir.", "neutral"); saveHistory(); return;
        }

        // ── Daily Digest ──────────────────────────────────────────────────────
        if (lower.matches(".*(?:daily digest|morning briefing|morning summary|good morning henry).*")) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            saveHistory(); showDailyDigest(); return;
        }

        // ── Shopping List ─────────────────────────────────────────────────────
        if (ShoppingList.isAddCommand(userText)) {
            String item = ShoppingList.parseItem(userText);
            if (item != null) {
                String reply = ShoppingList.add(this, item);
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String clean = stripEmotionTag(reply);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak(clean, extractEmotion(reply)); saveHistory(); return;
            }
        }
        if (ShoppingList.isShowCommand(userText)) {
            String reply = ShoppingList.readAll(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }
        if (ShoppingList.isClearCommand(userText)) {
            String reply = ShoppingList.clearAll(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "neutral"); saveHistory(); return;
        }

        // ── Custom Shortcut definition ("When I say X, do Y") ────────────────
        String[] shortcutDef = CustomShortcuts.parseDefinition(userText);
        if (shortcutDef != null) {
            String reply = CustomShortcuts.add(this, shortcutDef[0], shortcutDef[1]);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }
        if (lower.contains("my shortcut") || lower.contains("list shortcut") || lower.contains("show shortcut")) {
            String reply = CustomShortcuts.listAll(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak("Here are your shortcuts, sir.", "neutral"); saveHistory(); return;
        }

        // ── App Launcher ──────────────────────────────────────────────────────
        if (AppLauncher.isLaunchCommand(userText)) {
            String reply = AppLauncher.launch(this, userText);
            if (reply != null) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String clean = stripEmotionTag(reply);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak(clean, extractEmotion(reply)); saveHistory(); return;
            }
        }

        // ── Web Search ────────────────────────────────────────────────────────
        if (lower.startsWith("search") || lower.startsWith("google ") ||
            lower.contains("search google for") || lower.contains("search the web for") ||
            lower.contains("look up") || lower.contains("search for ")) {
            String query = userText
                .replaceFirst("(?i)search (google )?for\\s+", "")
                .replaceFirst("(?i)search the web for\\s+", "")
                .replaceFirst("(?i)google\\s+", "")
                .replaceFirst("(?i)look up\\s+", "")
                .replaceFirst("(?i)search\\s+", "")
                .trim();
            if (!query.isEmpty()) {
                String reply = AppLauncher.webSearch(this, query);
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String clean = stripEmotionTag(reply);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak(clean, extractEmotion(reply)); saveHistory(); return;
            }
        }

        // ── WhatsApp message ──────────────────────────────────────────────────
        // "WhatsApp John: I'm on my way" or "send whatsapp to John: message"
        java.util.regex.Matcher wam = java.util.regex.Pattern.compile(
            "(?:whatsapp|send\\s+(?:a\\s+)?whatsapp\\s+(?:to\\s+)?)([\\w\\s]+):\\s*(.+)",
            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userText);
        if (wam.find()) {
            String name = wam.group(1).trim();
            String msg  = wam.group(2).trim();
            String number = ContactsHelper.findNumber(this, name);
            String reply;
            if (number != null)
                reply = AppLauncher.whatsappMessage(this, number, msg);
            else
                reply = "[EMOTION:concerned] I couldn't find " + name + " in your contacts, sir.";
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── Calculator ────────────────────────────────────────────────────────
        String calcResult = Calculator.evaluate(userText);
        if (calcResult != null) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(calcResult);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "neutral"); saveHistory(); return;
        }

        // ── Unit / Currency Converter ─────────────────────────────────────────
        String convResult = Calculator.convert(userText);
        if (convResult != null) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(convResult);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "neutral"); saveHistory(); return;
        }

        // ── Alarm ─────────────────────────────────────────────────────────────
        String alarmReply = AlarmHelper.parseAndSet(this, userText);
        if (alarmReply != null) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(alarmReply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(alarmReply)); saveHistory(); return;
        }

        // ── Stopwatch ─────────────────────────────────────────────────────────
        if (lower.contains("stopwatch") || (lower.contains("stop") && lower.contains("watch"))) {
            String sw;
            if (lower.contains("start") || lower.contains("begin") || lower.contains("go"))
                sw = Stopwatch.start(null);
            else if (lower.contains("stop") || lower.contains("pause"))
                sw = Stopwatch.stop();
            else if (lower.contains("reset") || lower.contains("clear"))
                sw = Stopwatch.reset();
            else if (lower.contains("lap"))
                sw = Stopwatch.lap();
            else
                sw = Stopwatch.status();
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(sw);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(sw)); saveHistory(); return;
        }
        if (lower.equals("lap") && Stopwatch.isRunning()) {
            String sw = Stopwatch.lap();
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(sw);
            addJarvisMsg(clean); speak(clean, "excited"); saveHistory(); return;
        }

        // ── QR Scanner ────────────────────────────────────────────────────────
        if (QrScanner.isQrCommand(userText)) {
            String reply = QrScanner.scan(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── Multi-language voice input hint ───────────────────────────────────
        if (lower.contains("switch language") || lower.contains("speak in ") ||
            lower.contains("change language")) {
            String hint = "[EMOTION:neutral] Tap the mic, sir — your voice input language follows the device default. Change it in Settings → System → Language.";
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(hint);
            addJarvisMsg(clean); speak(clean, "neutral"); saveHistory(); return;
        }

        // ── [v11] Emergency SOS ───────────────────────────────────────────────
        if (EmergencySOS.isSOSCommand(userText)) {
            String reply = EmergencySOS.trigger(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "concerned"); saveHistory(); return;
        }
        if (EmergencySOS.isSetupCommand(userText)) {
            String contactName = EmergencySOS.parseContactName(userText);
            if (contactName != null) {
                String number = ContactsHelper.findNumber(this, contactName);
                if (number != null) {
                    EmergencySOS.setContact(this, contactName, number);
                    String reply = "[EMOTION:warm] SOS contact set to **" + contactName + "** (" + number + "), sir. Say 'SOS' in an emergency.";
                    history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                    String clean = stripEmotionTag(reply);
                    history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                    speak(clean, "warm"); saveHistory();
                } else {
                    showSOSSetupDialog();
                }
            } else {
                showSOSSetupDialog();
            }
            return;
        }

        // ── [v11] Mood Tracker ────────────────────────────────────────────────
        if (MoodTracker.isMoodCommand(userText)) {
            String reply = MoodTracker.handle(this, userText);
            if (reply != null) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String clean = stripEmotionTag(reply);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak(clean, extractEmotion(reply)); saveHistory(); return;
            }
        }

        // ── [v11] Habit Tracker ───────────────────────────────────────────────
        if (HabitTracker.isHabitCommand(userText)) {
            String reply = HabitTracker.handle(this, userText);
            if (reply != null) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String clean = stripEmotionTag(reply);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak(clean, extractEmotion(reply)); saveHistory(); return;
            }
        }

        // ── [v11] Pomodoro Timer ──────────────────────────────────────────────
        if (PomodoroTimer.isPomodoroCommand(userText)) {
            PomodoroTimer pomo = PomodoroTimer.getInstance(this);
            pomo.setCallback(new PomodoroTimer.Callback() {
                @Override public void onTick(String label, long secondsLeft) {}
                @Override public void onPhaseComplete(String phase, int sessions) {
                    mainHandler.post(() -> {
                        String msg;
                        if ("WORK".equals(phase)) {
                            msg = "[EMOTION:excited] Focus session complete, sir! Session " + sessions + " done. Take a break.";
                        } else {
                            msg = "[EMOTION:excited] Break's over, sir. Back to work!";
                        }
                        addJarvisMsg(stripEmotionTag(msg));
                        speak(msg, extractEmotion(msg));
                    });
                }
                @Override public void onStopped() {}
            });
            String reply = pomo.handle(userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v11] Weather Forecast ────────────────────────────────────────────
        if (WeatherForecast.isWeatherQuery(userText)) {
            boolean weekly = WeatherForecast.isWeeklyQuery(userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            addJarvisMsg(weekly ? "Fetching 7-day forecast, sir…" : "Checking weather, sir…");
            saveHistory();
            WeatherForecast.fetch(this, weekly, new WeatherForecast.Callback() {
                @Override public void onResult(String formatted) {
                    mainHandler.post(() -> {
                        String clean   = stripEmotionTag(formatted);
                        String emotion = extractEmotion(formatted);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean); speak(clean, emotion);
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        String clean = stripEmotionTag(reason);
                        addJarvisMsg(clean); speak(clean, "neutral");
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
        }

        // ── [v11] Voice Translator ────────────────────────────────────────────
        if (VoiceTranslator.isTranslateCommand(userText)) {
            String[] parsed = VoiceTranslator.parse(userText);
            if (parsed != null) {
                String textToTranslate = parsed[0], langCode = parsed[1], langName = parsed[2];
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                setState(OrbView.OrbState.THINKING);
                addJarvisMsg("Translating to " + langName + ", sir…");
                saveHistory();
                VoiceTranslator.translate(textToTranslate, langCode, langName,
                    new VoiceTranslator.Callback() {
                        @Override public void onResult(String translation, String lang, String code) {
                            mainHandler.post(() -> {
                                String reply = "[EMOTION:excited] **" + textToTranslate + "** in " + lang + ":\n\n**\"" + translation + "\"**";
                                String clean = stripEmotionTag(reply);
                                history.add(new HistoryItem("model", clean));
                                addJarvisMsg(clean);
                                speak(translation, "excited"); // speak the translation
                                saveHistory(); setState(OrbView.OrbState.IDLE);
                            });
                        }
                        @Override public void onError(String reason) {
                            mainHandler.post(() -> {
                                String clean = stripEmotionTag(reason);
                                addJarvisMsg(clean); speak(clean, "concerned");
                                setState(OrbView.OrbState.IDLE);
                            });
                        }
                    });
                return;
            }
        }

        // ── [v10] Sleep Mode ──────────────────────────────────────────────────
        if (SleepMode.isSleepCommand(userText)) {
            int[] wake = SleepMode.parseWakeTime(userText);
            int wh = wake != null ? wake[0] : -1, wm = wake != null ? wake[1] : 0;
            String reply = SleepMode.activate(this, wh, wm);
            sleepModeActive = true;
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "warm"); saveHistory(); return;
        }
        if (SleepMode.isWakeCommand(userText) && sleepModeActive) {
            String reply = SleepMode.deactivate(this);
            sleepModeActive = false;
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "excited"); saveHistory(); return;
        }

        // ── [v10] Fitness Tracker ─────────────────────────────────────────────
        if (FitnessTracker.isStepsCommand(userText)) {
            // Set goal?
            java.util.regex.Matcher goalM = java.util.regex.Pattern.compile(
                "(?:set.*goal|goal.*to).*?(\\d[\\d,]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(userText);
            if (goalM.find()) {
                int newGoal = Integer.parseInt(goalM.group(1).replace(",", ""));
                fitnessTracker.setDailyGoal(this, newGoal);
                String reply = "[EMOTION:excited] Daily step goal set to **" + String.format("%,d", newGoal) + "** steps, sir. Let's move!";
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String clean = stripEmotionTag(reply);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak(clean, "excited"); saveHistory(); return;
            }
            String reply = FitnessTracker.getStatusReport(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v10] Voice Journal ───────────────────────────────────────────────
        if (VoiceJournal.isSaveCommand(userText)) {
            String entry = VoiceJournal.parseEntry(userText);
            String reply = VoiceJournal.save(this, entry);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }
        if (VoiceJournal.isReadCommand(userText)) {
            String reply = VoiceJournal.readRecent(this, 5);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak("Here are your recent journal entries, sir.", "warm"); saveHistory(); return;
        }
        if (VoiceJournal.isSearchCommand(userText)) {
            String kw = userText.replaceAll("(?i)(search|journal|diary|find)\\s*", "").trim();
            String reply = VoiceJournal.search(this, kw);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak("Here are the journal results, sir.", "neutral"); saveHistory(); return;
        }
        if (VoiceJournal.isExportCommand(userText)) {
            Intent exportI = VoiceJournal.exportIntent(this);
            if (exportI != null) startActivity(exportI);
            else Toast.makeText(this, "Journal is empty", Toast.LENGTH_SHORT).show();
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = "[EMOTION:warm] Sharing your journal, sir.";
            String clean = stripEmotionTag(reply);
            addJarvisMsg(clean); speak(clean, "warm"); saveHistory(); return;
        }

        // ── [v10] Live Prices ─────────────────────────────────────────────────
        if (LivePrices.isPriceQuery(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            addJarvisMsg("Fetching live prices, sir…");
            saveHistory();
            LivePrices.query(userText, new LivePrices.Callback() {
                @Override public void onResult(String formatted) {
                    mainHandler.post(() -> {
                        String clean   = stripEmotionTag(formatted);
                        String emotion = extractEmotion(formatted);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean); speak(clean, emotion);
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        String clean = stripEmotionTag(reason);
                        addJarvisMsg(clean); speak(clean, "neutral");
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
        }

        // ── [v10] Document Scanner ────────────────────────────────────────────
        if (DocumentScanner.isScanCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            pendingDocScanQuestion = userText;
            // Open gallery or camera picker
            new AlertDialog.Builder(this)
                .setTitle("◆ Scan Document")
                .setItems(new String[]{"📷 Take photo", "🖼 Choose from gallery"}, (d, which) -> {
                    if (which == 0) {
                        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        try {
                            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                            File photo = File.createTempFile("SCAN_" + ts, ".jpg",
                                getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                            cameraImageUri = FileProvider.getUriForFile(
                                this, getPackageName() + ".provider", photo);
                            cam.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                            startActivityForResult(cam, REQUEST_CAMERA);
                            // Doc scan result handled in onActivityResult by using pendingDocScanQuestion
                        } catch (Exception e) {
                            Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                        pick.setType("image/*");
                        startActivityForResult(Intent.createChooser(pick, "Select document"), REQUEST_DOC_SCAN);
                    }
                })
                .setNegativeButton("Cancel", null).show();
            speak("Opening scanner, sir.", "neutral");
            saveHistory(); return;
        }

        // ── [v10] Conversation Insights ───────────────────────────────────────
        if (ConversationInsights.isInsightCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            addJarvisMsg("Analysing our conversation history, sir…");
            showTyping(); saveHistory();
            ConversationInsights.analyse(history, httpClient, userProfile, new ConversationInsights.Callback() {
                @Override public void onResult(String insight) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean   = stripEmotionTag(insight);
                        String emotion = extractEmotion(insight);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean); speak(clean, emotion);
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        hideTyping(); addJarvisMsg(reason);
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
        }

        // ── [v9] Live Camera Vision ───────────────────────────────────────────
        if (lower.contains("what do you see") || lower.contains("look at this") ||
            lower.contains("live camera") || lower.contains("point camera") ||
            lower.contains("describe what's in front") || lower.contains("analyse camera") ||
            lower.contains("camera vision") || lower.contains("what's in front of me")) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = "[EMOTION:excited] Opening live camera, sir. Point it at anything and tap Analyse.";
            addJarvisMsg(stripEmotionTag(reply)); speak(reply, "excited"); saveHistory();
            Intent camIntent = new Intent(this, LiveCameraActivity.class);
            camIntent.putExtra(LiveCameraActivity.EXTRA_QUESTION, userText);
            startActivityForResult(camIntent, REQUEST_LIVE_CAM);
            return;
        }

        // ── [v9] Nearby Places ────────────────────────────────────────────────
        if (NearbyPlaces.isNearbyQuery(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            saveHistory();
            setState(OrbView.OrbState.THINKING);
            addJarvisMsg("Searching nearby locations, sir…");
            // Request location permission if needed
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                setState(OrbView.OrbState.IDLE);
                return;
            }
            NearbyPlaces.search(this, userText, new NearbyPlaces.Callback() {
                @Override public void onResult(String formatted) {
                    mainHandler.post(() -> {
                        String clean   = stripEmotionTag(formatted);
                        String emotion = extractEmotion(formatted);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean); speak(clean, emotion);
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        String clean = stripEmotionTag(reason);
                        addJarvisMsg(clean); speak(clean, "concerned");
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
        }

        // ── [v9] Music Control ────────────────────────────────────────────────
        if (MusicControl.isMusicCommand(userText)) {
            String reply = MusicControl.handle(this, userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v9] Battery Guardian threshold setting ───────────────────────────
        String batchPctStr = BatteryGuardian.parseThresholdCommand(userText);
        if (batchPctStr != null) {
            int pct = Integer.parseInt(batchPctStr);
            BatteryGuardian.setThreshold(this, pct);
            String reply = "[EMOTION:warm] Battery alert set at " + pct + "%, sir. I'll warn you when you drop below that.";
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "warm"); saveHistory(); return;
        }

        // ── [v9] Screen Reader ────────────────────────────────────────────────
        if (ScreenReader.isScreenReaderCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            if (!ScreenReader.isEnabled(this)) {
                String reply = "[EMOTION:neutral] I need Accessibility permission to read the screen, sir. Opening settings now.";
                addJarvisMsg(stripEmotionTag(reply)); speak(reply, "neutral");
                ScreenReader.requestEnable(this);
            } else {
                String screenText = ScreenReader.captureNow();
                if (screenText == null || screenText.trim().isEmpty()) {
                    String reply = "[EMOTION:neutral] The screen appears empty or I can't read it right now, sir. Try again in a moment.";
                    addJarvisMsg(stripEmotionTag(reply)); speak(reply, "neutral");
                } else {
                    // Send screen text to AI for summary
                    setState(OrbView.OrbState.THINKING); showTyping();
                    final String prompt = "The following is text from the user's current phone screen. " +
                        "Summarise or explain it naturally:\n\n" + screenText;
                    List<HistoryItem> screenHist = new ArrayList<>();
                    screenHist.add(new HistoryItem("user", prompt));
                    JarvisApi.ask(screenHist, null, MODE_BALANCED, userProfile, new JarvisApi.Callback() {
                        @Override public void onSuccess(String reply, String imageUrl) {
                            mainHandler.post(() -> {
                                hideTyping();
                                String clean   = stripEmotionTag(reply);
                                String emotion = extractEmotion(reply);
                                addJarvisMsg(clean); speak(clean, emotion);
                                setState(OrbView.OrbState.IDLE);
                            });
                        }
                        @Override public void onError(String error) {
                            mainHandler.post(() -> { hideTyping(); setState(OrbView.OrbState.IDLE); });
                        }
                    });
                }
            }
            saveHistory(); return;
        }

        // ── [v9] Smart Compose ────────────────────────────────────────────────
        if (SmartCompose.isComposeCommand(userText)) {
            SmartCompose.ComposeRequest compReq = SmartCompose.parse(userText);
            if (compReq != null) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                setState(OrbView.OrbState.THINKING);
                addJarvisMsg("Drafting your message, sir…");
                SmartCompose.generate(this, compReq, httpClient, userProfile,
                    new SmartCompose.Callback() {
                        @Override public void onDraftReady(String draft, SmartCompose.ComposeRequest req,
                                                           String number, String email) {
                            mainHandler.post(() -> {
                                setState(OrbView.OrbState.IDLE);
                                String preview = "Here's your draft to **" + req.contactName + "**, sir:\n\n" +
                                    "\"" + draft + "\"\n\n" +
                                    "Say **send it** to send, or **cancel** to discard.";
                                addJarvisMsg(preview);
                                speak("Draft ready. Shall I send it, sir?", "warm");
                                history.add(new HistoryItem("model", preview));
                                // Show confirm dialog
                                mainHandler.post(() -> {
                                    new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("◆ Send Message?")
                                        .setMessage("To: " + req.contactName + "\n\n" + draft)
                                        .setPositiveButton("Send ✓", (d, w) -> {
                                            if (req.channel == SmartCompose.Channel.EMAIL && email != null)
                                                SmartCompose.sendEmail(MainActivity.this, email, "From HENRY", draft);
                                            else if (req.channel == SmartCompose.Channel.SMS && number != null)
                                                SmartCompose.sendSms(MainActivity.this, number, draft);
                                            else if (number != null)
                                                SmartCompose.sendWhatsApp(MainActivity.this, number, draft);
                                            else
                                                Toast.makeText(MainActivity.this, "Contact not found in your phonebook", Toast.LENGTH_LONG).show();
                                            String sent = "[EMOTION:proud] Message sent to " + req.contactName + ", sir.";
                                            addJarvisMsg(stripEmotionTag(sent)); speak(sent, "proud");
                                        })
                                        .setNegativeButton("Cancel", (d, w) ->
                                            speak("Message discarded, sir.", "neutral"))
                                        .show();
                                });
                                saveHistory();
                            });
                        }
                        @Override public void onError(String error) {
                            mainHandler.post(() -> {
                                addJarvisMsg(error); speak(error, "concerned");
                                setState(OrbView.OrbState.IDLE);
                            });
                        }
                    });
                return;
            }
        }

        // ── Battery / DateTime (answered locally, no AI needed) ───────────────
        if (lower.contains("battery") || lower.contains("charge") || lower.contains("power level")) {
            String reply = DeviceCommands.getBatteryInfo(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "neutral"); saveHistory(); return;
        }
        if (lower.matches(".*what('s| is) the (time|date|day|datetime).*")
            || lower.equals("time") || lower.equals("date")) {
            String reply = DeviceCommands.getDateTime();
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "neutral"); saveHistory(); return;
        }

        // ── Flashlight ────────────────────────────────────────────────────────
        if (lower.contains("flashlight") || lower.contains("torch")) {
            boolean on = lower.contains("on") || lower.contains("enable") || lower.contains("turn on");
            boolean off= lower.contains("off")|| lower.contains("disable")|| lower.contains("turn off");
            String reply = (on && !off) ? DeviceCommands.setFlashlight(this, true)
                         : (off && !on) ? DeviceCommands.setFlashlight(this, false)
                         : DeviceCommands.toggleFlashlight(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            history.add(new HistoryItem("model", reply)); addJarvisMsg(reply);
            speak(reply, "neutral"); saveHistory(); return;
        }

        // ── Brightness ────────────────────────────────────────────────────────
        if (lower.contains("brightness")) {
            java.util.regex.Matcher bm =
                java.util.regex.Pattern.compile("(\\d+)").matcher(lower);
            int pct = bm.find() ? Integer.parseInt(bm.group(1)) : 70;
            String reply = DeviceCommands.setBrightness(this, pct);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            history.add(new HistoryItem("model", reply)); addJarvisMsg(reply);
            speak(reply, "neutral"); saveHistory(); return;
        }

        // ── Do Not Disturb ────────────────────────────────────────────────────
        if (lower.contains("do not disturb") || lower.contains("dnd")
            || lower.contains("silent mode") || lower.contains("quiet mode")) {
            boolean enable = !lower.contains("off") && !lower.contains("disable");
            String reply = DeviceCommands.setDoNotDisturb(this, enable);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            history.add(new HistoryItem("model", reply)); addJarvisMsg(reply);
            speak(reply, "neutral"); saveHistory(); return;
        }

        // ── Voice Timer ───────────────────────────────────────────────────────
        // Cancel timer
        if ((lower.contains("cancel") || lower.contains("stop")) && lower.contains("timer")) {
            String reply = VoiceTimer.cancel();
            if (reply == null) reply = "[EMOTION:neutral] No active timer, sir.";
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }
        // Timer status
        if ((lower.contains("how long") || lower.contains("time left") || lower.contains("timer status"))
            && VoiceTimer.isActive()) {
            String reply = VoiceTimer.status();
            if (reply == null) reply = "[EMOTION:neutral] No active timer, sir.";
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }
        // Start timer
        String timerReply = VoiceTimer.startFromText(this, userText, new VoiceTimer.Callback() {
            @Override public void onTick(long secondsLeft) { /* optional countdown UI */ }
            @Override public void onFinish(String label) {
                mainHandler.post(() -> {
                    String msg = label + " done, sir. Time's up.";
                    addJarvisMsg(msg);
                    speak(msg, "excited");
                });
            }
        });
        if (timerReply != null) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(timerReply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(timerReply)); saveHistory(); return;
        }

        // ── Voice Notes ───────────────────────────────────────────────────────
        String noteContent = VoiceNotes.parseSaveCommand(userText);
        if (noteContent != null) {
            String reply = VoiceNotes.save(this, noteContent);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }
        if (VoiceNotes.isRecallCommand(userText)) {
            String reply = VoiceNotes.readAll(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }
        if (VoiceNotes.isDeleteCommand(userText)) {
            String reply = VoiceNotes.deleteAll(this);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── News ──────────────────────────────────────────────────────────────
        if (lower.contains("news") || lower.contains("headlines") || lower.contains("briefing")) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            saveHistory();
            readNewsBriefing(); return;
        }

        // ── Screen always-on toggle via voice ─────────────────────────────────
        if (lower.contains("screen") && (lower.contains("always on") || lower.contains("stay on")
            || lower.contains("keep on") || lower.contains("keep awake"))) {
            toggleScreenAlwaysOn();
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            saveHistory(); return;
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

        JarvisApi.ask(apiHistory, imageB64, responseMode, userProfile, new JarvisApi.Callback() {
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

    // ── SOS Setup Dialog ─────────────────────────────────────────────────────
    private void showSOSSetupDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        android.widget.EditText etName = field(layout, "Contact name", "");
        android.widget.EditText etNum  = field(layout, "Phone number", "");
        android.widget.EditText etMsg  = field(layout, "Custom SOS message (optional)", "");
        new AlertDialog.Builder(this)
            .setTitle("◆ Set SOS Emergency Contact")
            .setMessage("This contact will receive your location + SMS when you say 'SOS'.")
            .setView(layout)
            .setPositiveButton("Save", (d, w) -> {
                String name = etName.getText().toString().trim();
                String num  = etNum.getText().toString().trim();
                String msg  = etMsg.getText().toString().trim();
                if (!name.isEmpty() && !num.isEmpty()) {
                    EmergencySOS.setContact(this, name, num);
                    if (!msg.isEmpty()) EmergencySOS.setCustomMessage(this, msg);
                    String reply = "[EMOTION:warm] Emergency contact saved: **" + name + "**. Say 'SOS' anytime, sir.";
                    addJarvisMsg(stripEmotionTag(reply)); speak(reply, "warm");
                } else {
                    Toast.makeText(this, "Name and number required", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null).show();
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
        needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            needed.add(Manifest.permission.ACTIVITY_RECOGNITION);
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
        if (batteryHandler != null && batteryChecker != null)
            batteryHandler.removeCallbacks(batteryChecker);
        if (fitnessTracker != null) fitnessTracker.stop();
        try { unregisterReceiver(wakeReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(notifReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
