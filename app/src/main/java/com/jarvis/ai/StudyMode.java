package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI Study Mode — Socratic tutor, flashcards, topic explainer.
 * "Explain quantum physics like I'm 5"
 * "Teach me about the French Revolution"
 * "Give me a flashcard on photosynthesis"
 * "Quiz me on algebra"
 */
public class StudyMode {

    public interface Callback {
        void onResult(String response);
        void onError(String reason);
    }

    public static boolean isStudyCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("explain") || lower.startsWith("teach me") ||
               lower.startsWith("what is") && lower.contains("explain") ||
               lower.contains("like i'm 5") || lower.contains("like im 5") ||
               lower.contains("eli5") || lower.contains("flashcard") ||
               lower.contains("study") && (lower.contains("mode") || lower.contains("help")) ||
               lower.contains("summarise this topic") || lower.contains("summarize this topic") ||
               lower.contains("help me understand") || lower.contains("break it down") ||
               lower.contains("teach me about") || lower.contains("lesson on") ||
               lower.contains("explain to me") || lower.contains("what does") && lower.contains("mean") ||
               lower.startsWith("define ") || lower.contains("definition of");
    }

    public static void ask(String userQuery, String mode, OkHttpClient httpClient,
                           UserProfile profile, Callback cb) {
        new Thread(() -> {
            try {
                String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "sir";

                String systemPrompt;
                if (userQuery.toLowerCase().contains("flashcard")) {
                    systemPrompt = "You are H.E.N.R.Y, a brilliant tutor. Create a flashcard for " + name + ". " +
                        "Format: **FRONT:** [question or term] | **BACK:** [answer/definition]. " +
                        "Then add: **Remember it:** [one clever memory trick]. Keep it sharp.";
                } else if (userQuery.toLowerCase().contains("like i'm 5") ||
                           userQuery.toLowerCase().contains("eli5") ||
                           userQuery.toLowerCase().contains("like im 5")) {
                    systemPrompt = "You are H.E.N.R.Y. Explain the concept to " + name + " as if they are 5 years old. " +
                        "Use simple words, fun analogies, and a real-world example. Max 4 sentences.";
                } else if (userQuery.toLowerCase().contains("quiz")) {
                    systemPrompt = "You are H.E.N.R.Y, a sharp tutor. Create a quick 3-question quiz for " + name + " " +
                        "on the topic they mention. Show questions numbered, then reveal answers below marked ANSWERS:.";
                } else {
                    systemPrompt = "You are H.E.N.R.Y, an expert tutor who makes complex topics fascinating. " +
                        "Teach " + name + " about the requested topic. Structure: " +
                        "1) One-sentence summary, 2) Key concepts (bullet points), " +
                        "3) Real-world example, 4) One surprising fact. " +
                        "Be concise, insightful, and slightly witty.";
                }

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
                        cb.onError("[EMOTION:concerned] Study mode unavailable right now, sir.");
                        return;
                    }
                    JSONObject j = new JSONObject(resp.body().string());
                    String reply = j.optString("reply", "");
                    reply = reply.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                    cb.onResult("[EMOTION:excited] " + reply);
                }
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Study error: " + e.getMessage());
            }
        }).start();
    }
}
