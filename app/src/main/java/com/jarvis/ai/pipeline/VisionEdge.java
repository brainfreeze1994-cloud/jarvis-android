package com.jarvis.ai.pipeline;

/**
 * Represents a connection, optical line-of-sight, circuit trace, or pathway
 * between two VisionNodes in the vision graph.
 */
public class VisionEdge {
    public final int fromId;
    public final int toId;
    public final float weight; // Euclidean distance, resistance, or transit cost
    public final boolean directed;

    public VisionEdge(int fromId, int toId, float weight, boolean directed) {
        this.fromId = fromId;
        this.toId = toId;
        this.weight = weight;
        this.directed = directed;
    }

    public VisionEdge(int fromId, int toId, float weight) {
        this(fromId, toId, weight, false);
    }
}
