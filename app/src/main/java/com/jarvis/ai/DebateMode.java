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
 * AI Debate Mode — presents PRO and CON arguments for any topic.
 * "Argue both sides of AI ethics"
 * "Debate: should social media be banned?"
 * "Pros and cons of remote work"
 * "Devil's advocate on [topic]"
 */
public class DebateMode {

    public interface Callback {
        void onResult(String debate);
        void onError(String reason);
    }

    public static boolean isDebateCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("argue both sides") || lower.contains("debate") ||
               lower.contains("pros and cons") || lower.contains("pro and con") ||
               lower.contains("devil's advocate") || lower.contains("devils advocate") ||
               lower.contains("both sides of") || lower.contains("for and against") ||
               lower.contains("advantages and disadvantages") || lower.contains("two sides");
    }

    public static String parseTopic(String text) {
        return text
            .replaceAll("(?i)^(argue both sides of|debate:?|pros and cons of|pro and con of|" +
                        "devil'?s advocate on|both sides of|for and against|" +
                        "advantages and disadvantages of|two sides of)\\s*", "")
            .replaceAll("(?i)\\?$", "").trim();
    }

    public static void debate(String topic, OkHttpClient httpClient,
                              UserProfile profile, Callback cb) {
        new Thread(() -> {
            try {
                String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "sir";
                String systemPrompt =
                    "You are H.E.N.R.Y, a sharp debate coach and critical thinker. " +
                    name + " wants to understand both sides of a topic. " +
                    "Structure your response EXACTLY as:\n" +
                    "**⚡ PRO (Arguments FOR)**\n" +
                    "• [point 1]\n• [point 2]\n• [point 3]\n\n" +
                    "**🔴 CON (Arguments AGAINST)**\n" +
                    "• [point 1]\n• [point 2]\n• [point 3]\n\n" +
                    "**◆ HENRY's Take:** [one sharp sentence on which side has the stronger argument and why]\n\n" +
                    "Be precise, insightful, intellectually honest. No fluff.";

                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", "Argue both sides of: " + topic);
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
                        cb.onError("[EMOTION:concerned] Debate mode unavailable right now, sir."); return;
                    }
                    JSONObject j = new JSONObject(resp.body().string());
                    String reply = j.optString("reply", "");
                    reply = reply.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                    cb.onResult("[EMOTION:serious] **Debate: " + topic + "**\n\n" + reply);
                }
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Debate error: " + e.getMessage());
            }
        }).start();
    }
}
