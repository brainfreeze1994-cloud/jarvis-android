package com.jarvis.ai;

public class Message {
    public static final int TYPE_USER      = 0;
    public static final int TYPE_JARVIS    = 1;
    public static final int TYPE_TYPING    = 2;
    public static final int TYPE_IMAGE     = 3;  // user-sent local image
    public static final int TYPE_URL_IMAGE = 4;  // AI-generated image from URL
    public static final int TYPE_FILE_CARD = 5;  // Generated document/file card

    public final int    type;
    public final String text;
    public final String imageUri;  // for TYPE_IMAGE (local uri)
    public final String imageUrl;  // for TYPE_URL_IMAGE (http url)
    public final String filePath;
    public final String fileMimeType;
    public final String fileTitle;
    public final String fileDetails;
    public final String fileBadge;
    public final String fileIcon;
    public final java.util.List<String> imageUris;

    public static String stripEmotion(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)\\[emotion:[^\\]]*\\]\\s*", "")
                   .replaceAll("(?i)\\[emotion[^\\]]*\\]\\s*", "")
                   .trim();
    }

    // Text message
    public Message(int type, String text) {
        this.type         = type;
        this.text         = stripEmotion(text);
        this.imageUri     = null;
        this.imageUrl     = null;
        this.filePath     = null;
        this.fileMimeType = null;
        this.fileTitle    = null;
        this.fileDetails  = null;
        this.fileBadge    = null;
        this.fileIcon     = null;
        this.imageUris    = java.util.Collections.emptyList();
    }

    // Local image message
    public Message(int type, String text, String imageUri) {
        this.type         = type;
        this.text         = stripEmotion(text);
        this.imageUri     = imageUri;
        this.imageUrl     = null;
        this.filePath     = null;
        this.fileMimeType = null;
        this.fileTitle    = null;
        this.fileDetails  = null;
        this.fileBadge    = null;
        this.fileIcon     = null;
        this.imageUris    = (imageUri != null) ? java.util.Collections.singletonList(imageUri) : java.util.Collections.emptyList();
    }

    // Multi-attachment image message (User message with images)
    public Message(int type, String text, java.util.List<String> imageUris) {
        this.type         = type;
        this.text         = stripEmotion(text);
        this.imageUri     = (imageUris != null && !imageUris.isEmpty()) ? imageUris.get(0) : null;
        this.imageUrl     = null;
        this.filePath     = null;
        this.fileMimeType = null;
        this.fileTitle    = null;
        this.fileDetails  = null;
        this.fileBadge    = null;
        this.fileIcon     = null;
        this.imageUris    = (imageUris != null) ? new java.util.ArrayList<>(imageUris) : java.util.Collections.emptyList();
    }

    // URL image message (AI generated)
    public Message(int type, String text, String imageUri, String imageUrl) {
        this.type         = type;
        this.text         = stripEmotion(text);
        this.imageUri     = imageUri;
        this.imageUrl     = imageUrl;
        this.filePath     = null;
        this.fileMimeType = null;
        this.fileTitle    = null;
        this.fileDetails  = null;
        this.fileBadge    = null;
        this.fileIcon     = null;
        this.imageUris    = (imageUri != null) ? java.util.Collections.singletonList(imageUri) : java.util.Collections.emptyList();
    }

    // File card message
    public Message(int type, String text, String filePath, String fileMimeType,
                   String fileTitle, String fileDetails, String fileBadge, String fileIcon) {
        this.type         = type;
        this.text         = stripEmotion(text);
        this.imageUri     = null;
        this.imageUrl     = null;
        this.filePath     = filePath;
        this.fileMimeType = fileMimeType;
        this.fileTitle    = stripEmotion(fileTitle);
        this.fileDetails  = stripEmotion(fileDetails);
        this.fileBadge    = stripEmotion(fileBadge);
        this.fileIcon     = fileIcon;
        this.imageUris    = java.util.Collections.emptyList();
    }
}
