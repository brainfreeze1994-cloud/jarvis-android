package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Smart Compose — AI drafts messages for WhatsApp, SMS, Email.
 * Voice: "draft a whatsapp to John saying I'll be late"
 *        "compose email to mom: happy birthday"
 *        "write sms to Sarah: running late"
 */
public class SmartCompose {

    public enum Channel { WHATSAPP, SMS, EMAIL, UNKNOWN }

    public static class ComposeRequest {
        public Channel channel;
        public String contactName;
        public String rawInstruction;
        public String subject; // for email
    }

    public interface Callback {
        void onDraftReady(String draft, ComposeRequest req, String contactNumber, String contactEmail);
        void onError(String reason);
    }

    // ── Parse command ─────────────────────────────────────────────────────────
    public static ComposeRequest parse(String text) {
        String lower = text.toLowerCase(Locale.US);

        // Patterns:
        // "draft/compose/write a whatsapp/sms/email/message to [name]: [content]"
        // "send whatsapp to [name] saying [content]"
        // "henry draft a message to [name]: [content]"
        Pattern p = Pattern.compile(
            "(?:draft|compose|write|send)\\s+(?:a\\s+)?(?:(whatsapp|sms|text|email|message|msg)\\s+)?(?:to\\s+)?([\\w\\s]+?)(?::\\s*|\\s+saying\\s+|\\s+that\\s+|\\s+about\\s+)(.+)",
            Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text.trim());
        if (!m.find()) return null;

        String channelStr = m.group(1) != null ? m.group(1).trim().toLowerCase() : "";
        String contactRaw = m.group(2).trim();
        String instruction = m.group(3).trim();

        // Clean contact (strip trailing "to", "a", etc.)
        contactRaw = contactRaw.replaceAll("(?i)\\s+(to|a|an)$", "").trim();

        ComposeRequest req = new ComposeRequest();
        req.contactName    = contactRaw;
        req.rawInstruction = instruction;

        if (channelStr.contains("whatsapp"))       req.channel = Channel.WHATSAPP;
        else if (channelStr.contains("email"))     req.channel = Channel.EMAIL;
        else if (channelStr.contains("sms") || channelStr.contains("text")) req.channel = Channel.SMS;
        else req.channel = Channel.WHATSAPP; // default

        return req;
    }

    public static boolean isComposeCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.startsWith("draft") || lower.startsWith("compose") ||
                lower.startsWith("write") ||
                lower.contains("draft a") || lower.contains("compose a") ||
                lower.contains("write a message") || lower.contains("write a whatsapp") ||
                lower.contains("draft message") || lower.contains("draft whatsapp") ||
                lower.contains("draft email") || lower.contains("draft sms")) &&
               (lower.contains(" to ") || lower.contains(":"));
    }

    // ── Generate AI draft ─────────────────────────────────────────────────────
    public static void generate(Context ctx, ComposeRequest req,
                                OkHttpClient httpClient, UserProfile profile,
                                Callback cb) {
        new Thread(() -> {
            try {
                String channelLabel = req.channel == Channel.EMAIL ? "email"
                                    : req.channel == Channel.SMS   ? "text message"
                                    : "WhatsApp message";
                String name = profile != null && !profile.name.isEmpty() ? profile.name : "sir";
                String systemPrompt = "You are H.E.N.R.Y, a smart assistant. " +
                    "Draft a natural, human-sounding " + channelLabel + " from " + name + " " +
                    "to " + req.contactName + ". Keep it concise and friendly. " +
                    "Output ONLY the message body — no subject line, no greeting like 'Hi [name]:', " +
                    "no explanation. Just the message content.";

                JSONArray msgs = new JSONArray();
                JSONObject sys = new JSONObject();
                sys.put("role", "user");
                sys.put("content", "Write a " + channelLabel + " to " + req.contactName +
                    " about: " + req.rawInstruction);
                msgs.put(sys);

                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "brief");
                body.put("systemOverride", systemPrompt);

                RequestBody rb = RequestBody.create(
                    body.toString(), MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                    .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                    .post(rb)
                    .addHeader("Content-Type", "application/json").build();

                try (Response resp = httpClient.newCall(request).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        cb.onError("Couldn't generate draft, sir.");
                        return;
                    }
                    String raw   = resp.body().string();
                    JSONObject j = new JSONObject(raw);
                    String draft = j.optString("reply", raw);
                    draft = draft.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();

                    // Look up contact
                    String number = ContactsHelper.findNumber(ctx, req.contactName);
                    String email  = ContactsHelper.findEmail(ctx, req.contactName);

                    final String finalDraft  = draft;
                    final String finalNumber = number;
                    final String finalEmail  = email;
                    cb.onDraftReady(finalDraft, req, finalNumber, finalEmail);
                }
            } catch (Exception e) {
                cb.onError("Error drafting message: " + e.getMessage());
            }
        }).start();
    }

    // ── Send via app ──────────────────────────────────────────────────────────
    public static void sendWhatsApp(Context ctx, String number, String message) {
        String url = "https://api.whatsapp.com/send?phone=" +
            number.replaceAll("[^\\d+]", "") + "&text=" +
            Uri.encode(message);
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    public static void sendSms(Context ctx, String number, String message) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
        i.putExtra("sms_body", message);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    public static void sendEmail(Context ctx, String email, String subject, String body) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
        i.putExtra(Intent.EXTRA_SUBJECT, subject != null ? subject : "");
        i.putExtra(Intent.EXTRA_TEXT, body);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }
}
