package com.jarvis.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
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
 * In-app interactive Google Maps interface.
 * Real Google Maps with live search, satellite view, traffic, place markers,
 * and direct one-tap handoff to the official Google Maps Android app.
 */
public class MapActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY  = "map_query";
    public static final String EXTRA_LAT    = "map_lat";
    public static final String EXTRA_LON    = "map_lon";
    public static final String EXTRA_LABEL  = "map_label";

    private static final int PERM_LOC = 401;
    private WebView webView;
    private EditText etSearch;
    private String currentQuery = "";
    private double currentLat = 25.2048;
    private double currentLon = 55.2708;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF07152B);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0xFF0F2744);
        topBar.setPadding(12, 10, 12, 10);
        topBar.setGravity(android.view.Gravity.CENTER_VERTICAL);

        etSearch = new EditText(this);
        etSearch.setHint("Search Google Maps…");
        etSearch.setTextColor(0xFFFFFFFF);
        etSearch.setHintTextColor(0xFF88A0B8);
        etSearch.setBackgroundColor(0xFF091E36);
        etSearch.setPadding(16, 12, 16, 12);
        etSearch.setTextSize(14f);
        etSearch.setSingleLine(true);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        etLp.setMarginEnd(8);
        etSearch.setLayoutParams(etLp);
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            searchPlace(etSearch.getText().toString().trim());
            return true;
        });
        topBar.addView(etSearch);

        Button btnSearch = new Button(this);
        btnSearch.setText("GO");
        btnSearch.setBackgroundColor(0xFF00D4FF);
        btnSearch.setTextColor(0xFF051124);
        btnSearch.setTextSize(13f);
        btnSearch.setPadding(12, 4, 12, 4);
        btnSearch.setOnClickListener(v -> searchPlace(etSearch.getText().toString().trim()));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMarginEnd(8);
        btnSearch.setLayoutParams(btnLp);
        topBar.addView(btnSearch);

        Button btnOpenApp = new Button(this);
        btnOpenApp.setText("🗺 APP");
        btnOpenApp.setBackgroundColor(0xFF1E88E5);
        btnOpenApp.setTextColor(0xFFFFFFFF);
        btnOpenApp.setTextSize(12f);
        btnOpenApp.setPadding(10, 4, 10, 4);
        btnOpenApp.setOnClickListener(v -> {
            String q = etSearch.getText().toString().trim();
            if (q.isEmpty()) q = currentQuery;
            GoogleMapHelper.openGoogleMaps(MapActivity.this, q);
        });
        LinearLayout.LayoutParams appLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        appLp.setMarginEnd(8);
        btnOpenApp.setLayoutParams(appLp);
        topBar.addView(btnOpenApp);

        Button btnClose = new Button(this);
        btnClose.setText("✕");
        btnClose.setBackgroundColor(0xFF1A3350);
        btnClose.setTextColor(0xFF00D4FF);
        btnClose.setTextSize(14f);
        btnClose.setPadding(10, 4, 10, 4);
        btnClose.setOnClickListener(v -> finish());
        topBar.addView(btnClose);

        root.addView(topBar);

        // WebView with Google Maps
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setGeolocationEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setDatabaseEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setSupportZoom(true);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("geo:") || url.startsWith("intent:"))) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(i);
                        return true;
                    } catch (Exception ignored) {}
                }
                return false;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                if (view != null) {
                    ViewGroup parent = (ViewGroup) view.getParent();
                    if (parent != null) {
                        parent.removeView(view);
                    }
                    view.destroy();
                }
                return true;
            }
        });

        root.addView(webView);
        setContentView(root);

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERM_LOC);
        }

        // Parse extras
        if (getIntent() != null) {
            currentLat = getIntent().getDoubleExtra(EXTRA_LAT, currentLat);
            currentLon = getIntent().getDoubleExtra(EXTRA_LON, currentLon);
            String q = getIntent().getStringExtra(EXTRA_QUERY);
            if (q != null && !q.isEmpty()) {
                currentQuery = q;
                etSearch.setText(q);
            }
        }

        loadGoogleMap(currentQuery, currentLat, currentLon);
    }

    private void loadGoogleMap(String query, double lat, double lon) {
        String url;
        if (query != null && !query.trim().isEmpty()) {
            url = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query.trim());
        } else if (lat != 0 && lon != 0) {
            url = "https://www.google.com/maps?q=" + lat + "," + lon;
        } else {
            url = "https://www.google.com/maps";
        }
        if (webView != null) {
            webView.loadUrl(url);
        }
    }

    private void searchPlace(String query) {
        if (query == null || query.trim().isEmpty()) return;
        currentQuery = query.trim();
        loadGoogleMap(currentQuery, 0, 0);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERM_LOC && webView != null) {
            webView.reload();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
