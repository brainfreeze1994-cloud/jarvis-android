package com.jarvis.android.chemistry;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class MolecularStructureView extends View {
    private Paint atomPaint, bondPaint, textPaint;
    private String moleculeType = "H2O";
    private List<Atom> atoms = new ArrayList<>();
    private List<Bond> bonds = new ArrayList<>();
    
    static class Atom {
        float x, y, radius;
        String symbol;
        int color;
        
        Atom(String symbol, float x, float y, float radius, int color) {
            this.symbol = symbol;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
        }
    }
    
    static class Bond {
        int from, to;
        boolean isDouble;
        
        Bond(int from, int to) {
            this(from, to, false);
        }
        
        Bond(int from, int to, boolean isDouble) {
            this.from = from;
            this.to = to;
            this.isDouble = isDouble;
        }
    }
    
    public MolecularStructureView(Context context) {
        super(context);
        init();
    }
    
    public MolecularStructureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        atomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        atomPaint.setStyle(Paint.Style.FILL);
        
        bondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bondPaint.setStyle(Paint.Style.STROKE);
        bondPaint.setStrokeWidth(6);
        bondPaint.setColor(Color.GRAY);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        loadMolecule("H2O");
    }
    
    public void setMolecule(String type) {
        loadMolecule(type);
        invalidate();
    }
    
    private void loadMolecule(String type) {
        atoms.clear();
        bonds.clear();
        moleculeType = type;
        
        switch (type) {
            case "H2O":
                atoms.add(new Atom("O", 150, 150, 40, Color.RED));
                atoms.add(new Atom("H", 100, 200, 25, Color.WHITE));
                atoms.add(new Atom("H", 200, 200, 25, Color.WHITE));
                bonds.add(new Bond(0, 1));
                bonds.add(new Bond(0, 2));
                break;
            case "CO2":
                atoms.add(new Atom("C", 150, 150, 35, Color.BLACK));
                atoms.add(new Atom("O", 80, 150, 35, Color.RED));
                atoms.add(new Atom("O", 220, 150, 35, Color.RED));
                bonds.add(new Bond(0, 1, true));
                bonds.add(new Bond(0, 2, true));
                break;
            case "CH4":
                atoms.add(new Atom("C", 150, 150, 35, Color.BLACK));
                atoms.add(new Atom("H", 150, 90, 25, Color.WHITE));
                atoms.add(new Atom("H", 100, 200, 25, Color.WHITE));
                atoms.add(new Atom("H", 200, 200, 25, Color.WHITE));
                atoms.add(new Atom("H", 150, 210, 25, Color.GRAY));
                for (int i = 1; i < 5; i++) bonds.add(new Bond(0, i));
                break;
            case "NH3":
                atoms.add(new Atom("N", 150, 130, 35, Color.BLUE));
                atoms.add(new Atom("H", 100, 200, 25, Color.WHITE));
                atoms.add(new Atom("H", 150, 210, 25, Color.WHITE));
                atoms.add(new Atom("H", 200, 200, 25, Color.WHITE));
                bonds.add(new Bond(0, 1));
                bonds.add(new Bond(0, 2));
                bonds.add(new Bond(0, 3));
                break;
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw bonds
        for (Bond bond : bonds) {
            Atom from = atoms.get(bond.from);
            Atom to = atoms.get(bond.to);
            
            canvas.drawLine(from.x, from.y, to.x, to.y, bondPaint);
            
            if (bond.isDouble) {
                canvas.drawLine(from.x + 8, from.y, to.x + 8, to.y, bondPaint);
            }
        }
        
        // Draw atoms
        for (Atom atom : atoms) {
            atomPaint.setColor(atom.color);
            canvas.drawCircle(atom.x, atom.y, atom.radius, atomPaint);
            
            textPaint.setColor(atom.color == Color.WHITE ? Color.BLACK : Color.WHITE);
            canvas.drawText(atom.symbol, atom.x, atom.y + 8, textPaint);
        }
    }
}
