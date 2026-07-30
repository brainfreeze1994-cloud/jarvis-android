package com.jarvis.ai;

public class Message {
    public static final int TYPE_USER   = 0;
    public static final int TYPE_JARVIS = 1;
    public static final int TYPE_TYPING = 2;
    public static final int TYPE_IMAGE  = 3;

    public final int    type;
    public final String text;
    public final String imageUri;

    public Message(int type, String text) {
        this.type     = type;
        this.text     = text;
        this.imageUri = null;
    }

    public Message(int type, String text, String imageUri) {
        this.type     = type;
        this.text     = text;
        this.imageUri = imageUri;
    }
