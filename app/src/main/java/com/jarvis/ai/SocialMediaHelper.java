package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.util.*;

/**
 * SocialMediaHelper — Post to Instagram, X/Twitter, TikTok, LinkedIn
 * by voice or text via HENRY.
 * Uses Android share intents + AI caption generation.
 */
public class SocialMediaHelper {

    public interface Callback {
        void onCaption(String caption);
        void onPosted(String platform);
        void onError(String msg);
    }

    public static boolean isSocialMediaQuery(String input) {
        String t = input.toLowerCase();
        return t.contains("post") || t.contains("tweet") || t.contains("instagram")
            || t.contains("tiktok") || t.contains("linkedin") || t.contains("facebook")
            || t.contains("share") || t.contains("caption") || t.contains("story")
            || t.contains("reel") || t.contains("social media") || t.contains("x.com")
            || t.contains("twitter");
    }

    public static String detectPlatform(String input) {
        String t = input.toLowerCase();
        if (t.contains("instagram") || t.contains("insta") || t.contains("reel") || t.contains("story")) return "instagram";
        if (t.contains("twitter") || t.contains("tweet") || t.contains("x.com") || t.contains("on x")) return "twitter";
        if (t.contains("tiktok") || t.contains("tik tok")) return "tiktok";
        if (t.contains("linkedin") || t.contains("linked in")) return "linkedin";
        if (t.contains("facebook") || t.contains("fb")) return "facebook";
        if (t.contains("youtube") || t.contains("yt")) return "youtube";
        return "general";
    }

    public static String buildCaptionPrompt(String platform, String topic, String tone) {
        String platformGuide;
        switch (platform) {
            case "instagram":
                platformGuide = "Instagram caption: engaging opener, 3-5 sentences, 5-10 relevant hashtags, emoji-rich, call to action"; break;
            case "twitter":
                platformGuide = "Twitter/X post: punchy, max 280 characters, 1-2 hashtags, witty or insightful, no filler"; break;
            case "tiktok":
                platformGuide = "TikTok caption: short and catchy, trending hashtags (15+), hook in first line, Gen-Z friendly"; break;
            case "linkedin":
                platformGuide = "LinkedIn post: professional, value-driven, 3-5 paragraphs, 3-5 hashtags, storytelling format, end with question to audience"; break;
            case "facebook":
                platformGuide = "Facebook post: conversational, medium length, 2-3 hashtags, shareable, include a question"; break;
            default:
                platformGuide = "Social media caption: engaging, clear, with hashtags";
        }
        return "Write a " + platformGuide + " about: " + topic +
            (tone != null && !tone.isEmpty() ? "\n\nTone: " + tone : "") +
            "\n\nMake it authentic, attention-grabbing, and ready to post immediately.";
    }

    public static void shareToInstagram(Context ctx, android.net.Uri imageUri, String caption) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("image/*");
            i.setPackage("com.instagram.android");
            if (imageUri != null) i.putExtra(Intent.EXTRA_STREAM, imageUri);
            if (caption != null) i.putExtra(Intent.EXTRA_TEXT, caption);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(i);
        } catch (Exception e) {
            // Instagram not installed
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.instagram.android"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public static void shareToTwitter(Context ctx, String text) {
        try {
            // Try Twitter app first
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.setPackage("com.twitter.android");
            i.putExtra(Intent.EXTRA_TEXT, text);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            // Fallback to web
            String encoded = Uri.encode(text.substring(0, Math.min(280, text.length())));
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://twitter.com/intent/tweet?text=" + encoded))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public static void shareToLinkedIn(Context ctx, String text) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.setPackage("com.linkedin.android");
            i.putExtra(Intent.EXTRA_TEXT, text);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.linkedin.com/sharing/share-offsite/?url=&summary=" + Uri.encode(text)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public static void shareGeneral(Context ctx, String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(Intent.createChooser(i, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static void openTikTok(Context ctx) {
        Intent i = ctx.getPackageManager().getLaunchIntentForPackage("com.zhiliaoapp.musically");
        if (i == null) i = ctx.getPackageManager().getLaunchIntentForPackage("com.ss.android.ugc.trill");
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i); }
        else ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
