package com.jarvis.ai;

import android.content.*;
import android.graphics.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

/**
 * SensorySubstitutionActivity — Cross-Modal Perception Engine
 * Converts sensory information across modalities:
 * - Color → Sound (synesthesia simulator)
 * - Motion → Haptic patterns
 * - Text → Touch patterns (braille-style vibration)
 * - Environment description via HENRY AI
 */
public class SensorySubstitutionActivity extends AppCompatActivity
        implements SensorEventListener, TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SensorManager sensorManager;
    private Vibrator vibrator;

    private TextView tvTitle, tvDescription, tvSensoryOutput;
    private LinearLayout menuLayout, activeLayout;

    private String currentMode = null;
    private volatile boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensory);

        tvTitle        = findViewById(R.id.ss_title);
        tvDescription  = findViewById(R.id.ss_description);
        tvSensoryOutput= findViewById(R.id.ss_output);
        menuLayout     = findViewById(R.id.ss_menu);
        activeLayout   = findViewById(R.id.ss_active);

        sensorManager  = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        vibrator       = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        tts            = new TextToSpeech(this, this);

        showMenu();

        Button btnBack = findViewById(R.id.ss_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            if (currentMode != null) { stopMode(); showMenu(); }
            else finish();
        });
    }

    private void showMenu() {
        currentMode = null;
        running     = false;
        if (menuLayout   != null) menuLayout.setVisibility(View.VISIBLE);
        if (activeLayout != null) activeLayout.setVisibility(View.GONE);
        if (tvTitle      != null) tvTitle.setText("◈ SENSORY SUBSTITUTION");
        if (tvDescription!= null) tvDescription.setText(
            "Experience the world through different senses.\nHENRY translates between modalities.");

        String[][] modes = {
            { "🎵 Color → Sound",     "color_sound"    },
            { "📳 Motion → Haptic",   "motion_haptic"  },
            { "✋ Text → Touch",       "text_touch"     },
            { "🌡 Temp → Color",       "temp_color"     },
            { "🎙 Describe Scene",     "describe_scene" },
            { "🔤 Braille Vibration",  "braille"        },
        };

        LinearLayout menu = menuLayout;
        if (menu == null) return;
        menu.removeAllViews();
        for (String[] m : modes) {
            Button b = new Button(this);
            b.setText(m[0]);
            b.setBackgroundResource(R.drawable.bg_chip);
            b.setTextColor(0xFFFF9944);
            b.setTextSize(14f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 8, 0, 8);
            b.setLayoutParams(lp);
            String key = m[1];
            b.setOnClickListener(v -> startMode(key, m[0]));
            menu.addView(b);
        }
    }

    private void startMode(String key, String label) {
        currentMode = key;
        running     = true;
        if (menuLayout   != null) menuLayout.setVisibility(View.GONE);
        if (activeLayout != null) activeLayout.setVisibility(View.VISIBLE);
        if (tvTitle      != null) tvTitle.setText("◈ " + label.toUpperCase());

        switch (key) {
            case "color_sound":    startColorSound(); break;
            case "motion_haptic":  startMotionHaptic(); break;
            case "text_touch":     startTextTouch(); break;
            case "temp_color":     startTempColor(); break;
            case "describe_scene": startDescribeScene(); break;
            case "braille":        startBraille(); break;
        }
    }

    // ── Mode 1: Color → Sound ─────────────────────────────────────────────────
    private void startColorSound() {
        if (tvDescription != null) tvDescription.setText(
            "Synesthesia mode: each color produces its own tone.\n" +
            "Your visual cortex is now wired to the auditory cortex.");
        String[] colors = {"Red → 440 Hz (A4)", "Orange → 528 Hz", "Yellow → 639 Hz",
                           "Green → 741 Hz", "Blue → 852 Hz", "Violet → 963 Hz"};
        int[] freqs = {440, 528, 639, 741, 852, 963};
        String[] colorNames = {"Red", "Orange", "Yellow", "Green", "Blue", "Violet"};
        int[] colorVals = {0xFFFF3333, 0xFFFF9944, 0xFFFFDD00, 0xFF00CC66, 0xFF00AAFF, 0xFFCC88FF};

        LinearLayout ll = activeLayout;
        if (ll == null) return;
        ll.removeAllViews();
        for (int i = 0; i < colors.length; i++) {
            final int freq = freqs[i];
            final String name = colorNames[i];
            Button b = new Button(this);
            b.setText(colors[i]);
            b.setBackgroundColor(colorVals[i]);
            b.setTextColor(Color.WHITE);
            b.setTextSize(13f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 6, 0, 6);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                playTone(freq, 600);
                if (tvSensoryOutput != null)
                    tvSensoryOutput.setText("🎵 " + name + " → " + freq + " Hz\n\"" + getColorMeaning(name) + "\"");
                if (ttsReady) tts.speak(name + ": " + freq + " hertz", TextToSpeech.QUEUE_FLUSH, null, null);
            });
            ll.addView(b);
        }
    }

    private String getColorMeaning(String color) {
        switch (color) {
            case "Red": return "Energy, urgency, passion — the color of the heart";
            case "Orange": return "Creativity, warmth, enthusiasm — the sunrise";
            case "Yellow": return "Clarity, optimism, intellect — the mind awakens";
            case "Green": return "Growth, healing, balance — nature restores";
            case "Blue": return "Calm, trust, depth — the sky and ocean merge";
            case "Violet": return "Intuition, mystery, transcendence — the crown opens";
            default: return "";
        }
    }

    // ── Mode 2: Motion → Haptic ───────────────────────────────────────────────
    private void startMotionHaptic() {
        if (tvDescription != null) tvDescription.setText(
            "Tilt your device. Motion is translated into haptic vibration patterns.\n" +
            "Feel the world through touch.");
        if (tvSensoryOutput != null) tvSensoryOutput.setText("Move your device to feel it…");

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // ── Mode 3: Text → Touch ──────────────────────────────────────────────────
    private void startTextTouch() {
        if (tvDescription != null) tvDescription.setText(
            "Type a word. Feel it as a vibration pattern.\n" +
            "Each letter has its own haptic signature.");
        LinearLayout ll = activeLayout;
        if (ll == null) return;
        ll.removeAllViews();
        EditText et = new EditText(this);
        et.setHint("Type a word or phrase…");
        et.setHintTextColor(0xFF2A6A8A);
        et.setTextColor(0xFF00D4FF);
        et.setBackgroundColor(0xFF0A1E30);
        et.setTextSize(16f);
        ll.addView(et);
        Button b = new Button(this);
        b.setText("FEEL IT");
        b.setBackgroundResource(R.drawable.bg_chip);
        b.setTextColor(0xFFFF9944);
        b.setOnClickListener(v -> {
            String word = et.getText().toString().trim().toUpperCase();
            if (!word.isEmpty()) vibrateWord(word);
        });
        ll.addView(b);
    }

    private void vibrateWord(String word) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        new Thread(() -> {
            for (char c : word.toCharArray()) {
                int pattern = c - 'A' + 1; // A=1, B=2, etc.
                for (int i = 0; i < pattern && i < 6; i++) {
                    vibrator.vibrate(80);
                    try { Thread.sleep(120); } catch (Exception e) { break; }
                }
                try { Thread.sleep(300); } catch (Exception ignored) {}
            }
        }).start();
        if (tvSensoryOutput != null)
            tvSensoryOutput.setText("📳 Vibrating: " + word);
    }

    // ── Mode 4: Temp → Color ──────────────────────────────────────────────────
    private void startTempColor() {
        if (tvDescription != null) tvDescription.setText(
            "Thermal synesthesia: feel temperature as color.\n" +
            "Slide to experience the thermal spectrum.");
        LinearLayout ll = activeLayout;
        if (ll == null) return;
        ll.removeAllViews();
        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(50);
        ll.addView(sb);
        View colorBlock = new View(this);
        colorBlock.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 120));
        ll.addView(colorBlock);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int color = tempToColor(progress);
                colorBlock.setBackgroundColor(color);
                String desc = progress < 20 ? "🧊 Freezing — deep blue, crystal silence" :
                              progress < 40 ? "❄ Cool — blue-green, refreshing" :
                              progress < 60 ? "🌡 Warm — neutral, comfortable" :
                              progress < 80 ? "🔥 Hot — orange, intense" :
                              "☀ Scorching — red-white, overwhelm";
                if (tvSensoryOutput != null) tvSensoryOutput.setText(desc + "\n" + progress + "°C equivalent");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private int tempToColor(int pct) {
        // 0=blue (cold), 50=green (neutral), 100=red (hot)
        if (pct < 50) {
            float t = pct / 50f;
            int r = (int)(0   + t * 0);
            int g = (int)(0   + t * 200);
            int b = (int)(255 - t * 100);
            return Color.rgb(r, g, b);
        } else {
            float t = (pct - 50) / 50f;
            int r = (int)(0   + t * 255);
            int g = (int)(200 - t * 200);
            int b = (int)(155 - t * 155);
            return Color.rgb(r, g, b);
        }
    }

    // ── Mode 5: AI Scene Description ──────────────────────────────────────────
    private void startDescribeScene() {
        if (tvDescription != null) tvDescription.setText(
            "HENRY uses AI to describe any scene in rich sensory detail:\n" +
            "what you would hear, smell, feel, and taste — not just see.");
        LinearLayout ll = activeLayout;
        if (ll == null) return;
        ll.removeAllViews();
        EditText et = new EditText(this);
        et.setHint("Describe a place or situation…");
        et.setHintTextColor(0xFF2A6A8A);
        et.setTextColor(0xFF00D4FF);
        et.setBackgroundColor(0xFF0A1E30);
        et.setMinLines(3);
        et.setTextSize(14f);
        ll.addView(et);
        Button b = new Button(this);
        b.setText("EXPERIENCE ALL SENSES");
        b.setBackgroundResource(R.drawable.bg_chip);
        b.setTextColor(0xFFFF9944);
        b.setOnClickListener(v -> {
            String scene = et.getText().toString().trim();
            if (scene.isEmpty()) return;
            if (tvSensoryOutput != null) tvSensoryOutput.setText("Activating all senses…");
            String prompt = "Describe this scene using ALL five senses — what you hear, smell, taste, feel (touch/temperature/texture), and see. Be vivid and immersive: \"" + scene + "\"";
            List<HistoryItem> h = new ArrayList<>();
            h.add(new HistoryItem("user", prompt));
            JarvisApi.ask(h, null, "balanced", null, new JarvisApi.Callback() {
                @Override public void onSuccess(String reply, String imageUrl, List<String> fu) {
                    String clean = reply.replaceAll("\\[EMOTION:[^]]+]", "").trim();
                    handler.post(() -> {
                        if (tvSensoryOutput != null) tvSensoryOutput.setText(clean);
                        if (ttsReady) tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, null);
                    });
                }
                @Override public void onError(String e) {
                    handler.post(() -> { if (tvSensoryOutput != null) tvSensoryOutput.setText("Error: " + e); });
                }
            });
        });
        ll.addView(b);
    }

    // ── Mode 6: Braille Vibration ──────────────────────────────────────────────
    private void startBraille() {
        if (tvDescription != null) tvDescription.setText(
            "Feel text as braille-inspired vibration patterns.\n" +
            "Long pulses = raised dots in top row. Short pulses = bottom row.");
        LinearLayout ll = activeLayout;
        if (ll == null) return;
        ll.removeAllViews();
        EditText et = new EditText(this);
        et.setHint("Type a short message…");
        et.setHintTextColor(0xFF2A6A8A);
        et.setTextColor(0xFF00D4FF);
        et.setBackgroundColor(0xFF0A1E30);
        et.setTextSize(16f);
        ll.addView(et);
        Button b = new Button(this);
        b.setText("TRANSMIT VIA TOUCH");
        b.setBackgroundResource(R.drawable.bg_chip);
        b.setTextColor(0xFFFF9944);
        b.setOnClickListener(v -> {
            String msg = et.getText().toString().trim();
            if (!msg.isEmpty()) {
                if (tvSensoryOutput != null) tvSensoryOutput.setText("📳 Transmitting: " + msg + "\n\nEach letter = unique vibration signature.");
                vibrateWord(msg.toUpperCase());
            }
        });
        ll.addView(b);
    }

    // ── Tone generator ────────────────────────────────────────────────────────
    private void playTone(int freqHz, int durationMs) {
        new Thread(() -> {
            int sampleRate = 44100;
            int numSamples = sampleRate * durationMs / 1000;
            short[] samples = new short[numSamples];
            for (int i = 0; i < numSamples; i++) {
                double t = (double) i / sampleRate;
                double envelope = Math.min(1.0, Math.min(i / 1000.0, (numSamples - i) / 1000.0));
                samples[i] = (short) (envelope * 32767 * Math.sin(2 * Math.PI * freqHz * t));
            }
            AudioTrack track = new AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                samples.length * 2, AudioTrack.MODE_STATIC);
            track.write(samples, 0, samples.length);
            track.play();
            try { Thread.sleep(durationMs + 100); } catch (Exception ignored) {}
            track.stop(); track.release();
        }).start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!running || !currentMode.equals("motion_haptic")) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        float magnitude = (float) Math.sqrt(x*x + y*y + z*z);
        if (magnitude > 12f && vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate((long)(magnitude * 10));
        }
        if (tvSensoryOutput != null) {
            handler.post(() -> tvSensoryOutput.setText(String.format(
                "Motion: X=%.1f  Y=%.1f  Z=%.1f\nIntensity: %.1f\n%s",
                x, y, z, magnitude,
                magnitude > 14 ? "🔴 STRONG — heavy vibration" :
                magnitude > 11 ? "🟡 MEDIUM — moderate vibration" :
                "🟢 GENTLE — soft vibration")));
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void stopMode() {
        running = false;
        sensorManager.unregisterListener(this);
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            tts.setPitch(0.85f);
            tts.setSpeechRate(0.88f);
            ttsReady = true;
        }
    }

    @Override protected void onDestroy() {
        stopMode();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
