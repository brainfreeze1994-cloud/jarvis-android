package com.jarvis.ai;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Assembles a morning digest: weather + news + calendar + reminders.
 * All data fetched in parallel (threads), assembled into one reply.
 */
public class DailyDigest {

    public interface Callback {
        void onResult(String digest);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void build(Context ctx, Callback cb) {
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("[EMOTION:excited]\n");
            sb.append("# ☀️ Good morning, sir — your daily briefing\n\n");

            // 1. Date/time
            String dt = new SimpleDateFormat("EEEE, MMMM d yyyy — h:mm a", Locale.US).format(new Date());
            sb.append("**").append(dt).append("**\n\n");

            // 2. Weather
            sb.append(fetchWeather(ctx));

            // 3. Calendar
            sb.append(fetchCalendar(ctx));

            // 4. Reminders
            sb.append(fetchReminders(ctx));

            // 5. News headlines
            sb.append(fetchNews());

            sb.append("\n---\nThat's your briefing, sir. Ready when you are.");
            MAIN.post(() -> cb.onResult(sb.toString().trim()));
        }).start();
    }

    private static String fetchWeather(Context ctx) {
        try {
            UserProfile profile = UserProfile.load(ctx);
            String city = (profile.city != null && !profile.city.isEmpty()) ? profile.city : "Dubai";
            URL url = new URL("https://wttr.in/" + java.net.URLEncoder.encode(city, "UTF-8") + "?format=j1");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "HENRY/8.0");
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder js = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) js.append(line);
            br.close();
            JSONObject w   = new JSONObject(js.toString());
            JSONObject cur = w.getJSONArray("current_condition").getJSONObject(0);
            String cond    = cur.getJSONArray("weatherDesc").getJSONObject(0).getString("value");
            String tempC   = cur.getString("temp_C");
            String feelsC  = cur.getString("FeelsLikeC");
            String humid   = cur.getString("humidity");
            return "## 🌤 Weather — " + city + "\n"
                + "**" + cond + "**, " + tempC + "°C (feels " + feelsC + "°C), humidity " + humid + "%\n\n";
        } catch (Exception e) {
            return "## 🌤 Weather\nUnavailable right now, sir.\n\n";
        }
    }

    private static String fetchCalendar(Context ctx) {
        try {
            ContentResolver cr  = ctx.getContentResolver();
            long now     = System.currentTimeMillis();
            long end     = now + 24 * 3600_000L;
            Uri uri      = CalendarContract.Events.CONTENT_URI;
            String[] proj = { CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART };
            String sel    = CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.DTSTART + " <= ?";
            try (Cursor c = cr.query(uri, proj, sel,
                    new String[]{ String.valueOf(now), String.valueOf(end) },
                    CalendarContract.Events.DTSTART + " ASC")) {
                if (c == null || !c.moveToFirst())
                    return "## 📅 Calendar\nNo events today, sir.\n\n";
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
                StringBuilder sb = new StringBuilder("## 📅 Today's Events\n");
                int count = 0;
                do {
                    String title = c.getString(0);
                    long   start = c.getLong(1);
                    sb.append("• **").append(sdf.format(new Date(start))).append("** — ").append(title).append("\n");
                    count++;
                } while (c.moveToNext() && count < 5);
                return sb.append("\n").toString();
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static String fetchReminders(Context ctx) {
        try {
            String list = ReminderManager.listReminders(ctx);
            if (list == null || list.isEmpty()) return "";
            return "## ⏰ Reminders\n" + list.replaceAll("\\[EMOTION:\\w+\\]\\s*", "") + "\n\n";
        } catch (Exception e) { return ""; }
    }

    private static String fetchNews() {
        try {
            URL url = new URL("https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Ffeeds.bbci.co.uk%2Fnews%2Fworld%2Frss.xml");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "HENRY/8.0");
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder js  = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) js.append(line);
            br.close();
            JSONObject root  = new JSONObject(js.toString());
            if (!"ok".equals(root.optString("status"))) return "";
            org.json.JSONArray items = root.getJSONArray("items");
            StringBuilder sb = new StringBuilder("## 📰 Headlines\n");
            for (int i = 0; i < Math.min(5, items.length()); i++) {
                String title = items.getJSONObject(i).optString("title", "").trim();
                if (!title.isEmpty()) sb.append("• ").append(title).append("\n");
            }
            return sb.append("\n").toString();
        } catch (Exception e) { return ""; }
    }
}
