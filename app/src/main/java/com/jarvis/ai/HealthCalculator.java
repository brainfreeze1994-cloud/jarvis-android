package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Health Calculator — BMI, TDEE, water intake, ideal weight.
 * All calculations on-device, no API needed.
 * "My BMI is 75kg and 1.75m"
 * "How many calories do I need?"
 * "How much water should I drink?"
 * "What's my ideal weight?"
 */
public class HealthCalculator {

    private static final String PREFS      = "health_prefs";
    private static final String KEY_WEIGHT = "weight_kg";
    private static final String KEY_HEIGHT = "height_cm";
    private static final String KEY_AGE    = "age";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_ACT    = "activity";

    public static boolean isHealthCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("bmi") || lower.contains("body mass") ||
               lower.contains("calorie") || lower.contains("tdee") ||
               lower.contains("how much water") || lower.contains("water intake") ||
               lower.contains("ideal weight") || lower.contains("healthy weight") ||
               lower.contains("my weight") || lower.contains("my height") ||
               lower.contains("my bmi") || lower.contains("calculate bmi") ||
               lower.contains("how many calories") || lower.contains("daily calories") ||
               lower.contains("daily water") || lower.contains("water goal");
    }

    public static String handle(Context ctx, String text) {
        String lower = text.toLowerCase(Locale.US);
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Update stored stats if provided
        extractAndStore(ctx, text);

        float weight = prefs.getFloat(KEY_WEIGHT, 0);
        float height = prefs.getFloat(KEY_HEIGHT, 0); // in cm
        int   age    = prefs.getInt(KEY_AGE, 0);
        String gender = prefs.getString(KEY_GENDER, "male");
        int   activity = prefs.getInt(KEY_ACT, 2); // 1=sedentary…5=very active

        // BMI
        if (lower.contains("bmi") || lower.contains("body mass")) {
            if (weight <= 0 || height <= 0) return promptStats();
            return calcBMI(weight, height);
        }
        // Calories / TDEE
        if (lower.contains("calorie") || lower.contains("tdee") || lower.contains("how many calories")) {
            if (weight <= 0 || height <= 0 || age <= 0) return promptStats();
            return calcTDEE(weight, height, age, gender, activity);
        }
        // Water
        if (lower.contains("water")) {
            if (weight <= 0) return "[EMOTION:neutral] Tell me your weight first, sir. E.g. 'I weigh 75 kg'.";
            return calcWater(weight);
        }
        // Ideal weight
        if (lower.contains("ideal weight") || lower.contains("healthy weight")) {
            if (height <= 0) return "[EMOTION:neutral] Tell me your height first, sir. E.g. 'I am 175 cm tall'.";
            return calcIdealWeight(height, gender);
        }

        // If stats were just updated
        if (weight > 0 && height > 0) {
            String bmi  = calcBMI(weight, height);
            String water = calcWater(weight);
            return bmi + "\n\n" + water;
        }

        return promptStats();
    }

    private static void extractAndStore(Context ctx, String text) {
        SharedPreferences.Editor e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        String lower = text.toLowerCase(Locale.US);

        // Weight: "75 kg", "75kg", "weigh 75"
        Matcher wm = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*kg").matcher(lower);
        if (wm.find()) e.putFloat(KEY_WEIGHT, Float.parseFloat(wm.group(1)));

        // Weight in lbs
        Matcher lbm = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:lbs?|pounds?)").matcher(lower);
        if (lbm.find()) e.putFloat(KEY_WEIGHT, Float.parseFloat(lbm.group(1)) * 0.453592f);

        // Height in cm
        Matcher hm = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*cm").matcher(lower);
        if (hm.find()) e.putFloat(KEY_HEIGHT, Float.parseFloat(hm.group(1)));

        // Height in m
        Matcher mm = Pattern.compile("(1\\.\\d+)\\s*m(?!i)").matcher(lower);
        if (mm.find()) e.putFloat(KEY_HEIGHT, Float.parseFloat(mm.group(1)) * 100);

        // Height in feet/inches: 5'10" or 5 feet 10
        Matcher fm = Pattern.compile("(\\d)\\s*(?:feet|ft|')\\s*(\\d{1,2})?(?:\\s*(?:inches|in|\"))?").matcher(lower);
        if (fm.find()) {
            int feet = Integer.parseInt(fm.group(1));
            int inches = fm.group(2) != null ? Integer.parseInt(fm.group(2)) : 0;
            e.putFloat(KEY_HEIGHT, feet * 30.48f + inches * 2.54f);
        }

        // Age
        Matcher am = Pattern.compile("(\\d+)\\s*(?:years?\\s*old|yr|yo)").matcher(lower);
        if (am.find()) e.putInt(KEY_AGE, Integer.parseInt(am.group(1)));

        // Gender
        if (lower.contains("female") || lower.contains("woman") || lower.contains("girl"))
            e.putString(KEY_GENDER, "female");
        else if (lower.contains("male") || lower.contains("man") || lower.contains("boy"))
            e.putString(KEY_GENDER, "male");

        // Activity level
        if (lower.contains("sedentary") || lower.contains("not active"))        e.putInt(KEY_ACT, 1);
        else if (lower.contains("lightly active") || lower.contains("light"))   e.putInt(KEY_ACT, 2);
        else if (lower.contains("moderately active") || lower.contains("moderate")) e.putInt(KEY_ACT, 3);
        else if (lower.contains("very active") || lower.contains("active"))     e.putInt(KEY_ACT, 4);
        else if (lower.contains("extremely active") || lower.contains("athlete")) e.putInt(KEY_ACT, 5);

        e.apply();
    }

    private static String calcBMI(float weightKg, float heightCm) {
        float hm  = heightCm / 100f;
        float bmi = weightKg / (hm * hm);
        String cat, emotion;
        if      (bmi < 18.5) { cat = "Underweight ⚠️";  emotion = "concerned"; }
        else if (bmi < 25.0) { cat = "Normal weight ✅"; emotion = "excited";  }
        else if (bmi < 30.0) { cat = "Overweight ⚠️";   emotion = "neutral";  }
        else                 { cat = "Obese ⚠️";        emotion = "concerned"; }
        return String.format(Locale.US,
            "[EMOTION:%s] **BMI: %.1f** — %s\n" +
            "Weight: %.0f kg  |  Height: %.0f cm\n" +
            "Healthy range: **18.5 – 24.9**, sir.",
            emotion, bmi, cat, weightKg, heightCm);
    }

    private static String calcTDEE(float weight, float height, int age, String gender, int activity) {
        // Mifflin-St Jeor equation
        double bmr;
        if ("female".equals(gender))
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
        else
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;

        double[] multipliers = {1.2, 1.375, 1.55, 1.725, 1.9};
        double tdee = bmr * multipliers[Math.min(activity - 1, 4)];
        double lose  = tdee - 500;
        double gain  = tdee + 300;

        String[] actLabels = {"Sedentary", "Lightly active", "Moderately active", "Very active", "Extremely active"};
        return String.format(Locale.US,
            "[EMOTION:excited] **Daily Calorie Needs, sir:**\n" +
            "Activity: %s\n" +
            "🔥 Maintenance: **%.0f kcal/day**\n" +
            "📉 Weight loss: **%.0f kcal/day** (−500)\n" +
            "📈 Weight gain: **%.0f kcal/day** (+300)",
            actLabels[Math.min(activity - 1, 4)], tdee, lose, gain);
    }

    private static String calcWater(float weightKg) {
        double litres = weightKg * 0.033;
        int glasses = (int) Math.round(litres / 0.25);
        return String.format(Locale.US,
            "[EMOTION:excited] 💧 **Daily Water Goal: %.1f litres** (~%d glasses of 250 ml), sir.\n" +
            "Drink more in hot weather or after exercise.",
            litres, glasses);
    }

    private static String calcIdealWeight(float heightCm, String gender) {
        // Devine formula
        float inches = (heightCm / 2.54f) - 60;
        float ideal  = "female".equals(gender) ? 45.5f + 2.3f * inches : 50f + 2.3f * inches;
        float low = ideal - 5, high = ideal + 5;
        return String.format(Locale.US,
            "[EMOTION:excited] ⚖️ **Ideal weight for your height (%.0f cm):**\n" +
            "**%.0f – %.0f kg** (%.0f kg target), sir.",
            heightCm, low, high, ideal);
    }

    private static String promptStats() {
        return "[EMOTION:neutral] Tell me your stats, sir. E.g.:\n" +
               "\"I am 75 kg, 175 cm, 28 years old, moderately active.\"";
    }
}
