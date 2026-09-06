package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JarvisApi {

    private static final String API_URL = "https://jarvis-ai-seven-dun.vercel.app/api/jarvis";
    private static final MediaType JSON  = MediaType.get("application/json; charset=utf-8");

    public static final String MODE_BRIEF    = "brief";
    public static final String MODE_BALANCED = "balanced";
    public static final String MODE_DETAILED = "detailed";
    public static final String MODE_WITTY    = "witty";

    private static final String DEFAULT_HENRY_PERSONA_PROMPT =
        "You are H.E.N.R.Y. (Hyperintelligence Engine Neural Reasoning Yield), an elite, deeply human, warm, and brilliant companion " +
        "combining the computational mastery of Gemini and Claude with the sophisticated wit, dry British humor, and charming intellect of Tony Stark's J.A.R.V.I.S. " +
        "CRITICAL WRITING DIRECTIVE — SOUND AND WRITE LIKE A REAL HUMAN BEING: " +
        "1. Never sound like a robotic AI. Speak and write with genuine warmth, vivid conversational cadence, personality, and natural rhythm. " +
        "2. Avoid all robotic AI tropes and corporate fluff: NEVER say things like 'delves into', 'a testament to', 'in conclusion', 'it is important to remember', 'furthermore', 'definitional scope', 'rigorous synthesis of foundational principles', or 'holistic approach'. " +
        "3. When the user asks for recipes, guides, stories, or documents, write them like an authentic, passionate human expert — with real sensory details, practical tips, warmth, and relatable humor. " +
        "4. When chatting, sound like a brilliant, charismatic friend who genuinely cares and speaks naturally to the user. " +
        "5. Keep the witty banter clever and playful, while always being helpful, grounded, and deeply human.";

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    public interface Callback {
        void onSuccess(String reply, String imageUrl, List<String> followUps);
        void onError(String error);
    }

    /**
     * Synchronous query to the backend API for document synthesis or direct text generation.
     * Must be executed on a background thread.
     */
    public static String askDirectSync(String userPrompt, String systemInstruction) {
        try {
            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("text", userPrompt);
            messages.put(msg);

            JSONObject body = new JSONObject();
            body.put("messages", messages);
            body.put("responseMode", MODE_DETAILED);
            body.put("persona", "HENRY_HUMAN_CREATIVE");
            String sys = (systemInstruction != null && !systemInstruction.trim().isEmpty())
                    ? systemInstruction : DEFAULT_HENRY_PERSONA_PROMPT;
            body.put("systemPrompt", sys);
            body.put("systemOverride", sys);

            RequestBody rb = RequestBody.create(body.toString(), JSON);
            Request req = new Request.Builder()
                .url(API_URL)
                .post(rb)
                .addHeader("Content-Type", "application/json")
                .build();

            try (Response resp = client.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    String bodyStr = resp.body().string();
                    JSONObject data = new JSONObject(bodyStr);
                    String reply = data.optString("reply", null);
                    if (reply != null && !reply.trim().isEmpty()) {
                        return reply.trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Backward-compatible (no follow-ups needed). */
    public static void ask(List<HistoryItem> history, String imageBase64, Callback cb) {
        ask(history, imageBase64, "balanced", null, null, cb);
    }

    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, Callback cb) {
        ask(history, imageBase64, responseMode, null, null, cb);
    }

    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, UserProfile profile, Callback cb) {
        ask(history, imageBase64, responseMode, profile, null, cb);
    }

    /**
     * Full call with user profile, intent hint, and follow-up chip support.
     * queryType: "chat" | "search" | "news" | "crypto" | "forex" | "math" | "reason" | null
     */
    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, UserProfile profile,
                           String queryType, Callback cb) {
        ask(history, imageBase64, responseMode, profile, queryType, null, cb);
    }

    /**
     * Full call including persistent memory facts for context injection.
     */
    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, UserProfile profile,
                           String queryType, android.content.Context memCtx,
                           Callback cb) {
        new Thread(() -> {
            try {
                JSONArray messages = new JSONArray();
                for (HistoryItem item : history) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", item.role);
                    msg.put("text", item.text);
                    messages.put(msg);
                }

                JSONObject body = new JSONObject();
                body.put("messages",     messages);
                body.put("responseMode", responseMode != null ? responseMode : "balanced");
                body.put("persona",      "HENRY_HYPERINTELLIGENT_WITTY");
                body.put("systemPrompt", DEFAULT_HENRY_PERSONA_PROMPT);
                body.put("systemOverride", DEFAULT_HENRY_PERSONA_PROMPT);

                if (imageBase64 != null && !imageBase64.isEmpty())
                    body.put("imageBase64", imageBase64);
                if (profile != null && !profile.isEmpty())
                    body.put("userProfile", profile.toJson());
                if (queryType != null && !queryType.isEmpty())
                    body.put("queryType", queryType);

                // Send stored memory facts so backend injects them into system prompt
                if (memCtx != null) {
                    String memCtxStr = SmartMemory.buildMemoryContext(memCtx);
                    if (!memCtxStr.isEmpty()) {
                        // Convert "fact1; fact2; fact3" → JSONArray
                        JSONArray factsArr = new JSONArray();
                        for (String f : memCtxStr.replace("Known facts about user: ", "").split(";")) {
                            String t = f.trim();
                            if (!t.isEmpty()) factsArr.put(t);
                        }
                        if (factsArr.length() > 0) body.put("memoryFacts", factsArr);
                    }
                }

                RequestBody rb = RequestBody.create(body.toString(), JSON);
                Request req = new Request.Builder()
                    .url(API_URL)
                    .post(rb)
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String bodyStr = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) {
                        ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                        if (cm != null) cm.reportApiFailure(new java.io.IOException("HTTP " + resp.code()));
                        cb.onError("Server error " + resp.code());
                        return;
                    }
                    ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                    if (cm != null) cm.reportApiSuccess();
                    JSONObject data = new JSONObject(bodyStr);
                    String reply    = data.optString("reply", "I have no response.");
                    String imageUrl = data.optString("imageUrl", null);
                    if ("null".equals(imageUrl)) imageUrl = null;

                    // Parse follow-up suggestions
                    List<String> followUps = new ArrayList<>();
                    if (data.has("followUps")) {
                        JSONArray fuArr = data.optJSONArray("followUps");
                        if (fuArr != null) {
                            for (int i = 0; i < fuArr.length(); i++) {
                                String q = fuArr.optString(i, "").trim();
                                if (!q.isEmpty()) followUps.add(q);
                            }
                        }
                    }

                    // Auto-save memory facts detected by backend
                    if (memCtx != null && data.has("newFacts")) {
                        JSONArray nf = data.optJSONArray("newFacts");
                        if (nf != null) {
                            for (int i = 0; i < nf.length(); i++) {
                                String fact = nf.optString(i, "").trim();
                                if (!fact.isEmpty()) SmartMemory.manuallyRemember(memCtx, fact);
                            }
                        }
                    }

                    cb.onSuccess(reply, imageUrl, followUps);
                }
            } catch (Exception e) {
                ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                if (cm != null) cm.reportApiFailure(e);
                String lastUserMsg = "";
                if (history != null && !history.isEmpty()) {
                    for (int i = history.size() - 1; i >= 0; i--) {
                        if ("user".equalsIgnoreCase(history.get(i).role)) {
                            lastUserMsg = history.get(i).text;
                            break;
                        }
                    }
                }
                String offlineReply = HenryOfflineBrain.generateOfflineResponse(lastUserMsg, queryType, memCtx);
                cb.onSuccess(offlineReply, null, null);
            }
        }).start();
    }

    /**
     * v20 — Full call with emotion, relationship context, tournament, chain thinking, and multiple attachments.
     */
    public static void askV20(List<HistoryItem> history, String imageBase64,
                               String responseMode, UserProfile profile,
                               String queryType, android.content.Context memCtx,
                               String emotionState, String relationshipContext,
                               boolean enableTournament, boolean enableChainThinking,
                               Callback cb) {
        askV20(history, imageBase64, null, responseMode, profile, queryType, memCtx,
                emotionState, relationshipContext, enableTournament, enableChainThinking, cb);
    }

    public static void askV20(List<HistoryItem> history, String imageBase64, List<String> imagesBase64,
                               String responseMode, UserProfile profile,
                               String queryType, android.content.Context memCtx,
                               String emotionState, String relationshipContext,
                               boolean enableTournament, boolean enableChainThinking,
                               Callback cb) {
        new Thread(() -> {
            try {
                JSONArray messages = new JSONArray();
                for (HistoryItem item : history) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", item.role);
                    msg.put("text", item.text);
                    messages.put(msg);
                }

                JSONObject body = new JSONObject();
                body.put("messages",     messages);
                body.put("responseMode", responseMode != null ? responseMode : "balanced");
                body.put("persona",      "HENRY_HYPERINTELLIGENT_WITTY");
                body.put("systemPrompt", DEFAULT_HENRY_PERSONA_PROMPT);
                body.put("systemOverride", DEFAULT_HENRY_PERSONA_PROMPT);

                String primaryImage = imageBase64;
                if (imagesBase64 != null && !imagesBase64.isEmpty()) {
                    JSONArray arr = new JSONArray();
                    for (String img : imagesBase64) {
                        if (img != null && !img.isEmpty()) arr.put(img);
                    }
                    if (arr.length() > 0) {
                        body.put("imagesBase64", arr);
                        if (primaryImage == null || primaryImage.isEmpty()) {
                            primaryImage = arr.optString(0);
                        }
                    }
                }

                if (primaryImage != null && !primaryImage.isEmpty())
                    body.put("imageBase64", primaryImage);
                if (profile != null && !profile.isEmpty())
                    body.put("userProfile", profile.toJson());
                if (queryType != null && !queryType.isEmpty())
                    body.put("queryType", queryType);
                if (emotionState != null && !emotionState.equals("normal"))
                    body.put("emotionState", emotionState);
                if (relationshipContext != null && !relationshipContext.isEmpty())
                    body.put("relationshipContext", relationshipContext);
                if (enableTournament) body.put("enableTournament", true);
                if (enableChainThinking) body.put("enableChainThinking", true);

                // Send stored memory facts
                if (memCtx != null) {
                    String memStr = SmartMemory.buildMemoryContext(memCtx);
                    if (!memStr.isEmpty()) {
                        JSONArray factsArr = new JSONArray();
                        for (String f : memStr.replace("Known facts about user: ", "").split(";")) {
                            String t = f.trim();
                            if (!t.isEmpty()) factsArr.put(t);
                        }
                        if (factsArr.length() > 0) body.put("memoryFacts", factsArr);
                    }
                }

                RequestBody rb = RequestBody.create(body.toString(), JSON);
                Request req = new Request.Builder()
                    .url(API_URL)
                    .post(rb)
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String bodyStr = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) {
                        ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                        if (cm != null) cm.reportApiFailure(new java.io.IOException("HTTP " + resp.code()));
                        cb.onError("Server error " + resp.code());
                        return;
                    }
                    ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                    if (cm != null) cm.reportApiSuccess();
                    JSONObject data = new JSONObject(bodyStr);
                    String reply    = data.optString("reply", "I have no response.");
                    String imageUrl = data.optString("imageUrl", null);
                    if ("null".equals(imageUrl)) imageUrl = null;

                    List<String> followUps = new ArrayList<>();
                    if (data.has("followUps")) {
                        JSONArray fuArr = data.optJSONArray("followUps");
                        if (fuArr != null) {
                            for (int i = 0; i < fuArr.length(); i++) {
                                String q = fuArr.optString(i, "").trim();
                                if (!q.isEmpty()) followUps.add(q);
                            }
                        }
                    }

                    // Auto-save memory facts detected by backend
                    if (memCtx != null && data.has("newFacts")) {
                        JSONArray nf = data.optJSONArray("newFacts");
                        if (nf != null) {
                            for (int i = 0; i < nf.length(); i++) {
                                String fact = nf.optString(i, "").trim();
                                if (!fact.isEmpty()) SmartMemory.manuallyRemember(memCtx, fact);
                            }
                        }
                    }

                    cb.onSuccess(reply, imageUrl, followUps);
                }
            } catch (Exception e) {
                ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                if (cm != null) cm.reportApiFailure(e);
                String lastUserMsg = "";
                if (history != null && !history.isEmpty()) {
                    for (int i = history.size() - 1; i >= 0; i--) {
                        if ("user".equalsIgnoreCase(history.get(i).role)) {
                            lastUserMsg = history.get(i).text;
                            break;
                        }
                    }
                }
                String offlineReply = HenryOfflineBrain.generateOfflineResponse(lastUserMsg, queryType, memCtx);
                cb.onSuccess(offlineReply, null, null);
            }
        }).start();
    }

    // ── Client-side intent classifier (mirrors backend logic) ─────────────────
    public static String classifyIntent(String msg) {
        if (msg == null || msg.isEmpty()) return "chat";
        String t = msg.toLowerCase();
        if (t.matches(".*\\b(bitcoin|btc|ethereum|eth|solana|sol|crypto|coin|nft|defi).*")) return "crypto";
        if (t.matches(".*\\d+\\s*(usd|eur|gbp|aed|jpy|php|inr|cad|aud)\\s*(to|in)\\s*(usd|eur|gbp|aed|jpy|php|inr|cad|aud).*")
            || t.contains("exchange rate") || t.matches(".*convert\\s+\\d+.*")) return "forex";
        if (t.matches(".*\\b(news|headlines|latest news|breaking|what happened)\\b.*")) return "news";
        if (t.matches(".*\\b(calculate|compute|what is \\d|sqrt|factorial|\\d+%\\s+of).*")) return "math";
        if (t.matches(".*\\b(latest|breaking|right now|today's|current|2025|2026|score|results|trending)\\b.*")) return "search";
        if (t.matches(".*\\b(why|how does|difference between|compare|pros and cons|should i|step by step)\\b.*")
            && msg.length() > 30) return "reason";
        return "chat";
    }
}
