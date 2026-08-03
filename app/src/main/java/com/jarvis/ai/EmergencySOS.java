package com.jarvis.ai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * Emergency SOS — sends location + SMS to pre-set emergency contact.
 * Setup: "Set SOS contact to [name]" or via settings.
 * Trigger: "SOS", "Emergency", "Help me", "Call emergency"
 */
public class EmergencySOS {

    private static final String PREFS        = "sos_prefs";
    private static final String KEY_CONTACT  = "sos_contact";
    private static final String KEY_NUMBER   = "sos_number";
    private static final String KEY_MESSAGE  = "sos_message";

    public static boolean isSOSCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.equals("sos") || lower.equals("emergency") ||
               lower.contains("send sos") || lower.contains("emergency help") ||
               lower.contains("help me henry") || lower.contains("call 911") ||
               lower.contains("call police") || lower.contains("call ambulance") ||
               lower.contains("i need help") || lower.equals("mayday");
    }

    public static boolean isSetupCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.contains("sos") || lower.contains("emergency")) &&
               (lower.contains("set") || lower.contains("contact") ||
                lower.contains("number") || lower.contains("configure"));
    }

    public static String getSOSContact(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CONTACT, null);
    }

    public static String getSOSNumber(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NUMBER, null);
    }

    public static void setContact(Context ctx, String name, String number) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putString(KEY_CONTACT, name).putString(KEY_NUMBER, number).apply();
    }

    public static void setCustomMessage(Context ctx, String msg) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putString(KEY_MESSAGE, msg).apply();
    }

    /**
     * Triggers SOS: SMS + optionally opens dialler to emergency number.
     * Returns a status message for HENRY to speak.
     */
    public static String trigger(Context ctx) {
        String number  = getSOSNumber(ctx);
        String contact = getSOSContact(ctx);

        if (number == null) {
            return "[EMOTION:concerned] No emergency contact set, sir. Say 'Set SOS contact' to configure one.";
        }

        // Get location
        String locationText = "Location unknown.";
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            Location loc = null;
            try {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } catch (SecurityException ignored) {}
            if (loc != null) {
                locationText = String.format(Locale.US,
                    "Location: https://maps.google.com/?q=%.6f,%.6f",
                    loc.getLatitude(), loc.getLongitude());
            }
        }

        String customMsg = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                              .getString(KEY_MESSAGE, null);
        String smsBody = customMsg != null ? customMsg + "\n" + locationText
            : "EMERGENCY ALERT from H.E.N.R.Y AI assistant.\nI need immediate assistance.\n" + locationText;

        // Send SMS
        boolean smsSent = false;
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager sm = SmsManager.getDefault();
                sm.sendTextMessage(number, null, smsBody, null, null);
                smsSent = true;
            } catch (Exception e) {
                // SMS failed — try to open SMS app
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
                smsIntent.putExtra("sms_body", smsBody);
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { ctx.startActivity(smsIntent); } catch (Exception ignored) {}
            }
        }

        // Open dialler to emergency services
        Intent call = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
        call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { ctx.startActivity(call); } catch (Exception ignored) {}

        return "[EMOTION:concerned] SOS activated, sir. " +
            (smsSent ? "Emergency message sent to **" + contact + "**. " : "") +
            "Dialler opened. Stay calm — help is on the way.";
    }

    /** Parse "set SOS contact to John" and return contact name, or null */
    public static String parseContactName(String text) {
        String lower = text.toLowerCase(Locale.US);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "(?:set\\s+)?(?:sos|emergency)\\s+contact\\s+(?:to|as)?\\s+([\\w\\s]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }
}
