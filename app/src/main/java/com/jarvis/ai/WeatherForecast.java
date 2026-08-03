package com.jarvis.ai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 7-day weather forecast using Open-Meteo (free, no API key).
 * Also handles current weather queries.
 */
public class WeatherForecast {

    public interface Callback {
        void onResult(String formatted);
        void onError(String reason);
    }

    // WMO weather code descriptions
    private static final String[] WMO_DESC = {
        "Clear sky", "Mainly clear", "Partly cloudy", "Overcast",
        "Fog", "Icy fog", "Drizzle", "Heavy drizzle", "Freezing drizzle",
        "Rain", "Heavy rain", "Freezing rain", "Snow", "Heavy snow", "Snow grains",
        "Rain showers", "Heavy rain showers", "Snow showers", "Thunderstorm",
        "Thunderstorm with hail", "Heavy thunderstorm with hail"
    };

    private static String wmoDesc(int code) {
        if (code == 0)  return "Clear sky ☀️";
        if (code <= 2)  return "Partly cloudy ⛅";
        if (code == 3)  return "Overcast ☁️";
        if (code <= 49) return "Foggy 🌫️";
        if (code <= 59) return "Drizzle 🌦️";
        if (code <= 69) return "Rainy 🌧️";
        if (code <= 79) return "Snowy ❄️";
        if (code <= 84) return "Rain showers 🌦️";
        if (code <= 94) return "Thunderstorm ⛈️";
        return "Hailstorm ⛈️";
    }

    public static boolean isWeatherQuery(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("weather") || lower.contains("forecast") ||
               lower.contains("temperature") || lower.contains("rain") ||
               lower.contains("hot today") || lower.contains("cold today") ||
               lower.contains("will it rain") || lower.contains("sunny") ||
               lower.contains("humid") || lower.contains("wind speed") ||
               lower.contains("7 day") || lower.contains("week forecast") ||
               lower.contains("what's the weather") || lower.contains("how's the weather");
    }

    public static boolean isWeeklyQuery(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("7 day") || lower.contains("week") ||
               lower.contains("forecast") || lower.contains("next few days");
    }

    public static void fetch(Context ctx, boolean weekly, Callback cb) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            cb.onError("[EMOTION:concerned] Location permission needed for weather, sir.");
            return;
        }

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        Location loc = null;
        try {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        } catch (SecurityException ignored) {}

        // Fallback: use Dubai coordinates if no GPS
        final double lat = loc != null ? loc.getLatitude()  : 25.2048;
        final double lon = loc != null ? loc.getLongitude() : 55.2708;
        final String locNote = loc != null ? "" : " (using Dubai as fallback)";

        new Thread(() -> {
            try {
                String urlStr = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=" + lat + "&longitude=" + lon +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                    "precipitation_probability,wind_speed_10m,weathercode" +
                    "&daily=weathercode,temperature_2m_max,temperature_2m_min," +
                    "precipitation_probability_max,wind_speed_10m_max" +
                    "&timezone=auto&forecast_days=7";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000); conn.setReadTimeout(12000);
                conn.setRequestProperty("User-Agent", "HENRY-AI/1.0");
                conn.connect();

                InputStream is = conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[32768]; int read;
                while ((read = is.read(buf)) != -1) sb.append(new String(buf, 0, read, "UTF-8"));
                is.close();

                JSONObject json    = new JSONObject(sb.toString());
                JSONObject current = json.optJSONObject("current");
                JSONObject daily   = json.optJSONObject("daily");

                if (current == null) { cb.onError("[EMOTION:neutral] Weather data unavailable, sir."); return; }

                double temp     = current.optDouble("temperature_2m", 0);
                double feelsLike= current.optDouble("apparent_temperature", 0);
                double humidity = current.optDouble("relative_humidity_2m", 0);
                double windSpd  = current.optDouble("wind_speed_10m", 0);
                int rainPct     = (int) current.optDouble("precipitation_probability", 0);
                int wmoCode     = (int) current.optDouble("weathercode", 0);
                String condition = wmoDesc(wmoCode);

                String emotion = temp > 38 ? "concerned" : temp < 10 ? "concerned"
                               : rainPct > 60 ? "neutral" : "excited";

                StringBuilder result = new StringBuilder();
                result.append("[EMOTION:").append(emotion).append("] ");
                result.append("**Current Weather").append(locNote).append(":**\n");
                result.append(condition).append("\n");
                result.append(String.format(Locale.US,
                    "🌡 **%.0f°C** (feels like %.0f°C)  |  💧 Humidity: **%.0f%%**\n" +
                    "💨 Wind: **%.0f km/h**  |  ☔ Rain chance: **%d%%**\n\n",
                    temp, feelsLike, humidity, windSpd, rainPct));

                if (weekly && daily != null) {
                    JSONArray dates   = daily.optJSONArray("time");
                    JSONArray maxTemp = daily.optJSONArray("temperature_2m_max");
                    JSONArray minTemp = daily.optJSONArray("temperature_2m_min");
                    JSONArray wmos    = daily.optJSONArray("weathercode");
                    JSONArray rainMax = daily.optJSONArray("precipitation_probability_max");

                    result.append("**7-Day Forecast:**\n");
                    SimpleDateFormat inFmt  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    SimpleDateFormat outFmt = new SimpleDateFormat("EEE dd MMM", Locale.US);
                    int days = dates != null ? Math.min(dates.length(), 7) : 0;
                    for (int i = 0; i < days; i++) {
                        try {
                            Date d = inFmt.parse(dates.getString(i));
                            String dayStr = outFmt.format(d);
                            double mx = maxTemp != null ? maxTemp.optDouble(i, 0) : 0;
                            double mn = minTemp != null ? minTemp.optDouble(i, 0) : 0;
                            int wc   = wmos    != null ? (int) wmos.optDouble(i, 0) : 0;
                            int rp   = rainMax != null ? (int) rainMax.optDouble(i, 0) : 0;
                            result.append(String.format(Locale.US,
                                "**%s** — %s  %.0f°/%.0f°C ☔%d%%\n",
                                dayStr, wmoDesc(wc), mx, mn, rp));
                        } catch (Exception ignored) {}
                    }
                }

                cb.onResult(result.toString().trim());
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Couldn't fetch weather: " + e.getMessage());
            }
        }).start();
    }
}
