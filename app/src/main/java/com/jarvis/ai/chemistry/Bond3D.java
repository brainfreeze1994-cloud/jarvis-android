package com.jarvis.ai.chemistry;

/**
 * Represents a chemical bond between two atoms in 3D and 2D space.
 */
public class Bond3D {
    public final int fromAtomIndex;
    public final int toAtomIndex;
    public final int bondOrder; // 1 = single, 2 = double, 3 = triple, 0 = ionic coordination
    public final float lengthAngstrom;
    public final String bondDescription; // e.g. "σ (Sigma) covalent", "π (Pi) + σ", "Ionic electrostatic"

    public Bond3D(int fromAtomIndex, int toAtomIndex, int bondOrder, float lengthAngstrom, String bondDescription) {
        this.fromAtomIndex = fromAtomIndex;
        this.toAtomIndex = toAtomIndex;
        this.bondOrder = bondOrder;
        this.lengthAngstrom = lengthAngstrom;
        this.bondDescription = bondDescription;
    }
}
