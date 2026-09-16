package com.jarvis.ai;

/**
 * H.E.N.R.Y. — ITEM / PRODUCT IDENTIFICATION ENGINE
 *
 * Builds the vision prompt used when the user attaches one or more photos and asks
 * HENRY to identify/describe what's in them (e.g. "what is this?", "identify this item",
 * "what phone is this?"). This routes through the exact same real vision-analysis call
 * already used for general image analysis and the Kiss/Marry/Date game — this class only
 * supplies the structured prompt, it does not make its own network call.
 *
 * Deliberately scoped to OBJECTS/PRODUCTS, not people: identifying a specific individual
 * from a photo is a separate, much more sensitive capability that HENRY does not perform
 * regardless of entry point. Recognizing that a photographed object is, say, an iPhone 15
 * Pro carries none of that risk — it's the same category of task as Google Lens or a
 * store's visual product search.
 */
public final class HenryItemSearchEngine {

    private HenryItemSearchEngine() {}

    /**
     * Builds a structured prompt requesting one identification + description per photo,
     * in the order the photos were attached. Requiring a consistent "Photo N: ..." format
     * keeps multi-item results easy to read even when several photos are attached at once.
     */
    public static String buildItemSearchPrompt(int photoCount) {
        int n = Math.max(1, photoCount);
        StringBuilder template = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            template.append("Photo ").append(i).append(": <item name/brand/model> — <description>\n");
        }
        return "Identify the main item or product shown in " + (n == 1 ? "this photo" : ("each of these " + n + " photos")) +
                ", in the exact order they were sent.\n\n" +
                "For each photo, give:\n" +
                "1. Your best identification of the specific item — brand and model where visually " +
                "identifiable (e.g. \"Apple iPhone 15 Pro\", \"Nike Air Force 1\"), or the general " +
                "category if the exact model isn't determinable from the image (e.g. \"wireless over-ear " +
                "headphones\").\n" +
                "2. A concise, useful description: category, notable visible features (color, material, " +
                "design details, condition), and — only if you're confident about the era/model — roughly " +
                "when it was released or its general price bracket. Do not invent specific specs or exact " +
                "prices you aren't confident about; say \"not certain of the exact model\" rather than " +
                "guessing precisely.\n\n" +
                "This is about the OBJECT only — do not attempt to identify any person visible in the photo.\n\n" +
                "Reply with exactly " + n + " " + (n == 1 ? "entry" : "entries") + " in this exact format and " +
                "nothing else before or after:\n" + template +
                "Keep each description to 1–3 sentences.";
    }
}
