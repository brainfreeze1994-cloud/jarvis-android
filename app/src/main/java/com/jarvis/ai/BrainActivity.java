package com.jarvis.ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * BrainActivity — HENRY Brain Map
 * Hosts the HenryBrainView canvas and routes taps to the correct module.
 */
public class BrainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 4001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brain);

        TextView tvTitle = findViewById(R.id.brain_title);
        if (tvTitle != null) tvTitle.setText("H.E.N.R.Y BRAIN");

        HenryBrainView brainView = findViewById(R.id.brain_view);
        if (brainView != null) {
            brainView.setOnRegionClickListener(region -> {
                switch (region) {
                    case "reasoning_core":
                        launchPrompt("Explain the conceptual reasoning and architecture behind H.E.N.R.Y.");
                        break;
                    case "vision_cortex":
                        startActivity(new Intent(this, VisionActivity.class));
                        break;
                    case "memory_banks":
                        startActivity(new Intent(this, SmartMemoryActivity.class));
                        break;
                    case "planning_core":
                        launchPrompt("ULTRA: Plan and execute multi-step research and production workflow");
                        break;
                    case "action_engine":
                        launchPrompt("Show active tool execution and action engine commands");
                        break;
                    case "research_core":
                        startActivity(new Intent(this, SpaceActivity.class));
                        break;
                    case "creative_engine":
                        HenryStudioManager.showScriptwriterStudio(this, this::launchPrompt);
                        break;
                    case "safety_core":
                        startActivity(new Intent(this, SystemDiagnosticActivity.class));
                        break;
                    case "code_engine":
                        HenryStudioManager.showProgrammingStudio(this, this::launchPrompt);
                        break;
                    case "science_core":
                        startActivity(new Intent(this, com.jarvis.android.chemistry.ChemistryActivity.class));
                        break;
                    case "math_engine":
                        launchPrompt("Solve 3x^2 - 12x + 9 = 0 using Polya step-by-step reasoning");
                        break;
                    default:
                        Toast.makeText(this, "Region activated: " + region, Toast.LENGTH_SHORT).show();
                        break;
                }
            });
        }

        TextView tvBack = findViewById(R.id.brain_back);
        if (tvBack != null) tvBack.setOnClickListener(v -> finish());
    }

    private void launchPrompt(String prompt) {
        if (prompt == null || prompt.isEmpty()) return;
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.putExtra("launch_prompt", prompt);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showSkillsDialog(String title, String[] prompts) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(prompts, (dialog, which) -> {
                String selected = prompts[which];
                android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
                intent.putExtra("launch_prompt", selected);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Close", null)
            .show();
    }
}
