package com.jarvis.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Searches through in-memory message list by keyword.
 */
public class ChatSearch {

    public static class Result {
        public int    index;
        public String role;    // "user" or "henry"
        public String snippet; // up to 120 chars around match
        public Result(int index, String role, String snippet) {
            this.index = index; this.role = role; this.snippet = snippet;
        }
    }

    /**
     * Search through HistoryItem list for keyword matches.
     * Returns formatted reply string.
     */
    public static String search(List<HistoryItem> history, String keyword) {
        if (keyword == null || keyword.trim().isEmpty())
            return "[EMOTION:neutral] What should I search for, sir?";

        String kw = keyword.trim().toLowerCase(Locale.US);
        List<Result> results = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            HistoryItem item = history.get(i);
            if (item.text == null) continue;
            String text = item.text.toLowerCase(Locale.US);
            if (text.contains(kw)) {
                // Build snippet
                int idx = text.indexOf(kw);
                int start = Math.max(0, idx - 40);
                int end   = Math.min(item.text.length(), idx + keyword.length() + 60);
                String snippet = (start > 0 ? "…" : "") + item.text.substring(start, end) + (end < item.text.length() ? "…" : "");
                snippet = snippet.replaceAll("[\\r\\n]+", " ").trim();
                String role = "user".equals(item.role) ? "You" : "HENRY";
                results.add(new Result(i, role, snippet));
            }
        }

        if (results.isEmpty())
            return "[EMOTION:neutral] No results for **\"" + keyword + "\"** in our conversation, sir.";

        StringBuilder sb = new StringBuilder();
        sb.append("[EMOTION:neutral] Found **").append(results.size())
          .append(" result").append(results.size() == 1 ? "" : "s")
          .append("** for \"").append(keyword).append("\", sir:\n\n");

        // Show up to 6 results
        int show = Math.min(results.size(), 6);
        for (int i = 0; i < show; i++) {
            Result r = results.get(i);
            sb.append("**").append(r.role).append(":** ").append(r.snippet).append("\n\n");
        }
        if (results.size() > 6)
            sb.append("_…and ").append(results.size() - 6).append(" more._");

        return sb.toString().trim();
    }

    /**
     * Detect search command in user text.
     * Returns keyword to search, or null.
     */
    public static String parseSearchCommand(String text) {
        String t = text.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "(?:search|find|look for|search for|find in)\\s+(?:chat|conversation|history)\\s+(?:for\\s+)?[\"']?(.+?)[\"']?$",
            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(t);
        if (m.find()) return m.group(1).trim();

        m = java.util.regex.Pattern.compile(
            "(?:when did (?:i|we)|did (?:i|we) (?:talk about|discuss|mention|ask about))\\s+(.+?)(?:\\?|$)",
            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(t);
        if (m.find()) return m.group(1).trim();

        return null;
    }
}
