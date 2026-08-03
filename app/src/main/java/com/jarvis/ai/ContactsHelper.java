package com.jarvis.ai;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SmsManager;

public class ContactsHelper {

    /** Find best-matching phone number for a contact name. Returns null if not found. */
    public static String findNumber(Context ctx, String name) {
        ContentResolver cr = ctx.getContentResolver();
        String nameLower = name.toLowerCase().trim();
        try (Cursor c = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            }, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (c == null) return null;
            while (c.moveToNext()) {
                String cName = c.getString(0);
                if (cName != null && cName.toLowerCase().contains(nameLower)) {
                    return c.getString(1);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Returns an Intent to call the given number. Caller must check CALL_PHONE permission. */
    public static Intent callIntent(String number) {
        return new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
    }

    /** Returns an Intent to open SMS composer for the given number. */
    public static Intent smsIntent(String number, String body) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
        i.putExtra("sms_body", body != null ? body : "");
        return i;
    }

    /**
     * Parses "call Mom" / "text Sarah" / "message John hello how are you".
     * Returns action string for MainActivity to execute, or null if not a contacts command.
     * Format of return: "CALL:<name>" or "SMS:<name>:<optional message>"
     */
    /** Find email address for a contact name. Returns null if not found. */
    public static String findEmail(Context ctx, String name) {
        ContentResolver cr = ctx.getContentResolver();
        String nameLower = name.toLowerCase().trim();
        try (Cursor c = cr.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            }, null, null,
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME + " ASC")) {
            if (c == null) return null;
            while (c.moveToNext()) {
                String cName = c.getString(0);
                if (cName != null && cName.toLowerCase().contains(nameLower)) {
                    return c.getString(1);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String parseContactCommand(String text) {
        String lower = text.toLowerCase().trim();
        if (lower.startsWith("call ")) return "CALL:" + text.substring(5).trim();
        if (lower.startsWith("ring ")) return "CALL:" + text.substring(5).trim();
        if (lower.startsWith("phone ")) return "CALL:" + text.substring(6).trim();
        if (lower.startsWith("text ")) {
            String rest = text.substring(5).trim();
            // "text Sarah hello" → name=Sarah, msg=hello
            int space = rest.indexOf(' ');
            if (space > 0) return "SMS:" + rest.substring(0, space) + ":" + rest.substring(space + 1).trim();
            return "SMS:" + rest + ":";
        }
        if (lower.startsWith("message ") || lower.startsWith("msg ")) {
            int off = lower.startsWith("msg ") ? 4 : 8;
            String rest = text.substring(off).trim();
            int space = rest.indexOf(' ');
            if (space > 0) return "SMS:" + rest.substring(0, space) + ":" + rest.substring(space + 1).trim();
            return "SMS:" + rest + ":";
        }
        if (lower.startsWith("send sms to ")) return "SMS:" + text.substring(12).trim() + ":";
        return null;
    }
}
