package com.jarvis.ai;

public class Message {
    public static final int TYPE_USER   = 0;
    public static final int TYPE_JARVIS = 1;
    public static final int TYPE_TYPING = 2;

    public final int    type;
    public final String text;

    public Message(int type, String text) {
        this.type = type;
        this.text = text;
    }
}
