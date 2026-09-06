package com.jarvis.ai.chemistry;

import android.graphics.Color;

/**
 * Represents an individual atom in 3D Euclidean coordinate space (coordinates in Ångströms).
 * Encapsulates physical, quantum, and stoichiometric attributes.
 */
public class Atom3D {
    public final String symbol;
    public final String name;
    public final int atomicNumber;
    public final double atomicMass;
    public final String electronConfig;
    public final int valenceElectrons;

    // 3D position in Ångströms (relative to molecule center)
    public float x;
    public float y;
    public float z;

    // 2D planar position (for 2D and Lewis projections)
    public float x2d;
    public float y2d;

    // Visual attributes
    public final float radiusPm; // picometers scaled for ball-and-stick
    public final int color;

    // Chemical role & charge attributes
    public final String roleInMolecule; // e.g., "Central Atom", "Ligand / Terminal", "Counter-ion"
    public final int formalCharge;      // e.g. 0, +1, -1
    public final float partialCharge;   // e.g. +0.4 for H in H2O, -0.8 for O

    public Atom3D(String symbol, String name, int atomicNumber, double atomicMass,
                  String electronConfig, int valenceElectrons,
                  float x, float y, float z, float x2d, float y2d,
                  float radiusPm, int color,
                  String roleInMolecule, int formalCharge, float partialCharge) {
        this.symbol = symbol;
        this.name = name;
        this.atomicNumber = atomicNumber;
        this.atomicMass = atomicMass;
        this.electronConfig = electronConfig;
        this.valenceElectrons = valenceElectrons;
        this.x = x;
        this.y = y;
        this.z = z;
        this.x2d = x2d;
        this.y2d = y2d;
        this.radiusPm = radiusPm;
        this.color = color;
        this.roleInMolecule = roleInMolecule;
        this.formalCharge = formalCharge;
        this.partialCharge = partialCharge;
    }

    /**
     * Standard CPK (Corey-Pauling-Koltun) color mapping.
     */
    public static int getCpkColor(String symbol) {
        switch (symbol.toUpperCase()) {
            case "H":  return 0xFFFFFFFF; // White
            case "C":  return 0xFF3E434D; // Dark Slate / Carbon
            case "N":  return 0xFF2979FF; // Blue
            case "O":  return 0xFFFF3D00; // Bright Red
            case "F":  return 0xFF00E676; // Light Green
            case "CL": return 0xFF76FF03; // Lime Green
            case "BR": return 0xFFA52A2A; // Brown / Deep Red
            case "I":  return 0xFF9C27B0; // Violet
            case "S":  return 0xFFFFEA00; // Yellow
            case "P":  return 0xFFFF9100; // Orange
            case "NA": return 0xFF7C4DFF; // Purple
            case "K":  return 0xFFB388FF; // Light Violet
            case "CA": return 0xFF00B0FF; // Cyan
            case "FE": return 0xFFFF6D00; // Rust Orange
            case "CU": return 0xFFFF8A65; // Copper Bronze
            case "SI": return 0xFF90A4AE; // Metallic Gray
            case "B":  return 0xFFFFAB91; // Salmon
            case "TI": return 0xFF80DEEA; // Sky Blue
            case "AU": return 0xFFFFD700; // Gold
            case "NI": return 0xFF4DB6AC; // Teal Metallic
            case "GA": return 0xFFCE93D8; // Soft Purple
            case "CO": return 0xFF1565C0; // Cobalt Blue
            case "LI": return 0xFFE040FB; // Magenta
            default:   return 0xFF80CBC4; // Default Teal
        }
    }
}
