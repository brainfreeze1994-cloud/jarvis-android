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
 * Meal Planner — AI-generated 7-day meal plan + auto shopping list.
 * "Plan my meals for the week"
 * "Give me a keto meal plan"
 * "Vegetarian meal plan for 3 days"
 * "What should I eat today?"
 */
public class MealPlanner {

    public interface Callback {
        void onResult(String plan);
        void onError(String reason);
    }

    public static boolean isMealCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("meal plan") || lower.contains("meal planner") ||
               lower.contains("plan my meals") || lower.contains("what should i eat") ||
               lower.contains("diet plan") || lower.contains("weekly meals") ||
               lower.contains("food plan") || lower.contains("eating plan") ||
               lower.contains("breakfast lunch dinner") || lower.contains("keto plan") ||
               lower.contains("vegan plan") || lower.contains("vegetarian plan");
    }

    public static void generate(String userQuery, OkHttpClient httpClient,
                                UserProfile profile, boolean withShoppingList, Callback cb) {
        new Thread(() -> {
            try {
                String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "sir";
                String city = (profile != null && !profile.city.isEmpty()) ? profile.city : "";

                boolean isWeekly = userQuery.toLowerCase().contains("week") ||
                                   userQuery.toLowerCase().contains("7 day");
                int days = isWeekly ? 7 : 1;

                String systemPrompt =
                    "You are H.E.N.R.Y, a smart nutritionist and meal planning assistant for " + name + ". " +
                    (city.isEmpty() ? "" : "They are based in " + city + ". ") +
                    "Create a practical, balanced " + days + "-day meal plan. " +
                    "Format each day as:\n" +
                    "**Day X — [Day Name]**\n" +
                    "🌅 Breakfast: [meal]\n" +
                    "☀️ Lunch: [meal]\n" +
                    "🌙 Dinner: [meal]\n" +
                    "🍎 Snack: [optional]\n\n" +
                    (withShoppingList ?
                    "After the plan, add:\n**🛒 Shopping List:**\n[grouped ingredients by category]\n\n" : "") +
                    "Consider their dietary preference from query. Keep meals practical, delicious, and nutritious. " +
                    "Estimate calories for each meal in brackets e.g. (~450 kcal).";

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
                        cb.onError("[EMOTION:concerned] Meal planning unavailable right now, sir."); return;
                    }
                    JSONObject j = new JSONObject(resp.body().string());
                    String reply = j.optString("reply", "");
                    reply = reply.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                    cb.onResult("[EMOTION:excited] " + reply);
                }
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Meal plan error: " + e.getMessage());
            }
        }).start();
    }
}
