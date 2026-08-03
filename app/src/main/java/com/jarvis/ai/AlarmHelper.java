package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sets alarms using the system AlarmClock intent — works with any clock app.
 * No permissions needed. Supports repeating alarms (days of week).
 */
public class AlarmHelper {

    /**
     * Parses alarm commands and sets the alarm.
     * Returns a reply string or null if not an alarm command.
     */
    public static String parseAndSet(Context ctx, String text) {
        String t = text.toLowerCase();
        if (!t.contains("alarm") && !t.contains("wake me") && !t.contains("wake up")) return null;

        // Parse time
        int hour = -1, minute = 0;
        boolean pm = t.contains("pm") && !t.contains("am");
        boolean am = t.contains("am");

        // "7:30 am", "7:30am", "19:30"
        Matcher hm = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(?:am|pm)?").matcher(t);
        if (hm.find()) {
            hour   = Integer.parseInt(hm.group(1));
            minute = Integer.parseInt(hm.group(2));
        } else {
            // "7 am", "7am", plain "7"
            Matcher hOnly = Pattern.compile("(\\d{1,2})\\s*(?:am|pm|o'clock|oclock)?").matcher(t);
            if (hOnly.find()) {
                hour = Integer.parseInt(hOnly.group(1));
            }
        }
        if (hour < 0) return null;

        // 12-hour to 24-hour conversion
        if (pm && hour != 12) hour += 12;
        if (am && hour == 12) hour = 0;
        hour = Math.min(23, Math.max(0, hour));

        // Parse days
        List<Integer> days = parseDays(t); // AlarmClock.EXTRA_DAYS uses Calendar constants

        // Label
        String label = "H.E.N.R.Y Alarm";
        Matcher lm = Pattern.compile("(?:called|named|label(?:led)?)\\s+[\"']?([\\w\\s]{2,30})[\"']?").matcher(t);
        if (lm.find()) label = capitalize(lm.group(1).trim());

        try {
            Intent alarm = new Intent(AlarmClock.ACTION_SET_ALARM);
            alarm.putExtra(AlarmClock.EXTRA_HOUR,    hour);
            alarm.putExtra(AlarmClock.EXTRA_MINUTES, minute);
            alarm.putExtra(AlarmClock.EXTRA_MESSAGE, label);
            alarm.putExtra(AlarmClock.EXTRA_SKIP_UI, false); // show the alarm app briefly
            if (!days.isEmpty()) {
                alarm.putExtra(AlarmClock.EXTRA_DAYS, new ArrayList<>(days));
            }
            alarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(alarm);

            String timeStr = String.format(java.util.Locale.US, "%02d:%02d", hour, minute);
            String daysStr = days.isEmpty() ? "" : " (" + daysLabel(days) + ")";
            return "[EMOTION:proud] Alarm set for **" + timeStr + daysStr + "**, sir.";
        } catch (Exception e) {
            return "[EMOTION:concerned] Couldn't set alarm, sir. Please open your clock app manually.";
        }
    }

    private static List<Integer> parseDays(String t) {
        List<Integer> days = new ArrayList<>();
        // Calendar.MONDAY=2 … Calendar.SUNDAY=1
        if (t.contains("monday")    || t.contains("mon")) days.add(2);
        if (t.contains("tuesday")   || t.contains("tue")) days.add(3);
        if (t.contains("wednesday") || t.contains("wed")) days.add(4);
        if (t.contains("thursday")  || t.contains("thu")) days.add(5);
        if (t.contains("friday")    || t.contains("fri")) days.add(6);
        if (t.contains("saturday")  || t.contains("sat")) days.add(7);
        if (t.contains("sunday")    || t.contains("sun")) days.add(1);
        if (t.contains("every day") || t.contains("daily") || t.contains("weekday") && days.isEmpty()) {
            if (t.contains("weekday")) { days.add(2); days.add(3); days.add(4); days.add(5); days.add(6); }
            else                       { for (int i=1;i<=7;i++) days.add(i); }
        }
        if (t.contains("weekend") && days.isEmpty()) { days.add(1); days.add(7); }
        return days;
    }

    private static String daysLabel(List<Integer> days) {
        String[] names = {"","Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        StringBuilder sb = new StringBuilder();
        for (int d : days) { if (sb.length()>0) sb.append(","); sb.append(names[d]); }
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
