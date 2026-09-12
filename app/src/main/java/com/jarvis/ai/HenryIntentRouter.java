package com.jarvis.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * H.E.N.R.Y. — Deterministic Intent Classification & Multi-Intent Task Graph Engine
 *
 * Implements strict, priority-ordered intent categorization with confidence scoring
 * to eliminate misrouting (e.g., prevents "A tic tac toe game" from triggering Sports Tracker,
 * or "Tell me something fascinating" from triggering Crypto/News).
 */
public class HenryIntentRouter {

    public enum IntentType {
        GAME_GENERATION(0.95f),
        CODE_GENERATION(0.95f),
        ULTRA_ORCHESTRATION(0.98f),
        DOCUMENT_CREATION(0.95f),
        SCRIPTWRITING(0.95f),
        VIDEO_STUDIO(0.95f),
        MATH_SOLVE(0.95f),
        OPINION_COMPARISON(0.90f),
        WITTY_RESPONSE(0.90f),
        CHEMISTRY(0.92f),
        SYSTEM_AUDIT(0.95f),
        AUTO_REPAIR(0.95f),
        BRAIN_MAP(0.95f),
        IMAGE_GENERATION(0.92f),
        SPORTS_LIVE(0.85f),
        FINANCE_PRICES(0.85f),
        NEWS(0.80f),
        WEATHER(0.85f),
        DEVICE_COMMAND(0.90f),
        CONVERSATION(0.70f);

        public final float baseConfidence;

        IntentType(float baseConfidence) {
            this.baseConfidence = baseConfidence;
        }
    }

    public static class ClassifiedIntent {
        public final IntentType type;
        public final float confidence;
        public final String cleanQuery;
        public final List<String> extractedEntities;

        public ClassifiedIntent(IntentType type, float confidence, String cleanQuery, List<String> extractedEntities) {
            this.type = type;
            this.confidence = confidence;
            this.cleanQuery = cleanQuery;
            this.extractedEntities = extractedEntities != null ? extractedEntities : new ArrayList<>();
        }

        @Override
        public String toString() {
            return type.name() + " (" + String.format(Locale.US, "%.2f", confidence) + ")";
        }
    }

    public static class TaskGraph {
        public final ClassifiedIntent primaryIntent;
        public final List<ClassifiedIntent> subIntents;
        public final boolean isMultiIntent;

        public TaskGraph(ClassifiedIntent primaryIntent, List<ClassifiedIntent> subIntents) {
            this.primaryIntent = primaryIntent;
            this.subIntents = subIntents != null ? subIntents : new ArrayList<>();
            this.isMultiIntent = !this.subIntents.isEmpty();
        }
    }

    /**
     * Primary entry point: Routes user input deterministically to the correct intent graph.
     */
    public static TaskGraph route(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new TaskGraph(new ClassifiedIntent(IntentType.CONVERSATION, 1.0f, "", null), null);
        }

        String raw = input.trim();
        String lower = raw.toLowerCase(Locale.US);
        List<ClassifiedIntent> candidates = new ArrayList<>();

        // ── 1. ULTRA Mode Orchestration (Compound Multi-Task Request) ─────────
        // Example: "Research Filipino adobo, create a 5-slide presentation, include images, cite sources in APA 7, and create an Excel ingredient list."
        if (isUltraCompoundRequest(lower)) {
            ClassifiedIntent ultraIntent = new ClassifiedIntent(IntentType.ULTRA_ORCHESTRATION, 0.99f, raw, extractUltraSubgoals(raw));
            List<ClassifiedIntent> subs = new ArrayList<>();
            if (lower.contains("presentation") || lower.contains("slide")) {
                subs.add(new ClassifiedIntent(IntentType.DOCUMENT_CREATION, 0.95f, "presentation", null));
            }
            if (lower.contains("excel") || lower.contains("spreadsheet") || lower.contains("ingredient list")) {
                subs.add(new ClassifiedIntent(IntentType.DOCUMENT_CREATION, 0.95f, "spreadsheet", null));
            }
            return new TaskGraph(ultraIntent, subs);
        }

        // ── 2. System Audit & Auto-Repair (Diagnostic HUD) ───────────────────
        if (isSystemAuditRequest(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.SYSTEM_AUDIT, 0.98f, raw, null), null);
        }
        if (isAutoRepairRequest(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.AUTO_REPAIR, 0.98f, raw, null), null);
        }

        // ── 3. Brain Map Navigation ──────────────────────────────────────────
        if (isBrainMapRequest(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.BRAIN_MAP, 0.98f, raw, null), null);
        }

        // ── 4. Chemistry & Science Core ──────────────────────────────────────
        if (isChemistryRequest(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.CHEMISTRY, 0.95f, raw, null), null);
        }

        // ── 5. Games & Interactive Game Logic ────────────────────────────────
        // MUST BE CHECKED BEFORE SportsTracker!
        // "A tic tac toe game", "snake game", "pong", "flappy bird", "play chess"
        if (isGameGenerationRequest(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.GAME_GENERATION, 0.98f, raw, null), null);
        }

        // ── 6. Code Generation & Programming Studio ──────────────────────────
        // "Write me a Python script", "build a rust function", "debug this java code"
        if (isCodeGenerationRequest(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.CODE_GENERATION, 0.96f, raw, null), null);
        }

        // ── 7. Scriptwriting & Screenplay Engine ──────────────────────────────
        // "Create a 5-minute video script", "YouTube script", "horror screenplay", "logline"
        if (isScriptwriterRequest(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.SCRIPTWRITING, 0.96f, raw, null), null);
        }

        // ── 8. Video & Animation Studio ──────────────────────────────────────
        // "Create a 5-minute video about Ancient Egypt", "12-minute animation"
        if (isVideoStudioRequest(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.VIDEO_STUDIO, 0.97f, raw, null), null);
        }

        // ── 9. Image Generation ──────────────────────────────────────────────
        // "Draw me a sunset", "generate image of a dragon"
        if (isImageGenerationRequest(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.IMAGE_GENERATION, 0.95f, raw, null), null);
        }

        // ── 10. Document / Slide / Spreadsheet Generation ─────────────────────
        // "Create a 5-slide presentation about...", "generate excel spreadsheet"
        if (HenryFileEngine.isCreationRequest(raw)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.DOCUMENT_CREATION, 0.96f, raw, null), null);
        }

        // ── 11. Math Engine ──────────────────────────────────────────────────
        // "Solve 2x + 5 = 15", "calculate 45 * 89", "derivative of x^3"
        if (HenryMathEngine.isMathProblem(raw)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.MATH_SOLVE, 0.97f, raw, null), null);
        }

        // ── 12. Opinion & Direct Comparison Engine ────────────────────────────
        // "Which is better?", "Compare iPhone and Samsung", "What's your take?", "Who wins?"
        if (HenryOpinionEngine.isOpinionQuery(raw)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.OPINION_COMPARISON, 0.95f, raw, null), null);
        }

        // ── 13. Witty & Banter Engine ─────────────────────────────────────────
        // "Give me a funny answer", "roast me", Tagalog witty queries "matinik ka sa boys pero bangus ka"
        if (HenryWittyEngine.isWittyQuery(raw)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.WITTY_RESPONSE, 0.94f, raw, null), null);
        }

        // ── 14. Explicit Live Sports Query ────────────────────────────────────
        // STRICT: only triggers when user specifically requests live match scores, standings, or fixtures!
        // Never triggers on games, creation, coding, or casual chat.
        if (isStrictSportsQuery(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.SPORTS_LIVE, 0.88f, raw, null), null);
        }

        // ── 15. Explicit Finance / Crypto / Forex Prices ──────────────────────
        // STRICT: only triggers on actual financial queries ("bitcoin price", "exchange rate USD to EUR", "apple stock")
        // Never triggers on words like "something" (which contains "eth") or casual questions!
        if (isStrictPriceQuery(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.FINANCE_PRICES, 0.88f, raw, null), null);
        }

        // ── 16. Explicit News Query ───────────────────────────────────────────
        if (isStrictNewsQuery(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.NEWS, 0.85f, raw, null), null);
        }

        // ── 17. Explicit Weather Query ────────────────────────────────────────
        if (isStrictWeatherQuery(lower)) {
            return new TaskGraph(new ClassifiedIntent(IntentType.WEATHER, 0.88f, raw, null), null);
        }

        // ── 18. Default Conversational Intelligence ───────────────────────────
        // "Tell me something fascinating", "Hello", "Explain quantum physics", etc.
        return new TaskGraph(new ClassifiedIntent(IntentType.CONVERSATION, 0.80f, raw, null), null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DETERMINISTIC MATCHER IMPLEMENTATIONS
    // ──────────────────────────────────────────────────────────────────────────

    private static boolean isUltraCompoundRequest(String t) {
        int complexActions = 0;
        if (t.contains("research") || t.contains("investigate") || t.contains("study")) complexActions++;
        if (t.contains("presentation") || t.contains("slide") || t.contains("pptx")) complexActions++;
        if (t.contains("excel") || t.contains("spreadsheet") || t.contains("xlsx") || t.contains("table") || t.contains("ingredient list")) complexActions++;
        if (t.contains("cite") || t.contains("citation") || t.contains("apa 7") || t.contains("sources")) complexActions++;
        if (t.contains("image") || t.contains("photo") || t.contains("diagram")) complexActions++;
        if (t.contains("ultra mode") || t.startsWith("ultra:")) return true;
        return complexActions >= 3;
    }

    private static List<String> extractUltraSubgoals(String raw) {
        List<String> goals = new ArrayList<>();
        String[] parts = raw.split("[,;]|\\band\\b");
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) goals.add(trimmed);
        }
        return goals;
    }

    private static boolean isGameGenerationRequest(String t) {
        if (t.contains("tic tac toe") || t.contains("tictactoe") || t.contains("noughts and crosses")) return true;
        if (t.contains("snake game") || t.contains("flappy bird") || t.contains("pong game") || t.contains("tetris")) return true;
        if (t.matches(".*\\b(make|build|create|code|program|develop)\\s+(a|an)?\\s*(game|tic tac toe|snake|puzzle|quiz).*")) return true;
        if (t.matches(".*\\b(play|game of)\\s+(tic tac toe|chess|checkers|hangman|trivia|riddle).*")) return true;
        return false;
    }

    private static boolean isCodeGenerationRequest(String t) {
        if (t.contains("python script") || t.contains("write code") || t.contains("write a program") || t.contains("write a script")) return true;
        if (t.matches(".*\\b(write|create|code|build|generate)\\s+(me\\s+)?(a|an)?\\s*(python|javascript|java|c\\+\\+|html|css|sql|rust|kotlin|swift|bash|powershell|typescript)\\s*(script|function|program|app|class|code|snippet)?.*")) return true;
        if (t.matches(".*\\b(debug|fix|refactor|compile|lint)\\s+(this|my)?\\s*(code|script|function|bug|error).*")) return true;
        return false;
    }

    private static boolean isScriptwriterRequest(String t) {
        if (t.contains("scriptwriter") || t.contains("screenplay") || t.contains("logline") || t.contains("character bible")) return true;
        if (t.matches(".*\\b(write|create|generate)\\s+(a|an)?\\s*(youtube script|video script|movie script|documentary script|horror script|comedy script|short film script|podcast script|voice over script).*")) return true;
        if (t.contains("three-act structure") || t.contains("3-act structure") || t.contains("save the cat")) return true;
        return false;
    }

    private static boolean isVideoStudioRequest(String t) {
        if (t.contains("video studio") || t.contains("animation studio")) return true;
        boolean creationVerb = t.matches(".*\\b(create|make|produce|generate|render|animate)\\b.*");
        boolean videoNoun = t.matches(".*\\b(video|documentary|animation|animated|movie|film|clip)\\b.*");
        boolean duration = t.matches(".*\\b\\d+\\s*(second|seconds|sec|secs|minute|minutes|min|mins)\\b.*");
        // Allows either "create a 30-second video" or "make a video ... for 30 sec".
        if (creationVerb && videoNoun && duration) return true;
        if (t.matches(".*\\b(turn this script into a video|generate video from script|render video|video storyboard).*")) return true;
        return false;
    }

    private static boolean isImageGenerationRequest(String t) {
        return ImageGenerator.isImageCommand(t);
    }

    private static boolean isSystemAuditRequest(String t) {
        return t.equals("audit") || t.contains("system audit") || t.contains("run audit")
                || t.contains("system diagnostic") || t.contains("run diagnostics")
                || t.contains("health check") || t.contains("self test") || t.contains("subsystem audit");
    }

    private static boolean isAutoRepairRequest(String t) {
        return t.equals("repair") || t.contains("auto repair") || t.contains("auto-repair")
                || t.contains("fix system") || t.contains("repair subsystems") || t.contains("heal system");
    }

    private static boolean isBrainMapRequest(String t) {
        return t.equals("brain") || t.equals("brain map") || t.contains("open brain")
                || t.contains("show brain map") || t.contains("neural map") || t.contains("mind map");
    }

    private static boolean isChemistryRequest(String t) {
        if (t.equals("chemistry") || t.contains("periodic table") || t.contains("molecular structure")
                || t.contains("chemical mixer") || t.contains("vsepr") || t.contains("stoichiometry")) return true;
        if (t.matches(".*\\b(molecule|compound|chemical formula|elemental analysis)\\b.*")) return true;
        // Common exact formulas when queried specifically
        if (t.matches(".*\\b(h2o|co2|ch4|nh3|nacl|c2h5oh|caffeine|h2so4|glucose)\\b.*") && !t.contains("script") && !t.contains("presentation")) return true;
        return false;
    }

    private static boolean isStrictSportsQuery(String t) {
        // Prevent matching coding, gaming, recipes, or casual questions
        if (t.contains("game") && (t.contains("tic tac toe") || t.contains("snake") || t.contains("code") || t.contains("make") || t.contains("play"))) return false;
        if (t.contains("adobo") || t.contains("cook") || t.contains("recipe")) return false;
        
        // Positive sports queries
        if (t.contains("premier league") || t.contains("champions league") || t.contains("la liga") || t.contains("serie a")) return true;
        if (t.contains("nba scores") || t.contains("nba standings") || t.contains("football score") || t.contains("soccer score")) return true;
        if (t.contains("live score") || t.contains("match score") || t.contains("sports score")) return true;
        if ((t.contains("arsenal") || t.contains("chelsea") || t.contains("real madrid") || t.contains("barcelona") || t.contains("manchester united") || t.contains("lakers"))
                && (t.contains("score") || t.contains("match") || t.contains("game") || t.contains("result") || t.contains("standing") || t.contains("fixture"))) return true;
        return false;
    }

    private static boolean isStrictPriceQuery(String t) {
        // Never match "something" (which contains "eth")
        if (t.contains("something") && !t.contains("price") && !t.contains("stock")) return false;
        if (t.contains("bitcoin price") || t.contains("btc price") || t.contains("crypto price") || t.contains("ethereum price") || t.contains("solana price")) return true;
        if (t.matches(".*\\b(stock price of|shares of|market cap of|current ticker)\\b.*")) return true;
        if (t.matches(".*\\b\\d+\\s*(usd|eur|gbp|aed|jpy|php|inr)\\s*to\\s*(usd|eur|gbp|aed|jpy|php|inr)\\b.*")) return true;
        if (t.contains("exchange rate") && (t.contains("usd") || t.contains("eur") || t.contains("currency"))) return true;
        return false;
    }

    private static boolean isStrictNewsQuery(String t) {
        if (t.contains("something fascinating") || t.contains("tell me about")) return false;
        return t.equals("news") || t.equals("latest news") || t.equals("headlines")
                || t.equals("top headlines") || t.equals("world news") || t.equals("tech news")
                || t.startsWith("breaking news") || t.equals("read the news");
    }

    private static boolean isStrictWeatherQuery(String t) {
        if (t.contains("fascinating") || t.contains("story")) return false;
        return t.equals("weather") || t.startsWith("weather in ") || t.startsWith("forecast in ")
                || t.contains("temperature today") || t.contains("is it raining");
    }
}
