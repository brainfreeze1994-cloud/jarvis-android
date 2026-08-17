package com.jarvis.android.chemistry;

public class Element {
    public int atomicNumber;
    public String symbol;
    public String name;
    public String category;
    public String mass;
    public String config;
    public int color;

    public Element(int atomicNumber, String symbol, String name, String category, 
                   String mass, String config, int color) {
        this.atomicNumber = atomicNumber;
        this.symbol = symbol;
        this.name = name;
        this.category = category;
        this.mass = mass;
        this.config = config;
        this.color = color;
    }
}
