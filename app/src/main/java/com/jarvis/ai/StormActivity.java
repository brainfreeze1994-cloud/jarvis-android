package com.jarvis.ai;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Storm Activity — tracks active tropical storms, typhoons and hurricanes
 * worldwide (position, category, wind speed, affected countries) using the
 * free GDACS (Global Disaster Alert and Coordination System) API, no key
 * required. Same visual language as EarthRadarActivity (list) + MapActivity
 * (Leaflet WebView) so it matches the rest of the app.
 *
 * Voice/text triggers: "track storms", "typhoon tracker", "hurricane tracker",
 * "cyclone tracker", "storm activity", "active storms".
 */
public class StormActivity extends AppCompatActivity {

    private static final String FEED_URL =
        "https://www.gdacs.org/gdacsapi/api/Events/geteventlist/EVENTS4APP";

    private OkHttpClient client;
    private Handler mainHandler;
    private LinearLayout stormContainer;
    private TextView tvHeader;
    private ProgressBar progress;
    private WebView mapView;

    private static class Storm {
        String name, country, alertLevel, severityText, source, fromDate, toDate, reportUrl;
        double severity; // km/h
        double lat, lon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        client = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .build();
        mainHandler = new Handler(Looper.getMainLooper());

        setContentView(buildLayout());
        loadStorms();
    }

    // ── Layout (programmatic, matching EarthRadarActivity/MapActivity styling) ─
    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0d0d0d);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0xFF1a1a1a);
        topBar.setPadding(24, 20, 16, 20);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("🌀 STORM TRACKER");
        title.setTextColor(0xFFc9a84c);
        title.setTextSize(17f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);
        topBar.addView(title);

        TextView btnClose = new TextView(this);
        btnClose.setText("✕");
        btnClose.setTextColor(0xFFc9a84c);
        btnClose.setTextSize(20f);
        btnClose.setPadding(24, 0, 8, 0);
        btnClose.setOnClickListener(v -> finish());
        topBar.addView(btnClose);

        root.addView(topBar);

        // Map (Leaflet via WebView, same pattern as MapActivity)
        mapView = new WebView(this);
        LinearLayout.LayoutParams mapLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(260));
        mapView.setLayoutParams(mapLp);
        WebSettings ws = mapView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        mapView.setWebViewClient(new WebViewClient());
        root.addView(mapView);

        // Header row (storm count)
        tvHeader = new TextView(this);
        tvHeader.setText("Loading active storms…");
        tvHeader.setTextColor(0xFFc8e8f8);
        tvHeader.setTextSize(14f);
        tvHeader.setPadding(24, 20, 24, 8);
        root.addView(tvHeader);

        // Progress bar
        progress = new ProgressBar(this);
        LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progLp.gravity = Gravity.CENTER;
        progLp.topMargin = dp(24);
        progress.setLayoutParams(progLp);
        root.addView(progress);

        // Scrollable storm list
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        stormContainer = new LinearLayout(this);
        stormContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(stormContainer);
        root.addView(scroll);

        return root;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ── Data ────────────────────────────────────────────────────────────────
    private void loadStorms() {
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<Storm> storms = fetchStorms();
            mainHandler.post(() -> {
                progress.setVisibility(View.GONE);
                renderStorms(storms);
                renderMap(storms);
            });
        }).start();
    }

    private List<Storm> fetchStorms() {
        List<Storm> out = new ArrayList<>();
        try {
            Request r = new Request.Builder().url(FEED_URL).build();
            try (Response res = client.newCall(r).execute()) {
                if (!res.isSuccessful() || res.body() == null) return out;
                JSONObject j = new JSONObject(res.body().string());
                JSONArray feats = j.getJSONArray("features");
                for (int i = 0; i < feats.length(); i++) {
                    JSONObject feat = feats.getJSONObject(i);
                    JSONObject props = feat.getJSONObject("properties");
                    if (!"TC".equals(props.optString("eventtype"))) continue;

                    Storm s = new Storm();
                    s.name         = props.optString("eventname", props.optString("name", "Unnamed"));
                    s.country      = props.optString("country", "");
                    s.alertLevel   = props.optString("alertlevel", "Green");
                    s.source       = props.optString("source", "");
                    s.fromDate     = props.optString("fromdate", "");
                    s.toDate       = props.optString("todate", "");
                    s.reportUrl    = props.optJSONObject("url") != null
                        ? props.optJSONObject("url").optString("report", "") : "";
                    JSONObject sev = props.optJSONObject("severitydata");
                    if (sev != null) {
                        s.severity     = sev.optDouble("severity", 0);
                        s.severityText = sev.optString("severitytext", "");
                    }
                    JSONObject geom = feat.optJSONObject("geometry");
                    if (geom != null) {
                        JSONArray coords = geom.optJSONArray("coordinates");
                        if (coords != null && coords.length() >= 2) {
                            s.lon = coords.optDouble(0, 0);
                            s.lat = coords.optDouble(1, 0);
                        }
                    }
                    out.add(s);
                }
            }
        } catch (Exception ignored) {}

        // Red > Orange > Green, then strongest winds first
        out.sort((a, b) -> {
            int ra = alertRank(a.alertLevel), rb = alertRank(b.alertLevel);
            if (ra != rb) return rb - ra;
            return Double.compare(b.severity, a.severity);
        });
        return out;
    }

    private int alertRank(String level) {
        if ("Red".equalsIgnoreCase(level)) return 2;
        if ("Orange".equalsIgnoreCase(level)) return 1;
        return 0;
    }

    private int alertColor(String level) {
        if ("Red".equalsIgnoreCase(level)) return 0xFFFF4444;
        if ("Orange".equalsIgnoreCase(level)) return 0xFFFF9800;
        return 0xFF4CAF50;
    }

    // ── List rendering ──────────────────────────────────────────────────────
    private void renderStorms(List<Storm> storms) {
        stormContainer.removeAllViews();
        if (storms.isEmpty()) {
            tvHeader.setText("No active tropical storms right now, sir.");
            return;
        }
        tvHeader.setText(storms.size() + " active tropical storm" + (storms.size() != 1 ? "s" : "") + " worldwide");
        for (Storm s : storms) addStormRow(s);
    }

    private void addStormRow(Storm s) {
        int color = alertColor(s.alertLevel);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(24, 20, 24, 20);
        row.setBackgroundColor("Red".equalsIgnoreCase(s.alertLevel) ? 0xFF200A0A
            : "Orange".equalsIgnoreCase(s.alertLevel) ? 0xFF1A1200 : 0xFF050F20);
        row.setOnClickListener(v -> {
            if (!s.reportUrl.isEmpty())
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(s.reportUrl)));
        });

        // Name + alert badge
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(this);
        badge.setText(" " + s.alertLevel.toUpperCase(Locale.US) + " ");
        badge.setTextColor(0xFF000000);
        badge.setBackgroundColor(color);
        badge.setTextSize(11f);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(badge);

        TextView tvName = new TextView(this);
        tvName.setText("  " + s.name);
        tvName.setTextColor(0xFFFFFFFF);
        tvName.setTextSize(16f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(tvName);

        row.addView(top);

        // Category / severity text
        TextView tvCat = new TextView(this);
        tvCat.setText(s.severityText.isEmpty() ? "Category unknown" : s.severityText);
        tvCat.setTextColor(color);
        tvCat.setTextSize(13f);
        tvCat.setTypeface(Typeface.MONOSPACE);
        tvCat.setPadding(0, 6, 0, 0);
        row.addView(tvCat);

        // Affected countries
        if (!s.country.isEmpty()) {
            TextView tvCountry = new TextView(this);
            tvCountry.setText("📍 " + s.country);
            tvCountry.setTextColor(0xFFc8e8f8);
            tvCountry.setTextSize(13f);
            tvCountry.setPadding(0, 4, 0, 0);
            row.addView(tvCountry);
        }

        // Dates + source
        TextView tvMeta = new TextView(this);
        tvMeta.setText(formatDate(s.fromDate) + " → " + formatDate(s.toDate) + "   ·   " + s.source
            + "   ·   tap for full report");
        tvMeta.setTextColor(0xFF3a7aa0);
        tvMeta.setTextSize(11f);
        tvMeta.setPadding(0, 6, 0, 0);
        row.addView(tvMeta);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFF081830);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(row);
        wrap.addView(divider);
        stormContainer.addView(wrap);
    }

    private String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "?";
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("MMM dd", Locale.US);
            Date d = in.parse(iso);
            return d != null ? out.format(d) : iso;
        } catch (Exception e) { return iso; }
    }

    // ── Map rendering (Leaflet, same pattern as MapActivity) ───────────────
    private void renderMap(List<Storm> storms) {
        StringBuilder markers = new StringBuilder();
        for (Storm s : storms) {
            if (s.lat == 0 && s.lon == 0) continue;
            String hex = String.format("#%06X", (0xFFFFFF & alertColor(s.alertLevel)));
            String popup = (s.name + " — " + s.severityText).replace("'", "\\'");
            markers.append("L.circleMarker([").append(s.lat).append(",").append(s.lon).append("],")
                .append("{radius:9,color:'").append(hex).append("',fillColor:'").append(hex)
                .append("',fillOpacity:0.85,weight:2}).addTo(map).bindPopup('").append(popup).append("');");
        }

        String html = "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<style>html,body,#map{margin:0;padding:0;width:100%;height:100%;background:#0d0d0d;}" +
            ".leaflet-control-attribution{display:none}</style></head>" +
            "<body><div id='map'></div><script>" +
            "var map=L.map('map',{zoomControl:true,worldCopyJump:true}).setView([15,120],2);" +
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
            "{attribution:'OSM',maxZoom:19}).addTo(map);" +
            markers +
            "</script></body></html>";

        mapView.loadDataWithBaseURL("https://henry.ai", html, "text/html", "UTF-8", null);
    }
}
