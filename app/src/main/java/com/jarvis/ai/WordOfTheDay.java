package com.jarvis.ai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Word of the Day — fetches from Free Dictionary API (free, no key).
 * Shows at app start once per day, or on demand.
 * "Word of the day" / "Give me a new word" / "vocabulary"
 */
public class WordOfTheDay {

    private static final String PREFS      = "wotd_prefs";
    private static final String KEY_DATE   = "wotd_date";
    private static final String KEY_WORD   = "wotd_word";
    private static final String KEY_DEF    = "wotd_def";
    private static final String KEY_SHOWN  = "wotd_shown";

    // Curated daily words (fallback + cycle)
    private static final String[][] FALLBACK_WORDS = {
        {"Ephemeral",    "Lasting for a very short time.", "The beauty of cherry blossoms is ephemeral — they bloom for just a week."},
        {"Perspicacious","Having a ready insight; shrewd.", "A perspicacious analyst spotted the flaw before anyone else."},
        {"Sanguine",     "Optimistic, especially in difficult situations.", "Despite the setbacks, she remained sanguine about the project's success."},
        {"Equanimity",   "Mental calmness, especially in difficult situations.", "He faced the crisis with remarkable equanimity."},
        {"Recalcitrant", "Having an obstinately uncooperative attitude.", "The recalcitrant employee refused to follow the new policy."},
        {"Mellifluous",  "Sweet or musical; pleasant to hear.", "Her mellifluous voice filled the concert hall."},
        {"Tenacious",    "Tending to keep a firm hold; persistent.", "His tenacious work ethic made him unstoppable."},
        {"Pernicious",   "Having a harmful effect, especially gradually.", "The pernicious habit of procrastination held him back."},
        {"Eloquent",     "Fluent and persuasive in speaking or writing.", "Her eloquent speech moved the entire audience to tears."},
        {"Indefatigable","Persisting tirelessly.", "An indefatigable explorer, she never gave up on her mission."},
        {"Labyrinthine", "Intricate and complex like a labyrinth.", "The tax code is labyrinthine — even accountants struggle with it."},
        {"Serendipity",  "The occurrence of events by happy chance.", "Their meeting was pure serendipity — they were both lost in the same alley."},
        {"Ubiquitous",   "Present everywhere or seeming to be everywhere.", "Smartphones have become ubiquitous in modern life."},
        {"Sagacious",    "Having good judgement; wise.", "The sagacious mentor guided his students to success."},
        {"Laconic",      "Using very few words.", "His laconic reply — 'No' — ended the debate immediately."},
    };

    public static boolean isWordCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("word of the day") || lower.contains("new word") ||
               lower.contains("vocabulary") || lower.contains("teach me a word") ||
               lower.contains("word for today") || lower.contains("wotd") ||
               lower.contains("vocab word") || lower.contains("learn a word");
    }

    public static boolean shouldShowToday(Context ctx) {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATE, "");
        boolean shown = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SHOWN, false);
        return !today.equals(saved) || !shown;
    }

    public static void fetch(Context ctx, Callback cb) {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        // Check if we already fetched today
        String savedDate = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATE, "");
        if (today.equals(savedDate)) {
            String word = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_WORD, null);
            String def  = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DEF, null);
            if (word != null) { cb.onResult(buildResponse(word, def, null)); return; }
        }

        // Cycle through fallback words by day of year
        int dayOfYear = new Date().hashCode() % FALLBACK_WORDS.length;
        if (dayOfYear < 0) dayOfYear = -dayOfYear;
        final String[] fallback = FALLBACK_WORDS[dayOfYear % FALLBACK_WORDS.length];

        new Thread(() -> {
            try {
                // Try Free Dictionary API
                String urlStr = "https://api.dictionaryapi.dev/api/v2/entries/en/" +
                    java.net.URLEncoder.encode(fallback[0], "UTF-8");
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000); conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "HENRY-AI/1.0");
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    StringBuilder sb = new StringBuilder();
                    byte[] buf = new byte[16384]; int r;
                    while ((r = is.read(buf)) != -1) sb.append(new String(buf, 0, r, "UTF-8"));
                    is.close();

                    JSONArray arr  = new JSONArray(sb.toString());
                    JSONObject obj = arr.getJSONObject(0);
                    String word    = obj.optString("word", fallback[0]);
                    String phonetic = obj.optString("phonetic", "");
                    JSONArray meanings = obj.optJSONArray("meanings");
                    String def = null, partOfSpeech = null;
                    if (meanings != null && meanings.length() > 0) {
                        JSONObject meaning = meanings.getJSONObject(0);
                        partOfSpeech = meaning.optString("partOfSpeech", "");
                        JSONArray defs = meaning.optJSONArray("definitions");
                        if (defs != null && defs.length() > 0)
                            def = defs.getJSONObject(0).optString("definition", null);
                    }
                    String example = fallback[2];
                    String fullDef = (partOfSpeech.isEmpty() ? "" : "_" + partOfSpeech + "_ — ") +
                        (def != null ? def : fallback[1]);
                    saveToday(ctx, word, fullDef, today);
                    cb.onResult(buildResponse(word, fullDef, example));
                    return;
                }
            } catch (Exception ignored) {}

            // Fallback
            saveToday(ctx, fallback[0], fallback[1], today);
            cb.onResult(buildResponse(fallback[0], fallback[1], fallback[2]));
        }).start();
    }

    private static String buildResponse(String word, String def, String example) {
        StringBuilder sb = new StringBuilder("[EMOTION:excited] 📖 **Word of the Day, sir:**\n\n");
        sb.append("**").append(word).append("**\n");
        sb.append(def).append("\n");
        if (example != null && !example.isEmpty())
            sb.append("\n_Example:_ \"").append(example).append("\"");
        sb.append("\n\nUse it in a sentence today, sir. Vocabulary is power.");
        return sb.toString();
    }

    private static void saveToday(Context ctx, String word, String def, String date) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putString(KEY_DATE, date).putString(KEY_WORD, word)
           .putString(KEY_DEF, def).putBoolean(KEY_SHOWN, true).apply();
    }

    public interface Callback {
        void onResult(String formatted);
    }
}
