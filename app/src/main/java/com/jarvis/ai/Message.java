package com.jarvis.ai;

public class Message {
    public static final int TYPE_USER      = 0;
    public static final int TYPE_JARVIS    = 1;
    public static final int TYPE_TYPING    = 2;
    public static final int TYPE_IMAGE     = 3;  // user-sent local image
    public static final int TYPE_URL_IMAGE = 4;  // AI-generated image from URL

    public final int    type;
    public final String text;
    public final String imageUri;  // for TYPE_IMAGE (local uri)
    public final String imageUrl;  // for TYPE_URL_IMAGE (http url)

    public Message(int type, String text) {
        this.type = type; this.text = text;
        this.imageUri = null; this.imageUrl = null;
    }
    public Message(int type, String text, String imageUri) {
        this.type = type; this.text = text;
        this.imageUri = imageUri; this.imageUrl = null;
    }
    public Message(int type, String text, String imageUri, String imageUrl) {
        this.type = type; this.text = text;
        this.imageUri = imageUri; this.imageUrl = imageUrl;
    }
}
