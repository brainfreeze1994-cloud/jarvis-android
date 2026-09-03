package com.jarvis.ai;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HENRY Offline Autonomous Brain
 * Provides comprehensive on-device reasoning, mathematical calculation,
 * science/chemistry lookup, astronomy facts, and assistant intelligence
 * when the device is offline or remote servers are unreachable.
 */
public class HenryOfflineBrain {

    public static String generateOfflineResponse(String input, String intentType, Context context) {
        if (input == null || input.trim().isEmpty()) {
            return "[EMOTION:calm] I am here and listening, sir. Operating in offline tactical mode.";
        }

        String raw = input.trim();
        String lower = raw.toLowerCase(Locale.US);

        // 1. Math calculation
        String mathResult = evaluateMath(lower);
        if (mathResult != null) {
            return "[EMOTION:proud] " + mathResult;
        }

        // 2. Greetings and identity
        if (lower.matches(".*\\b(hello|hi|hey|good morning|good evening|good afternoon|greetings)\\b.*")) {
            return "[EMOTION:warm] Greetings, sir! I am HENRY (Hyperintelligence Engine Neural Reasoning Yield). I am operating in Offline Tactical Mode with all local sensors, chemistry matrices, and on-device features fully active.";
        }
        if (lower.matches(".*\\b(who are you|what is your name|your name|introduce yourself)\\b.*")) {
            return "[EMOTION:confident] I am HENRY, your personal artificial intelligence assistant. I am engineered to manage tasks, scan environments, analyze molecular structures, track orbital trajectories, and assist you whether online or completely offline.";
        }
        if (lower.matches(".*\\b(how are you|how are you doing|status report|system status)\\b.*")) {
            return "[EMOTION:focused] All on-device sub-routines are operating at 100% efficiency, sir. Neural reasoning yields are nominal, and local hardware sensors are ready for your command.";
        }

        // 3. Time & Date
        if (lower.contains("what time") || lower.contains("current time") || lower.contains("what is the time")) {
            String time = new SimpleDateFormat("hh:mm a", Locale.US).format(new Date());
            return "[EMOTION:calm] The current local time is " + time + ", sir.";
        }
        if (lower.contains("what day") || lower.contains("what date") || lower.contains("today's date")) {
            String date = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(new Date());
            return "[EMOTION:calm] Today is " + date + ", sir.";
        }

        // 4. Feature Navigation Guidance
        if (lower.contains("plant") && (lower.contains("scan") || lower.contains("identify") || lower.contains("open"))) {
            return "[EMOTION:helpful] You can launch the Plant Scanner at any time to identify flora, botanical species, and health metrics using your camera.";
        }
        if (lower.contains("animal") && (lower.contains("scan") || lower.contains("identify") || lower.contains("open"))) {
            return "[EMOTION:helpful] Launching the Animal Scanner allows you to classify wildlife, pets, and fauna using our high-precision computer vision pipeline.";
        }
        if (lower.contains("chemistry") || lower.contains("periodic") || lower.contains("element")) {
            return "[EMOTION:excited] The HENRY Periodic Matrix features all 118 chemical elements, Bohr atomic orbitals, and a multi-element Molecular Reaction Mixer capable of synthesizing complex compounds and predicting novel materials.";
        }
        if (lower.contains("asteroid") || lower.contains("space") || lower.contains("orbit")) {
            return "[EMOTION:focused] The HENRY Deep Space Orbital Monitor tracks real-time Near-Earth Objects (NEOs), trajectory distances, close approach countdowns, and the International Space Station.";
        }

        // 5. Chemistry & Science Encyclopedia (Offline)
        if (lower.contains("what is water") || lower.contains("chemical formula of water")) {
            return "[EMOTION:informative] Water (H₂O) is a polar inorganic compound consisting of two hydrogen atoms covalently bonded to a single oxygen atom at a 104.5° bond angle. It possesses anomalous density, high surface tension, and is essential for all known life.";
        }
        if (lower.contains("what is salt") || lower.contains("chemical formula of salt")) {
            return "[EMOTION:informative] Table salt is Sodium Chloride (NaCl), an ionic lattice compound formed by the transfer of an electron from sodium to chlorine. It forms cubic crystals and dissolves into Na⁺ and Cl⁻ electrolytes in water.";
        }
        if (lower.contains("gold") && lower.contains("element")) {
            return "[EMOTION:informative] Gold (Au, Z=79) is a dense, malleable transition metal prized for its corrosion resistance, electrical conductivity, and lustrous yellow color. Its atomic mass is 196.97 u.";
        }
        if (lower.contains("carbon") && lower.contains("element")) {
            return "[EMOTION:informative] Carbon (C, Z=6) is the foundational element of organic chemistry. It can form four covalent bonds and exists in allotropes ranging from soft graphite to ultra-hard diamond and ballistic graphene.";
        }
        if (lower.contains("photosynthesis")) {
            return "[EMOTION:informative] Photosynthesis is the biochemical process where photoautotrophs convert sunlight, carbon dioxide, and water into glucose and oxygen: 6CO₂ + 6H₂O + photons → C₆H₁₂O₆ + 6O₂.";
        }

        // 6. Astronomy & Space Facts
        if (lower.contains("how far is the moon") || lower.contains("distance to moon")) {
            return "[EMOTION:informative] The Moon is on average 384,400 kilometers (238,855 miles) away from Earth, equivalent to roughly 30 Earth diameters or 1.28 light-seconds.";
        }
        if (lower.contains("how fast is the iss") || lower.contains("speed of iss")) {
            return "[EMOTION:informative] The International Space Station orbits Earth at approximately 27,600 km/h (17,150 mph or 7.66 km/s), completing an entire orbit around the planet every 92 minutes.";
        }
        if (lower.contains("nearest star")) {
            return "[EMOTION:informative] The nearest star to Earth after our Sun is Proxima Centauri, located approximately 4.2465 light-years (40.17 trillion kilometers) away in the Alpha Centauri system.";
        }

        // 7. General Offline Fallback
        return "[EMOTION:calm] Operating in Autonomous Offline Mode, sir. All core on-device features, hardware sensors, camera scanners, chemistry matrices, and local calculators are functioning smoothly. Feel free to give me voice or touch commands.";
    }

    private static String evaluateMath(String query) {
        try {
            // Check for simple expressions like "25 * 4", "100 / 4", "sqrt(144)", "15% of 200"
            Pattern pctPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%\\s*of\\s*(\\d+(?:\\.\\d+)?)");
            Matcher mPct = pctPattern.matcher(query);
            if (mPct.find()) {
                double p = Double.parseDouble(mPct.group(1));
                double total = Double.parseDouble(mPct.group(2));
                double res = (p / 100.0) * total;
                return String.format(Locale.US, "%.2f%% of %.2f is %.4f", p, total, res);
            }

            Pattern sqrtPattern = Pattern.compile("sqrt\\s*\\(?\\s*(\\d+(?:\\.\\d+)?)\\s*\\)?");
            Matcher mSqrt = sqrtPattern.matcher(query);
            if (mSqrt.find()) {
                double val = Double.parseDouble(mSqrt.group(1));
                return String.format(Locale.US, "The square root of %s is %.4f", mSqrt.group(1), Math.sqrt(val));
            }

            Pattern mathPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([+\\-*/x×÷])\\s*(\\d+(?:\\.\\d+)?)");
            Matcher mMath = mathPattern.matcher(query);
            if (mMath.find()) {
                double a = Double.parseDouble(mMath.group(1));
                String op = mMath.group(2);
                double b = Double.parseDouble(mMath.group(3));
                double res = 0;
                switch (op) {
                    case "+": res = a + b; break;
                    case "-": res = a - b; break;
                    case "*":
                    case "x":
                    case "×": res = a * b; break;
                    case "/":
                    case "÷":
                        if (b == 0) return "Division by zero is undefined, sir.";
                        res = a / b;
                        break;
                }
                return String.format(Locale.US, "Calculation result: %s %s %s = %.4f", mMath.group(1), op, mMath.group(3), res);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
