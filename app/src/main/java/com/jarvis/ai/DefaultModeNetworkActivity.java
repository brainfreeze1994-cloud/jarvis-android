package com.jarvis.ai;

import android.content.SharedPreferences;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

/**
 * DefaultModeNetworkActivity — DMN Mind-Wandering Mode
 * The Default Mode Network is most active during:
 * - Mind-wandering & daydreaming
 * - Self-reflection & introspection
 * - Social cognition & empathy
 * - Creative incubation
 * - Future planning & simulation
 *
 * This module activates those states deliberately:
 * HENRY guides you through structured mind-wandering,
 * reflection journals, and creative incubation sessions.
 */
public class DefaultModeNetworkActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String PREFS      = "dmn_journal";
    private static final String KEY_ENTRIES = "entries";

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvTitle, tvPrompt, tvOutput;
    private LinearLayout menuLayout, activeLayout;
    private ScrollView journalScroll;
    private TextView tvJournal;

    private String currentMode = null;
    private int wanderStep = 0;
    private Runnable wanderRunnable;

    // DMN activation prompts — open-ended, reflective, non-analytical
    private static final String[] WANDER_PROMPTS = {
        "Let your mind go completely blank. Don't try to think. Just… drift.",
        "A thought floats by like a cloud. You don't hold it. You just watch it pass.",
        "Where does your mind naturally go when no one is asking anything of you?",
        "Imagine a version of yourself from 10 years in the future. What are they doing right now?",
        "If you had no obligations today, where would your mind wander first?",
        "Think of someone you love. What small detail about them makes you smile?",
        "A memory surfaces — unexpected, vivid. Let it play out completely.",
        "What unfinished thought has been quietly sitting in the background of your mind?",
        "Imagine a place that doesn't exist but feels completely real to you.",
        "What question do you keep asking yourself that you haven't answered yet?",
    };

    private static final String[] REFLECTION_PROMPTS = {
        "What was the most meaningful moment of this week? Describe it in detail.",
        "What belief have you held that has recently started to shift?",
        "Who has influenced who you are today, and how?",
        "What would your younger self think of who you are now?",
        "What are you tolerating in your life that you no longer need to?",
        "If you were brutally honest with yourself: what do you actually want?",
        "What pattern in your life keeps repeating? What is it trying to teach you?",
        "Name three things you are genuinely proud of — not for others, but for yourself.",
        "What would you do differently if no one were watching or judging?",
        "What does 'success' actually mean to you, in your own definition?",
    };

    private static final String[] INCUBATION_PROMPTS = {
        "Describe your unsolved problem in one sentence. Then let it go completely.",
        "What is the most creative solution you've rejected because it seemed too strange?",
        "If the problem solved itself while you slept, what would be different tomorrow morning?",
        "What approach are you NOT taking that might actually work?",
        "Imagine your best friend has the same problem. What would you tell them?",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmn);

        tvTitle      = findViewById(R.id.dmn_title);
        tvPrompt     = findViewById(R.id.dmn_prompt);
        tvOutput     = findViewById(R.id.dmn_output);
        menuLayout   = findViewById(R.id.dmn_menu);
        activeLayout = findViewById(R.id.dmn_active);
        journalScroll= findViewById(R.id.dmn_journal_scroll);
        tvJournal    = findViewById(R.id.dmn_journal_text);

        tts = new TextToSpeech(this, this);
        showMenu();

        Button btnBack = findViewById(R.id.dmn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            if (currentMode != null) { stopWander(); showMenu(); }
            else finish();
        });
    }

    private void showMenu() {
        currentMode = null;
        if (menuLayout   != null) menuLayout.setVisibility(View.VISIBLE);
        if (activeLayout != null) activeLayout.setVisibility(View.GONE);
        if (journalScroll!= null) journalScroll.setVisibility(View.GONE);
        if (tvTitle      != null) tvTitle.setText("◈ DEFAULT MODE NETWORK");
        if (tvPrompt     != null) tvPrompt.setText(
            "The DMN is your brain's most powerful network.\n" +
            "It activates when you're NOT focused — during reflection,\n" +
            "daydreaming, creativity, and self-understanding.");

        String[][] modes = {
            { "🌊 Mind-Wandering Session",   "wander"      },
            { "📖 Reflection Journal",        "reflect"     },
            { "💡 Creative Incubation",       "incubate"    },
            { "🔮 Future Self Simulation",    "future_self" },
            { "🤝 Empathy Expansion",         "empathy"     },
            { "📔 My DMN Journal",            "journal"     },
        };

        LinearLayout menu = menuLayout;
        if (menu == null) return;
        menu.removeAllViews();
        for (String[] m : modes) {
            Button b = new Button(this);
            b.setText(m[0]);
            b.setBackgroundResource(R.drawable.bg_chip);
            b.setTextColor(0xFFCC88FF);
            b.setTextSize(14f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 8, 0, 8);
            b.setLayoutParams(lp);
            String key = m[1];
            b.setOnClickListener(v -> startMode(key));
            menu.addView(b);
        }
    }

    private void startMode(String key) {
        currentMode = key;
        if (menuLayout   != null) menuLayout.setVisibility(View.GONE);

        if (key.equals("journal")) { showJournal(); return; }

        if (activeLayout != null) activeLayout.setVisibility(View.VISIBLE);

        switch (key) {
            case "wander":      startMindWander(); break;
            case "reflect":     startReflection(); break;
            case "incubate":    startIncubation(); break;
            case "future_self": startFutureSelf(); break;
            case "empathy":     startEmpathy(); break;
        }
    }

    // ── Mind-Wandering Session ────────────────────────────────────────────────
    private void startMindWander() {
        if (tvTitle != null) tvTitle.setText("◈ MIND-WANDERING");
        wanderStep = 0;
        List<String> prompts = new ArrayList<>(Arrays.asList(WANDER_PROMPTS));
        Collections.shuffle(prompts);
        runWander(prompts.toArray(new String[0]), 0);
    }

    private void runWander(String[] prompts, int idx) {
        if (idx >= prompts.length) {
            if (tvPrompt != null) tvPrompt.setText("Session complete.\n\nYour DMN has been fully activated.\nCapture any insights that emerged:");
            showJournalInput();
            return;
        }
        String prompt = prompts[idx];
        if (tvPrompt != null) tvPrompt.setText(prompt);
        if (ttsReady) tts.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, null);
        int words = prompt.split("\\s+").length;
        long delay = Math.max(12000, words * 600L);
        wanderRunnable = () -> runWander(prompts, idx + 1);
        handler.postDelayed(wanderRunnable, delay);
    }

    // ── Reflection Journal ────────────────────────────────────────────────────
    private void startReflection() {
        if (tvTitle != null) tvTitle.setText("◈ REFLECTION MODE");
        Random rng = new Random();
        String prompt = REFLECTION_PROMPTS[rng.nextInt(REFLECTION_PROMPTS.length)];
        if (tvPrompt != null) tvPrompt.setText(prompt);
        if (ttsReady) tts.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, null);

        // Use AI for deeper follow-up
        if (activeLayout != null) {
            activeLayout.removeAllViews();
            EditText et = new EditText(this);
            et.setHint("Write your reflection…");
            et.setHintTextColor(0xFF4A2A6A);
            et.setTextColor(0xFFCC88FF);
            et.setBackgroundColor(0xFF080818);
            et.setMinLines(5);
            et.setTextSize(15f);
            et.setPadding(16, 16, 16, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            et.setLayoutParams(lp);

            Button btnDeepen = new Button(this);
            btnDeepen.setText("◈ DEEPEN WITH HENRY");
            btnDeepen.setBackgroundResource(R.drawable.bg_chip);
            btnDeepen.setTextColor(0xFFCC88FF);

            Button btnSave = new Button(this);
            btnSave.setText("💾 SAVE TO JOURNAL");
            btnSave.setBackgroundResource(R.drawable.bg_chip);
            btnSave.setTextColor(0xFF00D4FF);

            btnDeepen.setOnClickListener(v -> {
                String text = et.getText().toString().trim();
                if (text.isEmpty()) return;
                String aiPrompt = "Reflection prompt: \"" + prompt + "\"\n\nUser wrote: \"" + text + "\"\n\n" +
                    "As HENRY, offer 2-3 deep, insightful follow-up questions that will help them go deeper into self-understanding. " +
                    "Be warm, perceptive, and genuinely curious. No generic advice.";
                List<HistoryItem> h = new ArrayList<>();
                h.add(new HistoryItem("user", aiPrompt));
                if (tvOutput != null) tvOutput.setText("Analysing your reflection…");
                JarvisApi.ask(h, null, "balanced", null, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imageUrl, List<String> fu) {
                        String clean = reply.replaceAll("\\[EMOTION:[^]]+]", "").trim();
                        handler.post(() -> {
                            if (tvOutput != null) tvOutput.setText(clean);
                            if (ttsReady) tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, null);
                        });
                    }
                    @Override public void onError(String e) {}
                });
            });

            btnSave.setOnClickListener(v -> {
                String text = et.getText().toString().trim();
                if (!text.isEmpty()) {
                    saveJournalEntry(prompt, text);
                    Toast.makeText(this, "Saved to DMN Journal", Toast.LENGTH_SHORT).show();
                    et.setText("");
                }
            });

            activeLayout.addView(et);
            activeLayout.addView(btnDeepen);
            activeLayout.addView(btnSave);
        }
    }

    // ── Creative Incubation ────────────────────────────────────────────────────
    private void startIncubation() {
        if (tvTitle != null) tvTitle.setText("◈ CREATIVE INCUBATION");
        String intro = "Incubation is when your subconscious solves problems you've consciously set aside.\n\n" +
            "You will state your challenge, then deliberately STOP thinking about it. " +
            "Your DMN will work on it in the background.";
        if (tvPrompt != null) tvPrompt.setText(intro);

        if (activeLayout != null) {
            activeLayout.removeAllViews();
            EditText et = new EditText(this);
            et.setHint("State your unsolved challenge in one clear sentence…");
            et.setHintTextColor(0xFF4A2A6A);
            et.setTextColor(0xFFCC88FF);
            et.setBackgroundColor(0xFF080818);
            et.setMinLines(3);
            et.setTextSize(15f);
            et.setPadding(16, 16, 16, 16);
            activeLayout.addView(et);

            Button b = new Button(this);
            b.setText("◈ BEGIN INCUBATION");
            b.setBackgroundResource(R.drawable.bg_chip);
            b.setTextColor(0xFFCC88FF);
            b.setOnClickListener(v -> {
                String challenge = et.getText().toString().trim();
                if (challenge.isEmpty()) return;
                // Use a random incubation prompt
                Random rng = new Random();
                String iPrompt = INCUBATION_PROMPTS[rng.nextInt(INCUBATION_PROMPTS.length)];
                if (tvPrompt != null) tvPrompt.setText("Challenge logged: \"" + challenge + "\"\n\n" + iPrompt + "\n\nNow — don't think about it. Let your DMN work.");
                if (ttsReady) tts.speak("Challenge logged. " + iPrompt + " Now let it go. Your subconscious will work on it.", TextToSpeech.QUEUE_FLUSH, null, null);
                saveJournalEntry("Incubation: " + challenge, iPrompt);
                et.setText("");
                et.setHint("Come back in 20+ minutes with any ideas…");
            });
            activeLayout.addView(b);
        }
    }

    // ── Future Self Simulation ─────────────────────────────────────────────────
    private void startFutureSelf() {
        if (tvTitle != null) tvTitle.setText("◈ FUTURE SELF SIMULATION");
        String prompt = "Imagine your ideal self exactly 5 years from now.\n\n" +
            "Describe in precise detail:\n" +
            "• Where are you living?\n• What are you working on?\n" +
            "• How do you feel when you wake up?\n• Who is around you?\n• What did it take to get here?";
        if (tvPrompt != null) tvPrompt.setText(prompt);
        if (ttsReady) tts.speak("Imagine your ideal self five years from now. Describe your life in detail.", TextToSpeech.QUEUE_FLUSH, null, null);

        if (activeLayout != null) {
            activeLayout.removeAllViews();
            EditText et = new EditText(this);
            et.setHint("Describe your future self's life…");
            et.setHintTextColor(0xFF4A2A6A);
            et.setTextColor(0xFFCC88FF);
            et.setBackgroundColor(0xFF080818);
            et.setMinLines(6);
            et.setTextSize(15f);
            et.setPadding(16, 16, 16, 16);
            activeLayout.addView(et);

            Button b = new Button(this);
            b.setText("◈ HENRY ANALYSES YOUR FUTURE");
            b.setBackgroundResource(R.drawable.bg_chip);
            b.setTextColor(0xFFCC88FF);
            b.setOnClickListener(v -> {
                String vision = et.getText().toString().trim();
                if (vision.isEmpty()) return;
                String aiPrompt = "My 5-year future vision: \"" + vision + "\"\n\n" +
                    "As HENRY, identify: (1) the biggest gap between now and this vision, " +
                    "(2) the first concrete step to take this week, " +
                    "(3) one hidden limiting belief that might prevent this. Be direct, warm, honest.";
                if (tvOutput != null) tvOutput.setText("Simulating your future…");
                List<HistoryItem> h = new ArrayList<>();
                h.add(new HistoryItem("user", aiPrompt));
                JarvisApi.ask(h, null, "detailed", null, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imageUrl, List<String> fu) {
                        String clean = reply.replaceAll("\\[EMOTION:[^]]+]", "").trim();
                        handler.post(() -> {
                            if (tvOutput != null) tvOutput.setText(clean);
                            if (ttsReady) tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, null);
                            saveJournalEntry("Future Self Vision", vision + "\n\nHENRY: " + clean);
                        });
                    }
                    @Override public void onError(String e) {}
                });
            });
            activeLayout.addView(b);
        }
    }

    // ── Empathy Expansion ──────────────────────────────────────────────────────
    private void startEmpathy() {
        if (tvTitle != null) tvTitle.setText("◈ EMPATHY EXPANSION");
        String[] scenarios = {
            "Think of someone who frustrates you. Describe their life from THEIR perspective — their fears, pressures, what they need.",
            "Imagine being a refugee arriving in a new country with nothing but a phone and one bag. What is your first hour like?",
            "You are 85 years old, looking back at your life. What do you wish you had done more of?",
            "Think of someone very different from you politically or culturally. What do you both want at the deepest level?",
        };
        Random rng = new Random();
        String scenario = scenarios[rng.nextInt(scenarios.length)];
        if (tvPrompt != null) tvPrompt.setText(scenario);
        if (ttsReady) tts.speak(scenario, TextToSpeech.QUEUE_FLUSH, null, null);

        if (activeLayout != null) {
            activeLayout.removeAllViews();
            EditText et = new EditText(this);
            et.setHint("Write from their perspective…");
            et.setHintTextColor(0xFF4A2A6A);
            et.setTextColor(0xFFCC88FF);
            et.setBackgroundColor(0xFF080818);
            et.setMinLines(5);
            et.setTextSize(15f);
            et.setPadding(16, 16, 16, 16);
            activeLayout.addView(et);

            Button b = new Button(this);
            b.setText("◈ REFLECT WITH HENRY");
            b.setBackgroundResource(R.drawable.bg_chip);
            b.setTextColor(0xFFCC88FF);
            b.setOnClickListener(v -> {
                String text = et.getText().toString().trim();
                if (text.isEmpty()) return;
                String aiPrompt = "Empathy exercise. Scenario: \"" + scenario + "\"\n\nUser wrote: \"" + text + "\"\n\n" +
                    "Offer a compassionate reflection: what they did well, what deeper insight they might have missed, and one question to go even further.";
                if (tvOutput != null) tvOutput.setText("Expanding your empathy…");
                List<HistoryItem> h = new ArrayList<>();
                h.add(new HistoryItem("user", aiPrompt));
                JarvisApi.ask(h, null, "balanced", null, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imageUrl, List<String> fu) {
                        String clean = reply.replaceAll("\\[EMOTION:[^]]+]", "").trim();
                        handler.post(() -> {
                            if (tvOutput != null) tvOutput.setText(clean);
                            if (ttsReady) tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, null);
                        });
                    }
                    @Override public void onError(String e) {}
                });
            });
            activeLayout.addView(b);
        }
    }

    // ── Journal ────────────────────────────────────────────────────────────────
    private void showJournal() {
        if (activeLayout  != null) activeLayout.setVisibility(View.GONE);
        if (journalScroll != null) journalScroll.setVisibility(View.VISIBLE);
        if (tvTitle       != null) tvTitle.setText("◈ DMN JOURNAL");

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String raw = prefs.getString(KEY_ENTRIES, "");

        if (tvJournal != null) {
            if (raw.isEmpty()) {
                tvJournal.setText("No entries yet.\n\nComplete a DMN session and save your reflections.");
            } else {
                tvJournal.setText(raw);
            }
        }
    }

    private void showJournalInput() {
        if (activeLayout == null) return;
        activeLayout.removeAllViews();
        EditText et = new EditText(this);
        et.setHint("Capture any insights or images that emerged…");
        et.setHintTextColor(0xFF4A2A6A);
        et.setTextColor(0xFFCC88FF);
        et.setBackgroundColor(0xFF080818);
        et.setMinLines(4);
        et.setTextSize(15f);
        et.setPadding(16, 16, 16, 16);
        activeLayout.addView(et);
        Button b = new Button(this);
        b.setText("💾 SAVE INSIGHT");
        b.setBackgroundResource(R.drawable.bg_chip);
        b.setTextColor(0xFF00D4FF);
        b.setOnClickListener(v -> {
            String text = et.getText().toString().trim();
            if (!text.isEmpty()) {
                saveJournalEntry("Mind-Wandering Insight", text);
                Toast.makeText(this, "Insight saved to DMN Journal", Toast.LENGTH_SHORT).show();
                et.setText("");
            }
        });
        activeLayout.addView(b);
    }

    private void saveJournalEntry(String heading, String content) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String existing = prefs.getString(KEY_ENTRIES, "");
        String timestamp = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(new Date());
        String entry = "─────────────────────\n[" + timestamp + "]\n" + heading + "\n\n" + content + "\n\n";
        prefs.edit().putString(KEY_ENTRIES, entry + existing).apply();
    }

    private void stopWander() {
        if (wanderRunnable != null) handler.removeCallbacks(wanderRunnable);
        if (ttsReady) tts.stop();
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            tts.setPitch(0.82f);
            tts.setSpeechRate(0.78f);
            ttsReady = true;
        }
    }

    @Override protected void onDestroy() {
        stopWander();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
