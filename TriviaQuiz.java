package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Trivia Quiz — pulls questions from Open Trivia DB (free, no key).
 * "Quiz me", "trivia question", "ask me something", "next question"
 */
public class TriviaQuiz {

    public interface Callback {
        void onQuestion(String question, List<String> options, String correct);
        void onError(String reason);
    }

    public interface AnswerCallback {
        void onResult(boolean correct, String correctAnswer, String explanation);
    }

    // Active quiz state (singleton)
    private static String activeQuestion  = null;
    private static String activeCorrect   = null;
    private static List<String> activeOpts = null;
    private static int score = 0;
    private static int total = 0;

    public static boolean isQuizCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("quiz") || lower.contains("trivia") ||
               lower.contains("ask me something") || lower.contains("test my knowledge") ||
               lower.contains("next question") || lower.contains("another question") ||
               lower.contains("quiz me") || lower.startsWith("question ");
    }

    public static boolean isAnswerCommand(String text) {
        return activeQuestion != null && !isQuizCommand(text);
    }

    public static boolean hasActiveQuestion() { return activeQuestion != null; }

    public static void fetchQuestion(String category, Callback cb) {
        new Thread(() -> {
            try {
                // Category mapping
                int catId = detectCategory(category);
                String urlStr = "https://opentdb.com/api.php?amount=1&type=multiple&difficulty=medium" +
                    (catId > 0 ? "&category=" + catId : "");

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000); conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "HENRY-AI/1.0");
                conn.connect();

                StringBuilder sb = new StringBuilder();
                InputStream is = conn.getInputStream();
                byte[] buf = new byte[16384]; int read;
                while ((read = is.read(buf)) != -1) sb.append(new String(buf, 0, read, "UTF-8"));
                is.close();

                JSONObject json    = new JSONObject(sb.toString());
                JSONArray results  = json.optJSONArray("results");
                if (results == null || results.length() == 0) {
                    cb.onError("[EMOTION:neutral] No trivia available right now, sir."); return;
                }

                JSONObject q    = results.getJSONObject(0);
                String question = htmlDecode(q.getString("question"));
                String correct  = htmlDecode(q.getString("correct_answer"));
                JSONArray wrong = q.getJSONArray("incorrect_answers");

                List<String> options = new ArrayList<>();
                options.add(correct);
                for (int i = 0; i < wrong.length(); i++) options.add(htmlDecode(wrong.getString(i)));
                Collections.shuffle(options);

                activeQuestion = question;
                activeCorrect  = correct;
                activeOpts     = options;
                total++;

                cb.onQuestion(question, options, correct);
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Couldn't fetch trivia: " + e.getMessage());
            }
        }).start();
    }

    public static String checkAnswer(String userAnswer) {
        if (activeQuestion == null || activeCorrect == null)
            return "[EMOTION:neutral] No active question, sir. Say 'quiz me' to start.";

        boolean correct = userAnswer.toLowerCase(Locale.US).contains(activeCorrect.toLowerCase(Locale.US));
        if (correct) score++;

        String resp;
        if (correct) {
            resp = String.format(Locale.US,
                "[EMOTION:proud] ✅ **Correct!** Well done, sir.\n" +
                "Score: **%d / %d** (%.0f%%)", score, total, (double)score/total*100);
        } else {
            resp = String.format(Locale.US,
                "[EMOTION:amused] ❌ Not quite, sir. The answer was: **%s**\n" +
                "Score: **%d / %d** (%.0f%%)", activeCorrect, score, total, (double)score/total*100);
        }

        activeQuestion = null;
        activeCorrect  = null;
        activeOpts     = null;
        return resp + "\n\nSay **next question** for another, sir.";
    }

    public static String formatQuestion(String question, List<String> options) {
        StringBuilder sb = new StringBuilder("[EMOTION:excited] 🧠 **Trivia Time, sir!**\n\n");
        sb.append("**Q:** ").append(question).append("\n\n");
        String[] letters = {"A", "B", "C", "D"};
        for (int i = 0; i < options.size() && i < 4; i++) {
            sb.append("**").append(letters[i]).append(":** ").append(options.get(i)).append("\n");
        }
        sb.append("\nReply with the letter or the answer, sir.");
        return sb.toString();
    }

    public static void resetScore() { score = 0; total = 0; }
    public static String getScore() {
        return total == 0 ? "No questions answered yet, sir."
            : String.format(Locale.US, "Score: **%d / %d** (%.0f%%), sir.", score, total, (double)score/total*100);
    }

    private static int detectCategory(String text) {
        if (text == null) return 0;
        String lower = text.toLowerCase(Locale.US);
        if (lower.contains("science"))     return 17;
        if (lower.contains("history"))     return 23;
        if (lower.contains("geography"))   return 22;
        if (lower.contains("sport"))       return 21;
        if (lower.contains("music"))       return 12;
        if (lower.contains("film") || lower.contains("movie")) return 11;
        if (lower.contains("computer"))    return 18;
        if (lower.contains("math"))        return 19;
        if (lower.contains("art"))         return 25;
        if (lower.contains("celebrity"))   return 26;
        if (lower.contains("animal"))      return 27;
        if (lower.contains("vehicle"))     return 28;
        return 0; // random
    }

    private static String htmlDecode(String s) {
        return s.replace("&quot;", "\"").replace("&#039;", "'")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
    }
}
