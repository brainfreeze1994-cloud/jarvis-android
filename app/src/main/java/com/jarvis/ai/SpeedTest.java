package com.jarvis.ai;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Locale;

/**
 * Speed Test — measures download speed, ping, and connection type.
 * Uses a small public test file. No API key needed.
 * "Test my internet speed", "how fast is my wifi", "ping test"
 */
public class SpeedTest {

    public interface Callback {
        void onResult(String formatted);
        void onError(String reason);
    }

    // ~5 MB test file from a reliable CDN
    private static final String TEST_URL_PRIMARY   = "https://speed.cloudflare.com/__down?bytes=5000000";
    private static final String TEST_URL_FALLBACK  = "https://httpbin.org/bytes/2000000";

    public static boolean isSpeedTestCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("speed test") || lower.contains("internet speed") ||
               lower.contains("wifi speed") || lower.contains("how fast") && lower.contains("internet") ||
               lower.contains("how fast") && lower.contains("wifi") ||
               lower.contains("ping test") || lower.contains("test my connection") ||
               lower.contains("network speed") || lower.contains("bandwidth") ||
               lower.contains("connection speed") || lower.contains("is my internet fast");
    }

    public static void run(Context ctx, Callback cb) {
        new Thread(() -> {
            try {
                // 1. Connection type
                String connType = getConnectionType(ctx);

                // 2. Ping
                long pingMs = measurePing("8.8.8.8");

                // 3. Download speed
                double mbps = measureDownload(TEST_URL_PRIMARY);
                if (mbps < 0) mbps = measureDownload(TEST_URL_FALLBACK);

                // 4. Rating
                String quality, emotion;
                if      (mbps >= 100) { quality = "Blazing fast 🚀";  emotion = "proud";    }
                else if (mbps >= 50)  { quality = "Excellent ✅";      emotion = "excited";  }
                else if (mbps >= 25)  { quality = "Good 👍";           emotion = "excited";  }
                else if (mbps >= 10)  { quality = "Average ⚠️";        emotion = "neutral";  }
                else if (mbps >= 3)   { quality = "Slow 🐢";           emotion = "concerned";}
                else if (mbps >= 0)   { quality = "Very slow 🐌";      emotion = "concerned";}
                else                  { quality = "Unable to measure"; emotion = "concerned";}

                String pingLabel = pingMs < 0 ? "N/A" : pingMs + " ms";
                String pingQuality = pingMs < 0 ? "" : pingMs < 30 ? " (excellent)" :
                                     pingMs < 80 ? " (good)" : pingMs < 200 ? " (average)" : " (high)";

                String result = String.format(Locale.US,
                    "[EMOTION:%s] **Internet Speed Test, sir:**\n\n" +
                    "📶 Connection: **%s**\n" +
                    "⬇️ Download: **%.1f Mbps** — %s\n" +
                    "🏓 Ping: **%s**%s",
                    emotion, connType, Math.max(0, mbps), quality, pingLabel, pingQuality);

                cb.onResult(result);
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Speed test failed: " + e.getMessage() + ", sir.");
            }
        }).start();
    }

    private static String getConnectionType(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "Unknown";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network net = cm.getActiveNetwork();
            if (net == null) return "No connection";
            NetworkCapabilities nc = cm.getNetworkCapabilities(net);
            if (nc == null) return "Unknown";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))     return "Wi-Fi";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobile data";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
            return "Other";
        } else {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni == null || !ni.isConnected()) return "No connection";
            return ni.getTypeName();
        }
    }

    private static long measurePing(String host) {
        try {
            long start = System.currentTimeMillis();
            InetAddress.getByName(host);
            return System.currentTimeMillis() - start;
        } catch (Exception e) { return -1; }
    }

    private static double measureDownload(String testUrl) {
        try {
            URL url = new URL(testUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", "HENRY-SpeedTest/1.0");
            conn.connect();

            InputStream is = conn.getInputStream();
            byte[] buf = new byte[65536];
            long bytesRead = 0;
            long start = System.currentTimeMillis();
            int read;
            long maxDuration = 8000; // 8 seconds max

            while ((read = is.read(buf)) != -1) {
                bytesRead += read;
                if (System.currentTimeMillis() - start > maxDuration) break;
            }
            is.close();

            long elapsed = System.currentTimeMillis() - start;
            if (elapsed <= 0 || bytesRead <= 0) return -1;

            // Convert bytes/ms → Mbps
            return (bytesRead * 8.0) / (elapsed * 1000.0);
        } catch (Exception e) { return -1; }
    }
}
