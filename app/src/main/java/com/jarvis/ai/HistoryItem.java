package com.jarvis.ai;

public class HistoryItem {
    public String role; // "user" or "model"
    public String text;
    public HistoryItem(String role, String text) { this.role = role; this.text = text; }
}
