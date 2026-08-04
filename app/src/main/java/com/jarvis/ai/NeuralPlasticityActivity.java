package com.jarvis.ai;

import android.content.SharedPreferences;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

/**
 * NeuralPlasticityActivity — Brain Training & Neuroplasticity Challenges
 * Daily exercises that build new neural pathways:
 * - Non-dominant hand challenges
 * - Pattern memorization
 * - Dual-task processing
 * - Creative divergent thinking
 * - Cognitive flexibility drills
 */
public class NeuralPlasticityActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String PREFS = "neural_plasticity";
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvTitle, tvScore, tvStreak, tvChallenge, tvFeedback;
    private LinearLayout menuLayout, exerciseLayout;
    private Button btnCheck, btnNext, btnBack;
    private LinearLayout answerContainer;

    private int score = 0;
    private int streak = 0;
    private int exerciseIndex = 0;
    private String currentExercise = null;
    private String correctAnswer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neural_plasticity);

        tvTitle         = findViewById(R.id.np_title);
        tvScore         = findViewById(R.id.np_score);
        tvStreak        = findViewById(R.id.np_streak);
        tvChallenge     = findViewById(R.id.np_challenge);
        tvFeedback      = findViewById(R.id.np_feedback);
        menuLayout      = findViewById(R.id.np_menu);
        exerciseLayout  = findViewById(R.id.np_exercise);
        btnCheck        = findViewById(R.id.np_btn_check);
        btnNext         = findViewById(R.id.np_btn_next);
        btnBack         = findViewById(R.id.np_back);
        answerContainer = findViewById(R.id.np_answers);

        tts = new TextToSpeech(this, this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        score  = prefs.getInt("score", 0);
        streak = prefs.getInt("streak", 0);

        updateStats();
        showMenu();

        if (btnBack != null) btnBack.setOnClickListener(v -> {
            if (currentExercise != null) { currentExercise = null; showMenu(); }
            else finish();
        });
        if (btnNext != null) btnNext.setOnClickListener(v -> nextExercise());
    }

    private void updateStats() {
        if (tvScore  != null) tvScore.setText("◈ SCORE: " + score);
        if (tvStreak != null) tvStreak.setText("🔥 STREAK: " + streak);
    }

    private void saveStats() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putInt("score", score).putInt("streak", streak).apply();
    }

    private void showMenu() {
        currentExercise = null;
        if (menuLayout    != null) menuLayout.setVisibility(View.VISIBLE);
        if (exerciseLayout!= null) exerciseLayout.setVisibility(View.GONE);
        if (tvTitle       != null) tvTitle.setText("◈ NEURAL PLASTICITY");
        if (tvChallenge   != null) tvChallenge.setText("Choose a brain training module to forge new neural pathways.");
        if (tvFeedback    != null) tvFeedback.setText("");

        String[][] modules = {
            { "🧮 Working Memory",      "memory"     },
            { "🔄 Cognitive Flip",      "flip"       },
            { "💡 Divergent Thinking",  "divergent"  },
            { "🎯 Dual Task",           "dual"       },
            { "🔢 Number Sense",        "numbers"    },
            { "🧩 Pattern Break",       "pattern"    },
            { "🔤 Word Reversal",       "word_rev"   },
            { "🔵 Stroop Challenge",    "stroop"     },
        };

        LinearLayout menu = menuLayout;
        if (menu == null) return;
        menu.removeAllViews();
        for (String[] m : modules) {
            Button b = new Button(this);
            b.setText(m[0]);
            b.setBackgroundResource(R.drawable.bg_chip);
            b.setTextColor(0xFF00FF99);
            b.setTextSize(14f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 8, 0, 8);
            b.setLayoutParams(lp);
            String key = m[1];
            b.setOnClickListener(v -> startExercise(key));
            menu.addView(b);
        }
    }

    private void startExercise(String key) {
        currentExercise = key;
        exerciseIndex   = 0;
        if (menuLayout    != null) menuLayout.setVisibility(View.GONE);
        if (exerciseLayout!= null) exerciseLayout.setVisibility(View.VISIBLE);
        nextExercise();
    }

    private void nextExercise() {
        if (tvFeedback != null) tvFeedback.setText("");
        if (btnCheck   != null) btnCheck.setVisibility(View.VISIBLE);
        if (answerContainer != null) answerContainer.removeAllViews();
        exerciseIndex++;

        switch (currentExercise) {
            case "memory":      runMemoryChallenge(); break;
            case "flip":        runCognitiveFlip(); break;
            case "divergent":   runDivergentThinking(); break;
            case "dual":        runDualTask(); break;
            case "numbers":     runNumberSense(); break;
            case "pattern":     runPatternBreak(); break;
            case "word_rev":    runWordReversal(); break;
            case "stroop":      runStroop(); break;
        }
    }

    // ── Exercise 1: Working Memory (N-back style) ─────────────────────────────
    private void runMemoryChallenge() {
        int[] nums = new int[5];
        StringBuilder display = new StringBuilder();
        Random rng = new Random();
        for (int i = 0; i < 5; i++) { nums[i] = rng.nextInt(9) + 1; display.append(nums[i]).append(" "); }
        int sum = 0; for (int n : nums) sum += n;
        correctAnswer = String.valueOf(sum);

        if (tvTitle != null) tvTitle.setText("◈ WORKING MEMORY");
        if (tvChallenge != null) tvChallenge.setText("Remember this sequence for 3 seconds:\n\n" + display + "\n\nWhat is the SUM of all numbers?");

        handler.postDelayed(() -> { if (tvChallenge != null) tvChallenge.setText("What is the sum of the numbers you just saw?"); }, 3000);
        setupTextInput("Enter the sum…");
    }

    // ── Exercise 2: Cognitive Flip ────────────────────────────────────────────
    private void runCognitiveFlip() {
        String[][] flips = {
            { "You are LEFT-handed for the next 30 seconds. Trace a circle in the air with your NON-dominant hand.\n\nHow many full circles can you make?",
              "describe" },
            { "Say the MONTHS of the year BACKWARDS, as fast as you can.\n\nWhich month comes 3rd from the end?",
              "October" },
            { "Count from 100 DOWN by 7s. First 4 steps.\n\n100, 93, 86, 79… what comes next?",
              "72" },
            { "Name 5 things that are BOTH red AND edible. (Type them separated by commas)",
              "describe" },
        };
        String[] q = flips[exerciseIndex % flips.length];
        correctAnswer = q[1];
        if (tvTitle     != null) tvTitle.setText("◈ COGNITIVE FLIP");
        if (tvChallenge != null) tvChallenge.setText(q[0]);
        if (correctAnswer.equals("describe")) {
            setupMCQ(new String[]{"Done! Felt hard", "Done! Felt medium", "Done! Felt easy", "Skipped"}, -1, "describe");
        } else {
            setupTextInput("Type your answer…");
        }
    }

    // ── Exercise 3: Divergent Thinking ────────────────────────────────────────
    private void runDivergentThinking() {
        String[] prompts = {
            "Name 10 uses for a BRICK other than building.\nType as many as you can in 60 seconds.",
            "What do a CLOCK and a RIVER have in common? Give 5 similarities.",
            "Invent a word for the feeling of being happy AND sad simultaneously. Explain it.",
            "If you could add one sense to the human body, what would it detect?",
        };
        correctAnswer = "describe";
        if (tvTitle     != null) tvTitle.setText("◈ DIVERGENT THINKING");
        if (tvChallenge != null) tvChallenge.setText(prompts[exerciseIndex % prompts.length]);
        setupTextInput("Write your creative answer…");
        if (ttsReady) tts.speak("Engage your creative divergent thinking. There are no wrong answers.", TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // ── Exercise 4: Dual Task ─────────────────────────────────────────────────
    private void runDualTask() {
        String[] tasks = {
            "While TAPPING your left knee rhythmically with your left hand,\nsolve this: 7 × 8 = ?",
            "While HUMMING any tune,\ncount the vowels in: \"The quick brown fox jumps\"",
            "While BLINKING slowly every 2 seconds,\nname 5 world capitals in alphabetical order.",
        };
        String[] answers = { "56", "8", "describe" };
        int idx = exerciseIndex % tasks.length;
        correctAnswer = answers[idx];
        if (tvTitle     != null) tvTitle.setText("◈ DUAL TASK");
        if (tvChallenge != null) tvChallenge.setText(tasks[idx] + "\n\n⚠ Do BOTH tasks simultaneously — that's the point.");
        if (correctAnswer.equals("describe")) {
            setupMCQ(new String[]{"Done!", "Could not do both", "Did one only"}, -1, "describe");
        } else {
            setupTextInput("Answer…");
        }
    }

    // ── Exercise 5: Number Sense ──────────────────────────────────────────────
    private void runNumberSense() {
        Random rng = new Random();
        int type = rng.nextInt(3);
        String q; int ans;
        if (type == 0) {
            int a = rng.nextInt(50) + 10, b = rng.nextInt(50) + 10;
            q = a + " × " + b + " = ?\n(Mental math only — no calculator!)";
            ans = a * b;
        } else if (type == 1) {
            int a = rng.nextInt(900) + 100, b = rng.nextInt(100) + 10;
            q = a + " ÷ " + b + " ≈ ?\n(Round to nearest whole number)";
            ans = Math.round((float) a / b);
        } else {
            int a = rng.nextInt(200) + 50, b = rng.nextInt(200) + 50;
            q = a + " + " + b + " = ?\n(No writing — mental calculation)";
            ans = a + b;
        }
        correctAnswer = String.valueOf(ans);
        if (tvTitle     != null) tvTitle.setText("◈ NUMBER SENSE");
        if (tvChallenge != null) tvChallenge.setText(q);
        setupTextInput("Your answer…");
    }

    // ── Exercise 6: Pattern Break ─────────────────────────────────────────────
    private void runPatternBreak() {
        String[][] patterns = {
            { "2, 4, 8, 16, 32, __?", "64" },
            { "Z, Y, X, W, __?", "V" },
            { "1, 1, 2, 3, 5, 8, __?", "13" },
            { "Monday, Wednesday, Friday, __?", "Sunday" },
            { "🔴, 🔵, 🟡, 🔴, 🔵, __?", "🟡" },
        };
        String[] p = patterns[exerciseIndex % patterns.length];
        correctAnswer = p[1];
        if (tvTitle     != null) tvTitle.setText("◈ PATTERN BREAK");
        if (tvChallenge != null) tvChallenge.setText("Complete the pattern:\n\n" + p[0]);
        setupTextInput("Next in sequence…");
    }

    // ── Exercise 7: Word Reversal ──────────────────────────────────────────────
    private void runWordReversal() {
        String[][] words = {
            { "Spell ELEPHANT backwards", "TNAHPELE" },
            { "What word does this spell backwards? STRESSED", "DESSERTS" },
            { "Reverse this sentence: \"I love you\"", "you love I" },
        };
        String[] w = words[exerciseIndex % words.length];
        correctAnswer = w[1].toLowerCase();
        if (tvTitle     != null) tvTitle.setText("◈ WORD REVERSAL");
        if (tvChallenge != null) tvChallenge.setText(w[0]);
        setupTextInput("Reversed…");
    }

    // ── Exercise 8: Stroop Challenge ──────────────────────────────────────────
    private void runStroop() {
        String[] words = { "RED", "BLUE", "GREEN", "YELLOW" };
        int[] colors   = { 0xFFFF3333, 0xFF3399FF, 0xFF00CC66, 0xFFFFDD00 };
        String[] colorNames = { "RED", "BLUE", "GREEN", "YELLOW" };
        Random rng = new Random();
        int wi = rng.nextInt(4), ci = rng.nextInt(4);
        // Make sure they mismatch (Stroop effect requires mismatch)
        while (ci == wi) ci = rng.nextInt(4);
        final int finalCi = ci;
        correctAnswer = colorNames[ci];
        if (tvTitle     != null) tvTitle.setText("◈ STROOP CHALLENGE");
        if (tvChallenge != null) {
            tvChallenge.setText("What COLOR is this text printed in?\n(Not the word — the COLOR)");
        }
        // Show the word in a different color
        if (answerContainer != null) {
            answerContainer.removeAllViews();
            TextView wordView = new TextView(this);
            wordView.setText(words[wi]);
            wordView.setTextColor(colors[finalCi]);
            wordView.setTextSize(48f);
            wordView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            wordView.setGravity(android.view.Gravity.CENTER);
            wordView.setPadding(0, 32, 0, 32);
            answerContainer.addView(wordView);

            for (String cn : colorNames) {
                Button b = new Button(this);
                b.setText(cn);
                b.setBackgroundResource(R.drawable.bg_chip);
                b.setTextColor(0xFFFFFFFF);
                b.setTextSize(15f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 6, 0, 6);
                b.setLayoutParams(lp);
                b.setOnClickListener(v -> checkAnswer(cn));
                answerContainer.addView(b);
            }
            if (btnCheck != null) btnCheck.setVisibility(View.GONE);
        }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────
    private void setupTextInput(String hint) {
        if (answerContainer == null) return;
        answerContainer.removeAllViews();
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(0xFF2A6A8A);
        et.setTextColor(0xFF00FF99);
        et.setBackgroundColor(0xFF0A1E30);
        et.setTextSize(16f);
        et.setPadding(16, 16, 16, 16);
        et.setId(View.generateViewId());
        answerContainer.addView(et);
        if (btnCheck != null) {
            btnCheck.setVisibility(View.VISIBLE);
            btnCheck.setOnClickListener(v -> {
                String answer = et.getText().toString().trim().toLowerCase();
                checkAnswer(answer);
            });
        }
    }

    private void setupMCQ(String[] options, int correctIdx, String fallback) {
        if (answerContainer == null) return;
        answerContainer.removeAllViews();
        if (btnCheck != null) btnCheck.setVisibility(View.GONE);
        for (int i = 0; i < options.length; i++) {
            Button b = new Button(this);
            b.setText(options[i]);
            b.setBackgroundResource(R.drawable.bg_chip);
            b.setTextColor(0xFF00FF99);
            b.setTextSize(14f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 6, 0, 6);
            b.setLayoutParams(lp);
            final boolean correct = (correctIdx < 0) || (i == correctIdx);
            b.setOnClickListener(v -> checkAnswer(correct ? correctAnswer : "wrong"));
            answerContainer.addView(b);
        }
    }

    private void checkAnswer(String answer) {
        boolean correct = correctAnswer.equals("describe") || answer.equalsIgnoreCase(correctAnswer);
        if (correct) {
            score += 10; streak++;
            if (tvFeedback != null) {
                tvFeedback.setText("✓ Excellent! Neural pathway reinforced. +" + 10);
                tvFeedback.setTextColor(0xFF00FF99);
            }
            if (ttsReady) tts.speak("Correct! Neural pathway reinforced.", TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            streak = 0;
            if (tvFeedback != null) {
                tvFeedback.setText("✗ Answer: " + correctAnswer + "\nNew pathway forming — that's the point.");
                tvFeedback.setTextColor(0xFFFF4444);
            }
        }
        updateStats(); saveStats();
        if (btnCheck != null) btnCheck.setVisibility(View.GONE);
        if (answerContainer != null) {
            for (int i = 0; i < answerContainer.getChildCount(); i++) {
                View v = answerContainer.getChildAt(i);
                if (v instanceof Button) v.setEnabled(false);
                if (v instanceof EditText) v.setEnabled(false);
            }
        }
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US); ttsReady = true;
        }
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
