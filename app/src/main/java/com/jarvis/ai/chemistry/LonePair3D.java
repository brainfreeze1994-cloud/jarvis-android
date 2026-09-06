package com.jarvis.ai.chemistry;

/**
 * Represents a non-bonding valence lone pair of electrons associated with an atom.
 * Used for Lewis structure dot-pair rendering and 3D VSEPR electron domain lobes.
 */
public class LonePair3D {
    public final int atomIndex;
    public final float angleDeg2D; // Angle in degrees in 2D projection (0 = East, 90 = South, 180 = West, 270 = North)
    public final float offsetX3D;  // 3D vector offset for VSEPR electron cloud
    public final float offsetY3D;
    public final float offsetZ3D;

    public LonePair3D(int atomIndex, float angleDeg2D, float offsetX3D, float offsetY3D, float offsetZ3D) {
        this.atomIndex = atomIndex;
        this.angleDeg2D = angleDeg2D;
        this.offsetX3D = offsetX3D;
        this.offsetY3D = offsetY3D;
        this.offsetZ3D = offsetZ3D;
    }
}
