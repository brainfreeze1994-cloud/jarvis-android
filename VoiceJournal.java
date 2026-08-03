package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Voice Journal — save timestamped diary entries by voice.
 * Stored as plain text in app files dir; exportable as .txt.
 */
public class VoiceJournal {

    private static final String FILE_NAME = "henry_journal.txt";
    private static final String DATE_FMT  = "yyyy-MM-dd HH:mm";

    // ── Parse command ─────────────────────────────────────────────────────────
    public static boolean isSaveCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("journal") || lower.startsWith("diary") ||
               lower.startsWith("log this") || lower.startsWith("log entry") ||
               lower.contains("journal entry") || lower.contains("diary entry") ||
               lower.startsWith("note to self") || lower.contains("write in my journal") ||
               lower.contains("add to my journal") || lower.contains("add to my diary");
    }

    public static boolean isReadCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.contains("journal") || lower.contains("diary")) &&
               (lower.contains("show") || lower.contains("read") || lower.contains("open") ||
                lower.contains("my entries") || lower.contains("what did i write"));
    }

    public static boolean isSearchCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.contains("journal") || lower.contains("diary")) && lower.contains("search");
    }

    public static boolean isExportCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.contains("journal") || lower.contains("diary")) &&
               (lower.contains("export") || lower.contains("share") || lower.contains("backup"));
    }

    public static String parseEntry(String text) {
        return text
            .replaceFirst("(?i)^(journal entry|journal|diary entry|diary|log entry|log this|note to self)[:\\s]*", "")
            .replaceFirst("(?i)(write in my journal|add to my journal|add to my diary)[:\\s]*", "")
            .trim();
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    public static String save(Context ctx, String content) {
        if (content == null || content.trim().isEmpty())
            return "[EMOTION:neutral] Nothing to journal, sir.";
        try {
            String ts    = new SimpleDateFormat(DATE_FMT, Locale.US).format(new Date());
            String entry = "── " + ts + " ──\n" + content.trim() + "\n\n";
            File f = journalFile(ctx);
            FileOutputStream fos = new FileOutputStream(f, true); // append
            fos.write(entry.getBytes("UTF-8")); fos.close();
            return "[EMOTION:warm] Logged, sir. Entry saved at " + ts + ".";
        } catch (Exception e) {
            return "[EMOTION:concerned] Couldn't save journal entry: " + e.getMessage();
        }
    }

    // ── Read recent ───────────────────────────────────────────────────────────
    public static String readRecent(Context ctx, int maxEntries) {
        try {
            File f = journalFile(ctx);
            if (!f.exists() || f.length() == 0)
                return "[EMOTION:neutral] Your journal is empty, sir. Say 'Journal entry: ...' to begin.";
            String content = readFile(f);
            String[] entries = content.split("── \\d{4}-\\d{2}-\\d{2}");
            if (entries.length <= 1) return "[EMOTION:neutral] No entries yet, sir.";
            int start = Math.max(1, entries.length - maxEntries);
            StringBuilder sb = new StringBuilder("[EMOTION:warm] Your recent journal entries, sir:\n\n");
            // Re-split properly to keep timestamps
            String[] lines = content.split("\n");
            int entryCount = 0;
            StringBuilder current = new StringBuilder();
            List<String> allEntries = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("── ")) {
                    if (current.length() > 0) allEntries.add(current.toString().trim());
                    current = new StringBuilder(line).append("\n");
                } else {
                    current.append(line).append("\n");
                }
            }
            if (current.length() > 0) allEntries.add(current.toString().trim());
            int from = Math.max(0, allEntries.size() - maxEntries);
            for (int i = from; i < allEntries.size(); i++)
                sb.append(allEntries.get(i)).append("\n\n");
            sb.append("Total entries: **").append(allEntries.size()).append("**");
            return sb.toString().trim();
        } catch (Exception e) {
            return "[EMOTION:concerned] Couldn't read journal: " + e.getMessage();
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    public static String search(Context ctx, String keyword) {
        try {
            File f = journalFile(ctx);
            if (!f.exists()) return "[EMOTION:neutral] Journal is empty, sir.";
            String content   = readFile(f);
            String[] lines   = content.split("\n");
            StringBuilder sb = new StringBuilder("[EMOTION:neutral] Journal entries matching **\"" + keyword + "\"**, sir:\n\n");
            String curDate   = "";
            int found        = 0;
            for (String line : lines) {
                if (line.startsWith("── ")) { curDate = line; continue; }
                if (line.toLowerCase().contains(keyword.toLowerCase())) {
                    sb.append(curDate).append("\n").append(line).append("\n\n");
                    found++;
                }
            }
            if (found == 0) return "[EMOTION:neutral] No journal entries matching \"" + keyword + "\", sir.";
            sb.append("Found **").append(found).append("** match(es).");
            return sb.toString().trim();
        } catch (Exception e) {
            return "[EMOTION:concerned] Search error: " + e.getMessage();
        }
    }

    // ── Export ────────────────────────────────────────────────────────────────
    public static Intent exportIntent(Context ctx) {
        try {
            File f = journalFile(ctx);
            if (!f.exists() || f.length() == 0) return null;
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                ctx, ctx.getPackageName() + ".provider", f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, "H.E.N.R.Y Journal Export");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            return Intent.createChooser(i, "Export Journal…");
        } catch (Exception e) { return null; }
    }

    // ── Entry count ───────────────────────────────────────────────────────────
    public static int entryCount(Context ctx) {
        try {
            File f = journalFile(ctx);
            if (!f.exists()) return 0;
            String content = readFile(f);
            int count = 0;
            for (String line : content.split("\n"))
                if (line.startsWith("── ")) count++;
            return count;
        } catch (Exception e) { return 0; }
    }

    private static File journalFile(Context ctx) {
        return new File(ctx.getFilesDir(), FILE_NAME);
    }

    private static String readFile(File f) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(f));
        StringBuilder sb  = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        br.close();
        return sb.toString();
    }
}
