package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Opens a QR/barcode scanner via intent.
 * Uses Google Lens (most common), falls back to ZXing, then Play Store.
 */
public class QrScanner {

    public static boolean isQrCommand(String text) {
        String t = text.toLowerCase();
        return (t.contains("scan") || t.contains("read")) &&
               (t.contains("qr") || t.contains("barcode") || t.contains("code"));
    }

    public static String scan(Context ctx) {
        // Try Google Lens
        if (tryLaunch(ctx, "com.google.ar.lens")) return "[EMOTION:neutral] Opening Google Lens to scan, sir.";
        // Try ZXing Barcode Scanner
        if (tryLaunch(ctx, "com.google.zxing.client.android")) return "[EMOTION:neutral] Opening barcode scanner, sir.";
        // Try Samsung scanner
        if (tryLaunch(ctx, "com.samsung.android.bixby.service")) return "[EMOTION:neutral] Opening scanner, sir.";
        // Fallback: open Google Lens via browser
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com/"));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "[EMOTION:neutral] Opening Google Lens in browser, sir.";
        } catch (Exception e) {
            return "[EMOTION:concerned] No QR scanner found. Install Google Lens from the Play Store, sir.";
        }
    }

    private static boolean tryLaunch(Context ctx, String pkg) {
        try {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) return false;
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return true;
        } catch (Exception e) { return false; }
    }
}
