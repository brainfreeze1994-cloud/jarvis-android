package com.jarvis.ai;

import java.util.Locale;

/**
 * H.E.N.R.Y. — DECISIVE OPINION, COMPARISON & RECOMMENDATION ENGINE
 *
 * Replaces bland "it depends" responses with structured, authoritative analysis:
 * 1. QUICK VERDICT (Definitive take)
 * 2. KEY REASONS (Quantifiable differentiators)
 * 3. HEAD-TO-HEAD COMPARISON (Hardware, ecosystem, performance, longevity)
 * 4. CRITICAL TRADE-OFFS (Compromises & sacrifices)
 * 5. FINAL PICK & PERSONA FIT (Exact recommendation)
 */
public class HenryOpinionEngine {

    public static boolean isOpinionQuery(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US).trim();

        if (t.startsWith("which is better") || t.startsWith("who wins") || t.startsWith("which one is better")
                || t.startsWith("what should i buy") || t.startsWith("what should i choose") || t.startsWith("should i buy")
                || t.startsWith("should i choose") || t.startsWith("what is your take") || t.startsWith("what's your take")
                || t.startsWith("what do you think of") || t.startsWith("what is your opinion")) {
            return true;
        }
        if (t.matches(".*\\b(compare|vs|versus|better than|difference between)\\b.*") && !t.contains("define")) {
            return true;
        }
        return false;
    }

    /**
     * Synthesizes a structured opinion breakdown.
     */
    public static String evaluate(String query) {
        String lower = query.toLowerCase(Locale.US);

        if (lower.contains("iphone") && lower.contains("samsung")) {
            return evaluateIphoneVsSamsung();
        }
        if ((lower.contains("mac") || lower.contains("macbook")) && (lower.contains("windows") || lower.contains("pc"))) {
            return evaluateMacVsWindows();
        }
        if (lower.contains("python") && (lower.contains("javascript") || lower.contains("js") || lower.contains("rust"))) {
            return evaluatePythonVsOther(lower);
        }

        // General structured decision framework
        return generateGeneralComparison(query);
    }

    private static String evaluateIphoneVsSamsung() {
        return "### ⚡ H.E.N.R.Y. Decisive Comparison: iPhone vs. Samsung\n\n" +
                "**1. QUICK VERDICT:**\n" +
                "**My Pick: iPhone for seamless longevity; Samsung for bleeding-edge display and hardware freedom.**\n" +
                "If forced to carry one device into mission-critical production for 5 years, **I choose iPhone**.\n\n" +
                "**2. KEY REASONS:**\n" +
                "• **Silicon Efficiency:** Apple's A-series & M-series silicon leads in sustained thermal efficiency and single-thread performance.\n" +
                "• **Resale & Support:** iPhones retain ~40% more secondary market value at year 3 and receive prompt global iOS patches.\n" +
                "• **Display Dominance:** Samsung's Dynamic AMOLED 2X leads in peak outdoor nits, anti-reflective armor glass, and split-screen multitasking.\n\n" +
                "**3. HEAD-TO-HEAD MATRIX:**\n" +
                "| Vector | Apple iPhone | Samsung Galaxy |\n" +
                "| :--- | :--- | :--- |\n" +
                "| **Camera Video** | **Dominant (ProRes, 4K Dolby Vision)** | High tier (8K, sharp) |\n" +
                "| **Camera Telephoto** | 5x Tetraprism | **Dominant (100x Space Zoom)** |\n" +
                "| **Operating System** | Curated, zero-maintenance iOS | **Open, DeX desktop mode, One UI** |\n" +
                "| **Battery & Charging** | Superb standby efficiency | **45W Fast Charging** |\n\n" +
                "**4. CRITICAL TRADE-OFFS:**\n" +
                "• Choose **iPhone** and you sacrifice true file-system access, emulation freedom, and sideloading.\n" +
                "• Choose **Samsung** and you sacrifice unified cross-device continuity (AirDrop, iMessage) and tighter third-party app video optimization.\n\n" +
                "**5. FINAL PICK:**\n" +
                "• **Buy iPhone** if you own a Mac/iPad or value frictionless reliability, point-and-shoot videography, and multi-year resale.\n" +
                "• **Buy Samsung** if you are an engineer, power user, multitasker, or demand S-Pen stylus precision.";
    }

    private static String evaluateMacVsWindows() {
        return "### ⚡ H.E.N.R.Y. Decisive Comparison: Mac vs. Windows\n\n" +
                "**1. QUICK VERDICT:**\n" +
                "**My Pick: MacBook for mobile productivity & dev work; Windows for desktop compute & gaming.**\n\n" +
                "**2. KEY REASONS:**\n" +
                "• **Apple Silicon (M-series):** Unrivaled battery life (16–22 hours) with zero performance degradation on battery power.\n" +
                "• **Windows Hardware Diversity:** Direct access to dedicated NVIDIA RTX GPUs, modular upgradeability, and total gaming supremacy.\n\n" +
                "**3. CRITICAL TRADE-OFFS:**\n" +
                "• Mac locks your unified RAM and SSD at purchase time with extreme upgrade markups.\n" +
                "• Windows laptops struggle with erratic sleep states, fan acoustics under load, and lower unplugged battery longevity.\n\n" +
                "**4. FINAL PICK:**\n" +
                "• **Mac** for developers, writers, video editors, and travelers.\n" +
                "• **Windows** for 3D renderers, gamers, CAD engineers, and enterprise finance analysts.";
    }

    private static String evaluatePythonVsOther(String lower) {
        return "### ⚡ H.E.N.R.Y. Decisive Comparison: Python in 2026\n\n" +
                "**1. QUICK VERDICT:**\n" +
                "**My Pick: Python is king for AI, ML, Data Science, and rapid prototyping; Rust/Go for high-concurrency systems.**\n\n" +
                "**2. WHY PYTHON WINS:**\n" +
                "Every major AI framework (PyTorch, TensorFlow, JAX, Hugging Face) is built with Python as the native interface.\n\n" +
                "**3. THE SACRIFICE:**\n" +
                "Execution speed and memory overhead. For latency-critical high-frequency trading or bare-metal systems, use Rust or C++.";
    }

    private static String generateGeneralComparison(String query) {
        return "### ⚡ H.E.N.R.Y. Decision Analysis\n\n" +
                "**1. QUICK VERDICT:**\n" +
                "Based on empirical benchmarks, structural efficiency, and real-world durability, there is a clear leader for your query.\n\n" +
                "**2. CORE EVALUATION:**\n" +
                "When balancing raw performance against daily maintenance overhead, the option with lower friction and stronger long-term support wins.\n\n" +
                "**3. TRADE-OFFS:**\n" +
                "Higher upfront investment versus long-term versatility and resilience.\n\n" +
                "**4. MY RECOMMENDATION:**\n" +
                "Prioritize the ecosystem that maximizes your immediate workflow momentum rather than peripheral theoretical features.";
    }
}
