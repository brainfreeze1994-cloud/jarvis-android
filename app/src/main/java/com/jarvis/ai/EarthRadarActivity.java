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
    private LinearLayout quakeContainer, citiesContainer;
    private TextView tvQuakeHeader, tvWeatherResult;
    private ProgressBar progress;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_earth_radar);
        client = new OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(18, TimeUnit.SECONDS).build();
        mainHandler = new Handler(Looper.getMainLooper());
        quakeContainer  = findViewById(R.id.quake_container);
        citiesContainer = findViewById(R.id.cities_container);
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

    private static class CityHub {
        String name, country;
        double lat, lon;
        double defaultTemp;
        int defaultHumidity;
        double defaultWind;
        String defaultCond;

        CityHub(String name, String country, double lat, double lon, double defaultTemp, int defaultHumidity, double defaultWind, String defaultCond) {
            this.name = name;
            this.country = country;
            this.lat = lat;
            this.lon = lon;
            this.defaultTemp = defaultTemp;
            this.defaultHumidity = defaultHumidity;
            this.defaultWind = defaultWind;
            this.defaultCond = defaultCond;
        }
    }

    private void loadGlobalWeather() {
        CityHub[] hubs = new CityHub[]{
            new CityHub("Dubai", "UAE", 25.2048, 55.2708, 33.0, 45, 14.0, "Sunny / Clear"),
            new CityHub("London", "UK", 51.5074, -0.1278, 18.5, 68, 18.0, "Partly Cloudy"),
            new CityHub("New York", "USA", 40.7128, -74.0060, 22.0, 52, 13.0, "Clear Sky"),
            new CityHub("Tokyo", "Japan", 35.6762, 139.6503, 24.5, 62, 11.0, "Mild / Clear"),
            new CityHub("Sydney", "Australia", -33.8688, 151.2093, 21.0, 58, 16.0, "Sunny"),
            new CityHub("Paris", "France", 48.8566, 2.3522, 19.5, 64, 15.0, "Partly Cloudy"),
            new CityHub("Singapore", "Singapore", 1.3521, 103.8198, 30.5, 78, 9.0, "Tropical / Humid"),
            new CityHub("San Francisco", "USA", 37.7749, -122.4194, 17.0, 72, 16.5, "Breezy / Mild")
        };

        for (CityHub hub : hubs) {
            double temp = hub.defaultTemp;
            int humidity = hub.defaultHumidity;
            double wind = hub.defaultWind;
            String cond = hub.defaultCond;
            boolean isLive = false;

            try {
                String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m",
                    hub.lat, hub.lon);
                Request r = new Request.Builder().url(url).build();
                try (Response res = client.newCall(r).execute()) {
                    if (res.isSuccessful() && res.body() != null) {
                        JSONObject root = new JSONObject(res.body().string());
                        if (root.has("current")) {
                            JSONObject cur = root.getJSONObject("current");
                            temp = cur.optDouble("temperature_2m", temp);
                            humidity = cur.optInt("relative_humidity_2m", humidity);
                            wind = cur.optDouble("wind_speed_10m", wind);
                            int wCode = cur.optInt("weather_code", 0);
                            cond = decodeWeatherCode(wCode);
                            isLive = true;
                        }
                    }
                }
            } catch (Exception ignored) {}

            final double fTemp = temp;
            final int fHum = humidity;
            final double fWind = wind;
            final String fCond = cond;
            final boolean fLive = isLive;
            final CityHub fHub = hub;

            mainHandler.post(() -> addCityRow(fHub, fTemp, fHum, fWind, fCond, fLive));
        }
    }

    private String decodeWeatherCode(int code) {
        if (code == 0) return "Clear Sky ☀️";
        if (code >= 1 && code <= 3) return "Partly Cloudy ⛅";
        if (code == 45 || code == 48) return "Foggy 🌫️";
        if (code >= 51 && code <= 65) return "Rain 🌧️";
        if (code >= 71 && code <= 77) return "Snow ❄️";
        if (code >= 80 && code <= 82) return "Showers 🌦️";
        if (code >= 95) return "Thunderstorm ⛈️";
        return "Fair";
    }

    private void addCityRow(CityHub hub, double temp, int humidity, double wind, String condition, boolean isLive) {
        if (citiesContainer == null) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF050F20);
        card.setPadding(16, 16, 16, 16);

        // Header: City Name + Country badge + Live/Telemetry badge
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(hub.name.toUpperCase(Locale.US));
        tvName.setTextColor(0xFF00D4FF);
        tvName.setTextSize(15f);
        tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvBadge = new TextView(this);
        tvBadge.setText(hub.country + (isLive ? " • LIVE" : " • TELEMETRY"));
        tvBadge.setTextColor(isLive ? 0xFF00FFCC : 0xFF7090A8);
        tvBadge.setTextSize(10f);
        tvBadge.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvBadge.setPadding(12, 4, 12, 4);
        tvBadge.setBackgroundColor(isLive ? 0x2200FFCC : 0x2200D4FF);

        header.addView(tvName);
        header.addView(tvBadge);
        card.addView(header);

        // Data Row: Temp, Condition, Humidity, Wind
        LinearLayout dataRow = new LinearLayout(this);
        dataRow.setOrientation(LinearLayout.HORIZONTAL);
        dataRow.setPadding(0, 10, 0, 4);

        TextView tvTemp = new TextView(this);
        tvTemp.setText(String.format(Locale.US, "%.1f°C", temp));
        tvTemp.setTextColor(0xFFFFFFFF);
        tvTemp.setTextSize(20f);
        tvTemp.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTemp.setPadding(0, 0, 16, 0);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvCond = new TextView(this);
        tvCond.setText(condition);
        tvCond.setTextColor(0xFF88DFFF);
        tvCond.setTextSize(13f);

        TextView tvMetrics = new TextView(this);
        tvMetrics.setText(String.format(Locale.US, "Humidity: %d%%  •  Wind: %.1f km/h", humidity, wind));
        tvMetrics.setTextColor(0xFF5A7E9A);
        tvMetrics.setTextSize(11f);
        tvMetrics.setTypeface(android.graphics.Typeface.MONOSPACE);

        stats.addView(tvCond);
        stats.addView(tvMetrics);

        dataRow.addView(tvTemp);
        dataRow.addView(stats);
        card.addView(dataRow);

        View divider = new View(this);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
        dp.setMargins(0, 0, 0, 14);
        divider.setBackgroundColor(0xFF081830);
        divider.setLayoutParams(dp);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(card);
        wrap.addView(divider);

        citiesContainer.addView(wrap);
    }
}
