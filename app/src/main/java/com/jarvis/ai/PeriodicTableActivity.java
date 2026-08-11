package com.jarvis.ai;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Periodic Table — drag one element onto another to see what real compound
 * they form, with the actual formula and a fact about it. All 118 elements,
 * authentic periodic table layout, curated database of 44 real, factually
 * checked combinations. Unlisted combinations get an honest "no known
 * reaction" rather than a made-up one.
 *
 * Voice/text trigger: "periodic table" / "element mixer" / "chemistry lab"
 */
public class PeriodicTableActivity extends AppCompatActivity {

    private static final int CELL = 44; // dp per element tile
    private FrameLayout tableLayer;
    private LinearLayout resultPanel;
    private TextView resultText;
    private final List<ElemView> elemViews = new ArrayList<>();

    // ── Data models ──────────────────────────────────────────────────────────
    private static class Elem {
        String symbol, name, color; int number, row, col;
        Elem(String s, String n, int num, String c, int r, int cl) {
            symbol=s; name=n; number=num; color=c; row=r; col=cl;
        }
    }
    private static class Compound {
        String a, b, formula, name, common, fact;
        Compound(String a, String b, String f, String n, String c, String fact) {
            this.a=a; this.b=b; formula=f; name=n; common=c; this.fact=fact;
        }
        boolean matches(String x, String y) {
            return (a.equals(x) && b.equals(y)) || (a.equals(y) && b.equals(x));
        }
    }

    private static final Elem[] ELEMENTS = {
            new Elem("H","Hydrogen",1,"#69DB7C",1,1),
            new Elem("He","Helium",2,"#845EF7",1,18),
            new Elem("Li","Lithium",3,"#FF6B6B",2,1),
            new Elem("Be","Beryllium",4,"#FFA94D",2,2),
            new Elem("B","Boron",5,"#FFD43B",2,13),
            new Elem("C","Carbon",6,"#69DB7C",2,14),
            new Elem("N","Nitrogen",7,"#69DB7C",2,15),
            new Elem("O","Oxygen",8,"#69DB7C",2,16),
            new Elem("F","Fluorine",9,"#FF922B",2,17),
            new Elem("Ne","Neon",10,"#845EF7",2,18),
            new Elem("Na","Sodium",11,"#FF6B6B",3,1),
            new Elem("Mg","Magnesium",12,"#FFA94D",3,2),
            new Elem("Al","Aluminum",13,"#9775FA",3,13),
            new Elem("Si","Silicon",14,"#FFD43B",3,14),
            new Elem("P","Phosphorus",15,"#69DB7C",3,15),
            new Elem("S","Sulfur",16,"#69DB7C",3,16),
            new Elem("Cl","Chlorine",17,"#FF922B",3,17),
            new Elem("Ar","Argon",18,"#845EF7",3,18),
            new Elem("K","Potassium",19,"#FF6B6B",4,1),
            new Elem("Ca","Calcium",20,"#FFA94D",4,2),
            new Elem("Sc","Scandium",21,"#4DABF7",4,3),
            new Elem("Ti","Titanium",22,"#4DABF7",4,4),
            new Elem("V","Vanadium",23,"#4DABF7",4,5),
            new Elem("Cr","Chromium",24,"#4DABF7",4,6),
            new Elem("Mn","Manganese",25,"#4DABF7",4,7),
            new Elem("Fe","Iron",26,"#4DABF7",4,8),
            new Elem("Co","Cobalt",27,"#4DABF7",4,9),
            new Elem("Ni","Nickel",28,"#4DABF7",4,10),
            new Elem("Cu","Copper",29,"#4DABF7",4,11),
            new Elem("Zn","Zinc",30,"#4DABF7",4,12),
            new Elem("Ga","Gallium",31,"#9775FA",4,13),
            new Elem("Ge","Germanium",32,"#FFD43B",4,14),
            new Elem("As","Arsenic",33,"#FFD43B",4,15),
            new Elem("Se","Selenium",34,"#69DB7C",4,16),
            new Elem("Br","Bromine",35,"#FF922B",4,17),
            new Elem("Kr","Krypton",36,"#845EF7",4,18),
            new Elem("Rb","Rubidium",37,"#FF6B6B",5,1),
            new Elem("Sr","Strontium",38,"#FFA94D",5,2),
            new Elem("Y","Yttrium",39,"#4DABF7",5,3),
            new Elem("Zr","Zirconium",40,"#4DABF7",5,4),
            new Elem("Nb","Niobium",41,"#4DABF7",5,5),
            new Elem("Mo","Molybdenum",42,"#4DABF7",5,6),
            new Elem("Tc","Technetium",43,"#4DABF7",5,7),
            new Elem("Ru","Ruthenium",44,"#4DABF7",5,8),
            new Elem("Rh","Rhodium",45,"#4DABF7",5,9),
            new Elem("Pd","Palladium",46,"#4DABF7",5,10),
            new Elem("Ag","Silver",47,"#4DABF7",5,11),
            new Elem("Cd","Cadmium",48,"#4DABF7",5,12),
            new Elem("In","Indium",49,"#9775FA",5,13),
            new Elem("Sn","Tin",50,"#9775FA",5,14),
            new Elem("Sb","Antimony",51,"#FFD43B",5,15),
            new Elem("Te","Tellurium",52,"#FFD43B",5,16),
            new Elem("I","Iodine",53,"#FF922B",5,17),
            new Elem("Xe","Xenon",54,"#845EF7",5,18),
            new Elem("Cs","Cesium",55,"#FF6B6B",6,1),
            new Elem("Ba","Barium",56,"#FFA94D",6,2),
            new Elem("La","Lanthanum",57,"#63E6BE",6,3),
            new Elem("Ce","Cerium",58,"#63E6BE",9,4),
            new Elem("Pr","Praseodymium",59,"#63E6BE",9,5),
            new Elem("Nd","Neodymium",60,"#63E6BE",9,6),
            new Elem("Pm","Promethium",61,"#63E6BE",9,7),
            new Elem("Sm","Samarium",62,"#63E6BE",9,8),
            new Elem("Eu","Europium",63,"#63E6BE",9,9),
            new Elem("Gd","Gadolinium",64,"#63E6BE",9,10),
            new Elem("Tb","Terbium",65,"#63E6BE",9,11),
            new Elem("Dy","Dysprosium",66,"#63E6BE",9,12),
            new Elem("Ho","Holmium",67,"#63E6BE",9,13),
            new Elem("Er","Erbium",68,"#63E6BE",9,14),
            new Elem("Tm","Thulium",69,"#63E6BE",9,15),
            new Elem("Yb","Ytterbium",70,"#63E6BE",9,16),
            new Elem("Lu","Lutetium",71,"#63E6BE",9,17),
            new Elem("Hf","Hafnium",72,"#4DABF7",6,4),
            new Elem("Ta","Tantalum",73,"#4DABF7",6,5),
            new Elem("W","Tungsten",74,"#4DABF7",6,6),
            new Elem("Re","Rhenium",75,"#4DABF7",6,7),
            new Elem("Os","Osmium",76,"#4DABF7",6,8),
            new Elem("Ir","Iridium",77,"#4DABF7",6,9),
            new Elem("Pt","Platinum",78,"#4DABF7",6,10),
            new Elem("Au","Gold",79,"#4DABF7",6,11),
            new Elem("Hg","Mercury",80,"#4DABF7",6,12),
            new Elem("Tl","Thallium",81,"#9775FA",6,13),
            new Elem("Pb","Lead",82,"#9775FA",6,14),
            new Elem("Bi","Bismuth",83,"#9775FA",6,15),
            new Elem("Po","Polonium",84,"#FFD43B",6,16),
            new Elem("At","Astatine",85,"#FF922B",6,17),
            new Elem("Rn","Radon",86,"#845EF7",6,18),
            new Elem("Fr","Francium",87,"#FF6B6B",7,1),
            new Elem("Ra","Radium",88,"#FFA94D",7,2),
            new Elem("Ac","Actinium",89,"#38D9A9",7,3),
            new Elem("Th","Thorium",90,"#38D9A9",10,4),
            new Elem("Pa","Protactinium",91,"#38D9A9",10,5),
            new Elem("U","Uranium",92,"#38D9A9",10,6),
            new Elem("Np","Neptunium",93,"#38D9A9",10,7),
            new Elem("Pu","Plutonium",94,"#38D9A9",10,8),
            new Elem("Am","Americium",95,"#38D9A9",10,9),
            new Elem("Cm","Curium",96,"#38D9A9",10,10),
            new Elem("Bk","Berkelium",97,"#38D9A9",10,11),
            new Elem("Cf","Californium",98,"#38D9A9",10,12),
            new Elem("Es","Einsteinium",99,"#38D9A9",10,13),
            new Elem("Fm","Fermium",100,"#38D9A9",10,14),
            new Elem("Md","Mendelevium",101,"#38D9A9",10,15),
            new Elem("No","Nobelium",102,"#38D9A9",10,16),
            new Elem("Lr","Lawrencium",103,"#38D9A9",10,17),
            new Elem("Rf","Rutherfordium",104,"#4DABF7",7,4),
            new Elem("Db","Dubnium",105,"#4DABF7",7,5),
            new Elem("Sg","Seaborgium",106,"#4DABF7",7,6),
            new Elem("Bh","Bohrium",107,"#4DABF7",7,7),
            new Elem("Hs","Hassium",108,"#4DABF7",7,8),
            new Elem("Mt","Meitnerium",109,"#4DABF7",7,9),
            new Elem("Ds","Darmstadtium",110,"#4DABF7",7,10),
            new Elem("Rg","Roentgenium",111,"#4DABF7",7,11),
            new Elem("Cn","Copernicium",112,"#4DABF7",7,12),
            new Elem("Nh","Nihonium",113,"#9775FA",7,13),
            new Elem("Fl","Flerovium",114,"#9775FA",7,14),
            new Elem("Mc","Moscovium",115,"#9775FA",7,15),
            new Elem("Lv","Livermorium",116,"#9775FA",7,16),
            new Elem("Ts","Tennessine",117,"#FF922B",7,17),
            new Elem("Og","Oganesson",118,"#845EF7",7,18)
    };

    private static final Compound[] COMPOUNDS = {
            new Compound("Na","Cl","NaCl","Sodium Chloride","Table Salt","The salt on your food — a violently reactive metal and a toxic gas combine into something you eat every day."),
            new Compound("H","O","H2O","Water","Water","Two flammable/reactive elements combine into the one thing that puts fires out."),
            new Compound("H","Cl","HCl","Hydrogen Chloride","Stomach Acid (when dissolved)","Your stomach makes this to digest food — concentrated, it dissolves metal."),
            new Compound("C","O","CO2","Carbon Dioxide","CO2 (what you exhale)","What plants breathe in and you breathe out."),
            new Compound("Na","O","Na2O","Sodium Oxide","Sodium Oxide","Reacts violently with water to form lye (sodium hydroxide)."),
            new Compound("Fe","O","Fe2O3","Iron(III) Oxide","Rust","What happens to iron left out in the rain."),
            new Compound("Al","O","Al2O3","Aluminum Oxide","Sapphire & Ruby (in gem form)","Pure aluminum oxide crystals, colored by trace metals, are sapphires and rubies."),
            new Compound("Mg","O","MgO","Magnesium Oxide","Milk of Magnesia (as hydroxide)","Burns with a blinding white light — used in old photography flashes and fireworks."),
            new Compound("Ca","O","CaO","Calcium Oxide","Quicklime","Reacts with water in a strongly exothermic reaction — used in cement."),
            new Compound("Si","O","SiO2","Silicon Dioxide","Quartz / Sand","The main component of beach sand and the glass in your window."),
            new Compound("N","O","NO2","Nitrogen Dioxide","Smog Gas","The reddish-brown gas responsible for a lot of urban air pollution."),
            new Compound("N","H","NH3","Ammonia","Ammonia","That sharp smell in cleaning products — also essential for making fertilizer."),
            new Compound("C","H","CH4","Methane","Natural Gas","The main component of the natural gas that heats homes and stoves."),
            new Compound("S","O","SO2","Sulfur Dioxide","Volcano/Match Smell","That burnt-match smell — also a major cause of acid rain."),
            new Compound("H","S","H2S","Hydrogen Sulfide","Rotten Egg Gas","Responsible for the smell of rotten eggs and some hot springs."),
            new Compound("K","Cl","KCl","Potassium Chloride","Salt Substitute","Used in low-sodium salt substitutes — and, at high doses, lethal injection."),
            new Compound("K","I","KI","Potassium Iodide","Iodized Salt Additive","Added to table salt to prevent iodine-deficiency disorders."),
            new Compound("Ca","Cl","CaCl2","Calcium Chloride","Road Salt / Ice Melt","Used to melt ice on roads — releases heat as it dissolves."),
            new Compound("Mg","Cl","MgCl2","Magnesium Chloride","De-icer / Bath Salts","Also sold as \"magnesium flakes\" for relaxing baths."),
            new Compound("Ag","Cl","AgCl","Silver Chloride","Photographic Film Compound","Darkens when exposed to light — the basis of old photographic film."),
            new Compound("Cu","O","CuO","Copper(II) Oxide","Black Copper Oxide","The black coating that forms on old copper pipes and pennies."),
            new Compound("Zn","O","ZnO","Zinc Oxide","Sunscreen / Diaper Cream","The white paste in mineral sunscreen and baby diaper cream."),
            new Compound("Ti","O","TiO2","Titanium Dioxide","White Pigment","Makes white paint white, and toothpaste opaque."),
            new Compound("H","F","HF","Hydrogen Fluoride","Glass-Etching Acid","One of the few acids that can dissolve glass itself."),
            new Compound("H","Br","HBr","Hydrogen Bromide","Hydrobromic Acid","A strong acid used in organic chemistry synthesis."),
            new Compound("H","I","HI","Hydrogen Iodide","Hydroiodic Acid","One of the strongest common acids, stronger than HCl."),
            new Compound("N","N","N2","Nitrogen Gas","The Air You Breathe (mostly)","About 78% of the air around you right now."),
            new Compound("O","O","O2","Oxygen Gas","The Oxygen You Breathe","About 21% of the air, and the part your body actually needs."),
            new Compound("H","H","H2","Hydrogen Gas","Hydrogen Fuel","The most abundant element in the universe, and a rocket fuel component."),
            new Compound("C","C","Diamond/Graphite","Carbon Allotropes","Diamond or Pencil Lead","Pure carbon can be the hardest natural material or one of the softest, depending on structure."),
            new Compound("Na","H","NaH","Sodium Hydride","Sodium Hydride","Reacts violently and explosively with water."),
            new Compound("Ca","C","CaC2","Calcium Carbide","Carbide (old lamp fuel)","Reacts with water to make acetylene gas — used in old miners' lamps."),
            new Compound("Fe","S","FeS","Iron(II) Sulfide","Fool's Gold (related)","A classic chemistry-class demo — iron filings and sulfur fused together."),
            new Compound("Cu","S","CuS","Copper(II) Sulfide","Covellite Mineral","A naturally occurring mineral, often iridescent blue."),
            new Compound("Pb","O","PbO2","Lead Dioxide","Car Battery Component","Used in the positive plate of lead-acid car batteries."),
            new Compound("Sn","O","SnO2","Tin Dioxide","Ceramic Glaze Ingredient","Used to make ceramic glazes opaque white."),
            new Compound("Br","Br","Br2","Bromine","Liquid Bromine","One of only two elements that are liquid at room temperature."),
            new Compound("Cl","Cl","Cl2","Chlorine Gas","Pool Chlorine Gas","Used to disinfect swimming pools and drinking water."),
            new Compound("P","O","P2O5","Phosphorus Pentoxide","Drying Agent","So good at absorbing water it's used as a powerful desiccant."),
            new Compound("Al","Cl","AlCl3","Aluminum Chloride","Antiperspirant Ingredient","The active ingredient in many antiperspirant deodorants."),
            new Compound("Zn","Cl","ZnCl2","Zinc Chloride","Soldering Flux","Used as a flux in soldering to clean metal surfaces."),
            new Compound("Ba","O","BaO","Barium Oxide","Glass Additive","Added to glass to increase its refractive index."),
            new Compound("Li","O","Li2O","Lithium Oxide","Ceramic Glass Additive","Used in some heat-resistant ceramic glass and lithium-ion battery research."),
            new Compound("Au","Cl","AuCl3","Gold(III) Chloride","Gold Plating Compound","Used in gold plating and as a photosensitizer.")
    };

    private static Compound findCompound(String a, String b) {
        for (Compound c : COMPOUNDS) if (c.matches(a, b)) return c;
        return null;
    }

    // ── Draggable element tile ───────────────────────────────────────────────
    private class ElemView extends FrameLayout {
        final Elem elem;
        float downRawX, downRawY, startX, startY;

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
        title.setText("\u269B PERIODIC TABLE — drag one element onto another");
        title.setTextColor(0xFFc9a84c);
        title.setTextSize(13f);
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
        resultText.setTextSize(14f);
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
            lp.topMargin  = dp((e.row - 1) * CELL) + (e.row >= 9 ? dp(CELL / 2) : 0); // gap before lanthanide/actinide rows
            ev.setLayoutParams(lp);
            ev.setX(lp.leftMargin);
            ev.setY(lp.topMargin);
            tableLayer.addView(ev);
            elemViews.add(ev);
        }

        return root;
    }

    // ── Touch-based drag ─────────────────────────────────────────────────────
    private boolean handleTouch(ElemView view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                view.downRawX = event.getRawX();
                view.downRawY = event.getRawY();
                view.startX = view.getX();
                view.startY = view.getY();
                view.bringToFront();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - view.downRawX;
                float dy = event.getRawY() - view.downRawY;
                view.setX(view.startX + dx);
                view.setY(view.startY + dy);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                ElemView target = findOverlap(view);
                if (target != null) {
                    onElementsCombined(view.elem, target.elem);
                }
                // Snap back to original grid position either way
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
                + "\uD83D\uDCA1 " + c.common + " — " + c.fact);
        } else {
            resultText.setText(a.symbol + " + " + b.symbol + " \u2192 no well-known everyday compound for this pair, sir — try another combination.");
        }
    }
}
