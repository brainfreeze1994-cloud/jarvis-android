package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI Recipe Generator — asks HENRY backend to generate a recipe.
 * "What can I make with chicken and rice?"
 * "Recipe for pasta carbonara"
 * "Give me a healthy breakfast idea"
 */
public class RecipeGenerator {

    public interface Callback {
        void onResult(String recipe);
        void onError(String reason);
    }

    public static boolean isRecipeCommand(String text) {
        // "recipe" is a bare substring, and "create a document about the
        // adobo recipe" contains it — that was silently hijacking every
        // recipe-related doc creation request before it ever reached
        // GoogleWorkspaceHelper. Reusing its own check here keeps both
        // features in sync instead of maintaining two divergent regexes.
        if (GoogleWorkspaceHelper.isDocCommand(text)) return false;
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("recipe") ||
               lower.contains("what can i make") || lower.contains("what can i cook") ||
               lower.contains("how do i cook") || lower.contains("how to cook") ||
               lower.contains("how to make") || lower.contains("cook for me") ||
               lower.contains("give me a recipe") || lower.contains("make with") ||
               lower.contains("i have") && (lower.contains("cook") || lower.contains("eat") || lower.contains("make")) ||
               lower.contains("meal idea") || lower.contains("dinner idea") ||
               lower.contains("lunch idea") || lower.contains("breakfast idea") ||
               lower.contains("healthy recipe") || lower.contains("quick meal");
    }

    public static void generate(String userQuery, OkHttpClient httpClient,
                                 UserProfile profile, Callback cb) {
        new Thread(() -> {
            try {
                String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "sir";
                String city = (profile != null && !profile.city.isEmpty()) ? profile.city : "";

                String systemOverride = "You are H.E.N.R.Y, a brilliant culinary assistant. " +
                    "Generate a clear, practical recipe. Format: " +
                    "1) Dish name in bold, 2) Ingredients list, 3) Step-by-step cooking instructions, " +
                    "4) One quick tip. Keep it concise and actionable. " +
                    (city.isEmpty() ? "" : "The user is in " + city + " so consider ingredient availability. ") +
                    "No lengthy preamble — just the recipe.";

                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", userQuery);
                msgs.put(msg);

                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "detailed");
                body.put("systemOverride", systemOverride);

                RequestBody rb = RequestBody.create(
                    body.toString(), MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                    .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                    .post(rb).addHeader("Content-Type", "application/json").build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        cb.onError("[EMOTION:concerned] Couldn't generate recipe right now, sir.");
                        return;
                    }
                    JSONObject j = new JSONObject(resp.body().string());
                    String recipe = j.optString("reply", "");
                    recipe = recipe.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                    if (recipe.isEmpty()) { cb.onError("[EMOTION:neutral] No recipe generated, sir."); return; }
                    cb.onResult("[EMOTION:excited] " + recipe);
                }
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Recipe error: " + e.getMessage());
            }
        }).start();
    }
}
