package com.jarvis.ai;

import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

/**
 * MentalImageryActivity — Visual Imagination Engine
 * HENRY guides you through vivid mental visualizations:
 * relaxation scenes, memory palace, creative visualization, dream mapping.
 */
public class MentalImageryActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvScene;
    private TextView tvPrompt;
    private ProgressBar progressBar;
    private Button btnNext, btnPause, btnBack;
    private LinearLayout btnContainer;

    private int currentStep = 0;
    private boolean paused = false;
    private Runnable nextStepRunnable;
    private String[] currentScript;

    // Visualization categories
    private static final String[][] CATEGORIES = {
        { "🌊 Ocean Calm",          "ocean_calm"     },
        { "🏔 Mountain Peak",        "mountain"       },
        { "🧠 Memory Palace",        "memory_palace"  },
        { "✨ Creative Vision",      "creative"       },
        { "🌌 Cosmic Journey",       "cosmic"         },
        { "🌿 Forest Healing",       "forest"         },
        { "🔥 Peak Performance",     "performance"    },
        { "💡 Problem Solving",      "problem_solve"  },
    };

    private static final Map<String, String[]> SCRIPTS = new HashMap<>();

    static {
        SCRIPTS.put("ocean_calm", new String[]{
            "Close your eyes. Take a deep breath in... and slowly release.",
            "You are standing at the edge of a vast, calm ocean at golden hour. Feel the warm sand between your toes.",
            "A gentle wave rolls in, just touching your feet. The water is perfectly warm — like a bath.",
            "Hear the rhythm: the slow crash, the soft retreat, the silence, and again.",
            "The horizon glows amber and rose. You are completely safe. Completely at peace.",
            "With each breath, feel yourself becoming calmer. Each exhale releases tension.",
            "You are as vast and deep as this ocean. Boundless. Peaceful. Powerful.",
            "Carry this feeling with you. Open your eyes when you are ready."
        });
        SCRIPTS.put("mountain", new String[]{
            "You are at the base of a magnificent mountain. The air is crisp and clean.",
            "With each step upward, your mind becomes clearer. Leave every worry on the trail below.",
            "Halfway up, you pause. See the world spread below you — cities, rivers, forests.",
            "You continue. The peak is near. The air is thin but exhilarating.",
            "You reach the summit. You are standing at the top of the world.",
            "From here, every problem looks small. Every challenge is conquerable.",
            "Breathe in the pure mountain air. You have the strength to face anything.",
            "Descend with clarity. You carry the mountain's power within you."
        });
        SCRIPTS.put("memory_palace", new String[]{
            "Imagine a place you know perfectly — your childhood home, a school, a building.",
            "Walk through the front entrance. Notice every detail: the light, the smell, the texture.",
            "Each room in this palace is a vault. Place what you wish to remember in specific spots.",
            "In the living room, place the first item. Give it an unusual, vivid image — make it absurd.",
            "Walk to the kitchen. Place the next item on the counter. See it clearly, in 3D, in color.",
            "Your brain remembers stories and places better than lists. This palace is your superpower.",
            "Walk through your palace again. Each room triggers its memory automatically.",
            "You now have a memory palace. Return here whenever you need to store or retrieve."
        });
        SCRIPTS.put("creative", new String[]{
            "You are entering your creative mind. This space has no rules, no limits, no judgments.",
            "Imagine a blank canvas as large as a wall. What color calls to you first?",
            "Let shapes and forms appear without forcing them. Your subconscious is the artist.",
            "Hear music that doesn't exist yet — your own private symphony.",
            "A figure forms in the mist. This is your creative self, the part that knows no boundaries.",
            "Ask it one question: what do you want to create? Listen. The answer will come.",
            "Your creativity is not blocked. It is simply waiting for your permission.",
            "Open your eyes. The canvas is ready. You are ready."
        });
        SCRIPTS.put("cosmic", new String[]{
            "You are floating in space. Below you, the Earth — a blue marble, glowing softly.",
            "The silence is absolute. The beauty is infinite. You are weightless and free.",
            "Stars surround you in every direction. Each one a sun with worlds of its own.",
            "You are made of stardust — the same atoms that forged galaxies billions of years ago.",
            "Zoom out. The solar system becomes a speck. The galaxy a river of light.",
            "You are both infinitely small and infinitely connected. You are part of everything.",
            "The universe is 13.8 billion years old. You are here, right now, aware. That is extraordinary.",
            "Return gently. Bring back the perspective of the cosmos."
        });
        SCRIPTS.put("forest", new String[]{
            "You are walking into an ancient forest. Sunlight filters through the canopy above.",
            "The ground is soft with moss. Each step is cushioned, silent, slow.",
            "Trees tower around you — centuries old, patient, deeply rooted.",
            "Touch the bark of a great oak. Feel its rough texture. This tree has lived through history.",
            "A stream appears, crystal clear. Sit beside it. Watch the water move over smooth stones.",
            "Breathe in the scent of pine, earth, and rain. Your nervous system is healing right now.",
            "In this forest, you are held by nature. You belong here. You always have.",
            "Take three deep breaths. Fill your lungs with forest air. Carry its healing with you."
        });
        SCRIPTS.put("performance", new String[]{
            "You are backstage. In moments, you will step onto the stage of your peak performance.",
            "Visualize the scene in perfect detail. See exactly what success looks like.",
            "Feel your body — strong, capable, precisely prepared. Every muscle knows what to do.",
            "See yourself executing perfectly. Every move, every word, every decision — flawless.",
            "The crowd, the challenge, the pressure — you welcome it. Pressure creates diamonds.",
            "See the moment of triumph. Feel it completely — the surge of pride and accomplishment.",
            "This vision is a rehearsal. Your brain cannot distinguish imagination from reality.",
            "You have already succeeded in your mind. Now go make it real."
        });
        SCRIPTS.put("problem_solve", new String[]{
            "Bring your unsolved problem into the space before you. Give it a shape, a color, a texture.",
            "Look at it from above. From the side. From behind. See it from every angle.",
            "What if the opposite approach were true? What would that look like?",
            "Shrink the problem to the size of a marble. Hold it in your palm. It is manageable.",
            "Ask your subconscious: what is the simplest solution? Breathe. Wait. Listen.",
            "The answer exists. Your mind has all the pieces. You are assembling them now.",
            "A door appears. On the other side is the solution, fully formed.",
            "Open the door. Step through. You have your answer."
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mental_imagery);

        tvScene     = findViewById(R.id.mi_scene_text);
        tvPrompt    = findViewById(R.id.mi_prompt);
        progressBar = findViewById(R.id.mi_progress);
        btnNext     = findViewById(R.id.mi_btn_next);
        btnPause    = findViewById(R.id.mi_btn_pause);
        btnBack     = findViewById(R.id.mi_btn_back);
        btnContainer = findViewById(R.id.mi_category_container);

        tts = new TextToSpeech(this, this);

        showCategoryMenu();

        if (btnBack != null) btnBack.setOnClickListener(v -> {
            stopVisualization();
            if (currentScript != null) {
                currentScript = null; showCategoryMenu();
            } else finish();
        });

        if (btnPause != null) btnPause.setOnClickListener(v -> {
            paused = !paused;
            btnPause.setText(paused ? "▶ RESUME" : "⏸ PAUSE");
            if (!paused && currentScript != null) scheduleNextStep();
        });

        if (btnNext != null) btnNext.setOnClickListener(v -> {
            if (nextStepRunnable != null) handler.removeCallbacks(nextStepRunnable);
            if (currentScript != null) advanceStep();
        });
    }

    private void showCategoryMenu() {
        currentScript = null;
        currentStep   = 0;
        if (tvScene   != null) tvScene.setText("◈ CHOOSE A VISUALIZATION");
        if (tvPrompt  != null) tvPrompt.setText("Select a mental journey to begin.");
        if (progressBar != null) progressBar.setProgress(0);
        if (btnPause  != null) btnPause.setVisibility(View.GONE);
        if (btnNext   != null) btnNext.setVisibility(View.GONE);
        if (btnContainer != null) {
            btnContainer.setVisibility(View.VISIBLE);
            btnContainer.removeAllViews();
            for (String[] cat : CATEGORIES) {
                Button b = new Button(this);
                b.setText(cat[0]);
                b.setBackgroundResource(R.drawable.bg_chip);
                b.setTextColor(0xFF00D4FF);
                b.setTextSize(13f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 6, 0, 6);
                b.setLayoutParams(lp);
                String key = cat[1];
                b.setOnClickListener(v -> startVisualization(key, cat[0]));
                btnContainer.addView(b);
            }
        }
    }

    private void startVisualization(String key, String title) {
        currentScript = SCRIPTS.get(key);
        if (currentScript == null) return;
        currentStep = 0;
        paused      = false;
        if (btnContainer != null) btnContainer.setVisibility(View.GONE);
        if (btnPause != null) { btnPause.setVisibility(View.VISIBLE); btnPause.setText("⏸ PAUSE"); }
        if (btnNext  != null) btnNext.setVisibility(View.VISIBLE);
        if (tvScene  != null) tvScene.setText("◈ " + title.toUpperCase());
        if (progressBar != null) { progressBar.setMax(currentScript.length); progressBar.setProgress(0); }
        advanceStep();
    }

    private void advanceStep() {
        if (currentScript == null) return;
        if (currentStep >= currentScript.length) {
            finishVisualization(); return;
        }
        String line = currentScript[currentStep];
        if (tvPrompt != null) tvPrompt.setText(line);
        if (progressBar != null) progressBar.setProgress(currentStep + 1);
        if (ttsReady) {
            tts.speak(line, TextToSpeech.QUEUE_FLUSH, null, "mi_" + currentStep);
        }
        currentStep++;
        if (!paused) scheduleNextStep();
    }

    private void scheduleNextStep() {
        if (nextStepRunnable != null) handler.removeCallbacks(nextStepRunnable);
        int words = currentScript != null && currentStep > 0
            ? currentScript[currentStep - 1].split("\\s+").length : 20;
        long delay = Math.max(5000, words * 420L); // ~420ms per word
        nextStepRunnable = this::advanceStep;
        handler.postDelayed(nextStepRunnable, delay);
    }

    private void stopVisualization() {
        if (nextStepRunnable != null) handler.removeCallbacks(nextStepRunnable);
        if (ttsReady) tts.stop();
    }

    private void finishVisualization() {
        if (tvScene  != null) tvScene.setText("◈ VISUALIZATION COMPLETE");
        if (tvPrompt != null) tvPrompt.setText("Take a moment to return fully to the present.\n\nHow do you feel?");
        if (ttsReady) tts.speak("Visualization complete. Take a moment to return fully to the present.", TextToSpeech.QUEUE_FLUSH, null, "mi_done");
        if (btnPause != null) btnPause.setVisibility(View.GONE);
        if (btnNext  != null) btnNext.setVisibility(View.GONE);
        handler.postDelayed(this::showCategoryMenu, 6000);
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(java.util.Locale.US);
            tts.setPitch(0.85f);
            tts.setSpeechRate(0.80f);
            ttsReady = true;
        }
    }

    @Override protected void onDestroy() {
        stopVisualization();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
