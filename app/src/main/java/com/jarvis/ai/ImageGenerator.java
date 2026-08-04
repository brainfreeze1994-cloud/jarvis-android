package com.jarvis.ai;

import java.net.URLEncoder;
import java.util.Locale;

/**
 * AI Image Generation via Pollinations.ai Flux — free, no API key.
 * "Draw me a sunset", "Generate image of a dragon", "Create a picture of..."
 */
public class ImageGenerator {

    private static final String BASE_URL = "https://image.pollinations.ai/prompt/";

    public static boolean isImageCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("draw ") || lower.startsWith("paint ") ||
               lower.startsWith("generate image") || lower.startsWith("create image") ||
               lower.startsWith("show me a picture") || lower.startsWith("show me an image") ||
               lower.contains("draw me a") || lower.contains("draw me an") ||
               lower.contains("generate a picture") || lower.contains("generate an image") ||
               lower.contains("create a picture") || lower.contains("create an image") ||
               lower.contains("make an image") || lower.contains("make a picture") ||
               lower.contains("show picture of") || lower.contains("show image of") ||
               lower.contains("illustrate ") || lower.contains("visualize ") ||
               lower.contains("visualise ");
    }

    public static String extractPrompt(String text) {
        String lower = text.toLowerCase(Locale.US);
        String[] prefixes = {
            "draw me a ", "draw me an ", "draw a ", "draw an ", "draw ",
            "generate image of ", "generate an image of ", "generate a picture of ",
            "create image of ", "create an image of ", "create a picture of ",
            "show me a picture of ", "show me an image of ",
            "show picture of ", "show image of ",
            "paint a ", "paint an ", "paint ",
            "make an image of ", "make a picture of ",
            "illustrate ", "visualize ", "visualise "
        };
        for (String p : prefixes) {
            if (lower.startsWith(p)) return text.substring(p.length()).trim();
        }
        for (String p : prefixes) {
            int idx = lower.indexOf(p);
            if (idx >= 0) return text.substring(idx + p.length()).trim();
        }
        return text.replaceAll("(?i)^(draw|generate|create|show|paint|make|illustrate|visualize|visualise)\\s*(me\\s*)?(a\\s*|an\\s*)?", "").trim();
    }

    public static String buildImageUrl(String prompt) {
        try {
            String clean = prompt.replaceAll("\\s+", " ").trim();
            if (clean.length() > 250) clean = clean.substring(0, 250);
            int seed = (int) (Math.random() * 999999);
            return BASE_URL + URLEncoder.encode(clean, "UTF-8") +
                "?model=flux&width=512&height=512&nologo=true&enhance=true&seed=" + seed;
        } catch (Exception e) {
            return BASE_URL + "beautiful+artwork?model=flux&width=1024&height=1024&nologo=true";
        }
    }
}
