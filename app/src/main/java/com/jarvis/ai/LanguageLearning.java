package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Language Learning — spaced repetition flashcards + daily lessons.
 * "Learn Spanish", "Spanish lesson", "Teach me French phrases",
 * "Practice Arabic", "Language flashcard", "Daily language lesson"
 */
public class LanguageLearning {

    private static final String PREFS      = "lang_prefs";
    private static final String KEY_LANG   = "learn_lang";
    private static final String KEY_LEVEL  = "learn_level";
    private static final String KEY_STREAK = "learn_streak";
    private static final String KEY_DATE   = "learn_date";
    private static final String KEY_CARDS  = "learn_cards";

    public interface Callback {
        void onResult(String content);
        void onError(String reason);
    }

    public static boolean isLangCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("learn ") && hasLangKeyword(lower) ||
               lower.contains("language lesson") || lower.contains("language flashcard") ||
               lower.contains("daily lesson") || lower.contains("practice my") ||
               lower.contains("teach me") && hasLangKeyword(lower) ||
               lower.contains("language practice") || lower.contains("vocab lesson") ||
               lower.contains("i want to learn") || lower.contains("language learning");
    }

    private static boolean hasLangKeyword(String lower) {
        String[] langs = {"spanish","french","arabic","japanese","korean","mandarin","chinese",
                          "german","italian","portuguese","hindi","tagalog","russian","dutch",
                          "turkish","thai","vietnamese","malay","indonesian","greek"};
        for (String l : langs) if (lower.contains(l)) return true;
        return false;
    }

    private static String detectLanguage(String lower) {
        String[][] langs = {
            {"spanish","Spanish"},{"french","French"},{"arabic","Arabic"},
            {"japanese","Japanese"},{"korean","Korean"},{"mandarin","Mandarin"},
            {"chinese","Chinese"},{"german","German"},{"italian","Italian"},
            {"portuguese","Portuguese"},{"hindi","Hindi"},{"tagalog","Tagalog"},
            {"russian","Russian"},{"dutch","Dutch"},{"turkish","Turkish"},
            {"thai","Thai"},{"vietnamese","Vietnamese"},{"malay","Malay"},
            {"indonesian","Indonesian"},{"greek","Greek"}
        };
        for (String[] l : langs) if (lower.contains(l[0])) return l[1];
        return null;
    }

    public static void handle(Context ctx, String userQuery, OkHttpClient httpClient,
                              UserProfile profile, Callback cb) {
        String lower = userQuery.toLowerCase(Locale.US);
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Detect or recall target language
        String lang = detectLanguage(lower);
        if (lang != null) {
            prefs.edit().putString(KEY_LANG, lang).apply();
        } else {
            lang = prefs.getString(KEY_LANG, "Spanish");
        }

        // Update streak
        String today = new java.text.SimpleDateFormat("yyyyMMdd", Locale.US).format(new java.util.Date());
        String lastDate = prefs.getString(KEY_DATE, "");
        int streak = prefs.getInt(KEY_STREAK, 0);
        if (!today.equals(lastDate)) {
            streak++;
            prefs.edit().putString(KEY_DATE, today).putInt(KEY_STREAK, streak).apply();
        }

        String level = prefs.getString(KEY_LEVEL, "beginner");
        if (lower.contains("advanced")) level = "advanced";
        else if (lower.contains("intermediate")) level = "intermediate";
        else if (lower.contains("beginner") || lower.contains("basic")) level = "beginner";
        prefs.edit().putString(KEY_LEVEL, level).apply();

        final String finalLang = lang;
        final int finalStreak  = streak;

        boolean isFlashcard = lower.contains("flashcard") || lower.contains("quiz") || lower.contains("test me");
        boolean isPhrases   = lower.contains("phrase") || lower.contains("sentence") || lower.contains("say");
        boolean isLesson    = !isFlashcard && !isPhrases;

        String systemPrompt;
        if (isFlashcard) {
            systemPrompt = "You are H.E.N.R.Y, a language tutor. Create 5 spaced-repetition flashcards " +
                "for " + level + " " + finalLang + ". Format each as:\n" +
                "**[English]** → **[" + finalLang + "]** (pronunciation: _[phonetic]_)\n" +
                "Include: 1 greeting, 1 number, 1 common verb, 1 food word, 1 useful phrase.";
        } else if (isPhrases) {
            systemPrompt = "You are H.E.N.R.Y, a language tutor. Give 8 essential " + level + " " +
                finalLang + " phrases for daily use. Format:\n" +
                "**[English phrase]**\n[" + finalLang + " translation] (_phonetic_)\n\n" +
                "Cover: greetings, shopping, directions, emergency, polite expressions.";
        } else {
            systemPrompt = "You are H.E.N.R.Y, a friendly language tutor teaching " + finalLang + " at " +
                level + " level. Deliver a short, engaging daily lesson. Include:\n" +
                "**📚 Today's Topic:** [one clear topic]\n" +
                "**🔤 5 New Words:** [word → translation → pronunciation]\n" +
                "**💬 2 Example Sentences:** [in " + finalLang + " with translation]\n" +
                "**🎯 Mini Challenge:** [one simple exercise for the user to try]\n" +
                "**🔥 Streak:** " + finalStreak + " day(s)! Keep it up!\n" +
                "Be encouraging, clear, and fun.";
        }

        new Thread(() -> {
            try {
                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", userQuery);
                msgs.put(msg);

                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "detailed");
                body.put("systemOverride", systemPrompt);

                RequestBody rb = RequestBody.create(
                    body.toString(), MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                    .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                    .post(rb).addHeader("Content-Type", "application/json").build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        cb.onError("[EMOTION:concerned] Language lesson unavailable, sir."); return;
                    }
                    JSONObject j = new JSONObject(resp.body().string());
                    String reply = j.optString("reply", "");
                    reply = reply.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                    cb.onResult("[EMOTION:excited] 🌍 **" + finalLang + " Lesson — " +
                        capitalize(level) + " | Streak: " + finalStreak + " day(s)**\n\n" + reply);
                }
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Language error: " + e.getMessage());
            }
        }).start();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
