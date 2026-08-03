package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Conversation Insights — analyses chat history and returns a personal summary.
 * Combines local keyword analysis + AI for a rich, personal response.
 */
public class ConversationInsights {

    public interface Callback {
        void onResult(String insight);
        void onError(String reason);
    }

    public static boolean isInsightCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("what have we talked about") || lower.contains("conversation summary") ||
               lower.contains("chat summary") || lower.contains("chat insights") ||
               lower.contains("our conversations") || lower.contains("what do you know about me") ||
               lower.contains("what have i been asking") || lower.contains("conversation history") ||
               lower.contains("what topics") || lower.contains("what do i usually ask") ||
               lower.contains("summarize our chat") || lower.contains("summarise our chat") ||
               lower.contains("analyse our chat") || lower.contains("analyze our chat") ||
               lower.contains("personal insights") || lower.contains("know about me");
    }

    public static void analyse(List<HistoryItem> history, OkHttpClient httpClient,
                                UserProfile profile, Callback cb) {
        if (history == null || history.isEmpty()) {
            cb.onResult("[EMOTION:neutral] We haven't talked much yet, sir. Ask me anything to get started.");
            return;
        }

        // ── Local analysis ────────────────────────────────────────────────────
        Map<String, Integer> topicCount = new HashMap<>();
        int userMsgs = 0, aiMsgs = 0;
        int totalWords = 0;
        String longestMsg = "";

        String[] topics = {
            "weather", "news", "music", "timer", "reminder", "alarm", "stock", "crypto",
            "bitcoin", "price", "fitness", "steps", "calendar", "schedule", "note",
            "journal", "shopping", "whatsapp", "call", "message", "translate",
            "image", "camera", "photo", "pdf", "search", "google"
        };

        for (HistoryItem item : history) {
            String lower = item.text.toLowerCase(Locale.US);
            if ("user".equals(item.role)) {
                userMsgs++;
                int words = item.text.trim().split("\\s+").length;
                totalWords += words;
                if (item.text.length() > longestMsg.length()) longestMsg = item.text;
                for (String topic : topics) {
                    if (lower.contains(topic)) {
                        topicCount.put(topic, topicCount.getOrDefault(topic, 0) + 1);
                    }
                }
            } else {
                aiMsgs++;
            }
        }

        // Sort topics by frequency
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(topicCount.entrySet());
        Collections.sort(sorted, (a, b) -> b.getValue() - a.getValue());
        StringBuilder topTopics = new StringBuilder();
        int shown = 0;
        for (Map.Entry<String, Integer> e : sorted) {
            if (shown++ >= 5) break;
            topTopics.append("**").append(e.getKey()).append("** (").append(e.getValue()).append("x), ");
        }
        if (topTopics.length() > 2) topTopics.setLength(topTopics.length() - 2);

        String localSummary = String.format(Locale.US,
            "Total messages: **%d** (%d from you, %d from me)\n" +
            "Your average message: **%d words**\n" +
            "Top topics: %s",
            userMsgs + aiMsgs, userMsgs, aiMsgs,
            userMsgs > 0 ? totalWords / userMsgs : 0,
            topTopics.length() > 0 ? topTopics.toString() : "varied");

        // ── AI deep analysis ──────────────────────────────────────────────────
        // Sample last 40 user messages for AI analysis
        List<String> userMessages = new ArrayList<>();
        for (HistoryItem item : history)
            if ("user".equals(item.role)) userMessages.add(item.text);
        List<String> sample = userMessages.size() > 40
            ? userMessages.subList(userMessages.size() - 40, userMessages.size()) : userMessages;

        StringBuilder convoText = new StringBuilder();
        for (String msg : sample) convoText.append("- ").append(msg).append("\n");

        String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "sir";
        String prompt = "You are H.E.N.R.Y analysing your own conversation history with " + name + ". " +
            "Based on these recent messages from the user, give a warm, personal, insightful summary: " +
            "What topics does this person care about most? What patterns do you notice? " +
            "What does this reveal about their personality or lifestyle? " +
            "Be specific, warm, slightly witty. 3-5 sentences max.\n\n" +
            "User messages:\n" + convoText.toString();

        new Thread(() -> {
            try {
                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user"); msg.put("content", prompt);
                msgs.put(msg);

                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "balanced");

                RequestBody rb = RequestBody.create(
                    body.toString(), MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                    .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                    .post(rb).addHeader("Content-Type", "application/json").build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    String aiInsight = "[No AI insight available]";
                    if (resp.isSuccessful() && resp.body() != null) {
                        JSONObject j = new JSONObject(resp.body().string());
                        aiInsight = j.optString("reply", aiInsight);
                        aiInsight = aiInsight.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                    }
                    final String finalAi    = aiInsight;
                    final String finalLocal = localSummary;
                    cb.onResult("[EMOTION:warm] **Conversation Insights, sir:**\n\n" +
                        finalLocal + "\n\n**My read on you:**\n" + finalAi);
                }
            } catch (Exception e) {
                cb.onResult("[EMOTION:warm] **Conversation Insights, sir:**\n\n" + localSummary);
            }
        }).start();
    }
}
