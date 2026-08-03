package com.jarvis.ai;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live Currency Converter using ExchangeRate-API open endpoint (free, no key).
 * "Convert 100 USD to AED"
 * "How much is 500 EUR in GBP?"
 * "50 AED to USD"
 * Supports 170+ currencies.
 */
public class CurrencyConverter {

    public interface Callback {
        void onResult(String formatted);
        void onError(String reason);
    }

    // Common currency name aliases
    private static final Map<String, String> NAME_MAP = new HashMap<String, String>() {{
        put("dollar",       "USD"); put("dollars",      "USD"); put("usd",          "USD");
        put("euro",         "EUR"); put("euros",         "EUR"); put("eur",          "EUR");
        put("pound",        "GBP"); put("pounds",        "GBP"); put("gbp",          "GBP");
        put("dirham",       "AED"); put("dirhams",       "AED"); put("aed",          "AED");
        put("peso",         "PHP"); put("pesos",         "PHP"); put("php",          "PHP");
        put("rupee",        "INR"); put("rupees",        "INR"); put("inr",          "INR");
        put("riyal",        "SAR"); put("riyals",        "SAR"); put("sar",          "SAR");
        put("yen",          "JPY"); put("jpy",           "JPY");
        put("won",          "KRW"); put("krw",           "KRW");
        put("yuan",         "CNY"); put("renminbi",      "CNY"); put("cny",          "CNY");
        put("franc",        "CHF"); put("francs",        "CHF"); put("chf",          "CHF");
        put("lira",         "TRY"); put("try",           "TRY");
        put("kroner",       "DKK"); put("dkk",           "DKK");
        put("ruble",        "RUB"); put("rubles",        "RUB"); put("rub",          "RUB");
        put("real",         "BRL"); put("reais",         "BRL"); put("brl",          "BRL");
        put("dinar",        "KWD"); put("dinars",        "KWD"); put("kwd",          "KWD");
        put("ringgit",      "MYR"); put("myr",           "MYR");
        put("baht",         "THB"); put("thb",           "THB");
        put("singapore",    "SGD"); put("sgd",           "SGD");
        put("canadian",     "CAD"); put("cad",           "CAD");
        put("australian",   "AUD"); put("aud",           "AUD");
        put("qatari",       "QAR"); put("qar",           "QAR");
        put("omani",        "OMR"); put("omr",           "OMR");
        put("bahraini",     "BHD"); put("bhd",           "BHD");
    }};

    public static boolean isCurrencyCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.contains("convert") || lower.contains("how much is") ||
                lower.contains("exchange") || lower.contains("change")) &&
               hasCurrencyKeyword(lower);
    }

    private static boolean hasCurrencyKeyword(String lower) {
        for (String key : NAME_MAP.keySet())
            if (lower.contains(key)) return true;
        // 3-letter code pattern
        return lower.matches(".*\\b[a-z]{3}\\s+to\\s+[a-z]{3}\\b.*");
    }

    /** Returns {amount, fromCode, toCode} or null */
    public static String[] parse(String text) {
        String lower = text.toLowerCase(Locale.US);

        // Pattern: "100 USD to AED" or "convert 100 dollars to dirhams"
        Matcher m = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s+([\\w]+)\\s+(?:to|in|into)\\s+([\\w]+)",
            Pattern.CASE_INSENSITIVE).matcher(lower);
        if (m.find()) {
            String amount  = m.group(1).replace(",", "");
            String fromRaw = m.group(2);
            String toRaw   = m.group(3);
            String from    = resolveCurrency(fromRaw);
            String to      = resolveCurrency(toRaw);
            if (from != null && to != null)
                return new String[]{amount, from, to};
        }

        // Pattern: "how much is 500 EUR in USD"
        Matcher m2 = Pattern.compile(
            "(?:how much is)?\\s*(\\d+(?:[.,]\\d+)?)\\s+([\\w]+)\\s+(?:in|to|into)\\s+([\\w]+)",
            Pattern.CASE_INSENSITIVE).matcher(lower);
        if (m2.find()) {
            String amount  = m2.group(1).replace(",", "");
            String from    = resolveCurrency(m2.group(2));
            String to      = resolveCurrency(m2.group(3));
            if (from != null && to != null)
                return new String[]{amount, from, to};
        }
        return null;
    }

    private static String resolveCurrency(String raw) {
        if (raw == null) return null;
        String lower = raw.toLowerCase(Locale.US).trim();
        // Direct 3-letter code
        if (lower.matches("[a-z]{3}")) return lower.toUpperCase(Locale.US);
        // Name lookup
        return NAME_MAP.get(lower);
    }

    public static void convert(double amount, String from, String to, Callback cb) {
        new Thread(() -> {
            try {
                String urlStr = "https://open.er-api.com/v6/latest/" + from;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000); conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "HENRY-AI/1.0");
                conn.connect();

                StringBuilder sb = new StringBuilder();
                InputStream is = conn.getInputStream();
                byte[] buf = new byte[32768]; int read;
                while ((read = is.read(buf)) != -1) sb.append(new String(buf, 0, read, "UTF-8"));
                is.close();

                JSONObject json  = new JSONObject(sb.toString());
                JSONObject rates = json.optJSONObject("rates");
                if (rates == null || !rates.has(to)) {
                    cb.onError("[EMOTION:neutral] Rate for **" + to + "** not available, sir.");
                    return;
                }
                double rate   = rates.getDouble(to);
                double result = amount * rate;
                String emotion = "neutral";
                cb.onResult(String.format(Locale.US,
                    "[EMOTION:%s] 💱 **%.2f %s = %.2f %s**\nRate: 1 %s = %.4f %s, sir.",
                    emotion, amount, from, result, to, from, rate, to));
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Conversion failed: " + e.getMessage());
            }
        }).start();
    }
}
