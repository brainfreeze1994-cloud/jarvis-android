package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Code Runner — executes code via Piston API (free, no key, 30+ languages).
 * "Run this code: print('hello')"
 * "Execute: for i in range(5): print(i)"
 * "Run python: [code]"
 * Works with code HENRY writes + user pastes.
 */
public class CodeRunner {

    private static final String PISTON_URL = "https://emkc.org/api/v2/piston/execute";

    public interface Callback {
        void onResult(String output, String language, String code);
        void onError(String reason);
    }

    // Language aliases
    private static final String[][] LANG_MAP = {
        {"python",      "python",     "3.10"},
        {"py",          "python",     "3.10"},
        {"javascript",  "javascript", "18.15"},
        {"js",          "javascript", "18.15"},
        {"node",        "javascript", "18.15"},
        {"java",        "java",       "15"},
        {"c++",         "c++",        "10"},
        {"cpp",         "c++",        "10"},
        {"c",           "c",          "10"},
        {"ruby",        "ruby",       "3.0"},
        {"go",          "go",         "1.16"},
        {"golang",      "go",         "1.16"},
        {"rust",        "rust",       "1.50"},
        {"php",         "php",        "8.0"},
        {"bash",        "bash",       "5.0"},
        {"shell",       "bash",       "5.0"},
        {"swift",       "swift",      "5.3"},
        {"kotlin",      "kotlin",     "1.5"},
        {"typescript",  "typescript", "5.0"},
        {"ts",          "typescript", "5.0"},
        {"r",           "r",          "4.1"},
        {"lua",         "lua",        "5.4"},
        {"perl",        "perl",       "5.36"},
    };

    public static boolean isRunCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("run ") || lower.startsWith("execute ") ||
               lower.startsWith("run this") || lower.startsWith("execute this") ||
               lower.contains("run the code") || lower.contains("execute the code") ||
               lower.contains("run python") || lower.contains("run javascript") ||
               lower.contains("run java") || lower.contains("run this code") ||
               lower.contains("compile and run") || lower.contains("test this code");
    }

    /** Returns {language, version, code} or null */
    public static String[] parse(String text) {
        String lower = text.toLowerCase(Locale.US);

        // Detect language
        String lang = "python", version = "3.10";
        for (String[] row : LANG_MAP) {
            if (lower.contains(row[0])) { lang = row[1]; version = row[2]; break; }
        }

        // Extract code
        String code = text;

        // Remove code block markdown ```lang ... ```
        java.util.regex.Matcher mdBlock = java.util.regex.Pattern.compile(
            "```(?:\\w+)?\\s*([\\s\\S]+?)```").matcher(code);
        if (mdBlock.find()) {
            code = mdBlock.group(1).trim();
        } else {
            // Strip command prefix
            code = code
                .replaceAll("(?i)^(run|execute|compile and run|test)\\s*(this\\s*)?(code\\s*)?", "")
                .replaceAll("(?i)^(python|javascript|java|c\\+\\+|cpp|ruby|go|golang|rust|php|bash|swift|kotlin|typescript|r|lua|perl|js|ts|node)\\s*:?\\s*", "")
                .trim();
        }

        if (code.isEmpty()) return null;
        return new String[]{lang, version, code};
    }

    public static void run(String language, String version, String code, Callback cb) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("language", language);
                body.put("version", version);

                JSONArray files = new JSONArray();
                JSONObject file = new JSONObject();
                file.put("name", "main." + getExt(language));
                file.put("content", code);
                files.put(file);
                body.put("files", files);
                body.put("stdin", "");
                body.put("args", new JSONArray());
                body.put("compile_timeout", 10000);
                body.put("run_timeout", 5000);
                body.put("compile_memory_limit", -1);
                body.put("run_memory_limit", -1);

                URL url = new URL(PISTON_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "HENRY-AI/1.0");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                InputStream is = conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[16384]; int r;
                while ((r = is.read(buf)) != -1) sb.append(new String(buf, 0, r, "UTF-8"));
                is.close();

                JSONObject result = new JSONObject(sb.toString());
                JSONObject run    = result.optJSONObject("run");
                JSONObject compile = result.optJSONObject("compile");

                String output = "";
                if (compile != null && !compile.optString("stderr", "").isEmpty()) {
                    output = "Compile Error:\n" + compile.optString("stderr");
                } else if (run != null) {
                    String stdout = run.optString("stdout", "").trim();
                    String stderr = run.optString("stderr", "").trim();
                    if (!stdout.isEmpty()) output = stdout;
                    if (!stderr.isEmpty()) output += (output.isEmpty() ? "" : "\n") + "Error: " + stderr;
                    if (output.isEmpty()) output = "(no output)";
                } else {
                    output = "No output received.";
                }

                // Truncate if too long
                if (output.length() > 1500) output = output.substring(0, 1497) + "…";
                cb.onResult(output, language, code);
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Execution failed: " + e.getMessage() + ", sir.");
            }
        }).start();
    }

    private static String getExt(String lang) {
        switch (lang) {
            case "python":     return "py";
            case "javascript": return "js";
            case "typescript": return "ts";
            case "java":       return "java";
            case "c++":        return "cpp";
            case "c":          return "c";
            case "ruby":       return "rb";
            case "go":         return "go";
            case "rust":       return "rs";
            case "php":        return "php";
            case "bash":       return "sh";
            case "swift":      return "swift";
            case "kotlin":     return "kt";
            case "r":          return "r";
            case "lua":        return "lua";
            case "perl":       return "pl";
            default:           return "txt";
        }
    }
}
