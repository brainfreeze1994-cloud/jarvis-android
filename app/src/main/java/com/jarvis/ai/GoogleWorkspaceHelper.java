package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GoogleWorkspaceHelper — creates Google Docs, Sheets, and Slides
 * via the HENRY Vercel backend (/api/google-workspace).
 * No OAuth on Android side — the backend holds the service account.
 */
public class GoogleWorkspaceHelper {

    public enum DocType { DOCS, SHEETS, SLIDES }

    public interface Callback {
        void onSuccess(String url, String title, DocType type);
        void onError(String reason);
    }

    private static final String API_URL = "https://jarvis-ai-seven-dun.vercel.app/api/google-workspace";

    // ── Intent detection ──────────────────────────────────────────────────────

    public static boolean isDocCommand(String input) {
        String t = input.toLowerCase();
        return (t.contains("create") || t.contains("make") || t.contains("new") || t.contains("open") || t.contains("start") || t.contains("generate")) &&
               (t.contains("doc") || t.contains("sheet") || t.contains("slide") || t.contains("spreadsheet") || t.contains("presentation") || t.contains("google"));
    }

    public static DocType detectType(String input) {
        String t = input.toLowerCase();
        if (t.contains("sheet") || t.contains("spreadsheet") || t.contains("excel") || t.contains("table") || t.contains("csv")) return DocType.SHEETS;
        if (t.contains("slide") || t.contains("presentation") || t.contains("powerpoint") || t.contains("deck")) return DocType.SLIDES;
        return DocType.DOCS;
    }

    public static String extractTitle(String input) {
        // Try to extract a title after common phrases
        Pattern[] patterns = {
            Pattern.compile("(?:called|titled|named|about|for|on|regarding)\\s+[\"']?(.+?)[\"']?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:create|make|new|start)\\s+(?:a\\s+)?(?:google\\s+)?(?:doc|sheet|slide|spreadsheet|presentation)\\s+(?:for|about|on|titled|called)?\\s*(.+)", Pattern.CASE_INSENSITIVE),
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(input.trim());
            if (m.find()) {
                String title = m.group(1).trim().replaceAll("[\"']", "");
                if (title.length() > 2 && title.length() < 80) return capitalize(title);
            }
        }
        return "HENRY Document";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Build real content from the current chat, per doc type ─────────────────
    // This is the piece that was always missing: create() was being called with
    // content=null, so every doc/sheet/slide came out empty regardless of what
    // was asked for.
    public static String buildContentFromHistory(java.util.List<HistoryItem> history, DocType type) {
        if (history == null || history.isEmpty()) return "";
        switch (type) {
            case SHEETS: return buildSheetContent(history);
            case SLIDES: return buildSlideContent(history);
            default:     return buildDocContent(history);
        }
    }

    private static String buildDocContent(java.util.List<HistoryItem> history) {
        StringBuilder sb = new StringBuilder();
        for (HistoryItem h : history) {
            String speaker = "user".equals(h.role) ? "You" : "HENRY";
            sb.append(speaker).append(":\n").append(h.text == null ? "" : h.text).append("\n\n");
        }
        return sb.toString().trim();
    }

    // Sheets content is parsed as CSV rows by the backend — one exchange per row.
    private static String buildSheetContent(java.util.List<HistoryItem> history) {
        StringBuilder sb = new StringBuilder("Speaker,Message\n");
        for (HistoryItem h : history) {
            String speaker = "user".equals(h.role) ? "You" : "HENRY";
            String msg = (h.text == null ? "" : h.text).replace("\"", "'").replace("\n", " ");
            sb.append(speaker).append(",\"").append(msg).append("\"\n");
        }
        return sb.toString().trim();
    }

    // Slides content uses a "SLIDE: <title>" marker per slide, body lines after
    // it until the next marker — the backend splits on this to build real slides
    // instead of one empty presentation. Groups the conversation into chunks of
    // a few exchanges per slide so a long chat doesn't become one giant slide.
    private static String buildSlideContent(java.util.List<HistoryItem> history) {
        StringBuilder sb = new StringBuilder();
        int chunkSize = 4; // messages per slide
        for (int i = 0; i < history.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, history.size());
            sb.append("SLIDE: Part ").append((i / chunkSize) + 1).append("\n");
            for (int j = i; j < end; j++) {
                HistoryItem h = history.get(j);
                String speaker = "user".equals(h.role) ? "You" : "HENRY";
                String msg = h.text == null ? "" : h.text;
                if (msg.length() > 200) msg = msg.substring(0, 200) + "…";
                sb.append(speaker).append(": ").append(msg).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    // ── Network call ──────────────────────────────────────────────────────────

    public static void create(String title, DocType type, String initialContent, Callback cb) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("type", type.name().toLowerCase()); // "docs" | "sheets" | "slides"
                body.put("title", title);
                if (initialContent != null && !initialContent.isEmpty()) {
                    body.put("content", initialContent);
                }

                HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                if (resp.optBoolean("success", false)) {
                    cb.onSuccess(resp.getString("url"), resp.getString("title"), type);
                } else {
                    cb.onError(resp.optString("error", "Unknown error"));
                }
            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        }).start();
    }

    // ── Open URL in browser ───────────────────────────────────────────────────
    public static void openInBrowser(Context ctx, String url) {
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {}
    }

    public static String typeName(DocType t) {
        switch (t) {
            case SHEETS: return "Google Sheet";
            case SLIDES: return "Google Slides";
            default:     return "Google Doc";
        }
    }
}
