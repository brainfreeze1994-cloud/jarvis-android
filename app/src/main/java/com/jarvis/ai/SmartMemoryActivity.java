package com.jarvis.ai;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

/**
 * SmartMemoryActivity — HENRY Memory Bank Viewer & Editor
 * Shows all facts HENRY has learned about the user,
 * allows adding, editing, and clearing memories.
 */
public class SmartMemoryActivity extends AppCompatActivity {

    private LinearLayout memoryList;
    private EditText etNewFact;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_memory);

        tvTitle    = findViewById(R.id.sm_title);
        memoryList = findViewById(R.id.sm_memory_list);
        etNewFact  = findViewById(R.id.sm_new_fact);

        if (tvTitle != null) tvTitle.setText("◈ MEMORY BANKS");

        Button btnAdd = findViewById(R.id.sm_btn_add);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> {
            String fact = etNewFact != null ? etNewFact.getText().toString().trim() : "";
            if (!fact.isEmpty()) {
                SmartMemory.addFact(this, fact);
                etNewFact.setText("");
                refreshList();
                Toast.makeText(this, "Memory stored, sir.", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnClear = findViewById(R.id.sm_btn_clear);
        if (btnClear != null) btnClear.setOnClickListener(v ->
            new android.app.AlertDialog.Builder(this)
                .setTitle("Clear All Memories?")
                .setMessage("This will erase everything HENRY knows about you.")
                .setPositiveButton("Clear", (d, w) -> { SmartMemory.clearAll(this); refreshList(); })
                .setNegativeButton("Cancel", null).show());

        Button btnBack = findViewById(R.id.sm_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        refreshList();
    }

    private void refreshList() {
        if (memoryList == null) return;
        memoryList.removeAllViews();
        List<String> facts = SmartMemory.getFacts(this);
        if (facts.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No memories stored yet.\nConverse with HENRY and he will learn.");
            tv.setTextColor(0xFF2A6A8A);
            tv.setTextSize(14f);
            tv.setPadding(16, 16, 16, 16);
            memoryList.addView(tv);
            return;
        }
        for (int i = 0; i < facts.size(); i++) {
            final String fact = facts.get(i);
            final int idx = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(8, 8, 8, 8);

            TextView tv = new TextView(this);
            tv.setText("◈ " + fact);
            tv.setTextColor(0xFF00D4FF);
            tv.setTextSize(13f);
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(tvLp);

            Button btnDel = new Button(this);
            btnDel.setText("✕");
            btnDel.setTextColor(0xFFFF4444);
            btnDel.setBackgroundColor(0x00000000);
            btnDel.setTextSize(14f);
            btnDel.setOnClickListener(v -> {
                SmartMemory.removeFact(this, idx);
                refreshList();
            });

            row.addView(tv);
            row.addView(btnDel);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFF0A2A3A);

            memoryList.addView(row);
            memoryList.addView(divider);
        }
    }
}
