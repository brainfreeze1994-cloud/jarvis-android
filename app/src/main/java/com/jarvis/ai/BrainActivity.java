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
                    case "mental_imagery":
                        startActivity(new Intent(this, MentalImageryActivity.class));
                        break;
                    case "sensory_substitution":
                        startActivity(new Intent(this, SensorySubstitutionActivity.class));
                        break;
                    case "neural_plasticity":
                        startActivity(new Intent(this, NeuralPlasticityActivity.class));
                        break;
                    case "default_mode":
                        startActivity(new Intent(this, DefaultModeNetworkActivity.class));
                        break;
                    case "memory":
                        startActivity(new Intent(this, SmartMemoryActivity.class));
                        break;
                    case "hacking_skills":
                        showSkillsDialog("Cybersecurity & Ethical Hacking Intelligence",
                            new String[]{
                                "Perform an OWASP Top 10 vulnerability audit for my web app",
                                "Explain binary exploitation, ROP chains, and memory defenses",
                                "Network security audit: packet analysis & Wireshark triage",
                                "Cryptographic implementation review: AES-GCM and ECC keys"
                            });
                        break;
                    case "finance_skills":
                        showSkillsDialog("Business Strategy & Financial Acumen",
                            new String[]{
                                "Build a Discounted Cash Flow (DCF) model and valuation",
                                "Analyze SaaS unit economics: CAC, LTV, Rule of 40, and Burn",
                                "Conduct forensic 3-statement financial analysis",
                                "Create a venture capital cap table and dilution scenario"
                            });
                        break;
                    case "medical_skills":
                        showSkillsDialog("Clinical Medicine & Healthcare Intelligence",
                            new String[]{
                                "Provide a structured differential diagnosis framework",
                                "Check clinical pharmacology and CYP450 drug interactions",
                                "Interpret complex laboratory panels (CBC, CMP, ABG, Cardiac)",
                                "Evidence-based clinical triage and pathophysiology overview"
                            });
                        break;
                    case "programming_studio":
                        showSkillsDialog("Programming Studio & Coding Mentor",
                            new String[]{
                                "Build mode: Create an Android Jetpack Compose clean architecture app",
                                "Debug mode: Diagnose and fix a NullPointerException with stack trace",
                                "Learn mode: Teach Kotlin Coroutines and StateFlow step-by-step",
                                "Review mode: Audit Python FastAPI service for security and performance",
                                "Translate mode: Convert Java networking code to Kotlin Coroutines",
                                "Test mode: Write unit tests and edge cases for an API client"
                            });
                        break;
                    case "artifact_studio":
                        showSkillsDialog("Artifact Creation Studio (Docs, Slides, Sheets)",
                            new String[]{
                                "Generate a polished executive project proposal PDF",
                                "Create a comprehensive multi-tab financial forecast spreadsheet",
                                "Draft a high-impact pitch deck slide outline with speaker notes",
                                "Build a clean technical architecture specification document"
                            });
                        break;
                }
            });
        }

        TextView tvBack = findViewById(R.id.brain_back);
        if (tvBack != null) tvBack.setOnClickListener(v -> finish());
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
