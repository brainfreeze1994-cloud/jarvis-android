package com.jarvis.ai.chemistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the complete scientific, geometric, and pedagogical data
 * for a verified chemical species in the HENRY Molecular Chemical Mixer.
 */
public class MolecularStructureData {

    // Identity
    public final String formula;
    public final String chemicalName;
    public final String commonName;
    public final String molecularWeight;
    public final String category; // e.g. "Polar Covalent Molecule", "Ionic Salt", "Wide-Bandgap Semiconductor"

    // VSEPR & Quantum Properties
    public final String molecularGeometry;   // e.g. "Bent (104.5°)", "Tetrahedral (109.5°)", "Linear (180°)"
    public final String electronGeometry;    // e.g. "Tetrahedral", "Trigonal Planar", "Linear"
    public final String hybridization;       // e.g. "sp³", "sp", "sp²", "sp³d", "Ionic"
    public final String bondAngle;           // e.g. "104.5°", "180°", "109.5°", "107.3°"
    public final String bondType;            // e.g. "Polar Covalent O-H (ΔEN = 1.24)"
    public final String polarity;            // e.g. "Polar (Net Dipole μ = 1.85 D)"
    public final float dipoleDebye;          // Numeric dipole moment in Debye (e.g. 1.85f for H2O, 0.0f for CO2)
    public final boolean isPolar;
    public final String intermolecularForces; // e.g. "Hydrogen Bonding & Dipole-Dipole"

    // Net Dipole 3D Vector (normalized direction)
    public final float netDipoleX;
    public final float netDipoleY;
    public final float netDipoleZ;

    // Structural Elements
    public final List<Atom3D> atoms = new ArrayList<>();
    public final List<Bond3D> bonds = new ArrayList<>();
    public final List<LonePair3D> lonePairs = new ArrayList<>();

    // Analysis & Formation Mechanism
    public final String connectionSummary;
    public final String geometryExplanation;
    public final String bondFormationReason;
    public final String polarityAnalysis;
    public final String advancedChemicalProperties;

    // Reaction & Synthesis Mechanism
    public final String balancedEquation;
    public final String reactantsSummary;
    public final String bondRearrangementSummary;
    public final String productSummary;
    public final String synthesisSafetyDisclaimer;

    // Educational Mode Pedagogical Insights
    public static class EducationalItem {
        public final String question;
        public final String answer;
        public EducationalItem(String q, String a) {
            this.question = q;
            this.answer = a;
        }
    }
    public final List<EducationalItem> educationalItems = new ArrayList<>();

    public MolecularStructureData(
            String formula, String chemicalName, String commonName, String molecularWeight, String category,
            String molecularGeometry, String electronGeometry, String hybridization, String bondAngle,
            String bondType, String polarity, float dipoleDebye, boolean isPolar, String intermolecularForces,
            float netDipoleX, float netDipoleY, float netDipoleZ,
            String connectionSummary, String geometryExplanation, String bondFormationReason,
            String polarityAnalysis, String advancedChemicalProperties,
            String balancedEquation, String reactantsSummary, String bondRearrangementSummary,
            String productSummary, String synthesisSafetyDisclaimer) {

        this.formula = formula;
        this.chemicalName = chemicalName;
        this.commonName = commonName;
        this.molecularWeight = molecularWeight;
        this.category = category;
        this.molecularGeometry = molecularGeometry;
        this.electronGeometry = electronGeometry;
        this.hybridization = hybridization;
        this.bondAngle = bondAngle;
        this.bondType = bondType;
        this.polarity = polarity;
        this.dipoleDebye = dipoleDebye;
        this.isPolar = isPolar;
        this.intermolecularForces = intermolecularForces;
        this.netDipoleX = netDipoleX;
        this.netDipoleY = netDipoleY;
        this.netDipoleZ = netDipoleZ;
        this.connectionSummary = connectionSummary;
        this.geometryExplanation = geometryExplanation;
        this.bondFormationReason = bondFormationReason;
        this.polarityAnalysis = polarityAnalysis;
        this.advancedChemicalProperties = advancedChemicalProperties;
        this.balancedEquation = balancedEquation;
        this.reactantsSummary = reactantsSummary;
        this.bondRearrangementSummary = bondRearrangementSummary;
        this.productSummary = productSummary;
        this.synthesisSafetyDisclaimer = synthesisSafetyDisclaimer;
    }

    public void addAtom(Atom3D atom) {
        atoms.add(atom);
    }

    public void addBond(Bond3D bond) {
        bonds.add(bond);
    }

    public void addLonePair(LonePair3D lp) {
        lonePairs.add(lp);
    }

    public void addEducationalItem(String q, String a) {
        educationalItems.add(new EducationalItem(q, a));
    }
}
