package com.jarvis.ai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MapActivity — Full Native Google Maps Implementation for H.E.N.R.Y.
 *
 * Features:
 * - Native Google Maps SDK rendering (GoogleMap, SupportMapFragment, CameraUpdateFactory)
 * - Turn-by-turn navigation and direct directions integration
 * - Real-time geocoding search for landmarks, cities, and addresses
 * - Tap-to-inspect reverse geocoding with custom markers
 * - Normal, Satellite, Terrain, and Hybrid map view toggle
 * - Real-time Google Maps live traffic layer toggle
 * - Current GPS location tracking & "My Location" positioning
 * - Interactive Bottom Sheet Info Card with Street View, Directions, and Location Share
 * - Resilient fallback to interactive Google Maps web engine if Play Services are unavailable
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    public static final String EXTRA_QUERY  = "map_query";
    public static final String EXTRA_LAT    = "map_lat";
    public static final String EXTRA_LON    = "map_lon";
    public static final String EXTRA_LABEL  = "map_label";

    private static final int PERM_LOCATION_REQ = 401;

    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;
    private FrameLayout mapContainer;
    private WebView fallbackWebView;

    private EditText etSearch;
    private ProgressBar searchProgress;
    private TextView tvStatusBanner;

    // Bottom Info Card components
    private LinearLayout bottomCard;
    private TextView tvCardTitle;
    private TextView tvCardAddress;
    private TextView tvCardCoords;
    private Button btnDirections;
    private Button btnStreetView;
    private Button btnSatelliteToggle;
    private Button btnShareLocation;

    private Marker currentMarker;
    private double currentLat = 25.2048; // Default Dubai coordinates
    private double currentLon = 55.2708;
    private String currentLabel = "Dubai";
    private String currentQuery = "";

    private boolean isTrafficEnabled = false;
    private int currentMapType = GoogleMap.MAP_TYPE_NORMAL;
    private boolean isNativeMapReady = false;

    // Real-Time LocationManager Tracking
    private JarvisLocationManager jarvisLocationManager;
    private Marker userLiveMarker;
    private Circle userAccuracyCircle;
    private boolean isLiveTrackingMode = false;
    private Button chipLiveGps;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Parse incoming intent extras
        if (getIntent() != null) {
            currentLat = getIntent().getDoubleExtra(EXTRA_LAT, currentLat);
            currentLon = getIntent().getDoubleExtra(EXTRA_LON, currentLon);
            String label = getIntent().getStringExtra(EXTRA_LABEL);
            if (label != null && !label.trim().isEmpty()) {
                currentLabel = label.trim();
            }
            String q = getIntent().getStringExtra(EXTRA_QUERY);
            if (q != null && !q.trim().isEmpty()) {
                currentQuery = q.trim();
                currentLabel = currentQuery;
            }
        }

        // Build Modern Dark Cyber UI Layout
        View contentView = buildUiLayout();
        setContentView(contentView);

        // Initialize Jarvis LocationManager
        jarvisLocationManager = JarvisLocationManager.getInstance(this);

        // Request location permissions if not already granted
        checkLocationPermission();

        // Check Google Play Services availability & API Key validity
        String mapKey = getString(R.string.google_maps_key);
        boolean isPlaceholderKey = mapKey == null || mapKey.contains("AIzaSyHENRY_Google_Maps_Key_Default") || mapKey.trim().isEmpty();

        int playServicesStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (!isPlaceholderKey && playServicesStatus == ConnectionResult.SUCCESS) {
            initNativeGoogleMap();
        } else {
            showStatusBanner("🗺️ Live Web & Satellite Map Engine Active", 3500);
            initFallbackWebView();
        }
    }

    private View buildUiLayout() {
        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(0xFF07152B);

        // ── 1. Top Header & Search Bar ─────────────────────────────────────────
        LinearLayout topHeader = new LinearLayout(this);
        topHeader.setId(View.generateViewId());
        topHeader.setOrientation(LinearLayout.VERTICAL);
        topHeader.setBackgroundColor(0xFF0C1F38);
        topHeader.setPadding(dp(12), dp(10), dp(12), dp(10));
        RelativeLayout.LayoutParams topLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        topHeader.setLayoutParams(topLp);

        // First Row: Back Button, Search Input, Search Action, External App Handoff
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);

        // Back button
        Button btnBack = createStyledButton("←", 0xFF142B47, 0xFF00E5FF, 16);
        btnBack.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnBack.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        backLp.setMarginEnd(dp(8));
        searchRow.addView(btnBack, backLp);

        // Search Input
        etSearch = new EditText(this);
        etSearch.setHint("Search places, cities, coords…");
        etSearch.setHintTextColor(0xFF88A0B8);
        etSearch.setTextColor(0xFFFFFFFF);
        etSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        etSearch.setSingleLine(true);
        etSearch.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(0xFF07152B);
        etBg.setCornerRadius(dp(8));
        etBg.setStroke(dp(1), 0xFF1C3D63);
        etSearch.setBackground(etBg);

        if (!currentQuery.isEmpty()) {
            etSearch.setText(currentQuery);
        }

        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etLp.setMarginEnd(dp(8));
        searchRow.addView(etSearch, etLp);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(etSearch.getText().toString().trim());
            return true;
        });

        // Search Button
        Button btnSearch = createStyledButton("SEARCH", 0xFF00E5FF, 0xFF07152B, 12);
        btnSearch.setTypeface(Typeface.DEFAULT_BOLD);
        btnSearch.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnSearch.setOnClickListener(v -> performSearch(etSearch.getText().toString().trim()));
        LinearLayout.LayoutParams searchBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchBtnLp.setMarginEnd(dp(8));
        searchRow.addView(btnSearch, searchBtnLp);

        // Open in official Google Maps app button
        Button btnOpenApp = createStyledButton("🗺 APP", 0xFF1E88E5, 0xFFFFFFFF, 12);
        btnOpenApp.setPadding(dp(10), dp(8), dp(10), dp(8));
        btnOpenApp.setOnClickListener(v -> {
            String q = etSearch.getText().toString().trim();
            if (q.isEmpty()) q = currentLabel;
            GoogleMapHelper.openGoogleMaps(MapActivity.this, q);
        });
        searchRow.addView(btnOpenApp);

        topHeader.addView(searchRow);

        // Second Row: Quick Filter & Control Chips (Scrollable)
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams chipScrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipScrollLp.topMargin = dp(8);
        chipScroll.setLayoutParams(chipScrollLp);

        LinearLayout chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setGravity(Gravity.CENTER_VERTICAL);

        // Normal Chip
        Button chipDefault = createStyledButton("Default", 0xFF142B47, 0xFF00E5FF, 11);
        chipDefault.setPadding(dp(10), dp(4), dp(10), dp(4));
        chipDefault.setOnClickListener(v -> setMapType(GoogleMap.MAP_TYPE_NORMAL, "Default map view"));
        chipsRow.addView(chipDefault, createChipLp());

        // Satellite Chip
        Button chipSatellite = createStyledButton("Satellite", 0xFF142B47, 0xFF88A0B8, 11);
        chipSatellite.setPadding(dp(10), dp(4), dp(10), dp(4));
        chipSatellite.setOnClickListener(v -> setMapType(GoogleMap.MAP_TYPE_SATELLITE, "Satellite view active"));
        chipsRow.addView(chipSatellite, createChipLp());

        // Terrain Chip
        Button chipTerrain = createStyledButton("Terrain", 0xFF142B47, 0xFF88A0B8, 11);
        chipTerrain.setPadding(dp(10), dp(4), dp(10), dp(4));
        chipTerrain.setOnClickListener(v -> setMapType(GoogleMap.MAP_TYPE_TERRAIN, "Terrain view active"));
        chipsRow.addView(chipTerrain, createChipLp());

        // Traffic Toggle Chip
        Button chipTraffic = createStyledButton("Traffic: OFF", 0xFF142B47, 0xFF88A0B8, 11);
        chipTraffic.setPadding(dp(10), dp(4), dp(10), dp(4));
        chipTraffic.setOnClickListener(v -> {
            isTrafficEnabled = !isTrafficEnabled;
            if (googleMap != null) {
                googleMap.setTrafficEnabled(isTrafficEnabled);
            }
            chipTraffic.setText(isTrafficEnabled ? "Traffic: ON" : "Traffic: OFF");
            chipTraffic.setTextColor(isTrafficEnabled ? 0xFF00E676 : 0xFF88A0B8);
            showStatusBanner(isTrafficEnabled ? "Live Google Maps traffic enabled" : "Traffic overlay disabled", 2500);
        });
        chipsRow.addView(chipTraffic, createChipLp());

        // Current Location Chip
        Button chipLoc = createStyledButton("📍 My Location", 0xFF142B47, 0xFF00E5FF, 11);
        chipLoc.setPadding(dp(10), dp(4), dp(10), dp(4));
        chipLoc.setOnClickListener(v -> moveToCurrentGpsLocation());
        chipsRow.addView(chipLoc, createChipLp());

        // Live GPS Tracking Toggle Chip
        chipLiveGps = createStyledButton("📡 Live GPS: OFF", 0xFF142B47, 0xFF88A0B8, 11);
        chipLiveGps.setPadding(dp(10), dp(4), dp(10), dp(4));
        chipLiveGps.setOnClickListener(v -> toggleLiveGpsTracking());
        chipsRow.addView(chipLiveGps, createChipLp());

        chipScroll.addView(chipsRow);
        topHeader.addView(chipScroll);

        // Search Progress Bar
        searchProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        searchProgress.setIndeterminate(true);
        searchProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
        progLp.topMargin = dp(4);
        topHeader.addView(searchProgress, progLp);

        root.addView(topHeader);

        // ── 2. Center Map View Container ───────────────────────────────────────
        mapContainer = new FrameLayout(this);
        mapContainer.setId(View.generateViewId());
        RelativeLayout.LayoutParams mapLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mapLp.addRule(RelativeLayout.BELOW, topHeader.getId());
        mapContainer.setLayoutParams(mapLp);

        root.addView(mapContainer);

        // Status Toast Banner (floating below header)
        tvStatusBanner = new TextView(this);
        tvStatusBanner.setTextColor(0xFF00E5FF);
        tvStatusBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tvStatusBanner.setPadding(dp(14), dp(6), dp(14), dp(6));
        tvStatusBanner.setGravity(Gravity.CENTER);
        tvStatusBanner.setVisibility(View.GONE);

        GradientDrawable bannerBg = new GradientDrawable();
        bannerBg.setColor(0xE60A1D36);
        bannerBg.setCornerRadius(dp(16));
        bannerBg.setStroke(dp(1), 0xFF00E5FF);
        tvStatusBanner.setBackground(bannerBg);

        RelativeLayout.LayoutParams bannerLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bannerLp.addRule(RelativeLayout.BELOW, topHeader.getId());
        bannerLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        bannerLp.topMargin = dp(12);
        root.addView(tvStatusBanner, bannerLp);

        // ── 3. Bottom Sliding Info Card ────────────────────────────────────────
        bottomCard = new LinearLayout(this);
        bottomCard.setId(View.generateViewId());
        bottomCard.setOrientation(LinearLayout.VERTICAL);
        bottomCard.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xF20B203D);
        cardBg.setCornerRadii(new float[]{dp(16), dp(16), dp(16), dp(16), 0, 0, 0, 0});
        cardBg.setStroke(dp(1), 0xFF1C426E);
        bottomCard.setBackground(cardBg);

        RelativeLayout.LayoutParams cardLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bottomCard.setLayoutParams(cardLp);

        // Drag handle indicator
        View dragBar = new View(this);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(0xFF426588);
        barBg.setCornerRadius(dp(3));
        dragBar.setBackground(barBg);
        LinearLayout.LayoutParams dragLp = new LinearLayout.LayoutParams(dp(40), dp(4));
        dragLp.gravity = Gravity.CENTER_HORIZONTAL;
        dragLp.bottomMargin = dp(10);
        bottomCard.addView(dragBar, dragLp);

        // Place Title
        tvCardTitle = new TextView(this);
        tvCardTitle.setText(currentLabel);
        tvCardTitle.setTextColor(0xFFFFFFFF);
        tvCardTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        tvCardTitle.setTypeface(Typeface.DEFAULT_BOLD);
        bottomCard.addView(tvCardTitle);

        // Address Snippet
        tvCardAddress = new TextView(this);
        tvCardAddress.setText("Tap anywhere on the map or search to inspect places");
        tvCardAddress.setTextColor(0xFF90A8C0);
        tvCardAddress.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        LinearLayout.LayoutParams addrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addrLp.topMargin = dp(2);
        addrLp.bottomMargin = dp(4);
        bottomCard.addView(tvCardAddress, addrLp);

        // Coordinates Text
        tvCardCoords = new TextView(this);
        tvCardCoords.setText(String.format(Locale.US, "Coordinates: %.4f, %.4f", currentLat, currentLon));
        tvCardCoords.setTextColor(0xFF00E5FF);
        tvCardCoords.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        bottomCard.addView(tvCardCoords);

        // Actions Row: Directions, Street View, Satellite, Share
        HorizontalScrollView actionScroll = new HorizontalScrollView(this);
        actionScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams actionScrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionScrollLp.topMargin = dp(12);
        actionScroll.setLayoutParams(actionScrollLp);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        // Directions Button
        btnDirections = createStyledButton("🧭 Directions", 0xFF00E5FF, 0xFF07152B, 13);
        btnDirections.setTypeface(Typeface.DEFAULT_BOLD);
        btnDirections.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnDirections.setOnClickListener(v -> {
            String dest = (tvCardTitle != null && !tvCardTitle.getText().toString().isEmpty())
                    ? tvCardTitle.getText().toString() : (currentLat + "," + currentLon);
            GoogleMapHelper.openGoogleMapsDirections(MapActivity.this, dest);
        });
        actionRow.addView(btnDirections, createChipLp());

        // Street View Button
        btnStreetView = createStyledButton("👁 Street View", 0xFF142B47, 0xFF00E5FF, 13);
        btnStreetView.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnStreetView.setOnClickListener(v -> {
            GoogleMapHelper.openGoogleMapsStreetView(MapActivity.this, currentLat, currentLon);
        });
        actionRow.addView(btnStreetView, createChipLp());

        // Satellite Toggle Button
        btnSatelliteToggle = createStyledButton("🛰 Satellite", 0xFF142B47, 0xFF88A0B8, 13);
        btnSatelliteToggle.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnSatelliteToggle.setOnClickListener(v -> {
            if (currentMapType == GoogleMap.MAP_TYPE_SATELLITE) {
                setMapType(GoogleMap.MAP_TYPE_NORMAL, "Default map view");
                btnSatelliteToggle.setText("🛰 Satellite");
            } else {
                setMapType(GoogleMap.MAP_TYPE_SATELLITE, "Satellite view active");
                btnSatelliteToggle.setText("🗺 Map View");
            }
        });
        actionRow.addView(btnSatelliteToggle, createChipLp());

        // Share Location Button
        btnShareLocation = createStyledButton("📤 Share", 0xFF142B47, 0xFF88A0B8, 13);
        btnShareLocation.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnShareLocation.setOnClickListener(v -> shareCurrentLocation());
        actionRow.addView(btnShareLocation, createChipLp());

        actionScroll.addView(actionRow);
        bottomCard.addView(actionScroll);

        root.addView(bottomCard);

        return root;
    }

    private LinearLayout.LayoutParams createChipLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        return lp;
    }

    private Button createStyledButton(String text, int bgColor, int textColor, int textSizeSp) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(6));
        btn.setBackground(bg);

        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);
        return btn;
    }

    private int dp(int val) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics());
    }

    // ── Native Google Maps Initialization ─────────────────────────────────────

    private void initNativeGoogleMap() {
        try {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(mapContainer.getId(), mapFragment)
                    .commit();

            mapFragment.getMapAsync(this);
        } catch (Throwable t) {
            // Fallback to web map if fragment failed to attach
            initFallbackWebView();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.isNativeMapReady = true;

        // Configure UI & Controls
        try {
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(true);
            googleMap.setMapType(currentMapType);
            googleMap.setOnMapClickListener(this);

            enableMyLocationIfPermitted();

            // Set initial position or search query
            if (!currentQuery.isEmpty()) {
                performSearch(currentQuery);
            } else {
                LatLng initial = new LatLng(currentLat, currentLon);
                updateMarkerPosition(initial, currentLabel, "Target Location");
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initial, 14f));
            }

            showStatusBanner("Google Maps initialized", 2000);
        } catch (Exception e) {
            showStatusBanner("Map configured", 2000);
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        this.currentLat = latLng.latitude;
        this.currentLon = latLng.longitude;

        updateMarkerPosition(latLng, "Inspecting location…", "Resolving address…");
        reverseGeocodeAsync(latLng);
    }

    private void updateMarkerPosition(LatLng latLng, String title, String snippet) {
        if (googleMap == null) return;
        this.currentLat = latLng.latitude;
        this.currentLon = latLng.longitude;

        if (currentMarker != null) {
            currentMarker.remove();
        }

        MarkerOptions opts = new MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        currentMarker = googleMap.addMarker(opts);
        if (currentMarker != null) {
            currentMarker.showInfoWindow();
        }

        // Update card info
        tvCardTitle.setText(title);
        tvCardAddress.setText(snippet);
        tvCardCoords.setText(String.format(Locale.US, "Coordinates: %.4f, %.4f", latLng.latitude, latLng.longitude));
    }

    private void setMapType(int mapType, String statusMsg) {
        this.currentMapType = mapType;
        if (googleMap != null) {
            googleMap.setMapType(mapType);
        }
        showStatusBanner(statusMsg, 2000);
    }

    // ── Geocoding & Search ───────────────────────────────────────────────────

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "Please enter a location or landmark to search", Toast.LENGTH_SHORT).show();
            return;
        }

        this.currentQuery = query.trim();
        searchProgress.setVisibility(View.VISIBLE);
        showStatusBanner("Searching Google Maps for: " + currentQuery, 3000);

        executor.execute(() -> {
            try {
                // Check if query is coordinates "lat, lon"
                if (query.matches("^-?\\d+(\\.\\d+)?,\\s*-?\\d+(\\.\\d+)?$")) {
                    String[] parts = query.split(",");
                    double lat = Double.parseDouble(parts[0].trim());
                    double lon = Double.parseDouble(parts[1].trim());
                    LatLng coords = new LatLng(lat, lon);
                    mainHandler.post(() -> {
                        searchProgress.setVisibility(View.GONE);
                        onSearchResultFound(coords, "Pinned Coordinates", String.format(Locale.US, "%.5f, %.5f", lat, lon));
                    });
                    return;
                }

                // Use Geocoder
                Geocoder geocoder = new Geocoder(MapActivity.this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(currentQuery, 1);

                if (results != null && !results.isEmpty()) {
                    Address address = results.get(0);
                    LatLng foundPos = new LatLng(address.getLatitude(), address.getLongitude());

                    String placeName = address.getFeatureName() != null ? address.getFeatureName() : currentQuery;
                    StringBuilder addressLine = new StringBuilder();
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        if (i > 0) addressLine.append(", ");
                        addressLine.append(address.getAddressLine(i));
                    }
                    String finalAddress = addressLine.length() > 0 ? addressLine.toString() : currentQuery;

                    mainHandler.post(() -> {
                        searchProgress.setVisibility(View.GONE);
                        onSearchResultFound(foundPos, placeName, finalAddress);
                    });
                } else {
                    mainHandler.post(() -> {
                        searchProgress.setVisibility(View.GONE);
                        Toast.makeText(MapActivity.this, "No direct coordinates found for \"" + currentQuery + "\". Showing map area.", Toast.LENGTH_SHORT).show();
                        if (!isNativeMapReady && fallbackWebView != null) {
                            fallbackWebView.loadUrl("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(currentQuery));
                        }
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    searchProgress.setVisibility(View.GONE);
                    if (!isNativeMapReady && fallbackWebView != null) {
                        fallbackWebView.loadUrl("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(currentQuery));
                    }
                });
            }
        });
    }

    private void onSearchResultFound(LatLng pos, String title, String snippet) {
        this.currentLat = pos.latitude;
        this.currentLon = pos.longitude;
        this.currentLabel = title;

        if (googleMap != null && isNativeMapReady) {
            updateMarkerPosition(pos, title, snippet);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        }
        if (fallbackWebView != null) {
            loadInteractiveWebMap(pos.latitude, pos.longitude, title, 15);
        }

        showStatusBanner("Found: " + title, 2500);
    }

    private void reverseGeocodeAsync(LatLng latLng) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(MapActivity.this, Locale.getDefault());
                List<Address> list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (list != null && !list.isEmpty()) {
                    Address addr = list.get(0);
                    String name = addr.getFeatureName() != null ? addr.getFeatureName() : (addr.getLocality() != null ? addr.getLocality() : "Selected Location");
                    StringBuilder line = new StringBuilder();
                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                        if (i > 0) line.append(", ");
                        line.append(addr.getAddressLine(i));
                    }
                    String full = line.length() > 0 ? line.toString() : name;

                    mainHandler.post(() -> {
                        this.currentLabel = name;
                        updateMarkerPosition(latLng, name, full);
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    // ── Current GPS Location Tracking ────────────────────────────────────────

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERM_LOCATION_REQ);
        }
    }

    private void enableMyLocationIfPermitted() {
        if (googleMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private final JarvisLocationManager.LocationUpdateListener locationUpdateListener = new JarvisLocationManager.LocationUpdateListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            handleLiveLocationUpdate(location);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            showStatusBanner("Location provider " + provider + " disabled", 2000);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            showStatusBanner("Location provider " + provider + " active", 2000);
        }
    };

    private void handleLiveLocationUpdate(Location location) {
        if (location == null) return;
        this.currentLat = location.getLatitude();
        this.currentLon = location.getLongitude();
        LatLng pos = new LatLng(currentLat, currentLon);

        if (googleMap != null) {
            // Update or add live user position marker
            if (userLiveMarker == null) {
                userLiveMarker = googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title("Your Current Location")
                        .snippet(String.format(Locale.US, "Accuracy: ±%.1f m", location.getAccuracy()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            } else {
                userLiveMarker.setPosition(pos);
                userLiveMarker.setSnippet(String.format(Locale.US, "Accuracy: ±%.1f m", location.getAccuracy()));
            }

            // Update or add accuracy radius circle
            if (userAccuracyCircle == null) {
                userAccuracyCircle = googleMap.addCircle(new CircleOptions()
                        .center(pos)
                        .radius(Math.max(location.getAccuracy(), 10))
                        .strokeColor(0xFF00E5FF)
                        .strokeWidth(2f)
                        .fillColor(0x2200E5FF));
            } else {
                userAccuracyCircle.setCenter(pos);
                userAccuracyCircle.setRadius(Math.max(location.getAccuracy(), 10));
            }

            // If user is following or live tracking
            if (isLiveTrackingMode) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(pos));
            }
        }

        // Update info card live coordinates
        if (tvCardCoords != null) {
            tvCardCoords.setText(String.format(Locale.US, "Live GPS: %.5f, %.5f (±%.1fm)", currentLat, currentLon, location.getAccuracy()));
        }

        // Reverse geocode address if viewing current location
        if ("My Location".equals(currentLabel) || "Your Current Location".equals(currentLabel)) {
            jarvisLocationManager.getAddressAsync(location, new JarvisLocationManager.AddressCallback() {
                @Override
                public void onAddressResolved(String addressLine, String city, String country) {
                    if (tvCardAddress != null) tvCardAddress.setText(addressLine);
                    if (tvCardTitle != null && city != null && !city.isEmpty()) tvCardTitle.setText(city);
                }

                @Override
                public void onError(String error) {}
            });
        }
    }

    private void toggleLiveGpsTracking() {
        if (!jarvisLocationManager.hasLocationPermission()) {
            checkLocationPermission();
            return;
        }

        isLiveTrackingMode = !isLiveTrackingMode;
        if (isLiveTrackingMode) {
            jarvisLocationManager.startTracking();
            if (chipLiveGps != null) {
                chipLiveGps.setText("📡 Live GPS: ON");
                chipLiveGps.setTextColor(0xFF00E676);
            }
            showStatusBanner("Live GPS Position Tracking Enabled", 2500);

            Location loc = jarvisLocationManager.getLastLocation();
            if (loc != null) {
                handleLiveLocationUpdate(loc);
                if (googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 16.5f));
                }
            } else {
                showStatusBanner("Acquiring high-accuracy GPS fix…", 3000);
            }
        } else {
            if (chipLiveGps != null) {
                chipLiveGps.setText("📡 Live GPS: OFF");
                chipLiveGps.setTextColor(0xFF88A0B8);
            }
            showStatusBanner("Live GPS Tracking Paused", 2000);
        }
    }

    private void moveToCurrentGpsLocation() {
        if (!jarvisLocationManager.hasLocationPermission()) {
            checkLocationPermission();
            return;
        }

        jarvisLocationManager.startTracking();
        Location loc = jarvisLocationManager.getLastLocation();
        if (loc != null) {
            LatLng myPos = new LatLng(loc.getLatitude(), loc.getLongitude());
            this.currentLat = loc.getLatitude();
            this.currentLon = loc.getLongitude();
            this.currentLabel = "My Location";

            handleLiveLocationUpdate(loc);
            if (googleMap != null && isNativeMapReady) {
                updateMarkerPosition(myPos, "My Location", "Current device location");
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myPos, 16.5f));
            } else if (fallbackWebView != null) {
                loadInteractiveWebMap(loc.getLatitude(), loc.getLongitude(), "My Location", 16);
            }
            showStatusBanner("Centered on your GPS position", 2000);
        } else {
            showStatusBanner("Acquiring GPS fix via LocationManager…", 3000);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_LOCATION_REQ && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationIfPermitted();
            moveToCurrentGpsLocation();
        }
    }

    // ── Resilient Interactive Web & Satellite Map Engine ─────────────────────

    private void initFallbackWebView() {
        if (fallbackWebView == null) {
            fallbackWebView = new WebView(this);
            fallbackWebView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            WebSettings ws = fallbackWebView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setGeolocationEnabled(true);
            ws.setAllowFileAccess(true);
            ws.setDatabaseEnabled(true);
            ws.setBuiltInZoomControls(true);
            ws.setDisplayZoomControls(false);
            ws.setSupportZoom(true);
            ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

            fallbackWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                    callback.invoke(origin, true, false);
                }
            });

            fallbackWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url != null && (url.startsWith("geo:") || url.startsWith("intent:") || url.contains("maps.google.com"))) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                            return true;
                        } catch (Exception ignored) {}
                    }
                    return false;
                }
            });

            fallbackWebView.addJavascriptInterface(new Object() {
                @android.webkit.JavascriptInterface
                public void onPinSelected(double lat, double lon, String title) {
                    mainHandler.post(() -> {
                        currentLat = lat;
                        currentLon = lon;
                        currentLabel = title;
                        if (tvCardTitle != null) tvCardTitle.setText(title);
                        if (tvCardAddress != null) tvCardAddress.setText(String.format(Locale.US, "Latitude: %.5f, Longitude: %.5f", lat, lon));
                        if (tvCardCoords != null) tvCardCoords.setText(String.format(Locale.US, "Coordinates: %.5f, %.5f", lat, lon));
                        reverseGeocodeAsync(new LatLng(lat, lon));
                    });
                }
            }, "AndroidMap");
        }

        mapContainer.removeAllViews();
        mapContainer.addView(fallbackWebView);
        loadInteractiveWebMap(currentLat, currentLon, currentLabel, 14);
    }

    private void loadInteractiveWebMap(double lat, double lon, String title, int zoom) {
        if (fallbackWebView == null) return;

        String safeTitle = (title != null && !title.isEmpty()) ? title.replace("'", "\\'").replace("\"", "\\\"") : "Location Pin";
        String html = "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'/>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>"
                + "html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #07152B; } "
                + ".leaflet-popup-content-wrapper { background: #0C1F38; color: #00E5FF; border: 1px solid #00E5FF; border-radius: 8px; font-family: sans-serif; } "
                + ".leaflet-popup-tip { background: #0C1F38; } "
                + ".leaflet-bar a { background-color: #0C1F38 !important; color: #00E5FF !important; border-bottom: 1px solid #142B47 !important; } "
                + "</style></head><body><div id='map'></div><script>"
                + "var map = L.map('map', {zoomControl: true}).setView([" + lat + ", " + lon + "], " + zoom + ");"
                + "var osm = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '© OpenStreetMap' }).addTo(map);"
                + "var satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19, attribution: '© Esri Satellite' });"
                + "var dark = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', { maxZoom: 19, attribution: '© CARTO' });"
                + "L.control.layers({'🗺️ Streets': osm, '🛰️ Satellite': satellite, '🌌 Dark Cyber': dark}, null, {position: 'topright'}).addTo(map);"
                + "var marker = L.marker([" + lat + ", " + lon + "]).addTo(map).bindPopup('<b>" + safeTitle + "</b><br>Lat: " + lat.toFixed(5) + "<br>Lon: " + lon.toFixed(5) + "').openPopup();"
                + "map.on('click', function(e) {"
                + "  marker.setLatLng(e.latlng).bindPopup('<b>Selected Location</b><br>Lat: ' + e.latlng.lat.toFixed(5) + '<br>Lon: ' + e.latlng.lng.toFixed(5)).openPopup();"
                + "  if (window.AndroidMap) window.AndroidMap.onPinSelected(e.latlng.lat, e.latlng.lng, 'Selected Location');"
                + "});"
                + "</script></body></html>";

        fallbackWebView.loadDataWithBaseURL("https://leafletjs.com", html, "text/html", "UTF-8", null);
    }

    // ── Helper Actions ────────────────────────────────────────────────────────

    private void showStatusBanner(String text, long durationMs) {
        if (tvStatusBanner == null) return;
        tvStatusBanner.setText(text);
        tvStatusBanner.setVisibility(View.VISIBLE);
        mainHandler.removeCallbacksAndMessages(tvStatusBanner);
        mainHandler.postDelayed(() -> {
            if (tvStatusBanner != null) tvStatusBanner.setVisibility(View.GONE);
        }, durationMs);
    }

    private void shareCurrentLocation() {
        String mapsUrl = "https://www.google.com/maps?q=" + currentLat + "," + currentLon;
        String shareText = "📍 " + currentLabel + "\n"
                + (tvCardAddress != null ? tvCardAddress.getText().toString() + "\n" : "")
                + "Coordinates: " + currentLat + ", " + currentLon + "\n"
                + mapsUrl;

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share Location via…"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fallbackWebView != null) fallbackWebView.onResume();
        if (jarvisLocationManager != null && jarvisLocationManager.hasLocationPermission()) {
            jarvisLocationManager.addListener(locationUpdateListener);
            jarvisLocationManager.startTracking();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fallbackWebView != null) fallbackWebView.onPause();
        if (jarvisLocationManager != null) {
            jarvisLocationManager.removeListener(locationUpdateListener);
            if (!isLiveTrackingMode) {
                jarvisLocationManager.stopTracking();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (jarvisLocationManager != null) {
            jarvisLocationManager.removeListener(locationUpdateListener);
            jarvisLocationManager.stopTracking();
        }
        executor.shutdown();
        if (fallbackWebView != null) {
            fallbackWebView.loadUrl("about:blank");
            fallbackWebView.destroy();
            fallbackWebView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (fallbackWebView != null && fallbackWebView.canGoBack()) {
            fallbackWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
