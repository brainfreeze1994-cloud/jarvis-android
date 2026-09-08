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
                        HenryStudioManager.showHackingStudio(this, this::launchPrompt);
                        break;
                    case "finance_skills":
                        HenryStudioManager.showBusinessStudio(this, this::launchPrompt);
                        break;
                    case "medical_skills":
                        HenryStudioManager.showMedicalStudio(this, this::launchPrompt);
                        break;
                    case "programming_studio":
                        HenryStudioManager.showProgrammingStudio(this, this::launchPrompt);
                        break;
                    case "artifact_studio":
                        HenryStudioManager.showArtifactStudio(this, this::launchPrompt);
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
