package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * WhatsApp AI Helper — draft WhatsApp replies, open chats, send messages.
 * "Draft a WhatsApp reply to John saying I'll be late"
 * "Send WhatsApp to Mom: Happy Birthday"
 * "Write a professional WhatsApp reply to my boss"
 */
public class WhatsAppHelper {

    public static boolean isWhatsAppCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("whatsapp") || lower.contains("whats app") ||
               lower.contains("wa message") || lower.contains("wa reply") ||
               (lower.contains("draft") && lower.contains("message")) ||
               (lower.contains("write") && lower.contains("message") && lower.contains("to"));
    }

    public interface Callback {
        void onResult(String draftMessage, String contactName);
        void onError(String reason);
    }

    public static void draftReply(String userQuery, UserProfile profile, Callback cb) {
        new Thread(() -> {
            try {
                String lower = userQuery.toLowerCase(Locale.US);

                // Extract contact name
                String contact = "someone";
                String[] patterns = {"to ", "for ", "reply to ", "message "};
                for (String p : patterns) {
                    int idx = lower.indexOf(p);
                    if (idx >= 0) {
                        String after = userQuery.substring(idx + p.length()).trim();
                        // Take first word(s) before common words
                        String[] words = after.split("\\s+");
                        if (words.length > 0) {
                            contact = words[0];
                            // Grab 2 words if second isn't a keyword
                            if (words.length > 1 && !isKeyword(words[1].toLowerCase(Locale.US))) {
                                contact += " " + words[1];
                            }
                            break;
                        }
                    }
                }
                final String finalContact = contact;

                // Tone detection
                String tone = "friendly";
                if (lower.contains("professional") || lower.contains("boss") || lower.contains("work")) tone = "professional";
                else if (lower.contains("formal")) tone = "formal";
                else if (lower.contains("casual") || lower.contains("friend")) tone = "casual";
                else if (lower.contains("apolog")) tone = "apologetic";

                String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "the user";
                String systemPrompt = "You are H.E.N.R.Y drafting a WhatsApp message for " + name + ". " +
                    "Tone: " + tone + ". Keep it concise (2-4 sentences max). " +
                    "Write ONLY the message text — no quotes, no preamble, no explanation. " +
                    "Sound natural for WhatsApp (not too formal, no 'Dear X').";

                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", userQuery);
                msgs.put(msg);
                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "brief");
                body.put("systemOverride", systemPrompt);

                URL url = new URL("https://jarvis-ai-seven-dun.vercel.app/api/jarvis");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); conn.setDoOutput(true);
                conn.setConnectTimeout(15000); conn.setReadTimeout(20000);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) { os.write(body.toString().getBytes("UTF-8")); }
                InputStream is = conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[4096]; int r;
                while ((r = is.read(buf)) != -1) sb.append(new String(buf, 0, r, "UTF-8"));
                is.close();
                JSONObject j = new JSONObject(sb.toString());
                String draft = j.optString("reply", "").replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                cb.onResult(draft, finalContact);
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Couldn't draft message, sir: " + e.getMessage());
            }
        }).start();
    }

    /** Opens WhatsApp with pre-filled text */
    public static Intent buildWhatsAppIntent(Context ctx, String phoneOrName, String message) {
        try {
            // Try deep link with phone number
            Uri uri = Uri.parse("https://wa.me/?text=" + Uri.encode(message));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.whatsapp");
            return intent;
        } catch (Exception e) { return null; }
    }

    /** Open WhatsApp to a specific number */
    public static Intent openWhatsAppToNumber(String phone, String message) {
        try {
            String clean = phone.replaceAll("[^0-9+]", "");
            Uri uri = Uri.parse("https://wa.me/" + clean + "?text=" + Uri.encode(message));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.whatsapp");
            return intent;
        } catch (Exception e) { return null; }
    }

    private static boolean isKeyword(String word) {
        return word.equals("saying") || word.equals("that") || word.equals("about") ||
               word.equals("to") || word.equals("and") || word.equals("the") ||
               word.equals("a") || word.equals("an") || word.equals("i");
    }
}
