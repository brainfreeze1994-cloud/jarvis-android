package com.jarvis.ai;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * HENRY Real-Time Web Search Engine
 * Performs multi-source live web searches across Wikipedia, DuckDuckGo,
 * and Google News RSS to provide verified, factual data directly inside HENRY.
 */
public class HenryWebSearch {

    public interface SearchCallback {
        void onSearchResult(String summary, String source, List<String> sources);
        void onError(String reason);
    }

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build();

    public static boolean isRefusal(String text) {
        if (text == null) return false;
        String l = text.toLowerCase();
        return l.contains("wasn't able to get a verified answer")
            || l.contains("rather than guess")
            || l.contains("don't have a confirmed source")
            || l.contains("do not have a confirmed source")
            || l.contains("could not find a verified answer")
            || l.contains("was unable to get a verified answer");
    }

    public static boolean isSearchQuery(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String l = text.toLowerCase().trim();
        return l.matches(".*\\b(search|look up|find out|google|who is|what is|where is|latest|newest|news about|recent|compare|specs|when is|who won|score of|price of)\\b.*");
    }

    public static void search(String query, SearchCallback cb) {
        new Thread(() -> {
            try {
                String cleanQuery = query
                    .replaceAll("(?i)^(who is|what is|where is|tell me about|search for|look up|search the web for|find out|google)\\s+", "")
                    .replaceAll("\\[[^\\]]*\\]", "")
                    .trim();
                if (cleanQuery.isEmpty()) cleanQuery = query.trim();

                StringBuilder summary = new StringBuilder();
                List<String> sources = new ArrayList<>();

                // 1. Query Wikipedia Search & Extract
                try {
                    String wikiSearchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch="
                            + Uri.encode(cleanQuery) + "&format=json&utf8=1";
                    Request req = new Request.Builder()
                            .url(wikiSearchUrl)
                            .header("User-Agent", "HENRY-Assistant/1.0 (Android)")
                            .build();
                    try (Response resp = httpClient.newCall(req).execute()) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            JSONObject json = new JSONObject(resp.body().string());
                            JSONObject qObj = json.optJSONObject("query");
                            if (qObj != null) {
                                JSONArray items = qObj.optJSONArray("search");
                                if (items != null && items.length() > 0) {
                                    String topTitle = items.getJSONObject(0).optString("title", "");
                                    if (!topTitle.isEmpty()) {
                                        String extractUrl = "https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=1&explaintext=1&titles="
                                                + Uri.encode(topTitle) + "&format=json&utf8=1";
                                        Request exReq = new Request.Builder()
                                                .url(extractUrl)
                                                .header("User-Agent", "HENRY-Assistant/1.0 (Android)")
                                                .build();
                                        try (Response exResp = httpClient.newCall(exReq).execute()) {
                                            if (exResp.isSuccessful() && exResp.body() != null) {
                                                JSONObject exJson = new JSONObject(exResp.body().string());
                                                JSONObject pages = exJson.optJSONObject("query") != null
                                                        ? exJson.getJSONObject("query").optJSONObject("pages") : null;
                                                if (pages != null) {
                                                    java.util.Iterator<String> keys = pages.keys();
                                                    if (keys.hasNext()) {
                                                        JSONObject page = pages.getJSONObject(keys.next());
                                                        String extract = page.optString("extract", "").trim();
                                                        if (!extract.isEmpty()) {
                                                            summary.append(extract);
                                                            sources.add("Wikipedia (" + topTitle + ")");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // 2. Query DuckDuckGo Instant Answer
                if (summary.length() < 120) {
                    try {
                        String ddgUrl = "https://api.duckduckgo.com/?q=" + Uri.encode(cleanQuery) + "&format=json&no_html=1&skip_disambig=1";
                        Request req = new Request.Builder()
                                .url(ddgUrl)
                                .header("User-Agent", "HENRY-Assistant/1.0")
                                .build();
                        try (Response resp = httpClient.newCall(req).execute()) {
                            if (resp.isSuccessful() && resp.body() != null) {
                                JSONObject json = new JSONObject(resp.body().string());
                                String abs = json.optString("AbstractText", "").trim();
                                if (!abs.isEmpty()) {
                                    if (summary.length() > 0) summary.append("\n\n");
                                    summary.append(abs);
                                    sources.add("DuckDuckGo");
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // 3. Query Google News RSS if it's news, current events, or query is time-sensitive
                if (query.toLowerCase().contains("news") || query.toLowerCase().contains("latest") || summary.length() < 100) {
                    try {
                        String newsUrl = "https://news.google.com/rss/search?q=" + Uri.encode(cleanQuery) + "&hl=en-US&gl=US&ceid=US:en";
                        Request req = new Request.Builder()
                                .url(newsUrl)
                                .header("User-Agent", "Mozilla/5.0")
                                .build();
                        try (Response resp = httpClient.newCall(req).execute()) {
                            if (resp.isSuccessful() && resp.body() != null) {
                                String xml = resp.body().string();
                                Pattern p = Pattern.compile("<item>[\\s\\S]*?<title>(.*?)</title>[\\s\\S]*?</item>");
                                Matcher m = p.matcher(xml);
                                List<String> headlines = new ArrayList<>();
                                while (m.find() && headlines.size() < 4) {
                                    String rawTitle = m.group(1).replaceAll("<!\\[CDATA\\[(.*?)\\]\\]>", "$1").trim();
                                    headlines.add("• " + rawTitle);
                                }
                                if (!headlines.isEmpty()) {
                                    if (summary.length() > 0) summary.append("\n\n**Latest Headlines:**\n");
                                    for (String h : headlines) summary.append(h).append("\n");
                                    sources.add("Google News");
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (summary.length() > 0) {
                        String finalSources = sources.isEmpty() ? "Web Verification" : String.join(", ", sources);
                        cb.onSearchResult(summary.toString().trim(), finalSources, sources);
                    } else {
                        cb.onError("No verified search results found.");
                    }
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }
}
