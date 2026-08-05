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
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpaceActivity extends AppCompatActivity {
    private OkHttpClient client;
    private Handler mainHandler;
    private TextView tvIssPos, tvIssDesc, tvNasaTitle, tvNasaText, tvAstCount, tvAstClosest;
    private ProgressBar progress;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_space);
        client = new OkHttpClient.Builder().connectTimeout(12,TimeUnit.SECONDS).readTimeout(18,TimeUnit.SECONDS).build();
        mainHandler = new Handler(Looper.getMainLooper());
        tvIssPos    = findViewById(R.id.tv_iss_pos);
        tvIssDesc   = findViewById(R.id.tv_iss_desc);
        tvNasaTitle = findViewById(R.id.tv_nasa_title);
        tvNasaText  = findViewById(R.id.tv_nasa_text);
        tvAstCount  = findViewById(R.id.tv_ast_count);
        tvAstClosest= findViewById(R.id.tv_ast_closest);
        progress    = findViewById(R.id.space_progress);
        TextView btnBack = findViewById(R.id.btn_space_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        TextView btnFr24 = findViewById(R.id.btn_spot_station);
        if (btnFr24 != null) btnFr24.setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://spotthestation.nasa.gov"))));
        loadAll();
    }

    private void loadAll() {
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            loadISS();
            loadNASA();
            loadAsteroids();
            mainHandler.post(() -> progress.setVisibility(View.GONE));
        }).start();
    }

    private void loadISS() {
        try {
            Request r = new Request.Builder().url("http://api.open-notify.org/iss-now.json").build();
            try (Response res = client.newCall(r).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    JSONObject j   = new JSONObject(res.body().string());
                    JSONObject pos = j.getJSONObject("iss_position");
                    double lat = Double.parseDouble(pos.getString("latitude"));
                    double lon = Double.parseDouble(pos.getString("longitude"));
                    String txt = String.format("%.3f°N  %.3f°E", lat, lon);
                    mainHandler.post(() -> { tvIssPos.setText(txt); tvIssDesc.setText("Orbiting at ~408 km • 28,000 km/h • 1 orbit per 92 min"); });
                }
            }
        } catch (Exception e) { mainHandler.post(() -> tvIssPos.setText("ISS data unavailable")); }
    }

    private void loadNASA() {
        try {
            Request r = new Request.Builder().url("https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY").build();
            try (Response res = client.newCall(r).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    JSONObject j = new JSONObject(res.body().string());
                    String title = j.optString("title","NASA APOD");
                    String expl  = j.optString("explanation","").replace("\n"," ");
                    if (expl.length() > 280) expl = expl.substring(0,280) + "…";
                    final String t = title, x = expl;
                    mainHandler.post(() -> { tvNasaTitle.setText(t); tvNasaText.setText(x); });
                }
            }
        } catch (Exception e) { mainHandler.post(() -> tvNasaTitle.setText("NASA photo unavailable")); }
    }

    private void loadAsteroids() {
        try {
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
            Request r = new Request.Builder().url("https://api.nasa.gov/neo/rest/v1/feed?start_date="+today+"&end_date="+today+"&api_key=DEMO_KEY").build();
            try (Response res = client.newCall(r).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    JSONObject j   = new JSONObject(res.body().string());
                    int count = j.optInt("element_count",0);
                    JSONObject neos = j.optJSONObject("near_earth_objects");
                    String closest = "N/A";
                    if (neos != null) {
                        JSONArray day = neos.optJSONArray(today);
                        if (day != null && day.length() > 0) {
                            JSONObject first = day.getJSONObject(0);
                            String name = first.optString("name","Unknown");
                            JSONArray approaches = first.optJSONArray("close_approach_data");
                            String dist = "?";
                            if (approaches != null && approaches.length() > 0) {
                                dist = approaches.getJSONObject(0).getJSONObject("miss_distance").optString("kilometers","?");
                                try { dist = String.format("%,.0f km", Double.parseDouble(dist)); } catch(Exception ignored){}
                            }
                            closest = name + " at " + dist;
                        }
                    }
                    final int c = count; final String cl = closest;
                    mainHandler.post(() -> {
                        tvAstCount.setText(c + " near-Earth objects tracked today");
                        tvAstClosest.setText("Closest: " + cl);
                    });
                }
            }
        } catch (Exception e) { mainHandler.post(() -> tvAstCount.setText("Asteroid data unavailable")); }
    }
}
