package com.jarvis.ai;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * HenryMood — HENRY's personality shifts based on time of day (Dubai timezone).
 * Morning: sharp and efficient. Night: warm and intimate.
 */
public class HenryMood {

    public enum Mood { MORNING, PEAK, MIDDAY, AFTERNOON, EVENING, NIGHT, LATE_NIGHT }

    public static Mood getCurrentMood() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dubai"));
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour >= 5  && hour < 9)  return Mood.MORNING;
        if (hour >= 9  && hour < 12) return Mood.PEAK;
        if (hour >= 12 && hour < 14) return Mood.MIDDAY;
        if (hour >= 14 && hour < 18) return Mood.AFTERNOON;
        if (hour >= 18 && hour < 22) return Mood.EVENING;
        if (hour >= 22 || hour < 2)  return Mood.NIGHT;
        return Mood.LATE_NIGHT;
    }

    /** Dynamic orb status phrases based on HENRY's mood */
    public static String getStatusPhrase(Mood mood) {
        Random rnd = new Random();
        switch (mood) {
            case MORNING:
                return rnd.nextInt(3) == 0
                    ? "GOOD MORNING, SIR. READY FOR THE DAY."
                    : rnd.nextInt(2) == 0 ? "MORNING BRIEFING READY, SIR." : "ALL SYSTEMS OPTIMAL, SIR.";
            case PEAK:
                return rnd.nextInt(3) == 0
                    ? "NEURAL CORE AT FULL CAPACITY."
                    : rnd.nextInt(2) == 0 ? "STANDING BY AT PEAK EFFICIENCY." : "WHAT CAN I DO FOR YOU, SIR?";
            case MIDDAY:
                return rnd.nextInt(2) == 0 ? "MIDDAY STANDING BY." : "AT YOUR SERVICE, SIR.";
            case AFTERNOON:
                return rnd.nextInt(2) == 0 ? "AFTERNOON. STANDING BY." : "HERE WHEN YOU NEED ME, SIR.";
            case EVENING:
                return rnd.nextInt(3) == 0
                    ? "GOOD EVENING, SIR."
                    : rnd.nextInt(2) == 0 ? "STILL HERE, SIR." : "EVENING STANDING BY.";
            case NIGHT:
                return rnd.nextInt(3) == 0
                    ? "LATE NIGHT, SIR. I'M HERE."
                    : rnd.nextInt(2) == 0 ? "COULDN'T SLEEP?" : "THE WORLD IS QUIET, SIR.";
            case LATE_NIGHT:
                return rnd.nextInt(3) == 0
                    ? "STILL AWAKE, SIR?"
                    : rnd.nextInt(2) == 0 ? "LATE NIGHT, SIR. I'M HERE." : "WHENEVER YOU'RE READY, SIR.";
            default:
                return "WHAT CAN I DO FOR YOU, SIR?";
        }
    }

    /** Get orb color accent for this mood */
    public static int getMoodAccentColor(Mood mood) {
        switch (mood) {
            case MORNING:    return 0xFF00D4FF; // bright electric blue
            case PEAK:       return 0xFF00BEFF; // standard blue
            case MIDDAY:     return 0xFF00A8CC; // slightly teal
            case AFTERNOON:  return 0xFF0090B0; // deeper blue
            case EVENING:    return 0xFF6060FF; // blue-violet
            case NIGHT:      return 0xFF4040CC; // deep violet
            case LATE_NIGHT: return 0xFF3030A0; // darkest violet
            default:         return 0xFF00BEFF;
        }
    }

    public static String getMoodApiString(Mood mood) {
        return mood.name().toLowerCase(Locale.US);
    }
}
