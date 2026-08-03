package com.jarvis.ai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expense Tracker — log, categorise, and summarise spending.
 * "I spent 50 AED on food"
 * "Add expense 120 transport"
 * "My expenses this month"
 * "How much did I spend on food?"
 */
public class ExpenseTracker {

    private static final String PREFS    = "expense_prefs";
    private static final String KEY_DATA = "expenses_json";
    private static final String DATE_FMT = "yyyy-MM-dd";
    private static final String DISPLAY_FMT = "dd MMM";

    // ── Categories ────────────────────────────────────────────────────────────
    private static final Map<String, String[]> CATEGORIES = new HashMap<String, String[]>() {{
        put("food",          new String[]{"food","eat","restaurant","lunch","dinner","breakfast","coffee","cafe","groceries","grocery","meal","snack","pizza","burger","kebab","sushi"});
        put("transport",     new String[]{"transport","taxi","uber","careem","bus","metro","fuel","petrol","gas","parking","lyft","car","ride"});
        put("shopping",      new String[]{"shopping","clothes","shoes","mall","online","amazon","noon","store","shop","fashion","accessories","bag","watch"});
        put("bills",         new String[]{"bill","electric","water","internet","phone","rent","subscription","netflix","spotify","utilities"});
        put("health",        new String[]{"health","medicine","pharmacy","doctor","hospital","clinic","gym","fitness","dental","medical"});
        put("entertainment", new String[]{"entertainment","movie","cinema","game","concert","event","ticket","fun","hobby","sport","netflix"});
        put("travel",        new String[]{"travel","hotel","flight","holiday","vacation","trip","booking","airbnb","visa"});
        put("education",     new String[]{"education","course","book","tuition","school","study","class","learning"});
        put("other",         new String[]{"other","misc","miscellaneous","stuff"});
    }};

    // ── Data model ────────────────────────────────────────────────────────────
    static class Expense {
        double amount;
        String currency;
        String category;
        String note;
        String date;

        Expense(double amount, String currency, String category, String note, String date) {
            this.amount = amount; this.currency = currency;
            this.category = category; this.note = note; this.date = date;
        }

        Expense(JSONObject j) throws Exception {
            amount   = j.getDouble("amount");
            currency = j.optString("currency", "AED");
            category = j.optString("category", "other");
            note     = j.optString("note", "");
            date     = j.optString("date", "");
        }

        JSONObject toJson() throws Exception {
            return new JSONObject()
                .put("amount", amount).put("currency", currency)
                .put("category", category).put("note", note).put("date", date);
        }
    }

    // ── Load / Save ───────────────────────────────────────────────────────────
    private static List<Expense> load(Context ctx) {
        List<Expense> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(new Expense(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return list;
    }

    private static void save(Context ctx, List<Expense> expenses) {
        try {
            JSONArray arr = new JSONArray();
            for (Expense e : expenses) arr.put(e.toJson());
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putString(KEY_DATA, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    // ── Detection ─────────────────────────────────────────────────────────────
    public static boolean isExpenseCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("i spent") || lower.startsWith("i paid") ||
               lower.startsWith("add expense") || lower.startsWith("log expense") ||
               lower.startsWith("expense") || lower.contains("i bought") ||
               lower.contains("my expenses") || lower.contains("spending report") ||
               lower.contains("how much did i spend") || lower.contains("total expenses") ||
               lower.contains("spending summary") || lower.contains("expense report");
    }

    // ── Parse & Log ───────────────────────────────────────────────────────────
    public static String handle(Context ctx, String text) {
        String lower = text.toLowerCase(Locale.US);
        List<Expense> expenses = load(ctx);

        // Show report
        if (lower.contains("my expenses") || lower.contains("spending report") ||
            lower.contains("how much did i spend") || lower.contains("total expenses") ||
            lower.contains("spending summary") || lower.contains("expense report")) {
            return buildReport(expenses);
        }

        // Clear all
        if (lower.contains("clear expense") || lower.contains("reset expense") || lower.contains("delete expense")) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_DATA).apply();
            return "[EMOTION:neutral] All expenses cleared, sir.";
        }

        // Parse amount
        Matcher amtM = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(aed|usd|eur|gbp|php|inr|sgd|sar|qar|bhd|kwd|omr)?",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (!amtM.find()) return "[EMOTION:neutral] How much did you spend, sir? Try: 'I spent 50 AED on food'.";

        double amount = Double.parseDouble(amtM.group(1).replace(",", "."));
        String currency = amtM.group(2) != null ? amtM.group(2).toUpperCase() : "AED";
        String category = detectCategory(lower);
        String note = text.replaceAll("(?i)(i spent|i paid|i bought|add expense|log expense|expense)\\s*", "")
                          .replaceAll("(?i)\\d+(?:[.,]\\d+)?\\s*(aed|usd|eur|gbp|php|inr|sgd|sar|qar|bhd|kwd|omr)?\\s*", "")
                          .replaceAll("(?i)on\\s+", "").trim();
        String date = new SimpleDateFormat(DATE_FMT, Locale.US).format(new Date());

        expenses.add(new Expense(amount, currency, category, note, date));
        if (expenses.size() > 500) expenses = expenses.subList(expenses.size() - 500, expenses.size());
        save(ctx, expenses);

        // Monthly total for this category
        double monthTotal = monthTotal(expenses, category, currency);
        String catIcon = categoryIcon(category);

        return String.format(Locale.US,
            "[EMOTION:neutral] %s Logged **%.2f %s** on **%s**, sir.\n" +
            "%s total this month: **%.2f %s**.",
            catIcon, amount, currency, category, capitalize(category), monthTotal, currency);
    }

    private static String detectCategory(String lower) {
        for (Map.Entry<String, String[]> e : CATEGORIES.entrySet())
            for (String kw : e.getValue())
                if (lower.contains(kw)) return e.getKey();
        return "other";
    }

    private static double monthTotal(List<Expense> expenses, String category, String currency) {
        String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        double total = 0;
        for (Expense e : expenses)
            if (e.date.startsWith(month) && e.category.equals(category) && e.currency.equals(currency))
                total += e.amount;
        return total;
    }

    private static String buildReport(List<Expense> expenses) {
        if (expenses.isEmpty())
            return "[EMOTION:neutral] No expenses logged yet, sir. Try 'I spent 50 AED on food'.";

        String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        String monthLabel = new SimpleDateFormat("MMMM yyyy", Locale.US).format(new Date());

        Map<String, Double> catTotals = new HashMap<>();
        double grandTotal = 0;
        String primaryCurrency = "AED";
        List<Expense> thisMonth = new ArrayList<>();

        for (Expense e : expenses) {
            if (e.date.startsWith(month)) {
                thisMonth.add(e);
                catTotals.merge(e.category, e.amount, Double::sum);
                grandTotal += e.amount;
                primaryCurrency = e.currency;
            }
        }

        if (thisMonth.isEmpty())
            return "[EMOTION:neutral] No expenses logged this month yet, sir.";

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(catTotals.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        StringBuilder sb = new StringBuilder();
        sb.append("[EMOTION:neutral] **Expense Report — ").append(monthLabel).append(", sir:**\n\n");
        sb.append(String.format(Locale.US, "💰 **Total: %.2f %s**\n\n", grandTotal, primaryCurrency));
        sb.append("**By category:**\n");
        for (Map.Entry<String, Double> e : sorted) {
            double pct = grandTotal > 0 ? e.getValue() / grandTotal * 100 : 0;
            sb.append(String.format(Locale.US, "%s **%s**: %.2f %s (%.0f%%)\n",
                categoryIcon(e.getKey()), capitalize(e.getKey()), e.getValue(), primaryCurrency, pct));
        }
        sb.append("\n**Last 5 transactions:**\n");
        int from = Math.max(0, thisMonth.size() - 5);
        for (int i = from; i < thisMonth.size(); i++) {
            Expense e = thisMonth.get(i);
            try {
                Date d = new SimpleDateFormat(DATE_FMT, Locale.US).parse(e.date);
                String ds = new SimpleDateFormat(DISPLAY_FMT, Locale.US).format(d);
                sb.append(String.format(Locale.US, "%s %s — %.2f %s%s\n",
                    categoryIcon(e.category), ds, e.amount, e.currency,
                    e.note.isEmpty() ? "" : " (" + e.note + ")"));
            } catch (Exception ignored) {}
        }
        return sb.toString().trim();
    }

    private static String categoryIcon(String cat) {
        switch (cat) {
            case "food":          return "🍽";
            case "transport":     return "🚗";
            case "shopping":      return "🛍";
            case "bills":         return "📄";
            case "health":        return "💊";
            case "entertainment": return "🎬";
            case "travel":        return "✈️";
            case "education":     return "📚";
            default:              return "💸";
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
