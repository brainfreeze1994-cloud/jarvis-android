package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

/**
 * PackageTracker — Track packages by opening courier tracking pages
 * Supports DHL, FedEx, UPS, Aramex, Emirates Post, Noon, Amazon
 */
public class PackageTracker {

    public interface Callback {
        void onResult(String message, String url);
    }

    private static final Handler H = new Handler(Looper.getMainLooper());

    public static boolean isPackageQuery(String input) {
        String t = input.toLowerCase();
        return t.contains("package") || t.contains("parcel") || t.contains("shipment")
            || t.contains("tracking") || t.contains("track") || t.contains("delivery")
            || t.contains("dhl") || t.contains("fedex") || t.contains("ups")
            || t.contains("aramex") || t.contains("order status");
    }

    public static String extractTrackingNumber(String input) {
        // Common tracking number patterns
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\\b([A-Z0-9]{10,30})\\b").matcher(input.toUpperCase());
        while (m.find()) {
            String g = m.group(1);
            if (!g.matches("[A-Z]+") && !g.matches("\\d{4,5}")) return g;
        }
        return null;
    }

    public static String detectCourier(String input) {
        String t = input.toLowerCase();
        if (t.contains("dhl")) return "dhl";
        if (t.contains("fedex")) return "fedex";
        if (t.contains("ups")) return "ups";
        if (t.contains("aramex")) return "aramex";
        if (t.contains("emirates post") || t.contains("empost")) return "empost";
        if (t.contains("amazon")) return "amazon";
        if (t.contains("noon")) return "noon";
        if (t.contains("shein")) return "shein";
        return "universal";
    }

    public static void track(Context ctx, String trackingNum, String courier, Callback cb) {
        String url, name;
        switch (courier) {
            case "dhl":
                url  = "https://www.dhl.com/ae-en/home/tracking.html?tracking-id=" + trackingNum;
                name = "DHL"; break;
            case "fedex":
                url  = "https://www.fedex.com/fedextrack/?trknbr=" + trackingNum;
                name = "FedEx"; break;
            case "ups":
                url  = "https://www.ups.com/track?tracknum=" + trackingNum;
                name = "UPS"; break;
            case "aramex":
                url  = "https://www.aramex.com/ae/en/track/results?ShipmentNumber=" + trackingNum;
                name = "Aramex"; break;
            case "empost":
                url  = "https://emiratespost.ae/tracking?trackNumber=" + trackingNum;
                name = "Emirates Post"; break;
            case "amazon":
                url  = "https://track.amazon.ae/?trackingId=" + trackingNum;
                name = "Amazon"; break;
            default:
                url  = "https://parcelsapp.com/en/tracking/" + trackingNum;
                name = "Universal Tracker"; break;
        }

        final String finalUrl = url;
        final String finalName = name;
        H.post(() -> cb.onResult(
            "📦 Tracking " + trackingNum + " via " + finalName + "\nOpening live tracker now, sir.",
            finalUrl));
    }

    public static void openTracker(Context ctx, String url) {
        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
