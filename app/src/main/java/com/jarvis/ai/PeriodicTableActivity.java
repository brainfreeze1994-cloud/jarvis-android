package com.jarvis.ai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Stark Chemical Synthesizer & Holographic Periodic Matrix.
 * Inspired by Tony Stark's Iron Man 2 workshop holographic periodic table.
 *
 * Features:
 *  - 18-Column Interactive Holographic Matrix (All 118 Elements)
 *  - Arc Reactor / Particle Accelerator New Element Synthesizer
 *  - Chemical Compound Combiner & Reaction Vessel
 *  - Interactive Bohr Atomic Orbital Simulator (K, L, M, N... shells)
 *  - Real-Time Search & Category Filters
 *  - J.A.R.V.I.S. / H.E.N.R.Y. Voice Intel Diagnostics
 */
public class PeriodicTableActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int CELL = 52; // dp per element tile
    private static final int TAP_THRESHOLD_DP = 8;

    private TextView tvLabTitle, tvLabSubtitle, btnBack, btnVoiceInspect, tvMatrixStatus, tvElementCounter;
    private TextView tabMatrix, tabMixer, tabBohr;
    private View panelMatrix, panelMixer, panelBohr;
    private View searchFilterBar, cardMiniInspect;
    private EditText etSearch;
    private TextView btnClearSearch;
    private LinearLayout categoryChipsContainer;
    private FrameLayout tableLayer;

    // Mini inspect bar views
    private TextView miniZNum, miniSymbol, miniName, miniEconfig, btnInspectFull;
    private LinearLayout miniTilePreview;

    // Mixer views
    private LinearLayout selectedElementsContainer;
    private TextView btnAddElement, btnClearElements;
    private LinearLayout cardMultipleCompounds, multipleCompoundsChips;
    private TextView tvMultipleCompoundsTitle;
    private LinearLayout cardMixerResult;
    private TextView tvCompoundFormula, tvCompoundCommon, tvCompoundStructure, tvCompoundBond, tvCompoundSynthesis, tvCompoundUses;
    private LinearLayout commonCompoundsContainer;
    private TextView btnNovelChemical;
    private LinearLayout novelChemicalOutput;
    private TextView tvNovelName, tvNovelFormula, tvNovelStructure, tvNovelSynthesis, tvNovelUses;

    private final List<Elem> selectedReactantElements = new ArrayList<>();
    private final List<Compound> matchingCompounds = new ArrayList<>();
    private Compound selectedCompound = null;

    // Bohr views
    private BohrAtomView bohrAtomCanvas;
    private TextView tvBohrElementTitle, tvBohrEconfig, tvBohrShellsDetail, tvBohrValence, tvBohrSummary;

    private final List<ElemView> elemViews = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private Elem selectedElement;
    private String selectedCategory = "ALL";

    private enum Mode { MATRIX, MIXER, BOHR }
    private Mode currentMode = Mode.MATRIX;

    // ── Data Models ──────────────────────────────────────────────────────────
    static class Elem {
        String symbol, name, color, econfig, summary, image, uses, category, weight;
        int number, row, col;

        Elem(String s, String n, int num, String c, int r, int cl,
             String econfig, String summary, String image, String uses, String category, String weight) {
            this.symbol = s; this.name = n; this.number = num; this.color = c; this.row = r; this.col = cl;
            this.econfig = econfig; this.summary = summary; this.image = image; this.uses = uses;
            this.category = category; this.weight = weight;
        }
    }

    static class Compound {
        String[] requiredElements;
        String formula, name, common, structure, bond, synthesis, uses, category;

        Compound(String[] req, String f, String n, String c, String st, String b, String syn, String u, String cat) {
            this.requiredElements = req; this.formula = f; this.name = n; this.common = c;
            this.structure = st; this.bond = b; this.synthesis = syn; this.uses = u; this.category = cat;
        }

        boolean canFormFrom(List<Elem> elements) {
            Set<String> pool = new HashSet<>();
            for (Elem e : elements) pool.add(e.symbol.toUpperCase(Locale.US));
            for (String req : requiredElements) {
                if (!pool.contains(req.toUpperCase(Locale.US))) return false;
            }
            return true;
        }
    }

    private static final Elem[] ELEMENTS = {
            new Elem("H","Hydrogen",1,"#69DB7C",1,1,"1s1","Hydrogen is the lightest and most abundant chemical element in the universe, powering stellar fusion.","https://upload.wikimedia.org/wikipedia/commons/d/d9/Hydrogenglow.jpg","Rocket fuel, ammonia production, clean fuel cells","Reactive Nonmetal","1.008 u"),
            new Elem("He","Helium",2,"#845EF7",1,18,"1s2","Helium is a colorless, odorless, non-toxic, inert noble gas with the lowest boiling point of all elements.","https://upload.wikimedia.org/wikipedia/commons/0/00/Helium-glow.jpg","MRI magnet cooling, deep-sea diving gas mixes, balloons","Noble Gas","4.0026 u"),
            new Elem("Li","Lithium",3,"#FF6B6B",2,1,"[He] 2s1","Lithium is the lightest metal and least dense solid element, highly reactive in water.","https://upload.wikimedia.org/wikipedia/commons/e/e2/0.5_grams_lithium_under_argon.jpg","EV batteries, energy storage, ceramics, mood stabilization","Alkali Metal","6.94 u"),
            new Elem("Be","Beryllium",4,"#FFA94D",2,2,"[He] 2s2","Beryllium is a lightweight, rigid alkaline earth metal created through stellar nucleosynthesis.","https://upload.wikimedia.org/wikipedia/commons/e/e2/Beryllium_%28Be%29.jpg","James Webb space telescope mirrors, aerospace alloys","Alkaline Earth","9.0122 u"),
            new Elem("B","Boron",5,"#FFD43B",2,13,"[He] 2s2 2p1","Boron is a versatile metalloid used in heat-resistant borosilicate glass and bulletproof armor.","https://upload.wikimedia.org/wikipedia/commons/a/a2/Boron.jpg","Pyrex glass, semiconductors, fiberglass, armor plating","Metalloid","10.81 u"),
            new Elem("C","Carbon",6,"#69DB7C",2,14,"[He] 2s2 2p2","Carbon forms the chemical backbone of all organic life and exhibits allotropes from graphite to diamond.","https://upload.wikimedia.org/wikipedia/commons/6/68/Pure_Carbon.png","Carbon fiber armor, diamond optics, steel, nanotechnology","Reactive Nonmetal","12.011 u"),
            new Elem("N","Nitrogen",7,"#69DB7C",2,15,"[He] 2s2 2p3","Nitrogen makes up 78% of Earth's atmosphere and is vital for amino acids and agricultural fertilizer.","https://upload.wikimedia.org/wikipedia/commons/2/2d/Nitrogen-glow.jpg","Cryogenic cooling, fertilizer synthesis, food preservation","Reactive Nonmetal","14.007 u"),
            new Elem("O","Oxygen",8,"#69DB7C",2,16,"[He] 2s2 2p4","Oxygen is a highly reactive oxidizing agent essential for aerobic respiration and combustion.","https://upload.wikimedia.org/wikipedia/commons/a/a0/Liquid_oxygen_in_a_beaker_%28cropped_and_retouched%29.jpg","Rocket propulsion, life support, steel manufacturing","Reactive Nonmetal","15.999 u"),
            new Elem("F","Fluorine",9,"#FF922B",2,17,"[He] 2s2 2p5","Fluorine is the most electronegative and chemically reactive of all elements.","https://upload.wikimedia.org/wikipedia/commons/2/2c/Fluoro_liquido_a_-196%C2%B0C_1.jpg","Teflon polymers, uranium enrichment, pharmaceuticals","Halogen","18.998 u"),
            new Elem("Ne","Neon",10,"#845EF7",2,18,"[He] 2s2 2p6","Neon glows with an intense reddish-orange light when excited in high-voltage electrical fields.","https://upload.wikimedia.org/wikipedia/commons/f/f8/Neon-glow.jpg","High-voltage indicators, cryogenic refrigeration, signage","Noble Gas","20.180 u"),
            new Elem("Na","Sodium",11,"#FF6B6B",3,1,"[Ne] 3s1","Sodium is a soft, silvery-white alkali metal that reacts vigorously with water.","https://upload.wikimedia.org/wikipedia/commons/2/27/Na_%28Sodium%29.jpg","Table salt (NaCl), coolant in fast nuclear reactors, streetlights","Alkali Metal","22.990 u"),
            new Elem("Mg","Magnesium",12,"#FFA94D",3,2,"[Ne] 3s2","Magnesium is a lightweight structural metal that burns with a brilliant white flame.","https://upload.wikimedia.org/wikipedia/commons/3/3f/Magnesium_crystals.jpg","Lightweight racing alloys, flares, aerospace structural parts","Alkaline Earth","24.305 u"),
            new Elem("Al","Aluminium",13,"#9775FA",3,13,"[Ne] 3s2 3p1","Aluminium is a lightweight, corrosion-resistant post-transition metal with high conductivity.","https://upload.wikimedia.org/wikipedia/commons/3/3e/Aluminium.jpg","Aircraft fuselages, electrical power grids, armor plating","Post-Transition Metal","26.982 u"),
            new Elem("Si","Silicon",14,"#FFD43B",3,14,"[Ne] 3s2 3p2","Silicon is the foundational semiconductor of all modern computing and solar microchips.","https://upload.wikimedia.org/wikipedia/commons/2/2c/Silicon.jpg","Microprocessors, photovoltaic solar cells, silicone polymers","Metalloid","28.085 u"),
            new Elem("P","Phosphorus",15,"#69DB7C",3,15,"[Ne] 3s2 3p3","Phosphorus is an essential building block of DNA, RNA, ATP energy transfer, and cellular membranes.","https://upload.wikimedia.org/wikipedia/commons/6/6d/Phosphorus-purple.jpg","Fertilizers, matches, LEDs, steel manufacturing","Reactive Nonmetal","30.974 u"),
            new Elem("S","Sulfur",16,"#69DB7C",3,16,"[Ne] 3s2 3p4","Sulfur is a bright yellow nonmetal used in sulfuric acid synthesis and rubber vulcanization.","https://upload.wikimedia.org/wikipedia/commons/2/23/Native_sulfur_%28Vodinskoe_Deposit%3B_quarry_near_Samara%2C_Russia%29_9.jpg","Sulfuric acid, battery electrolytes, rubber hardening","Reactive Nonmetal","32.06 u"),
            new Elem("Cl","Chlorine",17,"#FF922B",3,17,"[Ne] 3s2 3p5","Chlorine is a yellow-green halogen gas used universally for water purification and plastics.","https://upload.wikimedia.org/wikipedia/commons/9/9a/Chlorine-sample-flip.jpg","Water disinfection, PVC polymers, disinfectants","Halogen","35.45 u"),
            new Elem("Ar","Argon",18,"#845EF7",3,18,"[Ne] 3s2 3p6","Argon is an inert shielding noble gas preventing oxidation in high-energy arc welding.","https://upload.wikimedia.org/wikipedia/commons/5/53/Argon-glow.jpg","TIG welding shields, laser optics, incandescent lighting","Noble Gas","39.948 u"),
            new Elem("K","Potassium",19,"#FF6B6B",4,1,"[Ar] 4s1","Potassium is an alkali metal critical for neural electrochemistry and cardiac function.","https://upload.wikimedia.org/wikipedia/commons/b/b3/Potassium.JPG","Electrolytes, fertilizers, heat transfer fluids","Alkali Metal","39.098 u"),
            new Elem("Ca","Calcium",20,"#FFA94D",4,2,"[Ar] 4s2","Calcium is the structural mineral in biological skeletons and architectural concrete.","https://upload.wikimedia.org/wikipedia/commons/7/72/Calcium.jpg","Cement, structural bones, steel refining","Alkaline Earth","40.078 u"),
            new Elem("Sc","Scandium",21,"#4DABF7",4,3,"[Ar] 3d1 4s2","Scandium strengthens aluminum alloys dramatically for supersonic aerospace engineering.","https://upload.wikimedia.org/wikipedia/commons/f/f5/Scandium%2C_Sc.jpg","Fighter jet airframes, high-intensity stadium lights","Transition Metal","44.956 u"),
            new Elem("Ti","Titanium",22,"#4DABF7",4,4,"[Ar] 3d2 4s2","Titanium has the highest strength-to-density ratio of any metallic element; immune to sea-water corrosion.","https://upload.wikimedia.org/wikipedia/commons/e/ec/Titanium.jpg","Iron Man armor plating, aerospace turbines, surgical implants","Transition Metal","47.867 u"),
            new Elem("V","Vanadium",23,"#4DABF7",4,5,"[Ar] 3d3 4s2","Vanadium creates ultra-tough steel alloys and redox flow batteries for grid-scale power.","https://upload.wikimedia.org/wikipedia/commons/0/0a/Vanadium-pieces.jpg","Grid-scale redox batteries, high-speed tool steel","Transition Metal","50.942 u"),
            new Elem("Cr","Chromium",24,"#4DABF7",4,6,"[Ar] 3d5 4s1","Chromium gives stainless steel its mirror-like corrosion resistance and hardness.","https://upload.wikimedia.org/wikipedia/commons/a/a1/Chromium.jpg","Stainless steel, chrome plating, tanning, dye pigments","Transition Metal","51.996 u"),
            new Elem("Mn","Manganese",25,"#4DABF7",4,7,"[Ar] 3d5 4s2","Manganese removes sulfur during iron smelting to forge high-tensile impact steel.","https://upload.wikimedia.org/wikipedia/commons/6/64/Manganese_element.jpg","High-impact steel, alkaline batteries, cathode materials","Transition Metal","54.938 u"),
            new Elem("Fe","Iron",26,"#4DABF7",4,8,"[Ar] 3d6 4s2","Iron forms much of Earth's outer and inner core, hemoglobin in blood, and global civil infrastructure.","https://images-of-elements.com/iron-2.jpg","Steel frameworks, magnets, hemoglobin blood oxygenation","Transition Metal","55.845 u"),
            new Elem("Co","Cobalt",27,"#4DABF7",4,9,"[Ar] 3d7 4s2","Cobalt is essential for ultra-high energy density lithium battery cathodes and magnetic alloys.","https://upload.wikimedia.org/wikipedia/commons/6/62/Cobalt_ore_2.jpg","EV battery cathodes, samarium-cobalt magnets, jet engines","Transition Metal","58.933 u"),
            new Elem("Ni","Nickel",28,"#4DABF7",4,10,"[Ar] 3d8 4s2","Nickel resists corrosion at extreme temperatures, forming the base of turbine superalloys.","https://upload.wikimedia.org/wikipedia/commons/5/57/Nickel_chunk.jpg","Turbine superalloys, rechargeable batteries, electroplating","Transition Metal","58.693 u"),
            new Elem("Cu","Copper",29,"#4DABF7",4,11,"[Ar] 3d10 4s1","Copper delivers extraordinary thermal and electrical conductivity throughout power grids.","https://upload.wikimedia.org/wikipedia/commons/f/f0/NatCopper.jpg","Electric motor windings, PCB traces, power transmission","Transition Metal","63.546 u"),
            new Elem("Zn","Zinc",30,"#4DABF7",4,12,"[Ar] 3d10 4s2","Zinc protects steel from rust via galvanic sacrificial anodes and zinc-air batteries.","https://upload.wikimedia.org/wikipedia/commons/b/ba/Zinc_%2830_Zn%29.jpg","Galvanizing, die-casting, sunscreen optics, batteries","Transition Metal","65.38 u"),
            new Elem("Ga","Gallium",31,"#9775FA",4,13,"[Ar] 3d10 4s2 4p1","Gallium melts in the palm of your hand (29.7°C) and powers Gallium-Nitride (GaN) high-speed microchips.","https://upload.wikimedia.org/wikipedia/commons/b/b1/Solid_gallium_%28Ga%29.jpg","GaN power semiconductors, radar systems, lasers","Post-Transition Metal","69.723 u"),
            new Elem("Ge","Germanium",32,"#FFD43B",4,14,"[Ar] 3d10 4s2 4p2","Germanium is transparent to infrared radiation, ideal for night-vision optics and fiber communications.","https://upload.wikimedia.org/wikipedia/commons/0/08/Polycrystalline-germanium.jpg","Infrared night vision, fiber optic cables, solar cells","Metalloid","72.630 u"),
            new Elem("As","Arsenic",33,"#FFD43B",4,15,"[Ar] 3d10 4s2 4p3","Arsenic is a semiconductor dopant used in high-efficiency Gallium-Arsenide satellite solar arrays.","https://upload.wikimedia.org/wikipedia/commons/3/3b/Arsenic_%2833_As%29.jpg","GaAs solar panels, laser diodes, alloy hardening","Metalloid","74.922 u"),
            new Elem("Se","Selenium",34,"#69DB7C",4,16,"[Ar] 3d10 4s2 4p4","Selenium exhibits photoconductivity, generating electric current when exposed to light.","https://upload.wikimedia.org/wikipedia/commons/7/7f/Selenium.jpg","Photocopiers, glass decoloring, solar cells","Reactive Nonmetal","78.971 u"),
            new Elem("Br","Bromine",35,"#FF922B",4,17,"[Ar] 3d10 4s2 4p5","Bromine is one of only two liquid elements at standard room temperature, dense and reddish-brown.","https://upload.wikimedia.org/wikipedia/commons/8/87/Bromine-ampoule.jpg","Flame retardants, water treatment, pharmaceuticals","Halogen","79.904 u"),
            new Elem("Kr","Krypton",36,"#845EF7",4,18,"[Ar] 3d10 4s2 4p6","Krypton is a dense noble gas used in high-speed flash photography and Ion propulsion thrusters.","https://upload.wikimedia.org/wikipedia/commons/9/9c/Krypton-glow.jpg","Satellite ion thrusters, insulated windows, laser surgery","Noble Gas","83.798 u"),
            new Elem("Rb","Rubidium",37,"#FF6B6B",5,1,"[Kr] 5s1","Rubidium vapor powers laser-cooled atomic clocks and quantum magnetometer navigation.","https://upload.wikimedia.org/wikipedia/commons/c/c9/Rb5.JPG","Quantum atomic clocks, GPS satellites, photocells","Alkali Metal","85.468 u"),
            new Elem("Sr","Strontium",38,"#FFA94D",5,2,"[Kr] 5s2","Strontium produces brilliant crimson flames and powers ultra-precise optical lattice atomic clocks.","https://upload.wikimedia.org/wikipedia/commons/8/84/Strontium-1.jpg","Optical atomic clocks, fireworks, ferrite magnets","Alkaline Earth","87.62 u"),
            new Elem("Y","Yttrium",39,"#4DABF7",5,3,"[Kr] 4d1 5s2","Yttrium is a rare-earth metal that enables high-temperature YBCO superconductors.","https://upload.wikimedia.org/wikipedia/commons/9/90/Piece_of_Yttrium.jpg","YBCO superconductors, LED phosphors, laser crystals","Transition Metal","88.906 u"),
            new Elem("Zr","Zirconium",40,"#4DABF7",5,4,"[Kr] 4d2 5s2","Zirconium has exceptional resistance to neutron absorption, making it the premier nuclear fuel cladding.","https://upload.wikimedia.org/wikipedia/commons/1/1d/Zirconium-pieces.jpg","Nuclear submarine reactor cladding, surgical ceramics","Transition Metal","91.224 u"),
            new Elem("Nb","Niobium",41,"#4DABF7",5,5,"[Kr] 4d4 5s1","Niobium is a premier superconductor metal used in particle accelerators and MRI magnetic coils.","https://upload.wikimedia.org/wikipedia/commons/c/c2/Niobium_strips.JPG","Superconducting magnets (LHC), rocket engine nozzles","Transition Metal","92.906 u"),
            new Elem("Mo","Molybdenum",42,"#4DABF7",5,6,"[Kr] 4d5 5s1","Molybdenum retains extreme tensile strength at temperatures exceeding 1000°C.","https://upload.wikimedia.org/wikipedia/commons/f/f0/Molybdenum.jpg","Missile armor, high-temperature electrodes, catalysts","Transition Metal","95.95 u"),
            new Elem("Tc","Technetium",43,"#4DABF7",5,7,"[Kr] 4d5 5s2","Technetium is the lowest atomic number element with no stable isotopes, widely used in nuclear medicine.","https://upload.wikimedia.org/wikipedia/commons/a/ab/Technetium-sample-cropped.jpg","Medical radioisotope scans (Tc-99m), cancer imaging","Transition Metal","98 u"),
            new Elem("Ru","Ruthenium",44,"#4DABF7",5,8,"[Kr] 4d7 5s1","Ruthenium hardens platinum and palladium alloys and serves as a crucial solar dye catalyst.","https://upload.wikimedia.org/wikipedia/commons/a/a8/Ruthenium_crystal.jpg","Hard disk drive coatings, microelectronics contacts","Transition Metal","101.07 u"),
            new Elem("Rh","Rhodium",45,"#4DABF7",5,9,"[Kr] 4d8 5s1","Rhodium is an extraordinarily rare, corrosion-resistant platinum group precious metal.","https://upload.wikimedia.org/wikipedia/commons/5/54/Rhodium_%28Rh%29.jpg","Automotive catalytic scrubbers, optical searchlight mirrors","Transition Metal","102.91 u"),
            new Elem("Pd","Palladium",46,"#4DABF7",5,10,"[Kr] 4d10","Palladium can absorb up to 900 times its own volume of hydrogen gas; used in Tony's original Arc Reactor.","https://upload.wikimedia.org/wikipedia/commons/d/d7/Palladium_%2846_Pd%29.jpg","Hydrogen storage, catalytic converters, Arc Reactor Mark I-II","Transition Metal","106.42 u"),
            new Elem("Ag","Silver",47,"#4DABF7",5,11,"[Kr] 4d10 5s1","Silver has the highest electrical conductivity, thermal conductivity, and optical reflectivity of all metals.","https://upload.wikimedia.org/wikipedia/commons/e/e4/Silver-nugget.jpg","Super-conductive circuit boards, solar panels, mirrors","Transition Metal","107.87 u"),
            new Elem("Cd","Cadmium",48,"#4DABF7",5,12,"[Kr] 4d10 5s2","Cadmium is a soft toxic metal used in nickel-cadmium batteries and nuclear reactor control rods.","https://images-of-elements.com/cadmium-4.jpg","Nuclear control rods, solar cell coatings, batteries","Transition Metal","112.41 u"),
            new Elem("In","Indium",49,"#9775FA",5,13,"[Kr] 4d10 5s2 5p1","Indium Tin Oxide (ITO) forms the transparent conductive layer in all smartphone touchscreens.","https://images-of-elements.com/indium-2.jpg","Touchscreens, OLED holographic displays, cryogenic seals","Post-Transition Metal","114.82 u"),
            new Elem("Sn","Tin",50,"#9775FA",5,14,"[Kr] 4d10 5s2 5p2","Tin is a malleable post-transition metal essential for soldering microelectronic circuit boards.","https://upload.wikimedia.org/wikipedia/commons/6/6a/Tin-2.jpg","Microelectronic solder, bronze alloys, corrosion coatings","Post-Transition Metal","118.71 u"),
            new Elem("Sb","Antimony",51,"#FFD43B",5,15,"[Kr] 4d10 5s2 5p3","Antimony expands upon cooling, allowing precision casting of ballistic armor alloys.","https://upload.wikimedia.org/wikipedia/commons/5/5c/Antimony-4.jpg","Infrared detectors, flame-retardants, semiconductor diodes","Metalloid","121.76 u"),
            new Elem("Te","Tellurium",52,"#FFD43B",5,16,"[Kr] 4d10 5s2 5p4","Tellurium forms high-efficiency Cadmium-Telluride thin-film photovoltaic solar matrices.","https://upload.wikimedia.org/wikipedia/commons/c/c1/Tellurium2.jpg","Thermoelectric cooling devices, CdTe solar modules","Metalloid","127.60 u"),
            new Elem("I","Iodine",53,"#FF922B",5,17,"[Kr] 4d10 5s2 5p5","Iodine sublimes into a deep purple vapor, crucial for thyroid metabolism and medical antiseptics.","https://upload.wikimedia.org/wikipedia/commons/c/c2/Iodine-sample.jpg","Radioactive shielding blockers, disinfectants, polarizers","Halogen","126.90 u"),
            new Elem("Xe","Xenon",54,"#845EF7",5,18,"[Kr] 4d10 5s2 5p6","Xenon is a heavy noble gas used as propellant in NASA Deep Space Ion Engines.","https://upload.wikimedia.org/wikipedia/commons/5/5d/Xenon-glow.jpg","Deep space ion propulsion engines, excimer lasers","Noble Gas","131.29 u"),
            new Elem("Cs","Cesium",55,"#FF6B6B",6,1,"[Xe] 6s1","Cesium vibrations (9,192,631,770 Hz) define the international SI standard second.","https://upload.wikimedia.org/wikipedia/commons/3/3d/Cesium.jpg","SI International standard atomic clocks, GPS time calibration","Alkali Metal","132.91 u"),
            new Elem("Ba","Barium",56,"#FFA94D",6,2,"[Xe] 6s2","Barium absorbs X-rays efficiently and creates brilliant green fireworks and high-temperature superconductors.","https://upload.wikimedia.org/wikipedia/commons/f/f5/Barium_%2856_Ba%29.jpg","Medical gastrointestinal imaging, spark-plug alloys","Alkaline Earth","137.33 u"),
            new Elem("La","Lanthanum",57,"#63E6BE",6,3,"[Xe] 5d1 6s2","Lanthanum is the prototype of the rare-earth series, used in high-refractive optics and hybrid battery cathodes.","https://upload.wikimedia.org/wikipedia/commons/f/f7/Lanthanum.jpg","Studio camera optics, hybrid car nickel-metal batteries","Lanthanide","138.91 u"),
            new Elem("Ce","Cerium",58,"#63E6BE",9,4,"[Xe] 4f1 5d1 6s2","Cerium is the most abundant rare earth, used in self-cleaning ovens and catalytic converters.","https://upload.wikimedia.org/wikipedia/commons/0/0d/Cerium2.jpg","Catalytic scrubbers, precision glass polishing","Lanthanide","140.12 u"),
            new Elem("Pr","Praseodymium",59,"#63E6BE",9,5,"[Xe] 4f3 6s2","Praseodymium creates strong magnets and provides yellow-green color filtration for welder goggles.","https://upload.wikimedia.org/wikipedia/commons/c/c7/Praseodymium.jpg","Aircraft engine alloys, didymium welder eye filters","Lanthanide","140.91 u"),
            new Elem("Nd","Neodymium",60,"#63E6BE",9,6,"[Xe] 4f4 6s2","Neodymium produces the world's strongest permanent magnets (NdFeB), powering EV electric motors.","https://upload.wikimedia.org/wikipedia/commons/c/c9/Neodymium_%2860_Nd%29.jpg","High-torque EV drive motors, wind turbine generators","Lanthanide","144.24 u"),
            new Elem("Pm","Promethium",61,"#63E6BE",9,7,"[Xe] 4f5 6s2","Promethium is a radioactive rare earth powering miniature nuclear batteries for guided space probes.","https://upload.wikimedia.org/wikipedia/commons/5/5b/Promethium.jpg","Nuclear micro-batteries, guided space telemetry power","Lanthanide","145 u"),
            new Elem("Sm","Samarium",62,"#63E6BE",9,8,"[Xe] 4f6 6s2","Samarium-cobalt magnets retain their magnetic strength at temperatures exceeding 300°C in jet engines.","https://upload.wikimedia.org/wikipedia/commons/8/88/Samarium-2.jpg","Missile guidance actuators, high-temp jet magnets","Lanthanide","150.36 u"),
            new Elem("Eu","Europium",63,"#63E6BE",9,9,"[Xe] 4f7 6s2","Europium phosphors emit vibrant red and blue fluorescence in banknote security and TV displays.","https://upload.wikimedia.org/wikipedia/commons/6/6a/Europium.jpg","Anti-counterfeiting banknotes, Quantum memory dots","Lanthanide","151.96 u"),
            new Elem("Gd","Gadolinium",64,"#63E6BE",9,10,"[Xe] 4f7 5d1 6s2","Gadolinium exhibits strong paramagnetic properties, used as an MRI contrast agent.","https://upload.wikimedia.org/wikipedia/commons/c/c2/Gadolinium-2.jpg","MRI intravenous contrast agents, neutron radiography","Lanthanide","157.25 u"),
            new Elem("Tb","Terbium",65,"#63E6BE",9,11,"[Xe] 4f9 6s2","Terbium forms Terfenol-D, an alloy that physically changes shape under magnetic fields (magnetostriction).","https://upload.wikimedia.org/wikipedia/commons/9/9a/Terbium-2.jpg","Sonar transducers, green fluorescent phosphors","Lanthanide","158.93 u"),
            new Elem("Dy","Dysprosium",66,"#63E6BE",9,12,"[Xe] 4f10 6s2","Dysprosium prevents demagnetization of EV electric motors during extreme thermal acceleration.","https://upload.wikimedia.org/wikipedia/commons/5/55/Dysprosium-2.jpg","High-performance EV motors, nuclear control rods","Lanthanide","162.50 u"),
            new Elem("Ho","Holmium",67,"#63E6BE",9,13,"[Xe] 4f11 6s2","Holmium has the highest magnetic moment of any element, concentrating magnetic flux lines.","https://upload.wikimedia.org/wikipedia/commons/0/0a/Holmium2.jpg","Holmium-YAG medical surgical lasers, pole pieces","Lanthanide","164.93 u"),
            new Elem("Er","Erbium",68,"#63E6BE",9,14,"[Xe] 4f12 6s2","Erbium amplifies optical signals in transoceanic fiber-optic telecommunication cables (EDFA).","https://upload.wikimedia.org/wikipedia/commons/2/2a/Erbium-2.jpg","Transoceanic optical fiber amplifiers, surgical lasers","Lanthanide","167.26 u"),
            new Elem("Tm","Thulium",69,"#63E6BE",9,15,"[Xe] 4f13 6s2","Thulium is one of the rarest lanthanides, used in portable X-ray emitters that require no power supply.","https://upload.wikimedia.org/wikipedia/commons/6/6b/Thulium-2.jpg","Portable battlefield X-ray units, fiber lasers","Lanthanide","168.93 u"),
            new Elem("Yb","Ytterbium",70,"#63E6BE",9,16,"[Xe] 4f14 6s2","Ytterbium fiber lasers can cut through dense armor plates with micron precision.","https://upload.wikimedia.org/wikipedia/commons/c/ce/Ytterbium-3.jpg","High-energy industrial cutting lasers, atomic clocks","Lanthanide","173.05 u"),
            new Elem("Lu","Lutetium",71,"#63E6BE",9,17,"[Xe] 4f14 5d1 6s2","Lutetium is the heaviest and hardest lanthanide, used in PET scan scintillation detectors.","https://upload.wikimedia.org/wikipedia/commons/e/e8/Lutetium.jpg","PET cancer scanners, petroleum cracking catalysts","Lanthanide","174.97 u"),
            new Elem("Hf","Hafnium",72,"#4DABF7",6,4,"[Xe] 4f14 5d2 6s2","Hafnium dioxide replaces silicon dioxide as the high-k dielectric gate in advanced microprocessors.","https://upload.wikimedia.org/wikipedia/commons/1/17/Hafnium_%2872_Hf%29.jpg","Sub-3nm microchips, nuclear submarine control rods","Transition Metal","178.49 u"),
            new Elem("Ta","Tantalum",73,"#4DABF7",6,5,"[Xe] 4f14 5d3 6s2","Tantalum capacitors store electrical charge in compact volumes for aerospace avionics and smartphones.","https://upload.wikimedia.org/wikipedia/commons/6/61/Tantalum.jpg","Avionics capacitors, surgical bone pins, superalloys","Transition Metal","180.95 u"),
            new Elem("W","Tungsten",74,"#4DABF7",6,6,"[Xe] 4f14 5d4 6s2","Tungsten has the highest melting point of all metals (3,422°C), ideal for kinetic orbital penetrators.","https://upload.wikimedia.org/wikipedia/commons/c/c8/Tungsten_rod_with_oxidised_surface.jpg","Kinetic penetrators, rocket nozzles, armor-piercing sabots","Transition Metal","183.84 u"),
            new Elem("Re","Rhenium",75,"#4DABF7",6,7,"[Xe] 4f14 5d5 6s2","Rhenium alloyed into single-crystal turbine blades allows jet engines to operate above metal melting points.","https://upload.wikimedia.org/wikipedia/commons/d/d9/Pure_rhenium_bead%2C_arc_melted%2C_21_grams._Original_size_in_cm_-_1.5_x_1.7.jpg","Supersonic jet engine turbine blades, catalysts","Transition Metal","186.21 u"),
            new Elem("Os","Osmium",76,"#4DABF7",6,8,"[Xe] 4f14 5d6 6s2","Osmium is the densest naturally occurring element on Earth (22.59 g/cm³), twice as dense as lead.","https://upload.wikimedia.org/wikipedia/commons/3/3c/Osmium-bead.jpg","Fountain pen nibs, electrical contacts, microscopy stains","Transition Metal","190.23 u"),
            new Elem("Ir","Iridium",77,"#4DABF7",6,9,"[Xe] 4f14 5d7 6s2","Iridium is the most corrosion-resistant element known, famous for the dinosaur-extinction asteroid layer.","https://upload.wikimedia.org/wikipedia/commons/a/a8/Iridium-2.jpg","Deep-sea spark plugs, satellite crucibles, oncology","Transition Metal","192.22 u"),
            new Elem("Pt","Platinum",78,"#4DABF7",6,10,"[Xe] 4f14 5d9 6s1","Platinum is an unreactive precious metal critical for hydrogen fuel cells and chemotherapy drugs.","https://upload.wikimedia.org/wikipedia/commons/6/68/Platinum_crystals.jpg","Hydrogen fuel cell catalysts, cisplatin cancer drugs","Transition Metal","195.08 u"),
            new Elem("Au","Gold",79,"#4DABF7",6,11,"[Xe] 4f14 5d10 6s1","Gold reflects 99% of infrared radiation and never tarnishes, coating satellite thermal visors.","https://upload.wikimedia.org/wikipedia/commons/8/8a/Gold_%2879_Au%29.jpg","Spacecraft thermal blankets, microchip wire bonding","Transition Metal","196.97 u"),
            new Elem("Hg","Mercury",80,"#4DABF7",6,12,"[Xe] 4f14 5d10 6s2","Mercury is the only metallic element that remains liquid at standard temperature and pressure.","https://upload.wikimedia.org/wikipedia/commons/b/be/Hydrargyrum_%2880_Hg%29.jpg","Liquid mirror telescopes, amalgam dental alloys","Transition Metal","200.59 u"),
            new Elem("Tl","Thallium",81,"#9775FA",6,13,"[Xe] 4f14 5d10 6s2 6p1","Thallium forms heavy optical lenses with high refractive index and low melting point.","https://upload.wikimedia.org/wikipedia/commons/5/55/Thallium_%2881_Tl%29.jpg","Infrared optical sensors, cardiac stress testing","Post-Transition Metal","204.38 u"),
            new Elem("Pb","Lead",82,"#9775FA",6,14,"[Xe] 4f14 5d10 6s2 6p2","Lead provides dense nuclear radiation shielding against gamma rays and X-rays.","https://upload.wikimedia.org/wikipedia/commons/6/63/Lead-2.jpg","Radiation shielding, lead-acid backup battery banks","Post-Transition Metal","207.2 u"),
            new Elem("Bi","Bismuth",83,"#9775FA",6,15,"[Xe] 4f14 5d10 6s2 6p3","Bismuth forms iridescent hopper crystals and has the strongest diamagnetic repulsion of all metals.","https://upload.wikimedia.org/wikipedia/commons/a/a5/Bismuth-2.jpg","Magnetic levitation tracks, Pepto-Bismol, fire sprinklers","Post-Transition Metal","208.98 u"),
            new Elem("Po","Polonium",84,"#FFD43B",6,16,"[Xe] 4f14 5d10 6s2 6p4","Polonium is an intensely radioactive alpha emitter discovered by Marie Curie in pitchblende.","https://images-of-elements.com/polonium.jpg","Antistatic brushes, satellite thermoelectric heaters","Metalloid","209 u"),
            new Elem("At","Astatine",85,"#FF922B",6,17,"[Xe] 4f14 5d10 6s2 6p5","Astatine is the rarest naturally occurring element in Earth's crust, with less than 1 gram present globally.","https://images-of-elements.com/astatine.jpg","Targeted alpha-particle cancer radiotherapy research","Halogen","210 u"),
            new Elem("Rn","Radon",86,"#845EF7",6,18,"[Xe] 4f14 5d10 6s2 6p6","Radon is a heavy radioactive noble gas produced by the radioactive decay of radium.","https://images-of-elements.com/radon.jpg","Earthquake tracking telemetry, cancer radiotherapy","Noble Gas","222 u"),
            new Elem("Fr","Francium",87,"#FF6B6B",7,1,"[Rn] 7s1","Francium is an extremely unstable alkali metal whose longest-lived isotope has a half-life of 22 minutes.","https://images-of-elements.com/francium.jpg","Atomic physics spectroscopy, subatomic research","Alkali Metal","223 u"),
            new Elem("Ra","Radium",88,"#FFA94D",7,2,"[Rn] 7s2","Radium glows faintly blue in the dark due to intense ionizing alpha-particle emissions.","https://upload.wikimedia.org/wikipedia/commons/b/bb/Radium226.jpg","Bone cancer radiotherapy, historical luminous dials","Alkaline Earth","226 u"),
            new Elem("Ac","Actinium",89,"#38D9A9",7,3,"[Rn] 6d1 7s2","Actinium is a powerful neutron source with intense blue glow, used in targeted alpha therapy.","https://upload.wikimedia.org/wikipedia/commons/2/27/Actinium_sample_%2831481701837%29.png","Targeted alpha therapy for metastatic cancers","Actinide","227 u"),
            new Elem("Th","Thorium",90,"#38D9A9",10,4,"[Rn] 6d2 7s2","Thorium is 3x more abundant than uranium and can fuel molten-salt clean nuclear fission reactors.","https://upload.wikimedia.org/wikipedia/commons/f/f7/Thorium-1.jpg","Thorium molten-salt clean nuclear power, gas mantles","Actinide","232.04 u"),
            new Elem("Pa","Protactinium",91,"#38D9A9",10,5,"[Rn] 5f2 6d1 7s2","Protactinium is a toxic and radioactive actinide metal that becomes superconducting at 1.4 K.","https://upload.wikimedia.org/wikipedia/commons/a/af/Protactinium-233.jpg","Geochronological dating of ocean sediments","Actinide","231.04 u"),
            new Elem("U","Uranium",92,"#38D9A9",10,6,"[Rn] 5f3 6d1 7s2","Uranium powers nuclear power stations worldwide via uranium-235 chain fission reactions.","https://upload.wikimedia.org/wikipedia/commons/b/b2/Ames_Process_uranium_biscuit.jpg","Nuclear power plants, armor-piercing kinetic sabots","Actinide","238.03 u"),
            new Elem("Np","Neptunium",93,"#38D9A9",10,7,"[Rn] 5f4 6d1 7s2","Neptunium was the first synthetic transuranic element ever created, by bombarding uranium with neutrons.","https://upload.wikimedia.org/wikipedia/commons/e/e5/Neptunium2.jpg","Precursor in Plutonium-238 production for deep space probes","Actinide","237 u"),
            new Elem("Pu","Plutonium",94,"#38D9A9",10,8,"[Rn] 5f6 7s2","Plutonium-238 generates thermal heat to power Mars rovers (Curiosity/Perseverance) and Voyager probes.","https://upload.wikimedia.org/wikipedia/commons/0/0f/Plutonium_ring.jpg","Mars rover RTG power generators, nuclear deterrents","Actinide","244 u"),
            new Elem("Am","Americium",95,"#38D9A9",10,9,"[Rn] 5f7 7s2","Americium-241 ionizes air in household smoke detectors to trigger fire safety alarms.","https://upload.wikimedia.org/wikipedia/commons/e/ee/Americium_microscope.jpg","Ionization smoke detectors, industrial moisture gauges","Actinide","243 u"),
            new Elem("Cm","Curium",96,"#38D9A9",10,10,"[Rn] 5f7 6d1 7s2","Curium emits strong alpha particles, utilized in Alpha Proton X-ray Spectrometers on Mars rovers.","https://images-of-elements.com/s/curium-glow.jpg","Mars rover rock composition spectrometers (APXS)","Actinide","247 u"),
            new Elem("Bk","Berkelium",97,"#38D9A9",10,11,"[Rn] 5f9 7s2","Berkelium is a synthetic actinide used to synthesize heavier superheavy elements like Tennessine.","https://upload.wikimedia.org/wikipedia/commons/f/fc/Berkelium.jpg","Target material for superheavy element synthesis","Actinide","247 u"),
            new Elem("Cf","Californium",98,"#38D9A9",10,12,"[Rn] 5f10 7s2","Californium-252 is an intense portable neutron emitter used in airport explosive detection.","https://upload.wikimedia.org/wikipedia/commons/9/93/Californium.jpg","Nuclear reactor startup sources, explosive detection scanners","Actinide","251 u"),
            new Elem("Es","Einsteinium",99,"#38D9A9",10,13,"[Rn] 5f11 7s2","Einsteinium was discovered in the fallout debris of the first thermonuclear hydrogen bomb test in 1952.","https://upload.wikimedia.org/wikipedia/commons/5/55/Einsteinium.jpg","Fundamental actinide chemistry research","Actinide","252 u"),
            new Elem("Fm","Fermium",100,"#38D9A9",10,14,"[Rn] 5f12 7s2","Fermium is the heaviest element that can be synthesized in microgram quantities in nuclear reactors.","https://upload.wikimedia.org/wikipedia/commons/5/58/Ivy_Mike_-_mushroom_cloud.jpg","Nuclear physics research","Actinide","257 u"),
            new Elem("Md","Mendelevium",101,"#38D9A9",10,15,"[Rn] 5f13 7s2","Mendelevium is named after Dmitri Mendeleev, the father of the periodic table of elements.","https://images-of-elements.com/s/mendelevium.jpg","Subatomic valence research","Actinide","258 u"),
            new Elem("No","Nobelium",102,"#38D9A9",10,16,"[Rn] 5f14 7s2","Nobelium is named in honor of Alfred Nobel, the inventor of dynamite and Nobel Prize founder.","https://images-of-elements.com/nobelium.jpg","Quantum nuclear physics","Actinide","259 u"),
            new Elem("Lr","Lawrencium",103,"#38D9A9",10,17,"[Rn] 5f14 7s2 7p1","Lawrencium completes the actinide series, discovered at the Berkeley Heavy Ion Linear Accelerator.","https://images-of-elements.com/lawrencium.jpg","Heavy ion physics research","Actinide","266 u"),
            new Elem("Rf","Rutherfordium",104,"#4DABF7",7,4,"[Rn] 5f14 6d2 7s2","Rutherfordium is the first transactinide superheavy element, synthesized via particle acceleration.","https://images-of-elements.com/s/rutherfordium.jpg","Relativistic quantum chemistry","Transition Metal","267 u"),
            new Elem("Db","Dubnium",105,"#4DABF7",7,5,"*[Rn] 5f14 6d3 7s2","Dubnium is a superheavy synthetic element discovered in Dubna, Russia.","https://images-of-elements.com/s/transactinoid.png","Superheavy element investigations","Transition Metal","268 u"),
            new Elem("Sg","Seaborgium",106,"#4DABF7",7,6,"*[Rn] 5f14 6d4 7s2","Seaborgium was the first element named after a living person, American chemist Glenn Seaborg.","https://images-of-elements.com/s/transactinoid.png","Gas-phase carbonyl complex research","Transition Metal","269 u"),
            new Elem("Bh","Bohrium",107,"#4DABF7",7,7,"*[Rn] 5f14 6d5 7s2","Bohrium is named after Danish quantum physicist Niels Bohr.","https://images-of-elements.com/s/transactinoid.png","Quantum shell model research","Transition Metal","270 u"),
            new Elem("Hs","Hassium",108,"#4DABF7",7,8,"*[Rn] 5f14 6d6 7s2","Hassium forms a volatile tetroxide compound analogous to osmium tetroxide.","https://images-of-elements.com/s/transactinoid.png","Superheavy chemical bonding research","Transition Metal","270 u"),
            new Elem("Mt","Meitnerium",109,"#4DABF7",7,9,"*[Rn] 5f14 6d7 7s2","Meitnerium is named in honor of nuclear physicist Lise Meitner, who co-discovered nuclear fission.","https://images-of-elements.com/s/transactinoid.png","Nuclear fission research","Transition Metal","278 u"),
            new Elem("Ds","Darmstadtium",110,"#4DABF7",7,10,"*[Rn] 5f14 6d9 7s1","Darmstadtium was created at the GSI Helmholtz Centre for Heavy Ion Research.","https://images-of-elements.com/s/transactinoid.png","Superheavy relativistic physics","Transition Metal","281 u"),
            new Elem("Rg","Roentgenium",111,"#4DABF7",7,11,"*[Rn] 5f14 6d10 7s1","Roentgenium is named after Wilhelm Röntgen, discoverer of X-rays.","https://images-of-elements.com/s/transactinoid.png","Relativistic electron shell testing","Transition Metal","282 u"),
            new Elem("Cn","Copernicium",112,"#4DABF7",7,12,"*[Rn] 5f14 6d10 7s2","Copernicium exhibits volatile metallic gas behavior due to extreme relativistic electron contraction.","https://images-of-elements.com/s/transactinoid.png","Relativistic atomic physics","Transition Metal","285 u"),
            new Elem("Nh","Nihonium",113,"#9775FA",7,13,"*[Rn] 5f14 6d10 7s2 7p1","Nihonium was the first element discovered in Asia, synthesized at RIKEN in Japan.","https://images-of-elements.com/s/transactinoid.png","Superheavy p-block synthesis","Post-Transition Metal","286 u"),
            new Elem("Fl","Flerovium",114,"#9775FA",7,14,"*[Rn] 5f14 6d10 7s2 7p2","Flerovium sits near the theoretical 'Island of Stability' for superheavy atomic nuclei.","https://images-of-elements.com/s/transactinoid.png","Island of Stability nuclear experiments","Post-Transition Metal","289 u"),
            new Elem("Mc","Moscovium",115,"#9775FA",7,15,"*[Rn] 5f14 6d10 7s2 7p3","Moscovium is an extremely radioactive synthetic element synthesized via calcium-48 bombardment.","https://images-of-elements.com/s/transactinoid.png","Superheavy decay chain studies","Post-Transition Metal","290 u"),
            new Elem("Lv","Livermorium",116,"#9775FA",7,16,"*[Rn] 5f14 6d10 7s2 7p4","Livermorium was discovered jointly by scientists at Lawrence Livermore and Dubna.","https://images-of-elements.com/s/transactinoid.png","Superheavy alpha decay studies","Post-Transition Metal","293 u"),
            new Elem("Ts","Tennessine",117,"#FF922B",7,17,"*[Rn] 5f14 6d10 7s2 7p5","Tennessine is the second-heaviest known element, completing Period 7 halogen group.","https://images-of-elements.com/s/transactinoid.png","Superheavy halogen chemistry","Halogen","294 u"),
            new Elem("Og","Oganesson",118,"#845EF7",7,18,"*[Rn] 5f14 6d10 7s2 7p6","Oganesson has the highest atomic number and mass of all known elements on the Periodic Table.","https://images-of-elements.com/s/transactinoid.png","Relativistic noble gas physics","Noble Gas","294 u")
    };

    private static final Compound[] COMPOUNDS = {
            new Compound(new String[]{"H","O"}, "H₂O", "Dihydrogen Monoxide", "Water / Universal Solvent",
                    "Bent Molecular Geometry • 104.5° H-O-H Bond Angle • sp³ Hybridized Oxygen • Dipole Moment 1.85 D • Intermolecular Hydrogen Bonding Network.",
                    "Polar Covalent O-H Bonds (ΔEN = 1.24) • Strong Liquid Cohesion",
                    "2 H₂ (g) + O₂ (g) → 2 H₂O (l) + 572 kJ\nRapid highly exothermic combustion initiated by electric spark or platinum catalyst.",
                    "• Universal biological cellular solvent for all metabolic life.\n• Industrial steam turbine power generation working fluid.\n• Agricultural crop irrigation & hydroponics.\n• Precursor for green electrolytic hydrogen energy storage.", "Polar Covalent Molecule"),

            new Compound(new String[]{"H","O"}, "H₂O₂", "Hydrogen Peroxide", "Peroxide / Bleaching Agent",
                    "Non-planar Open-Book (Skewed) Geometry • Dihedral Angle 111.5° • Reactive O-O Peroxo Single Bond.",
                    "Covalent Single O-O Peroxy Linkage (Weak 146 kJ/mol) • Strong Oxidizing Potential",
                    "Anthraquinone autoxidation process: H₂ + O₂ catalyzed over alkylanthrahydroquinone in organic solution.",
                    "• Medical antiseptic disinfectant (3% topical solution).\n• Monopropellant rocket fuel & satellite attitude thrusters (high-test 90%+).\n• Environmental wastewater oxidation of sulfur and cyanides.\n• Pulp and paper industrial bleaching agent.", "Oxidizing Peroxide"),

            new Compound(new String[]{"Na","Cl"}, "NaCl", "Sodium Chloride", "Table Salt / Halite",
                    "Face-Centered Cubic (FCC) Ionic Lattice • Coordination Number 6:6 [Na⁺ surrounded by 6 Cl⁻ octahedra] • Lattice Constant 564.02 pm.",
                    "Strong Electrostatic Ionic Bond (ΔEN = 2.23) • Lattice Energy: 786 kJ/mol",
                    "2 Na (s) + Cl₂ (g) → 2 NaCl (s) + 822 kJ\nSodium metal burns vigorously with bright yellow luminescence in toxic chlorine gas.",
                    "• Essential biological dietary electrolyte for neuromuscular transmission.\n• Highway de-icing via freezing-point depression.\n• Chloralkali chemical manufacturing for Chlorine, NaOH, and PVC plastics.\n• Medical isotonic saline (0.9%) IV fluid resuscitation.", "Ionic Salt"),

            new Compound(new String[]{"C","O"}, "CO₂", "Carbon Dioxide", "Carbonic Gas / Dry Ice",
                    "Linear Molecular Geometry • 180° O=C=O Bond Angle • sp Hybridized Carbon • Zero Net Molecular Dipole Moment (Symmetric).",
                    "Double Covalent C=O Bonds (Bond Energy 799 kJ/mol)",
                    "C (s) + O₂ (g) → CO₂ (g) + 393.5 kJ\nComplete combustion of carbonaceous fuels or limestone calcination (CaCO₃ → CaO + CO₂).",
                    "• Plant photosynthesis reactant supporting planetary biomass.\n• Supercritical CO₂ decaffeination and aerogel extraction solvent.\n• Fire extinguishers & beverage carbonation.\n• Cryogenic cooling as Dry Ice (-78.5°C sublimation).", "Covalent Gas"),

            new Compound(new String[]{"C","O"}, "CO", "Carbon Monoxide", "Coal Gas / Syngas Precursor",
                    "Linear Diatomic • Triple Bond (one coordinate dative bond from Oxygen to Carbon) • Bond Length 112.8 pm.",
                    "Strongest chemical bond in neutral molecules (1072 kJ/mol)",
                    "2 C (s) + O₂ (g) [limited] → 2 CO (g) or Steam reforming: CH₄ + H₂O → CO + 3 H₂.",
                    "• Industrial blast-furnace reduction of iron ore (Fe₂O₃ + 3 CO → 2 Fe + 3 CO₂).\n• Fischer-Tropsch synthetic hydrocarbon fuel production.\n• Acetic acid synthesis via methanol carbonylation (Monsanto process).", "Reducing Gas"),

            new Compound(new String[]{"C","H"}, "CH₄", "Methane", "Natural Gas / Firedamp",
                    "Tetrahedral Molecular Geometry • 109.5° H-C-H Bond Angle • sp³ Hybridized Carbon • 4 Equivalent C-H σ Bonds.",
                    "Non-polar Covalent C-H Bonds (439 kJ/mol)",
                    "Sabatier reaction: CO₂ + 4 H₂ → CH₄ + 2 H₂O (Nickel catalyst, 300°C-400°C).",
                    "• Clean-burning electrical power generation in gas turbines.\n• Methalox rocket propellant (SpaceX Raptor & Starship engines).\n• Primary steam-reforming feedstock for commercial Hydrogen production.\n• Domestic residential heating and cooking.", "Hydrocarbon Gas"),

            new Compound(new String[]{"C","H","O"}, "C₂H₅OH", "Ethanol", "Grain Alcohol / Biofuel",
                    "Tetrahedral around Carbon atoms • Bent around Oxygen atom (108.5°) • Polar Hydroxyl (-OH) Functional Group.",
                    "Covalent C-C, C-H, C-O, and O-H Bonds • Capable of Hydrogen Bonding",
                    "C₂H₄ + H₂O (Steam hydration over H₃PO₄ catalyst at 300°C) or microbial sugar fermentation: C₆H₁₂O₆ → 2 C₂H₅OH + 2 CO₂.",
                    "• Renewable E85 motor vehicle biofuel.\n• Antiseptic hand sanitizers and medical disinfectants (70% solution).\n• Organic chemical solvent for perfumes, paints, and pharmaceuticals.\n• Precursor for ethyl esters, acetic acid, and acetaldehyde.", "Organic Alcohol"),

            new Compound(new String[]{"C","H","O"}, "C₆H₁₂O₆", "D-Glucose", "Blood Sugar / Dextrose",
                    "Six-membered Pyranose Ring (Haworth Projection) • Chair Conformation • 5 Hydroxyl Groups and 1 Hydroxymethyl Group.",
                    "Covalent Polyhydroxy Aldehyde Backbone",
                    "Biological Photosynthesis: 6 CO₂ + 6 H₂O + Photons → C₆H₁₂O₆ + 6 O₂.",
                    "• Primary universal energetic cellular fuel in aerobic glycolysis (produces 30-32 ATP).\n• Medical IV dextrose infusions for hypoglycemia and dehydration.\n• Feedstock for bioplastics (PLA - Polylactic Acid) synthesis.\n• Essential food nutrient and cellular metabolism driver.", "Monosaccharide Carbohydrate"),

            new Compound(new String[]{"C","H","O"}, "CH₃COOH", "Ethanoic Acid", "Acetic Acid / Vinegar",
                    "Planar Carboxyl Group (-COOH) bonded to Methyl group (-CH₃) • Hydrogen-bonded Dimers in Gas/Liquid Phase.",
                    "Polar Covalent with Resonantly Stabilized Carboxylate Anion",
                    "CH₃OH + CO → CH₃COOH (Monsanto/Cativa carbonylation over Iridium/Rhodium catalyst).",
                    "• Vinyl acetate monomer (VAM) production for paints, glues, and coatings.\n• Food flavoring and biological preservation (Vinegar 5-8%).\n• Cellulose acetate synthesis for photographic film and synthetic fibers.\n• Industrial organic chemistry solvent.", "Carboxylic Acid"),

            new Compound(new String[]{"C","H","O"}, "C₃H₆O", "Propan-2-one", "Acetone / Dimethyl Ketone",
                    "Trigonal Planar Carbonyl Carbon (120° C-C(=O)-C) • sp² Hybridized Central Carbon.",
                    "Polar Carbonyl C=O Double Bond with high dipole moment (2.88 D)",
                    "Cumene process: Benzene + Propylene → Cumene + O₂ → Acetone + Phenol.",
                    "• Universal laboratory and industrial degreasing solvent.\n• Precursor to Methyl Methacrylate (Plexiglas / Lucite acrylic).\n• Nail polish remover and chemical lacquer solvent.\n• Safe storage solvent for compressed Acetylene gas tanks.", "Ketone Solvent"),

            new Compound(new String[]{"N","H"}, "NH₃", "Ammonia", "Ammonia Gas / Fertilizer Base",
                    "Trigonal Pyramidal Geometry • 107.8° H-N-H Bond Angle • Lone Pair on Nitrogen • Strong Hydrogen Bonding.",
                    "Polar Covalent N-H Bonds (391 kJ/mol) • Lewis Base",
                    "Haber-Bosch process: N₂ (g) + 3 H₂ (g) ⇌ 2 NH₃ (g) (Iron catalyst, 450°C, 200 atm).",
                    "• Agricultural fertilizer precursor (urea, ammonium nitrate) feeding ~50% of Earth's population.\n• Industrial refrigerant (R-717) in large cold-storage warehouses.\n• Precursor for nitric acid, hydrazine, and nylon polymers.\n• Zero-carbon maritime carrier fuel for hydrogen transport.", "Nitrogen Hydride"),

            new Compound(new String[]{"N","H","O"}, "HNO₃", "Nitric Acid", "Aqua Fortis / Strong Acid",
                    "Planar Molecule • Resonantly Delocalized Nitrate Group • O-N-O angles ~130° and 115°.",
                    "Strong Polar Covalent Acid (pKa = -1.4) • Powerful Oxidizing Agent",
                    "Ostwald process: 4 NH₃ + 5 O₂ → 4 NO + 6 H₂O (Pt-Rh gauze catalyst, 900°C), followed by NO oxidation and absorption in water.",
                    "• Ammonium nitrate fertilizer synthesis.\n• Semiconductor silicon wafer chemical etching and cleaning.\n• Rocket propellant oxidizer (Red Fuming Nitric Acid).\n• Metallurgical gold and platinum refining (component of Aqua Regia with HCl).", "Mineral Acid"),

            new Compound(new String[]{"Ni","Ti"}, "NiTi", "Nickel-Titanium", "Nitinol Shape-Memory Alloy",
                    "Reversible Solid-State Phase Transformation • High-temp Austenite (B2 Cubic CsCl-type) to Low-temp Martensite (B19' Monoclinic).",
                    "Metallic Bonding with Strong d-Orbital Overlap • Superelasticity up to 8% Strain",
                    "Vacuum Induction Melting (VIM) or Vacuum Arc Remelting (VAR) under argon atmosphere at 1,310°C.",
                    "• Self-expanding cardiac vascular stents and orthodontic archwires.\n• Aerospace actuators, Mars rover non-pneumatic wire-mesh tires.\n• Surgical laparoscopic tools and robotic micro-manipulators.\n• Temperature-activated safety valves and smart couplings.", "Smart Shape-Memory Superalloy"),

            new Compound(new String[]{"Ti","Au"}, "β-Ti₃Au", "Beta-Phase Titanium-Gold", "Superhard Biocompatible Intermetallic",
                    "Ordered Cubic L1₂ Lattice • High Cohesive Energy • Dislocation Motion Hindered by Intermetallic Ordering.",
                    "Metallic-Covalent Hybridized Intermetallic Bonding • 4x Hardness of Pure Titanium",
                    "High-vacuum arc melting of 3:1 atomic ratio Ti and Au at >1,500°C followed by controlled furnace annealing.",
                    "• Ultra-durable joint prosthetics (artificial hips and knees) with near-zero wear debris.\n• Dental implants with superior biocompatibility.\n• Deep-well drilling precision bearings.\n• Luxury scratch-proof horology and aerospace instrumentation.", "Intermetallic Superalloy"),

            new Compound(new String[]{"Fe","C","Cr","Ni"}, "Fe-Cr-Ni-C", "Austenitic Stainless Steel (316)", "Marine Grade Stainless Steel",
                    "Face-Centered Cubic (FCC) Austenite Matrix stabilized by Nickel • Solid Solution of Carbon in Iron Interstices.",
                    "Metallic Bonding with Spontaneous Chromium Oxide (Cr₂O₃) Passive Nanofilm",
                    "Electric Arc Furnace (EAF) smelting with Argon Oxygen Decarburization (AOD) refining: 70% Fe, 18% Cr, 10% Ni, 2% Mo, <0.08% C.",
                    "• Marine, chemical processing, and pharmaceutical equipment.\n• Surgical scalpels, needles, and medical implants.\n• Food and beverage sanitary containment tanks.\n• Architectural structural cladding and aerospace manifolds.", "Structural Superalloy"),

            new Compound(new String[]{"Si","O"}, "SiO₂", "Silicon Dioxide", "Quartz / Beach Sand / Aerogel",
                    "3D Continuous Covalent Network • Corner-Sharing SiO₄ Tetrahedra (Si-O-Si angle ~144°) • Trigonal α-Quartz or Amorphous Glass.",
                    "Strong Covalent Network (Bond Energy 460 kJ/mol) • High Melting Point (1,710°C)",
                    "Si (s) + O₂ (g) → SiO₂ (s) or Hydrolysis of tetraethyl orthosilicate (TEOS) with supercritical drying to yield Aerogel.",
                    "• Silicon microelectronics semiconductor wafer fabrication & gate oxides.\n• Optical fiber communication cables and precision camera lenses.\n• NASA spacecraft thermal insulation (as 99.8% air Aerogel 'frozen smoke').\n• Glass containers, window panes, and structural concrete aggregates.", "Covalent Network Mineral"),

            new Compound(new String[]{"Si","C"}, "SiC", "Silicon Carbide", "Carborundum / Extreme Semiconductor",
                    "Tetrahedral sp³ Network • Zincblende (3C) or Wurtzite (4H/6H) Polytypes • Mohs Hardness 9.5.",
                    "Strong Covalent Si-C Bonds (ΔEN = 0.65) • Wide Bandgap 3.26 eV",
                    "Acheson process: SiO₂ + 3 C → SiC + 2 CO (Electric resistance furnace, 2,500°C).",
                    "• EV power inverters, high-voltage fast chargers, and rail power grids.\n• Carbon-ceramic brake discs for hypercars and aircraft.\n• Abrasive grinding wheels, sandpaper, and ballistic armor plates.\n• High-temperature furnace heating elements.", "Wide-Bandgap Semiconductor"),

            new Compound(new String[]{"Ga","N"}, "GaN", "Gallium Nitride", "GaN Power Crystal",
                    "Wurtzite Hexagonal Crystal Lattice • High Electron Saturation Velocity (2.5 × 10⁷ cm/s).",
                    "Covalent with Partial Ionic Character • Direct Bandgap 3.4 eV",
                    "Metal-Organic Chemical Vapor Deposition (MOCVD): Trimethylgallium + Ammonia at 1,050°C.",
                    "• Ultra-compact high-efficiency GaN smartphone and laptop fast chargers.\n• Blue and violet laser diodes (Blu-ray, surgical lasers, LED lighting).\n• 5G cellular base station RF power amplifiers.\n• Military Active Electronically Scanned Array (AESA) radars.", "Compound Semiconductor"),

            new Compound(new String[]{"Li","Co","O"}, "LiCoO₂", "Lithium Cobalt(III) Oxide", "LCO Battery Cathode",
                    "Layered α-NaFeO₂ Rock-Salt Lattice (R3m space group) • Alternating layers of Lithium ions and CoO₆ octahedra.",
                    "Mixed Ionic-Covalent with Reversible Intercalation Channels",
                    "Solid-state reaction of Lithium Carbonate (Li₂CO₃) and Cobalt Carbonate (CoCO₃) calcined at 900°C in air.",
                    "• High-energy-density rechargeable battery cathodes in smartphones and laptops.\n• Portable medical equipment power supplies.\n• High-discharge consumer electronics.", "Battery Cathode Material"),

            new Compound(new String[]{"C","H","N","O"}, "C₈H₁₀N₄O₂", "1,3,7-Trimethylxanthine", "Caffeine / Stimulant",
                    "Fused Bicyclic Purine Ring System (Pyrimidinedione + Imidazole) • Planar sp² Skeleton.",
                    "Heterocyclic Organic Molecule with Tertiary Amine and Amide Centers",
                    "Traube synthesis from Dimethylurea and Ethyl Cyanoacetate, or supercritical CO₂ extraction from coffee beans.",
                    "• Central nervous system psychostimulant (adenosine receptor antagonist).\n• Enhances athletic alertness and cognitive focus.\n• Combined in analgesic formulations to boost pain relief efficacy.", "Purine Alkaloid"),

            new Compound(new String[]{"C","H","N","O"}, "C₉H₈O₄", "Acetylsalicylic Acid", "Aspirin",
                    "Benzene Ring substituted with Carboxylic Acid (-COOH) and Acetoxy (-OCOCH₃) Groups.",
                    "Aromatic Ring with Ester and Carboxylic Covalent Linkages",
                    "Salicylic acid + Acetic anhydride → Acetylsalicylic acid + Acetic acid (Acid-catalyzed esterification).",
                    "• Analgesic pain relief and antipyretic fever reducer.\n• Anti-inflammatory treatment for arthritis.\n• Low-dose antiplatelet cardiovascular therapy to prevent stroke and heart attacks.", "Pharmaceutical Drug"),

            new Compound(new String[]{"Fe","O"}, "Fe₂O₃", "Iron(III) Oxide", "Rust / Hematite Ore",
                    "Corundum-type Rhombohedral Lattice • Hexagonal Close-Packed Oxygen anions with Fe³⁺ in 2/3 octahedral sites.",
                    "Ionic Crystal Lattice (Lattice Energy 14,774 kJ/mol)",
                    "4 Fe (s) + 3 O₂ (g) + 6 H₂O (l) → 4 Fe(OH)₃ (s) → 2 Fe₂O₃·3H₂O (Corrosion oxidation).",
                    "• Primary ore for industrial pig iron and steel smelting in blast furnaces.\n• Red ferric pigment in paints, ceramics, and cosmetics (Rouge).\n• Magnetic recording tapes and hard drive storage media.\n• Thermite cutting and welding reactions with Aluminum powder.", "Metal Oxide"),

            new Compound(new String[]{"Cu","Sn"}, "Cu₉Sn", "Phosphor Bronze", "Alpha Bronze / Bell Metal",
                    "Face-Centered Cubic (FCC) Alpha Solid Solution • Tin atoms distort copper lattice, inhibiting dislocation glide.",
                    "Metallic Bonding with High Tensile Strength and Ductility",
                    "Smelting molten copper (1,085°C) with tin ingots (232°C) under charcoal flux cover.",
                    "• Ship marine propellers and underwater fittings (impervious to saltwater corrosion).\n• Heavy-duty low-friction machine bearings and bushings.\n• Musical acoustic guitar strings, cymbals, and church bells.\n• Historical monument sculpture casting.", "Alloy")
    };

    // ── Draggable + Tappable Holographic Element Tile ──────────────────────────
    private class ElemView extends FrameLayout {
        final Elem elem;
        float downRawX, downRawY, startX, startY, totalMove;
        TextView tvSymbol, tvNumber, tvWeight;

        ElemView(Context ctx, Elem e) {
            super(ctx);
            elem = e;

            // Background with holographic border
            GradientDrawable gd = new GradientDrawable();
            int baseColor = Color.parseColor(e.color);
            gd.setColor(0xD00A1A2E);
            gd.setStroke(dp(1), baseColor);
            gd.setCornerRadius(dp(4));
            setBackground(gd);

            LinearLayout box = new LinearLayout(ctx);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            box.setPadding(dp(2), dp(2), dp(2), dp(2));

            tvNumber = new TextView(ctx);
            tvNumber.setText(String.valueOf(e.number));
            tvNumber.setTextColor(0xFF88AADD);
            tvNumber.setTextSize(8f);
            box.addView(tvNumber);

            tvSymbol = new TextView(ctx);
            tvSymbol.setText(e.symbol);
            tvSymbol.setTextColor(baseColor);
            tvSymbol.setTypeface(Typeface.DEFAULT_BOLD);
            tvSymbol.setTextSize(14f);
            box.addView(tvSymbol);

            tvWeight = new TextView(ctx);
            tvWeight.setText(e.name);
            tvWeight.setTextColor(0xFFCCCCCC);
            tvWeight.setTextSize(7f);
            tvWeight.setSingleLine(true);
            box.addView(tvWeight);

            addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setClickable(true);
            setFocusable(true);
            setOnClickListener(v -> selectElement(elem));
        }

        void highlight(boolean active) {
            GradientDrawable gd = (GradientDrawable) getBackground();
            int baseColor = Color.parseColor(elem.color);
            if (active) {
                gd.setColor(0xE00D2847);
                gd.setStroke(dp(2), baseColor);
                setAlpha(1.0f);
            } else {
                gd.setColor(0x30050D17);
                gd.setStroke(dp(1), 0x33446688);
                setAlpha(0.35f);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_periodic_table);

        tts = new TextToSpeech(this, this);

        initViews();
        setupTabs();
        setupCategoryChips();
        setupSearch();
        buildPeriodicTableGrid();
        setupMixerPanel();
        setupBohrPanel();

        // Default selected element: Iron (Fe)
        selectElement(ELEMENTS[25]);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            ttsReady = true;
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private void initViews() {
        tvLabTitle         = findViewById(R.id.tv_lab_title);
        tvLabSubtitle      = findViewById(R.id.tv_lab_subtitle);
        btnBack            = findViewById(R.id.btn_periodic_back);
        btnVoiceInspect    = findViewById(R.id.btn_voice_inspect);
        tvMatrixStatus     = findViewById(R.id.tv_matrix_status);
        tvElementCounter   = findViewById(R.id.tv_element_counter);

        tabMatrix          = findViewById(R.id.tab_matrix);
        tabMixer           = findViewById(R.id.tab_mixer);
        tabBohr            = findViewById(R.id.tab_bohr);

        panelMatrix        = findViewById(R.id.panel_matrix);
        panelMixer         = findViewById(R.id.panel_mixer);
        panelBohr          = findViewById(R.id.panel_bohr);

        searchFilterBar    = findViewById(R.id.search_filter_bar);
        etSearch           = findViewById(R.id.et_element_search);
        btnClearSearch     = findViewById(R.id.btn_clear_search);
        categoryChipsContainer = findViewById(R.id.category_chips_container);
        tableLayer         = findViewById(R.id.table_layer);

        cardMiniInspect    = findViewById(R.id.card_mini_inspect);
        miniTilePreview    = findViewById(R.id.mini_tile_preview);
        miniZNum           = findViewById(R.id.mini_z_num);
        miniSymbol         = findViewById(R.id.mini_symbol);
        miniName           = findViewById(R.id.mini_name);
        miniEconfig        = findViewById(R.id.mini_econfig);
        btnInspectFull     = findViewById(R.id.btn_inspect_full);

        btnBack.setOnClickListener(v -> finish());
        btnVoiceInspect.setOnClickListener(v -> speakCurrentElementTelemetry());
        btnInspectFull.setOnClickListener(v -> {
            if (selectedElement != null) showElementDetailModal(selectedElement);
        });
    }

    private void setupTabs() {
        tabMatrix.setOnClickListener(v -> switchMode(Mode.MATRIX));
        tabMixer.setOnClickListener(v -> switchMode(Mode.MIXER));
        tabBohr.setOnClickListener(v -> switchMode(Mode.BOHR));
    }

    private void switchMode(Mode mode) {
        currentMode = mode;

        tabMatrix.setTextColor(mode == Mode.MATRIX ? 0xFF00FFCC : 0xFF88AABB);
        tabMatrix.setBackgroundColor(mode == Mode.MATRIX ? 0x2500FFCC : 0xFF0A1526);

        tabMixer.setTextColor(mode == Mode.MIXER ? 0xFF00FFCC : 0xFF88AABB);
        tabMixer.setBackgroundColor(mode == Mode.MIXER ? 0x2500FFCC : 0xFF0A1526);

        tabBohr.setTextColor(mode == Mode.BOHR ? 0xFF00FFCC : 0xFF88AABB);
        tabBohr.setBackgroundColor(mode == Mode.BOHR ? 0x2500FFCC : 0xFF0A1526);

        panelMatrix.setVisibility(mode == Mode.MATRIX ? View.VISIBLE : View.GONE);
        panelMixer.setVisibility(mode == Mode.MIXER ? View.VISIBLE : View.GONE);
        panelBohr.setVisibility(mode == Mode.BOHR ? View.VISIBLE : View.GONE);
        searchFilterBar.setVisibility(mode == Mode.MATRIX ? View.VISIBLE : View.GONE);

        if (mode == Mode.MATRIX) {
            tvLabTitle.setText("HENRY PERIODIC MATRIX");
            tvLabSubtitle.setText("-- INITIATED -- • [SEGMENTS: 118 ELEMENTS]");
        } else if (mode == Mode.MIXER) {
            tvLabTitle.setText("HENRY MOLECULAR CHEMICAL MIXER");
            tvLabSubtitle.setText("MULTI-ELEMENT ADVANCED SYNTHESIS & REACTION VESSEL");
        } else if (mode == Mode.BOHR) {
            tvLabTitle.setText("BOHR ATOMIC ORBITALS");
            tvLabSubtitle.setText("ELECTRON QUANTUM SHELL DIAGNOSTICS");
            if (selectedElement != null) updateBohrView(selectedElement);
        }
    }

    private void setupCategoryChips() {
        String[] categories = {
                "ALL", "Alkali Metal", "Alkaline Earth", "Transition Metal",
                "Post-Transition Metal", "Metalloid", "Reactive Nonmetal",
                "Halogen", "Noble Gas", "Lanthanide", "Actinide"
        };

        categoryChipsContainer.removeAllViews();
        for (String cat : categories) {
            TextView chip = new TextView(this);
            chip.setText(cat.toUpperCase(Locale.US));
            chip.setTextSize(10f);
            chip.setPadding(dp(10), dp(4), dp(10), dp(4));
            chip.setTextColor(cat.equals(selectedCategory) ? 0xFF001520 : 0xFF88AADD);
            chip.setTypeface(Typeface.DEFAULT_BOLD);

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(cat.equals(selectedCategory) ? 0xFF00FFCC : 0x2000D4FF);
            gd.setStroke(dp(1), 0x5500E5FF);
            gd.setCornerRadius(dp(12));
            chip.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(6), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                selectedCategory = cat;
                setupCategoryChips(); // re-render chip styles
                filterElements();
            });
            categoryChipsContainer.addView(chip);
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterElements();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            filterElements();
        });
    }

    private void filterElements() {
        String query = etSearch.getText().toString().trim().toLowerCase(Locale.US);
        int matchedCount = 0;

        for (ElemView ev : elemViews) {
            boolean matchesCategory = selectedCategory.equals("ALL") ||
                    ev.elem.category.equalsIgnoreCase(selectedCategory);

            boolean matchesQuery = query.isEmpty() ||
                    ev.elem.name.toLowerCase(Locale.US).contains(query) ||
                    ev.elem.symbol.toLowerCase(Locale.US).startsWith(query) ||
                    String.valueOf(ev.elem.number).equals(query);

            boolean visible = matchesCategory && matchesQuery;
            ev.highlight(visible);
            if (visible) matchedCount++;
        }

        tvElementCounter.setText(matchedCount + " / 118");
        if (!query.isEmpty()) {
            tvMatrixStatus.setText("Found " + matchedCount + " element(s) matching \"" + query + "\"");
        } else {
            tvMatrixStatus.setText("💡 Swipe in any direction to explore 118 elements • Tap to inspect telemetry");
        }
    }

    private void buildPeriodicTableGrid() {
        tableLayer.removeAllViews();
        elemViews.clear();

        int tableW = dp(CELL * 19);
        int tableH = dp(CELL * 11);
        tableLayer.setLayoutParams(new FrameLayout.LayoutParams(tableW, tableH));

        for (Elem e : ELEMENTS) {
            ElemView ev = new ElemView(this, e);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(CELL - 4), dp(CELL - 4));
            lp.leftMargin = dp((e.col - 1) * CELL);
            lp.topMargin  = dp((e.row - 1) * CELL) + (e.row >= 9 ? dp(CELL / 2) : 0);
            ev.setLayoutParams(lp);
            tableLayer.addView(ev);
            elemViews.add(ev);
        }
    }

    private void selectElement(Elem elem) {
        this.selectedElement = elem;
        cardMiniInspect.setVisibility(View.VISIBLE);

        miniZNum.setText(String.valueOf(elem.number));
        miniSymbol.setText(elem.symbol);
        miniSymbol.setTextColor(Color.parseColor(elem.color));
        miniName.setText(elem.name + " (" + elem.category + ")");
        miniEconfig.setText(elem.econfig + " • " + elem.weight);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0x2000FFCC);
        gd.setStroke(dp(1), Color.parseColor(elem.color));
        gd.setCornerRadius(dp(4));
        miniTilePreview.setBackground(gd);
    }

    private void onElementsCombined(Elem a, Elem b) {
        selectedReactantElements.clear();
        selectedReactantElements.add(a);
        if (!b.symbol.equalsIgnoreCase(a.symbol)) {
            selectedReactantElements.add(b);
        }
        updateMixerUI();
        switchMode(Mode.MIXER);
    }

    // ── Mode 2: Multi-Element Chemical Mixer Setup ────────────────────────────
    private void setupMixerPanel() {
        selectedElementsContainer = findViewById(R.id.selected_elements_container);
        btnAddElement             = findViewById(R.id.btn_add_element);
        btnClearElements          = findViewById(R.id.btn_clear_elements);

        cardMultipleCompounds     = findViewById(R.id.card_multiple_compounds);
        tvMultipleCompoundsTitle  = findViewById(R.id.tv_multiple_compounds_title);
        multipleCompoundsChips    = findViewById(R.id.multiple_compounds_chips);

        cardMixerResult           = findViewById(R.id.card_mixer_result);
        tvCompoundFormula         = findViewById(R.id.tv_compound_formula);
        tvCompoundCommon          = findViewById(R.id.tv_compound_common);
        tvCompoundStructure       = findViewById(R.id.tv_compound_structure);
        tvCompoundBond            = findViewById(R.id.tv_compound_bond);
        tvCompoundSynthesis       = findViewById(R.id.tv_compound_synthesis);
        tvCompoundUses            = findViewById(R.id.tv_compound_uses);

        commonCompoundsContainer  = findViewById(R.id.common_compounds_container);

        btnNovelChemical          = findViewById(R.id.btn_novel_chemical);
        novelChemicalOutput       = findViewById(R.id.novel_chemical_output);
        tvNovelName               = findViewById(R.id.tv_novel_name);
        tvNovelFormula            = findViewById(R.id.tv_novel_formula);
        tvNovelStructure          = findViewById(R.id.tv_novel_structure);
        tvNovelSynthesis          = findViewById(R.id.tv_novel_synthesis);
        tvNovelUses               = findViewById(R.id.tv_novel_uses);

        // Default combo: Sodium (Na) + Chlorine (Cl)
        selectedReactantElements.clear();
        selectedReactantElements.add(ELEMENTS[10]); // Na
        selectedReactantElements.add(ELEMENTS[16]); // Cl

        btnAddElement.setOnClickListener(v -> showElementSelectorDialog());
        btnClearElements.setOnClickListener(v -> {
            selectedReactantElements.clear();
            updateMixerUI();
        });

        btnNovelChemical.setOnClickListener(v -> synthesizeNovelChemicalWithHenry());

        setupQuickCompounds();
        updateMixerUI();
    }

    private void showElementSelectorDialog() {
        String[] names = new String[ELEMENTS.length];
        for (int i = 0; i < ELEMENTS.length; i++) {
            names[i] = ELEMENTS[i].number + ". " + ELEMENTS[i].name + " (" + ELEMENTS[i].symbol + ") - " + ELEMENTS[i].category;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Element to Add:")
                .setItems(names, (dialog, which) -> {
                    Elem chosen = ELEMENTS[which];
                    boolean already = false;
                    for (Elem e : selectedReactantElements) {
                        if (e.symbol.equalsIgnoreCase(chosen.symbol)) {
                            already = true;
                            break;
                        }
                    }
                    if (!already) {
                        selectedReactantElements.add(chosen);
                    } else {
                        Toast.makeText(this, chosen.name + " is already in the reaction vessel.", Toast.LENGTH_SHORT).show();
                    }
                    updateMixerUI();
                })
                .show();
    }

    private void renderSelectedReactantChips() {
        selectedElementsContainer.removeAllViews();
        if (selectedReactantElements.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No elements in vessel. Tap '+ ADD ELEMENT' above to select reactants.");
            emptyText.setTextColor(0xFF6B8A9E);
            emptyText.setTextSize(12f);
            emptyText.setPadding(dp(8), dp(8), dp(8), dp(8));
            selectedElementsContainer.addView(emptyText);
            return;
        }

        for (int i = 0; i < selectedReactantElements.size(); i++) {
            final Elem elem = selectedReactantElements.get(i);

            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(10), dp(6), dp(8), dp(6));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(0x220A1E34);
            gd.setStroke(dp(1), Color.parseColor(elem.color));
            gd.setCornerRadius(dp(14));
            chip.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);

            TextView zNum = new TextView(this);
            zNum.setText(String.valueOf(elem.number));
            zNum.setTextSize(9f);
            zNum.setTextColor(0xFF88AABB);
            zNum.setPadding(0, 0, dp(4), 0);
            chip.addView(zNum);

            TextView sym = new TextView(this);
            sym.setText(elem.symbol);
            sym.setTextSize(13f);
            sym.setTypeface(Typeface.DEFAULT_BOLD);
            sym.setTextColor(Color.parseColor(elem.color));
            chip.addView(sym);

            TextView name = new TextView(this);
            name.setText(" " + elem.name);
            name.setTextSize(11f);
            name.setTextColor(0xFFD8E9F8);
            name.setPadding(0, 0, dp(6), 0);
            chip.addView(name);

            // [×] remove button
            TextView btnRemove = new TextView(this);
            btnRemove.setText("✕");
            btnRemove.setTextSize(11f);
            btnRemove.setTextColor(0xFFFF6B6B);
            btnRemove.setPadding(dp(4), dp(2), dp(4), dp(2));
            btnRemove.setOnClickListener(v -> {
                selectedReactantElements.remove(elem);
                updateMixerUI();
            });
            chip.addView(btnRemove);

            selectedElementsContainer.addView(chip);
        }
    }

    private void updateMixerUI() {
        renderSelectedReactantChips();

        matchingCompounds.clear();
        if (!selectedReactantElements.isEmpty()) {
            for (Compound c : COMPOUNDS) {
                if (c.canFormFrom(selectedReactantElements)) {
                    matchingCompounds.add(c);
                }
            }
        }

        // Render multiple matching compounds cards
        if (matchingCompounds.size() > 1) {
            cardMultipleCompounds.setVisibility(View.VISIBLE);
            tvMultipleCompoundsTitle.setText("POSSIBLE CHEMICALS FORMED (" + matchingCompounds.size() + " SYNTHESIS PATHS AVAILABLE)");
            multipleCompoundsChips.removeAllViews();

            for (int i = 0; i < matchingCompounds.size(); i++) {
                final Compound comp = matchingCompounds[i];
                boolean isSelected = (selectedCompound == comp || (selectedCompound == null && i == 0));
                if (isSelected && selectedCompound == null) {
                    selectedCompound = comp;
                }

                TextView chip = new TextView(this);
                chip.setText((isSelected ? "▶ " : "") + comp.formula + " (" + comp.common + ")");
                chip.setTextSize(11f);
                chip.setTypeface(Typeface.DEFAULT_BOLD);
                chip.setTextColor(isSelected ? 0xFF00FFCC : 0xFF88AABB);
                chip.setPadding(dp(12), dp(6), dp(12), dp(6));

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(isSelected ? 0x2800FFCC : 0x140B1A2E);
                gd.setStroke(dp(1), isSelected ? 0xFF00FFCC : 0x33446688);
                gd.setCornerRadius(dp(14));
                chip.setBackground(gd);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, dp(8), 0);
                chip.setLayoutParams(lp);

                chip.setOnClickListener(v -> {
                    selectedCompound = comp;
                    updateMixerUI();
                });
                multipleCompoundsChips.addView(chip);
            }
        } else {
            cardMultipleCompounds.setVisibility(View.GONE);
            if (!matchingCompounds.isEmpty()) {
                selectedCompound = matchingCompounds.get(0);
            } else {
                selectedCompound = null;
            }
        }

        // Display current compound details or instructions
        if (selectedCompound != null && matchingCompounds.contains(selectedCompound)) {
            cardMixerResult.setVisibility(View.VISIBLE);
            tvCompoundFormula.setText(selectedCompound.formula + " • " + selectedCompound.name);
            tvCompoundCommon.setText("Common Name: " + selectedCompound.common + " (" + selectedCompound.category + ")");
            tvCompoundStructure.setText(selectedCompound.structure);
            tvCompoundBond.setText(selectedCompound.bond);
            tvCompoundSynthesis.setText(selectedCompound.synthesis);
            tvCompoundUses.setText(selectedCompound.uses);
        } else if (!matchingCompounds.isEmpty()) {
            selectedCompound = matchingCompounds.get(0);
            cardMixerResult.setVisibility(View.VISIBLE);
            tvCompoundFormula.setText(selectedCompound.formula + " • " + selectedCompound.name);
            tvCompoundCommon.setText("Common Name: " + selectedCompound.common + " (" + selectedCompound.category + ")");
            tvCompoundStructure.setText(selectedCompound.structure);
            tvCompoundBond.setText(selectedCompound.bond);
            tvCompoundSynthesis.setText(selectedCompound.synthesis);
            tvCompoundUses.setText(selectedCompound.uses);
        } else if (selectedReactantElements.size() >= 2) {
            cardMixerResult.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            for (Elem e : selectedReactantElements) {
                if (sb.length() > 0) sb.append(" + ");
                sb.append(e.symbol);
            }
            tvCompoundFormula.setText(sb.toString() + " (Uncatalogued Binary/Multi-Phase)");
            tvCompoundCommon.setText("No standard everyday room-temperature compound catalogued.");
            tvCompoundStructure.setText("Multi-element precursor blend. Requires high-temperature sintering, chemical vapor deposition, or laser induction.");
            tvCompoundBond.setText("Predicted intermetallic coordination / complex ionic crystal lattice.");
            tvCompoundSynthesis.setText("High-temperature vacuum furnace melting (>1,200°C) or plasma spark sintering.");
            tvCompoundUses.setText("• Potential high-entropy alloy or custom catalytic formulation.\n• Tap 'SYNTHESIZE NOVEL CHEMICAL WITH HENRY' below to predict a novel material!");
        } else {
            cardMixerResult.setVisibility(View.VISIBLE);
            tvCompoundFormula.setText("Awaiting Reactants");
            tvCompoundCommon.setText("Add 2 or more elements to inspect molecular reaction paths.");
            tvCompoundStructure.setText("--");
            tvCompoundBond.setText("--");
            tvCompoundSynthesis.setText("--");
            tvCompoundUses.setText("--");
        }
    }

    private void synthesizeNovelChemicalWithHenry() {
        if (selectedReactantElements.isEmpty()) {
            Toast.makeText(this, "Please add at least one element to the vessel.", Toast.LENGTH_SHORT).show();
            return;
        }

        novelChemicalOutput.setVisibility(View.VISIBLE);

        Set<String> syms = new HashSet<>();
        for (Elem e : selectedReactantElements) syms.add(e.symbol.toUpperCase(Locale.US));

        String name;
        String formula;
        String structure;
        String synthesis;
        String uses;

        if (syms.contains("C") && syms.contains("N")) {
            name = "Carbon Nitride Super-Diamond Lattice (β-C₃N₄)";
            formula = "β-C₃N₄";
            structure = "Hexagonal Network of Corner-Sharing sp³ Carbon and sp² Nitrogen atoms with extreme bond energy and bulk modulus rivaling or exceeding natural diamond.";
            synthesis = "Laser-heated diamond anvil cell synthesis under ultra-high pressure (70 GPa) and 2,500 K from triazine and dicyandiamide precursors.";
            uses = "• Ultra-wear-resistant cutting tools for titanium and aerospace superalloys.\n• High-temperature semiconductor heat spreaders.\n• Next-generation bulletproof transparent protective shields.";
        } else if (syms.contains("TI") && (syms.contains("B") || syms.contains("C"))) {
            name = "Titanium Diboride Ultra-Refractory Ceramic";
            formula = "TiB₂";
            structure = "Hexagonal AlB₂-type crystal structure featuring alternating close-packed titanium sheets and planar hexagonal boron rings.";
            synthesis = "Carbothermal boronation of TiO₂ and B₂O₃ in vacuum induction furnace at 1,900°C.";
            uses = "• Hypersonic leading-edge thermal protection shields (stable to 3,000°C).\n• Molten metal evaporation crucibles (impervious to molten aluminum attack).\n• Lightweight ceramic ballistic body armor plates.";
        } else if (syms.contains("H") && (syms.contains("LA") || syms.contains("Y") || syms.contains("CA"))) {
            name = "Lanthanide Clathrate Room-Temperature Superconductor Precursor";
            formula = "LaH₁₀";
            structure = "Face-centered cubic clathrate cage structure with 32 hydrogen atoms encapsulating central Lanthanum cations; high phonon frequency electron pairing.";
            synthesis = "Direct laser heating of Lanthanum foil in pure liquid hydrogen medium inside a Diamond Anvil Cell at 170 GPa.";
            uses = "• Near-room-temperature superconductivity (Tc = 250 K / -23°C).\n• Zero-resistance magnetic levitation transport.\n• Lossless continental electric power grids and compact fusion tokamak magnet coils.";
        } else if (syms.contains("C")) {
            name = "Twisted Bilayer Graphene Moiré Super-Lattice";
            formula = "C₂ (θ = 1.08° 'Magic Angle')";
            structure = "Two atomically thin graphene monolayers rotated precisely to the 1.08° 'magic angle', inducing flat energy bands and correlated electron superconductivity.";
            synthesis = "Tear-and-stack dry viscoelastic stamp transfer under ultra-high vacuum with atomic force microscope rotational angle alignment.";
            uses = "• Quantum computing topological qubits.\n• Superconducting field-effect transistors with ultra-low thermal dissipation.\n• Ballistic quantum sensory arrays.";
        } else if (syms.contains("SI") && syms.contains("C")) {
            name = "Silicon Oxycarbide (SiCO) High-Temperature Aerogel Ceramic";
            formula = "SiC_{x}O_{y}";
            structure = "Amorphous covalently bonded silicon, carbon, and oxygen network with embedded turbostratic carbon graphene nanodomains.";
            synthesis = "Polymer-derived ceramic (PDC) route: crosslinking polycarbosilane followed by pyrolysis under argon at 1,100°C.";
            uses = "• Extreme aerospace insulation up to 1,500°C in oxidizing environments.\n• High-capacity anode material for fast-charging lithium and sodium batteries.\n• Radiation-resistant nuclear cladding.";
        } else {
            // General multi-element novel metal-organic framework / hyper-alloy
            StringBuilder formulaBuilder = new StringBuilder();
            StringBuilder elemNames = new StringBuilder();
            for (Elem e : selectedReactantElements) {
                formulaBuilder.append(e.symbol);
                if (elemNames.length() > 0) elemNames.append("-");
                elemNames.append(e.symbol);
            }
            formulaBuilder.append("ₙ (HENRY-MOF-").append(selectedReactantElements.size()).append(")");

            name = "HENRY Metallo-Crystalline Framework (" + elemNames.toString() + ")";
            formula = formulaBuilder.toString();
            structure = "Engineered sub-nanometer coordination matrix with customizable pore topology, ultra-high surface area (>6,000 m²/g), and coordinated unsaturated metallic sites.";
            synthesis = "Solvothermal micro-crystallization in autoclaves under microwave-assisted heating at 180°C for 24 hours.";
            uses = "• Direct air capture of atmospheric carbon dioxide.\n• Solar-driven drinking water extraction from low-humidity desert air.\n• Safe high-density hydrogen fuel storage without cryogenic pressurization.";
        }

        tvNovelName.setText(name);
        tvNovelFormula.setText("Formula: " + formula);
        tvNovelStructure.setText(structure);
        tvNovelSynthesis.setText(synthesis);
        tvNovelUses.setText(uses);

        speakHenry("Novel synthesis analysis complete. I have calculated the synthesis parameters for " + name + ".");
    }

    private void setupQuickCompounds() {
        commonCompoundsContainer.removeAllViews();
        String[] quickCombos = {
                "Water (H₂O)", "Table Salt (NaCl)", "Carbon Dioxide (CO₂)",
                "Methane (CH₄)", "Ethanol (C₂H₅OH)", "Glucose (C₆H₁₂O₆)",
                "Ammonia (NH₃)", "Nitinol (NiTi)", "Titanium-Gold (β-Ti₃Au)",
                "Stainless Steel (Fe-Cr-Ni-C)", "Silicon Dioxide (SiO₂)",
                "Silicon Carbide (SiC)", "Gallium Nitride (GaN)"
        };
        String[][] combos = {
                {"H", "O"}, {"Na", "Cl"}, {"C", "O"},
                {"C", "H"}, {"C", "H", "O"}, {"C", "H", "O"},
                {"N", "H"}, {"Ni", "Ti"}, {"Ti", "Au"},
                {"Fe", "C", "Cr", "Ni"}, {"Si", "O"},
                {"Si", "C"}, {"Ga", "N"}
        };

        for (int i = 0; i < quickCombos.length; i++) {
            final int index = i;
            TextView pill = new TextView(this);
            pill.setText(quickCombos[i]);
            pill.setTextColor(0xFF00FFCC);
            pill.setTextSize(11f);
            pill.setPadding(dp(12), dp(6), dp(12), dp(6));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(0x1800FFCC);
            gd.setStroke(dp(1), 0x5500FFCC);
            gd.setCornerRadius(dp(14));
            pill.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            pill.setLayoutParams(lp);

            pill.setOnClickListener(v -> {
                selectedReactantElements.clear();
                for (String sym : combos[index]) {
                    selectedReactantElements.add(findElement(sym));
                }
                updateMixerUI();
            });
            commonCompoundsContainer.addView(pill);
        }
    }

    // ── Mode 4: Bohr Model Setup ──────────────────────────────────────────────
    private void setupBohrPanel() {
        bohrAtomCanvas       = findViewById(R.id.bohr_atom_canvas);
        tvBohrElementTitle   = findViewById(R.id.tv_bohr_element_title);
        tvBohrEconfig        = findViewById(R.id.tv_bohr_econfig);
        tvBohrShellsDetail   = findViewById(R.id.tv_bohr_shells_detail);
        tvBohrValence        = findViewById(R.id.tv_bohr_valence);
        tvBohrSummary        = findViewById(R.id.tv_bohr_summary);

        if (selectedElement != null) updateBohrView(selectedElement);
    }

    private void updateBohrView(Elem e) {
        if (bohrAtomCanvas != null) {
            bohrAtomCanvas.setElement(e.symbol, e.number, Color.parseColor(e.color));
        }
        if (tvBohrElementTitle != null) {
            tvBohrElementTitle.setText(e.name + " (" + e.symbol + ") • Atomic #" + e.number);
            tvBohrElementTitle.setTextColor(Color.parseColor(e.color));
        }
        if (tvBohrEconfig != null) {
            tvBohrEconfig.setText("Electron Config: " + e.econfig + "  •  Weight: " + e.weight);
        }
        if (tvBohrShellsDetail != null) {
            tvBohrShellsDetail.setText("Category: " + e.category + "  •  Period " + e.row + ", Group " + e.col);
        }
        if (tvBohrValence != null) {
            tvBohrValence.setText("Primary Applications: " + (e.uses.isEmpty() ? "Scientific laboratory research" : e.uses));
        }
        if (tvBohrSummary != null) {
            tvBohrSummary.setText(e.summary);
        }
    }

    // ── Full Holographic Element Detail Modal ────────────────────────────────
    private void showElementDetailModal(Elem e) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF071220);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(16), dp(20), dp(16));

        // Real Photo Preview (Async)
        ImageView img = new ImageView(this);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(160));
        imgLp.bottomMargin = dp(12);
        img.setLayoutParams(imgLp);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(0xFF0D1B2A);
        container.addView(img);

        ProgressBar spinner = new ProgressBar(this);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(dp(32), dp(32));
        spinLp.gravity = Gravity.CENTER;
        spinner.setLayoutParams(spinLp);
        container.addView(spinner);
        loadImageAsync(e.image, img, spinner);

        // Header Title
        TextView name = new TextView(this);
        name.setText(e.name.toUpperCase(Locale.US) + " (" + e.symbol + ") • #" + e.number);
        name.setTextColor(Color.parseColor(e.color));
        name.setTextSize(18f);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        container.addView(name);

        addModalPropertyRow(container, "CATEGORY", e.category);
        addModalPropertyRow(container, "ATOMIC WEIGHT", e.weight);
        addModalPropertyRow(container, "ELECTRON CONFIGURATION", e.econfig);
        addModalPropertyRow(container, "SUMMARY & OCCURRENCE", e.summary);
        addModalPropertyRow(container, "COMMON & INDUSTRIAL USES", e.uses.isEmpty() ? "Specialized aerospace / laboratory research." : e.uses);

        // Action Buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        btnRow.setPadding(0, dp(16), 0, 0);

        TextView btnBohr = new TextView(this);
        btnBohr.setText("🔬 BOHR ORBITALS");
        btnBohr.setTextColor(0xFF00FFCC);
        btnBohr.setTextSize(12f);
        btnBohr.setTypeface(Typeface.DEFAULT_BOLD);
        btnBohr.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnBohr.setBackgroundColor(0x2500FFCC);
        btnBohr.setOnClickListener(v -> {
            updateBohrView(e);
            switchMode(Mode.BOHR);
        });
        btnRow.addView(btnBohr);

        container.addView(btnRow);
        scroll.addView(container);

        new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Close", null)
                .show();
    }

    private void addModalPropertyRow(LinearLayout parent, String label, String value) {
        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFF6088B0);
        lbl.setTextSize(10f);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setPadding(0, dp(8), 0, dp(2));
        parent.addView(lbl);

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(0xFFE2F0FF);
        val.setTextSize(13f);
        parent.addView(val);
    }

    private void speakCurrentElementTelemetry() {
        if (selectedElement == null) return;
        String text = selectedElement.name + ", symbol " + selectedElement.symbol + ", atomic number " +
                selectedElement.number + ". " + selectedElement.summary;
        speakHenry(text);
    }

    private void speakHenry(String text) {
        if (ttsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HENRY_CHEM");
        }
    }

    private static Compound findCompound(String a, String b) {
        List<Elem> list = new ArrayList<>();
        list.add(findElement(a));
        list.add(findElement(b));
        for (Compound c : COMPOUNDS) {
            if (c.canFormFrom(list)) return c;
        }
        return null;
    }

    private static Elem findElement(String symbol) {
        for (Elem e : ELEMENTS) if (e.symbol.equalsIgnoreCase(symbol)) return e;
        return ELEMENTS[0];
    }

    private void loadImageAsync(String url, ImageView target, ProgressBar spinner) {
        if (url == null || url.isEmpty()) { spinner.setVisibility(View.GONE); return; }
        new Thread(() -> {
            Bitmap bmp = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("User-Agent", "HENRY-Android");
                bmp = BitmapFactory.decodeStream(conn.getInputStream());
            } catch (Exception ignored) {}
            final Bitmap result = bmp;
            mainHandler.post(() -> {
                spinner.setVisibility(View.GONE);
                if (result != null) target.setImageBitmap(result);
                else target.setBackgroundColor(0xFF0D1B2A);
            });
        }).start();
    }
}
