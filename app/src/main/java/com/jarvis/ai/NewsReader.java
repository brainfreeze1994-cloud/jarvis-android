package com.jarvis.ai;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches top headlines from GNews API (free tier: 100 req/day, no key needed for basic).
 * Falls back to RSS via rss2json.com (free, no key).
 */
public class NewsReader {

    public interface Callback {
        void onResult(String formatted);
        void onError(String reason);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // GNews free endpoint (no key for basic English news)
    private static final String GNEWS_URL =
        "https://gnews.io/api/v4/top-headlines?lang=en&max=5&apikey=demokey";

    // Fallback: BBC Top Stories via rss2json (free, no account)
    private static final String RSS2JSON_URL =
        "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Ffeeds.bbci.co.uk%2Fnews%2Fworld%2Frss.xml";

    public static void fetch(Callback cb) {
        new Thread(() -> {
            try {
                // Try BBC RSS via rss2json first (most reliable free option)
                String json = get(RSS2JSON_URL);
                JSONObject root = new JSONObject(json);
                String status = root.optString("status", "");
                if ("ok".equals(status)) {
                    JSONArray items = root.getJSONArray("items");
                    List<String> headlines = new ArrayList<>();
                    for (int i = 0; i < Math.min(items.length(), 7); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String title = item.optString("title", "").trim();
                        if (!title.isEmpty()) headlines.add(title);
                    }
                    if (!headlines.isEmpty()) {
                        MAIN.post(() -> cb.onResult(format(headlines, "BBC World")));
                        return;
                    }
                }
                MAIN.post(() -> cb.onError("No headlines available right now, sir."));
            } catch (Exception e) {
                MAIN.post(() -> cb.onError("Could not fetch news: " + e.getMessage()));
            }
        }).start();
    }

    private static String format(List<String> headlines, String source) {
        StringBuilder sb = new StringBuilder();
        sb.append("[EMOTION:excited] **Morning briefing — top ").append(headlines.size())
          .append(" headlines from ").append(source).append(":**\n\n");
        for (int i = 0; i < headlines.size(); i++) {
            sb.append("**").append(i + 1).append(".** ").append(headlines.get(i)).append("\n\n");
        }
        sb.append("That's your briefing, sir.");
        return sb.toString().trim();
    }

    private static String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "HENRY-AI/4.0");
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
}
