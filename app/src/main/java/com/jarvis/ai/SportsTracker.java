package com.jarvis.ai;

import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * SportsTracker — Live sports scores via free APIs
 * Football: api-football.com free tier / TheSportsDB (free, no key)
 * NBA: balldontlie.io (free, no key)
 */
public class SportsTracker {

    public interface Callback {
        void onResult(String result);
        void onError(String msg);
    }

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final String SPORTSDB = "https://www.thesportsdb.com/api/v1/json/3";

    public static boolean isSportsQuery(String input) {
        if (input == null) return false;
        String t = input.toLowerCase().trim();
        // Never hijack coding, creation, gaming, or app logic
        if (t.contains("create") || t.contains("build") || t.contains("make") || t.contains("code")
            || t.contains("program") || t.contains("play") || t.contains("develop") || t.contains("write")
            || t.contains("tic tac toe") || t.contains("snake") || t.contains("flappy") || t.contains("chess")
            || t.contains("riddle") || t.contains("trivia") || t.contains("quiz") || t.contains("design")) {
            return false;
        }
        // Match specific sports entities or live scores
        if (t.contains("football") || t.contains("soccer") || t.contains("basketball")
            || t.contains("nba") || t.contains("premier league") || t.contains("champions league")
            || t.contains("cricket") || t.contains("sports") || t.contains("standings") || t.contains("fixtures")) {
            return true;
        }
        if (t.contains("live score") || t.contains("match result") || t.contains("game score") || t.contains("sports score")) {
            return true;
        }
        return false;
    }

    public static void getLiveScores(String sport, Callback cb) {
        new Thread(() -> {
            try {
                // TheSportsDB — free, no key needed
                String endpoint = sport.toLowerCase().contains("basketball") || sport.toLowerCase().contains("nba")
                    ? SPORTSDB + "/eventspastleague.php?id=4387"  // NBA
                    : SPORTSDB + "/eventspastleague.php?id=4328"; // Premier League

                String raw = get(endpoint);
                if (raw != null) {
                    JSONObject obj = new JSONObject(raw);
                    JSONArray events = obj.optJSONArray("events");
                    if (events != null && events.length() > 0) {
                        StringBuilder sb = new StringBuilder("⚽ Recent Results:\n\n");
                        int limit = Math.min(5, events.length());
                        for (int i = events.length() - 1; i >= events.length() - limit; i--) {
                            JSONObject e = events.getJSONObject(i);
                            String home  = e.optString("strHomeTeam", "?");
                            String away  = e.optString("strAwayTeam", "?");
                            String sh    = e.optString("intHomeScore", "-");
                            String sa    = e.optString("intAwayScore", "-");
                            String date  = e.optString("dateEvent", "");
                            sb.append(String.format("  %s %s – %s %s\n  %s\n\n", home, sh, sa, away, date));
                        }
                        H.post(() -> cb.onResult(sb.toString()));
                        return;
                    }
                }
                H.post(() -> cb.onResult("⚽ Live scores unavailable right now. Try ESPN or BBC Sport for live updates, sir."));
            } catch (Exception e) {
                H.post(() -> cb.onError("Sports data error: " + e.getMessage()));
            }
        }).start();
    }

    public static void searchTeam(String teamName, Callback cb) {
        new Thread(() -> {
            try {
                String raw = get(SPORTSDB + "/searchteams.php?t=" + URLEncoder.encode(teamName, "UTF-8"));
                if (raw != null) {
                    JSONObject obj = new JSONObject(raw);
                    JSONArray teams = obj.optJSONArray("teams");
                    if (teams != null && teams.length() > 0) {
                        JSONObject t = teams.getJSONObject(0);
                        String name    = t.optString("strTeam", teamName);
                        String league  = t.optString("strLeague", "Unknown");
                        String country = t.optString("strCountry", "Unknown");
                        String formed  = t.optString("intFormedYear", "Unknown");
                        String stadium = t.optString("strStadium", "Unknown");
                        String desc    = t.optString("strDescriptionEN", "");
                        String summary = String.format(
                            "🏆 %s\nLeague: %s\nCountry: %s\nFounded: %s\nStadium: %s\n\n%s",
                            name, league, country, formed, stadium,
                            desc.length() > 300 ? desc.substring(0, 300) + "…" : desc);
                        H.post(() -> cb.onResult(summary));
                        return;
                    }
                }
                H.post(() -> cb.onResult("No data found for team: " + teamName));
            } catch (Exception e) {
                H.post(() -> cb.onError("Team search error: " + e.getMessage()));
            }
        }).start();
    }

    private static String get(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000); c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "HENRY-AI/24");
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) { return null; }
    }
}
