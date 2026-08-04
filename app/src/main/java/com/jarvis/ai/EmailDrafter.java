package com.jarvis.ai;

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
 * AI Email Drafter — draft professional emails by voice.
 * "Draft an email to my boss about being late"
 * "Write a resignation email"
 * "Compose an email requesting a meeting"
 */
public class EmailDrafter {

    public static boolean isEmailCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.contains("email") || lower.contains("e-mail")) &&
               (lower.contains("draft") || lower.contains("write") ||
                lower.contains("compose") || lower.contains("send") ||
                lower.contains("reply"));
    }

    public interface Callback {
        void onResult(String subject, String body, String recipient);
        void onError(String reason);
    }

    public static void draft(String userQuery, UserProfile profile, Callback cb) {
        new Thread(() -> {
            try {
                String lower = userQuery.toLowerCase(Locale.US);

                // Detect recipient
                String recipient = "";
                String[] toPatterns = {"to my ", "to ", "for "};
                for (String p : toPatterns) {
                    int idx = lower.indexOf(p);
                    if (idx >= 0) {
                        String after = userQuery.substring(idx + p.length()).trim();
                        String[] words = after.split("\\s+");
                        if (words.length > 0) { recipient = words[0]; break; }
                    }
                }

                // Detect tone
                String tone = "professional";
                if (lower.contains("friendly") || lower.contains("casual")) tone = "friendly";
                else if (lower.contains("formal")) tone = "formal and formal";
                else if (lower.contains("apolog")) tone = "apologetic and sincere";
                else if (lower.contains("urgent")) tone = "urgent and direct";

                String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "the sender";
                String systemPrompt = "You are H.E.N.R.Y, an expert email writer for " + name + ". " +
                    "Draft a " + tone + " email based on the request. " +
                    "Format your response EXACTLY as:\n" +
                    "SUBJECT: [subject line]\n\n[email body]\n\n" +
                    "Regards,\n" + name + "\n\n" +
                    "Keep it concise and appropriate for the tone. No extra commentary.";

                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user"); msg.put("content", userQuery);
                msgs.put(msg);
                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "balanced");
                body.put("systemOverride", systemPrompt);

                URL url = new URL("https://jarvis-ai-seven-dun.vercel.app/api/jarvis");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); conn.setDoOutput(true);
                conn.setConnectTimeout(15000); conn.setReadTimeout(25000);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) { os.write(body.toString().getBytes("UTF-8")); }
                InputStream is = conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[4096]; int r;
                while ((r = is.read(buf)) != -1) sb.append(new String(buf, 0, r, "UTF-8"));
                is.close();

                JSONObject j = new JSONObject(sb.toString());
                String draft = j.optString("reply", "").replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();

                // Parse subject
                String subject = "Email from HENRY";
                String emailBody = draft;
                if (draft.startsWith("SUBJECT:")) {
                    int nl = draft.indexOf("\n");
                    if (nl > 0) {
                        subject = draft.substring(8, nl).trim();
                        emailBody = draft.substring(nl).trim();
                    }
                }
                final String finalSubject = subject;
                final String finalBody = emailBody;
                final String finalRecipient = recipient;
                cb.onResult(finalSubject, finalBody, finalRecipient);
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Couldn't draft email, sir: " + e.getMessage());
            }
        }).start();
    }

    /** Opens Gmail/email app with pre-filled content */
    public static Intent buildEmailIntent(String to, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, to.isEmpty() ? new String[]{} : new String[]{to});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        return intent;
    }
}
