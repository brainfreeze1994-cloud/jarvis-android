package com.jarvis.android.chemistry;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jarvis.android.R;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ChemistryActivity extends AppCompatActivity {
    
    private GridLayout periodicGrid;
    private MolecularStructureView structureView;
    private TextView infoText;
    private List<Element> elements = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chemistry);
        
        periodicGrid = findViewById(R.id.periodic_grid);
        structureView = findViewById(R.id.structure_view);
        infoText = findViewById(R.id.info_text);
        
        periodicGrid.setColumnCount(18);
        
        loadElements();
        renderPeriodicTable();
        setupMoleculeButtons();
    }
    
    private void loadElements() {
        elements.add(new Element(1, "H", "Hydrogen", "Nonmetal", "1.008", "1s¹", 0xFF4CAF50));
        elements.add(new Element(2, "He", "Helium", "Noble Gas", "4.0026", "1s²", 0xFF9C27B0));
        elements.add(new Element(3, "Li", "Lithium", "Alkali Metal", "6.94", "[He] 2s¹", 0xFFF44336));
        elements.add(new Element(4, "Be", "Beryllium", "Alkaline Earth", "9.0122", "[He] 2s²", 0xFFFF9800));
        elements.add(new Element(5, "B", "Boron", "Metalloid", "10.81", "[He] 2s² 2p¹", 0xFF009688));
        elements.add(new Element(6, "C", "Carbon", "Nonmetal", "12.011", "[He] 2s² 2p²", 0xFF4CAF50));
        elements.add(new Element(7, "N", "Nitrogen", "Nonmetal", "14.007", "[He] 2s² 2p³", 0xFF4CAF50));
        elements.add(new Element(8, "O", "Oxygen", "Nonmetal", "15.999", "[He] 2s² 2p⁴", 0xFF4CAF50));
        elements.add(new Element(9, "F", "Fluorine", "Halogen", "18.998", "[He] 2s² 2p⁵", 0xFF03A9F4));
        elements.add(new Element(10, "Ne", "Neon", "Noble Gas", "20.180", "[He] 2s² 2p⁶", 0xFF9C27B0));
        // Add more elements as needed
    }
    
    private void renderPeriodicTable() {
        periodicGrid.removeAllViews();
        
        for (Element el : elements) {
            TextView tv = new TextView(this);
            tv.setText(el.symbol);
            tv.setTextColor(android.graphics.Color.WHITE);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(16);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setBackgroundColor(el.color);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(getColumn(el.atomicNumber), 1f);
            params.rowSpec = GridLayout.spec(getRow(el.atomicNumber));
            params.setMargins(2, 2, 2, 2);
            
            tv.setLayoutParams(params);
            tv.setOnClickListener(v -> showElementDetails(el));
            
            periodicGrid.addView(tv);
        }
    }
    
    private void setupMoleculeButtons() {
        LinearLayout buttonContainer = findViewById(R.id.molecule_buttons);
        String[] molecules = {"H2O", "CO2", "CH4", "NH3", "NaCl"};
        
        for (String mol : molecules) {
            Button btn = new Button(this);
            btn.setText(mol);
            btn.setOnClickListener(v -> {
                structureView.setMolecule(mol);
                updateMoleculeInfo(mol);
            });
            buttonContainer.addView(btn);
        }
    }
    
    private void updateMoleculeInfo(String mol) {
        String info = "";
        switch (mol) {
            case "H2O":
                info = "Water - Bent geometry, 104.5° angle, sp³ hybridization";
                break;
            case "CO2":
                info = "Carbon Dioxide - Linear geometry, 180° angle, sp hybridization";
                break;
            case "CH4":
                info = "Methane - Tetrahedral geometry, 109.5° angle, sp³ hybridization";
                break;
            case "NH3":
                info = "Ammonia - Trigonal pyramidal, 107° angle, sp³ hybridization";
                break;
            case "NaCl":
                info = "Sodium Chloride - Ionic crystal lattice structure";
                break;
        }
        infoText.setText(info);
    }
    
    private void showElementDetails(Element el) {
        Toast.makeText(this, 
            el.atomicNumber + ". " + el.name + "\n" + 
            "Mass: " + el.mass + " u\n" + 
            "Config: " + el.config,
            Toast.LENGTH_LONG).show();
    }
    
    private int getRow(int atomicNumber) {
        if (atomicNumber == 1 || atomicNumber == 2) return 0;
        if (atomicNumber >= 3 && atomicNumber <= 10) return 1;
        if (atomicNumber >= 11 && atomicNumber <= 18) return 2;
        return 3;
    }
    
    private int getColumn(int atomicNumber) {
        if (atomicNumber == 1) return 0;
        if (atomicNumber == 2) return 17;
        if (atomicNumber >= 3 && atomicNumber <= 4) return atomicNumber - 3;
        if (atomicNumber >= 5 && atomicNumber <= 10) return atomicNumber + 8;
        if (atomicNumber >= 11 && atomicNumber <= 12) return atomicNumber - 11;
        if (atomicNumber >= 13 && atomicNumber <= 18) return atomicNumber + 2;
        return 0;
    }
    
    public void exportToExcel(View v) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Periodic Table");
            
            Row header = sheet.createRow(0);
            String[] headers = {"Number", "Symbol", "Name", "Category", "Mass", "Config"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            int rowNum = 1;
            for (Element el : elements) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(el.atomicNumber);
                row.createCell(1).setCellValue(el.symbol);
                row.createCell(2).setCellValue(el.name);
                row.createCell(3).setCellValue(el.category);
                row.createCell(4).setCellValue(el.mass);
                row.createCell(5).setCellValue(el.config);
            }
            
            String fileName = "HENRY_PeriodicTable_" + System.currentTimeMillis() + ".xlsx";
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName;
            
            FileOutputStream fos = new FileOutputStream(path);
            workbook.write(fos);
            fos.close();
            workbook.close();
            
            Toast.makeText(this, "Saved: " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
