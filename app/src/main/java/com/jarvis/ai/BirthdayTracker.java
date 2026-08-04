package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Contact Birthday Tracker — remember birthdays, remind before them, draft wishes.
 * "Remember John's birthday is March 15"
 * "When is Sarah's birthday?"
 * "Who has a birthday this month?"
 * "Write a birthday message for Tom"
 */
public class BirthdayTracker {

    private static final String PREFS    = "birthday_prefs";
    private static final String KEY_DATA = "birthdays";

    public static boolean isBirthdayCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("birthday") || lower.contains("born on") ||
               lower.contains("birth date") || lower.contains("bday");
    }

    public static String handle(Context ctx, String text, UserProfile profile) {
        String lower = text.toLowerCase(Locale.US);

        if (lower.contains("remember") || lower.contains("add") || lower.contains("save") ||
            lower.contains("is on") || lower.contains("is in") || lower.contains("born on")) {
            return addBirthday(ctx, text);
        }
        if (lower.contains("this month") || lower.contains("upcoming") || lower.contains("coming up")) {
            return getUpcomingBirthdays(ctx);
        }
        if (lower.contains("who has") || lower.contains("list") || lower.contains("all birthday")) {
            return listAllBirthdays(ctx);
        }
        if (lower.contains("when is") || lower.contains("birthday of")) {
            return findBirthday(ctx, text);
        }
        if (lower.contains("wish") || lower.contains("message for") || lower.contains("write")) {
            return generateWish(text, profile);
        }
        if (lower.contains("delete") || lower.contains("remove")) {
            return deleteBirthday(ctx, text);
        }
        return getUpcomingBirthdays(ctx);
    }

    private static String addBirthday(Context ctx, String text) {
        try {
            // Parse: "John's birthday is March 15" or "Add birthday for Sarah on April 20"
            String lower = text.toLowerCase(Locale.US);
            String[] months = {"january","february","march","april","may","june",
                               "july","august","september","october","november","december"};
            int month = -1, day = -1;
            for (int i = 0; i < months.length; i++) {
                if (lower.contains(months[i])) {
                    month = i + 1;
                    // Find day number near month name
                    String[] words = lower.split("\\s+");
                    for (int w = 0; w < words.length; w++) {
                        if (words[w].equals(months[i]) || words[w].startsWith(months[i])) {
                            // Check word before and after
                            if (w > 0) { try { day = Integer.parseInt(words[w-1].replaceAll("[^0-9]","")); } catch (Exception ignored) {} }
                            if (day <= 0 && w < words.length - 1) { try { day = Integer.parseInt(words[w+1].replaceAll("[^0-9]","")); } catch (Exception ignored) {} }
                        }
                    }
                    break;
                }
            }
            // Find numeric date fallback
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})").matcher(text);
            if (m.find() && month == -1) {
                month = Integer.parseInt(m.group(1));
                day   = Integer.parseInt(m.group(2));
            }
            if (month == -1 || day <= 0) return "[EMOTION:neutral] I couldn't parse the date, sir. Try: \"Remember John's birthday is March 15\"";

            // Extract name
            String name = extractName(text);
            if (name.isEmpty()) return "[EMOTION:neutral] Whose birthday is this, sir?";

            saveBirthday(ctx, name, month, day);
            return String.format(Locale.US, "[EMOTION:warm] Saved! I'll remember that **%s's birthday is %s %d**, sir. 🎂",
                name, months[month - 1].substring(0,1).toUpperCase() + months[month-1].substring(1), day);
        } catch (Exception e) {
            return "[EMOTION:neutral] Couldn't save that birthday, sir. Try: \"Remember John's birthday is March 15\"";
        }
    }

    private static String findBirthday(Context ctx, String text) {
        try {
            String name = extractName(text).toLowerCase(Locale.US);
            JSONArray arr = loadBirthdays(ctx);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.getString("name").toLowerCase(Locale.US).contains(name)) {
                    int m = o.getInt("month"), d = o.getInt("day");
                    int daysAway = daysUntil(m, d);
                    return String.format(Locale.US,
                        "[EMOTION:warm] **%s's birthday is %s %d** — %s, sir! 🎂",
                        o.getString("name"), monthName(m), d,
                        daysAway == 0 ? "**TODAY**" : daysAway == 1 ? "**TOMORROW**" : "in " + daysAway + " days");
                }
            }
            return "[EMOTION:neutral] I don't have " + name + "'s birthday saved, sir.";
        } catch (Exception e) { return "[EMOTION:neutral] Couldn't find that birthday, sir."; }
    }

    private static String getUpcomingBirthdays(Context ctx) {
        try {
            JSONArray arr = loadBirthdays(ctx);
            if (arr.length() == 0) return "[EMOTION:neutral] No birthdays saved yet, sir. Say 'Remember John's birthday is March 15' to add one.";
            List<JSONObject> upcoming = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int days = daysUntil(o.getInt("month"), o.getInt("day"));
                if (days <= 30) { o.put("days_away", days); upcoming.add(o); }
            }
            if (upcoming.isEmpty()) return "[EMOTION:neutral] No birthdays in the next 30 days, sir.";
            upcoming.sort((a, b) -> { try { return a.getInt("days_away") - b.getInt("days_away"); } catch (Exception e) { return 0; } });
            StringBuilder sb = new StringBuilder("[EMOTION:warm] **🎂 Upcoming Birthdays:**\n\n");
            for (JSONObject o : upcoming) {
                int days = o.optInt("days_away");
                String when = days == 0 ? "**TODAY** 🎉" : days == 1 ? "**TOMORROW** 🎂" : "in " + days + " days";
                sb.append(String.format(Locale.US, "• **%s** — %s %d (%s)\n",
                    o.getString("name"), monthName(o.getInt("month")), o.getInt("day"), when));
            }
            return sb.toString();
        } catch (Exception e) { return "[EMOTION:neutral] Error loading birthdays, sir."; }
    }

    private static String listAllBirthdays(Context ctx) {
        try {
            JSONArray arr = loadBirthdays(ctx);
            if (arr.length() == 0) return "[EMOTION:neutral] No birthdays saved yet, sir.";
            StringBuilder sb = new StringBuilder("[EMOTION:neutral] **🎂 All Birthdays:**\n\n");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                sb.append(String.format(Locale.US, "%d. **%s** — %s %d\n",
                    i+1, o.getString("name"), monthName(o.getInt("month")), o.getInt("day")));
            }
            return sb.toString();
        } catch (Exception e) { return "[EMOTION:neutral] Error listing birthdays, sir."; }
    }

    private static String generateWish(String text, UserProfile profile) {
        // Synchronous simple wish — async handled by MainActivity via AI
        String name = extractName(text);
        if (name.isEmpty()) name = "them";
        return "[EMOTION:warm] **Happy Birthday, " + name + "!** 🎂🎉\n\n" +
               "Wishing you a day as wonderful as you are. May this year bring you joy, success, and everything your heart desires. Here's to another year of amazing moments!\n\n" +
               "— Sent with love via H.E.N.R.Y";
    }

    private static String deleteBirthday(Context ctx, String text) {
        try {
            String name = extractName(text).toLowerCase(Locale.US);
            JSONArray arr = loadBirthdays(ctx);
            JSONArray updated = new JSONArray();
            boolean found = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.getString("name").toLowerCase(Locale.US).contains(name)) { found = true; }
                else updated.put(o);
            }
            if (!found) return "[EMOTION:neutral] Couldn't find that person's birthday, sir.";
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DATA, updated.toString()).apply();
            return "[EMOTION:neutral] Removed that birthday, sir.";
        } catch (Exception e) { return "[EMOTION:neutral] Error removing birthday, sir."; }
    }

    public static String checkTodayBirthdays(Context ctx) {
        try {
            Calendar cal = Calendar.getInstance();
            int todayM = cal.get(Calendar.MONTH) + 1, todayD = cal.get(Calendar.DAY_OF_MONTH);
            JSONArray arr = loadBirthdays(ctx);
            List<String> today = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.getInt("month") == todayM && o.getInt("day") == todayD)
                    today.add(o.getString("name"));
            }
            if (today.isEmpty()) return null;
            return "[EMOTION:warm] 🎂 Today is **" + String.join("** and **", today) + "'s birthday**, sir! Don't forget to wish them!";
        } catch (Exception e) { return null; }
    }

    private static void saveBirthday(Context ctx, String name, int month, int day) throws Exception {
        JSONArray arr = loadBirthdays(ctx);
        // Update if exists
        for (int i = 0; i < arr.length(); i++) {
            if (arr.getJSONObject(i).getString("name").equalsIgnoreCase(name)) {
                arr.getJSONObject(i).put("month", month).put("day", day); 
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DATA, arr.toString()).apply();
                return;
            }
        }
        JSONObject entry = new JSONObject();
        entry.put("name", name); entry.put("month", month); entry.put("day", day);
        arr.put(entry);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DATA, arr.toString()).apply();
    }

    private static JSONArray loadBirthdays(Context ctx) {
        try { return new JSONArray(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private static int daysUntil(int month, int day) {
        Calendar today = Calendar.getInstance();
        Calendar bday = Calendar.getInstance();
        bday.set(Calendar.MONTH, month - 1);
        bday.set(Calendar.DAY_OF_MONTH, day);
        bday.set(Calendar.HOUR_OF_DAY, 0); bday.set(Calendar.MINUTE, 0); bday.set(Calendar.SECOND, 0);
        if (bday.before(today)) bday.add(Calendar.YEAR, 1);
        long diff = bday.getTimeInMillis() - today.getTimeInMillis();
        return (int)(diff / (1000 * 60 * 60 * 24));
    }

    private static String monthName(int m) {
        String[] names = {"","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        return m >= 1 && m <= 12 ? names[m] : "?";
    }

    private static String extractName(String text) {
        String lower = text.toLowerCase(Locale.US);
        String[] removals = {"remember","add","save","when is","birthday of","birthday","is on","is in",
                             "born on","for","the","a","an","my","their","his","her","bday",
                             "wish","message","write","delete","remove","'s","'s","is"};
        String result = text;
        for (String r : removals) result = result.replaceAll("(?i)\\b" + r + "\\b", "");
        // Remove month names and numbers
        result = result.replaceAll("(?i)(january|february|march|april|may|june|july|august|september|october|november|december)", "");
        result = result.replaceAll("[0-9/\\-,.]", "").trim();
        // Take first remaining word
        String[] words = result.trim().split("\\s+");
        for (String w : words) { if (w.length() > 1) return w.trim(); }
        return "";
    }
}
