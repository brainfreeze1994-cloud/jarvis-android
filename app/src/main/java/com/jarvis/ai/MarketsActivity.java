package com.jarvis.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MarketsActivity extends AppCompatActivity {
    private OkHttpClient client;
    private Handler mainHandler;
    private LinearLayout stocksContainer, cryptoContainer;
    private ProgressBar progress;

    private static final String[][] STOCKS = {
        {"AAPL","Apple"},{"TSLA","Tesla"},{"NVDA","NVIDIA"},
        {"GOOGL","Alphabet"},{"MSFT","Microsoft"},{"AMZN","Amazon"},{"META","Meta"}
    };
    private static final String[][] CRYPTO_IDS = {
        {"bitcoin","BTC"},{"ethereum","ETH"},{"solana","SOL"},
        {"binancecoin","BNB"},{"ripple","XRP"},{"dogecoin","DOGE"}
    };

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_markets);
        client = new OkHttpClient.Builder().connectTimeout(12,TimeUnit.SECONDS).readTimeout(18,TimeUnit.SECONDS).build();
        mainHandler = new Handler(Looper.getMainLooper());
        stocksContainer = findViewById(R.id.stocks_container);
        cryptoContainer = findViewById(R.id.crypto_container);
        progress        = findViewById(R.id.markets_progress);
        TextView btnBack = findViewById(R.id.btn_markets_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        TextView btnRef = findViewById(R.id.btn_markets_refresh);
        if (btnRef != null) btnRef.setOnClickListener(v -> loadAll());
        loadAll();
    }

    private void loadAll() {
        progress.setVisibility(View.VISIBLE);
        mainHandler.post(() -> { stocksContainer.removeAllViews(); cryptoContainer.removeAllViews(); });
        new Thread(() -> { loadStocks(); loadCrypto(); mainHandler.post(() -> progress.setVisibility(View.GONE)); }).start();
    }

    private void loadStocks() {
        for (String[] s : STOCKS) {
            try {
                Request r = new Request.Builder()
                    .url("https://query1.finance.yahoo.com/v8/finance/chart/" + s[0] + "?interval=1d&range=1d")
                    .header("User-Agent","Mozilla/5.0").build();
                try (Response res = client.newCall(r).execute()) {
                    if (!res.isSuccessful() || res.body() == null) continue;
                    JSONObject j    = new JSONObject(res.body().string());
                    JSONObject meta = j.getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONObject("meta");
                    double price    = meta.optDouble("regularMarketPrice",0);
                    double prev     = meta.optDouble("previousClose", meta.optDouble("chartPreviousClose",price));
                    double chg      = prev > 0 ? ((price-prev)/prev*100) : 0;
                    boolean up      = chg >= 0;
                    final String ticker = s[0], name = s[1];
                    final String priceStr = String.format("$%.2f", price);
                    final String chgStr   = String.format("%s%.2f%%", up?"+":"", chg);
                    final int    clr      = up ? 0xFF00C853 : 0xFFC62828;
                    mainHandler.post(() -> addMarketRow(stocksContainer, ticker, name, priceStr, chgStr, clr));
                }
            } catch (Exception ignored) {}
        }
    }

    private void loadCrypto() {
        try {
            StringBuilder ids = new StringBuilder();
            for (String[] c : CRYPTO_IDS) { if (ids.length()>0) ids.append(","); ids.append(c[0]); }
            Request r = new Request.Builder()
                .url("https://api.coingecko.com/api/v3/simple/price?ids="+ids+"&vs_currencies=usd&include_24hr_change=true")
                .build();
            try (Response res = client.newCall(r).execute()) {
                if (!res.isSuccessful() || res.body() == null) return;
                JSONObject j = new JSONObject(res.body().string());
                for (String[] c : CRYPTO_IDS) {
                    JSONObject cd = j.optJSONObject(c[0]);
                    if (cd == null) continue;
                    double price = cd.optDouble("usd",0);
                    double chg   = cd.optDouble("usd_24h_change",0);
                    boolean up   = chg >= 0;
                    String ps    = price >= 1 ? String.format("$%.2f",price) : String.format("$%.6f",price);
                    String cs    = String.format("%s%.2f%%", up?"+":"", chg);
                    int clr      = up ? 0xFF00C853 : 0xFFC62828;
                    final String sym=c[1]; final String fp=ps,fc=cs; final int fcl=clr;
                    mainHandler.post(() -> addMarketRow(cryptoContainer, sym, "Crypto", fp, fc, fcl));
                }
            }
        } catch (Exception ignored) {}
    }

    private void addMarketRow(LinearLayout container, String ticker, String name, String price, String change, int chgClr) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0,18,0,18);
        android.view.ViewGroup.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        android.widget.LinearLayout.LayoutParams llp = new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        left.setLayoutParams(llp);

        TextView tvTicker = new TextView(this); tvTicker.setText(ticker);
        tvTicker.setTextColor(0xFF00D4FF); tvTicker.setTextSize(15f);
        tvTicker.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvName = new TextView(this); tvName.setText(name);
        tvName.setTextColor(0xFF3a7aa0); tvName.setTextSize(12f);

        left.addView(tvTicker); left.addView(tvName);

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(android.view.Gravity.END);
        android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        right.setLayoutParams(rlp);

        TextView tvPrice = new TextView(this); tvPrice.setText(price);
        tvPrice.setTextColor(0xFFc8e8f8); tvPrice.setTextSize(15f);
        tvPrice.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL);

        TextView tvChg = new TextView(this); tvChg.setText(change);
        tvChg.setTextColor(chgClr); tvChg.setTextSize(13f);
        tvChg.setGravity(android.view.Gravity.END);

        right.addView(tvPrice); right.addView(tvChg);

        row.addView(left); row.addView(right);

        View divider = new View(this);
        android.widget.LinearLayout.LayoutParams dp = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(dp);
        divider.setBackgroundColor(0xFF081830);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(row);
        wrap.addView(divider);
        container.addView(wrap);
    }
}
