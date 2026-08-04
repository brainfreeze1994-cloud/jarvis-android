package com.jarvis.ai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Daily Summary — AI-powered end-of-day recap.
 * Combines: conversation highlights, tasks done, steps, mood, spending.
 * "Daily summary" / "Summarise my day" / "How was my day?"
 */
public class DailySummary {

    public interface Callback {
        void onResult(String summary);
        void onError(String reason);
    }

    public static boolean isSummaryCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("daily summary") || lower.contains("summarise my day") ||
               lower.contains("summarize my day") || lower.contains("how was my day") ||
               lower.contains("day summary") || lower.contains("end of day") ||
               lower.contains("recap my day") || lower.contains("what did i do today") ||
               lower.contains("today's summary") || lower.contains("day recap");
    }

    public static void generate(Context ctx, List<HistoryItem> history,
                                OkHttpClient httpClient, UserProfile profile, Callback cb) {
        new Thread(() -> {
            try {
                String date = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(new Date());
                String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "sir";

                // Gather data
                String stepsReport  = FitnessTracker.getStatusReport(ctx)
                    .replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                String moodResult   = MoodTracker.handle(ctx, "mood report");
                String moodSummary  = moodResult != null
                    ? moodResult.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim() : "No mood data";
                String taskResult   = TaskManager.handle(ctx, "my tasks");
                String taskSummary  = taskResult != null
                    ? taskResult.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim() : "No tasks";
                String expResult    = ExpenseTracker.handle(ctx, "my expenses");
                String expSummary   = expResult != null
                    ? expResult.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim() : "No expenses";

                // Get today's conversation highlights (last 20 user messages)
                List<String> todayMsgs = new ArrayList<>();
                for (HistoryItem item : history)
                    if ("user".equals(item.role)) todayMsgs.add(item.text);
                List<String> sample = todayMsgs.size() > 20
                    ? todayMsgs.subList(todayMsgs.size() - 20, todayMsgs.size()) : todayMsgs;
                StringBuilder convo = new StringBuilder();
                for (String m : sample) convo.append("- ").append(m).append("\n");

                String systemPrompt =
                    "You are H.E.N.R.Y giving " + name + " their end-of-day summary for " + date + ". " +
                    "Be warm, personal, insightful, and encouraging. " +
                    "Structure EXACTLY as:\n" +
                    "**☀️ Day Overview** — one warm sentence about their day\n" +
                    "**💬 Conversation Highlights** — 2-3 topics they explored\n" +
                    "**✅ Productivity** — quick note on tasks/reminders\n" +
                    "**🏃 Fitness** — step count & progress\n" +
                    "**💭 Mood** — how they seemed to feel\n" +
                    "**💰 Spending** — quick note if they tracked expenses\n" +
                    "**⭐ HENRY's Thought** — one personal, motivating closing line\n\n" +
                    "Keep each section to 1-2 lines. Be specific using the data provided.";

                String userContent =
                    "Today's data for " + name + ":\n\n" +
                    "STEPS:\n" + stepsReport + "\n\n" +
                    "MOOD:\n" + moodSummary + "\n\n" +
                    "TASKS:\n" + taskSummary + "\n\n" +
                    "EXPENSES:\n" + expSummary + "\n\n" +
                    "CONVERSATION (what they asked about today):\n" + convo.toString();

                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user"); msg.put("content", userContent);
                msgs.put(msg);

                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "balanced");
                body.put("systemOverride", systemPrompt);

                RequestBody rb = RequestBody.create(
                    body.toString(), MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                    .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                    .post(rb).addHeader("Content-Type", "application/json").build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        cb.onError("[EMOTION:concerned] Couldn't generate your daily summary, sir."); return;
                    }
                    JSONObject j = new JSONObject(resp.body().string());
                    String reply = j.optString("reply", "");
                    reply = reply.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                    cb.onResult("[EMOTION:warm] **Daily Summary — " + date + "**\n\n" + reply);
                }
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Summary error: " + e.getMessage());
            }
        }).start();
    }
}
