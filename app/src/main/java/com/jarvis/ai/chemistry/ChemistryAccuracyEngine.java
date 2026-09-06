package com.jarvis.ai.chemistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Scientific Accuracy Engine for the HENRY Molecular Chemical Mixer.
 * Stores verified chemical species with quantum, geometric, and topological attributes.
 * Validates elemental combinations, resolves multi-compound synthesis paths,
 * and provides scientific diagnostics when no verified compound can form.
 */
public class ChemistryAccuracyEngine {

    private static final Map<String, MolecularStructureData> COMPOUND_REGISTRY = new HashMap<>();
    public static final List<MolecularStructureData> ALL_COMPOUNDS = new ArrayList<>();

    public static List<MolecularStructureData> getAllCompounds() {
        return Collections.unmodifiableList(ALL_COMPOUNDS);
    }

    static {
        registerAllCompounds();
    }

    private static void register(String key, MolecularStructureData data) {
        COMPOUND_REGISTRY.put(key.toUpperCase(Locale.US), data);
        if (!ALL_COMPOUNDS.contains(data)) {
            ALL_COMPOUNDS.add(data);
        }
    }

    public static MolecularStructureData getCompound(String formulaOrKey) {
        if (formulaOrKey == null) return null;
        return COMPOUND_REGISTRY.get(formulaOrKey.toUpperCase(Locale.US));
    }

    public static MolecularStructureData findCompound(String formula, String name) {
        if (formula != null) {
            String clean = formula.replace("₂", "2").replace("₃", "3").replace("₄", "4").replace("₅", "5").replace("₆", "6").replace("₈", "8");
            MolecularStructureData m = getCompound(formula);
            if (m != null) return m;
            m = getCompound(clean);
            if (m != null) return m;
        }
        if (name != null) {
            MolecularStructureData m = getCompound(name);
            if (m != null) return m;
            String[] tokens = name.split("[,/\\s]+");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    m = getCompound(token);
                    if (m != null) return m;
                }
            }
        }
        return null;
    }

    public static String getUnverifiedExplanation(List<String> symbols) {
        return explainUnverifiedCombination(symbols);
    }

    public static List<MolecularStructureData> getAllCompounds() {
        return Collections.unmodifiableList(ALL_COMPOUNDS);
    }

    /**
     * Resolves all verified compounds that can be formed from the user-selected elements.
     */
    public static List<MolecularStructureData> findPossibleCompounds(List<String> symbols) {
        List<MolecularStructureData> matches = new ArrayList<>();
        if (symbols == null || symbols.isEmpty()) return matches;

        Set<String> selectedSet = new HashSet<>();
        for (String s : symbols) selectedSet.add(s.trim().toUpperCase(Locale.US));

        for (MolecularStructureData data : ALL_COMPOUNDS) {
            Set<String> compSymbols = getElementsInCompound(data);
            // Check if all elements in the compound are present in the user's selection
            if (selectedSet.containsAll(compSymbols)) {
                matches.add(data);
            }
        }
        return matches;
    }

    private static Set<String> getElementsInCompound(MolecularStructureData data) {
        Set<String> set = new HashSet<>();
        for (Atom3D atom : data.atoms) {
            set.add(atom.symbol.toUpperCase(Locale.US));
        }
        return set;
    }

    /**
     * Returns a scientific explanation if a combination cannot form a verified compound under standard conditions.
     */
    public static String explainUnverifiedCombination(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return "Please introduce 2 or more elemental reactants into the reaction vessel.";
        }
        if (symbols.size() == 1) {
            return "Single elemental reactant detected. Add one or more complementary elements to initiate a molecular synthesis pathway.";
        }

        Set<String> upper = new HashSet<>();
        for (String s : symbols) upper.add(s.toUpperCase(Locale.US));

        // 1. Noble Gas Check
        if (upper.contains("HE") || upper.contains("NE") || upper.contains("AR")) {
            return "NO VERIFIED COMPOUND: Noble gases (Helium, Neon, Argon) possess complete, energetically stable valence electron octets (1s² or ns²np⁶). They exhibit virtually zero electron affinity and exceptionally high ionization energies, precluding spontaneous chemical bond formation under ambient laboratory conditions.";
        }
        if (upper.contains("KR") && !upper.contains("F")) {
            return "NO VERIFIED COMPOUND: Krypton only forms stable covalent bonds with the most electronegative halogens (such as Fluorine in KrF₂) under extreme cryogenic, photochemical, or electric discharge conditions.";
        }

        // 2. Electropositive Metal + Metal (Non-intermetallic)
        boolean hasAlkali1 = upper.contains("NA") || upper.contains("K") || upper.contains("LI") || upper.contains("RB") || upper.contains("CS");
        boolean hasAlkali2 = upper.contains("CA") || upper.contains("MG") || upper.contains("BA") || upper.contains("SR");
        if (hasAlkali1 && hasAlkali2 && symbols.size() == 2) {
            return "NO VERIFIED COMPOUND: Combining two electropositive s-block metals (e.g., Alkali and Alkaline Earth metals) does not yield discrete chemical molecules or stable stoichiometric compounds due to identical low electronegativity and lack of directional covalent or ionic bonding.";
        }

        return "NO VERIFIED COMPOUND: The selected elemental combination does not correspond to a recognized stable stoichiometric compound under ambient conditions. Formation may require specialized high-temperature vacuum sintering, plasma-arc ionization, or high-pressure anvil conditions.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRY INITIALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    private static void registerAllCompounds() {

        // ─────────────────────────────────────────────────────────────────────
        // 1. WATER (H2O) - Bent 104.5°
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "H₂O", "Dihydrogen Monoxide", "Water / Universal Solvent", "18.015 g/mol", "Polar Covalent Molecule",
                "Bent (104.5°)", "Tetrahedral", "sp³", "104.5°",
                "Polar Covalent O-H (ΔEN = 1.24)", "Polar (Net Dipole μ = 1.85 D)", 1.85f, true,
                "Hydrogen Bonding & Dipole-Dipole Interaction",
                0.0f, 1.0f, 0.0f, // Net dipole points towards oxygen along +y
                "Two hydrogen atoms single-bonded to a central oxygen atom with two non-bonding lone pairs.",
                "VSEPR Repulsion: Oxygen has 4 electron domains (2 bonding pairs + 2 lone pairs). Lone pair-lone pair repulsion compresses the ideal 109.5° tetrahedral angle down to 104.5°, creating a bent geometry.",
                "Oxygen (EN = 3.44) strongly attracts valence electrons from Hydrogen (EN = 2.20), sharing an electron pair with each H atom to complete Oxygen's octet (8e⁻) and Hydrogen's duet (2e⁻).",
                "Asymmetric bent shape prevents bond dipole moments from canceling. Both O-H bond dipoles add constructively toward the electronegative oxygen apex, producing a substantial net dipole of 1.85 Debye.",
                "High dielectric constant (78.4 at 25°C), high specific heat capacity (4.184 J/g·K), anomalous density maximum at 4°C, universal dissolution of ionic salts.",
                "2 H₂ (g) + O₂ (g) → 2 H₂O (l)  [ΔH° = -571.6 kJ/mol]",
                "Hydrogen gas (H₂) and Oxygen gas (O₂) in a 2:1 stoichiometric ratio.",
                "Homolytic cleavage of H-H σ-bonds and O=O double bonds, followed by exothermic formation of four new O-H polar σ-bonds.",
                "Liquid water (H₂O) condensation with substantial heat release.",
                "THEORETICAL REACTION vs REAL-WORLD SYNTHESIS: Direct stoichiometric gas mixtures of H₂ and O₂ are highly explosive. In industrial and laboratory settings, water is formed via controlled catalytic fuel cells, neutralization reactions, or hydrocarbon combustion."
            );

            // Oxygen (Central)
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 66.0f, Atom3D.getCpkColor("O"),
                "Central Atom", 0, -0.82f));

            // Hydrogen 1
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                0.757f, 0.586f, 0.0f, 1.2f, 0.9f, 31.0f, Atom3D.getCpkColor("H"),
                "Terminal Ligand", 0, +0.41f));

            // Hydrogen 2
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -0.757f, 0.586f, 0.0f, -1.2f, 0.9f, 31.0f, Atom3D.getCpkColor("H"),
                "Terminal Ligand", 0, +0.41f));

            m.addBond(new Bond3D(0, 1, 1, 0.96f, "Polar Covalent O-H σ-bond"));
            m.addBond(new Bond3D(0, 2, 1, 0.96f, "Polar Covalent O-H σ-bond"));

            // 2 Lone pairs on oxygen
            m.addLonePair(new LonePair3D(0, 270.0f, 0.0f, -0.6f, 0.6f));
            m.addLonePair(new LonePair3D(0, 90.0f, 0.0f, -0.6f, -0.6f));

            m.addEducationalItem("Why does H₂O have a bent shape?",
                "Oxygen has 4 electron domains (2 single bonds to H and 2 unshared lone pairs). According to VSEPR theory, these 4 domains adopt a tetrahedral electron geometry. Because lone pairs occupy more angular space and exert stronger electrostatic repulsion than bonding pairs, they push the H-O-H bond angle inward from 109.5° to 104.5°.");
            m.addEducationalItem("Why is water polar?",
                "Oxygen has an electronegativity of 3.44 compared to Hydrogen's 2.20. This large difference (ΔEN = 1.24) pulls electron density strongly toward Oxygen, giving it a partial negative charge (δ⁻) and leaving Hydrogens partial positive (δ⁺). Because the molecule is bent rather than linear, these two bond dipoles do not cancel out, yielding a net molecular dipole moment of 1.85 Debye.");
            m.addEducationalItem("Why does oxygen form two bonds?",
                "Oxygen has 6 valence electrons and needs 2 more to achieve a stable octet (8 electrons). It achieves this by sharing 1 electron pair with each of two Hydrogen atoms, leaving 2 non-bonding lone pairs.");

            register("H2O", m);
            register("WATER", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 2. HYDROGEN PEROXIDE (H2O2) - Skewed / Open-Book 111.5°
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "H₂O₂", "Hydrogen Peroxide", "Peroxide / Antiseptic", "34.014 g/mol", "Oxidizing Covalent Peroxide",
                "Non-planar Open-Book (Skewed)", "Tetrahedral per Oxygen", "sp³", "94.8° (H-O-O) / 111.5° Dihedral",
                "Single Covalent O-O & Polar O-H", "Polar (Net Dipole μ = 2.26 D)", 2.26f, true,
                "Hydrogen Bonding & Strong Dipole Interaction",
                0.0f, 0.7f, 0.7f,
                "Two central oxygen atoms bonded by a peroxide single bond (-O-O-), with each oxygen bearing one terminal hydrogen and two lone pairs.",
                "Non-planar open-book geometry caused by mutual repulsion between the lone pairs on adjacent oxygen atoms, forcing the two H-O bonds into a dihedral angle of 111.5° in the gas phase.",
                "Oxygen-oxygen single bonds form with 146 kJ/mol bond energy. The weak peroxide link accounts for its high thermodynamic instability and oxidizing prowess.",
                "Strong net dipole moment (2.26 D) due to non-canceling polar O-H bonds oriented at a skew angle.",
                "Viscous pale blue liquid; potent bleaching agent, eco-friendly disinfectant yielding only water and oxygen on decomposition.",
                "H₂ (g) + O₂ (g) → H₂O₂ (l)  [ΔH° = -187.8 kJ/mol]",
                "Hydrogen and Oxygen gases (or anthraquinone auto-oxidation cycle).",
                "Coupling of hydroxyl radical species into a weak O-O single covalent bond.",
                "Hydrogen peroxide liquid product.",
                "SAFETY: Concentrated H₂O₂ (>70%) is a hypergolic monopropellant and dangerous oxidizer. Laboratory synthesis uses controlled electrochemical or quinone autoxidation processes."
            );

            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                -0.73f, 0.0f, 0.0f, -0.8f, 0.0f, 66.0f, Atom3D.getCpkColor("O"),
                "Central Peroxide Core", 0, -0.38f));

            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                0.73f, 0.0f, 0.0f, 0.8f, 0.0f, 66.0f, Atom3D.getCpkColor("O"),
                "Central Peroxide Core", 0, -0.38f));

            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -1.15f, 0.78f, 0.45f, -1.8f, 0.8f, 31.0f, Atom3D.getCpkColor("H"),
                "Terminal Hydrogen", 0, +0.38f));

            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                1.15f, -0.78f, 0.45f, 1.8f, -0.8f, 31.0f, Atom3D.getCpkColor("H"),
                "Terminal Hydrogen", 0, +0.38f));

            m.addBond(new Bond3D(0, 1, 1, 1.47f, "Peroxide Covalent O-O σ-bond (Weak)"));
            m.addBond(new Bond3D(0, 2, 1, 0.97f, "Polar Covalent O-H σ-bond"));
            m.addBond(new Bond3D(1, 3, 1, 0.97f, "Polar Covalent O-H σ-bond"));

            m.addLonePair(new LonePair3D(0, 180.0f, -0.7f, -0.5f, 0.5f));
            m.addLonePair(new LonePair3D(1, 0.0f, 0.7f, 0.5f, -0.5f));

            m.addEducationalItem("Why is H₂O₂ shaped like an open book?",
                "Each oxygen atom has two lone pairs and two single bonds. The repulsion between the lone pairs on the adjacent oxygen atoms prevents the molecule from lying flat, twisting the two H-O bonds into a dihedral angle of about 111.5°.");

            register("H2O2", m);
            register("HYDROGEN PEROXIDE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 3. CARBON DIOXIDE (CO2) - Linear 180°
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "CO₂", "Carbon Dioxide", "Carbonic Gas / Dry Ice", "44.009 g/mol", "Covalent Linear Gas",
                "Linear (180°)", "Linear", "sp", "180.0°",
                "Double Covalent C=O (ΔEN = 0.89)", "Non-polar (Symmetric, μ = 0.00 D)", 0.00f, false,
                "London Dispersion Forces",
                0.0f, 0.0f, 0.0f,
                "Central carbon atom double-bonded to two oxygen atoms in a straight line: O=C=O.",
                "VSEPR Repulsion: Carbon has 2 electron domains (2 double bonds, 0 lone pairs). To minimize electron repulsion, the two double bonds point in opposite directions at 180°, resulting in an exact linear geometry.",
                "Carbon (4 valence e⁻) forms two double bonds (one σ and one π bond each) with two Oxygen atoms (6 valence e⁻ each). This satisfies the octet of all three atoms with zero formal charges.",
                "Although each C=O double bond is highly polar due to Oxygen's higher electronegativity, the two identical bond dipoles point in exactly opposite directions (180°) and perfectly cancel each other out, making the overall molecule non-polar.",
                "Sublimes directly from solid to gas at -78.5°C; non-flammable, greenhouse gas, supercritical fluid solvent.",
                "C (s) + O₂ (g) → CO₂ (g)  [ΔH° = -393.5 kJ/mol]",
                "Carbon (solid graphite/coal) and atmospheric Oxygen gas.",
                "Exothermic oxidative cleavage of O=O bonds and covalent bond construction.",
                "Gaseous Carbon Dioxide.",
                "SAFETY: High concentrations of CO₂ cause asphyxiation by displacing oxygen. Stored in high-pressure cylinders or as cryogenic dry ice."
            );

            m.addAtom(new Atom3D("C", "Carbon", 6, 12.011, "[He] 2s² 2p²", 4,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, Atom3D.getCpkColor("C"),
                "Central sp Atom", 0, +0.70f));

            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                -1.16f, 0.0f, 0.0f, -1.8f, 0.0f, 66.0f, Atom3D.getCpkColor("O"),
                "Terminal Oxygen", 0, -0.35f));

            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                1.16f, 0.0f, 0.0f, 1.8f, 0.0f, 66.0f, Atom3D.getCpkColor("O"),
                "Terminal Oxygen", 0, -0.35f));

            m.addBond(new Bond3D(0, 1, 2, 1.16f, "Double Covalent C=O bond (1 σ + 1 π)"));
            m.addBond(new Bond3D(0, 2, 2, 1.16f, "Double Covalent C=O bond (1 σ + 1 π)"));

            m.addLonePair(new LonePair3D(1, 90.0f, -1.5f, 0.4f, 0.0f));
            m.addLonePair(new LonePair3D(1, 270.0f, -1.5f, -0.4f, 0.0f));
            m.addLonePair(new LonePair3D(2, 90.0f, 1.5f, 0.4f, 0.0f));
            m.addLonePair(new LonePair3D(2, 270.0f, 1.5f, -0.4f, 0.0f));

            m.addEducationalItem("Why is CO₂ non-polar when C=O bonds are polar?",
                "Each C=O bond is strongly polar because Oxygen (EN = 3.44) is more electronegative than Carbon (EN = 2.55). However, because CO₂ is strictly linear (180°), the two identical bond dipoles point in opposite directions and cancel each other out mathematically, leaving a net dipole moment of 0.");
            m.addEducationalItem("What is the hybridization of Carbon in CO₂?",
                "Carbon has 2 electron domains (2 double bonds). It mixes one 2s orbital and one 2p orbital to form two sp hybrid orbitals at 180° for the σ-bonds, leaving two unhybridized p orbitals for π-bonding.");

            register("CO2", m);
            register("CARBON DIOXIDE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 4. CARBON MONOXIDE (CO) - Linear Diatomic Triple Bond
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "CO", "Carbon Monoxide", "Coal Gas / Reducing Agent", "28.010 g/mol", "Reducing Toxic Diatomic Gas",
                "Linear (Diatomic)", "Linear", "sp", "180.0°",
                "Triple Covalent C≡O (Includes coordinate bond)", "Polar (Net Dipole μ = 0.122 D)", 0.122f, true,
                "Dipole-Dipole & London Dispersion",
                1.0f, 0.0f, 0.0f,
                "Diatomic molecule with a triple bond: two shared electron pairs plus one coordinate dative covalent bond from Oxygen to Carbon.",
                "Diatomic molecules are inherently linear. The C≡O bond length is 112.8 pm, one of the shortest and strongest bonds in chemistry (1072 kJ/mol).",
                "Carbon has 4 valence electrons and Oxygen has 6. To complete octets for both atoms, Oxygen donates a lone pair to form a coordinate covalent bond, resulting in formal charges of -1 on Carbon and +1 on Oxygen.",
                "Because Oxygen is more electronegative, it pulls σ-electrons toward itself, but the dative π-donation from Oxygen back to Carbon partially counteracts this, resulting in a small net dipole moment of 0.122 D with the negative end surprisingly on Carbon.",
                "Colorless, odorless, toxic gas that binds irreversibly to blood hemoglobin (forming carboxyhemoglobin); vital reducing agent in blast furnaces.",
                "2 C (s) + O₂ (g) [limited] → 2 CO (g)  [ΔH° = -221.0 kJ/mol]",
                "Carbon (coke) and limited sub-stoichiometric oxygen.",
                "Partial combustion yielding carbon monoxide.",
                "Toxic gaseous Carbon Monoxide.",
                "SAFETY: Carbon monoxide is a lethal, silent poison with 200x greater affinity for hemoglobin than oxygen. Requires certified gas sensors."
            );

            m.addAtom(new Atom3D("C", "Carbon", 6, 12.011, "[He] 2s² 2p²", 4,
                -0.56f, 0.0f, 0.0f, -0.8f, 0.0f, 70.0f, Atom3D.getCpkColor("C"),
                "Carbon Atom", -1, -0.15f));

            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                0.56f, 0.0f, 0.0f, 0.8f, 0.0f, 66.0f, Atom3D.getCpkColor("O"),
                "Oxygen Atom", +1, +0.15f));

            m.addBond(new Bond3D(0, 1, 3, 1.13f, "Triple Covalent C≡O (1 σ + 2 π including dative)"));
            m.addLonePair(new LonePair3D(0, 180.0f, -0.9f, 0.0f, 0.0f));
            m.addLonePair(new LonePair3D(1, 0.0f, 0.9f, 0.0f, 0.0f));

            m.addEducationalItem("Why is the C≡O bond so exceptionally strong?",
                "The bond dissociation energy of CO is 1,072 kJ/mol, making it one of the strongest chemical bonds known. The combination of one strong σ-bond and two mutually perpendicular π-bonds (including coordinate dative electron sharing) pulls the nuclei extraordinarily close together (112.8 pm).");

            register("CO", m);
            register("CARBON MONOXIDE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 5. SODIUM CHLORIDE (NaCl) - Ionic Lattice
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "NaCl", "Sodium Chloride", "Table Salt / Halite", "58.440 g/mol", "Ionic Crystal Lattice",
                "Face-Centered Cubic (FCC 6:6)", "Octahedral Coordination", "Ionic (No orbital hybridization)", "90.0°",
                "Electrostatic Ionic Bond (ΔEN = 2.23)", "Ionic (Extreme Dipole Separation)", 9.00f, true,
                "Strong Electrostatic Ionic Lattice Forces (786 kJ/mol)",
                1.0f, 0.0f, 0.0f,
                "Cation-anion electrostatic coordination: Each Na⁺ cation is surrounded by 6 Cl⁻ anions, and each Cl⁻ is surrounded by 6 Na⁺ in an alternating 3D cubic lattice.",
                "Radius Ratio Rule: The ionic radius ratio (r_Na⁺ / r_Cl⁻ = 102 pm / 181 pm = 0.56) falls in the 0.414–0.732 range, which geometrically favors 6-fold octahedral coordination.",
                "Complete electron transfer: Sodium (1s²2s²2p⁶3s¹) donates its single 3s valence electron to Chlorine (1s²2s²2p⁶3s²3p⁵), producing stable noble-gas electron configurations: Na⁺ ([Ne]) and Cl⁻ ([Ar]).",
                "Complete separation of integer charges (+1 and -1) creates immense local ionic dipoles within the crystal lattice.",
                "High melting point (801°C), brittle cleavage along crystal planes, excellent electrical conductivity when molten or dissolved in aqueous solution.",
                "2 Na (s) + Cl₂ (g) → 2 NaCl (s)  [ΔH° = -822.2 kJ/mol]",
                "Metallic sodium solid and chlorine gas.",
                "Vigorous oxidation of sodium and reduction of chlorine to form ionic salt crystals.",
                "Solid Sodium Chloride crystals.",
                "SAFETY: Metallic sodium reacts explosively with water; chlorine is a toxic choking gas. Industrial synthesis is safe when performed in regulated chloralkali facilities."
            );

            // Central Na+
            m.addAtom(new Atom3D("Na", "Sodium", 11, 22.990, "[Ne] 3s¹", 1,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 102.0f, Atom3D.getCpkColor("NA"),
                "Central Na⁺ Cation", +1, +1.0f));

            // 6 surrounding Cl- anions (Octahedral cluster)
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                1.4f, 0.0f, 0.0f, 1.6f, 0.0f, 181.0f, Atom3D.getCpkColor("CL"),
                "Coordinating Cl⁻ Anion", -1, -1.0f));
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                -1.4f, 0.0f, 0.0f, -1.6f, 0.0f, 181.0f, Atom3D.getCpkColor("CL"),
                "Coordinating Cl⁻ Anion", -1, -1.0f));
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                0.0f, 1.4f, 0.0f, 0.0f, 1.6f, 181.0f, Atom3D.getCpkColor("CL"),
                "Coordinating Cl⁻ Anion", -1, -1.0f));
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                0.0f, -1.4f, 0.0f, 0.0f, -1.6f, 181.0f, Atom3D.getCpkColor("CL"),
                "Coordinating Cl⁻ Anion", -1, -1.0f));
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                0.0f, 0.0f, 1.4f, 1.0f, 1.0f, 181.0f, Atom3D.getCpkColor("CL"),
                "Coordinating Cl⁻ Anion", -1, -1.0f));
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                0.0f, 0.0f, -1.4f, -1.0f, -1.0f, 181.0f, Atom3D.getCpkColor("CL"),
                "Coordinating Cl⁻ Anion", -1, -1.0f));

            m.addBond(new Bond3D(0, 1, 0, 2.82f, "Electrostatic Ionic Attraction"));
            m.addBond(new Bond3D(0, 2, 0, 2.82f, "Electrostatic Ionic Attraction"));
            m.addBond(new Bond3D(0, 3, 0, 2.82f, "Electrostatic Ionic Attraction"));
            m.addBond(new Bond3D(0, 4, 0, 2.82f, "Electrostatic Ionic Attraction"));
            m.addBond(new Bond3D(0, 5, 0, 2.82f, "Electrostatic Ionic Attraction"));
            m.addBond(new Bond3D(0, 6, 0, 2.82f, "Electrostatic Ionic Attraction"));

            m.addEducationalItem("Why is NaCl considered a lattice rather than a discrete molecule?",
                "In solid table salt, there is no isolated single 'NaCl' unit. Instead, billions of alternating positive sodium ions and negative chloride ions pack into a continuous 3D Face-Centered Cubic crystalline array held together by electrostatic forces in all directions.");
            m.addEducationalItem("Why do solid salts not conduct electricity, but salt solutions do?",
                "In solid crystals, ions are locked into fixed positions within the lattice. When dissolved in water or melted, the ions become free-flowing mobile charge carriers that conduct electric currents.");

            register("NACL", m);
            register("SALT", m);
            register("TABLE SALT", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 6. METHANE (CH4) - Tetrahedral 109.5°
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "CH₄", "Methane", "Natural Gas / Firedamp", "16.043 g/mol", "Hydrocarbon Alkane Gas",
                "Tetrahedral (109.5°)", "Tetrahedral", "sp³", "109.5°",
                "Non-polar Covalent C-H (ΔEN = 0.35)", "Non-polar (Symmetric, μ = 0.00 D)", 0.00f, false,
                "London Dispersion Forces",
                0.0f, 0.0f, 0.0f,
                "Central carbon atom covalently bonded to four terminal hydrogen atoms at the vertices of a regular tetrahedron.",
                "VSEPR Repulsion: Four identical bonding electron domains repel equally in three dimensions, adopting a regular tetrahedron with 109.5° bond angles.",
                "Carbon promotes a 2s electron into its vacant 2p orbital, then hybridizes its 2s and three 2p orbitals into four identical sp³ hybrid orbitals directed at 109.5° angles, each overlapping with a Hydrogen 1s orbital.",
                "The four C-H bond dipoles point symmetrically inward from the four vertices of a tetrahedron, canceling completely. Net dipole moment is 0.00 D.",
                "Cleanest burning fossil fuel producing minimal soot; primary rocket fuel for SpaceX Raptor methalox engines; odorless (mercaptans added commercially for leak detection).",
                "C (s) + 2 H₂ (g) → CH₄ (g)  [ΔH° = -74.8 kJ/mol]",
                "Carbon and Hydrogen gases (or Sabatier CO₂ + 4 H₂ methanation).",
                "Catalytic bond formation over nickel catalysts at 300°C–400°C.",
                "Methane gas (CH₄).",
                "SAFETY: Methane forms explosive mixtures with air between 5% and 15% concentration (firedamp in mines)."
            );

            // Central C
            m.addAtom(new Atom3D("C", "Carbon", 6, 12.011, "[He] 2s² 2p²", 4,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, Atom3D.getCpkColor("C"),
                "Central sp³ Carbon", 0, -0.40f));

            // 4 Hydrogens
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                0.0f, 1.09f, 0.0f, 0.0f, 1.3f, 31.0f, Atom3D.getCpkColor("H"),
                "Vertex Hydrogen", 0, +0.10f));
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                1.028f, -0.363f, 0.0f, 1.2f, -0.6f, 31.0f, Atom3D.getCpkColor("H"),
                "Vertex Hydrogen", 0, +0.10f));
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -0.514f, -0.363f, 0.89f, -1.1f, -0.9f, 31.0f, Atom3D.getCpkColor("H"),
                "Vertex Hydrogen (Wedge)", 0, +0.10f));
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -0.514f, -0.363f, -0.89f, -0.6f, 0.7f, 31.0f, Atom3D.getCpkColor("H"),
                "Vertex Hydrogen (Dash)", 0, +0.10f));

            m.addBond(new Bond3D(0, 1, 1, 1.09f, "Covalent C-H σ-bond (sp³-1s)"));
            m.addBond(new Bond3D(0, 2, 1, 1.09f, "Covalent C-H σ-bond (sp³-1s)"));
            m.addBond(new Bond3D(0, 3, 1, 1.09f, "Covalent C-H σ-bond (sp³-1s)"));
            m.addBond(new Bond3D(0, 4, 1, 1.09f, "Covalent C-H σ-bond (sp³-1s)"));

            m.addEducationalItem("Why is the H-C-H angle in methane exactly 109.5°?",
                "Methane has 4 identical single bonds around the central Carbon with no unshared lone pairs. The geometry that maximizes the distance between 4 mutually repelling electron domains in 3D space is a regular tetrahedron, whose geometric angle between center and vertices is arccos(-1/3) ≈ 109.47°.");

            register("CH4", m);
            register("METHANE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 7. AMMONIA (NH3) - Trigonal Pyramidal 107.3°
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "NH₃", "Ammonia", "Ammonia / Fertilizer Base", "17.031 g/mol", "Polar Nitrogen Hydride Base",
                "Trigonal Pyramidal (107.3°)", "Tetrahedral", "sp³", "107.3°",
                "Polar Covalent N-H (ΔEN = 0.84)", "Polar (Net Dipole μ = 1.47 D)", 1.47f, true,
                "Hydrogen Bonding & Dipole-Dipole",
                0.0f, 1.0f, 0.0f,
                "Central nitrogen atom bonded to three hydrogen atoms with one prominent non-bonding lone pair capping the apex.",
                "VSEPR Repulsion: Four electron domains (3 bonding pairs + 1 lone pair). The lone pair repels the three bonding pairs more strongly, compressing the tetrahedral angle from 109.5° down to 107.3°.",
                "Nitrogen (5 valence e⁻) shares 3 electron pairs with three Hydrogen atoms, leaving one localized non-bonding lone pair that acts as a potent Lewis base.",
                "The three polar N-H bonds point downward in a pyramid; their vertical components add constructively with the top lone pair's electron density to create a substantial net dipole of 1.47 D.",
                "Pungent alkaline gas; agricultural fertilizer synthesis (Haber-Bosch); undergoes nitrogen umbrella inversion at 24 GHz.",
                "N₂ (g) + 3 H₂ (g) ⇌ 2 NH₃ (g)  [ΔH° = -92.2 kJ/mol]",
                "Atmospheric Nitrogen (N₂) and Hydrogen (H₂) from steam-reformed natural gas.",
                "Haber-Bosch catalytic synthesis over promoted iron catalyst at 450°C and 200 atmospheres.",
                "Pure Ammonia gas (NH₃).",
                "SAFETY: Toxic and corrosive when inhaled; hazardous vapor cloud risk. Industrial synthesis requires massive pressure and strict safety valves."
            );

            // Nitrogen (Apex)
            m.addAtom(new Atom3D("N", "Nitrogen", 7, 14.007, "1s² 2s² 2p³", 5,
                0.0f, 0.38f, 0.0f, 0.0f, 0.4f, 65.0f, Atom3D.getCpkColor("N"),
                "Central Apex Nitrogen", 0, -0.60f));

            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                0.94f, -0.13f, 0.0f, 1.1f, -0.5f, 31.0f, Atom3D.getCpkColor("H"),
                "Base Hydrogen", 0, +0.20f));
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -0.47f, -0.13f, 0.81f, -1.0f, -0.5f, 31.0f, Atom3D.getCpkColor("H"),
                "Base Hydrogen", 0, +0.20f));
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -0.47f, -0.13f, -0.81f, 0.0f, -0.9f, 31.0f, Atom3D.getCpkColor("H"),
                "Base Hydrogen", 0, +0.20f));

            m.addBond(new Bond3D(0, 1, 1, 1.01f, "Polar Covalent N-H σ-bond"));
            m.addBond(new Bond3D(0, 2, 1, 1.01f, "Polar Covalent N-H σ-bond"));
            m.addBond(new Bond3D(0, 3, 1, 1.01f, "Polar Covalent N-H σ-bond"));

            m.addLonePair(new LonePair3D(0, 270.0f, 0.0f, 0.9f, 0.0f));

            m.addEducationalItem("Why is ammonia's bond angle 107° while water is 104.5°?",
                "Both molecules have 4 electron domains and sp³ hybridization. However, ammonia has only 1 lone pair, while water has 2 lone pairs. Each lone pair exerts extra repulsion and squeezes the bonding pairs by approximately 2° to 2.5°. Water's 2 lone pairs squeeze the angle twice as much, compressing it down to 104.5° compared to ammonia's 107.3°.");

            register("NH3", m);
            register("AMMONIA", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 8. BORON TRIFLUORIDE (BF3) - Trigonal Planar 120°
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "BF₃", "Boron Trifluoride", "Boron Trifluoride / Lewis Acid", "67.805 g/mol", "Incomplete Octet Lewis Acid",
                "Trigonal Planar (120°)", "Trigonal Planar", "sp²", "120.0°",
                "Polar Covalent B-F (ΔEN = 1.94)", "Non-polar (Symmetric, μ = 0.00 D)", 0.00f, false,
                "London Dispersion Forces",
                0.0f, 0.0f, 0.0f,
                "Central boron atom surrounded by three fluorine atoms in a single flat plane at 120° angles.",
                "VSEPR Repulsion: Three electron domains (3 single bonds, 0 lone pairs) repel equally in 2D space, adopting a flat trigonal planar geometry with exact 120° angles.",
                "Boron has 3 valence electrons and shares them with 3 Fluorine atoms. Boron is an 'octet exception' with only 6 valence electrons around it, making it an extraordinarily potent Lewis acid (electron pair acceptor).",
                "Although B-F bonds are intensely polar (ΔEN = 1.94), the three coplanar bond dipoles are separated by 120° and cancel out perfectly. The overall molecule is non-polar.",
                "Toxic fuming gas; premier Lewis acid catalyst in organic synthesis (Friedel-Crafts alkylations, polymerization).",
                "B₂O₃ + 6 HF → 2 BF₃ + 3 H₂O",
                "Boron trioxide and hydrofluoric acid.",
                "Fluorination and distillation yielding boron trifluoride.",
                "Boron trifluoride gas.",
                "SAFETY: Corrosive and toxic; hydrolyzes in moist air to produce toxic HF fumes."
            );

            m.addAtom(new Atom3D("B", "Boron", 5, 10.810, "[He] 2s² 2p¹", 3,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 82.0f, Atom3D.getCpkColor("B"),
                "Central sp² Boron (Incomplete Octet)", 0, +0.80f));

            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7,
                0.0f, 1.30f, 0.0f, 0.0f, 1.5f, 57.0f, Atom3D.getCpkColor("F"),
                "Terminal Fluorine", 0, -0.27f));
            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7,
                1.125f, -0.65f, 0.0f, 1.3f, -0.75f, 57.0f, Atom3D.getCpkColor("F"),
                "Terminal Fluorine", 0, -0.27f));
            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7,
                -1.125f, -0.65f, 0.0f, -1.3f, -0.75f, 57.0f, Atom3D.getCpkColor("F"),
                "Terminal Fluorine", 0, -0.27f));

            m.addBond(new Bond3D(0, 1, 1, 1.30f, "Polar Covalent B-F σ-bond"));
            m.addBond(new Bond3D(0, 2, 1, 1.30f, "Polar Covalent B-F σ-bond"));
            m.addBond(new Bond3D(0, 3, 1, 1.30f, "Polar Covalent B-F σ-bond"));

            m.addEducationalItem("Why does Boron only have 6 valence electrons in BF₃?",
                "Boron (Group 13) has only 3 valence electrons. Even after sharing one electron with each of 3 Fluorines, it only has 6 electrons in its valence shell. This makes it an 'incomplete octet' molecule and an aggressive Lewis acid hungry to accept a lone pair from electron donors like ammonia or ether.");

            register("BF3", m);
            register("BORON TRIFLUORIDE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 9. ETHANOL (C2H5OH) - Organic Alcohol
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "C₂H₅OH", "Ethanol", "Grain Alcohol / Biofuel", "46.069 g/mol", "Organic Alcohol",
                "Tetrahedral per Carbon, Bent at Oxygen (108.5°)", "Tetrahedral per C & O", "sp³", "109.5° (C-C-H) / 108.5° (C-O-H)",
                "Covalent C-C, C-H, C-O, and Polar O-H", "Polar (Net Dipole μ = 1.69 D)", 1.69f, true,
                "Intermolecular Hydrogen Bonding & Dipole-Dipole",
                0.5f, 0.8f, 0.0f,
                "Ethyl group (CH₃-CH₂-) covalently bonded to a polar hydroxyl (-OH) group.",
                "Geometry is tetrahedral around both carbons (sp³), and bent (108.5°) around the oxygen atom due to its two lone pairs.",
                "Carbon-carbon and carbon-hydrogen bonds form non-polar covalent frameworks, while the electronegative oxygen atom forms a polar hydroxyl bond capable of robust hydrogen bonding.",
                "The bent hydroxyl group imparts a permanent dipole moment (1.69 D), making ethanol miscible with water in all proportions.",
                "Universal renewable biofuel, laboratory antiseptic, perfume solvent, beverage ethanol.",
                "C₂H₄ + H₂O → C₂H₅OH  (or C₆H₁₂O₆ → 2 C₂H₅OH + 2 CO₂)",
                "Ethylene gas and steam (hydration) or sugar fermentation.",
                "Acid-catalyzed hydration over phosphoric acid at 300°C.",
                "Liquid Ethanol (C₂H₅OH).",
                "SAFETY: Flammable liquid and vapor; keep away from ignition sources."
            );

            m.addAtom(new Atom3D("C", "Carbon", 6, 12.011, "[He] 2s² 2p²", 4,
                -1.2f, 0.0f, 0.0f, -1.8f, 0.0f, 70.0f, Atom3D.getCpkColor("C"),
                "Methyl Carbon (C1)", 0, -0.30f));
            m.addAtom(new Atom3D("C", "Carbon", 6, 12.011, "[He] 2s² 2p²", 4,
                0.2f, 0.0f, 0.0f, -0.4f, 0.0f, 70.0f, Atom3D.getCpkColor("C"),
                "Methylene Carbon (C2)", 0, +0.10f));
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                1.0f, 1.1f, 0.0f, 0.8f, 1.0f, 66.0f, Atom3D.getCpkColor("O"),
                "Hydroxyl Oxygen", 0, -0.70f));
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                1.9f, 0.9f, 0.0f, 1.8f, 1.4f, 31.0f, Atom3D.getCpkColor("H"),
                "Hydroxyl Hydrogen", 0, +0.40f));

            // Hydrogens on C1
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -1.6f, -1.0f, 0.0f, -2.4f, -1.0f, 31.0f, Atom3D.getCpkColor("H"), "Methyl H", 0, +0.10f));
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -1.6f, 0.5f, 0.9f, -2.4f, 0.8f, 31.0f, Atom3D.getCpkColor("H"), "Methyl H", 0, +0.10f));
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -1.6f, 0.5f, -0.9f, -1.8f, 1.1f, 31.0f, Atom3D.getCpkColor("H"), "Methyl H", 0, +0.10f));

            // Hydrogens on C2
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                0.5f, -0.6f, 0.9f, -0.4f, -1.1f, 31.0f, Atom3D.getCpkColor("H"), "Methylene H", 0, +0.10f));
            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                0.5f, -0.6f, -0.9f, 0.3f, -0.9f, 31.0f, Atom3D.getCpkColor("H"), "Methylene H", 0, +0.10f));

            m.addBond(new Bond3D(0, 1, 1, 1.54f, "Covalent C-C σ-bond"));
            m.addBond(new Bond3D(1, 2, 1, 1.43f, "Covalent C-O σ-bond"));
            m.addBond(new Bond3D(2, 3, 1, 0.96f, "Polar Covalent O-H σ-bond"));
            m.addBond(new Bond3D(0, 4, 1, 1.09f, "C-H σ-bond"));
            m.addBond(new Bond3D(0, 5, 1, 1.09f, "C-H σ-bond"));
            m.addBond(new Bond3D(0, 6, 1, 1.09f, "C-H σ-bond"));
            m.addBond(new Bond3D(1, 7, 1, 1.09f, "C-H σ-bond"));
            m.addBond(new Bond3D(1, 8, 1, 1.09f, "C-H σ-bond"));

            m.addLonePair(new LonePair3D(2, 90.0f, 0.8f, 1.5f, 0.4f));
            m.addLonePair(new LonePair3D(2, 270.0f, 0.8f, 1.5f, -0.4f));

            register("C2H5OH", m);
            register("ETHANOL", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 10. HYDROCHLORIC ACID (HCl) - Polar Diatomic
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "HCl", "Hydrogen Chloride", "Hydrochloric Acid / Muriatic Acid", "36.461 g/mol", "Strong Polar Acid Gas",
                "Linear (Diatomic)", "Linear", "None (Diatomic σ)", "180.0°",
                "Polar Covalent H-Cl (ΔEN = 0.96)", "Polar (Net Dipole μ = 1.08 D)", 1.08f, true,
                "Dipole-Dipole & Hydrogen Bonding in solution",
                1.0f, 0.0f, 0.0f,
                "Diatomic molecule: one hydrogen atom single-bonded to a chlorine atom with 3 non-bonding lone pairs on chlorine.",
                "All diatomic molecules are linear. Bond length is 127.4 pm.",
                "Hydrogen shares its single 1s electron with Chlorine's unpaired 3p electron, completing Helium's duet (2e⁻) and Argon's octet (8e⁻).",
                "Chlorine's high electronegativity (3.16) vs Hydrogen (2.20) concentrates electron density on Chlorine, creating a dipole of 1.08 Debye.",
                "Fuming acidic gas; fully dissociates in water into H⁺ and Cl⁻ ions (pKa = -5.9); crucial for gastric digestion and industrial steel pickling.",
                "H₂ (g) + Cl₂ (g) → 2 HCl (g)  [ΔH° = -184.6 kJ/mol]",
                "Hydrogen gas and Chlorine gas.",
                "UV- or thermal-activated free radical chain reaction.",
                "Hydrogen Chloride gas (dissolves in water to form Hydrochloric Acid).",
                "SAFETY: Concentrated HCl produces choking, corrosive acid fumes that cause severe respiratory damage."
            );

            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -0.64f, 0.0f, 0.0f, -1.0f, 0.0f, 31.0f, Atom3D.getCpkColor("H"),
                "Electropositive Terminal H", 0, +0.25f));
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                0.64f, 0.0f, 0.0f, 1.0f, 0.0f, 99.0f, Atom3D.getCpkColor("CL"),
                "Electronegative Terminal Cl", 0, -0.25f));

            m.addBond(new Bond3D(0, 1, 1, 1.27f, "Polar Covalent H-Cl σ-bond"));

            m.addLonePair(new LonePair3D(1, 0.0f, 1.2f, 0.0f, 0.0f));
            m.addLonePair(new LonePair3D(1, 90.0f, 0.7f, 0.6f, 0.0f));
            m.addLonePair(new LonePair3D(1, 270.0f, 0.7f, -0.6f, 0.0f));

            register("HCL", m);
            register("HYDROCHLORIC ACID", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 11. SILICON DIOXIDE (SiO2) - 3D Covalent Network
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "SiO₂", "Silicon Dioxide", "Quartz / Silica / Aerogel", "60.084 g/mol", "Covalent Network Mineral",
                "Tetrahedral SiO₄ Network (Si-O-Si ~144°)", "Tetrahedral", "sp³", "109.5° (O-Si-O) / 144° (Si-O-Si)",
                "Polar Covalent Network Si-O (ΔEN = 1.54)", "Non-polar Macro-Lattice", 0.0f, false,
                "Gigantic Continuous Covalent Network (Bond Energy 460 kJ/mol)",
                0.0f, 0.0f, 0.0f,
                "Continuous 3D covalent lattice composed of corner-sharing SiO₄ tetrahedra, with each silicon atom bonded to 4 oxygen atoms and each oxygen bridging two silicon atoms.",
                "Tetrahedral coordination around Silicon (sp³) with flexible, bent Si-O-Si bridging angles (~144°), allowing a diverse array of crystalline polytypes (quartz, cristobalite) and amorphous glass.",
                "Silicon (1.90 EN) and Oxygen (3.44 EN) form extremely durable σ-bonds with 460 kJ/mol dissociation energy, conferring immense mechanical hardness and thermal stability.",
                "Macroscopic symmetrical network cancels internal bond dipoles; completely non-polar bulk mineral.",
                "Melting point of 1,710°C; piezoelectric in quartz watches; optical clarity in glass and fiber-optic communication; precursor for 99.8% air aerogel 'frozen smoke'.",
                "Si (s) + O₂ (g) → SiO₂ (s)  [ΔH° = -910.7 kJ/mol]",
                "Silicon metal and Oxygen gas.",
                "High-temperature thermal oxidation in cleanrooms.",
                "Solid Silicon Dioxide quartz / glass.",
                "SAFETY: Chronic inhalation of crystalline silica dust causes silicosis and lung fibrosis."
            );

            // Central Silicon
            m.addAtom(new Atom3D("Si", "Silicon", 14, 28.085, "[Ne] 3s² 3p²", 4,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 111.0f, Atom3D.getCpkColor("SI"),
                "Central sp³ Silicon", 0, +0.90f));

            // 4 Bridging Oxygens
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                0.0f, 1.6f, 0.0f, 0.0f, 1.8f, 66.0f, Atom3D.getCpkColor("O"), "Bridging Oxygen", 0, -0.45f));
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                1.5f, -0.5f, 0.0f, 1.7f, -0.8f, 66.0f, Atom3D.getCpkColor("O"), "Bridging Oxygen", 0, -0.45f));
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                -0.75f, -0.5f, 1.3f, -1.5f, -1.1f, 66.0f, Atom3D.getCpkColor("O"), "Bridging Oxygen", 0, -0.45f));
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                -0.75f, -0.5f, -1.3f, -0.8f, 1.0f, 66.0f, Atom3D.getCpkColor("O"), "Bridging Oxygen", 0, -0.45f));

            m.addBond(new Bond3D(0, 1, 1, 1.61f, "Strong Covalent Si-O σ-network bond"));
            m.addBond(new Bond3D(0, 2, 1, 1.61f, "Strong Covalent Si-O σ-network bond"));
            m.addBond(new Bond3D(0, 3, 1, 1.61f, "Strong Covalent Si-O σ-network bond"));
            m.addBond(new Bond3D(0, 4, 1, 1.61f, "Strong Covalent Si-O σ-network bond"));

            register("SIO2", m);
            register("SILICON DIOXIDE", m);
            register("QUARTZ", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 12. IRON(III) OXIDE (Fe2O3) - Corundum Lattice / Rust
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "Fe₂O₃", "Iron(III) Oxide", "Rust / Hematite Ore", "159.69 g/mol", "Inorganic Transition Metal Oxide",
                "Rhombohedral Corundum Lattice (Fe³⁺ in 2/3 octahedral sites)", "Octahedral Coordination", "Ionic / d-Orbital Overlap", "90.0°",
                "Ionic with Covalent Character (Lattice Energy 14,774 kJ/mol)", "Polar Solid State Array", 0.0f, false,
                "Immense Electrostatic Ionic Crystal Lattice Forces",
                0.0f, 0.0f, 0.0f,
                "Dense corundum-type rhombohedral lattice: Hexagonal close-packed oxygen anions with Fe³⁺ cations occupying two-thirds of the octahedral coordination interstices.",
                "Fe³⁺ has a high charge density (ionic radius 64 pm) favoring 6-coordinate octahedral coordination with Oxygen anions.",
                "Electrochemical oxidation of elemental iron in the presence of oxygen and atmospheric moisture transfers 3 electrons per iron atom, producing Fe³⁺ ([Ar] 3d⁵) high-spin stable half-filled shells.",
                "Symmetric crystal array with alternating charge balance.",
                "Primary ore for pig iron and steel smelting; pigment in paints (rouge); thermite reactions with aluminum; magnetic storage media.",
                "4 Fe (s) + 3 O₂ (g) + 6 H₂O (l) → 4 Fe(OH)₃ → 2 Fe₂O₃·3H₂O",
                "Metallic Iron, atmospheric Oxygen, and moisture.",
                "Electrochemical corrosion producing hydrated ferric oxide.",
                "Solid Hematite / Rust (Fe₂O₃).",
                "SAFETY: Inhalation of iron oxide fumes during welding can cause siderosis."
            );

            // Fe1
            m.addAtom(new Atom3D("Fe", "Iron", 26, 55.845, "[Ar] 3d⁶ 4s²", 8,
                -0.9f, 0.0f, 0.0f, -1.2f, 0.0f, 126.0f, Atom3D.getCpkColor("FE"),
                "Fe³⁺ Cation", +3, +1.5f));
            // Fe2
            m.addAtom(new Atom3D("Fe", "Iron", 26, 55.845, "[Ar] 3d⁶ 4s²", 8,
                0.9f, 0.0f, 0.0f, 1.2f, 0.0f, 126.0f, Atom3D.getCpkColor("FE"),
                "Fe³⁺ Cation", +3, +1.5f));

            // 3 Oxygens
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                0.0f, 1.2f, 0.0f, 0.0f, 1.4f, 66.0f, Atom3D.getCpkColor("O"), "Bridging O²⁻", -2, -1.0f));
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                0.0f, -0.6f, 1.0f, -0.6f, -1.1f, 66.0f, Atom3D.getCpkColor("O"), "Bridging O²⁻", -2, -1.0f));
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                0.0f, -0.6f, -1.0f, 0.6f, -1.1f, 66.0f, Atom3D.getCpkColor("O"), "Bridging O²⁻", -2, -1.0f));

            m.addBond(new Bond3D(0, 2, 0, 1.95f, "Fe-O Coordination Linkage"));
            m.addBond(new Bond3D(1, 2, 0, 1.95f, "Fe-O Coordination Linkage"));
            m.addBond(new Bond3D(0, 3, 0, 1.95f, "Fe-O Coordination Linkage"));
            m.addBond(new Bond3D(1, 3, 0, 1.95f, "Fe-O Coordination Linkage"));
            m.addBond(new Bond3D(0, 4, 0, 1.95f, "Fe-O Coordination Linkage"));
            m.addBond(new Bond3D(1, 4, 0, 1.95f, "Fe-O Coordination Linkage"));

            register("FE2O3", m);
            register("RUST", m);
            register("HEMATITE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 13. NITINOL (NiTi) - Shape-Memory Intermetallic
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "NiTi", "Nickel-Titanium", "Nitinol Shape-Memory Superalloy", "106.58 g/mol", "Smart Shape-Memory Superalloy",
                "Austenite B2 Cubic (High-T) / Martensite Monoclinic (Low-T)", "Cubic Intermetallic Lattice", "Metallic d-Orbital Overlap", "90.0° (Austenite)",
                "Metallic-Covalent Hybridized Intermetallic", "Non-polar (Metallic Sea of Electrons)", 0.0f, false,
                "Delocalized Metallic Conduction Band Bonding",
                0.0f, 0.0f, 0.0f,
                "Ordered 1:1 equiatomic intermetallic crystal lattice capable of reversible, diffusionless solid-state phase transformations between high-symmetry cubic Austenite and low-symmetry monoclinic Martensite.",
                "Near-equiatomic stoichiometry (50% Ni, 50% Ti) creates a CsCl-type B2 cubic structure at high temperatures that twins into a martensitic structure under stress without breaking atomic bonds.",
                "Overlap of Nickel 3d and Titanium 3d valence orbitals provides a high cohesive energy, allowing the alloy to recover up to 8% mechanical strain upon warming above its transformation temperature.",
                "Metallic conductor with delocalized valence electron sea.",
                "Cardiovascular self-expanding stents, orthodontic memory archwires, Mars rover wire-mesh tires, surgical instruments.",
                "Ni (s) + Ti (s) → NiTi (s)  [Vacuum Arc Melting at 1,310°C]",
                "High-purity Nickel and Titanium metals.",
                "Vacuum induction melting (VIM) under argon atmosphere.",
                "Solid Nitinol shape-memory ingot.",
                "SAFETY: Molten titanium is extremely reactive with oxygen, nitrogen, and carbon; requires inert vacuum processing."
            );

            // Ni central
            m.addAtom(new Atom3D("Ni", "Nickel", 28, 58.693, "[Ar] 3d⁸ 4s²", 10,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 124.0f, Atom3D.getCpkColor("NI"),
                "Intermetallic Nickel Site", 0, 0.0f));

            // Ti coordinating atoms
            m.addAtom(new Atom3D("Ti", "Titanium", 22, 47.867, "[Ar] 3d² 4s²", 4,
                1.3f, 1.3f, 1.3f, 1.4f, 1.4f, 140.0f, Atom3D.getCpkColor("TI"),
                "Intermetallic Titanium Site", 0, 0.0f));
            m.addAtom(new Atom3D("Ti", "Titanium", 22, 47.867, "[Ar] 3d² 4s²", 4,
                -1.3f, 1.3f, -1.3f, -1.4f, 1.4f, 140.0f, Atom3D.getCpkColor("TI"),
                "Intermetallic Titanium Site", 0, 0.0f));
            m.addAtom(new Atom3D("Ti", "Titanium", 22, 47.867, "[Ar] 3d² 4s²", 4,
                1.3f, -1.3f, -1.3f, 1.4f, -1.4f, 140.0f, Atom3D.getCpkColor("TI"),
                "Intermetallic Titanium Site", 0, 0.0f));
            m.addAtom(new Atom3D("Ti", "Titanium", 22, 47.867, "[Ar] 3d² 4s²", 4,
                -1.3f, -1.3f, 1.3f, -1.4f, -1.4f, 140.0f, Atom3D.getCpkColor("TI"),
                "Intermetallic Titanium Site", 0, 0.0f));

            m.addBond(new Bond3D(0, 1, 0, 2.60f, "Intermetallic Metallic Bond"));
            m.addBond(new Bond3D(0, 2, 0, 2.60f, "Intermetallic Metallic Bond"));
            m.addBond(new Bond3D(0, 3, 0, 2.60f, "Intermetallic Metallic Bond"));
            m.addBond(new Bond3D(0, 4, 0, 2.60f, "Intermetallic Metallic Bond"));

            register("NITI", m);
            register("NITINOL", m);
        }

        registerExtraCompounds();
    }

    private static void registerExtraCompounds() {
        // ─────────────────────────────────────────────────────────────────────
        // 14. BENZENE (C6H6) - Planar Aromatic Ring with Pi Resonance
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "C₆H₆", "Benzene", "Benzol / Aromatic Prototype", "78.114 g/mol", "Aromatic Hydrocarbon",
                "Planar Hexagonal Ring (120°)", "Trigonal Planar per Carbon", "sp²", "120.0°",
                "Aromatic Resonance Delocalized π-Bonds (Bond Order 1.5)", "Non-polar (Symmetric, μ = 0.00 D)", 0.00f, false,
                "London Dispersion & π-π Stacking",
                0.0f, 0.0f, 0.0f,
                "Six sp² carbon atoms arranged in a flat regular hexagon, each bonded to one hydrogen, with 6 delocalized π-electrons circulating in a continuous aromatic ring.",
                "Each carbon has 3 electron domains (2 C-C bonds and 1 C-H bond) adopting an exact 120° trigonal planar geometry within a flat plane.",
                "Delocalization of 6 π-electrons across all 6 carbons grants immense thermodynamic resonance energy (150 kJ/mol stabilization over theoretical 1,3,5-cyclohexatriene).",
                "Complete six-fold hexagonal symmetry cancels all C-H and C-C bond dipoles, resulting in μ = 0.00 D.",
                "Fundamental building block for polymers, pharmaceuticals, dyes, and petrochemical synthesis.",
                "3 C₂H₂ (g) → C₆H₆ (l)  [Catalytic Cyclotrimerization]",
                "Acetylene gas over transition metal catalyst at elevated temperature.",
                "Reorganization of three triple bonds into a conjugated cyclic aromatic ring.",
                "Liquid Benzene (C₆H₆).",
                "SAFETY: Known human carcinogen linked to leukemia; avoid vapor inhalation."
            );

            // 6 Ring Carbons
            float rC = 1.40f;
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i);
                float x = (float) (rC * Math.cos(angle));
                float y = (float) (rC * Math.sin(angle));
                m.addAtom(new Atom3D("C", "Carbon", 6, 12.011, "[He] 2s² 2p²", 4,
                    x, y, 0.0f, x * 1.1f, y * 1.1f, 70.0f, Atom3D.getCpkColor("C"),
                    "Aromatic Ring Carbon", 0, -0.15f));
            }

            // 6 Terminal Hydrogens
            float rH = 2.48f;
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i);
                float x = (float) (rH * Math.cos(angle));
                float y = (float) (rH * Math.sin(angle));
                m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                    x, y, 0.0f, x * 1.1f, y * 1.1f, 31.0f, Atom3D.getCpkColor("H"),
                    "Aromatic Terminal Hydrogen", 0, +0.15f));
            }

            // Ring Bonds (alternating double/single for 2D representation)
            for (int i = 0; i < 6; i++) {
                int next = (i + 1) % 6;
                int order = (i % 2 == 0) ? 2 : 1;
                m.addBond(new Bond3D(i, next, order, 1.40f, "Aromatic Delocalized C-C Bond"));
            }
            // C-H Bonds
            for (int i = 0; i < 6; i++) {
                m.addBond(new Bond3D(i, i + 6, 1, 1.08f, "C-H σ-bond"));
            }

            m.addEducationalItem("Why is benzene flat?",
                "All 6 carbon atoms are sp² hybridized with trigonal planar 120° bond angles. To allow the remaining unhybridized p-orbitals to overlap side-by-side and form a continuous delocalized π-cloud above and below the ring, all nuclei must lie in a single plane.");

            register("C6H6", m);
            register("BENZENE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 15. SULFUR HEXAFLUORIDE (SF6) - Octahedral 90°
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "SF₆", "Sulfur Hexafluoride", "SF6 / Heavy Dielectric Gas", "146.06 g/mol", "Hypervalent Covalent Gas",
                "Octahedral (90°)", "Octahedral", "sp³d²", "90.0°",
                "Polar Covalent S-F (ΔEN = 1.40)", "Non-polar (Symmetric, μ = 0.00 D)", 0.00f, false,
                "London Dispersion Forces",
                0.0f, 0.0f, 0.0f,
                "Central sulfur atom surrounded symmetrically by six fluorine atoms pointing toward the six vertices of an octahedron.",
                "VSEPR Repulsion: Six bonding pairs (0 lone pairs) experience minimum electrostatic repulsion when oriented in an octahedral geometry with 90° angles.",
                "Sulfur expands its valence octet using d-orbitals to accommodate 12 valence electrons in six equivalent sp³d² hybrid orbitals.",
                "Perfect octahedral symmetry causes all six intense S-F bond dipoles to cancel mutually, leaving zero net dipole moment.",
                "Potent electrical insulator in high-voltage circuit breakers and transformers; non-toxic and non-flammable; 5x denser than air.",
                "S₈ (s) + 24 F₂ (g) → 8 SF₆ (g)  [Exothermic Fluorination]",
                "Molten sulfur and fluorine gas.",
                "Vigorous direct fluorination followed by scrubbing to remove toxic disulfur decafluoride.",
                "Pure SF₆ gas.",
                "SAFETY: Heavy asphyxiant; potent greenhouse gas with GWP 23,500x that of CO₂."
            );

            // Sulfur Central
            m.addAtom(new Atom3D("S", "Sulfur", 16, 32.060, "[Ne] 3s² 3p⁴", 6,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 104.0f, Atom3D.getCpkColor("S"),
                "Central Hypervalent sp³d² Sulfur", 0, +1.20f));

            // 6 Fluorines
            float dF = 1.56f;
            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7,  dF, 0f, 0f,  1.7f, 0f, 57.0f, Atom3D.getCpkColor("F"), "Octahedral Vertex", 0, -0.20f));
            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7, -dF, 0f, 0f, -1.7f, 0f, 57.0f, Atom3D.getCpkColor("F"), "Octahedral Vertex", 0, -0.20f));
            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7, 0f,  dF, 0f, 0f,  1.7f, 57.0f, Atom3D.getCpkColor("F"), "Octahedral Vertex", 0, -0.20f));
            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7, 0f, -dF, 0f, 0f, -1.7f, 57.0f, Atom3D.getCpkColor("F"), "Octahedral Vertex", 0, -0.20f));
            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7, 0f, 0f,  dF,  1.2f,  1.2f, 57.0f, Atom3D.getCpkColor("F"), "Octahedral Vertex", 0, -0.20f));
            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7, 0f, 0f, -dF, -1.2f, -1.2f, 57.0f, Atom3D.getCpkColor("F"), "Octahedral Vertex", 0, -0.20f));

            for (int i = 1; i <= 6; i++) {
                m.addBond(new Bond3D(0, i, 1, 1.56f, "Polar Covalent S-F σ-bond"));
            }

            register("SF6", m);
            register("SULFUR HEXAFLUORIDE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 16. PHOSPHORUS PENTACHLORIDE (PCl5) - Trigonal Bipyramidal
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "PCl₅", "Phosphorus Pentachloride", "Phosphorus Pentachloride / Chlorinating Agent", "208.24 g/mol", "Hypervalent Trigonal Bipyramid",
                "Trigonal Bipyramidal (90° Axial / 120° Equatorial)", "Trigonal Bipyramidal", "sp³d", "90.0° / 120.0°",
                "Polar Covalent P-Cl (ΔEN = 0.97)", "Non-polar (Symmetric, μ = 0.00 D)", 0.00f, false,
                "London Dispersion Forces",
                0.0f, 0.0f, 0.0f,
                "Central phosphorus atom surrounded by 5 chlorine atoms: 3 in the equatorial plane (120°) and 2 in axial positions (180° to each other, 90° to equatorial).",
                "VSEPR Repulsion: Five electron pairs minimize repulsion in a trigonal bipyramid. The axial bonds experience greater 90° repulsion from 3 equatorial bonds, resulting in longer axial P-Cl bonds (214 pm) than equatorial bonds (202 pm).",
                "Phosphorus expands into empty 3d orbitals to form five sp³d hybrid orbitals.",
                "Equatorial dipoles cancel via 120° threefold symmetry; axial dipoles cancel along the linear vertical axis, yielding 0.00 D net dipole.",
                "Powerful chlorinating agent in organic synthesis for converting carboxylic acids to acyl chlorides and alcohols to alkyl chlorides.",
                "PCl₃ (l) + Cl₂ (g) ⇌ PCl₅ (s)  [ΔH° = -124 kJ/mol]",
                "Phosphorus trichloride liquid and chlorine gas.",
                "Reversible addition of chlorine across phosphorus lone pair.",
                "Yellow-green solid Phosphorus Pentachloride.",
                "SAFETY: Reacts violently with water releasing toxic HCl and phosphoric acid fumes."
            );

            // Central P
            m.addAtom(new Atom3D("P", "Phosphorus", 15, 30.974, "[Ne] 3s² 3p³", 5,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 107.0f, Atom3D.getCpkColor("P"),
                "Central sp³d Phosphorus", 0, +0.80f));

            // Axial Cl (Top & Bottom)
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                0.0f, 2.0f, 0.0f, 0.0f, 1.8f, 99.0f, Atom3D.getCpkColor("CL"), "Axial Ligand (90°)", 0, -0.16f));
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                0.0f, -2.0f, 0.0f, 0.0f, -1.8f, 99.0f, Atom3D.getCpkColor("CL"), "Axial Ligand (90°)", 0, -0.16f));

            // Equatorial Cl (3 at 120°)
            float rEq = 1.7f;
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                rEq, 0.0f, 0.0f, 1.6f, 0.0f, 99.0f, Atom3D.getCpkColor("CL"), "Equatorial Ligand (120°)", 0, -0.16f));
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                -0.85f, 0.0f, 1.47f, -1.3f, 0.8f, 99.0f, Atom3D.getCpkColor("CL"), "Equatorial Ligand (120°)", 0, -0.16f));
            m.addAtom(new Atom3D("Cl", "Chlorine", 17, 35.450, "[Ne] 3s² 3p⁵", 7,
                -0.85f, 0.0f, -1.47f, -1.3f, -0.8f, 99.0f, Atom3D.getCpkColor("CL"), "Equatorial Ligand (120°)", 0, -0.16f));

            for (int i = 1; i <= 5; i++) {
                m.addBond(new Bond3D(0, i, 1, (i <= 2 ? 2.14f : 2.02f), "Polar Covalent P-Cl σ-bond"));
            }

            register("PCL5", m);
            register("PHOSPHORUS PENTACHLORIDE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 17. SULFUR DIOXIDE (SO2) - Bent 119°
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "SO₂", "Sulfur Dioxide", "Sulfur Dioxide / Vitriol Gas", "64.066 g/mol", "Polar Bent Gas",
                "Bent (119.0°)", "Trigonal Planar", "sp²", "119.0°",
                "Polar Covalent S=O with Resonance", "Polar (Net Dipole μ = 1.63 D)", 1.63f, true,
                "Dipole-Dipole & London Dispersion",
                0.0f, 1.0f, 0.0f,
                "Central sulfur atom bonded to two oxygen atoms with an angle of 119° and one prominent lone pair on sulfur.",
                "VSEPR Repulsion: Sulfur has 3 electron domains (2 resonance bonds and 1 lone pair) in trigonal planar electron geometry. The lone pair repels the two S-O bonds, compressing the 120° angle slightly to 119°.",
                "Sulfur and oxygen participate in resonance between single and double bonds, maintaining an average bond order of 1.5.",
                "Asymmetric bent shape prevents bond dipole cancellation, resulting in a net dipole moment of 1.63 D pointing toward the oxygens.",
                "Precursor to sulfuric acid via contact process; food preservative (E220); bleaching agent.",
                "S (s) + O₂ (g) → SO₂ (g)  [ΔH° = -296.8 kJ/mol]",
                "Sulfur and oxygen gas.",
                "Combustion of elemental sulfur with brilliant blue flame.",
                "Sulfur Dioxide gas.",
                "SAFETY: Toxic gas with suffocating odor; major cause of acid rain."
            );

            m.addAtom(new Atom3D("S", "Sulfur", 16, 32.060, "[Ne] 3s² 3p⁴", 6,
                0.0f, 0.2f, 0.0f, 0.0f, 0.2f, 104.0f, Atom3D.getCpkColor("S"),
                "Central sp² Sulfur", +1, +0.60f));

            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                1.25f, -0.5f, 0.0f, 1.5f, -0.6f, 66.0f, Atom3D.getCpkColor("O"), "Terminal Oxygen", 0, -0.30f));
            m.addAtom(new Atom3D("O", "Oxygen", 8, 15.999, "1s² 2s² 2p⁴", 6,
                -1.25f, -0.5f, 0.0f, -1.5f, -0.6f, 66.0f, Atom3D.getCpkColor("O"), "Terminal Oxygen", 0, -0.30f));

            m.addBond(new Bond3D(0, 1, 2, 1.43f, "Resonant Covalent S=O Bond"));
            m.addBond(new Bond3D(0, 2, 2, 1.43f, "Resonant Covalent S=O Bond"));
            m.addLonePair(new LonePair3D(0, 270.0f, 0.0f, 0.8f, 0.0f));

            register("SO2", m);
            register("SULFUR DIOXIDE", m);
        }

        // ─────────────────────────────────────────────────────────────────────
        // 18. HYDROGEN FLUORIDE (HF) - Linear Diatomic
        // ─────────────────────────────────────────────────────────────────────
        {
            MolecularStructureData m = new MolecularStructureData(
                "HF", "Hydrogen Fluoride", "Hydrofluoric Acid / Etching Acid", "20.006 g/mol", "Extreme Hydrogen-Bonded Acid",
                "Linear (Diatomic)", "Linear", "None (Diatomic σ)", "180.0°",
                "Polar Covalent H-F (ΔEN = 1.78)", "Polar (Net Dipole μ = 1.82 D)", 1.82f, true,
                "Exceptionally Strong Intermolecular Hydrogen Bonding",
                1.0f, 0.0f, 0.0f,
                "Diatomic molecule: one hydrogen atom single-bonded to the most electronegative element, fluorine.",
                "Diatomic geometry is strictly linear with a short 91.7 pm bond length.",
                "Large electronegativity difference (ΔEN = 1.78) creates an intense partial negative charge on fluorine and nearly bare proton on hydrogen.",
                "Net dipole moment is 1.82 Debye, forming robust zigzag hydrogen-bonded chains in liquid and gas phases.",
                "Essential for etching silicon dioxide in semiconductor chips, manufacturing Teflon, and refining uranium.",
                "CaF₂ (s) + H₂SO₄ (l) → CaSO₄ (s) + 2 HF (g)",
                "Fluorite mineral and sulfuric acid in heated kiln.",
                "Protonation and distillation of HF vapor.",
                "Hydrogen Fluoride gas / liquid.",
                "SAFETY: Extremely hazardous contact poison; penetrates skin to precipitate calcium in bone (hypocalcemia). Requires immediate calcium gluconate antidote."
            );

            m.addAtom(new Atom3D("H", "Hydrogen", 1, 1.008, "1s¹", 1,
                -0.46f, 0.0f, 0.0f, -0.9f, 0.0f, 31.0f, Atom3D.getCpkColor("H"), "Electropositive Proton", 0, +0.42f));
            m.addAtom(new Atom3D("F", "Fluorine", 9, 18.998, "[He] 2s² 2p⁵", 7,
                0.46f, 0.0f, 0.0f, 0.9f, 0.0f, 57.0f, Atom3D.getCpkColor("F"), "Electronegative Fluorine", 0, -0.42f));

            m.addBond(new Bond3D(0, 1, 1, 0.92f, "Intensely Polar H-F σ-bond"));
            m.addLonePair(new LonePair3D(1, 0.0f, 1.0f, 0.0f, 0.0f));
            m.addLonePair(new LonePair3D(1, 90.0f, 0.6f, 0.5f, 0.0f));
            m.addLonePair(new LonePair3D(1, 270.0f, 0.6f, -0.5f, 0.0f));

            register("HF", m);
            register("HYDROGEN FLUORIDE", m);
            register("HYDROFLUORIC ACID", m);
        }
    }
}
