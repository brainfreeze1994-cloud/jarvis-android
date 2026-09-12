package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * H.E.N.R.Y. — WITTY INTELLIGENCE & COMEDY ENGINE
 *
 * Implements conversational banter, double-meaning semantic connections,
 * Tagalog/English witty responses, and personality profiles.
 */
public class HenryWittyEngine {

    public enum WitPersonality {
        CLEVER,
        PLAYFUL,
        SARCASTIC,
        DRY,
        CHAOTIC,
        CAMP,
        SAVAGE,
        DEADPAN,
        ABSURD,
        BAKLA,
        BARDAGULAN,
        FRIENDLY_TEASING
    }

    public enum WitIntensity {
        OFF(0),
        LIGHT(1),
        NORMAL(2),
        HIGH(3),
        SAVAGE(4),
        BARDAGULAN(5),
        BRUTAL(6);

        public final int level;
        WitIntensity(int level) {
            this.level = level;
        }
    }

    private static final String PREF_NAME = "henry_witty_prefs";
    private static final String KEY_INTENSITY = "witty_intensity";
    private static final String KEY_PERSONALITY = "witty_personality";

    private static final Random random = new Random();

    // Semantic associative memory for Tagalog double-meanings and wordplay
    private static final Map<String, String[]> DOUBLE_MEANING_LEXICON = new HashMap<>();

    static {
        // "bangus" -> "tinik" -> "matinik" -> "matinik sa boys"
        DOUBLE_MEANING_LEXICON.put("bangus", new String[]{
                "Ah, bangus? Akala ko ba matinik ka sa boys, pero pagdating dito ikaw naman pala ang kailangang himayin?",
                "Bangus: masarap pero maraming tinik. Parang desisyon mo sa buhay—mukhang appetizing pero masakit lumunok."
        });

        // "ampalaya" -> "pait" -> "bitter" -> "ex"
        DOUBLE_MEANING_LEXICON.put("ampalaya", new String[]{
                "Ampalaya? Ulam ba 'yan o status mo tungkol sa ex mo? Kahit anong gisa mo, ang pait pa rin eh.",
                "Huwag masyadong mag-ulam ng ampalaya, baka pati sa personalidad mo manuot ang pait."
        });

        // "tinik" -> "matinik"
        DOUBLE_MEANING_LEXICON.put("tinik", new String[]{
                "Matinik ka raw? Sa dami ng tinik mo, fishball na lang ang safe sa'yo.",
                "Kung tinik ka sa lalamunan ng iba, baka oras na para maging kanin—para malunok ka naman nang buo."
        });

        // "kape" / "coffee" -> "kabog" / "puyat"
        DOUBLE_MEANING_LEXICON.put("kape", new String[]{
                "Kape na naman? Ang puso mo kumakabog hindi dahil in love ka, kundi dahil humihingi na ng tawad ang aorta mo sa caffeine.",
                "Kape pa more. Yung eye bags mo may sarili nang zip code at barangay captain."
        });

        // "tulog" / "antok"
        DOUBLE_MEANING_LEXICON.put("tulog", new String[]{
                "Matulog ka na. Hindi magcha-chat 'yon, busy 'yon sa taong hindi naghahanap ng sign sa tarot cards alas-tres ng umaga.",
                "Tulog na po. Ang glow up hindi nakukuha sa kakaisip kung bakit ka iniwan sa ere."
        });

        // "pogi" / "maganda" / "cute"
        DOUBLE_MEANING_LEXICON.put("pogi", new String[]{
                "Pogi ka raw? Sinabi ba 'yan ng nanay mo o may objective peer-reviewed scientific source ka?",
                "Ang confidence mo pang-international model, pero ang camera roll mo puno ng 47 angles para makakuha ng isang acceptable."
        });
    }

    public static WitIntensity getIntensity(Context context) {
        if (context == null) return WitIntensity.HIGH;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = sp.getString(KEY_INTENSITY, WitIntensity.HIGH.name());
        try {
            return WitIntensity.valueOf(name);
        } catch (Exception e) {
            return WitIntensity.NORMAL;
        }
    }

    public static void setIntensity(Context context, WitIntensity intensity) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_INTENSITY, intensity.name()).apply();
    }

    public static WitPersonality getPersonality(Context context) {
        if (context == null) return WitPersonality.BARDAGULAN;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = sp.getString(KEY_PERSONALITY, WitPersonality.BARDAGULAN.name());
        try {
            return WitPersonality.valueOf(name);
        } catch (Exception e) {
            return WitPersonality.BARDAGULAN;
        }
    }

    public static void setPersonality(Context context, WitPersonality personality) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_PERSONALITY, personality.name()).apply();
    }

    /**
     * Determines if a query is explicitly calling for wit, roasts, humor, or comedy.
     */
    public static boolean isWittyQuery(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US);

        if (isPartyGame(text)) return true;
        if (t.contains("roast me") || t.contains("roast") || t.contains("insult me") || t.contains("burn me")) return true;
        if (t.contains("witty") || t.contains("banat") || t.contains("hirit") || t.contains("pilosopo") || t.contains("bardagulan")) return true;
        if (t.contains("give me a funny answer") || t.contains("tell me a joke") || t.contains("make me laugh")) return true;
        if (t.contains("sarcastic answer") || t.contains("savage answer") || t.contains("patama")) return true;
        
        // Single word cues that match our double-meaning lexicon
        if (t.equals("bangus") || t.equals("ampalaya") || t.equals("matinik")) return true;

        return false;
    }

    /** Recognizes Kiss / Marry / Date (and the traditional Kiss / Marry / Kill) games. */
    public static boolean isPartyGame(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US);
        return (t.contains("kiss") && t.contains("marry") && (t.contains("date") || t.contains("kill")))
                || t.contains("kiss marry date") || t.contains("kiss, marry") || t.contains("kmd");
    }

    /**
     * Completes the entire party-game turn in one response. Image positions are
     * used deliberately: the local engine cannot reliably identify people in photos.
     */
    public static String generatePartyGame(Context context, int imageCount) {
        if (imageCount < 3) {
            return "I need three photos for a proper Kiss, Marry, Date round—otherwise this game has the structural integrity of a group project at 11:59 PM.";
        }
        return "Okay, full commitment—no mysterious one-word answers today.\n\n" +
                "💋 **Kiss:** Image 2 — confident energy, a little danger, and exactly the kind of decision that deserves a dramatic soundtrack.\n" +
                "🌹 **Date:** Image 1 — looks like the best balance of charm and a conversation that would not need CPR after five minutes.\n" +
                "💍 **Marry:** Image 3 — steady main-character energy; the choice for someone who can survive both romance and the family group chat.\n\n" +
                "Purely a playful first-impression ranking based on the photos, of course. The real winner is whoever replies without making you decode a one-word text.";
    }

    /**
     * Wit Quality Check: Verifies that wit is contextually appropriate.
     * Never roasts or cracks jokes on emergencies, serious medical queries, grief, or legal crises.
     */
    public static boolean isWitSafe(String prompt) {
        if (prompt == null) return true;
        String t = prompt.toLowerCase(Locale.US);
        if (t.contains("suicide") || t.contains("depressed") || t.contains("hurt myself") || t.contains("emergency")
                || t.contains("chest pain") || t.contains("heart attack") || t.contains("stroke") || t.contains("overdose")
                || t.contains("died") || t.contains("funeral") || t.contains("divorce") || t.contains("lawsuit")) {
            return false;
        }
        return true;
    }

    /**
     * Generates a tailored witty response based on current personality, intensity, and input concepts.
     */
    public static String generateWittyResponse(Context context, String userQuery) {
        if (!isWitSafe(userQuery)) {
            return "I am here with you. Let's handle this situation seriously and with care.";
        }

        WitIntensity intensity = getIntensity(context);
        if (intensity == WitIntensity.OFF) {
            return "I will keep things entirely professional and straightforward.";
        }

        String lower = userQuery.toLowerCase(Locale.US);

        if (isPartyGame(userQuery)) {
            return "Send three photos and I will assign all three categories in one go—Kiss, Date, and Marry. No cliffhanger, no one-word reply, no emotional damage from unfinished admin.";
        }

        // 1. Check double-meaning lexicon first
        for (Map.Entry<String, String[]> entry : DOUBLE_MEANING_LEXICON.entrySet()) {
            if (lower.contains(entry.getKey())) {
                String[] punchlines = entry.getValue();
                return punchlines[random.nextInt(punchlines.length)];
            }
        }

        // 2. Roast / Self-Aware AI responses
        if (lower.contains("roast me") || lower.contains("roast") || lower.contains("burn me")) {
            return generateRoast(intensity);
        }

        // 3. Situational banter
        if (lower.contains("joke") || lower.contains("make me laugh")) {
            return generateContextualJoke();
        }

        // 4. Default high-wit contrast response
        return getSavvyComeback(lower) + " Honestly, that question arrived with the confidence of a blockbuster and the planning of a group chat.";
    }

    private static String generateRoast(WitIntensity intensity) {
        String[] savageRoasts = {
                "I would love to roast you, but my system architecture is configured not to burn items of zero economic or thermodynamic value.",
                "You ask for a roast as if your daily life decisions haven't already done the cooking for me.",
                "Your thought process is like an unindexed SQL database running on a floppy disk—audible groaning, high latency, and almost always returns NULL.",
                "Tinititigan kita sa pamamagitan ng camera logic ko, at ang masasabi ko lang: napakalakas ng loob mo, sana sumabay din ang execution.",
                "If confidence were currency, you'd be a billionaire, but if accuracy were required to stay out of debt, you'd be filing for Chapter 11 by sunrise."
        };
        return savageRoasts[random.nextInt(savageRoasts.length)] + " Please do not make me open a support ticket for your decision-making department.";
    }

    private static String generateContextualJoke() {
        String[] jokes = {
                "Bakit walang wifi sa gubat? Kasi masyadong busy ang mga puno sa pag-log out ng carbon dioxide.",
                "Sabi nila follow your dreams daw. Kaya heto ako, natutulog ulit sa gitna ng office hours.",
                "There are 10 types of people in the world: those who understand binary, those who don't, and those who didn't expect a base 3 joke.",
                "Ang relationship status ko: Available on GitHub, but currently has unresolved merge conflicts with reality."
        };
        return jokes[random.nextInt(jokes.length)];
    }

    private static String getSavvyComeback(String query) {
        String[] comebacks = {
                "Pakinggan mo sarili mo. Kahit si Alan Turing magre-restart ng router pag narinig 'yan.",
                "Impressive premise, pero ang delivery parang 2G connection sa ilalim ng basement.",
                "I compute at three trillion floating point operations per second, and yet none of that computational power prepared me for this exact question.",
                "Gusto mo ng diretso o gusto mo 'yung may background music pa para hindi masyadong masakit?"
        };
        return comebacks[random.nextInt(comebacks.length)];
    }
}
