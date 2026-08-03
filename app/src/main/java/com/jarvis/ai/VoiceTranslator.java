package com.jarvis.ai;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Real-time Voice Translator using MyMemory API (free, no key, 5000 chars/day).
 * "translate [text] to [language]" or "how do you say [text] in [language]"
 */
public class VoiceTranslator {

    public interface Callback {
        void onResult(String translation, String targetLang, String langCode);
        void onError(String reason);
    }

    // Language map: spoken name → MyMemory lang code
    private static final Map<String, String[]> LANG_MAP = new HashMap<String, String[]>() {{
        put("arabic",     new String[]{"ar", "Arabic"});
        put("chinese",    new String[]{"zh", "Chinese"});
        put("mandarin",   new String[]{"zh", "Chinese"});
        put("french",     new String[]{"fr", "French"});
        put("german",     new String[]{"de", "German"});
        put("hindi",      new String[]{"hi", "Hindi"});
        put("italian",    new String[]{"it", "Italian"});
        put("japanese",   new String[]{"ja", "Japanese"});
        put("korean",     new String[]{"ko", "Korean"});
        put("malay",      new String[]{"ms", "Malay"});
        put("portuguese", new String[]{"pt", "Portuguese"});
        put("russian",    new String[]{"ru", "Russian"});
        put("spanish",    new String[]{"es", "Spanish"});
        put("tagalog",    new String[]{"tl", "Tagalog"});
        put("filipino",   new String[]{"tl", "Filipino"});
        put("thai",       new String[]{"th", "Thai"});
        put("turkish",    new String[]{"tr", "Turkish"});
        put("ukrainian",  new String[]{"uk", "Ukrainian"});
        put("urdu",       new String[]{"ur", "Urdu"});
        put("vietnamese", new String[]{"vi", "Vietnamese"});
        put("indonesian", new String[]{"id", "Indonesian"});
        put("dutch",      new String[]{"nl", "Dutch"});
        put("greek",      new String[]{"el", "Greek"});
        put("hebrew",     new String[]{"he", "Hebrew"});
        put("polish",     new String[]{"pl", "Polish"});
        put("swedish",    new String[]{"sv", "Swedish"});
        put("danish",     new String[]{"da", "Danish"});
        put("finnish",    new String[]{"fi", "Finnish"});
        put("norwegian",  new String[]{"no", "Norwegian"});
        put("romanian",   new String[]{"ro", "Romanian"});
        put("czech",      new String[]{"cs", "Czech"});
        put("hungarian",  new String[]{"hu", "Hungarian"});
        put("english",    new String[]{"en", "English"});
    }};

    public static boolean isTranslateCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("translate") ||
               lower.contains("how do you say") ||
               lower.contains("how to say") ||
               lower.contains("say it in") ||
               lower.contains("in arabic") || lower.contains("in french") ||
               lower.contains("in spanish") || lower.contains("in tagalog") ||
               lower.contains("in filipino") || lower.contains("in japanese") ||
               lower.contains("in chinese") || lower.contains("in korean") ||
               lower.contains("in german") || lower.contains("in italian") ||
               lower.contains("in hindi") || lower.contains("in russian") ||
               lower.contains("in portuguese") || lower.contains("in dutch") ||
               (lower.contains("translate") && lower.contains(" to "));
    }

    /** Parses text + target lang from query. Returns {textToTranslate, langCode, langName} or null. */
    public static String[] parse(String text) {
        String lower = text.toLowerCase(Locale.US);

        // Detect target language
        String[] detected = detectLang(lower);
        if (detected == null) return null;
        String langCode = detected[0], langName = detected[1];

        // Extract text to translate
        String toTranslate = text
            .replaceAll("(?i)translate\\s+", "")
            .replaceAll("(?i)how\\s+(do\\s+you|to)\\s+say\\s+", "")
            .replaceAll("(?i)say\\s+it\\s+in\\s+\\w+", "")
            .replaceAll("(?i)in\\s+" + langName + "\\s*$", "")
            .replaceAll("(?i)to\\s+" + langName + "\\s*$", "")
            .replaceAll("(?i)in\\s+" + detected[0] + "\\s*$", "")
            .replaceAll("(?i)(in|to)\\s+(arabic|french|spanish|tagalog|filipino|japanese|chinese|korean|german|italian|hindi|russian|portuguese|dutch|malay|turkish|vietnamese|indonesian|greek|hebrew|polish|swedish|danish|finnish|norwegian|romanian|czech|hungarian|ukrainian|urdu|thai|english|mandarin)\\s*$", "")
            .trim();

        if (toTranslate.isEmpty()) return null;
        return new String[]{toTranslate, langCode, langName};
    }

    private static String[] detectLang(String lower) {
        for (Map.Entry<String, String[]> e : LANG_MAP.entrySet())
            if (lower.contains(e.getKey())) return e.getValue();
        return null;
    }

    public static void translate(String text, String targetLangCode, String targetLangName, Callback cb) {
        new Thread(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(text, "UTF-8");
                String urlStr  = "https://api.mymemory.translated.net/get?q=" + encoded +
                                 "&langpair=en|" + targetLangCode;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000); conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "HENRY-AI/1.0");
                conn.connect();

                InputStream is = conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[8192]; int read;
                while ((read = is.read(buf)) != -1) sb.append(new String(buf, 0, read, "UTF-8"));
                is.close();

                JSONObject json   = new JSONObject(sb.toString());
                JSONObject match  = json.optJSONObject("responseData");
                if (match == null) { cb.onError("[EMOTION:concerned] Translation failed, sir."); return; }
                String translated = match.optString("translatedText", "");
                if (translated.isEmpty() || translated.equalsIgnoreCase("MYMEMORY WARNING")) {
                    cb.onError("[EMOTION:concerned] Translation unavailable right now, sir.");
                    return;
                }
                cb.onResult(translated, targetLangName, targetLangCode);
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Translation error: " + e.getMessage());
            }
        }).start();
    }
}
