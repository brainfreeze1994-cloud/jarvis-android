package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Live Prices — stocks, crypto, forex.
 * Uses:
 *   Crypto:  CoinGecko API (free, no key)
 *   Forex:   ExchangeRate-API open (free, no key)
 *   Stocks:  Yahoo Finance unofficial endpoint (free, no key)
 */
public class LivePrices {

    // ── Crypto ID map ─────────────────────────────────────────────────────────
    private static final Map<String, String> CRYPTO_IDS = new HashMap<String, String>() {{
        put("bitcoin",      "bitcoin");    put("btc",         "bitcoin");
        put("ethereum",     "ethereum");   put("eth",         "ethereum");
        put("solana",       "solana");     put("sol",         "solana");
        put("cardano",      "cardano");    put("ada",         "cardano");
        put("xrp",          "ripple");     put("ripple",      "ripple");
        put("dogecoin",     "dogecoin");   put("doge",        "dogecoin");
        put("polkadot",     "polkadot");   put("dot",         "polkadot");
        put("shiba",        "shiba-inu");  put("shib",        "shiba-inu");
        put("tron",         "tron");       put("trx",         "tron");
        put("litecoin",     "litecoin");   put("ltc",         "litecoin");
        put("chainlink",    "chainlink");  put("link",        "chainlink");
        put("bnb",          "binancecoin");put("binance",     "binancecoin");
        put("usdt",         "tether");     put("tether",      "tether");
        put("usdc",         "usd-coin");
    }};

    // ── Forex pairs ───────────────────────────────────────────────────────────
    private static final String[][] FOREX_PAIRS = {
        {"usd to aed", "USD", "AED"}, {"aed to usd", "AED", "USD"},
        {"usd to eur", "USD", "EUR"}, {"eur to usd", "EUR", "USD"},
        {"usd to gbp", "USD", "GBP"}, {"gbp to usd", "GBP", "USD"},
        {"usd to php", "USD", "PHP"}, {"php to usd", "PHP", "USD"},
        {"usd to inr", "USD", "INR"}, {"inr to usd", "INR", "USD"},
        {"usd to jpy", "USD", "JPY"}, {"gbp to aed", "GBP", "AED"},
        {"eur to aed", "EUR", "AED"}, {"usd to cad", "USD", "CAD"},
        {"usd to aud", "USD", "AUD"}, {"usd to sgd", "USD", "SGD"},
    };

    // ── Detection ─────────────────────────────────────────────────────────────
    public static boolean isPriceQuery(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("price") || lower.contains("stock") || lower.contains("crypto") ||
               lower.contains("bitcoin") || lower.contains("ethereum") || lower.contains("btc") ||
               lower.contains("eth") || lower.contains("doge") || lower.contains("solana") ||
               lower.contains(" to aed") || lower.contains(" to usd") || lower.contains(" to eur") ||
               lower.contains("exchange rate") || lower.contains("currency") ||
               lower.contains("how much is") || lower.contains("what's the rate") ||
               lower.contains("market") || lower.contains("shares") || lower.contains("ticker");
    }

    public interface Callback {
        void onResult(String formatted);
        void onError(String reason);
    }

    public static void query(String text, Callback cb) {
        String lower = text.toLowerCase(Locale.US);

        // 1. Crypto
        String cryptoId = detectCrypto(lower);
        if (cryptoId != null) { fetchCrypto(cryptoId, detectCoinName(lower), cb); return; }

        // 2. Forex
        String[] forexPair = detectForex(lower);
        if (forexPair != null) { fetchForex(forexPair[0], forexPair[1], cb); return; }

        // 3. Stock — extract ticker or company name
        String ticker = detectStock(lower, text);
        if (ticker != null) { fetchStock(ticker, cb); return; }

        cb.onError("[EMOTION:neutral] I couldn't identify what price you're after, sir. Try 'Bitcoin price' or 'USD to AED'.");
    }

    // ── Crypto ────────────────────────────────────────────────────────────────
    private static String detectCrypto(String lower) {
        for (Map.Entry<String, String> e : CRYPTO_IDS.entrySet())
            if (lower.contains(e.getKey())) return e.getValue();
        return null;
    }

    private static String detectCoinName(String lower) {
        for (Map.Entry<String, String> e : CRYPTO_IDS.entrySet())
            if (lower.contains(e.getKey())) return e.getKey().substring(0,1).toUpperCase() + e.getKey().substring(1);
        return "Crypto";
    }

    private static void fetchCrypto(String id, String name, Callback cb) {
        new Thread(() -> {
            try {
                String rawUrl = "https://api.coingecko.com/api/v3/simple/price" +
                    "?ids=" + id + "&vs_currencies=usd,aed&include_24hr_change=true";
                JSONObject json = httpGet(rawUrl);
                JSONObject coin = json.optJSONObject(id);
                if (coin == null) { cb.onError("[EMOTION:neutral] Price not available right now, sir."); return; }
                double usd    = coin.optDouble("usd", 0);
                double aed    = coin.optDouble("aed", 0);
                double chg24h = coin.optDouble("usd_24h_change", 0);
                String dir    = chg24h >= 0 ? "▲" : "▼";
                String emotion = chg24h >= 5 ? "excited" : chg24h <= -5 ? "concerned" : "neutral";
                String result = String.format(Locale.US,
                    "[EMOTION:%s] **%s** live price:\n" +
                    "USD: **$%,.2f**  |  AED: **%,.2f**\n" +
                    "24h change: %s **%.2f%%**, sir.",
                    emotion, name, usd, aed, dir, Math.abs(chg24h));
                cb.onResult(result);
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Couldn't fetch crypto price: " + e.getMessage());
            }
        }).start();
    }

    // ── Forex ─────────────────────────────────────────────────────────────────
    private static String[] detectForex(String lower) {
        for (String[] pair : FOREX_PAIRS)
            if (lower.contains(pair[0])) return new String[]{pair[1], pair[2]};
        // generic "rate from X to Y"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "([a-z]{3})\\s+to\\s+([a-z]{3})").matcher(lower);
        if (m.find()) return new String[]{m.group(1).toUpperCase(), m.group(2).toUpperCase()};
        return null;
    }

    private static void fetchForex(String from, String to, Callback cb) {
        new Thread(() -> {
            try {
                String rawUrl = "https://open.er-api.com/v6/latest/" + from;
                JSONObject json = json = httpGet(rawUrl);
                JSONObject rates = json.optJSONObject("rates");
                if (rates == null) { cb.onError("[EMOTION:neutral] Rate unavailable, sir."); return; }
                double rate = rates.optDouble(to, -1);
                if (rate < 0) { cb.onError("[EMOTION:neutral] Currency pair not found, sir."); return; }
                cb.onResult(String.format(Locale.US,
                    "[EMOTION:neutral] **1 %s = %.4f %s** as of now, sir.", from, rate, to));
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Couldn't fetch exchange rate: " + e.getMessage());
            }
        }).start();
    }

    // ── Stock ─────────────────────────────────────────────────────────────────
    private static final Map<String, String> KNOWN_TICKERS = new HashMap<String, String>() {{
        put("apple",     "AAPL"); put("microsoft", "MSFT"); put("google",    "GOOGL");
        put("amazon",    "AMZN"); put("tesla",     "TSLA"); put("meta",      "META");
        put("facebook",  "META"); put("nvidia",    "NVDA"); put("netflix",   "NFLX");
        put("twitter",   "TWTR"); put("x corp",    "TWTR"); put("samsung",   "005930.KS");
        put("alibaba",   "BABA"); put("toyota",    "TM");   put("disney",    "DIS");
        put("boeing",    "BA");   put("intel",     "INTC"); put("amd",       "AMD");
        put("paypal",    "PYPL"); put("uber",      "UBER"); put("airbnb",    "ABNB");
        put("shopify",   "SHOP"); put("snapchat",  "SNAP"); put("twitter x", "TWTR");
    }};

    private static String detectStock(String lower, String original) {
        for (Map.Entry<String, String> e : KNOWN_TICKERS.entrySet())
            if (lower.contains(e.getKey())) return e.getValue();
        // Look for uppercase ticker like "AAPL stock" or "stock TSLA"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "\\b([A-Z]{2,5})\\b").matcher(original);
        while (m.find()) {
            String candidate = m.group(1);
            // Avoid common non-ticker uppercase words
            if (!candidate.matches("(I|A|THE|MY|AN|AT|IN|ON|TO|DO|GO|UP)"))
                return candidate;
        }
        return null;
    }

    private static void fetchStock(String ticker, Callback cb) {
        new Thread(() -> {
            try {
                String rawUrl = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker
                    + "?interval=1d&range=1d";
                JSONObject json = httpGet(rawUrl);
                JSONObject chart = json.optJSONObject("chart");
                if (chart == null) { cb.onError("[EMOTION:neutral] Stock data unavailable, sir."); return; }
                JSONArray results = chart.optJSONArray("result");
                if (results == null || results.length() == 0) {
                    cb.onError("[EMOTION:neutral] No data for ticker **" + ticker + "**, sir."); return;
                }
                JSONObject meta = results.getJSONObject(0).optJSONObject("meta");
                if (meta == null) { cb.onError("[EMOTION:neutral] Metadata missing, sir."); return; }
                double price  = meta.optDouble("regularMarketPrice", 0);
                double prev   = meta.optDouble("previousClose", 0);
                double chg    = price - prev;
                double chgPct = prev > 0 ? (chg / prev) * 100 : 0;
                String dir    = chg >= 0 ? "▲" : "▼";
                String cur    = meta.optString("currency", "USD");
                String name   = meta.optString("shortName", ticker);
                String emotion = chgPct >= 3 ? "excited" : chgPct <= -3 ? "concerned" : "neutral";
                cb.onResult(String.format(Locale.US,
                    "[EMOTION:%s] **%s** (%s):\n" +
                    "Price: **%s %.2f**\n" +
                    "Change: %s **%.2f (%.2f%%)** today, sir.",
                    emotion, name, ticker, cur, price, dir, Math.abs(chg), Math.abs(chgPct)));
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Couldn't fetch stock price: " + e.getMessage());
            }
        }).start();
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────
    private static JSONObject httpGet(String rawUrl) throws Exception {
        URL url = new URL(rawUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000); conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept", "application/json");
        conn.connect();
        InputStream is = conn.getInputStream();
        byte[] buf = new byte[131072]; int read;
        StringBuilder sb = new StringBuilder();
        while ((read = is.read(buf)) != -1) sb.append(new String(buf, 0, read, "UTF-8"));
        is.close();
        return new JSONObject(sb.toString());
    }
}
