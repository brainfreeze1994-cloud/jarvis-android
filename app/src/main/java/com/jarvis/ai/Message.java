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

    // Text message
    public Message(int type, String text) {
        this.type         = type;
        this.text         = text;
        this.imageUri     = null;
        this.imageUrl     = null;
        this.filePath     = null;
        this.fileMimeType = null;
        this.fileTitle    = null;
        this.fileDetails  = null;
        this.fileBadge    = null;
        this.fileIcon     = null;
    }

    // Local image message
    public Message(int type, String text, String imageUri) {
        this.type         = type;
        this.text         = text;
        this.imageUri     = imageUri;
        this.imageUrl     = null;
        this.filePath     = null;
        this.fileMimeType = null;
        this.fileTitle    = null;
        this.fileDetails  = null;
        this.fileBadge    = null;
        this.fileIcon     = null;
    }

    // URL image message (AI generated)
    public Message(int type, String text, String imageUri, String imageUrl) {
        this.type         = type;
        this.text         = text;
        this.imageUri     = imageUri;
        this.imageUrl     = imageUrl;
        this.filePath     = null;
        this.fileMimeType = null;
        this.fileTitle    = null;
        this.fileDetails  = null;
        this.fileBadge    = null;
        this.fileIcon     = null;
    }

    // File card message
    public Message(int type, String text, String filePath, String fileMimeType,
                   String fileTitle, String fileDetails, String fileBadge, String fileIcon) {
        this.type         = type;
        this.text         = text;
        this.imageUri     = null;
        this.imageUrl     = null;
        this.filePath     = filePath;
        this.fileMimeType = fileMimeType;
        this.fileTitle    = fileTitle;
        this.fileDetails  = fileDetails;
        this.fileBadge    = fileBadge;
        this.fileIcon     = fileIcon;
    }
}
