package com.jarvis.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Periodic Table — drag one element onto another to see what real compound
 * they form (formula, structure, bond type, a fact). Tap a single element to
 * see its real data: atomic number, electron configuration, a genuine
 * Wikipedia-sourced summary, common uses, and a real photo.
 *
 * All element data (electron config, summary, image) pulled from
 * Bowserinator/Periodic-Table-JSON, a real open-source dataset — not
 * fabricated. Common-uses text is curated for well-known elements only;
 * unlisted elements show that honestly rather than inventing a use.
 *
 * Voice/text trigger: "periodic table" / "element mixer" / "chemistry lab"
 */
public class PeriodicTableActivity extends AppCompatActivity {

    private static final int CELL = 44; // dp per element tile
    private static final int TAP_THRESHOLD_DP = 10;
    private FrameLayout tableLayer;
    private LinearLayout resultPanel;
    private TextView resultText;
    private final List<ElemView> elemViews = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Data models ──────────────────────────────────────────────────────────
    private static class Elem {
        String symbol, name, color, econfig, summary, image, uses;
        int number, row, col;
        Elem(String s, String n, int num, String c, int r, int cl,
             String econfig, String summary, String image, String uses) {
            symbol=s; name=n; number=num; color=c; row=r; col=cl;
            this.econfig=econfig; this.summary=summary; this.image=image; this.uses=uses;
        }
    }
    private static class Compound {
        String a, b, formula, name, common, fact, structure, bond;
        Compound(String a, String b, String f, String n, String c, String fact, String structure, String bond) {
            this.a=a; this.b=b; formula=f; name=n; common=c; this.fact=fact;
            this.structure=structure; this.bond=bond;
        }
        boolean matches(String x, String y) {
            return (a.equals(x) && b.equals(y)) || (a.equals(y) && b.equals(x));
        }
    }

    private static final Elem[] ELEMENTS = {
            new Elem("H","Hydrogen",1,"#69DB7C",1,1,"1s1","Hydrogen is a chemical element with chemical symbol H and atomic number 1. With an atomic weight of 1.00794 u, hydrogen is the lightest element on the periodic table.","https://upload.wikimedia.org/wikipedia/commons/d/d9/Hydrogenglow.jpg","Rocket fuel, ammonia production, hydrogenating fats"),
            new Elem("He","Helium",2,"#845EF7",1,18,"1s2","Helium is a chemical element with symbol He and atomic number 2. It is a colorless, odorless, tasteless, non-toxic, inert, monatomic gas that heads the noble gas group in the periodic table. Its boiling and melting points are the lowest among all the elements.","https://upload.wikimedia.org/wikipedia/commons/0/00/Helium-glow.jpg","Balloons, MRI magnet cooling, deep-sea diving gas mixes"),
            new Elem("Li","Lithium",3,"#FF6B6B",2,1,"[He] 2s1","Lithium (from Greek:λίθος lithos, \\\"stone\\\") is a chemical element with the symbol Li and atomic number 3. It is a soft, silver-white metal belonging to the alkali metal group of chemical elements.","https://upload.wikimedia.org/wikipedia/commons/e/e2/0.5_grams_lithium_under_argon.jpg","Rechargeable batteries, mood-stabilizing medication"),
            new Elem("Be","Beryllium",4,"#FFA94D",2,2,"[He] 2s2","Beryllium is a chemical element with symbol Be and atomic number 4. It is created through stellar nucleosynthesis and is a relatively rare element in the universe. It is a divalent element which occurs naturally only in combination with other elements in minerals.","https://upload.wikimedia.org/wikipedia/commons/e/e2/Beryllium_%28Be%29.jpg",""),
            new Elem("B","Boron",5,"#FFD43B",2,13,"[He] 2s2 2p1","Boron is a metalloid chemical element with symbol B and atomic number 5. Produced entirely by cosmic ray spallation and supernovae and not by stellar nucleosynthesis, it is a low-abundance element in both the Solar system and the Earth's crust.","https://upload.wikimedia.org/wikipedia/commons/a/a2/Boron.jpg",""),
            new Elem("C","Carbon",6,"#69DB7C",2,14,"[He] 2s2 2p2","Carbon (from Latin:carbo \\\"coal\\\") is a chemical element with symbol C and atomic number 6. On the periodic table, it is the first (row 2) of six elements in column (group) 14, which have in common the composition of their outer electron shell.","https://upload.wikimedia.org/wikipedia/commons/6/68/Pure_Carbon.png","Steel-making, pencils (graphite), diamonds, all organic life"),
            new Elem("N","Nitrogen",7,"#69DB7C",2,15,"[He] 2s2 2p3","Nitrogen is a chemical element with symbol N and atomic number 7. It is the lightest pnictogen and at room temperature, it is a transparent, odorless diatomic gas.","https://upload.wikimedia.org/wikipedia/commons/2/2d/Nitrogen-glow.jpg","Fertilizer, food packaging (inert atmosphere), liquid nitrogen freezing"),
            new Elem("O","Oxygen",8,"#69DB7C",2,16,"[He] 2s2 2p4","Oxygen is a chemical element with symbol O and atomic number 8. It is a member of the chalcogen group on the periodic table and is a highly reactive nonmetal and oxidizing agent that readily forms compounds (notably oxides) with most elements.","https://upload.wikimedia.org/wikipedia/commons/a/a0/Liquid_oxygen_in_a_beaker_%28cropped_and_retouched%29.jpg","Breathing, welding, steel production, medical oxygen"),
            new Elem("F","Fluorine",9,"#FF922B",2,17,"[He] 2s2 2p5","Fluorine is a chemical element with symbol F and atomic number 9. It is the lightest halogen and exists as a highly toxic pale yellow diatomic gas at standard conditions.","https://upload.wikimedia.org/wikipedia/commons/2/2c/Fluoro_liquido_a_-196%C2%B0C_1.jpg","Toothpaste (fluoride), Teflon, refrigerants"),
            new Elem("Ne","Neon",10,"#845EF7",2,18,"[He] 2s2 2p6","Neon is a chemical element with symbol Ne and atomic number 10. It is in group 18 (noble gases) of the periodic table. Neon is a colorless, odorless, inert monatomic gas under standard conditions, with about two-thirds the density of air.","https://upload.wikimedia.org/wikipedia/commons/f/f8/Neon-glow.jpg","Neon signs, high-voltage indicators"),
            new Elem("Na","Sodium",11,"#FF6B6B",3,1,"[Ne] 3s1","Sodium /ˈsoʊdiəm/ is a chemical element with symbol Na (from Ancient Greek Νάτριο) and atomic number 11. It is a soft, silver-white, highly reactive metal.","https://upload.wikimedia.org/wikipedia/commons/2/27/Na_%28Sodium%29.jpg","Table salt, streetlights, soap-making"),
            new Elem("Mg","Magnesium",12,"#FFA94D",3,2,"[Ne] 3s2","Magnesium is a chemical element with symbol Mg and atomic number 12. It is a shiny gray solid which bears a close physical resemblance to the other five elements in the second column (Group 2, or alkaline earth metals) of the periodic table:they each have the same electron...","https://upload.wikimedia.org/wikipedia/commons/3/3f/Magnesium_crystals.jpg","Fireworks, lightweight alloys, dietary supplements"),
            new Elem("Al","Aluminium",13,"#9775FA",3,13,"[Ne] 3s2 3p1","Aluminium (or aluminum; see different endings) is a chemical element in the boron group with symbol Al and atomic number 13. It is a silvery-white, soft, nonmagnetic, ductile metal.","https://upload.wikimedia.org/wikipedia/commons/3/3e/Aluminium.jpg","Cans, foil, aircraft parts, window frames"),
            new Elem("Si","Silicon",14,"#FFD43B",3,14,"[Ne] 3s2 3p2","Silicon is a chemical element with symbol Si and atomic number 14. It is a tetravalent metalloid, more reactive than germanium, the metalloid directly below it in the table. Controversy about silicon's character dates to its discovery.","https://upload.wikimedia.org/wikipedia/commons/2/2c/Silicon.jpg","Computer chips, glass, solar panels"),
            new Elem("P","Phosphorus",15,"#69DB7C",3,15,"[Ne] 3s2 3p3","Phosphorus is a chemical element with symbol P and atomic number 15. As an element, phosphorus exists in two major forms—white phosphorus and red phosphorus—but due to its high reactivity, phosphorus is never found as a free element on Earth.","https://upload.wikimedia.org/wikipedia/commons/6/6d/Phosphorus-purple.jpg","Fertilizer, matches, DNA/bone structure"),
            new Elem("S","Sulfur",16,"#69DB7C",3,16,"[Ne] 3s2 3p4","Sulfur or sulphur (see spelling differences) is a chemical element with symbol S and atomic number 16. It is an abundant, multivalent non-metal. Under normal conditions, sulfur atoms form cyclic octatomic molecules with chemical formula S8.","https://upload.wikimedia.org/wikipedia/commons/2/23/Native_sulfur_%28Vodinskoe_Deposit%3B_quarry_near_Samara%2C_Russia%29_9.jpg","Sulfuric acid, rubber vulcanization, gunpowder"),
            new Elem("Cl","Chlorine",17,"#FF922B",3,17,"[Ne] 3s2 3p5","Chlorine is a chemical element with symbol Cl and atomic number 17. It also has a relative atomic mass of 35.5. Chlorine is in the halogen group (17) and is the second lightest halogen following fluorine.","https://upload.wikimedia.org/wikipedia/commons/9/9a/Chlorine-sample-flip.jpg","Water disinfection, PVC plastic, bleach"),
            new Elem("Ar","Argon",18,"#845EF7",3,18,"[Ne] 3s2 3p6","Argon is a chemical element with symbol Ar and atomic number 18. It is in group 18 of the periodic table and is a noble gas.","https://upload.wikimedia.org/wikipedia/commons/5/53/Argon-glow.jpg","Inert shielding gas for welding, incandescent bulb filling"),
            new Elem("K","Potassium",19,"#FF6B6B",4,1,"[Ar] 4s1","Potassium is a chemical element with symbol K (derived from Neo-Latin, kalium) and atomic number 19. It was first isolated from potash, the ashes of plants, from which its name is derived.","https://upload.wikimedia.org/wikipedia/commons/b/b3/Potassium.JPG","Fertilizer, banana ripening, muscle/nerve function"),
            new Elem("Ca","Calcium",20,"#FFA94D",4,2,"[Ar] 4s2","Calcium is a chemical element with symbol Ca and atomic number 20. Calcium is a soft gray alkaline earth metal, fifth-most-abundant element by mass in the Earth's crust.","https://upload.wikimedia.org/wikipedia/commons/7/72/Calcium.jpg","Bones and teeth, cement, chalk"),
            new Elem("Sc","Scandium",21,"#4DABF7",4,3,"[Ar] 3d1 4s2","Scandium is a chemical element with symbol Sc and atomic number 21. A silvery-white metallic d-block element, it has historically been sometimes classified as a rare earth element, together with yttrium and the lanthanoids.","https://upload.wikimedia.org/wikipedia/commons/f/f5/Scandium%2C_Sc.jpg","Aerospace alloys, stadium lighting"),
            new Elem("Ti","Titanium",22,"#4DABF7",4,4,"[Ar] 3d2 4s2","Titanium is a chemical element with symbol Ti and atomic number 22. It is a lustrous transition metal with a silver color, low density and high strength. It is highly resistant to corrosion in sea water, aqua regia and chlorine.","https://upload.wikimedia.org/wikipedia/commons/e/ec/Titanium.jpg","Aircraft frames, hip replacements, white paint pigment"),
            new Elem("V","Vanadium",23,"#4DABF7",4,5,"[Ar] 3d3 4s2","Vanadium is a chemical element with symbol V and atomic number 23. It is a hard, silvery grey, ductile and malleable transition metal.","https://upload.wikimedia.org/wikipedia/commons/0/0a/Vanadium-pieces.jpg","Steel alloys (strength), catalysts"),
            new Elem("Cr","Chromium",24,"#4DABF7",4,6,"[Ar] 3d5 4s1","Chromium is a chemical element with symbol Cr and atomic number 24. It is the first element in Group 6. It is a steely-gray, lustrous, hard and brittle metal which takes a high polish, resists tarnishing, and has a high melting point.","https://upload.wikimedia.org/wikipedia/commons/a/a1/Chromium.jpg","Stainless steel, chrome plating"),
            new Elem("Mn","Manganese",25,"#4DABF7",4,7,"[Ar] 3d5 4s2","Manganese is a chemical element with symbol Mn and atomic number 25. It is not found as a free element in nature; it is often found in combination with iron, and in many minerals. Manganese is a metal with important industrial metal alloy uses, particularly in stainless steels.","https://upload.wikimedia.org/wikipedia/commons/6/64/Manganese_element.jpg","Steel-making, batteries"),
            new Elem("Fe","Iron",26,"#4DABF7",4,8,"[Ar] 3d6 4s2","Iron is a chemical element with symbol Fe (from Latin:ferrum) and atomic number 26. It is a metal in the first transition series. It is by mass the most common element on Earth, forming much of Earth's outer and inner core.","https://images-of-elements.com/iron-2.jpg","Steel, construction, hemoglobin in blood"),
            new Elem("Co","Cobalt",27,"#4DABF7",4,9,"[Ar] 3d7 4s2","Cobalt is a chemical element with symbol Co and atomic number 27. Like nickel, cobalt in the Earth's crust is found only in chemically combined form, save for small deposits found in alloys of natural meteoric iron.","https://upload.wikimedia.org/wikipedia/commons/6/62/Cobalt_ore_2.jpg","Rechargeable battery cathodes, blue pigment, magnets"),
            new Elem("Ni","Nickel",28,"#4DABF7",4,10,"[Ar] 3d8 4s2","Nickel is a chemical element with symbol Ni and atomic number 28. It is a silvery-white lustrous metal with a slight golden tinge. Nickel belongs to the transition metals and is hard and ductile.","https://upload.wikimedia.org/wikipedia/commons/5/57/Nickel_chunk.jpg","Stainless steel, coins, rechargeable batteries"),
            new Elem("Cu","Copper",29,"#4DABF7",4,11,"[Ar] 3d10 4s1","Copper is a chemical element with symbol Cu (from Latin:cuprum) and atomic number 29. It is a soft, malleable and ductile metal with very high thermal and electrical conductivity. A freshly exposed surface of pure copper has a reddish-orange color.","https://upload.wikimedia.org/wikipedia/commons/f/f0/NatCopper.jpg","Electrical wiring, plumbing, coins"),
            new Elem("Zn","Zinc",30,"#4DABF7",4,12,"[Ar] 3d10 4s2","Zinc, in commerce also spelter, is a chemical element with symbol Zn and atomic number 30. It is the first element of group 12 of the periodic table. In some respects zinc is chemically similar to magnesium:its ion is of similar size and its only common oxidation state is +2.","https://upload.wikimedia.org/wikipedia/commons/b/ba/Zinc_%2830_Zn%29.jpg","Galvanizing steel, batteries, sunscreen"),
            new Elem("Ga","Gallium",31,"#9775FA",4,13,"[Ar] 3d10 4s2 4p1","Gallium is a chemical element with symbol Ga and atomic number 31. Elemental gallium does not occur in free form in nature, but as the gallium(III) compounds that are in trace amounts in zinc ores and in bauxite.","https://upload.wikimedia.org/wikipedia/commons/b/b1/Solid_gallium_%28Ga%29.jpg","LEDs, semiconductors"),
            new Elem("Ge","Germanium",32,"#FFD43B",4,14,"[Ar] 3d10 4s2 4p2","Germanium is a chemical element with symbol Ge and atomic number 32. It is a lustrous, hard, grayish-white metalloid in the carbon group, chemically similar to its group neighbors tin and silicon.","https://upload.wikimedia.org/wikipedia/commons/0/08/Polycrystalline-germanium.jpg","Fiber optics, infrared optics, semiconductors"),
            new Elem("As","Arsenic",33,"#FFD43B",4,15,"[Ar] 3d10 4s2 4p3","Arsenic is a chemical element with symbol As and atomic number 33. Arsenic occurs in many minerals, usually in conjunction with sulfur and metals, and also as a pure elemental crystal. Arsenic is a metalloid.","https://upload.wikimedia.org/wikipedia/commons/3/3b/Arsenic_%2833_As%29.jpg","Semiconductors (historically also pesticides — now restricted)"),
            new Elem("Se","Selenium",34,"#69DB7C",4,16,"[Ar] 3d10 4s2 4p4","Selenium is a chemical element with symbol Se and atomic number 34. It is a nonmetal with properties that are intermediate between those of its periodic table column-adjacent chalcogen elements sulfur and tellurium.","https://upload.wikimedia.org/wikipedia/commons/7/7f/Selenium.jpg","Photocopiers, glassmaking, dietary supplement"),
            new Elem("Br","Bromine",35,"#FF922B",4,17,"[Ar] 3d10 4s2 4p5","Bromine (from Ancient Greek:βρῶμος, brómos, meaning \\\"stench\\\") is a chemical element with symbol Br, and atomic number 35. It is a halogen. The element was isolated independently by two chemists, Carl Jacob Löwig and Antoine Jerome Balard, in 1825–1826.","https://upload.wikimedia.org/wikipedia/commons/8/87/Bromine-ampoule.jpg","Flame retardants, photography chemicals"),
            new Elem("Kr","Krypton",36,"#845EF7",4,18,"[Ar] 3d10 4s2 4p6","Krypton (from Greek:κρυπτός kryptos \\\"the hidden one\\\") is a chemical element with symbol Kr and atomic number 36. It is a member of group 18 (noble gases) elements.","https://upload.wikimedia.org/wikipedia/commons/9/9c/Krypton-glow.jpg","Camera flash bulbs, fluorescent lighting"),
            new Elem("Rb","Rubidium",37,"#FF6B6B",5,1,"[Kr] 5s1","Rubidium is a chemical element with symbol Rb and atomic number 37. Rubidium is a soft, silvery-white metallic element of the alkali metal group, with an atomic mass of 85.4678.","https://upload.wikimedia.org/wikipedia/commons/c/c9/Rb5.JPG","Atomic clocks, research"),
            new Elem("Sr","Strontium",38,"#FFA94D",5,2,"[Kr] 5s2","Strontium is a chemical element with symbol Sr and atomic number 38. An alkaline earth metal, strontium is a soft silver-white or yellowish metallic element that is highly reactive chemically. The metal turns yellow when it is exposed to air.","https://upload.wikimedia.org/wikipedia/commons/8/84/Strontium-1.jpg","Red fireworks, fluorescent lighting"),
            new Elem("Y","Yttrium",39,"#4DABF7",5,3,"[Kr] 4d1 5s2","Yttrium is a chemical element with symbol Y and atomic number 39. It is a silvery-metallic transition metal chemically similar to the lanthanides and it has often been classified as a \\\"rare earth element\\\".","https://upload.wikimedia.org/wikipedia/commons/9/90/Piece_of_Yttrium.jpg","LED phosphors, camera lenses"),
            new Elem("Zr","Zirconium",40,"#4DABF7",5,4,"[Kr] 4d2 5s2","Zirconium is a chemical element with symbol Zr and atomic number 40. The name of zirconium is taken from the name of the mineral zircon, the most important source of zirconium. The word zircon comes from the Persian word zargun زرگون, meaning \\\"gold-colored\\\".","https://upload.wikimedia.org/wikipedia/commons/1/1d/Zirconium-pieces.jpg","Nuclear reactor cladding, ceramics, jewelry (cubic zirconia)"),
            new Elem("Nb","Niobium",41,"#4DABF7",5,5,"[Kr] 4d4 5s1","Niobium, formerly columbium, is a chemical element with symbol Nb (formerly Cb) and atomic number 41. It is a soft, grey, ductile transition metal, which is often found in the pyrochlore mineral, the main commercial source for niobium, and columbite.","https://upload.wikimedia.org/wikipedia/commons/c/c2/Niobium_strips.JPG","MRI magnet wiring (superconductors), steel alloys"),
            new Elem("Mo","Molybdenum",42,"#4DABF7",5,6,"[Kr] 4d5 5s1","Molybdenum is a chemical element with symbol Mo and atomic number 42. The name is from Neo-Latin molybdaenum, from Ancient Greek Μόλυβδος molybdos, meaning lead, since its ores were confused with lead ores.","https://upload.wikimedia.org/wikipedia/commons/f/f0/Molybdenum.jpg","High-strength steel alloys, electrodes"),
            new Elem("Tc","Technetium",43,"#4DABF7",5,7,"[Kr] 4d5 5s2","Technetium (/tɛkˈniːʃiəm/) is a chemical element with symbol Tc and atomic number 43. It is the element with the lowest atomic number in the periodic table that has no stable isotopes:every form of it is radioactive.","https://upload.wikimedia.org/wikipedia/commons/a/ab/Technetium-sample-cropped.jpg","Medical imaging (radioactive tracer)"),
            new Elem("Ru","Ruthenium",44,"#4DABF7",5,8,"[Kr] 4d7 5s1","Ruthenium is a chemical element with symbol Ru and atomic number 44. It is a rare transition metal belonging to the platinum group of the periodic table. Like the other metals of the platinum group, ruthenium is inert to most other chemicals.","https://upload.wikimedia.org/wikipedia/commons/a/a8/Ruthenium_crystal.jpg","Electrical contacts, jewelry alloys"),
            new Elem("Rh","Rhodium",45,"#4DABF7",5,9,"[Kr] 4d8 5s1","Rhodium is a chemical element with symbol Rh and atomic number 45. It is a rare, silvery-white, hard, and chemically inert transition metal. It is a member of the platinum group.","https://upload.wikimedia.org/wikipedia/commons/5/54/Rhodium_%28Rh%29.jpg","Catalytic converters, jewelry plating"),
            new Elem("Pd","Palladium",46,"#4DABF7",5,10,"[Kr] 4d10","Palladium is a chemical element with symbol Pd and atomic number 46. It is a rare and lustrous silvery-white metal discovered in 1803 by William Hyde Wollaston.","https://upload.wikimedia.org/wikipedia/commons/d/d7/Palladium_%2846_Pd%29.jpg","Catalytic converters, jewelry, electronics"),
            new Elem("Ag","Silver",47,"#4DABF7",5,11,"[Kr] 4d10 5s1","Silver is a chemical element with symbol Ag (Greek:άργυρος árguros, Latin:argentum, both from the Indo-European root *h₂erǵ- for \\\"grey\\\" or \\\"shining\\\") and atomic number 47.","https://upload.wikimedia.org/wikipedia/commons/e/e4/Silver-nugget.jpg","Jewelry, silverware, electrical contacts, mirrors"),
            new Elem("Cd","Cadmium",48,"#4DABF7",5,12,"[Kr] 4d10 5s2","Cadmium is a chemical element with symbol Cd and atomic number 48. This soft, bluish-white metal is chemically similar to the two other stable metals in group 12, zinc and mercury.","https://images-of-elements.com/cadmium-4.jpg","Rechargeable batteries, pigments"),
            new Elem("In","Indium",49,"#9775FA",5,13,"[Kr] 4d10 5s2 5p1","Indium is a chemical element with symbol In and atomic number 49. It is a post-transition metallic element that is rare in Earth's crust. The metal is very soft, malleable and easily fusible, with a melting point higher than sodium, but lower than lithium or tin.","https://images-of-elements.com/indium-2.jpg","Touchscreens, LCD displays"),
            new Elem("Sn","Tin",50,"#9775FA",5,14,"[Kr] 4d10 5s2 5p2","Tin is a chemical element with the symbol Sn (for Latin:stannum) and atomic number 50. It is a main group metal in group 14 of the periodic table.","https://upload.wikimedia.org/wikipedia/commons/6/6a/Tin-2.jpg","Tin cans, solder, bronze alloy"),
            new Elem("Sb","Antimony",51,"#FFD43B",5,15,"[Kr] 4d10 5s2 5p3","Antimony is a chemical element with symbol Sb (from Latin:stibium) and atomic number 51. A lustrous gray metalloid, it is found in nature mainly as the sulfide mineral stibnite (Sb2S3).","https://upload.wikimedia.org/wikipedia/commons/5/5c/Antimony-4.jpg","Flame retardants, batteries"),
            new Elem("Te","Tellurium",52,"#FFD43B",5,16,"[Kr] 4d10 5s2 5p4","Tellurium is a chemical element with symbol Te and atomic number 52. It is a brittle, mildly toxic, rare, silver-white metalloid. Tellurium is chemically related to selenium and sulfur.","https://upload.wikimedia.org/wikipedia/commons/c/c1/Tellurium2.jpg","Solar panels, alloys"),
            new Elem("I","Iodine",53,"#FF922B",5,17,"[Kr] 4d10 5s2 5p5","Iodine is a chemical element with symbol I and atomic number 53. The name is from Greek ἰοειδής ioeidēs, meaning violet or purple, due to the color of iodine vapor.","https://upload.wikimedia.org/wikipedia/commons/c/c2/Iodine-sample.jpg","Antiseptics, iodized salt, X-ray contrast dye"),
            new Elem("Xe","Xenon",54,"#845EF7",5,18,"[Kr] 4d10 5s2 5p6","Xenon is a chemical element with symbol Xe and atomic number 54. It is a colorless, dense, odorless noble gas, that occurs in the Earth's atmosphere in trace amounts.","https://upload.wikimedia.org/wikipedia/commons/5/5d/Xenon-glow.jpg","Car headlights (xenon lamps), anesthesia research"),
            new Elem("Cs","Cesium",55,"#FF6B6B",6,1,"[Xe] 6s1","Caesium or cesium is a chemical element with symbol Cs and atomic number 55. It is a soft, silvery-gold alkali metal with a melting point of 28 °C (82 °F), which makes it one of only five elemental metals that are liquid at or near room temperature.","https://upload.wikimedia.org/wikipedia/commons/3/3d/Cesium.jpg","Atomic clocks (defines the second)"),
            new Elem("Ba","Barium",56,"#FFA94D",6,2,"[Xe] 6s2","Barium is a chemical element with symbol Ba and atomic number 56. It is the fifth element in Group 2, a soft silvery metallic alkaline earth metal. Because of its high chemical reactivity barium is never found in nature as a free element.","https://upload.wikimedia.org/wikipedia/commons/f/f5/Barium_%2856_Ba%29.jpg","X-ray/CT contrast drink (\"barium meal\"), fireworks"),
            new Elem("La","Lanthanum",57,"#63E6BE",6,3,"[Xe] 5d16s2","Lanthanum is a soft, ductile, silvery-white metallic chemical element with symbol La and atomic number 57. It tarnishes rapidly when exposed to air and is soft enough to be cut with a knife.","https://upload.wikimedia.org/wikipedia/commons/f/f7/Lanthanum.jpg","Camera lenses, hybrid car batteries"),
            new Elem("Ce","Cerium",58,"#63E6BE",9,4,"[Xe] 4f1 5d1 6s2","Cerium is a chemical element with symbol Ce and atomic number 58. It is a soft, silvery, ductile metal which easily oxidizes in air. Cerium was named after the dwarf planet Ceres (itself named after the Roman goddess of agriculture).","https://upload.wikimedia.org/wikipedia/commons/0/0d/Cerium2.jpg",""),
            new Elem("Pr","Praseodymium",59,"#63E6BE",9,5,"[Xe] 4f3 6s2","Praseodymium is a chemical element with symbol Pr and atomic number 59. Praseodymium is a soft, silvery, malleable and ductile metal in the lanthanide group. It is valued for its magnetic, electrical, chemical, and optical properties.","https://upload.wikimedia.org/wikipedia/commons/c/c7/Praseodymium.jpg",""),
            new Elem("Nd","Neodymium",60,"#63E6BE",9,6,"[Xe] 4f4 6s2","Neodymium is a chemical element with symbol Nd and atomic number 60. It is a soft silvery metal that tarnishes in air. Neodymium was discovered in 1885 by the Austrian chemist Carl Auer von Welsbach.","https://upload.wikimedia.org/wikipedia/commons/c/c9/Neodymium_%2860_Nd%29.jpg",""),
            new Elem("Pm","Promethium",61,"#63E6BE",9,7,"[Xe] 4f5 6s2","Promethium, originally prometheum, is a chemical element with the symbol Pm and atomic number 61. All of its isotopes are radioactive; it is one of only two such elements that are followed in the periodic table by elements with stable forms, a distinction shared with technetium.","https://upload.wikimedia.org/wikipedia/commons/5/5b/Promethium.jpg",""),
            new Elem("Sm","Samarium",62,"#63E6BE",9,8,"[Xe] 4f6 6s2","Samarium is a chemical element with symbol Sm and atomic number 62. It is a moderately hard silvery metal that readily oxidizes in air. Being a typical member of the lanthanide series, samarium usually assumes the oxidation state +3.","https://upload.wikimedia.org/wikipedia/commons/8/88/Samarium-2.jpg",""),
            new Elem("Eu","Europium",63,"#63E6BE",9,9,"[Xe] 4f7 6s2","Europium is a chemical element with symbol Eu and atomic number 63. It was isolated in 1901 and is named after the continent of Europe. It is a moderately hard, silvery metal which readily oxidizes in air and water.","https://upload.wikimedia.org/wikipedia/commons/6/6a/Europium.jpg",""),
            new Elem("Gd","Gadolinium",64,"#63E6BE",9,10,"[Xe] 4f7 5d1 6s2","Gadolinium is a chemical element with symbol Gd and atomic number 64. It is a silvery-white, malleable and ductile rare-earth metal. It is found in nature only in combined (salt) form.","https://upload.wikimedia.org/wikipedia/commons/c/c2/Gadolinium-2.jpg",""),
            new Elem("Tb","Terbium",65,"#63E6BE",9,11,"[Xe] 4f9 6s2","Terbium is a chemical element with symbol Tb and atomic number 65. It is a silvery-white rare earth metal that is malleable, ductile and soft enough to be cut with a knife.","https://upload.wikimedia.org/wikipedia/commons/9/9a/Terbium-2.jpg",""),
            new Elem("Dy","Dysprosium",66,"#63E6BE",9,12,"[Xe] 4f10 6s2","Dysprosium is a chemical element with the symbol Dy and atomic number 66. It is a rare earth element with a metallic silver luster. Dysprosium is never found in nature as a free element, though it is found in various minerals, such as xenotime.","https://upload.wikimedia.org/wikipedia/commons/5/55/Dysprosium-2.jpg",""),
            new Elem("Ho","Holmium",67,"#63E6BE",9,13,"[Xe] 4f11 6s2","Holmium is a chemical element with symbol Ho and atomic number 67. Part of the lanthanide series, holmium is a rare earth element. Holmium was discovered by Swedish chemist Per Theodor Cleve.","https://upload.wikimedia.org/wikipedia/commons/0/0a/Holmium2.jpg",""),
            new Elem("Er","Erbium",68,"#63E6BE",9,14,"[Xe] 4f12 6s2","Erbium is a chemical element in the lanthanide series, with symbol Er and atomic number 68. A silvery-white solid metal when artificially isolated, natural erbium is always found in chemical combination with other elements on Earth.","https://upload.wikimedia.org/wikipedia/commons/2/2a/Erbium-2.jpg",""),
            new Elem("Tm","Thulium",69,"#63E6BE",9,15,"[Xe] 4f13 6s2","Thulium is a chemical element with symbol Tm and atomic number 69. It is the thirteenth and antepenultimate (third-last) element in the lanthanide series. Like the other lanthanides, the most common oxidation state is +3, seen in its oxide, halides and other compounds.","https://upload.wikimedia.org/wikipedia/commons/6/6b/Thulium-2.jpg",""),
            new Elem("Yb","Ytterbium",70,"#63E6BE",9,16,"[Xe] 4f14 6s2","Ytterbium is a chemical element with symbol Yb and atomic number 70. It is the fourteenth and penultimate element in the lanthanide series, which is the basis of the relative stability of its +2 oxidation state.","https://upload.wikimedia.org/wikipedia/commons/c/ce/Ytterbium-3.jpg",""),
            new Elem("Lu","Lutetium",71,"#63E6BE",9,17,"[Xe] 4f14 5d1 6s2","Lutetium is a chemical element with symbol Lu and atomic number 71. It is a silvery white metal, which resists corrosion in dry, but not in moist air.","https://upload.wikimedia.org/wikipedia/commons/e/e8/Lutetium.jpg",""),
            new Elem("Hf","Hafnium",72,"#4DABF7",6,4,"[Xe] 4f14 5d2 6s2","Hafnium is a chemical element with symbol Hf and atomic number 72. A lustrous, silvery gray, tetravalent transition metal, hafnium chemically resembles zirconium and is found in zirconium minerals.","https://upload.wikimedia.org/wikipedia/commons/1/17/Hafnium_%2872_Hf%29.jpg",""),
            new Elem("Ta","Tantalum",73,"#4DABF7",6,5,"[Xe] 4f14 5d3 6s2","Tantalum is a chemical element with symbol Ta and atomic number 73. Previously known as tantalium, its name comes from Tantalus, an antihero from Greek mythology. Tantalum is a rare, hard, blue-gray, lustrous transition metal that is highly corrosion-resistant.","https://upload.wikimedia.org/wikipedia/commons/6/61/Tantalum.jpg",""),
            new Elem("W","Tungsten",74,"#4DABF7",6,6,"[Xe] 4f14 5d4 6s2","Tungsten, also known as wolfram, is a chemical element with symbol W and atomic number 74. The word tungsten comes from the Swedish language tung sten, which directly translates to heavy stone.","https://upload.wikimedia.org/wikipedia/commons/c/c8/Tungsten_rod_with_oxidised_surface.jpg","Light bulb filaments, drill bits, jewelry"),
            new Elem("Re","Rhenium",75,"#4DABF7",6,7,"[Xe] 4f14 5d5 6s2","Rhenium is a chemical element with symbol Re and atomic number 75. It is a silvery-white, heavy, third-row transition metal in group 7 of the periodic table.","https://upload.wikimedia.org/wikipedia/commons/d/d9/Pure_rhenium_bead%2C_arc_melted%2C_21_grams._Original_size_in_cm_-_1.5_x_1.7.jpg",""),
            new Elem("Os","Osmium",76,"#4DABF7",6,8,"[Xe] 4f14 5d6 6s2","Osmium (from Greek osme (ὀσμή) meaning \\\"smell\\\") is a chemical element with symbol Os and atomic number 76. It is a hard, brittle, bluish-white transition metal in the platinum group that is found as a trace element in alloys, mostly in platinum ores.","https://upload.wikimedia.org/wikipedia/commons/3/3c/Osmium-bead.jpg",""),
            new Elem("Ir","Iridium",77,"#4DABF7",6,9,"[Xe] 4f14 5d7 6s2","Iridium is a chemical element with symbol Ir and atomic number 77. A very hard, brittle, silvery-white transition metal of the platinum group, iridium is generally credited with being the second densest element (after osmium) based on measured density, although calculations...","https://upload.wikimedia.org/wikipedia/commons/a/a8/Iridium-2.jpg",""),
            new Elem("Pt","Platinum",78,"#4DABF7",6,10,"[Xe] 4f14 5d9 6s1","Platinum is a chemical element with symbol Pt and atomic number 78. It is a dense, malleable, ductile, highly unreactive, precious, gray-white transition metal. Its name is derived from the Spanish term platina, which is literally translated into \\\"little silver\\\".","https://upload.wikimedia.org/wikipedia/commons/6/68/Platinum_crystals.jpg","Catalytic converters, jewelry, cancer drugs"),
            new Elem("Au","Gold",79,"#4DABF7",6,11,"[Xe] 4f14 5d10 6s1","Gold is a chemical element with symbol Au (from Latin:aurum) and atomic number 79. In its purest form, it is a bright, slightly reddish yellow, dense, soft, malleable and ductile metal. Chemically, gold is a transition metal and a group 11 element.","https://upload.wikimedia.org/wikipedia/commons/8/8a/Gold_%2879_Au%29.jpg","Jewelry, electronics, currency reserves"),
            new Elem("Hg","Mercury",80,"#4DABF7",6,12,"[Xe] 4f14 5d10 6s2","Mercury is a chemical element with symbol Hg and atomic number 80. It is commonly known as quicksilver and was formerly named hydrargyrum (/haɪˈdrɑːrdʒərəm/).","https://upload.wikimedia.org/wikipedia/commons/b/be/Hydrargyrum_%2880_Hg%29.jpg","Old thermometers, fluorescent lighting (largely phased out)"),
            new Elem("Tl","Thallium",81,"#9775FA",6,13,"[Xe] 4f14 5d10 6s2 6p1","Thallium is a chemical element with symbol Tl and atomic number 81. This soft gray post-transition metal is not found free in nature. When isolated, it resembles tin, but discolors when exposed to air.","https://upload.wikimedia.org/wikipedia/commons/5/55/Thallium_%2881_Tl%29.jpg","Historically rat poison (now restricted); electronics"),
            new Elem("Pb","Lead",82,"#9775FA",6,14,"[Xe] 4f14 5d10 6s2 6p2","Lead (/lɛd/) is a chemical element in the carbon group with symbol Pb (from Latin:plumbum) and atomic number 82. Lead is a soft, malleable and heavy post-transition metal.","https://upload.wikimedia.org/wikipedia/commons/6/63/Lead-2.jpg","Car batteries, radiation shielding, historically pipes/paint"),
            new Elem("Bi","Bismuth",83,"#9775FA",6,15,"[Xe] 4f14 5d10 6s2 6p3","Bismuth is a chemical element with symbol Bi and atomic number 83. Bismuth, a pentavalent post-transition metal, chemically resembles arsenic and antimony. Elemental bismuth may occur naturally, although its sulfide and oxide form important commercial ores.","https://upload.wikimedia.org/wikipedia/commons/a/a5/Bismuth-2.jpg","Stomach medicine (Pepto-Bismol), fire sprinklers, cosmetics"),
            new Elem("Po","Polonium",84,"#FFD43B",6,16,"[Xe] 4f14 5d10 6s2 6p4","Polonium is a chemical element with symbol Po and atomic number 84, discovered in 1898 by Marie Curie and Pierre Curie. A rare and highly radioactive element with no stable isotopes, polonium is chemically similar to bismuth and tellurium, and it occurs in uranium ores.","https://images-of-elements.com/polonium.jpg",""),
            new Elem("At","Astatine",85,"#FF922B",6,17,"[Xe] 4f14 5d10 6s2 6p5","Astatine is a very rare radioactive chemical element with the chemical symbol At and atomic number 85. It occurs on Earth as the decay product of various heavier elements. All its isotopes are short-lived; the most stable is astatine-210, with a half-life of 8.1 hours.","https://images-of-elements.com/astatine.jpg",""),
            new Elem("Rn","Radon",86,"#845EF7",6,18,"[Xe] 4f14 5d10 6s2 6p6","Radon is a chemical element with symbol Rn and atomic number 86. It is a radioactive, colorless, odorless, tasteless noble gas, occurring naturally as a decay product of radium. Its most stable isotope, 222Rn, has a half-life of 3.8 days.","https://images-of-elements.com/radon.jpg",""),
            new Elem("Fr","Francium",87,"#FF6B6B",7,1,"[Rn] 7s1","Francium is a chemical element with symbol Fr and atomic number 87. It used to be known as eka-caesium and actinium K. It is the second-least electronegative element, behind only caesium. Francium is a highly radioactive metal that decays into astatine, radium, and radon.","https://images-of-elements.com/francium.jpg",""),
            new Elem("Ra","Radium",88,"#FFA94D",7,2,"[Rn] 7s2","Radium is a chemical element with symbol Ra and atomic number 88. It is the sixth element in group 2 of the periodic table, also known as the alkaline earth metals.","https://upload.wikimedia.org/wikipedia/commons/b/bb/Radium226.jpg",""),
            new Elem("Ac","Actinium",89,"#38D9A9",7,3,"[Rn] 6d1 7s2","Actinium is a radioactive chemical element with symbol Ac (not to be confused with the abbreviation for an acetyl group) and atomic number 89, which was discovered in 1899. It was the first non-primordial radioactive element to be isolated.","https://upload.wikimedia.org/wikipedia/commons/2/27/Actinium_sample_%2831481701837%29.png",""),
            new Elem("Th","Thorium",90,"#38D9A9",10,4,"[Rn] 6d2 7s2","Thorium is a chemical element with symbol Th and atomic number 90. A radioactive actinide metal, thorium is one of only two significantly radioactive elements that still occur naturally in large quantities as a primordial element (the other being uranium).","https://upload.wikimedia.org/wikipedia/commons/f/f7/Thorium-1.jpg",""),
            new Elem("Pa","Protactinium",91,"#38D9A9",10,5,"[Rn] 5f2 6d1 7s2","Protactinium is a chemical element with symbol Pa and atomic number 91. It is a dense, silvery-gray metal which readily reacts with oxygen, water vapor and inorganic acids.","https://upload.wikimedia.org/wikipedia/commons/a/af/Protactinium-233.jpg",""),
            new Elem("U","Uranium",92,"#38D9A9",10,6,"[Rn] 5f3 6d1 7s2","Uranium is a chemical element with symbol U and atomic number 92. It is a silvery-white metal in the actinide series of the periodic table. A uranium atom has 92 protons and 92 electrons, of which 6 are valence electrons.","https://upload.wikimedia.org/wikipedia/commons/b/b2/Ames_Process_uranium_biscuit.jpg","Nuclear power/fuel, historically glass coloring"),
            new Elem("Np","Neptunium",93,"#38D9A9",10,7,"[Rn] 5f4 6d1 7s2","Neptunium is a chemical element with symbol Np and atomic number 93. A radioactive actinide metal, neptunium is the first transuranic element.","https://upload.wikimedia.org/wikipedia/commons/e/e5/Neptunium2.jpg",""),
            new Elem("Pu","Plutonium",94,"#38D9A9",10,8,"[Rn] 5f6 7s2","Plutonium is a transuranic radioactive chemical element with symbol Pu and atomic number 94. It is an actinide metal of silvery-gray appearance that tarnishes when exposed to air, and forms a dull coating when oxidized.","https://upload.wikimedia.org/wikipedia/commons/0/0f/Plutonium_ring.jpg","Nuclear weapons/reactors, spacecraft power sources"),
            new Elem("Am","Americium",95,"#38D9A9",10,9,"[Rn] 5f7 7s2","Americium is a radioactive transuranic chemical element with symbol Am and atomic number 95. This member of the actinide series is located in the periodic table under the lanthanide element europium, and thus by analogy was named after the Americas.","https://upload.wikimedia.org/wikipedia/commons/e/ee/Americium_microscope.jpg",""),
            new Elem("Cm","Curium",96,"#38D9A9",10,10,"[Rn] 5f7 6d1 7s2","Curium is a transuranic radioactive chemical element with symbol Cm and atomic number 96. This element of the actinide series was named after Marie and Pierre Curie – both were known for their research on radioactivity.","https://images-of-elements.com/s/curium-glow.jpg",""),
            new Elem("Bk","Berkelium",97,"#38D9A9",10,11,"[Rn] 5f9 7s2","Berkelium is a transuranic radioactive chemical element with symbol Bk and atomic number 97. It is a member of the actinide and transuranium element series.","https://upload.wikimedia.org/wikipedia/commons/f/fc/Berkelium.jpg",""),
            new Elem("Cf","Californium",98,"#38D9A9",10,12,"[Rn] 5f10 7s2","Californium is a radioactive metallic chemical element with symbol Cf and atomic number 98. The element was first made in 1950 at the University of California Radiation Laboratory in Berkeley, by bombarding curium with alpha particles (helium-4 ions).","https://upload.wikimedia.org/wikipedia/commons/9/93/Californium.jpg",""),
            new Elem("Es","Einsteinium",99,"#38D9A9",10,13,"[Rn] 5f11 7s2","Einsteinium is a synthetic element with symbol Es and atomic number 99. It is the seventh transuranic element, and an actinide. Einsteinium was discovered as a component of the debris of the first hydrogen bomb explosion in 1952, and named after Albert Einstein.","https://upload.wikimedia.org/wikipedia/commons/5/55/Einsteinium.jpg",""),
            new Elem("Fm","Fermium",100,"#38D9A9",10,14,"[Rn] 5f12 7s2","Fermium is a synthetic element with symbol Fm and atomic number 100. It is a member of the actinide series.","https://upload.wikimedia.org/wikipedia/commons/5/58/Ivy_Mike_-_mushroom_cloud.jpg",""),
            new Elem("Md","Mendelevium",101,"#38D9A9",10,15,"[Rn] 5f13 7s2","Mendelevium is a synthetic element with chemical symbol Md (formerly Mv) and atomic number 101.","https://images-of-elements.com/s/mendelevium.jpg",""),
            new Elem("No","Nobelium",102,"#38D9A9",10,16,"[Rn] 5f14 7s2","Nobelium is a synthetic chemical element with symbol No and atomic number 102. It is named in honor of Alfred Nobel, the inventor of dynamite and benefactor of science. A radioactive metal, it is the tenth transuranic element and is the penultimate member of the actinide series.","https://images-of-elements.com/nobelium.jpg",""),
            new Elem("Lr","Lawrencium",103,"#38D9A9",10,17,"[Rn] 5f14 7s2 7p1","Lawrencium is a synthetic chemical element with chemical symbol Lr (formerly Lw) and atomic number 103. It is named in honor of Ernest Lawrence, inventor of the cyclotron, a device that was used to discover many artificial radioactive elements.","https://images-of-elements.com/lawrencium.jpg",""),
            new Elem("Rf","Rutherfordium",104,"#4DABF7",7,4,"[Rn] 5f14 6d2 7s2","Rutherfordium is a chemical element with symbol Rf and atomic number 104, named in honor of physicist Ernest Rutherford.","https://images-of-elements.com/s/rutherfordium.jpg",""),
            new Elem("Db","Dubnium",105,"#4DABF7",7,5,"*[Rn] 5f14 6d3 7s2","Dubnium is a chemical element with symbol Db and atomic number 105. It is named after the town of Dubna in Russia (north of Moscow), where it was first produced.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Sg","Seaborgium",106,"#4DABF7",7,6,"*[Rn] 5f14 6d4 7s2","Seaborgium is a synthetic element with symbol Sg and atomic number 106. Its most stable known isotope, 271Sg, has a half-life of about 1.9 minutes.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Bh","Bohrium",107,"#4DABF7",7,7,"*[Rn] 5f14 6d5 7s2","Bohrium is a chemical element with symbol Bh and atomic number 107. It is named after Danish physicist Niels Bohr.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Hs","Hassium",108,"#4DABF7",7,8,"*[Rn] 5f14 6d6 7s2","Hassium is a chemical element with symbol Hs and atomic number 108, named after the German state of Hesse.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Mt","Meitnerium",109,"#4DABF7",7,9,"*[Rn] 5f14 6d7 7s2","Meitnerium is a chemical element with symbol Mt and atomic number 109. It is an extremely radioactive synthetic element (an element not found in nature that can be created in a laboratory). The most stable known isotope, meitnerium-278, has a half-life of 7.6 seconds.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Ds","Darmstadtium",110,"#4DABF7",7,10,"*[Rn] 5f14 6d9 7s1","Darmstadtium is a chemical element with symbol Ds and atomic number 110. It is an extremely radioactive synthetic element. The most stable known isotope, darmstadtium-281, has a half-life of approximately 10 seconds.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Rg","Roentgenium",111,"#4DABF7",7,11,"*[Rn] 5f14 6d10 7s1","Roentgenium is a chemical element with symbol Rg and atomic number 111. It is an extremely radioactive synthetic element (an element that can be created in a laboratory but is not found in nature); the most stable known isotope, roentgenium-282, has a half-life of 2.1 minutes.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Cn","Copernicium",112,"#4DABF7",7,12,"*[Rn] 5f14 6d10 7s2","Copernicium is a chemical element with symbol Cn and atomic number 112. It is an extremely radioactive synthetic element that can only be created in a laboratory.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Nh","Nihonium",113,"#9775FA",7,13,"*[Rn] 5f14 6d10 7s2 7p1","Nihonium is a chemical element with atomic number 113. It has a symbol Nh. It is a synthetic element (an element that can be created in a laboratory but is not found in nature) and is extremely radioactive; its most stable known isotope, nihonium-286, has a half-life of 20...","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Fl","Flerovium",114,"#9775FA",7,14,"*[Rn] 5f14 6d10 7s2 7p2","Flerovium is a superheavy artificial chemical element with symbol Fl and atomic number 114. It is an extremely radioactive synthetic element.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Mc","Moscovium",115,"#9775FA",7,15,"*[Rn] 5f14 6d10 7s2 7p3","Moscovium is the name of a synthetic superheavy element in the periodic table that has the symbol Mc and has the atomic number 115. It is an extremely radioactive element; its most stable known isotope, moscovium-289, has a half-life of only 220 milliseconds.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Lv","Livermorium",116,"#9775FA",7,16,"*[Rn] 5f14 6d10 7s2 7p4","Livermorium is a synthetic superheavy element with symbol Lv and atomic number 116. It is an extremely radioactive element that has only been created in the laboratory and has not been observed in nature.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Ts","Tennessine",117,"#FF922B",7,17,"*[Rn] 5f14 6d10 7s2 7p5","Tennessine is a superheavy artificial chemical element with an atomic number of 117 and a symbol of Ts. Also known as eka-astatine or element 117, it is the second-heaviest known element and penultimate element of the 7th period of the periodic table.","https://images-of-elements.com/s/transactinoid.png",""),
            new Elem("Og","Oganesson",118,"#845EF7",7,18,"*[Rn] 5f14 6d10 7s2 7p6","Oganesson is IUPAC's name for the transactinide element with the atomic number 118 and element symbol Og. It is also known as eka-radon or element 118, and on the periodic table of the elements it is a p-block element and the last one of the 7th period.","https://images-of-elements.com/s/transactinoid.png","")
    };

    private static final Compound[] COMPOUNDS = {
            new Compound("Na","Cl","NaCl","Sodium Chloride","Table Salt","The salt on your food — a violently reactive metal and a toxic gas combine into something you eat every day.","Ionic crystal lattice","Ionic"),
            new Compound("H","O","H2O","Water","Water","Two flammable/reactive elements combine into the one thing that puts fires out.","Bent molecular structure","Covalent (polar)"),
            new Compound("H","Cl","HCl","Hydrogen Chloride","Stomach Acid (when dissolved)","Your stomach makes this to digest food — concentrated, it dissolves metal.","Linear diatomic molecule","Covalent (polar)"),
            new Compound("C","O","CO2","Carbon Dioxide","CO2 (what you exhale)","What plants breathe in and you breathe out.","Linear molecular structure","Covalent"),
            new Compound("Na","O","Na2O","Sodium Oxide","Sodium Oxide","Reacts violently with water to form lye (sodium hydroxide).","Ionic crystal lattice","Ionic"),
            new Compound("Fe","O","Fe2O3","Iron(III) Oxide","Rust","What happens to iron left out in the rain.","Ionic crystal lattice","Ionic"),
            new Compound("Al","O","Al2O3","Aluminum Oxide","Sapphire & Ruby (in gem form)","Pure aluminum oxide crystals, colored by trace metals, are sapphires and rubies.","Ionic crystal lattice (corundum)","Ionic"),
            new Compound("Mg","O","MgO","Magnesium Oxide","Milk of Magnesia (as hydroxide)","Burns with a blinding white light — used in old photography flashes and fireworks.","Ionic crystal lattice","Ionic"),
            new Compound("Ca","O","CaO","Calcium Oxide","Quicklime","Reacts with water in a strongly exothermic reaction — used in cement.","Ionic crystal lattice","Ionic"),
            new Compound("Si","O","SiO2","Silicon Dioxide","Quartz / Sand","The main component of beach sand and the glass in your window.","Covalent network lattice (quartz)","Covalent"),
            new Compound("N","O","NO2","Nitrogen Dioxide","Smog Gas","The reddish-brown gas responsible for a lot of urban air pollution.","Bent molecular structure","Covalent"),
            new Compound("N","H","NH3","Ammonia","Ammonia","That sharp smell in cleaning products — also essential for making fertilizer.","Trigonal pyramidal molecule","Covalent (polar)"),
            new Compound("C","H","CH4","Methane","Natural Gas","The main component of the natural gas that heats homes and stoves.","Tetrahedral molecule","Covalent"),
            new Compound("S","O","SO2","Sulfur Dioxide","Volcano/Match Smell","That burnt-match smell — also a major cause of acid rain.","Bent molecular structure","Covalent"),
            new Compound("H","S","H2S","Hydrogen Sulfide","Rotten Egg Gas","Responsible for the smell of rotten eggs and some hot springs.","Bent molecular structure","Covalent (polar)"),
            new Compound("K","Cl","KCl","Potassium Chloride","Salt Substitute","Used in low-sodium salt substitutes — and, at high doses, lethal injection.","Ionic crystal lattice","Ionic"),
            new Compound("K","I","KI","Potassium Iodide","Iodized Salt Additive","Added to table salt to prevent iodine-deficiency disorders.","Ionic crystal lattice","Ionic"),
            new Compound("Ca","Cl","CaCl2","Calcium Chloride","Road Salt / Ice Melt","Used to melt ice on roads — releases heat as it dissolves.","Ionic crystal lattice","Ionic"),
            new Compound("Mg","Cl","MgCl2","Magnesium Chloride","De-icer / Bath Salts","Also sold as \"magnesium flakes\" for relaxing baths.","Ionic crystal lattice","Ionic"),
            new Compound("Ag","Cl","AgCl","Silver Chloride","Photographic Film Compound","Darkens when exposed to light — the basis of old photographic film.","Ionic crystal lattice","Ionic"),
            new Compound("Cu","O","CuO","Copper(II) Oxide","Black Copper Oxide","The black coating that forms on old copper pipes and pennies.","Ionic crystal lattice","Ionic"),
            new Compound("Zn","O","ZnO","Zinc Oxide","Sunscreen / Diaper Cream","The white paste in mineral sunscreen and baby diaper cream.","Ionic crystal lattice (wurtzite)","Ionic"),
            new Compound("Ti","O","TiO2","Titanium Dioxide","White Pigment","Makes white paint white, and toothpaste opaque.","Ionic crystal lattice (rutile)","Ionic"),
            new Compound("H","F","HF","Hydrogen Fluoride","Glass-Etching Acid","One of the few acids that can dissolve glass itself.","Linear diatomic molecule","Covalent (polar)"),
            new Compound("H","Br","HBr","Hydrogen Bromide","Hydrobromic Acid","A strong acid used in organic chemistry synthesis.","Linear diatomic molecule","Covalent (polar)"),
            new Compound("H","I","HI","Hydrogen Iodide","Hydroiodic Acid","One of the strongest common acids, stronger than HCl.","Linear diatomic molecule","Covalent (polar)"),
            new Compound("N","N","N2","Nitrogen Gas","The Air You Breathe (mostly)","About 78% of the air around you right now.","Linear diatomic molecule (triple bond)","Covalent"),
            new Compound("O","O","O2","Oxygen Gas","The Oxygen You Breathe","About 21% of the air, and the part your body actually needs.","Linear diatomic molecule (double bond)","Covalent"),
            new Compound("H","H","H2","Hydrogen Gas","Hydrogen Fuel","The most abundant element in the universe, and a rocket fuel component.","Diatomic molecule (single bond)","Covalent"),
            new Compound("C","C","Diamond/Graphite","Carbon Allotropes","Diamond or Pencil Lead","Pure carbon can be the hardest natural material or one of the softest, depending on structure.","Covalent network (diamond) or layered sheets (graphite)","Covalent"),
            new Compound("Na","H","NaH","Sodium Hydride","Sodium Hydride","Reacts violently and explosively with water.","Ionic crystal lattice","Ionic"),
            new Compound("Ca","C","CaC2","Calcium Carbide","Carbide (old lamp fuel)","Reacts with water to make acetylene gas — used in old miners' lamps.","Ionic crystal lattice","Ionic (with covalent C≡C unit)"),
            new Compound("Fe","S","FeS","Iron(II) Sulfide","Fool's Gold (related)","A classic chemistry-class demo — iron filings and sulfur fused together.","Ionic/metallic lattice","Ionic"),
            new Compound("Cu","S","CuS","Copper(II) Sulfide","Covellite Mineral","A naturally occurring mineral, often iridescent blue.","Ionic crystal lattice","Ionic"),
            new Compound("Pb","O","PbO2","Lead Dioxide","Car Battery Component","Used in the positive plate of lead-acid car batteries.","Ionic crystal lattice","Ionic"),
            new Compound("Sn","O","SnO2","Tin Dioxide","Ceramic Glaze Ingredient","Used to make ceramic glazes opaque white.","Ionic crystal lattice (rutile)","Ionic"),
            new Compound("Br","Br","Br2","Bromine","Liquid Bromine","One of only two elements that are liquid at room temperature.","Diatomic molecule","Covalent"),
            new Compound("Cl","Cl","Cl2","Chlorine Gas","Pool Chlorine Gas","Used to disinfect swimming pools and drinking water.","Diatomic molecule","Covalent"),
            new Compound("P","O","P2O5","Phosphorus Pentoxide","Drying Agent","So good at absorbing water it's used as a powerful desiccant.","Molecular cage structure","Covalent"),
            new Compound("Al","Cl","AlCl3","Aluminum Chloride","Antiperspirant Ingredient","The active ingredient in many antiperspirant deodorants.","Layered/dimeric molecular structure","Covalent (polar, borderline ionic)"),
            new Compound("Zn","Cl","ZnCl2","Zinc Chloride","Soldering Flux","Used as a flux in soldering to clean metal surfaces.","Ionic crystal lattice","Ionic"),
            new Compound("Ba","O","BaO","Barium Oxide","Glass Additive","Added to glass to increase its refractive index.","Ionic crystal lattice","Ionic"),
            new Compound("Li","O","Li2O","Lithium Oxide","Ceramic Glass Additive","Used in some heat-resistant ceramic glass and lithium-ion battery research.","Ionic crystal lattice","Ionic"),
            new Compound("Au","Cl","AuCl3","Gold(III) Chloride","Gold Plating Compound","Used in gold plating and as a photosensitizer.","Dimeric molecular structure","Covalent (polar)")
    };

    private static Compound findCompound(String a, String b) {
        for (Compound c : COMPOUNDS) if (c.matches(a, b)) return c;
        return null;
    }

    // ── Draggable + tappable element tile ────────────────────────────────────
    private class ElemView extends FrameLayout {
        final Elem elem;
        float downRawX, downRawY, startX, startY, totalMove;

        ElemView(Context ctx, Elem e) {
            super(ctx);
            elem = e;
            setBackgroundColor(Color.parseColor(e.color));

            TextView tv = new TextView(ctx);
            tv.setText(e.symbol);
            tv.setTextColor(Color.BLACK);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setTextSize(13f);
            tv.setGravity(Gravity.CENTER);
            addView(tv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            setOnTouchListener((v, event) -> handleTouch(this, event));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildLayout());
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0d0d0d);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0xFF1a1a1a);
        topBar.setPadding(dp(16), dp(14), dp(16), dp(14));
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("\u269B PERIODIC TABLE — tap for info, drag to combine");
        title.setTextColor(0xFFc9a84c);
        title.setTextSize(12f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);
        topBar.addView(title);

        TextView btnClose = new TextView(this);
        btnClose.setText("\u2715");
        btnClose.setTextColor(0xFFc9a84c);
        btnClose.setTextSize(20f);
        btnClose.setPadding(dp(20), 0, dp(4), 0);
        btnClose.setOnClickListener(v -> finish());
        topBar.addView(btnClose);
        root.addView(topBar);

        // Result panel (hidden until a match happens)
        resultPanel = new LinearLayout(this);
        resultPanel.setOrientation(LinearLayout.VERTICAL);
        resultPanel.setBackgroundColor(0xFF05201a);
        resultPanel.setPadding(dp(16), dp(12), dp(16), dp(12));
        resultPanel.setVisibility(View.GONE);
        resultText = new TextView(this);
        resultText.setTextColor(0xFF7CFFB2);
        resultText.setTextSize(13f);
        resultPanel.addView(resultText);
        root.addView(resultPanel);

        // Scrollable periodic table area
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        ScrollView vScroll = new ScrollView(this);
        hScroll.addView(vScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(hScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        tableLayer = new FrameLayout(this);
        int tableW = dp(CELL * 19);
        int tableH = dp(CELL * 11);
        tableLayer.setLayoutParams(new ViewGroup.LayoutParams(tableW, tableH));
        vScroll.addView(tableLayer);

        for (Elem e : ELEMENTS) {
            ElemView ev = new ElemView(this, e);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(CELL - 2), dp(CELL - 2));
            lp.leftMargin = dp((e.col - 1) * CELL);
            lp.topMargin  = dp((e.row - 1) * CELL) + (e.row >= 9 ? dp(CELL / 2) : 0);
            ev.setLayoutParams(lp);
            ev.setX(lp.leftMargin);
            ev.setY(lp.topMargin);
            tableLayer.addView(ev);
            elemViews.add(ev);
        }

        return root;
    }

    // ── Touch: distinguishes a tap (show info) from a drag (combine) ────────
    private boolean handleTouch(ElemView view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                view.downRawX = event.getRawX();
                view.downRawY = event.getRawY();
                view.startX = view.getX();
                view.startY = view.getY();
                view.totalMove = 0f;
                view.bringToFront();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - view.downRawX;
                float dy = event.getRawY() - view.downRawY;
                view.totalMove = (float) Math.hypot(dx, dy);
                view.setX(view.startX + dx);
                view.setY(view.startY + dy);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (view.totalMove < dp(TAP_THRESHOLD_DP)) {
                    // Treated as a tap, not a drag — snap back instantly and show info.
                    view.setX(view.startX);
                    view.setY(view.startY);
                    showElementDetail(view.elem);
                    return true;
                }
                ElemView target = findOverlap(view);
                if (target != null) onElementsCombined(view.elem, target.elem);
                view.animate().x(view.startX).y(view.startY).setDuration(200).start();
                return true;
        }
        return false;
    }

    private ElemView findOverlap(ElemView moved) {
        float mx = moved.getX() + moved.getWidth() / 2f;
        float my = moved.getY() + moved.getHeight() / 2f;
        ElemView best = null;
        float bestDist = Float.MAX_VALUE;
        for (ElemView other : elemViews) {
            if (other == moved) continue;
            float ox = other.getX() + other.getWidth() / 2f;
            float oy = other.getY() + other.getHeight() / 2f;
            float dist = (float) Math.hypot(mx - ox, my - oy);
            if (dist < dp(CELL) * 0.7f && dist < bestDist) { best = other; bestDist = dist; }
        }
        return best;
    }

    private void onElementsCombined(Elem a, Elem b) {
        Compound c = findCompound(a.symbol, b.symbol);
        resultPanel.setVisibility(View.VISIBLE);
        if (c != null) {
            resultText.setText(a.symbol + " + " + b.symbol + " \u2192 " + c.formula + "  (" + c.name + ")\n"
                + "Structure: " + c.structure + "   \u00B7   Bond: " + c.bond + "\n"
                + "\uD83D\uDCA1 " + c.common + " — " + c.fact);
        } else {
            resultText.setText(a.symbol + " + " + b.symbol + " \u2192 no well-known everyday compound for this pair, sir — try another combination.");
        }
    }

    // ── Tap: full element detail dialog with real photo ─────────────────────
    private void showElementDetail(Elem e) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(16), dp(20), dp(8));
        container.setBackgroundColor(0xFF0d0d0d);

        ImageView img = new ImageView(this);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(160));
        imgLp.bottomMargin = dp(12);
        img.setLayoutParams(imgLp);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(0xFF1a1a1a);
        container.addView(img);

        ProgressBar spinner = new ProgressBar(this);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(dp(32), dp(32));
        spinLp.gravity = Gravity.CENTER;
        spinner.setLayoutParams(spinLp);
        container.addView(spinner);
        loadImageAsync(e.image, img, spinner);

        TextView name = new TextView(this);
        name.setText(e.name + "  (" + e.symbol + ")");
        name.setTextColor(0xFFc9a84c);
        name.setTextSize(18f);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        container.addView(name);

        addDetailRow(container, "Atomic Number", String.valueOf(e.number));
        addDetailRow(container, "Electron Configuration", e.econfig);
        addDetailRow(container, "Definition", e.summary);
        addDetailRow(container, "Common Uses", e.uses.isEmpty() ? "No widely known everyday use, sir — mostly research/lab contexts." : e.uses);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(container);

        new AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show();
    }

    private void addDetailRow(LinearLayout parent, String label, String value) {
        TextView lbl = new TextView(this);
        lbl.setText(label.toUpperCase());
        lbl.setTextColor(0xFF3a7aa0);
        lbl.setTextSize(11f);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setPadding(0, dp(10), 0, dp(2));
        parent.addView(lbl);

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(0xFFc8e8f8);
        val.setTextSize(14f);
        parent.addView(val);
    }

    // Simple dependency-free async image loader (no Glide/Picasso needed).
    private void loadImageAsync(String url, ImageView target, ProgressBar spinner) {
        if (url == null || url.isEmpty()) { spinner.setVisibility(View.GONE); return; }
        new Thread(() -> {
            Bitmap bmp = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "HENRY-Android");
                bmp = BitmapFactory.decodeStream(conn.getInputStream());
            } catch (Exception ignored) {}
            final Bitmap result = bmp;
            mainHandler.post(() -> {
                spinner.setVisibility(View.GONE);
                if (result != null) target.setImageBitmap(result);
                else target.setBackgroundColor(0xFF2a2a2a);
            });
        }).start();
    }
}
