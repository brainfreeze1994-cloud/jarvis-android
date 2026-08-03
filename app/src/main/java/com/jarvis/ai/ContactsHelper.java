package com.jarvis.ai;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public class ContactsHelper {

    public static String findNumber(Context ctx, String name) {
        String nameLower = name.toLowerCase().trim();
        try (Cursor c = ctx.getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            }, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (c == null) return null;
            while (c.moveToNext()) {
                String cName = c.getString(0);
                if (cName != null && cName.toLowerCase().contains(nameLower))
                    return c.getString(1);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Intent callIntent(String number) {
        return new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
    }

    public static Intent smsIntent(String number, String body) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
        i.putExtra("sms_body", body != null ? body : "");
        return i;
    }

    public static String parseContactCommand(String text) {
        String lower = text.toLowerCase().trim();
        if (lower.startsWith("call "))   return "CALL:" + text.substring(5).trim();
        if (lower.startsWith("ring "))   return "CALL:" + text.substring(5).trim();
        if (lower.startsWith("phone "))  return "CALL:" + text.substring(6).trim();
        if (lower.startsWith("text ")) {
            String rest = text.substring(5).trim();
            int sp = rest.indexOf(' ');
            if (sp > 0) return "SMS:" + rest.substring(0, sp) + ":" + rest.substring(sp + 1).trim();
            return "SMS:" + rest + ":";
        }
        if (lower.startsWith("message ")) {
            String rest = text.substring(8).trim();
            int sp = rest.indexOf(' ');
            if (sp > 0) return "SMS:" + rest.substring(0, sp) + ":" + rest.substring(sp + 1).trim();
            return "SMS:" + rest + ":";
        }
        return null;
    }
}
