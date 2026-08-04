package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Smart File Manager — browse, share, delete files by voice.
 * "Show my files", "Open downloads", "Share last photo", "Delete file X"
 */
public class SmartFileManager {

    public static boolean isFileCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("my files") || lower.contains("file manager") ||
               lower.contains("open downloads") || lower.contains("browse files") ||
               lower.contains("show files") || lower.contains("list files") ||
               lower.contains("delete file") || lower.contains("share file") ||
               lower.contains("open file") || lower.contains("find file") ||
               lower.contains("recent files") || lower.contains("my documents") ||
               lower.contains("my downloads") || lower.contains("my photos") ||
               lower.contains("largest files") || lower.contains("storage usage") ||
               lower.contains("free space") || lower.contains("disk space");
    }

    public static String handle(Context ctx, String text) {
        String lower = text.toLowerCase(Locale.US);

        if (lower.contains("storage") || lower.contains("free space") || lower.contains("disk space")) {
            return getStorageInfo();
        }
        if (lower.contains("recent files") || lower.contains("show files") || lower.contains("list files") || lower.contains("my files")) {
            return listRecentFiles(ctx);
        }
        if (lower.contains("downloads") || lower.contains("my downloads")) {
            return listDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        }
        if (lower.contains("photos") || lower.contains("pictures") || lower.contains("my photos")) {
            return listDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
        }
        if (lower.contains("documents") || lower.contains("my documents")) {
            return listDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
        }
        if (lower.contains("largest files")) {
            return findLargestFiles();
        }
        if (lower.contains("delete file")) {
            String name = lower.replace("delete file", "").trim();
            return deleteFileByName(ctx, name);
        }
        if (lower.contains("share file")) {
            String name = lower.replace("share file", "").trim();
            return shareFileByName(ctx, name);
        }
        return listRecentFiles(ctx);
    }

    private static String getStorageInfo() {
        try {
            File f = Environment.getExternalStorageDirectory();
            long total = f.getTotalSpace();
            long free  = f.getFreeSpace();
            long used  = total - free;
            return String.format(Locale.US,
                "[EMOTION:neutral] **📱 Storage Info:**\n\n" +
                "💾 Total: **%.1f GB**\n" +
                "✅ Used: **%.1f GB**\n" +
                "🆓 Free: **%.1f GB**\n" +
                "📊 Usage: **%d%%**",
                total / 1e9, used / 1e9, free / 1e9, (int)(used * 100 / total));
        } catch (Exception e) {
            return "[EMOTION:neutral] Storage info unavailable, sir.";
        }
    }

    private static String listRecentFiles(Context ctx) {
        try {
            List<File> files = new ArrayList<>();
            addFilesFrom(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), files);
            addFilesFrom(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), files);
            addFilesFrom(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), files);
            addFilesFrom(ctx.getExternalFilesDir(null), files);

            files.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            if (files.isEmpty()) return "[EMOTION:neutral] No files found, sir.";

            StringBuilder sb = new StringBuilder("[EMOTION:neutral] **📁 Recent Files:**\n\n");
            int count = Math.min(10, files.size());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.US);
            for (int i = 0; i < count; i++) {
                File file = files.get(i);
                sb.append(String.format(Locale.US, "%d. **%s** — %s, %s\n",
                    i + 1, file.getName(), formatSize(file.length()),
                    sdf.format(new Date(file.lastModified()))));
            }
            return sb.toString();
        } catch (Exception e) {
            return "[EMOTION:neutral] Can't read files, sir. Storage permission may be needed.";
        }
    }

    private static String listDirectory(File dir) {
        if (dir == null || !dir.exists()) return "[EMOTION:neutral] Directory not found, sir.";
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return "[EMOTION:neutral] That folder is empty, sir.";
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        StringBuilder sb = new StringBuilder("[EMOTION:neutral] **📂 " + dir.getName() + ":**\n\n");
        int count = Math.min(10, files.length);
        for (int i = 0; i < count; i++) {
            sb.append(String.format(Locale.US, "%d. %s — %s\n",
                i + 1, files[i].getName(), formatSize(files[i].length())));
        }
        if (files.length > 10) sb.append("…and ").append(files.length - 10).append(" more.");
        return sb.toString();
    }

    private static String findLargestFiles() {
        try {
            List<File> files = new ArrayList<>();
            addFilesFrom(Environment.getExternalStorageDirectory(), files);
            files.sort((a, b) -> Long.compare(b.length(), a.length()));
            StringBuilder sb = new StringBuilder("[EMOTION:neutral] **📦 Largest Files:**\n\n");
            int count = Math.min(8, files.size());
            for (int i = 0; i < count; i++) {
                sb.append(String.format(Locale.US, "%d. %s — **%s**\n",
                    i + 1, files.get(i).getName(), formatSize(files.get(i).length())));
            }
            return sb.toString();
        } catch (Exception e) {
            return "[EMOTION:neutral] Can't scan for large files, sir.";
        }
    }

    private static String deleteFileByName(Context ctx, String name) {
        if (name.isEmpty()) return "[EMOTION:neutral] Which file should I delete, sir?";
        List<File> files = new ArrayList<>();
        addFilesFrom(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), files);
        addFilesFrom(ctx.getExternalFilesDir(null), files);
        for (File f : files) {
            if (f.getName().toLowerCase(Locale.US).contains(name)) {
                boolean deleted = f.delete();
                return deleted
                    ? "[EMOTION:neutral] Deleted **" + f.getName() + "**, sir."
                    : "[EMOTION:concerned] Couldn't delete that file, sir. It may be in use.";
            }
        }
        return "[EMOTION:neutral] File not found, sir. Try 'show my files' first.";
    }

    private static String shareFileByName(Context ctx, String name) {
        List<File> files = new ArrayList<>();
        addFilesFrom(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), files);
        addFilesFrom(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), files);
        for (File f : files) {
            if (f.getName().toLowerCase(Locale.US).contains(name)) {
                return "SHARE:" + f.getAbsolutePath(); // Handled in MainActivity
            }
        }
        return "[EMOTION:neutral] File not found to share, sir.";
    }

    public static Intent buildShareIntent(Context ctx, String path) {
        try {
            File f = new File(path);
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", f);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return Intent.createChooser(intent, "Share via");
        } catch (Exception e) { return null; }
    }

    private static void addFilesFrom(File dir, List<File> list) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile() && f.length() > 0) list.add(f);
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
