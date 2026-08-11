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

    // Sleep tracker
    private SleepTracker  sleepTracker;
    private FitnessCoach  fitnessCoach;

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
        // [v17] Handle widget button command
        String widgetCmd = intent.getStringExtra("widget_command");
        if (widgetCmd != null && !widgetCmd.isEmpty()) {
            hideWelcome();
            final String cmd = widgetCmd;
            mainHandler.postDelayed(() -> askJarvis(cmd), 600);
        } else if (VoiceShortcutWidget.ACTION_MIC.equals(intent.getAction())) {
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

        // Daily Briefing — arrives automatically each morning by default;
        // reschedule is cheap/idempotent so calling this on every app start is fine.
        BriefingReceiver.scheduleNext(this);

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

        // Sleep tracker (singleton)
        sleepTracker  = SleepTracker.getInstance(this);
        fitnessCoach  = FitnessCoach.getInstance(this);

        // Button listeners
        if (orbView   != null) orbView.setOnClickListener(v -> toggleListening());
        if (btnMic    != null) btnMic.setOnClickListener(v -> toggleListening());
        if (btnSend   != null) btnSend.setOnClickListener(v -> sendText());
        if (btnClear  != null) btnClear.setOnClickListener(v -> showClearMenu());
        if (btnVoice  != null) btnVoice.setOnClickListener(v -> showVoicePicker());
        if (btnAttach != null) btnAttach.setOnClickListener(v -> showAttachDialog());

        // 🌍 Earth Map button
        // 🚀 Space Command button
        android.widget.TextView btnSpace = findViewById(R.id.btn_space);
        if (btnSpace != null) btnSpace.setOnClickListener(v ->
            startActivity(new android.content.Intent(this, SpaceActivity.class)));

        // 📈 Live Markets button
        android.widget.TextView btnMarkets = findViewById(R.id.btn_markets);
        if (btnMarkets != null) btnMarkets.setOnClickListener(v ->
            startActivity(new android.content.Intent(this, MarketsActivity.class)));

        // 🌐 Earth Radar button
        android.widget.TextView btnEarthRadar = findViewById(R.id.btn_earth_radar);
        if (btnEarthRadar != null) btnEarthRadar.setOnClickListener(v ->
            startActivity(new android.content.Intent(this, EarthRadarActivity.class)));

        // ✈ Flight Tracker button
        android.widget.TextView btnFlight = findViewById(R.id.btn_flight);
        if (btnFlight != null) btnFlight.setOnClickListener(v -> openFlightTracker(null));

        // 🧠 Brain button
        android.widget.TextView btnBrain = findViewById(R.id.btn_brain);
        if (btnBrain != null) btnBrain.setOnClickListener(v ->
            startActivity(new android.content.Intent(this, BrainActivity.class)));

        // 🌍 Earth Map button
        android.widget.TextView btnEarthMap = findViewById(R.id.btn_earth_map);
        if (btnEarthMap != null) btnEarthMap.setOnClickListener(v -> openEarthMap(null));

        // 🐾 Animal Scanner button
        android.widget.TextView btnAnimalScan = findViewById(R.id.btn_animal_scan);
        if (btnAnimalScan != null) btnAnimalScan.setOnClickListener(v -> openAnimalScanner());

        // 🌿 Plant Scanner button
        android.widget.TextView btnPlantScan = findViewById(R.id.btn_plant_scan);
        if (btnPlantScan != null) btnPlantScan.setOnClickListener(v -> openPlantScanner());

        // 🌀 Storm Tracker button
        android.widget.TextView btnStorm = findViewById(R.id.btn_storm);
        if (btnStorm != null) btnStorm.setOnClickListener(v -> startActivity(new Intent(this, StormActivity.class)));
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

        // [v17] Proactive suggestion — time/context aware nudge
        mainHandler.postDelayed(() -> {
            String nudge = ProactiveSuggestions.getSuggestion(this, userProfile, history);
            if (nudge != null) {
                addJarvisMsg(stripEmotionTag(nudge));
                speak(stripEmotionTag(nudge), extractEmotion(nudge));
            }
        }, 4500);

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
                    @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
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

        // 🐾 Animal Scanner result
        if (req == AnimalScannerActivity.REQUEST_CODE && data != null) {
            String animalResult = data.getStringExtra(AnimalScannerActivity.EXTRA_RESULT);
            if (animalResult != null && !animalResult.isEmpty()) {
                String clean = animalResult.replaceAll("\\[EMOTION:[^\\]]+\\]", "").trim();
                addJarvisMsg(clean);
                speak(clean, "excited");
                history.add(new HistoryItem("model", clean));
                saveHistory();
            }
            return;
        }

        // 🌿 Plant Scanner result
        if (req == PlantScannerActivity.REQUEST_CODE && data != null) {
            String plantResult = data.getStringExtra(PlantScannerActivity.EXTRA_RESULT);
            if (plantResult != null && !plantResult.isEmpty()) {
                String clean = plantResult.replaceAll("\\[EMOTION:[^\\]]+\\]", "").trim();
                addJarvisMsg(clean);
                speak(clean, "warm");
                history.add(new HistoryItem("model", clean));
                saveHistory();
            }
            return;
        }

        // 🌍 Earth Map result — country selected, ask HENRY about it
        if (req == EarthMapActivity.REQUEST_CODE && data != null) {
            String country = data.getStringExtra(EarthMapActivity.EXTRA_COUNTRY);
            if (country != null && !country.isEmpty()) {
                String prompt = "Tell me about " + country +
                    " — history, culture, top tourist spots, traditional food, and estimated population. Be engaging.";
                addUserMsg("Tell me about " + country);
                history.add(new HistoryItem("user", prompt));
                setState(OrbView.OrbState.THINKING); showTyping();
                JarvisApi.ask(history, null, responseMode, userProfile, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
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
            }
            return;
        }

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
                            @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
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
                                @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
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

    // ── Story Generation ──────────────────────────────────────────────────────
    private void generateAndReadStory(String userRequest, boolean readAloud) {
        setState(OrbView.OrbState.THINKING);
        showTyping();
        if (btnSend != null) btnSend.setEnabled(false);
        String storyPrompt = userRequest + "\n\nWrite an engaging, vivid story (3–5 paragraphs). "
            + "Give it a proper beginning, middle, and satisfying end. "
            + "Use descriptive language, dialogue where natural, and make it entertaining.";
        List<HistoryItem> stHist = new ArrayList<>(history);
        if (!stHist.isEmpty()) {
            stHist.set(stHist.size() - 1, new HistoryItem("user", storyPrompt));
        }
        JarvisApi.askV20(stHist, null, MODE_DETAILED, userProfile, "story",
                this, "warm", null, false, false, new JarvisApi.Callback() {
            @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
                mainHandler.post(() -> {
                    hideTyping();
                    String clean = stripEmotionTag(reply);
                    history.add(new HistoryItem("model", clean));
                    addJarvisMsg(clean);
                    saveHistory();
                    setState(OrbView.OrbState.IDLE);
                    if (btnSend != null) btnSend.setEnabled(true);
                    if (readAloud) {
                        speak(clean, "warm");
                    } else {
                        speak("Your story is ready, sir.", "warm");
                    }
                });
            }
            @Override public void onError(String e) {
                mainHandler.post(() -> {
                    hideTyping(); setState(OrbView.OrbState.IDLE);
                    if (btnSend != null) btnSend.setEnabled(true);
                });
            }
        });
    }

    // ── PDF Reading ───────────────────────────────────────────────────────────
    private void handlePdfAttachment(Uri pdfUri) {
        Toast.makeText(this, "Reading PDF…", Toast.LENGTH_SHORT).show();
        if (ivAttachPreview != null) ivAttachPreview.setVisibility(View.GONE);

        PdfReader.read(this, pdfUri, new PdfReader.Callback() {
            @Override public void onResult(String text, int pages) {
                mainHandler.post(() -> {
                    // Auto-summarize the PDF immediately
                    hideWelcome();
                    addUserMsg("📄 PDF attached (" + pages + " page" + (pages > 1 ? "s" : "") + ") — please summarize");
                    setState(OrbView.OrbState.THINKING);
                    showTyping();
                    if (btnSend != null) btnSend.setEnabled(false);

                    String summaryPrompt = "The user has attached a PDF document. Please summarize it clearly.\n\n"
                        + "PDF Content:\n" + text + "\n\n---\n\n"
                        + "Provide a structured summary with:\n"
                        + "**📄 Document Type** — what kind of document is this\n"
                        + "**📌 Key Points** — the most important information (bullet list)\n"
                        + "**📊 Details** — any notable facts, figures, dates, or names\n"
                        + "**💡 Summary** — 2-3 sentence overall summary\n"
                        + "Be thorough and accurate, sir.";

                    List<HistoryItem> pdfHist = new ArrayList<>();
                    pdfHist.add(new HistoryItem("user", summaryPrompt));

                    JarvisApi.askV20(pdfHist, null, MODE_DETAILED, userProfile, "document",
                            MainActivity.this, "neutral", null, false, false, new JarvisApi.Callback() {
                        @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
                            mainHandler.post(() -> {
                                hideTyping();
                                String clean = stripEmotionTag(reply);
                                history.add(new HistoryItem("user", "Summarize attached PDF"));
                                history.add(new HistoryItem("model", clean));
                                addJarvisMsg(clean);
                                speak("PDF summary ready, sir.", extractEmotion(reply));
                                // Keep PDF text for follow-up questions
                                pendingPdfText = text;
                                if (etInput != null) etInput.setHint("Ask more about the PDF…");
                                if (followUps != null && !followUps.isEmpty()) showFollowUpChips(followUps);
                                saveHistory(); setState(OrbView.OrbState.IDLE);
                                if (btnSend != null) btnSend.setEnabled(true);
                            });
                        }
                        @Override public void onError(String e) {
                            mainHandler.post(() -> {
                                hideTyping();
                                // Fallback: keep PDF for manual query
                                pendingPdfText = text;
                                if (etInput != null) etInput.setHint("Ask about the PDF…");
                                Toast.makeText(MainActivity.this, "PDF read (" + pages + " pages). Ask me anything.", Toast.LENGTH_LONG).show();
                                setState(OrbView.OrbState.IDLE);
                                if (btnSend != null) btnSend.setEnabled(true);
                            });
                        }
                    });
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
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Install Google app for voice recognition", Toast.LENGTH_LONG).show();
            return;
        }
        // Destroy previous instance cleanly
        if (speechRec != null) {
            try { speechRec.stopListening(); } catch (Exception ignored) {}
            try { speechRec.destroy(); } catch (Exception ignored) {}
            speechRec = null;
        }
        try {
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
                    // Error 5 = ERROR_CLIENT (recognizer busy/config issue) → silently retry once
                    // Error 11 = ERROR_SERVER_DISCONNECTED (transient network blip) → silently retry once
                    // Error 6 = ERROR_SPEECH_TIMEOUT, 7 = ERROR_NO_MATCH, 8 = ERROR_RECOGNIZER_BUSY → silent
                    if (error == SpeechRecognizer.ERROR_CLIENT ||
                        error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED) {
                        // Destroy and reinit recognizer, then auto-retry after short delay
                        if (speechRec != null) {
                            try { speechRec.destroy(); } catch (Exception ignored) {}
                            speechRec = null;
                        }
                        mainHandler.postDelayed(() -> startListening(), 800);
                    } else if (error != SpeechRecognizer.ERROR_NO_MATCH
                        && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        Toast.makeText(MainActivity.this, "Voice unavailable. Tap mic to retry.", Toast.LENGTH_SHORT).show();
                    }
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
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        speechRec.startListening(i);
        } catch (Exception e) {
            isListening = false;
            setState(OrbView.OrbState.IDLE);
            Toast.makeText(this, "Voice error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
        // ── Vision Intelligence — checked FIRST ───────────────────────────────
        {
            String vl = userText.toLowerCase(java.util.Locale.US).trim();
            int visionMode = 0;
            if (vl.matches(".*(classify|what is this|identify|image classification|label this).*"))
                visionMode = VisionActivity.MODE_CLASSIFY;
            else if (vl.matches(".*(detect object|object detection|find object|what.*objects|locate object).*"))
                visionMode = VisionActivity.MODE_DETECT;
            else if (vl.matches(".*(track|tracking|live track|object track).*") && vl.contains("object"))
                visionMode = VisionActivity.MODE_TRACK;
            else if (vl.matches(".*(face|facial|detect face|who is this|face recognition|face detect).*"))
                visionMode = VisionActivity.MODE_FACE;
            else if (vl.matches(".*(similar image|image retrieval|find similar|reverse image|content.*image).*"))
                visionMode = VisionActivity.MODE_RETRIEVE;
            else if (vl.matches(".*(open vision|vision hub|vision mode|visual intelligence).*"))
                visionMode = -1; // menu

            if (visionMode != 0) {
                android.content.Intent vi = new android.content.Intent(this, VisionActivity.class);
                if (visionMode > 0) vi.putExtra(VisionActivity.EXTRA_MODE, visionMode);
                startActivity(vi);
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String[] names = { "", "image classification", "object detection",
                    "object tracking", "facial recognition", "image retrieval" };
                String r = visionMode > 0
                    ? "Opening " + names[visionMode] + ", sir."
                    : "Opening Vision Intelligence hub, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r);
                speak(r, "excited"); saveHistory(); return;
            }
        }

        // ── HENRY Brain modules — checked FIRST before any other handler ──────
        {
            String bl = userText.toLowerCase(java.util.Locale.US).trim();
            if (bl.matches(".*(open|show|launch|start).*(brain|neural map|mind map).*")
                || bl.equals("brain") || bl.equals("henry brain") || bl.equals("open brain")) {
                startActivity(new android.content.Intent(this, BrainActivity.class));
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "Opening your neural command center, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r);
                speak(r, "excited"); saveHistory(); return;
            }
            if (bl.matches(".*(mental imagery|guided visualization|visuali[sz]e|imagine a scene).*")) {
                startActivity(new android.content.Intent(this, MentalImageryActivity.class));
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "Opening mental imagery mode, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r);
                speak(r, "warm"); saveHistory(); return;
            }
            if (bl.matches(".*(sensory substitution|synesthesia|color.*sound|cross.?modal).*")) {
                startActivity(new android.content.Intent(this, SensorySubstitutionActivity.class));
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "Activating sensory substitution, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r);
                speak(r, "excited"); saveHistory(); return;
            }
            if (bl.matches(".*(brain train|neural plasticity|neuroplasticity|cognitive exercise|brain exercise).*")) {
                startActivity(new android.content.Intent(this, NeuralPlasticityActivity.class));
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "Loading neural plasticity training, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r);
                speak(r, "excited"); saveHistory(); return;
            }
            if (bl.matches(".*(default mode|mind wander|daydream|open dmn|incubat|future self|empathy expand).*")
                || bl.equals("reflect") || bl.equals("mind wander")) {
                startActivity(new android.content.Intent(this, DefaultModeNetworkActivity.class));
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "Entering default mode network, sir. Let your mind expand.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r);
                speak(r, "warm"); saveHistory(); return;
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // v24 — FINAL BIG UPDATE HANDLERS
        // ══════════════════════════════════════════════════════════════════════

        // ── Dashboard ─────────────────────────────────────────────────────────
        {
            String bl = userText.toLowerCase().trim();
            if (bl.matches(".*(open|show|launch).*(dashboard|stats|usage|activity|analytics).*")
                || bl.equals("dashboard") || bl.equals("my stats")) {
                startActivity(new android.content.Intent(this, DashboardActivity.class));
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "[EMOTION:proud] Opening your HENRY dashboard, sir. All systems nominal.";
                history.add(new HistoryItem("model", stripEmotionTag(r))); addJarvisMsg(stripEmotionTag(r));
                speak(stripEmotionTag(r), "proud"); saveHistory(); return;
            }
        }

        // ── Security — Biometric Lock ─────────────────────────────────────────
        {
            String bl = userText.toLowerCase().trim();
            if (bl.contains("enable") && (bl.contains("lock") || bl.contains("biometric"))) {
                BiometricLock.setLockEnabled(this, true);
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "[EMOTION:serious] Biometric lock enabled, sir. HENRY is now secured.";
                history.add(new HistoryItem("model", stripEmotionTag(r))); addJarvisMsg(stripEmotionTag(r));
                speak(stripEmotionTag(r), "serious"); saveHistory(); return;
            }
            if (bl.contains("stealth mode") || (bl.contains("stealth") && bl.contains("on"))) {
                BiometricLock.setStealthMode(this, true);
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "[EMOTION:serious] Stealth mode activated, sir. HENRY will be invisible.";
                history.add(new HistoryItem("model", stripEmotionTag(r))); addJarvisMsg(stripEmotionTag(r));
                speak(stripEmotionTag(r), "serious"); saveHistory(); return;
            }
        }

        // ── Flight Tracker ────────────────────────────────────────────────────
        if (FlightTracker.isFlightQuery(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String flightNum = FlightTracker.extractFlightNumber(userText);
            if (flightNum != null) {
                addJarvisMsg("Tracking flight " + flightNum + "…");
                DashboardActivity.incrementMessages(this);
                FlightTracker.track(this, flightNum, new FlightTracker.Callback() {
                    @Override public void onResult(String summary) {
                        history.add(new HistoryItem("model", summary));
                        runOnUiThread(() -> {
                            addJarvisMsg(summary);
                            speak(summary.substring(0, Math.min(120, summary.length())), "neutral");
                        });
                        saveHistory();
                    }
                    @Override public void onError(String msg) {
                        runOnUiThread(() -> addJarvisMsg(msg));
                    }
                });
            } else {
                String r = "Please give me a flight number like 'EK201' and I'll track it for you, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r);
                speak(r, "neutral");
            }
            saveHistory(); return;
        }

        // ── Sports Tracker ────────────────────────────────────────────────────
        if (SportsTracker.isSportsQuery(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            addJarvisMsg("Fetching live sports data…");
            String sport = userText.toLowerCase().contains("basketball") || userText.toLowerCase().contains("nba")
                ? "basketball" : "football";
            SportsTracker.getLiveScores(sport, new SportsTracker.Callback() {
                @Override public void onResult(String result) {
                    history.add(new HistoryItem("model", result));
                    runOnUiThread(() -> {
                        addJarvisMsg(result);
                        speak(result.substring(0, Math.min(100, result.length())), "excited");
                    });
                    saveHistory();
                }
                @Override public void onError(String msg) {
                    runOnUiThread(() -> addJarvisMsg(msg));
                }
            });
            return;
        }

        // ── Package Tracker ───────────────────────────────────────────────────
        if (PackageTracker.isPackageQuery(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String trackNum = PackageTracker.extractTrackingNumber(userText);
            String courier  = PackageTracker.detectCourier(userText);
            if (trackNum != null) {
                PackageTracker.track(this, trackNum, courier, new PackageTracker.Callback() {
                    @Override public void onResult(String message, String url) {
                        history.add(new HistoryItem("model", message));
                        runOnUiThread(() -> {
                            addJarvisMsg(message);
                            speak("Opening tracker for " + trackNum, "neutral");
                            PackageTracker.openTracker(MainActivity.this, url);
                        });
                        saveHistory();
                    }
                });
            } else {
                String r = "Give me the tracking number and I'll open the tracker, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r); speak(r, "neutral");
            }
            saveHistory(); return;
        }

        // ── Smart Home ────────────────────────────────────────────────────────
        if (SmartHomeHelper.isSmartHomeQuery(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String cmd    = SmartHomeHelper.parseCommand(userText);
            String device = SmartHomeHelper.parseDevice(userText);
            String bl2    = userText.toLowerCase();
            if (bl2.contains("google home")) {
                SmartHomeHelper.openGoogleHome(this, new SmartHomeHelper.Callback() {
                    @Override public void onResult(String r) {
                        history.add(new HistoryItem("model", r)); runOnUiThread(() -> { addJarvisMsg(r); speak(r, "neutral"); });
                    }
                    @Override public void onError(String e) { runOnUiThread(() -> addJarvisMsg(e)); }
                });
            } else if (bl2.contains("alexa")) {
                SmartHomeHelper.openAlexa(this, new SmartHomeHelper.Callback() {
                    @Override public void onResult(String r) {
                        history.add(new HistoryItem("model", r)); runOnUiThread(() -> { addJarvisMsg(r); speak(r, "neutral"); });
                    }
                    @Override public void onError(String e) { runOnUiThread(() -> addJarvisMsg(e)); }
                });
            } else {
                String guide = SmartHomeHelper.getSmartHomeSetupGuide(device, cmd);
                history.add(new HistoryItem("model", guide)); addJarvisMsg(guide);
                speak("Smart home command received, sir.", "neutral");
            }
            saveHistory(); return;
        }

        // ── Social Media ──────────────────────────────────────────────────────
        if (SocialMediaHelper.isSocialMediaQuery(userText)) {
            String platform = SocialMediaHelper.detectPlatform(userText);
            String bl3      = userText.toLowerCase();
            // If asking to generate caption
            if (bl3.contains("caption") || bl3.contains("write") || bl3.contains("post") || bl3.contains("draft")) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                addJarvisMsg("Crafting your " + platform + " caption…");
                String topic = userText.replaceAll("(?i)(write|create|make|draft|generate|caption|for|post|on|instagram|twitter|tiktok|linkedin|facebook)", "").trim();
                if (topic.isEmpty()) topic = "my lifestyle";
                List<HistoryItem> msgs = new java.util.ArrayList<>();
                msgs.add(new HistoryItem("user", SocialMediaHelper.buildCaptionPrompt(platform, topic, null)));
                JarvisApi.ask(msgs, null, "balanced", null, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imgUrl, java.util.List<String> fu) {
                        String clean = reply.replaceAll("\\[EMOTION:[^]]+]", "").trim();
                        history.add(new HistoryItem("model", clean));
                        runOnUiThread(() -> { addJarvisMsg(clean); speak("Caption ready, sir.", "excited"); });
                        saveHistory();
                    }
                    @Override public void onError(String e) { runOnUiThread(() -> addJarvisMsg("Could not generate caption: " + e)); }
                });
                return;
            }
            // Direct share
            if (bl3.contains("tweet") || (bl3.contains("post") && bl3.contains("twitter"))) {
                String text = userText.replaceAll("(?i)(tweet|post on twitter|share on x)", "").trim();
                SocialMediaHelper.shareToTwitter(this, text.isEmpty() ? "Shared via H·E·N·R·Y™" : text);
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "Opening Twitter to post, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r); speak(r, "excited");
                saveHistory(); return;
            }
            if (bl3.contains("linkedin")) {
                String text = userText.replaceAll("(?i)(post on linkedin|share on linkedin)", "").trim();
                SocialMediaHelper.shareToLinkedIn(this, text.isEmpty() ? "Shared via H·E·N·R·Y™" : text);
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "Opening LinkedIn, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r); speak(r, "excited");
                saveHistory(); return;
            }
            if (bl3.contains("tiktok")) {
                SocialMediaHelper.openTikTok(this);
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                String r = "Opening TikTok, sir.";
                history.add(new HistoryItem("model", r)); addJarvisMsg(r); speak(r, "excited");
                saveHistory(); return;
            }
        }

        // ── Games ─────────────────────────────────────────────────────────────
        if (HenryGames.isGameQuery(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String bl4 = userText.toLowerCase().trim();
            // Active game responses
            if (HenryGames.isRiddleActive()) {
                if (bl4.contains("give up") || bl4.contains("skip")) {
                    String r = HenryGames.giveUpRiddle();
                    history.add(new HistoryItem("model", r)); addJarvisMsg(r); speak(r, "amused");
                    saveHistory(); return;
                }
                if (!bl4.contains("riddle") || bl4.startsWith("next")) {
                    if (bl4.startsWith("next") || bl4.contains("another")) {
                        String r = HenryGames.startRiddle();
                        history.add(new HistoryItem("model", r)); addJarvisMsg(r); speak("New riddle coming up!", "excited");
                    } else {
                        String r = HenryGames.checkRiddleAnswer(userText);
                        history.add(new HistoryItem("model", r)); addJarvisMsg(r);
                        speak(r.startsWith("✓") ? "Brilliant!" : "Hmm, not quite.", r.startsWith("✓") ? "proud" : "amused");
                    }
                    saveHistory(); return;
                }
            }
            if (HenryGames.isTriviaActive()) {
                String r = HenryGames.checkTriviaAnswer(userText);
                history.add(new HistoryItem("model", r)); addJarvisMsg(r);
                speak(r.contains("✓") ? "Correct!" : "Wrong.", r.contains("✓") ? "excited" : "amused");
                saveHistory(); return;
            }
            if (HenryGames.is20QActive()) {
                HenryGames.increment20Q();
                List<HistoryItem> msgs = new java.util.ArrayList<>();
                msgs.add(new HistoryItem("system", HenryGames.build20QSystemPrompt()));
                msgs.add(new HistoryItem("user", userText));
                JarvisApi.ask(msgs, null, "brief", null, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imgUrl, java.util.List<String> fu) {
                        String clean = reply.replaceAll("\\[EMOTION:[^]]+]", "").trim();
                        history.add(new HistoryItem("model", clean));
                        runOnUiThread(() -> { addJarvisMsg(clean); speak(clean, "amused"); });
                        saveHistory();
                    }
                    @Override public void onError(String e) { runOnUiThread(() -> addJarvisMsg(e)); }
                });
                return;
            }
            // Start new game
            String game = HenryGames.detectGame(userText);
            String r;
            switch (game) {
                case "riddle":   r = HenryGames.startRiddle(); speak("Riddle time!", "excited"); break;
                case "trivia":   r = HenryGames.startTrivia(); speak("Trivia tournament starts now!", "excited"); break;
                case "20q":      r = HenryGames.start20Questions(); speak("Twenty Questions — I'm thinking of something.", "amused"); break;
                case "wyr":      r = HenryGames.wouldYouRather(); speak("Would you rather…", "amused"); break;
                default:         r = HenryGames.startTrivia(); speak("Let's play trivia!", "excited"); break;
            }
            history.add(new HistoryItem("model", r)); addJarvisMsg(r);
            saveHistory(); return;
        }

        // ── Business Tools ────────────────────────────────────────────────────
        if (BusinessTools.isBusinessQuery(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            addJarvisMsg("Generating your business document…");
            String docType = BusinessTools.detectDocumentType(userText);
            String prompt;
            switch (docType) {
                case "invoice":      prompt = BusinessTools.buildInvoicePrompt(userText); break;
                case "contract":     prompt = BusinessTools.buildContractPrompt("service", userText); break;
                case "pitch":        prompt = BusinessTools.buildPitchDeckPrompt("my business", userText); break;
                case "business_plan":prompt = BusinessTools.buildBusinessPlanPrompt("my business", userText); break;
                case "swot":         prompt = BusinessTools.buildSWOTPrompt(userText); break;
                case "agenda":       prompt = BusinessTools.buildMeetingAgendaPrompt("our meeting", userText); break;
                case "press_release":prompt = BusinessTools.buildPressReleasePrompt("my announcement", userText); break;
                default:             prompt = "Generate a professional " + docType + " for: " + userText; break;
            }
            List<HistoryItem> msgs = new java.util.ArrayList<>();
            msgs.add(new HistoryItem("user", prompt));
            JarvisApi.ask(msgs, null, "detailed", null, new JarvisApi.Callback() {
                @Override public void onSuccess(String reply, String imgUrl, java.util.List<String> fu) {
                    String clean = reply.replaceAll("\\[EMOTION:[^]]+]", "").trim();
                    history.add(new HistoryItem("model", clean));
                    runOnUiThread(() -> {
                        addJarvisMsg(clean);
                        speak("Your " + docType.replace("_", " ") + " is ready, sir.", "proud");
                    });
                    saveHistory();
                }
                @Override public void onError(String e) { runOnUiThread(() -> addJarvisMsg("Could not generate document: " + e)); }
            });
            return;
        }

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

        // ── [v16] Code Runner ─────────────────────────────────────────────────
        if (CodeRunner.isRunCommand(userText)) {
            String[] parsed = CodeRunner.parse(userText);
            if (parsed != null) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                setState(OrbView.OrbState.THINKING);
                addJarvisMsg("Running **" + parsed[0] + "** code, sir…");
                showTyping(); saveHistory();
                CodeRunner.run(parsed[0], parsed[1], parsed[2], new CodeRunner.Callback() {
                    @Override public void onResult(String output, String lang, String code) {
                        mainHandler.post(() -> {
                            hideTyping();
                            String reply = "[EMOTION:excited] ✅ **" + capitalize(lang) + " Output:**\n```\n" + output + "\n```";
                            String clean = stripEmotionTag(reply);
                            history.add(new HistoryItem("model", clean));
                            addJarvisMsg(clean);
                            speak("Execution complete, sir.", "excited");
                            saveHistory(); setState(OrbView.OrbState.IDLE);
                        });
                    }
                    @Override public void onError(String reason) {
                        mainHandler.post(() -> {
                            hideTyping();
                            String clean = stripEmotionTag(reason);
                            addJarvisMsg(clean); speak(clean, "concerned");
                            setState(OrbView.OrbState.IDLE);
                        });
                    }
                });
                return;
            }
        }

        // ── [v16] Language Learning ───────────────────────────────────────────
        if (LanguageLearning.isLangCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            addJarvisMsg("Preparing your language lesson, sir…");
            showTyping(); saveHistory();
            LanguageLearning.handle(this, userText, httpClient, userProfile, new LanguageLearning.Callback() {
                @Override public void onResult(String content) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean   = stripEmotionTag(content);
                        String emotion = extractEmotion(content);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean); speak("Lesson ready, sir.", emotion);
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        hideTyping(); addJarvisMsg(stripEmotionTag(reason));
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
        }

        // ── [v16] In-App Browser ──────────────────────────────────────────────
        if (InAppBrowser.isBrowserCommand(userText)) {
            String url = InAppBrowser.parseUrl(userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = "[EMOTION:excited] Opening browser, sir.";
            addJarvisMsg(stripEmotionTag(reply)); speak(reply, "excited");
            InAppBrowser.open(this, url);
            saveHistory(); return;
        }

        // ── [v16] Car Mode ────────────────────────────────────────────────────
        if (lower.contains("car mode") || lower.equals("driving mode") ||
            lower.contains("start car mode") || lower.contains("hands free")) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = "[EMOTION:excited] Switching to car mode, sir. Stay safe.";
            addJarvisMsg(stripEmotionTag(reply)); speak(reply, "excited");
            startActivity(new Intent(this, CarModeActivity.class));
            saveHistory(); return;
        }

        // ── [v16] Breathing Exercise ──────────────────────────────────────────
        if (BreathingExercise.isBreathingCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            BreathingExercise.Mode mode = BreathingExercise.Mode.BOX;
            if (lower.contains("4-7-8") || lower.contains("478") || lower.contains("sleep breath"))
                mode = BreathingExercise.Mode.FOUR_SEVEN_EIGHT;
            else if (lower.contains("deep breath") || lower.contains("simple"))
                mode = BreathingExercise.Mode.DEEP;
            String reply = "[EMOTION:calm] Opening **" + mode.name + "**, sir. Follow the orb.";
            addJarvisMsg(stripEmotionTag(reply)); speak("Starting breathing exercise, sir.", "calm");
            final BreathingExercise.Mode finalMode = mode;
            mainHandler.postDelayed(() ->
                BreathingExercise.show(this, finalMode, text -> speak(text, "calm")), 800);
            saveHistory(); return;
        }

        // ── [v16] Sleep Tracker ───────────────────────────────────────────────
        if (SleepTracker.isSleepCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply;
            if (lower.contains("stop") || lower.contains("end") || lower.contains("report") ||
                lower.contains("how did") || lower.contains("quality") || lower.contains("score")) {
                if (sleepTracker.isActive()) reply = sleepTracker.stop();
                else reply = sleepTracker.generateReport();
            } else {
                reply = sleepTracker.start();
            }
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v16] Meal Planner ────────────────────────────────────────────────
        if (MealPlanner.isMealCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            boolean withList = userText.toLowerCase().contains("shopping") ||
                               userText.toLowerCase().contains("week");
            addJarvisMsg("Planning your meals, sir…");
            showTyping(); saveHistory();
            MealPlanner.generate(userText, httpClient, userProfile, withList, new MealPlanner.Callback() {
                @Override public void onResult(String plan) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean   = stripEmotionTag(plan);
                        String emotion = extractEmotion(plan);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean); speak("Meal plan ready, sir.", emotion);
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        hideTyping(); addJarvisMsg(stripEmotionTag(reason));
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
        }

        // ── [v15] Daily Summary ───────────────────────────────────────────────
        if (DailySummary.isSummaryCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            addJarvisMsg("Compiling your daily summary, sir…");
            showTyping(); saveHistory();
            DailySummary.generate(this, history, httpClient, userProfile, new DailySummary.Callback() {
                @Override public void onResult(String summary) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean   = stripEmotionTag(summary);
                        String emotion = extractEmotion(summary);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean); speak(clean, emotion);
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        hideTyping(); addJarvisMsg(stripEmotionTag(reason));
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
        }

        // ── [v15] Debate Mode ─────────────────────────────────────────────────
        if (DebateMode.isDebateCommand(userText)) {
            String topic = DebateMode.parseTopic(userText);
            if (!topic.isEmpty()) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                setState(OrbView.OrbState.THINKING);
                addJarvisMsg("Preparing debate on **" + topic + "**, sir…");
                showTyping(); saveHistory();
                DebateMode.debate(topic, httpClient, userProfile, new DebateMode.Callback() {
                    @Override public void onResult(String debate) {
                        mainHandler.post(() -> {
                            hideTyping();
                            String clean   = stripEmotionTag(debate);
                            String emotion = extractEmotion(debate);
                            history.add(new HistoryItem("model", clean));
                            addJarvisMsg(clean); speak("Debate ready, sir.", emotion);
                            saveHistory(); setState(OrbView.OrbState.IDLE);
                        });
                    }
                    @Override public void onError(String reason) {
                        mainHandler.post(() -> {
                            hideTyping(); addJarvisMsg(stripEmotionTag(reason));
                            setState(OrbView.OrbState.IDLE);
                        });
                    }
                });
                return;
            }
        }

        // ── [v15] Focus Mode ──────────────────────────────────────────────────
        if (FocusMode.isFocusCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply;
            boolean stopping = lower.contains("stop") || lower.contains("end") ||
                               lower.contains("disable") || lower.contains("exit");
            if (stopping && FocusMode.isActive()) {
                reply = FocusMode.deactivate(this);
            } else if (!stopping) {
                reply = FocusMode.activate(this, userProfile);
            } else {
                reply = "[EMOTION:neutral] Focus mode isn't active, sir.";
            }
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v15] Commands Dashboard ──────────────────────────────────────────
        if (CommandsDashboard.isDashboardCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            // Check for category-specific request
            String[] categories = {"AI","Weather","Maps","Finance","Health","Productivity",
                                   "Notes","Device","Contacts","Camera","Apps","Memory"};
            String catMatch = null;
            for (String cat : categories)
                if (lower.contains(cat.toLowerCase(Locale.US))) { catMatch = cat; break; }

            String reply;
            if (catMatch != null) {
                reply = CommandsDashboard.formatCategory(catMatch);
            } else {
                reply = CommandsDashboard.formatSummary();
            }
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak("Here's the command overview, sir.", "excited"); saveHistory(); return;
        }

        // ── [v15] Smart Clipboard ─────────────────────────────────────────────
        if (ClipboardManager2.isCopyCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            if (lower.contains("history") || lower.contains("what did i copy")) {
                String hist = ClipboardManager2.getHistory(this);
                String clean = stripEmotionTag(hist);
                addJarvisMsg(clean); speak("Here's your clipboard history, sir.", "neutral");
                history.add(new HistoryItem("model", clean)); saveHistory(); return;
            }
            // Find last HENRY message to copy
            String lastReply = null;
            for (int i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i).type == Message.TYPE_JARVIS) {
                    lastReply = messages.get(i).text; break;
                }
            }
            String reply = ClipboardManager2.copy(this, lastReply, "HENRY reply");
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v14] Notification Summary ────────────────────────────────────────
        if (NotificationSummary.isSummaryCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String summary = NotificationSummary.summarise(this);
            String clean   = stripEmotionTag(summary);
            String emotion = extractEmotion(summary);
            history.add(new HistoryItem("model", clean));
            addJarvisMsg(clean);
            speak(NotificationSummary.getSpokenSummary(this), emotion);
            saveHistory(); return;
        }

        // ── [v14] Task Manager ────────────────────────────────────────────────
        if (TaskManager.isTaskCommand(userText)) {
            String reply = TaskManager.handle(this, userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v14] Word of the Day ─────────────────────────────────────────────
        if (WordOfTheDay.isWordCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            saveHistory();
            WordOfTheDay.fetch(this, result -> {
                mainHandler.post(() -> {
                    String clean   = stripEmotionTag(result);
                    String emotion = extractEmotion(result);
                    history.add(new HistoryItem("model", clean));
                    addJarvisMsg(clean); speak(clean, emotion);
                    saveHistory(); setState(OrbView.OrbState.IDLE);
                });
            });
            return;
        }

        // ── [v14] Currency Converter ──────────────────────────────────────────
        if (CurrencyConverter.isCurrencyCommand(userText)) {
            String[] parsed = CurrencyConverter.parse(userText);
            if (parsed != null) {
                double amount = Double.parseDouble(parsed[0]);
                String from = parsed[1], to = parsed[2];
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                setState(OrbView.OrbState.THINKING);
                addJarvisMsg("Converting, sir…");
                saveHistory();
                CurrencyConverter.convert(amount, from, to, new CurrencyConverter.Callback() {
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
        }

        // ── [v14] Study Mode ──────────────────────────────────────────────────
        if (StudyMode.isStudyCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            showTyping(); saveHistory();
            StudyMode.ask(userText, "balanced", httpClient, userProfile, new StudyMode.Callback() {
                @Override public void onResult(String response) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean   = stripEmotionTag(response);
                        String emotion = extractEmotion(response);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean); speak(clean, emotion);
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean = stripEmotionTag(reason);
                        addJarvisMsg(clean); speak(clean, "concerned");
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
        }

        // ── [v13] Navigation ──────────────────────────────────────────────────
        if (NavigationHelper.isNavCommand(userText)) {
            String dest = NavigationHelper.parseDestination(userText);
            boolean waze = lower.contains("waze");
            String reply = NavigationHelper.navigate(this, dest, waze);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "excited"); saveHistory(); return;
        }

        // ── [v13] Travel Time ─────────────────────────────────────────────────
        if (TravelTime.isTravelTimeQuery(userText)) {
            String dest = TravelTime.parseDestination(userText);
            if (!dest.isEmpty()) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                setState(OrbView.OrbState.THINKING);
                addJarvisMsg("Calculating travel time to **" + dest + "**, sir…");
                saveHistory();
                TravelTime.query(this, dest, new TravelTime.Callback() {
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
        }

        // ── [v13] Location Share ──────────────────────────────────────────────
        if (LocationShare.isShareCommand(userText)) {
            String contact = LocationShare.parseContact(userText);
            boolean wa = LocationShare.isWhatsAppPreferred(userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            if (contact.isEmpty()) {
                // Share via system sheet
                LocationShare.shareViaSheet(this);
                String reply = "[EMOTION:warm] Sharing your location, sir.";
                String clean = stripEmotionTag(reply);
                addJarvisMsg(clean); speak(clean, "warm");
            } else {
                String reply = LocationShare.share(this, contact, wa);
                String clean = stripEmotionTag(reply);
                history.add(new HistoryItem("model", clean));
                addJarvisMsg(clean); speak(clean, extractEmotion(reply));
            }
            saveHistory(); return;
        }

        // ── Protocols ────────────────────────────────────────────────────────
        if (ProtocolManager.isCreateCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = ProtocolManager.create(this, userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "proud");
            saveHistory();
            return;
        }
        if (ProtocolManager.isListCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = ProtocolManager.list(this);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "neutral");
            saveHistory();
            return;
        }
        if (ProtocolManager.isDeleteCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = ProtocolManager.delete(this, userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "neutral");
            saveHistory();
            return;
        }
        String matchedProtocol = ProtocolManager.matchRunCommand(this, userText);
        if (matchedProtocol != null) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            List<String> steps = ProtocolManager.getSteps(this, matchedProtocol);
            String announce = "[EMOTION:excited] Running \"" + matchedProtocol + "\", sir — " + steps.size() + " step" + (steps.size() != 1 ? "s" : "") + ".";
            String clean = stripEmotionTag(announce);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "excited");
            saveHistory();
            // Staggered execution — each step replays through the normal command
            // pipeline exactly as if typed, so it supports everything HENRY
            // already understands with no per-feature wiring needed.
            for (int i = 0; i < steps.size(); i++) {
                String step = steps.get(i);
                mainHandler.postDelayed(() -> askJarvis(step), i * 3500L);
            }
            return;
        }

        // ── Daily Briefing ───────────────────────────────────────────────────
        if (DailyBriefing.isBriefingCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            saveHistory();
            addJarvisMsg("Pulling your briefing together, sir…");
            setState(OrbView.OrbState.THINKING);
            DailyBriefing.generate(this, briefing -> mainHandler.post(() -> {
                setState(OrbView.OrbState.IDLE);
                String clean = stripEmotionTag(briefing);
                java.util.regex.Matcher em = java.util.regex.Pattern.compile("\\[EMOTION:(\\w+)\\]").matcher(briefing);
                String emotion = em.find() ? em.group(1) : "neutral";
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak(clean, emotion);
                saveHistory();
            }));
            return;
        }

        // ── Periodic Table / Element Mixer ──────────────────────────────────
        String periodicLower = userText.toLowerCase(Locale.US);
        if (periodicLower.contains("periodic table") || periodicLower.contains("element mixer") ||
            periodicLower.contains("chemistry lab") || periodicLower.contains("mix elements")) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = "[EMOTION:excited] Opening the periodic table, sir. Drag one element onto another to see what they form.";
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, "excited");
            saveHistory();
            startActivity(new Intent(this, PeriodicTableActivity.class));
            return;
        }

        // ── Storm Tracker ────────────────────────────────────────────────────
        String stormLower = userText.toLowerCase(Locale.US);
        if (stormLower.contains("storm tracker") || stormLower.contains("track storm") ||
            stormLower.contains("typhoon tracker") || stormLower.contains("hurricane tracker") ||
            stormLower.contains("cyclone tracker") || stormLower.contains("storm activity") ||
            stormLower.contains("active storms") || stormLower.contains("active typhoons") ||
            stormLower.contains("active hurricanes") || stormLower.contains("active cyclones")) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = "[EMOTION:excited] Pulling up the storm tracker, sir.";
            addJarvisMsg(stripEmotionTag(reply)); speak(stripEmotionTag(reply), "excited");
            saveHistory();
            startActivity(new Intent(this, StormActivity.class));
            return;
        }

        // ── [v13] Place Details ───────────────────────────────────────────────
        if (PlaceDetails.isPlaceQuery(userText)) {
            String placeQ = PlaceDetails.parseQuery(userText);
            if (!placeQ.isEmpty()) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                setState(OrbView.OrbState.THINKING);
                addJarvisMsg("Looking up **" + placeQ + "**, sir…");
                saveHistory();
                PlaceDetails.fetch(placeQ, new PlaceDetails.Callback() {
                    @Override public void onResult(String summary, double lat, double lon, String name) {
                        mainHandler.post(() -> {
                            String clean   = stripEmotionTag(summary);
                            String emotion = extractEmotion(summary);
                            history.add(new HistoryItem("model", clean));
                            addJarvisMsg(clean);
                            speak(clean, emotion);
                            // If we have coordinates, offer to show on map
                            if (lat != 0 || lon != 0) {
                                final double fLat = lat; final double fLon = lon;
                                final String fName = name;
                                mainHandler.postDelayed(() -> {
                                    new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Show on Map?")
                                        .setMessage("Open " + fName + " on the map, sir?")
                                        .setPositiveButton("Open Map", (d, w) -> {
                                            Intent mapI = new Intent(MainActivity.this, MapActivity.class);
                                            mapI.putExtra(MapActivity.EXTRA_LAT, fLat);
                                            mapI.putExtra(MapActivity.EXTRA_LON, fLon);
                                            mapI.putExtra(MapActivity.EXTRA_LABEL, fName);
                                            startActivity(mapI);
                                        })
                                        .setNegativeButton("No thanks", null).show();
                                }, 1500);
                            }
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
        }

        // ── [v13] In-App Map (open map / show map) ────────────────────────────
        if (lower.startsWith("open map") || lower.startsWith("show map") ||
            lower.startsWith("show me the map") || lower.equals("map") ||
            lower.startsWith("map of ") || lower.startsWith("find on map")) {
            String mapQuery = userText
                .replaceAll("(?i)^(open|show|find on)\\s+map(\\s+of)?\\s*", "").trim();
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = "[EMOTION:excited] Opening map, sir.";
            addJarvisMsg(stripEmotionTag(reply)); speak(reply, "excited");
            Intent mapI = new Intent(this, MapActivity.class);
            if (!mapQuery.isEmpty()) mapI.putExtra(MapActivity.EXTRA_QUERY, mapQuery);
            startActivity(mapI);
            saveHistory(); return;
        }

        // ── [v12] Speed Test ──────────────────────────────────────────────────
        if (SpeedTest.isSpeedTestCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            addJarvisMsg("Running speed test, sir… this takes a few seconds.");
            saveHistory();
            SpeedTest.run(this, new SpeedTest.Callback() {
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

        // ── [v12] Password Vault ──────────────────────────────────────────────
        if (PasswordVault.isVaultCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String[] cmd = PasswordVault.parseCommand(userText);
            if (cmd == null) {
                addJarvisMsg("Try: 'Save password for Netflix: mypass123' or 'What's my Netflix password?'");
                saveHistory(); return;
            }
            // Generate — no PIN needed
            if ("generate".equals(cmd[0])) {
                java.util.regex.Matcher lenM = java.util.regex.Pattern.compile("(\\d+)").matcher(userText);
                int len = lenM.find() ? Integer.parseInt(lenM.group(1)) : 16;
                len = Math.max(8, Math.min(32, len));
                boolean syms = !userText.toLowerCase().contains("no symbol") &&
                               !userText.toLowerCase().contains("letters only");
                String pwd = PasswordVault.generate(len, syms);
                String reply = "[EMOTION:proud] Generated password: **" + pwd + "**\n\nSay 'Save password for [service]: " + pwd + "' to store it, sir.";
                String clean = stripEmotionTag(reply);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak("Password generated, sir.", "proud"); saveHistory(); return;
            }
            // All other actions need PIN
            showVaultPinDialog(cmd);
            saveHistory(); return;
        }

        // ── [v12] Trivia Quiz ─────────────────────────────────────────────────
        // Check answer first if question is active
        if (TriviaQuiz.hasActiveQuestion() && !TriviaQuiz.isQuizCommand(userText)) {
            String result = TriviaQuiz.checkAnswer(userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(result);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(result)); saveHistory(); return;
        }
        if (TriviaQuiz.isQuizCommand(userText)) {
            if (userText.toLowerCase().contains("score")) {
                String score = TriviaQuiz.getScore();
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                addJarvisMsg(score); speak(score, "neutral"); saveHistory(); return;
            }
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            addJarvisMsg("Fetching trivia question, sir…");
            saveHistory();
            TriviaQuiz.fetchQuestion(userText, new TriviaQuiz.Callback() {
                @Override public void onQuestion(String question, java.util.List<String> options, String correct) {
                    mainHandler.post(() -> {
                        String formatted = TriviaQuiz.formatQuestion(question, options);
                        String clean     = stripEmotionTag(formatted);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean);
                        speak("Here's your question, sir. What's your answer?", "excited");
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

        // ── [v12] Health Calculator ───────────────────────────────────────────
        if (HealthCalculator.isHealthCommand(userText)) {
            String reply = HealthCalculator.handle(this, userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v12] Expense Tracker ─────────────────────────────────────────────
        if (ExpenseTracker.isExpenseCommand(userText)) {
            String reply = ExpenseTracker.handle(this, userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v12] Recipe Generator ────────────────────────────────────────────
        if (RecipeGenerator.isRecipeCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            addJarvisMsg("Generating recipe, sir…");
            showTyping(); saveHistory();
            RecipeGenerator.generate(userText, httpClient, userProfile, new RecipeGenerator.Callback() {
                @Override public void onResult(String recipe) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean   = stripEmotionTag(recipe);
                        String emotion = extractEmotion(recipe);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean); speak("Recipe ready, sir.", emotion);
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean = stripEmotionTag(reason);
                        addJarvisMsg(clean); speak(clean, "concerned");
                        setState(OrbView.OrbState.IDLE);
                    });
                }
            });
            return;
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
                        @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
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

        // ── [v17] Smart Memory ────────────────────────────────────────────────
        SmartMemory.learnFromMessage(this, userText);
        if (SmartMemory.isMemoryCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = SmartMemory.handle(this, userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v17] AI Image Generation ─────────────────────────────────────────
        if (ImageGenerator.isImageCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING);
            String prompt = ImageGenerator.extractPrompt(userText);
            String imgUrl = ImageGenerator.buildImageUrl(prompt);
            String teaser = "[EMOTION:excited] Generating \"" + prompt + "\" for you, sir…";
            addJarvisMsg(stripEmotionTag(teaser));
            speak("Generating image now, sir.", "excited");
            // Show image inline as URL image
            messages.add(new Message(Message.TYPE_URL_IMAGE, null, imgUrl));
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            String done = "[EMOTION:proud] Here's your image, sir! Tap to view full size.";
            history.add(new HistoryItem("model", "Generated image: " + prompt));
            addJarvisMsg(stripEmotionTag(done));
            setState(OrbView.OrbState.IDLE); saveHistory(); return;
        }

        // ── [v17] Meeting Recorder ────────────────────────────────────────────
        if (MeetingRecorder.isRecordingCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            if (lower.contains("stop") || lower.contains("end")) {
                if (MeetingRecorder.isRecording()) {
                    addJarvisMsg("Stopping and summarising, sir…");
                    speak("Stopping recording, sir.", "neutral");
                    setState(OrbView.OrbState.THINKING); showTyping();
                    MeetingRecorder.stopAndSummarise(this, httpClient, new MeetingRecorder.Callback() {
                        @Override public void onResult(String summary) {
                            mainHandler.post(() -> {
                                hideTyping();
                                String clean = stripEmotionTag(summary);
                                history.add(new HistoryItem("model", clean));
                                addJarvisMsg(clean);
                                speak("Summary ready, sir.", extractEmotion(summary));
                                saveHistory(); setState(OrbView.OrbState.IDLE);
                            });
                        }
                        @Override public void onError(String e) {
                            mainHandler.post(() -> { hideTyping(); addJarvisMsg(e); saveHistory(); setState(OrbView.OrbState.IDLE); });
                        }
                    });
                } else {
                    String r = "[EMOTION:neutral] No active recording to stop, sir.";
                    addJarvisMsg(stripEmotionTag(r)); speak(stripEmotionTag(r), "neutral");
                }
            } else if (lower.contains("list") || lower.contains("my recording")) {
                String r = MeetingRecorder.listRecordings(this);
                String clean = stripEmotionTag(r);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak("Here are your recordings, sir.", "neutral");
            } else if (lower.contains("status")) {
                String r = MeetingRecorder.isRecording()
                    ? "[EMOTION:excited] Currently recording, sir. Say 'stop recording' when done."
                    : "[EMOTION:neutral] No active recording, sir. Say 'record meeting' to start.";
                String clean = stripEmotionTag(r); history.add(new HistoryItem("model", clean));
                addJarvisMsg(clean); speak(clean, extractEmotion(r));
            } else {
                // Start recording
                String r = MeetingRecorder.startRecording(this);
                String clean = stripEmotionTag(r);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak(clean, extractEmotion(r));
            }
            saveHistory(); return;
        }

        // ── [v17] Smart File Manager ──────────────────────────────────────────
        if (SmartFileManager.isFileCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String result = SmartFileManager.handle(this, userText);
            if (result != null && result.startsWith("SHARE:")) {
                String path = result.substring(6);
                android.content.Intent shareIntent = SmartFileManager.buildShareIntent(this, path);
                if (shareIntent != null) startActivity(shareIntent);
                result = "[EMOTION:neutral] Sharing that file, sir.";
            }
            String clean = stripEmotionTag(result);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean.length() > 60 ? "Here are your files, sir." : clean, extractEmotion(result));
            saveHistory(); return;
        }

        // ── [v17] Fitness Coach ────────────────────────────────────────────────
        if (FitnessCoach.isFitnessCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String sync = fitnessCoach.handle(userText, userProfile, new FitnessCoach.Callback() {
                @Override public void onResult(String reply) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean = stripEmotionTag(reply);
                        history.add(new HistoryItem("model", clean));
                        addJarvisMsg(clean);
                        speak("Workout plan ready, sir.", extractEmotion(reply));
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String e) {
                    mainHandler.post(() -> { hideTyping(); addJarvisMsg(e); setState(OrbView.OrbState.IDLE); });
                }
            });
            if (sync != null) {
                String clean = stripEmotionTag(sync);
                history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                speak(clean, extractEmotion(sync)); saveHistory();
            } else {
                setState(OrbView.OrbState.THINKING); showTyping();
            }
            return;
        }

        // ── [v17] Live Subtitles ───────────────────────────────────────────────
        if (LiveSubtitles.isSubtitleCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply;
            if (lower.contains("stop") || lower.contains("off") || lower.contains("hide")) {
                stopService(new android.content.Intent(this, LiveSubtitles.class));
                reply = "[EMOTION:neutral] Live captions off, sir.";
            } else if (LiveSubtitles.isRunning()) {
                reply = "[EMOTION:neutral] Live captions are already running, sir.";
            } else {
                if (android.provider.Settings.canDrawOverlays(this)) {
                    android.content.Intent si = new android.content.Intent(this, LiveSubtitles.class);
                    si.setAction(LiveSubtitles.ACTION_START);
                    startService(si);
                    reply = "[EMOTION:excited] Live captions on, sir! Subtitles will appear at the bottom of your screen.";
                } else {
                    android.content.Intent perm = new android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(perm);
                    reply = "[EMOTION:neutral] Please grant overlay permission, sir, then try again.";
                }
            }
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── [v17] WhatsApp Helper ─────────────────────────────────────────────
        if (WhatsAppHelper.isWhatsAppCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING); showTyping();
            addJarvisMsg("Drafting your message, sir…");
            WhatsAppHelper.draftReply(userText, userProfile, new WhatsAppHelper.Callback() {
                @Override public void onResult(String draft, String contact) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String display = "[EMOTION:excited] **WhatsApp draft for " + contact + ":**\n\n" + draft +
                            "\n\n_Tap to open WhatsApp and send, sir._";
                        String clean = stripEmotionTag(display);
                        history.add(new HistoryItem("model", draft));
                        addJarvisMsg(clean);
                        speak("Draft ready, sir.", "excited");
                        // Open WhatsApp with pre-filled message
                        android.content.Intent wa = WhatsAppHelper.buildWhatsAppIntent(MainActivity.this, contact, draft);
                        if (wa != null) {
                            try { startActivity(wa); } catch (Exception ignored) {}
                        }
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String e) {
                    mainHandler.post(() -> { hideTyping(); addJarvisMsg(e); setState(OrbView.OrbState.IDLE); });
                }
            });
            return;
        }

        // ── [v17] Email Drafter ───────────────────────────────────────────────
        if (EmailDrafter.isEmailCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING); showTyping();
            addJarvisMsg("Composing your email, sir…");
            EmailDrafter.draft(userText, userProfile, new EmailDrafter.Callback() {
                @Override public void onResult(String subject, String body, String recipient) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String display = "[EMOTION:excited] **Email Draft:**\n**Subject:** " + subject + "\n\n" + body;
                        String clean = stripEmotionTag(display);
                        history.add(new HistoryItem("model", "Email: " + subject));
                        addJarvisMsg(clean);
                        speak("Email drafted, sir. Opening your mail app now.", "excited");
                        android.content.Intent emailIntent = EmailDrafter.buildEmailIntent(recipient, subject, body);
                        try { startActivity(emailIntent); } catch (Exception ignored) {}
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String e) {
                    mainHandler.post(() -> { hideTyping(); addJarvisMsg(e); setState(OrbView.OrbState.IDLE); });
                }
            });
            return;
        }

        // ── [v17] Social Caption Generator ────────────────────────────────────
        if (SocialCaptionGenerator.isCaptionCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            setState(OrbView.OrbState.THINKING); showTyping();
            addJarvisMsg("Writing your caption, sir…");
            SocialCaptionGenerator.generate(userText, userProfile, new SocialCaptionGenerator.Callback() {
                @Override public void onResult(String caption) {
                    mainHandler.post(() -> {
                        hideTyping();
                        String clean = stripEmotionTag(caption);
                        history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                        speak("Caption ready, sir.", extractEmotion(caption));
                        saveHistory(); setState(OrbView.OrbState.IDLE);
                    });
                }
                @Override public void onError(String e) {
                    mainHandler.post(() -> { hideTyping(); addJarvisMsg(e); setState(OrbView.OrbState.IDLE); });
                }
            });
            return;
        }

        // ── [v17] Birthday Tracker ────────────────────────────────────────────
        if (BirthdayTracker.isBirthdayCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = BirthdayTracker.handle(this, userText, userProfile);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean.length() > 80 ? "Here's the birthday info, sir." : clean, extractEmotion(reply));
            saveHistory(); return;
        }

        // ── [v17] Voice Shortcut Widget setup ─────────────────────────────────
        if (VoiceShortcutWidget.isWidgetCommand(userText)) {
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            String reply = VoiceShortcutWidget.handleWidgetSetup(this, userText);
            String clean = stripEmotionTag(reply);
            history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
            speak(clean, extractEmotion(reply)); saveHistory(); return;
        }

        // ── Google Workspace (Docs / Sheets / Slides) ─────────────────────────
        if (GoogleWorkspaceHelper.isDocCommand(userText)) {
            GoogleWorkspaceHelper.DocType docType = GoogleWorkspaceHelper.detectType(userText);
            String docTitle = GoogleWorkspaceHelper.extractTitle(userText);
            history.add(new HistoryItem("user", userText)); addUserMsg(userText);
            saveHistory();
            String typeName = GoogleWorkspaceHelper.typeName(docType);
            addJarvisMsg("Creating your " + typeName + " titled **\"" + docTitle + "\"**…");
            speak("Creating your " + typeName + " now, sir.", "excited");
            setState(OrbView.OrbState.THINKING);
            // Was always passed null here — nothing ever built real content from
            // the chat, so every doc/slide came out empty no matter what was asked.
            String docContent = GoogleWorkspaceHelper.buildContentFromHistory(history, docType);
            GoogleWorkspaceHelper.create(docTitle, docType, docContent, new GoogleWorkspaceHelper.Callback() {
                @Override public void onSuccess(String url, String title, GoogleWorkspaceHelper.DocType type) {
                    mainHandler.post(() -> {
                        setState(OrbView.OrbState.IDLE);
                        // No Google service account configured server-side means the
                        // backend can only hand back a blank docs.new/sheets.new/
                        // slides.new link — it has no way to pre-fill a brand new
                        // Google doc's content via URL. Bridge that gap by copying
                        // the content to the clipboard so it's one paste away.
                        boolean isBlankShortcut = url.equals("https://docs.new")
                            || url.equals("https://sheets.new") || url.equals("https://slides.new");
                        if (isBlankShortcut && docContent != null && !docContent.isEmpty()) {
                            android.content.ClipboardManager cm =
                                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(android.content.ClipData.newPlainText(typeName + " content", docContent));
                        }
                        String msg = isBlankShortcut
                            ? "[EMOTION:proud]\n**" + typeName + " opened!**\n\nI don't have Google credentials configured yet, so I couldn't create and fill it automatically — but your content is copied to the clipboard. Just paste it in (long-press → Paste)."
                            : "[EMOTION:proud]\n**" + typeName + " created!**\n\nTitle: " + title + "\n\n[Open →](" + url + ")";
                        String clean = stripEmotionTag(msg);
                        history.add(new HistoryItem("model", clean)); addJarvisMsg(clean);
                        speak(isBlankShortcut
                            ? "Opened it, sir — your content's on the clipboard, just paste it in."
                            : "Your " + typeName + " is ready, sir. Opening now.", "proud");
                        saveHistory();
                        // Show dialog to open in browser
                        new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle(typeName + (isBlankShortcut ? " Opened — Content Copied" : " Ready"))
                            .setMessage(isBlankShortcut
                                ? "Paste (long-press → Paste) into the new " + typeName + " to add your content."
                                : "\"" + title + "\" has been created.\n\n" + url)
                            .setPositiveButton("Open", (d, w) -> GoogleWorkspaceHelper.openInBrowser(MainActivity.this, url))
                            .setNegativeButton("Later", null)
                            .show();
                    });
                }
                @Override public void onError(String reason) {
                    mainHandler.post(() -> {
                        setState(OrbView.OrbState.IDLE);
                        // Fallback: open Google Workspace directly
                        String fallbackUrl = docType == GoogleWorkspaceHelper.DocType.SHEETS
                            ? "https://sheets.new"
                            : docType == GoogleWorkspaceHelper.DocType.SLIDES
                            ? "https://slides.new"
                            : "https://docs.new";
                        if (docContent != null && !docContent.isEmpty()) {
                            android.content.ClipboardManager cm =
                                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(android.content.ClipData.newPlainText(typeName + " content", docContent));
                        }
                        String reply = "I'll open " + typeName + " for you directly, sir — your content's copied to the clipboard, just paste it in.";
                        history.add(new HistoryItem("model", reply)); addJarvisMsg(reply);
                        speak(reply, "neutral"); saveHistory();
                        GoogleWorkspaceHelper.openInBrowser(MainActivity.this, fallbackUrl);
                    });
                }
            });
            return;
        }


        // ── Story: ask before reading aloud ───────────────────────────────────
        // Detect story requests; ask user whether HENRY should read it aloud
        {
            String stLower = userText.toLowerCase(java.util.Locale.US);
            boolean isStoryRequest =
                stLower.matches(".*(tell me a story|tell me a tale|tell me about|read me a story|bedtime story|short story|fairy tale|narrate a story|once upon a time).*") ||
                (stLower.contains("story") && (stLower.contains("tell") || stLower.contains("read") || stLower.contains("write")));
            if (isStoryRequest) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                addJarvisMsg("Shall I read the story aloud for you once it's ready, sir?");
                speak("Shall I read the story aloud for you once it's ready, sir?", "warm");
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Read Story Aloud?")
                    .setMessage("Would you like me to read the story aloud after generating it?")
                    .setPositiveButton("Yes, read it", (d, w) -> {
                        generateAndReadStory(userText, true);
                    })
                    .setNegativeButton("Just show text", (d, w) -> {
                        generateAndReadStory(userText, false);
                    })
                    .setCancelable(false)
                    .show();
                return;
            }
        }

        // ── Recipe: rich structured summary ───────────────────────────────────
        {
            String rcLower = userText.toLowerCase(java.util.Locale.US);
            boolean isRecipeRequest =
                rcLower.matches(".*(recipe|how to (make|cook|bake|prepare)|ingredients for|how do (i|you) (make|cook|bake)|steps to (make|cook|bake)).*") &&
                !rcLower.contains("recommend") && !rcLower.contains("suggest");
            if (isRecipeRequest) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                saveHistory();
                setState(OrbView.OrbState.THINKING);
                showTyping();
                // Build an enriched recipe prompt
                String recipePrompt = userText + "\n\nStructure your answer with these sections:\n" +
                    "**🍽 Dish Overview** — brief description & origin\n" +
                    "**📋 Ingredients** — bullet list with quantities\n" +
                    "**👨‍🍳 Steps** — numbered cooking steps\n" +
                    "**💡 Pro Tips** — 2-3 chef tips\n" +
                    "**🕐 Time** — prep time & cook time\n" +
                    "Make it detailed and delicious, sir.";
                List<HistoryItem> recipeHist = new ArrayList<>(history);
                if (!recipeHist.isEmpty()) {
                    recipeHist.set(recipeHist.size() - 1, new HistoryItem("user", recipePrompt));
                }
                JarvisApi.askV20(recipeHist, null, MODE_DETAILED, userProfile, "recipe",
                        this, "normal", null, false, false, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
                        mainHandler.post(() -> {
                            hideTyping();
                            String clean = stripEmotionTag(reply);
                            history.add(new HistoryItem("model", clean));
                            addJarvisMsg(clean);
                            speak("Your recipe is ready, sir.", extractEmotion(reply));
                            if (followUps != null && !followUps.isEmpty()) showFollowUpChips(followUps);
                            saveHistory(); setState(OrbView.OrbState.IDLE);
                            if (btnSend != null) btnSend.setEnabled(true);
                        });
                    }
                    @Override public void onError(String e) {
                        mainHandler.post(() -> { hideTyping(); setState(OrbView.OrbState.IDLE); if (btnSend != null) btnSend.setEnabled(true); });
                    }
                });
                return;
            }
        }

        // ── Country Info: rich structured summary ──────────────────────────────
        {
            String ctLower = userText.toLowerCase(java.util.Locale.US);
            boolean isCountryRequest =
                ctLower.matches(".*(tell me about|info(rmation)? (about|on)|facts about|summarize|summary of|all about).*(country|nation|place|island|city|capital|republic|kingdom|state).*") ||
                ctLower.matches(".*(country|nation|place).*(history|culture|food|tourism|population|economy|facts).*") ||
                (ctLower.contains("about") && (ctLower.contains("history") || ctLower.contains("culture") || ctLower.contains("tourism")));
            if (isCountryRequest) {
                history.add(new HistoryItem("user", userText)); addUserMsg(userText);
                saveHistory();
                setState(OrbView.OrbState.THINKING);
                showTyping();
                String countryPrompt = userText + "\n\nProvide a comprehensive summary with these sections:\n" +
                    "**🌍 Overview** — what & where it is\n" +
                    "**📜 History** — key historical highlights\n" +
                    "**🎭 Culture & Traditions** — customs, art, religion\n" +
                    "**🍜 Cuisine** — famous dishes & food culture\n" +
                    "**🗺 Top Tourist Attractions** — must-see places\n" +
                    "**💰 Economy & Facts** — currency, GDP, population, capital\n" +
                    "**✈ Travel Tips** — best time to visit, visa info\n" +
                    "Be engaging and informative, sir.";
                List<HistoryItem> ctHist = new ArrayList<>(history);
                if (!ctHist.isEmpty()) {
                    ctHist.set(ctHist.size() - 1, new HistoryItem("user", countryPrompt));
                }
                JarvisApi.askV20(ctHist, null, MODE_DETAILED, userProfile, "country",
                        this, "normal", null, false, false, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
                        mainHandler.post(() -> {
                            hideTyping();
                            String clean = stripEmotionTag(reply);
                            history.add(new HistoryItem("model", clean));
                            addJarvisMsg(clean);
                            speak("Here's the country briefing, sir.", extractEmotion(reply));
                            if (followUps != null && !followUps.isEmpty()) showFollowUpChips(followUps);
                            saveHistory(); setState(OrbView.OrbState.IDLE);
                            if (btnSend != null) btnSend.setEnabled(true);
                        });
                    }
                    @Override public void onError(String e) {
                        mainHandler.post(() -> { hideTyping(); setState(OrbView.OrbState.IDLE); if (btnSend != null) btnSend.setEnabled(true); });
                    }
                });
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

        // [v20] Emotion detection — adapt orb hint and tell backend
        EmotionDetector.EmotionalState userEmotion = EmotionDetector.detect(userText);
        String emotionStr = EmotionDetector.toApiString(userEmotion);
        String emotionHint = EmotionDetector.getOrbHint(userEmotion, "PROCESSING…");
        if (tvOrbHint != null) tvOrbHint.setText(emotionHint);

        // [v20] Relationship learning
        RelationshipBrain.learnFromMessage(this, userText);

        // 🌍 Earth Map voice trigger
        String lowerInput = userText.toLowerCase(java.util.Locale.US);
        if (lowerInput.matches(".*(open|show|launch|earth|globe|world).*(map|globe|earth|world).*") ||
            lowerInput.matches(".*(map|globe).*")) {
            java.util.regex.Matcher mFly = java.util.regex.Pattern.compile(
                "(?:show|fly to|go to|open|find|locate)\\s+(.+?)\\s+on\\s+(?:the\\s+)?(?:map|globe)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userText);
            String flyTo = mFly.find() ? mFly.group(1).trim() : null;
            openEarthMap(flyTo);
        }
        // 🐾 Animal Scanner voice trigger
        if (lowerInput.matches(".*(what animal|animal scanner|identify animal|scan animal).*")) {
            openAnimalScanner();
        }
        // 🌿 Plant Scanner voice trigger
        if (lowerInput.matches(".*(what plant|plant scanner|identify plant|scan plant|what flower|what tree|what herb).*")) {
            openPlantScanner();
        }
        // 🚀 Space Command voice trigger
        if (lowerInput.matches(".*(space station|iss|nasa|asteroid|open space|space command).*")) {
            startActivity(new android.content.Intent(this, SpaceActivity.class));
        }
        // 📈 Markets voice trigger
        if (lowerInput.matches(".*(stock market|live market|open market|bitcoin price|crypto price|nasdaq|dow jones).*")) {
            startActivity(new android.content.Intent(this, MarketsActivity.class));
        }
        // 🌐 Earth Radar voice trigger
        if (lowerInput.matches(".*(earthquake|seismic|earth radar|global weather|open radar).*")) {
            startActivity(new android.content.Intent(this, EarthRadarActivity.class));
        }

        // ✈ Flight Tracker voice trigger
        java.util.regex.Matcher mFlight = java.util.regex.Pattern.compile(
            "(?:track|check|status of|show|find)\\s+(?:flight\\s+)?([A-Za-z]{2}\\d{1,4})",
            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userText);
        if (lowerInput.contains("flight tracker") || lowerInput.contains("open flight")) {
            openFlightTracker(null);
        } else if (mFlight.find()) {
            openFlightTracker(mFlight.group(1).toUpperCase());
        }

        // [v20] Dubai transit detection — add deep-link buttons after response
        final boolean isTransit = TransitHelper.isTransitQuery(userText);
        final boolean isLegal   = UAELawHelper.isLegalQuery(userText);
        final String[] transitRoute = isTransit ? TransitHelper.extractRoute(userText) : null;
        final UAELawHelper.LegalCategory legalCat = isLegal ? UAELawHelper.classify(userText) : null;

        // [v18] Dynamic thinking message based on intent
        String intentType = (pendingImageBase64 != null) ? "vision"
                : isTransit ? "transit"
                : isLegal   ? "legal"
                : JarvisApi.classifyIntent(userText);
        showTypingWithHint(intentType);

        if (btnSend != null) btnSend.setEnabled(false);
        String imageB64 = pendingImageBase64;
        clearAttachment();

        // Build history for API call
        List<HistoryItem> apiHistory = new ArrayList<>(history);
        if (!effectiveUserText.equals(userText) && !apiHistory.isEmpty()) {
            apiHistory.set(apiHistory.size() - 1, new HistoryItem("user", effectiveUserText));
        }

        // [v19] Auto-learn from user message before sending
        SmartMemory.learnFromMessage(this, userText);

        // [v20] Build relationship context for backend
        String relCtx = RelationshipBrain.buildContext(this);

        // [v20] Determine if tournament or chain thinking needed
        boolean useTournament = isImportantQuery(userText);
        boolean useChain      = isDeepReasoningQuery(userText);

        JarvisApi.askV20(apiHistory, imageB64, responseMode, userProfile, intentType,
                this, emotionStr, relCtx.isEmpty() ? null : relCtx,
                useTournament, useChain,
                new JarvisApi.Callback() {
            @Override public void onSuccess(String reply, String imageUrl, java.util.List<String> followUps) {
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

                    // [v20] Transit action buttons
                    if (isTransit) {
                        showTransitActions(transitRoute);
                    }

                    // [v20] Legal action button
                    if (isLegal && legalCat != null) {
                        showLegalAction(legalCat);
                    }

                    // [v18] Follow-up chips
                    if (followUps != null && !followUps.isEmpty()) {
                        showFollowUpChips(followUps);
                    }

                    saveHistory();
                    if (btnSend != null) btnSend.setEnabled(true);

                    // [v20] Update orb mood after response
                    updateMoodOrb();
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

    // ── Vault PIN Dialog ──────────────────────────────────────────────────────
    private void showVaultPinDialog(String[] cmd) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        android.widget.EditText etPin = new android.widget.EditText(this);
        etPin.setHint(PasswordVault.hasPin(this) ? "Enter vault PIN" : "Create a 4-digit PIN");
        etPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                           android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(etPin);

        new AlertDialog.Builder(this)
            .setTitle("◆ Password Vault")
            .setMessage(PasswordVault.hasPin(this) ? "Enter your vault PIN to continue, sir."
                : "First time: create a PIN to protect your vault, sir.")
            .setView(layout)
            .setPositiveButton("Unlock", (d, w) -> {
                String pin = etPin.getText().toString().trim();
                if (pin.isEmpty()) { Toast.makeText(this, "PIN required", Toast.LENGTH_SHORT).show(); return; }

                if (!PasswordVault.hasPin(this)) {
                    PasswordVault.setPin(this, pin);
                    Toast.makeText(this, "Vault PIN created", Toast.LENGTH_SHORT).show();
                } else if (!PasswordVault.checkPin(this, pin)) {
                    String wrong = "[EMOTION:concerned] Wrong PIN, sir. Vault remains locked.";
                    addJarvisMsg(stripEmotionTag(wrong)); speak(wrong, "concerned"); return;
                }

                String reply;
                switch (cmd[0]) {
                    case "save":
                        reply = PasswordVault.save(this, pin, cmd[1],
                            cmd[2] != null ? cmd[2] : "", cmd[3] != null ? cmd[3] : "");
                        break;
                    case "get":
                        reply = PasswordVault.retrieve(this, pin, cmd[1]);
                        break;
                    case "list":
                        reply = PasswordVault.listServices(this, pin);
                        break;
                    case "delete":
                        reply = PasswordVault.delete(this, pin, cmd[1]);
                        break;
                    default:
                        reply = "[EMOTION:neutral] Unknown vault command, sir.";
                }
                String clean = stripEmotionTag(reply);
                addJarvisMsg(clean); speak(clean, extractEmotion(reply));
                history.add(new HistoryItem("model", clean)); saveHistory();
            })
            .setNegativeButton("Cancel", null).show();
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
        int pos = messages.size() - 1;
        adapter.notifyItemInserted(pos);
        scrollToBottom();
        // [v18] Long-press any HENRY message to copy it
        mainHandler.post(() -> {
            android.view.View v = recycler.findViewHolderForAdapterPosition(pos) != null
                ? recycler.findViewHolderForAdapterPosition(pos).itemView : null;
            if (v != null) {
                final String copyText = text;
                v.setOnLongClickListener(lv -> {
                    android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("HENRY", copyText));
                        android.widget.Toast.makeText(this, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }
        });
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

    // [v18] Dynamic thinking message based on intent type
    private void showTypingWithHint(String intentType) {
        String hint;
        switch (intentType != null ? intentType : "chat") {
            case "search":  hint = "Searching the web…";        break;
            case "news":    hint = "Fetching latest news…";     break;
            case "crypto":  hint = "Checking live prices…";     break;
            case "forex":   hint = "Getting exchange rates…";   break;
            case "math":    hint = "Calculating…";              break;
            case "vision":  hint = "Analysing image…";          break;
            case "reason":  hint = "Thinking step by step…";    break;
            case "transit": hint = "Planning your route…";      break;
            case "legal":   hint = "Checking UAE law…";         break;
            default:        hint = "Thinking…";                 break;
        }
        if (tvOrbHint != null) tvOrbHint.setText(hint.toUpperCase());
        showTyping();
    }

    private void hideTyping() {
        if (typingPos >= 0 && typingPos < messages.size()) {
            messages.remove(typingPos);
            adapter.notifyItemRemoved(typingPos); typingPos = -1;
        }
        if (tvOrbHint != null) tvOrbHint.setText("WHAT CAN I DO FOR YOU, SIR?");
    }

    // [v18] Show contextual follow-up chips after HENRY replies
    private void showFollowUpChips(java.util.List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) return;
        // Temporarily override chip 1-3 with suggestions, restore after 20s
        int[] chipIds = { R.id.chip1, R.id.chip2, R.id.chip3 };
        for (int i = 0; i < Math.min(suggestions.size(), chipIds.length); i++) {
            final int idx  = i;
            final String q = suggestions.get(i);
            View chip = findViewById(chipIds[i]);
            if (chip == null) continue;
            if (chip instanceof android.widget.Button) {
                ((android.widget.Button) chip).setText(q);
            } else if (chip instanceof android.widget.TextView) {
                ((android.widget.TextView) chip).setText(q);
            }
            chip.setOnClickListener(v -> {
                hideWelcome();
                restoreChip(chipIds[idx], idx);
                // [v20] Route special actions differently
                if (q.startsWith("🗺") || q.startsWith("🚌") || q.startsWith("🚕")
                    || q.startsWith("📋") || q.startsWith("📞") || q.startsWith("📝")) {
                    handleSpecialChipAction(q);
                } else {
                    askJarvis(q);
                }
            });
        }
        // Auto-restore after 25 seconds
        mainHandler.postDelayed(() -> {
            for (int i = 0; i < chipIds.length; i++) restoreChip(chipIds[i], i);
        }, 25000);
    }

    // ── v20: Transit action buttons after transit response ────────────────────
    private void showTransitActions(String[] route) {
        final String origin = (route != null && route.length > 0) ? route[0] : "";
        final String dest   = (route != null && route.length > 1) ? route[1] : "";
        if (dest.isEmpty() && origin.isEmpty()) return;

        String actionMsg = "**Quick actions:**";
        addJarvisMsg(actionMsg);

        // Build a chip row for transit options
        final String finalDest = dest.isEmpty() ? origin : dest;
        final String finalOrigin = origin;
        java.util.List<String> actions = new java.util.ArrayList<>();
        actions.add("🗺 Open Google Maps");
        actions.add("🚌 RTA Journey Planner");
        actions.add("🚕 Book Careem");
        showFollowUpChips(actions);
    }

    // ── v20: UAE Law complaint button ─────────────────────────────────────────
    private void showLegalAction(UAELawHelper.LegalCategory cat) {
        java.util.List<String> actions = new java.util.ArrayList<>();
        actions.add("📋 File complaint with " + UAELawHelper.getPortalName(cat));
        actions.add("📞 Hotline: " + UAELawHelper.getHotline(cat));
        actions.add("📝 Draft a formal notice");
        showFollowUpChips(actions);
    }

    // ── v20: Handle transit/legal chip actions ────────────────────────────────
    private void handleSpecialChipAction(String text) {
        if (text.contains("Google Maps")) {
            // Try to extract last asked route from history
            String lastUserMsg = "";
            for (int i = history.size()-1; i >= 0; i--) {
                if ("user".equals(history.get(i).role)) { lastUserMsg = history.get(i).text; break; }
            }
            String[] route = TransitHelper.extractRoute(lastUserMsg);
            String origin = route != null && route.length > 0 ? route[0] : "";
            String dest   = route != null && route.length > 1 ? route[1] : (route != null ? route[0] : "");
            TransitHelper.openGoogleMapsTransit(this, origin, dest);
        } else if (text.contains("RTA Journey") || text.contains("Transit Map")) {
            // Open Google Maps transit mode globally instead of Dubai-only portal
            TransitHelper.openGoogleMapsTransit(this, "", "");
        } else if (text.contains("Careem") || text.contains("Uber") || text.contains("Ride")) {
            String lastUserMsg = "";
            for (int i = history.size()-1; i >= 0; i--) {
                if ("user".equals(history.get(i).role)) { lastUserMsg = history.get(i).text; break; }
            }
            String[] route = TransitHelper.extractRoute(lastUserMsg);
            String dest = route != null && route.length > 1 ? route[1] : lastUserMsg;
            TransitHelper.openBestRideApp(this, dest);
        } else if (text.contains("File complaint")) {
            // Extract category from button text and open portal
            UAELawHelper.LegalCategory cat = UAELawHelper.LegalCategory.GENERAL;
            if (text.contains("RERA")) cat = UAELawHelper.LegalCategory.TENANCY;
            else if (text.contains("MOHRE")) cat = UAELawHelper.LegalCategory.LABOUR;
            else if (text.contains("DED")) cat = UAELawHelper.LegalCategory.CONSUMER;
            else if (text.contains("RTA")) cat = UAELawHelper.LegalCategory.TRAFFIC;
            else if (text.contains("CBUAE")) cat = UAELawHelper.LegalCategory.FINANCIAL;
            UAELawHelper.openComplaintPortal(this, cat);
        } else if (text.contains("Hotline:")) {
            String number = text.replaceAll(".*Hotline:\\s*","").trim().replaceAll("[^0-9]","");
            if (!number.isEmpty()) {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_DIAL,
                    android.net.Uri.parse("tel:" + number)));
            }
        } else if (text.contains("Draft a formal")) {
            askJarvis("Please draft a formal legal notice letter for my situation");
        } else {
            // Default — treat as normal chat
            askJarvis(text);
        }
    }

    // ── 🌍 Open Earth Map ─────────────────────────────────────────────────────
    private void openFlightTracker(String flightNumber) {
        Intent intent = new Intent(this, FlightActivity.class);
        if (flightNumber != null && !flightNumber.isEmpty()) {
            intent.putExtra("flight_number", flightNumber);
        }
        startActivity(intent);
    }

    private void openEarthMap(String flyToCountry) {
        Intent intent = new Intent(this, EarthMapActivity.class);
        if (flyToCountry != null && !flyToCountry.isEmpty()) {
            intent.putExtra("fly_to", flyToCountry);
        }
        startActivityForResult(intent, EarthMapActivity.REQUEST_CODE);
    }

    // ── 🐾 Open Animal Scanner ────────────────────────────────────────────────
    private void openAnimalScanner() {
        Intent intent = new Intent(this, AnimalScannerActivity.class);
        startActivityForResult(intent, AnimalScannerActivity.REQUEST_CODE);
    }

    private void openPlantScanner() {
        Intent intent = new Intent(this, PlantScannerActivity.class);
        startActivityForResult(intent, PlantScannerActivity.REQUEST_CODE);
    }

    // ── v20: Update orb accent color based on HENRY's mood ───────────────────
    private void updateMoodOrb() {
        HenryMood.Mood mood = HenryMood.getCurrentMood();
        // Orb hint phrase
        String phrase = HenryMood.getStatusPhrase(mood);
        if (tvOrbHint != null && currentState == OrbView.OrbState.IDLE) {
            tvOrbHint.setText(phrase);
        }
    }

    // ── v20: Detect if query warrants multi-model tournament ─────────────────
    private boolean isImportantQuery(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US);
        return t.matches(".*\\b(should i invest|is it safe|medical|diagnosis|symptoms|treatment|doctor|hospital|legal advice|court|financial decision|mortgage|loan|insurance|life-changing|career|should i quit|should i move)\\b.*");
    }

    // ── v20: Detect deep reasoning query ─────────────────────────────────────
    private boolean isDeepReasoningQuery(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US);
        return t.matches(".*\\b(explain deeply|step by step|walk me through|break it down|analyse|analyze in detail|compare thoroughly|pros and cons of|help me understand|how exactly does)\\b.*")
            && text.length() > 40;
    }

    private void restoreChip(int chipId, int index) {
        View chip = findViewById(chipId);
        if (chip == null) return;
        String text = ChipPrefs.get(this, index);
        if (chip instanceof android.widget.Button) {
            ((android.widget.Button) chip).setText(text);
        } else if (chip instanceof android.widget.TextView) {
            ((android.widget.TextView) chip).setText(text);
        }
        chip.setOnClickListener(v -> { hideWelcome(); askJarvis(ChipPrefs.get(this, index)); });
    }
    private void setState(OrbView.OrbState state) {
        currentState = state;
        if (orbView  != null) orbView.setState(state);
        if (tvStatus != null) {
            final String[] labels = {"STANDBY","LISTENING…","PROCESSING…","SPEAKING…","WAKE"};
            tvStatus.setText(labels[state.ordinal()]);
            // Blue accent when active
            tvStatus.setTextColor(state == OrbView.OrbState.IDLE
                ? 0xFF004466 : 0xFF00BEFF);
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
