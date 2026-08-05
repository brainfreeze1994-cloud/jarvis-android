package com.jarvis.ai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FlightActivity extends AppCompatActivity {

    private static final String API_URL  = "https://jarvis-ai-seven-dun.vercel.app/api/jarvis";
    private static final String SKY_URL  = "https://opensky-network.org/api/states/all";

    private EditText     etFlightNum;
    private TextView     tvStatus, tvCallsign, tvRoute, tvAltitude, tvSpeed,
                         tvPosition, tvCountry, tvResult, tvBtnSearch, tvBtnMap,
                         tvBtnBack, tvBtnRefresh;
    private LinearLayout cardFlight, cardAi;
    private ProgressBar  progressBar;
    private OkHttpClient client;
    private Handler      mainHandler;
    private String       lastFlightNum = "";
    private double       lastLat = 0, lastLon = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight);

        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());

        etFlightNum   = findViewById(R.id.et_flight_num);
        tvStatus      = findViewById(R.id.tv_flight_status);
        tvCallsign    = findViewById(R.id.tv_callsign);
        tvRoute       = findViewById(R.id.tv_route);
        tvAltitude    = findViewById(R.id.tv_altitude);
        tvSpeed       = findViewById(R.id.tv_speed);
        tvPosition    = findViewById(R.id.tv_position);
        tvCountry     = findViewById(R.id.tv_country);
        tvResult      = findViewById(R.id.tv_result);
        cardFlight    = findViewById(R.id.card_flight);
        cardAi        = findViewById(R.id.card_ai);
        progressBar   = findViewById(R.id.progress_bar);
        tvBtnSearch   = findViewById(R.id.btn_search);
        tvBtnMap      = findViewById(R.id.btn_open_map);
        // Also wire the AI card's map button if visible
        TextView tvBtnMapAi = findViewById(R.id.btn_open_map_ai);
        if (tvBtnMapAi != null) tvBtnMapAi.setOnClickListener(v -> tvBtnMap.performClick());
        tvBtnBack     = findViewById(R.id.btn_back);
        tvBtnRefresh  = findViewById(R.id.btn_refresh);

        tvBtnBack.setOnClickListener(v -> finish());
        tvBtnSearch.setOnClickListener(v -> doSearch());
        tvBtnRefresh.setOnClickListener(v -> {
            if (!lastFlightNum.isEmpty()) searchFlight(lastFlightNum);
        });
        tvBtnMap.setOnClickListener(v -> {
            String url = lastLat != 0
                    ? "https://www.flightradar24.com/?lat=" + lastLat + "&lon=" + lastLon + "&zoom=7"
                    : "https://www.flightradar24.com/";
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        etFlightNum.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE) {
                doSearch();
                return true;
            }
            return false;
        });

        // Pre-loaded flight number from intent (voice command or topbar button)
        String preloaded = getIntent().getStringExtra("flight_number");
        if (preloaded != null && !preloaded.isEmpty()) {
            etFlightNum.setText(preloaded.toUpperCase());
            searchFlight(preloaded.toUpperCase());
        }
    }

    private void doSearch() {
        String num = etFlightNum.getText().toString().trim().toUpperCase()
                .replaceAll("\\s+", "");
        if (TextUtils.isEmpty(num)) {
            Toast.makeText(this, "Enter a flight number (e.g. EK001)", Toast.LENGTH_SHORT).show();
            return;
        }
        searchFlight(num);
    }

    private void searchFlight(final String flightNum) {
        lastFlightNum = flightNum;
        showLoading(true);
        cardFlight.setVisibility(View.GONE);
        cardAi.setVisibility(View.GONE);
        tvBtnRefresh.setVisibility(View.GONE);
        tvBtnMap.setVisibility(View.GONE);

        new Thread(() -> {
            // ── Step 1: OpenSky live position ──────────────────────────
            boolean foundLive = false;
            try {
                Request skyReq = new Request.Builder().url(SKY_URL).build();
                try (Response skyRes = client.newCall(skyReq).execute()) {
                    if (skyRes.isSuccessful() && skyRes.body() != null) {
                        JSONObject json   = new JSONObject(skyRes.body().string());
                        JSONArray  states = json.optJSONArray("states");
                        if (states != null) {
                            for (int i = 0; i < states.length(); i++) {
                                JSONArray s        = states.getJSONArray(i);
                                String    callsign = s.optString(1, "").trim().toUpperCase()
                                        .replaceAll("\\s+", "");
                                if (callsign.contains(flightNum) ||
                                        flightNum.contains(callsign.replaceAll("[^A-Z0-9]", ""))) {

                                    String  country  = s.optString(2, "Unknown");
                                    boolean onGround = s.optBoolean(8, false);
                                    double  lat      = s.optDouble(6, 0);
                                    double  lon      = s.optDouble(5, 0);
                                    double  alt      = s.optDouble(7, 0);
                                    double  speed    = s.optDouble(9, 0);
                                    double  heading  = s.optDouble(10, 0);
                                    lastLat = lat;
                                    lastLon = lon;

                                    String statusTxt  = onGround ? "ON GROUND" : "✈ EN ROUTE";
                                    int    statusClr  = onGround ? 0xFFFFC107 : 0xFF00D4FF;
                                    String altStr     = onGround ? "0 m (on ground)"
                                            : String.format("%.0f m  (%.0f ft)", alt, alt * 3.281);
                                    String spdStr     = String.format("%.0f km/h", speed * 3.6);
                                    String posStr     = String.format("%.3f°N,  %.3f°E", lat, lon);
                                    String hdgStr     = String.format("  HDG %.0f°", heading);

                                    final String fStatus   = statusTxt;
                                    final int    fClr      = statusClr;
                                    final String fCallsign = "✈ " + callsign;
                                    final String fCountry  = "🌍 Origin: " + country;
                                    final String fAlt      = altStr;
                                    final String fSpd      = spdStr + hdgStr;
                                    final String fPos      = posStr;

                                    mainHandler.post(() -> {
                                        showLoading(false);
                                        cardFlight.setVisibility(View.VISIBLE);
                                        tvBtnRefresh.setVisibility(View.VISIBLE);
                                        tvBtnMap.setVisibility(View.VISIBLE);
                                        tvStatus.setText(fStatus);
                                        tvStatus.setTextColor(fClr);
                                        tvCallsign.setText(fCallsign);
                                        tvCountry.setText(fCountry);
                                        tvRoute.setText("Real-time data • OpenSky Network");
                                        tvAltitude.setText(fAlt);
                                        tvSpeed.setText(fSpd);
                                        tvPosition.setText(fPos);
                                    });
                                    foundLive = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (foundLive) return;

            // ── Step 2: Ask HENRY AI ───────────────────────────────────
            try {
                JSONObject reqBody = new JSONObject();
                JSONArray  msgs    = new JSONArray();
                JSONObject msg     = new JSONObject();
                msg.put("role", "user");
                msg.put("text",
                        "Track flight " + flightNum + ". Provide airline name, origin airport, " +
                        "destination airport, scheduled departure & arrival times, current status " +
                        "(on time/delayed/cancelled), and any useful travel tips. " +
                        "If live data isn't available, give your best knowledge and suggest " +
                        "flightradar24.com or flightaware.com for live tracking.");
                msgs.put(msg);
                reqBody.put("messages", msgs);
                reqBody.put("queryType", "flight");
                reqBody.put("responseMode", "balanced");

                RequestBody body = RequestBody.create(
                        reqBody.toString(), MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder().url(API_URL).post(body).build();

                try (Response res = client.newCall(req).execute()) {
                    if (res.body() != null) {
                        JSONObject resp  = new JSONObject(res.body().string());
                        String     reply = resp.optString("reply", "No data available.");
                        reply = reply.replaceAll("\\[EMOTION:[a-z]+\\]\\s*", "").trim();
                        final String finalReply = reply;
                        mainHandler.post(() -> {
                            showLoading(false);
                            cardAi.setVisibility(View.VISIBLE);
                            tvBtnMap.setVisibility(View.VISIBLE);
                            tvBtnRefresh.setVisibility(View.VISIBLE);
                            tvResult.setText(finalReply);
                        });
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    cardAi.setVisibility(View.VISIBLE);
                    tvResult.setText("Could not retrieve data for " + flightNum +
                            ".\n\nCheck:\n• flightradar24.com\n• flightaware.com\n• radarbox.com");
                });
            }
        }).start();
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
