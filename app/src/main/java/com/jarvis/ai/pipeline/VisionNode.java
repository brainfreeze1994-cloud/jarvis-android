package com.jarvis.ai.pipeline;

import android.graphics.Color;

/**
 * Represents an extracted visual feature or topological graph vertex
 * in the HENRY Computer Vision Pipeline.
 */
public class VisionNode {
    public final int id;
    public float x; // Relative [0.0, 1.0] coordinate within image/canvas space
    public float y; // Relative [0.0, 1.0] coordinate within image/canvas space
    public String label;
    public String type; // "hub", "waypoint", "terminal", "junction", "cluster"
    public float intensity; // Visual feature strength / pixel luminance
    public int customColor;

    public VisionNode(int id, float x, float y, String label, String type, float intensity) {
        this.id = id;
        this.x = Math.max(0.02f, Math.min(0.98f, x));
        this.y = Math.max(0.02f, Math.min(0.98f, y));
        this.label = label != null ? label : "N" + id;
        this.type = type != null ? type : "waypoint";
        this.intensity = intensity;
        this.customColor = 0xFF00D4FF;
    }

    public VisionNode(int id, float x, float y, String label) {
        this(id, x, y, label, "waypoint", 1.0f);
    }

    public float distanceTo(VisionNode other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
