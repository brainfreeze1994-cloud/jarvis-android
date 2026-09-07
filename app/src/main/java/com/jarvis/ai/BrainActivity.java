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
                    case "google_docs":
                        Toast.makeText(this, "Say: \"Create a Google Doc about…\"", Toast.LENGTH_LONG).show();
                        break;
                    case "google_sheets":
                        Toast.makeText(this, "Say: \"Create a Google Sheet for…\"", Toast.LENGTH_LONG).show();
                        break;
                    case "google_slides":
                        Toast.makeText(this, "Say: \"Create a Google Slides about…\"", Toast.LENGTH_LONG).show();
                        break;
                }
            });
        }

        TextView tvBack = findViewById(R.id.brain_back);
        if (tvBack != null) tvBack.setOnClickListener(v -> finish());
    }
}
