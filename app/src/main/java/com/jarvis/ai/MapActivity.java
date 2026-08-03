package com.jarvis.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * In-app map using OpenStreetMap via Leaflet.js (WebView).
 * Free, no API key. Shows current location, search, pins.
 */
public class MapActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY  = "map_query";
    public static final String EXTRA_LAT    = "map_lat";
    public static final String EXTRA_LON    = "map_lon";
    public static final String EXTRA_LABEL  = "map_label";

    private static final int PERM_LOC = 401;
    private WebView webView;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0d0d0d);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0xFF1a1a1a);
        topBar.setPadding(8, 8, 8, 8);

        etSearch = new EditText(this);
        etSearch.setHint("Search place…");
        etSearch.setTextColor(0xFFFFFFFF);
        etSearch.setHintTextColor(0xFF888888);
        etSearch.setBackgroundColor(0xFF2a2a2a);
        etSearch.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        etSearch.setLayoutParams(etLp);
        topBar.addView(etSearch);

        Button btnSearch = new Button(this);
        btnSearch.setText("GO");
        btnSearch.setBackgroundColor(0xFFc9a84c);
        btnSearch.setTextColor(0xFF0d0d0d);
        btnSearch.setOnClickListener(v -> searchPlace(etSearch.getText().toString().trim()));
        topBar.addView(btnSearch);

        Button btnClose = new Button(this);
        btnClose.setText("✕");
        btnClose.setBackgroundColor(0xFF333333);
        btnClose.setTextColor(0xFFc9a84c);
        btnClose.setOnClickListener(v -> finish());
        topBar.addView(btnClose);

        root.addView(topBar);

        // WebView
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setGeolocationEnabled(true);
        ws.setAllowFileAccess(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin,
                android.webkit.GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new MapBridge(), "Android");
        root.addView(webView);

        setContentView(root);

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERM_LOC);
        }

        // Load map
        double initLat = 25.2048, initLon = 55.2708; // Dubai default
        String initLabel = "";
        if (getIntent() != null) {
            initLat   = getIntent().getDoubleExtra(EXTRA_LAT, initLat);
            initLon   = getIntent().getDoubleExtra(EXTRA_LON, initLon);
            initLabel = getIntent().getStringExtra(EXTRA_LABEL) != null
                        ? getIntent().getStringExtra(EXTRA_LABEL) : "";
            String query = getIntent().getStringExtra(EXTRA_QUERY);
            if (query != null && !query.isEmpty()) etSearch.setText(query);
        }

        loadMap(initLat, initLon, initLabel);
    }

    private void loadMap(double lat, double lon, String label) {
        String markerJs = label.isEmpty() ? "" :
            "var marker = L.marker([" + lat + "," + lon + "]).addTo(map)" +
            ".bindPopup('" + label.replace("'", "\\'") + "').openPopup();";

        String html = "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<style>html,body,#map{margin:0;padding:0;width:100%;height:100%;background:#0d0d0d;}" +
            ".leaflet-control-attribution{display:none}</style></head>" +
            "<body><div id='map'></div><script>" +
            "var map=L.map('map',{zoomControl:true}).setView([" + lat + "," + lon + "],14);" +
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
            "{attribution:'OSM',maxZoom:19}).addTo(map);" +
            // Current location marker (gold dot)
            "var goldIcon=L.divIcon({className:'',html:" +
            "'<div style=\"width:16px;height:16px;background:#c9a84c;border-radius:50%;border:2px solid #fff\"></div>'," +
            "iconSize:[16,16],iconAnchor:[8,8]});" +
            markerJs +
            // Try geolocation
            "if(navigator.geolocation){" +
            "navigator.geolocation.getCurrentPosition(function(p){" +
            "L.marker([p.coords.latitude,p.coords.longitude],{icon:goldIcon}).addTo(map)" +
            ".bindPopup('You are here');},function(){})}" +
            // Click on map to pin
            "map.on('click',function(e){" +
            "L.marker([e.latlng.lat,e.latlng.lng]).addTo(map)" +
            ".bindPopup(e.latlng.lat.toFixed(5)+','+e.latlng.lng.toFixed(5)).openPopup();" +
            "Android.onMapTap(e.latlng.lat,e.latlng.lng);})" +
            // Search function
            ";function searchOnMap(q){" +
            "fetch('https://nominatim.openstreetmap.org/search?q='+encodeURIComponent(q)+'&format=json&limit=1'," +
            "{headers:{'User-Agent':'HENRY-AI/1.0'}})" +
            ".then(r=>r.json()).then(d=>{if(d.length>0){" +
            "var lat=parseFloat(d[0].lat),lon=parseFloat(d[0].lon);" +
            "map.setView([lat,lon],16);" +
            "L.marker([lat,lon]).addTo(map).bindPopup(d[0].display_name).openPopup();" +
            "Android.onSearchResult(lat,lon,d[0].display_name);" +
            "}else{Android.onSearchResult(0,0,'not_found');}})}" +
            "</script></body></html>";

        webView.loadDataWithBaseURL("https://henry.ai", html, "text/html", "UTF-8", null);
    }

    private void searchPlace(String query) {
        if (query.isEmpty()) return;
        webView.evaluateJavascript("searchOnMap('" + query.replace("'", "\\'") + "')", null);
    }

    class MapBridge {
        @JavascriptInterface
        public void onMapTap(double lat, double lon) {
            runOnUiThread(() -> Toast.makeText(MapActivity.this,
                String.format("%.5f, %.5f", lat, lon), Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void onSearchResult(double lat, double lon, String name) {
            runOnUiThread(() -> {
                if ("not_found".equals(name)) {
                    Toast.makeText(MapActivity.this, "Place not found", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MapActivity.this, name, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERM_LOC) webView.reload();
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
