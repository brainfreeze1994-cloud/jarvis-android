package com.jarvis.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On-device instant calculator — no AI call needed.
 * Handles: basic math, percentages, square root, power, unit conversion.
 */
public class Calculator {

    /**
     * Returns formatted result string, or null if not a math query.
     */
    public static String evaluate(String text) {
        String t = text.toLowerCase().trim();

        // ── Percentage ────────────────────────────────────────────────────────
        // "X% of Y" or "what is X percent of Y"
        Matcher pm = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%\\s*(?:of)\\s*(\\d+(?:\\.\\d+)?)").matcher(t);
        if (pm.find()) {
            double pct = Double.parseDouble(pm.group(1));
            double val = Double.parseDouble(pm.group(2));
            double result = pct / 100.0 * val;
            return "[EMOTION:neutral] " + pct + "% of " + nice(val) + " = **" + nice(result) + "**, sir.";
        }

        // ── Square root ───────────────────────────────────────────────────────
        Matcher sqm = Pattern.compile("(?:sqrt|square root of|√)\\s*(\\d+(?:\\.\\d+)?)").matcher(t);
        if (sqm.find()) {
            double n = Double.parseDouble(sqm.group(1));
            double result = Math.sqrt(n);
            return "[EMOTION:neutral] √" + nice(n) + " = **" + nice(result) + "**, sir.";
        }

        // ── Power ─────────────────────────────────────────────────────────────
        Matcher pwm = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:\\^|\\*\\*|to the power of|squared|cubed)\\s*(\\d+(?:\\.\\d+)?)").matcher(t);
        if (pwm.find()) {
            double base = Double.parseDouble(pwm.group(1));
            double exp  = Double.parseDouble(pwm.group(2));
            double result = Math.pow(base, exp);
            return "[EMOTION:neutral] " + nice(base) + "^" + nice(exp) + " = **" + nice(result) + "**, sir.";
        }

        // ── Basic arithmetic: detect "X op Y" ────────────────────────────────
        // operations: +, -, *, /, divided by, times, plus, minus, multiplied by
        String normalized = t
            .replace("divided by", "/").replace("times", "*")
            .replace("multiplied by", "*").replace("plus", "+")
            .replace("minus", "-").replace("x", "*")
            .replaceAll("[^0-9+\\-*/.()]", " ").trim();

        // Remove multiple spaces
        normalized = normalized.replaceAll("\\s+", " ").trim();

        // Simple two-operand math
        Matcher am = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(-?\\d+(?:\\.\\d+)?)").matcher(normalized);
        if (am.find()) {
            double a  = Double.parseDouble(am.group(1));
            String op = am.group(2);
            double b  = Double.parseDouble(am.group(3));
            double result;
            switch (op) {
                case "+": result = a + b; break;
                case "-": result = a - b; break;
                case "*": result = a * b; break;
                case "/":
                    if (b == 0) return "[EMOTION:amused] Division by zero, sir. Even I have limits.";
                    result = a / b; break;
                default:  return null;
            }
            String opStr = op.equals("+") ? "+" : op.equals("-") ? "−"
                         : op.equals("*") ? "×" : "÷";
            return "[EMOTION:neutral] " + nice(a) + " " + opStr + " " + nice(b) +
                   " = **" + nice(result) + "**, sir.";
        }

        return null;
    }

    // ── Unit Converter ────────────────────────────────────────────────────────
    /**
     * Returns a formatted conversion string or null if not a conversion query.
     */
    public static String convert(String text) {
        String t = text.toLowerCase().trim();
        if (!t.contains("convert") && !t.contains(" to ") && !t.contains(" in ")) return null;

        // Grab number
        Matcher nm = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(t);
        if (!nm.find()) return null;
        double value = Double.parseDouble(nm.group(1));

        // Length
        if (t.contains("km") && (t.contains("mile") || t.contains("mi")))
            return fmt(value, "km", value * 0.621371, "miles");
        if ((t.contains("mile") || t.contains("mi")) && t.contains("km"))
            return fmt(value, "miles", value * 1.60934, "km");
        if (t.contains("meter") && t.contains("feet") || t.contains("metre") && t.contains("feet"))
            return fmt(value, "meters", value * 3.28084, "feet");
        if (t.contains("feet") && (t.contains("meter") || t.contains("metre")))
            return fmt(value, "feet", value / 3.28084, "meters");
        if (t.contains("inch") && t.contains("cm"))
            return fmt(value, "inches", value * 2.54, "cm");
        if (t.contains("cm") && t.contains("inch"))
            return fmt(value, "cm", value / 2.54, "inches");

        // Weight
        if (t.contains("kg") && (t.contains("lb") || t.contains("pound")))
            return fmt(value, "kg", value * 2.20462, "lbs");
        if ((t.contains("lb") || t.contains("pound")) && t.contains("kg"))
            return fmt(value, "lbs", value / 2.20462, "kg");
        if (t.contains("gram") && (t.contains("oz") || t.contains("ounce")))
            return fmt(value, "grams", value * 0.035274, "oz");

        // Temperature
        if ((t.contains("celsius") || t.contains("°c")) && (t.contains("fahrenheit") || t.contains("°f")))
            return fmt(value, "°C", value * 9.0/5 + 32, "°F");
        if ((t.contains("fahrenheit") || t.contains("°f")) && (t.contains("celsius") || t.contains("°c")))
            return fmt(value, "°F", (value - 32) * 5.0/9, "°C");
        if ((t.contains("celsius") || t.contains("°c")) && t.contains("kelvin"))
            return fmt(value, "°C", value + 273.15, "K");

        // Currency (static rates — AED base, Dubai-relevant)
        if (t.contains("usd") && t.contains("aed"))  return fmt(value, "USD", value * 3.6725, "AED");
        if (t.contains("aed") && t.contains("usd"))  return fmt(value, "AED", value / 3.6725, "USD");
        if (t.contains("eur") && t.contains("aed"))  return fmt(value, "EUR", value * 4.00, "AED");
        if (t.contains("aed") && t.contains("eur"))  return fmt(value, "AED", value / 4.00, "EUR");
        if (t.contains("gbp") && t.contains("aed"))  return fmt(value, "GBP", value * 4.65, "AED");
        if (t.contains("aed") && t.contains("gbp"))  return fmt(value, "AED", value / 4.65, "GBP");
        if (t.contains("php") && t.contains("aed"))  return fmt(value, "PHP", value * 0.065, "AED");
        if (t.contains("aed") && t.contains("php"))  return fmt(value, "AED", value / 0.065, "PHP");
        if (t.contains("usd") && t.contains("php"))  return fmt(value, "USD", value * 56.5, "PHP");
        if (t.contains("php") && t.contains("usd"))  return fmt(value, "PHP", value / 56.5, "USD");
        if (t.contains("inr") && t.contains("aed"))  return fmt(value, "INR", value * 0.044, "AED");
        if (t.contains("aed") && t.contains("inr"))  return fmt(value, "AED", value / 0.044, "INR");

        // Speed
        if (t.contains("kmh") && t.contains("mph") || t.contains("km/h") && t.contains("mph"))
            return fmt(value, "km/h", value * 0.621371, "mph");
        if (t.contains("mph") && (t.contains("kmh") || t.contains("km/h")))
            return fmt(value, "mph", value * 1.60934, "km/h");

        return null;
    }

    private static String fmt(double from, String fromU, double to, String toU) {
        return "[EMOTION:neutral] " + nice(from) + " " + fromU + " = **" + nice(to) + " " + toU + "**, sir.";
    }

    private static String nice(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v) && Math.abs(v) < 1e9)
            return String.valueOf((long) v);
        return String.format(java.util.Locale.US, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
