package com.jarvis.ai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EarthRadarActivity extends AppCompatActivity {
    private OkHttpClient client;
    private Handler mainHandler;
    private LinearLayout quakeContainer;
    private TextView tvQuakeHeader, tvWeatherResult;
    private ProgressBar progress;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_earth_radar);
        client = new OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(18, TimeUnit.SECONDS).build();
        mainHandler = new Handler(Looper.getMainLooper());
        quakeContainer  = findViewById(R.id.quake_container);
        tvQuakeHeader   = findViewById(R.id.tv_quake_header);
        tvWeatherResult = findViewById(R.id.tv_weather_result);
        progress        = findViewById(R.id.radar_progress);

        TextView btnBack = findViewById(R.id.btn_radar_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView btnUsgs = findViewById(R.id.btn_usgs);
        if (btnUsgs != null) btnUsgs.setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://earthquake.usgs.gov/earthquakes/map/"))));

        loadAll();
    }

    private void loadAll() {
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> { loadQuakes(); loadGlobalWeather(); mainHandler.post(() -> progress.setVisibility(View.GONE)); }).start();
    }

    private void loadQuakes() {
        try {
            Request r = new Request.Builder()
                .url("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_week.geojson")
                .build();
            try (Response res = client.newCall(r).execute()) {
                if (!res.isSuccessful() || res.body() == null) return;
                JSONObject j      = new JSONObject(res.body().string());
                JSONArray  feats  = j.getJSONArray("features");
                int count = feats.length();
                final String header = count + " significant earthquake" + (count != 1 ? "s" : "") + " this week";
                mainHandler.post(() -> tvQuakeHeader.setText(header));

                for (int i = 0; i < Math.min(count, 8); i++) {
                    JSONObject props = feats.getJSONObject(i).getJSONObject("properties");
                    double mag  = props.optDouble("mag", 0);
                    String place = props.optString("place", "Unknown");
                    long   time  = props.optLong("time", 0);
                    String date  = new SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(new Date(time));
                    boolean hazardous = mag >= 6.0;
                    final double fmag = mag; final String fplace = place; final String fdate = date; final boolean fhaz = hazardous;
                    mainHandler.post(() -> addQuakeRow(fmag, fplace, fdate, fhaz));
                }
            }
        } catch (Exception e) {
            mainHandler.post(() -> tvQuakeHeader.setText("Earthquake data unavailable"));
        }
    }

    private void addQuakeRow(double mag, String place, String date, boolean hazardous) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 16, 16, 16);
        row.setBackgroundColor(hazardous ? 0xFF0D0A00 : 0xFF050F20);

        // Magnitude badge
        TextView tvMag = new TextView(this);
        tvMag.setText(String.format("M%.1f", mag));
        tvMag.setTextColor(mag >= 7 ? 0xFFFF4444 : mag >= 6 ? 0xFFFFC107 : 0xFF00D4FF);
        tvMag.setTextSize(16f);
        tvMag.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvMag.setMinWidth(80);
        tvMag.setPadding(0, 0, 16, 0);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(lp);

        TextView tvPlace = new TextView(this);
        tvPlace.setText(place); tvPlace.setTextColor(0xFFc8e8f8); tvPlace.setTextSize(14f);

        TextView tvDate = new TextView(this);
        tvDate.setText(date + (hazardous ? "  ⚠ MAJOR" : ""));
        tvDate.setTextColor(hazardous ? 0xFFFF8800 : 0xFF3a7aa0); tvDate.setTextSize(12f);

        info.addView(tvPlace); info.addView(tvDate);
        row.addView(tvMag); row.addView(info);

        View divider = new View(this);
        android.widget.LinearLayout.LayoutParams dp = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divider.setBackgroundColor(0xFF081830);
        divider.setLayoutParams(dp);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(row); wrap.addView(divider);
        quakeContainer.addView(wrap);
    }

    private void loadGlobalWeather() {
        // Load weather for a few major cities
        String[] cities = {"Dubai", "London", "New York", "Tokyo", "Sydney"};
        StringBuilder sb = new StringBuilder();
        for (String city : cities) {
            try {
                Request r = new Request.Builder()
                    .url("https://wttr.in/" + city.replace(" ","+") + "?format=%C+%t+%h")
                    .build();
                try (Response res = client.newCall(r).execute()) {
                    if (res.isSuccessful() && res.body() != null) {
                        String line = res.body().string().trim();
                        sb.append(city).append(":  ").append(line).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
        final String result = sb.toString().trim();
        mainHandler.post(() -> {
            if (result.isEmpty()) tvWeatherResult.setText("Weather data unavailable");
            else tvWeatherResult.setText(result);
        });
    }
}
