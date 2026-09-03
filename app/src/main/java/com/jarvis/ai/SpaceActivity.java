package com.jarvis.ai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
 * NASA Eyes on Asteroids, Asteroid Watch, and ISS Space Command Activity.
 * Displays interactive 3D orbital trajectories, live close approaches, real-time countdown timer,
 * and ISS live orbital telemetry.
 */
public class SpaceActivity extends AppCompatActivity {

    private OkHttpClient client;
    private Handler mainHandler;
    private Runnable tickerRunnable;

    private AsteroidOrbitView orbitView;
    private TextView tvHeaderTitle, tvAstPager, tvAstName, tvAstDate, tvAstDist, tvAstSize;
    private TextView tvAstCountdown, tvAstDots, tvLiveClock;
    private TextView btnPrevAst, btnNextAst, btnNasaWeb, btnBack;
    private TextView tabAsteroids, tabAstList, tabIss, tabApod;
    private View cardAsteroidWatch, cardIssPanel, cardApodPanel, panelAsteroidList;
    private TextView btnSeeAllAsteroids, tvAstCountBadge;
    private RecyclerView rvAsteroids;
    private AsteroidAdapter asteroidAdapter;
    private TextView tvIssPos, tvIssDesc, btnSpotStation;
    private TextView tvNasaTitle, tvNasaText;
    private ImageView ivApodImage;
    private ProgressBar pbApod;
    private TextView tvApodDate, tvApodCredit;
    private TextView btnApodHd, btnApodRefresh;
    private String currentApodImageUrl = null;
    private String currentApodHdUrl = null;
    private ProgressBar progress;

    private final List<AsteroidOrbitView.AsteroidOrbital> asteroidList = new ArrayList<>();
    private int currentAsteroidIndex = 0;

    private enum Mode { ASTEROIDS, ASTEROID_LIST, ISS, APOD }
    private Mode currentMode = Mode.ASTEROIDS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_space);

        client = new OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(18, TimeUnit.SECONDS)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupListeners();
        loadInitialData();
        startLiveTicker();
    }

    private void initViews() {
        orbitView        = findViewById(R.id.asteroid_orbit_view);
        tvHeaderTitle    = findViewById(R.id.tv_header_title);
        tvAstPager       = findViewById(R.id.tv_ast_pager);
        tvAstName        = findViewById(R.id.tv_ast_name);
        tvAstDate        = findViewById(R.id.tv_ast_date);
        tvAstDist        = findViewById(R.id.tv_ast_dist);
        tvAstSize        = findViewById(R.id.tv_ast_size);
        tvAstCountdown   = findViewById(R.id.tv_ast_countdown);
        tvAstDots        = findViewById(R.id.tv_ast_dots);
        tvLiveClock      = findViewById(R.id.tv_live_clock);
        btnPrevAst       = findViewById(R.id.btn_prev_ast);
        btnNextAst       = findViewById(R.id.btn_next_ast);
        btnNasaWeb       = findViewById(R.id.btn_nasa_web);
        btnBack          = findViewById(R.id.btn_space_back);

        tabAsteroids     = findViewById(R.id.tab_asteroids);
        tabAstList       = findViewById(R.id.tab_ast_list);
        tabIss           = findViewById(R.id.tab_iss);
        tabApod          = findViewById(R.id.tab_apod);

        cardAsteroidWatch = findViewById(R.id.card_asteroid_watch);
        panelAsteroidList = findViewById(R.id.panel_asteroid_list);
        btnSeeAllAsteroids = findViewById(R.id.btn_see_all_asteroids);
        tvAstCountBadge   = findViewById(R.id.tv_ast_count_badge);
        rvAsteroids       = findViewById(R.id.rv_asteroids);

        cardIssPanel      = findViewById(R.id.card_iss_panel);
        cardApodPanel     = findViewById(R.id.card_apod_panel);

        tvIssPos         = findViewById(R.id.tv_iss_pos);
        tvIssDesc        = findViewById(R.id.tv_iss_desc);
        btnSpotStation   = findViewById(R.id.btn_spot_station);

        tvNasaTitle      = findViewById(R.id.tv_nasa_title);
        tvNasaText       = findViewById(R.id.tv_nasa_text);
        ivApodImage      = findViewById(R.id.iv_apod_image);
        pbApod           = findViewById(R.id.pb_apod);
        tvApodDate       = findViewById(R.id.tv_apod_date);
        tvApodCredit     = findViewById(R.id.tv_apod_credit);
        btnApodHd        = findViewById(R.id.btn_apod_hd);
        btnApodRefresh   = findViewById(R.id.btn_apod_refresh);
        progress         = findViewById(R.id.space_progress);

        if (rvAsteroids != null) {
            rvAsteroids.setLayoutManager(new LinearLayoutManager(this));
            asteroidAdapter = new AsteroidAdapter(asteroidList, (position, asteroid) -> {
                currentAsteroidIndex = position;
                orbitView.setSelectedIndex(position);
                displayAsteroid(position);
                switchMode(Mode.ASTEROIDS);
            });
            rvAsteroids.setAdapter(asteroidAdapter);
        }
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnNasaWeb != null) {
            btnNasaWeb.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://eyes.nasa.gov/apps/asteroids/"));
                startActivity(i);
            });
        }

        if (btnSpotStation != null) {
            btnSpotStation.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://spotthestation.nasa.gov/"));
                startActivity(i);
            });
        }

        if (btnApodHd != null) {
            btnApodHd.setOnClickListener(v -> {
                String target = currentApodHdUrl != null ? currentApodHdUrl : currentApodImageUrl;
                if (target != null && !target.isEmpty()) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
                        startActivity(i);
                    } catch (Exception ignored) {}
                }
            });
        }

        if (ivApodImage != null) {
            ivApodImage.setOnClickListener(v -> {
                if (btnApodHd != null) btnApodHd.performClick();
            });
        }

        if (btnApodRefresh != null) {
            btnApodRefresh.setOnClickListener(v -> {
                if (pbApod != null) pbApod.setVisibility(View.VISIBLE);
                loadNASA();
            });
        }

        if (btnPrevAst != null) {
            btnPrevAst.setOnClickListener(v -> {
                if (asteroidList.isEmpty()) return;
                currentAsteroidIndex = (currentAsteroidIndex - 1 + asteroidList.size()) % asteroidList.size();
                orbitView.setSelectedIndex(currentAsteroidIndex);
                displayAsteroid(currentAsteroidIndex);
            });
        }

        if (btnNextAst != null) {
            btnNextAst.setOnClickListener(v -> {
                if (asteroidList.isEmpty()) return;
                currentAsteroidIndex = (currentAsteroidIndex + 1) % asteroidList.size();
                orbitView.setSelectedIndex(currentAsteroidIndex);
                displayAsteroid(currentAsteroidIndex);
            });
        }

        if (btnSeeAllAsteroids != null) {
            btnSeeAllAsteroids.setOnClickListener(v -> switchMode(Mode.ASTEROID_LIST));
        }

        if (orbitView != null) {
            orbitView.setOnAsteroidSelectedListener((index, asteroid) -> {
                currentAsteroidIndex = index;
                displayAsteroid(index);
            });
        }

        tabAsteroids.setOnClickListener(v -> switchMode(Mode.ASTEROIDS));
        if (tabAstList != null) tabAstList.setOnClickListener(v -> switchMode(Mode.ASTEROID_LIST));
        tabIss.setOnClickListener(v -> switchMode(Mode.ISS));
        tabApod.setOnClickListener(v -> switchMode(Mode.APOD));
    }

    private void switchMode(Mode mode) {
        currentMode = mode;

        tabAsteroids.setTextColor(mode == Mode.ASTEROIDS ? 0xFF00FF99 : 0xFF88AABB);
        tabAsteroids.setBackgroundColor(mode == Mode.ASTEROIDS ? 0x2000FF99 : 0xFF0A1828);

        if (tabAstList != null) {
            tabAstList.setTextColor(mode == Mode.ASTEROID_LIST ? 0xFF00FF99 : 0xFF88AABB);
            tabAstList.setBackgroundColor(mode == Mode.ASTEROID_LIST ? 0x2000FF99 : 0xFF0A1828);
        }

        tabIss.setTextColor(mode == Mode.ISS ? 0xFF00D4FF : 0xFF88AABB);
        tabIss.setBackgroundColor(mode == Mode.ISS ? 0x2000D4FF : 0xFF0A1828);

        tabApod.setTextColor(mode == Mode.APOD ? 0xFFCC88FF : 0xFF88AABB);
        tabApod.setBackgroundColor(mode == Mode.APOD ? 0x20CC88FF : 0xFF0A1828);

        cardAsteroidWatch.setVisibility(mode == Mode.ASTEROIDS ? View.VISIBLE : View.GONE);
        if (panelAsteroidList != null) {
            panelAsteroidList.setVisibility(mode == Mode.ASTEROID_LIST ? View.VISIBLE : View.GONE);
        }
        cardIssPanel.setVisibility(mode == Mode.ISS ? View.VISIBLE : View.GONE);
        cardApodPanel.setVisibility(mode == Mode.APOD ? View.VISIBLE : View.GONE);

        if (mode == Mode.ASTEROIDS) {
            tvHeaderTitle.setText("EYES ON ASTEROIDS");
        } else if (mode == Mode.ASTEROID_LIST) {
            tvHeaderTitle.setText("NEO ASTEROID FEED");
        } else if (mode == Mode.ISS) {
            tvHeaderTitle.setText("ISS SPACE STATION");
            loadISS();
        } else if (mode == Mode.APOD) {
            tvHeaderTitle.setText("NASA DEEP SPACE");
            loadNASA();
        }
    }

    private void loadInitialData() {
        populateDefaultAsteroids();
        fetchLiveAsteroids();
        loadISS();
        loadNASA();
    }

    private void populateDefaultAsteroids() {
        asteroidList.clear();
        long now = System.currentTimeMillis();

        // 1. 2025 QV5 (Closest approach)
        asteroidList.add(new AsteroidOrbitView.AsteroidOrbital(
                "2025 QV5", 1.15, 0.22, 14.2, 450, 0xFF00FF99,
                11.5f, "SEP 2, 2026 3:46:02 PM", 5433910.0, 72340.0, false,
                now + 26 * 3600 * 1000L + 40 * 60 * 1000L + 9000L
        ));

        // 2. 2022 RK
        asteroidList.add(new AsteroidOrbitView.AsteroidOrbital(
                "2022 RK", 1.08, 0.18, 9.5, 410, 0xFF00D4FF,
                38.0f, "SEP 3, 2026 8:12:30 AM", 2150000.0, 54200.0, false,
                now + 48 * 3600 * 1000L + 12 * 60 * 1000L
        ));

        // 3. 2026 OH3
        asteroidList.add(new AsteroidOrbitView.AsteroidOrbital(
                "2026 OH3", 1.32, 0.29, 21.0, 560, 0xFFFFCC00,
                85.0f, "SEP 4, 2026 11:20:00 PM", 6890000.0, 68400.0, false,
                now + 86 * 3600 * 1000L + 20 * 60 * 1000L
        ));

        // 4. 2024 RP12
        asteroidList.add(new AsteroidOrbitView.AsteroidOrbital(
                "2024 RP12", 1.45, 0.35, 17.8, 620, 0xFFFF9944,
                14.2f, "SEP 6, 2026 1:04:15 PM", 3420000.0, 81000.0, false,
                now + 124 * 3600 * 1000L
        ));

        // 5. 99942 Apophis
        asteroidList.add(new AsteroidOrbitView.AsteroidOrbital(
                "99942 Apophis", 0.92, 0.19, 3.3, 323, 0xFFFF4444,
                340.0f, "APR 13, 2029 9:46:00 PM", 31600.0, 107000.0, true,
                now + 820L * 24 * 3600 * 1000L
        ));

        orbitView.setAsteroids(asteroidList);
        if (asteroidAdapter != null) asteroidAdapter.setAsteroids(asteroidList);
        if (tvAstCountBadge != null) tvAstCountBadge.setText(asteroidList.size() + " TRACKED");
        displayAsteroid(0);
    }

    private void displayAsteroid(int index) {
        if (asteroidList.isEmpty() || index < 0 || index >= asteroidList.size()) return;
        AsteroidOrbitView.AsteroidOrbital ast = asteroidList.get(index);

        tvAstPager.setText((index + 1) + " OF " + asteroidList.size());
        tvAstName.setText(ast.name);
        tvAstDate.setText(ast.closeApproachDate);
        tvAstDist.setText(String.format(Locale.US, "%,.0f km", ast.missDistanceKm));
        tvAstSize.setText(String.format(Locale.US, "%.1f m", ast.sizeMeters));

        // Dots indicator
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < asteroidList.size(); i++) {
            if (i == index) dots.append("● ");
            else dots.append("○ ");
        }
        tvAstDots.setText(dots.toString().trim());

        updateCountdownText(ast.approachTimestampMs);
    }

    private void startLiveTicker() {
        tickerRunnable = new Runnable() {
            @Override
            public void run() {
                // Update Live Clock (Matching screenshot format: SEP 01, 2026  01:05:54 pm)
                SimpleDateFormat clockFmt = new SimpleDateFormat("MMM dd, yyyy  hh:mm:ss a", Locale.US);
                if (tvLiveClock != null) {
                    tvLiveClock.setText(clockFmt.format(new Date()));
                }

                // Update Countdown for currently selected asteroid
                if (!asteroidList.isEmpty() && currentAsteroidIndex < asteroidList.size()) {
                    AsteroidOrbitView.AsteroidOrbital ast = asteroidList.get(currentAsteroidIndex);
                    updateCountdownText(ast.approachTimestampMs);
                }

                mainHandler.postDelayed(this, 1000);
            }
        };
        mainHandler.post(tickerRunnable);
    }

    private void updateCountdownText(long targetTimestamp) {
        long diff = targetTimestamp - System.currentTimeMillis();
        if (diff < 0) diff = 0;

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;

        String formatted = String.format(Locale.US, "T -  %02d  :  %02d  :  %02d  :  %02d",
                days, hours, minutes, seconds);
        if (tvAstCountdown != null) {
            tvAstCountdown.setText(formatted);
        }
    }

    private void fetchLiveAsteroids() {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                Request r = new Request.Builder()
                        .url("https://api.nasa.gov/neo/rest/v1/feed?start_date=" + today + "&end_date=" + today + "&api_key=DEMO_KEY")
                        .build();

                try (Response res = client.newCall(r).execute()) {
                    if (res.isSuccessful() && res.body() != null) {
                        JSONObject root = new JSONObject(res.body().string());
                        JSONObject neos = root.optJSONObject("near_earth_objects");
                        if (neos != null) {
                            JSONArray dayArray = neos.optJSONArray(today);
                            if (dayArray != null && dayArray.length() > 0) {
                                List<AsteroidOrbitView.AsteroidOrbital> fetched = new ArrayList<>();
                                SimpleDateFormat parseFmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm", Locale.US);
                                SimpleDateFormat outFmt = new SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.US);

                                for (int i = 0; i < Math.min(5, dayArray.length()); i++) {
                                    JSONObject obj = dayArray.getJSONObject(i);
                                    String name = obj.optString("name", "NEO " + (i + 1));
                                    boolean hazardous = obj.optBoolean("is_potentially_hazardous_asteroid", false);

                                    // Estimated size
                                    JSONObject diam = obj.optJSONObject("estimated_diameter");
                                    float sizeM = 15f;
                                    if (diam != null && diam.has("meters")) {
                                        JSONObject meters = diam.getJSONObject("meters");
                                        double minD = meters.optDouble("estimated_diameter_min", 10.0);
                                        double maxD = meters.optDouble("estimated_diameter_max", 20.0);
                                        sizeM = (float) ((minD + maxD) / 2.0);
                                    }

                                    // Close approach
                                    JSONArray cad = obj.optJSONArray("close_approach_data");
                                    double missKm = 4500000.0;
                                    double velKmh = 65000.0;
                                    String dateStr = "TODAY";
                                    long approachMs = System.currentTimeMillis() + (i + 1) * 86400000L;

                                    if (cad != null && cad.length() > 0) {
                                        JSONObject c0 = cad.getJSONObject(0);
                                        String fullDate = c0.optString("close_approach_date_full", "");
                                        try {
                                            Date d = parseFmt.parse(fullDate);
                                            if (d != null) {
                                                dateStr = outFmt.format(d).toUpperCase(Locale.US);
                                                approachMs = d.getTime();
                                            }
                                        } catch (Exception ignored) {
                                            dateStr = c0.optString("close_approach_date", today);
                                        }

                                        JSONObject missObj = c0.optJSONObject("miss_distance");
                                        if (missObj != null) {
                                            missKm = missObj.optDouble("kilometers", missKm);
                                        }

                                        JSONObject velObj = c0.optJSONObject("relative_velocity");
                                        if (velObj != null) {
                                            velKmh = velObj.optDouble("kilometers_per_hour", velKmh);
                                        }
                                    }

                                    double a = 1.0 + (i * 0.12);
                                    double e = 0.15 + (i * 0.04);
                                    double inc = 8.0 + (i * 3.5);
                                    double period = 380 + (i * 45);
                                    int color = hazardous ? 0xFFFF4444 : (i == 0 ? 0xFF00FF99 : 0xFF00D4FF);

                                    fetched.add(new AsteroidOrbitView.AsteroidOrbital(
                                            name, a, e, inc, period, color, sizeM,
                                            dateStr, missKm, velKmh, hazardous, approachMs
                                    ));
                                }

                                if (!fetched.isEmpty()) {
                                    mainHandler.post(() -> {
                                        asteroidList.clear();
                                        asteroidList.addAll(fetched);
                                        orbitView.setAsteroids(asteroidList);
                                        if (asteroidAdapter != null) asteroidAdapter.setAsteroids(asteroidList);
                                        if (tvAstCountBadge != null) tvAstCountBadge.setText(asteroidList.size() + " TRACKED");
                                        displayAsteroid(0);
                                    });
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback list is already displayed
            } finally {
                mainHandler.post(() -> {
                    if (progress != null) progress.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void loadISS() {
        new Thread(() -> {
            try {
                Request r = new Request.Builder().url("http://api.open-notify.org/iss-now.json").build();
                try (Response res = client.newCall(r).execute()) {
                    if (res.isSuccessful() && res.body() != null) {
                        JSONObject j = new JSONObject(res.body().string());
                        JSONObject pos = j.getJSONObject("iss_position");
                        double lat = Double.parseDouble(pos.getString("latitude"));
                        double lon = Double.parseDouble(pos.getString("longitude"));
                        String txt = String.format(Locale.US, "%.3f° %s   %.3f° %s",
                                Math.abs(lat), (lat >= 0 ? "N" : "S"),
                                Math.abs(lon), (lon >= 0 ? "E" : "W"));
                        mainHandler.post(() -> {
                            if (tvIssPos != null) tvIssPos.setText(txt);
                            if (tvIssDesc != null) {
                                tvIssDesc.setText("Altitude: ~408.2 km • Velocity: 27,600 km/h (7.66 km/s) • Orbit: 92.6 min • 7 crew onboard");
                            }
                        });
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (tvIssPos != null) tvIssPos.setText("45.120° N   12.340° E");
                });
            }
        }).start();
    }

    private void loadNASA() {
        new Thread(() -> {
            try {
                Request r = new Request.Builder().url("https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY").build();
                try (Response res = client.newCall(r).execute()) {
                    if (res.isSuccessful() && res.body() != null) {
                        JSONObject j = new JSONObject(res.body().string());
                        String title = j.optString("title", "NASA Astronomy Picture of the Day");
                        String date = j.optString("date", "TODAY");
                        String credit = j.optString("copyright", "NASA / STScI / ESA");
                        String expl = j.optString("explanation", "").replace("\n", " ");
                        String imgUrl = j.optString("url", null);
                        String hdUrl = j.optString("hdurl", imgUrl);

                        currentApodImageUrl = imgUrl;
                        currentApodHdUrl = hdUrl;

                        mainHandler.post(() -> {
                            if (tvNasaTitle != null) tvNasaTitle.setText(title);
                            if (tvApodDate != null) tvApodDate.setText(date);
                            if (tvApodCredit != null) tvApodCredit.setText("Credit: " + credit);
                            if (tvNasaText != null) tvNasaText.setText(expl);
                        });

                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            fetchAndDisplayApodBitmap(imgUrl);
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Graceful fallback if offline or API rate limit
            loadFallbackAPOD();
        }).start();
    }

    private void fetchAndDisplayApodBitmap(String url) {
        new Thread(() -> {
            try {
                Request imgReq = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                        .build();
                try (Response imgRes = client.newCall(imgReq).execute()) {
                    if (imgRes.isSuccessful() && imgRes.body() != null) {
                        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(imgRes.body().byteStream());
                        if (bmp != null) {
                            mainHandler.post(() -> {
                                if (ivApodImage != null) {
                                    ivApodImage.setImageBitmap(bmp);
                                }
                                if (pbApod != null) pbApod.setVisibility(View.GONE);
                            });
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {}
            mainHandler.post(() -> {
                if (pbApod != null) pbApod.setVisibility(View.GONE);
            });
        }).start();
    }

    private void loadFallbackAPOD() {
        mainHandler.post(() -> {
            String title = "Cosmic Cliffs in the Carina Nebula";
            String date = "JWST DEEP FIELD";
            String credit = "NASA, ESA, CSA, STScI";
            String expl = "Captured in infrared light by NASA's James Webb Space Telescope, this image reveals previously invisible areas of star birth in the Carina Nebula (NGC 3324). Towering gas cliffs reach roughly 7 light-years high, sculpted by intense ultraviolet radiation and stellar winds from massive young stars.";
            currentApodImageUrl = "https://images-assets.nasa.gov/image/PIA25430/PIA25430~orig.jpg";
            currentApodHdUrl = "https://images-assets.nasa.gov/image/PIA25430/PIA25430~orig.jpg";

            if (tvNasaTitle != null) tvNasaTitle.setText(title);
            if (tvApodDate != null) tvApodDate.setText(date);
            if (tvApodCredit != null) tvApodCredit.setText("Credit: " + credit);
            if (tvNasaText != null) tvNasaText.setText(expl);

            fetchAndDisplayApodBitmap(currentApodImageUrl);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tickerRunnable != null) {
            mainHandler.removeCallbacks(tickerRunnable);
        }
    }
}
