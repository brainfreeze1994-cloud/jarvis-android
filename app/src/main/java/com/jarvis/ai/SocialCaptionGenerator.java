package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Social Media Caption Generator — AI captions for Instagram, LinkedIn, Twitter/X, TikTok.
 * "Write an Instagram caption for my beach photo"
 * "LinkedIn post about my new job"
 * "Tweet about Dubai sunset"
 */
public class SocialCaptionGenerator {

    public static boolean isCaptionCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("instagram caption") || lower.contains("ig caption") ||
               lower.contains("linkedin post") || lower.contains("tweet") ||
               lower.contains("twitter post") || lower.contains("tiktok caption") ||
               lower.contains("facebook post") || lower.contains("social media post") ||
               lower.contains("write a caption") || lower.contains("caption for") ||
               lower.contains("post about") || lower.contains("caption about") ||
               lower.contains("social post");
    }

    public interface Callback {
        void onResult(String caption);
        void onError(String reason);
    }

    public static void generate(String userQuery, UserProfile profile, Callback cb) {
        new Thread(() -> {
            try {
                String lower = userQuery.toLowerCase(Locale.US);

                // Detect platform
                String platform = "Instagram";
                int maxLen = 2200;
                String style = "engaging, emoji-rich, with relevant hashtags";

                if (lower.contains("linkedin")) {
                    platform = "LinkedIn"; maxLen = 700;
                    style = "professional, insightful, thought-provoking, minimal hashtags (3-5)";
                } else if (lower.contains("tweet") || lower.contains("twitter") || lower.contains("x post")) {
                    platform = "Twitter/X"; maxLen = 280;
                    style = "witty, punchy, under 250 chars, 2-3 hashtags";
                } else if (lower.contains("tiktok")) {
                    platform = "TikTok"; maxLen = 500;
                    style = "trendy, fun, with viral hooks and hashtags";
                } else if (lower.contains("facebook")) {
                    platform = "Facebook"; maxLen = 500;
                    style = "friendly, relatable, conversational";
                }

                // Detect tone
                String tone = "engaging";
                if (lower.contains("funny") || lower.contains("humour")) tone = "funny and witty";
                else if (lower.contains("inspir")) tone = "inspirational";
                else if (lower.contains("profess")) tone = "professional";
                else if (lower.contains("romantic")) tone = "romantic and poetic";
                else if (lower.contains("motivat")) tone = "motivational";

                String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "the user";
                String systemPrompt = "You are H.E.N.R.Y, a creative social media expert for " + name + ". " +
                    "Write a " + tone + " " + platform + " caption (" + style + "). " +
                    "Max " + maxLen + " characters. " +
                    "Include 5-10 relevant hashtags at the end (fewer for LinkedIn/Twitter), always featuring #HENRY and #HENRYAI. Never include #Jarvis or #IronMan. " +
                    "Write ONLY the caption — no quotes, no 'here's a caption', just the text.";

                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user"); msg.put("content", userQuery);
                msgs.put(msg);
                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "balanced");
                body.put("systemOverride", systemPrompt);

                URL url = new URL("https://jarvis-ai-seven-dun.vercel.app/api/jarvis");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); conn.setDoOutput(true);
                conn.setConnectTimeout(15000); conn.setReadTimeout(20000);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) { os.write(body.toString().getBytes("UTF-8")); }
                InputStream is = conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[4096]; int r;
                while ((r = is.read(buf)) != -1) sb.append(new String(buf, 0, r, "UTF-8"));
                is.close();
                JSONObject j = new JSONObject(sb.toString());
                String caption = j.optString("reply", "").replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                cb.onResult("[EMOTION:excited] **" + platform + " Caption:**\n\n" + caption);
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Caption error, sir: " + e.getMessage());
            }
        }).start();
    }
}
