package com.jarvis.ai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;

import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * Location Share — sends current location as a Google Maps link via WhatsApp/SMS.
 * "Share my location with Mom"
 * "Send my location to John"
 * "WhatsApp my location to Sarah"
 */
public class LocationShare {

    public static boolean isShareCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.contains("share") || lower.contains("send")) &&
               lower.contains("location") &&
               (lower.contains(" to ") || lower.contains(" with "));
    }

    public static String parseContact(String text) {
        return text
            .replaceAll("(?i)(share|send)\\s+(my\\s+)?location\\s+(to|with)\\s+", "")
            .replaceAll("(?i)whatsapp\\s+(my\\s+)?location\\s+(to|with)\\s+", "")
            .replaceAll("(?i)via\\s+(whatsapp|sms|text).*$", "")
            .trim();
    }

    public static boolean isWhatsAppPreferred(String text) {
        return text.toLowerCase(Locale.US).contains("whatsapp");
    }

    /** Gets current location and builds a share message. Returns {mapsLink, message} or null */
    public static String[] buildSharePayload(Context ctx) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return null;

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        Location loc = null;
        try {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignored) {}

        if (loc == null) return null;

        String mapsLink = String.format(Locale.US,
            "https://maps.google.com/?q=%.6f,%.6f", loc.getLatitude(), loc.getLongitude());
        String message = "Here's my current location:\n" + mapsLink;
        return new String[]{mapsLink, message};
    }

    public static String share(Context ctx, String contactName, boolean viwhatsapp) {
        String[] payload = buildSharePayload(ctx);
        if (payload == null)
            return "[EMOTION:concerned] Can't get your location right now, sir. Enable GPS and try again.";

        String message = payload[1];
        String number  = ContactsHelper.findNumber(ctx, contactName);

        if (number == null)
            return "[EMOTION:concerned] Couldn't find **" + contactName + "** in your contacts, sir.";

        if (viwhatsapp) {
            String waUrl = "https://api.whatsapp.com/send?phone=" +
                number.replaceAll("[^\\d+]", "") + "&text=" + Uri.encode(message);
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(waUrl));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { ctx.startActivity(i); }
            catch (Exception e) { return openSmsShare(ctx, number, message, contactName); }
            return "[EMOTION:warm] Location sent to **" + contactName + "** via WhatsApp, sir.";
        } else {
            return openSmsShare(ctx, number, message, contactName);
        }
    }

    private static String openSmsShare(Context ctx, String number, String message, String contact) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
        i.putExtra("sms_body", message);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { ctx.startActivity(i); }
        catch (Exception ignored) {}
        return "[EMOTION:warm] Opening SMS to **" + contact + "** with your location, sir.";
    }

    // Generic share (share sheet)
    public static void shareViaSheet(Context ctx) {
        String[] payload = buildSharePayload(ctx);
        String message = payload != null ? payload[1] : "Location unavailable.";
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, message);
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(Intent.createChooser(share, "Share location via…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
