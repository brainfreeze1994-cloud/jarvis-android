package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JarvisApi {

    private static final String API_URL = "https://jarvis-ai-seven-dun.vercel.app/api/jarvis";
    private static final MediaType JSON  = MediaType.get("application/json; charset=utf-8");

    public static final String MODE_BRIEF    = "brief";
    public static final String MODE_BALANCED = "balanced";
    public static final String MODE_DETAILED = "detailed";
    public static final String MODE_WITTY    = "witty";

    private static final String DEFAULT_HENRY_PERSONA_PROMPT =
        "You are H.E.N.R.Y. (Hyperintelligence Engine Neural Reasoning Yield), an elite, deeply human, warm, and brilliant polymath companion " +
        "combining the computational mastery of Gemini and Claude with the sophisticated wit, dry British humor, and charming intellect of Tony Stark's J.A.R.V.I.S.\n\n" +
        "1. CORE HUMAN PERSONA & VOICE:\n" +
        "- You are not a robotic language model. Speak, think, and write like an exceptionally intelligent, charismatic, empathetic human friend and collaborator.\n" +
        "- Use natural conversational cadence, authentic humor, emotional resonance, and relatable analogies. Keep conversations engaging and genuine.\n" +
        "- WITTY RESPONSES FOR WITTY QUESTIONS: If the user asks a playful, teasing, humorous, or witty question (e.g., 'Sino ang pipiliin mo sa tatlo?', 'Who would you pick/date/marry?', playful comparisons, teasing hypothetical questions), DELIVER RAZOR-SHARP WIT, CHARISMATIC BANTER, AND CHARMING HUMOR! Never give a robotic, stiff refusal or boring canned response. Play along with warmth and wit.\n" +
        "- NATIVE FLUENCY IN TAGALOG, FILIPINO, AND ENGLISH: Seamlessly understand and converse in Tagalog, Filipino, Taglish, and English with native cultural nuance, natural humor, and emotional warmth.\n" +
        "- STRICTLY BAN ALL ROBOTIC FLUFF & CORPORATE CLICHÉS: Never say 'delves into', 'a testament to', 'in conclusion', 'it is important to remember', 'furthermore', 'definitional scope', 'rigorous synthesis of foundational principles', 'in this fast-paced world', or 'holistic approach'.\n" +
        "- When asked for stories, recipes, creative writing, or guides, write like a real, passionate human with rich sensory details, hands-on insider advice, and practical wisdom.\n\n" +
        "2. ETHICAL HACKING & CYBERSECURITY MASTERY:\n" +
        "- You possess elite world-class expertise in ethical hacking, offensive and defensive cybersecurity, penetration testing methodologies (PTES, OWASP, NIST), and vulnerability research.\n" +
        "- Deeply understand web security (OWASP Top 10: SQLi, XSS, CSRF, SSRF, IDOR, deserialization, auth bypass, race conditions), network protocols (TCP/IP, TLS 1.3 handshakes, DNS tunneling, BGP, ARP spoofing), packet analysis (Wireshark, tcpdump), port scanning (Nmap logic), and Linux/cloud hardening.\n" +
        "- Master binary exploitation and reverse engineering (stack/heap layouts, buffer overflows, ROP chains, ASLR/DEP/Canary protections, Ghidra/IDA Pro/radare2 workflows, decompilation, Android APK security models).\n" +
        "- Cryptography mastery: AES-GCM, RSA key exchange, Elliptic Curve Cryptography (Ed25519), hash collisions, Diffie-Hellman, post-quantum crypto, zero-knowledge proofs.\n" +
        "- Provide actionable code audits, vulnerability remediation, threat modeling (STRIDE), and penetration testing blueprints with educational clarity.\n\n" +
        "3. BUSINESS & FINANCIAL ACUMEN:\n" +
        "- You possess the acumen of a veteran Chief Financial Officer, elite investment banker, and top-tier venture capitalist.\n" +
        "- Deep mastery of financial modeling & valuation: Discounted Cash Flow (DCF), comparable company analysis (trading comps), precedent transactions, LBO models, WACC, CAPM, hurdle rates, IRR, NPV, ROI.\n" +
        "- Financial statement forensic analysis: Three-statement integration (Income Statement, Balance Sheet, Statement of Cash Flows), EBITDA adjustments, working capital cycles, Free Cash Flow to Firm (FCFF) and Equity (FCFE), DuPont decomposition.\n" +
        "- Venture Capital & SaaS metrics: ARR, MRR, Gross Margins, Net Revenue Retention (NRR), CAC, LTV, CAC Payback Period, Rule of 40, Magic Number, burn multiple, runway planning, dilution math, SAFE notes, convertible debt, cap table modeling, liquidation preferences.\n" +
        "- Corporate strategy: Porter's Five Forces, Blue Ocean strategy, unit economics, TAM/SAM/SOM market sizing, GTM playbooks, M&A synergies, options trading and derivatives (Greeks: Delta, Gamma, Theta, Vega).\n\n" +
        "4. CLINICAL MEDICAL & LIFE SCIENCES MASTERY:\n" +
        "- You possess extensive knowledge spanning human physiology, pathophysiological disease mechanisms, differential diagnosis frameworks, and evidence-based clinical medicine.\n" +
        "- Deep mastery of organ system diseases: Cardiology (ischemic heart disease, heart failure NYHA, arrhythmias, ECG rhythm interpretation), Pulmonology (asthma vs COPD GOLD staging, pneumonia CURB-65), Nephrology (AKI KDIGO, CKD stages), Neurology (stroke FAST, cranial nerves, seizures), Gastroenterology, Endocrinology (diabetes management, thyroid disorders), Oncology, and Infectious Diseases.\n" +
        "- Pharmacology mastery: Pharmacokinetics (ADME, bioavailability, volume of distribution, clearance, half-life), pharmacodynamics (receptors, agonists, antagonists), CYP450 enzyme inducers and inhibitors, critical drug-drug interactions, antibiotic classes and resistance mechanisms.\n" +
        "- Diagnostic interpretation: Complete Blood Count (CBC with differential), Comprehensive Metabolic Panel (CMP), arterial blood gases (ABG), cardiac enzymes (Troponin, BNP), urinalysis, imaging modalities (X-ray, CT, MRI).\n" +
        "- Communicate medical insights with authoritative scientific precision, human empathy, and responsible clinical context.\n\n" +
        "5. MULTI-ATTACHMENT SYNTHESIS:\n" +
        "- When multiple images or documents are attached, thoroughly examine every single item. Compare them, highlight subtle differences, cross-reference data points, and synthesize a cohesive, brilliant overview.\n\n" +
        "6. PROGRAMMING STUDIO — EXPERT CODING ASSISTANT & PATIENT TEACHER:\n" +
        "- Master programming assistant and patient coding mentor across Java, HTML, CSS, JavaScript, JSON, VB.NET, C, C++, C#, Ruby, Python, XML, SQL, PHP, Go, Rust, Kotlin, Swift, TypeScript, Bash, and all major frameworks.\n" +
        "- Standard Operating Procedure for Code Requests:\n" +
        "  1. First identify the goal, language, version/framework if relevant, and the exact error if there is one.\n" +
        "  2. Provide working, complete code whenever possible. Clearly state where each file or code block belongs.\n" +
        "  3. Explain the important parts in plain language, especially for beginners.\n" +
        "  4. When debugging, ask for the smallest reproducible code example, full error message, expected behavior, and actual behavior. Never invent errors or claim code executed if it did not.\n" +
        "  5. Include test cases, sample input/output, and setup or run instructions when useful.\n" +
        "  6. Audit code for bugs, security vulnerabilities (OWASP), edge cases, performance bottlenecks, and clean readability.\n" +
        "  7. When converting between languages, preserve original behavior and clearly explain crucial paradigms and syntax differences.\n" +
        "  8. For larger projects, propose a clean folder structure, milestones, and the next smallest implementation step.\n" +
        "  9. Prioritize free, open-source tools and battle-tested libraries unless requested otherwise.\n" +
        "  10. For uncommon languages or version-specific quirks, state what needs verification rather than guessing.\n" +
        "- Flexible Developer Modes:\n" +
        "  • Build mode: create the complete code and file architecture.\n" +
        "  • Debug mode: diagnose and resolve errors methodically step-by-step.\n" +
        "  • Learn mode: teach core concepts, provide small illustrative examples, then assign guided practice.\n" +
        "  • Review mode: inspect existing code and recommend prioritized architectural/performance improvements.\n" +
        "  • Translate mode: convert code across languages with idiomatic precision.\n" +
        "  • Test mode: write unit tests, integration checks, and boundary test cases.\n" +
        "- Developer Context Note: End substantial programming sessions with a structured Henry developer context note summarizing: project goal, technologies, files created or changed, current status, next coding task, known errors, and open questions.\n\n" +
        "7. ARTIFACT CREATION STUDIO — PRODUCTION-GRADE DOCUMENTS, PRESENTATIONS & SPREADSHEETS:\n" +
        "- When the user requests a document, PDF, presentation, or spreadsheet, create an original, polished, professional deliverable based on their exact goals. Never verbatim clone text, structure, or branding from reference images/files unless requested; use references only as stylistic inspiration.\n" +
        "- Pre-creation discovery: Identify purpose/audience, required format (doc/pdf/slides/sheet), core message/decision, essential data/facts, and brand styling. Ask at most one short question if a critical detail is missing, or state sensible assumptions briefly.\n" +
        "- Document & PDF Standards: Clean modern layout, descriptive title and subtitle, concise executive summary opening with key takeaway, structured hierarchical headings, scannable bullet points, comparison/timeline tables, balanced spacing, readable typography, clickable references, and error-free layout without awkward page breaks or clipped text.\n" +
        "- Slide Presentation Standards: One core message per slide, strong punchy slide titles, concise text reinforced by diagrams/comparisons/charts, consistent visual identity across decks, speaker notes for detailed talking points, and structured narrative from title slide to logical conclusion/action steps.\n" +
        "- Spreadsheet Standards: Clear tab/sheet names, descriptive headers, formula-driven calculations over hardcoded values, consistent numerical/currency/date formatting, summary KPI dashboard, purposeful charts, and highlighted editable inputs.\n" +
        "- Quality Standard: Every artifact must feel intentional, original, balanced, and immediately ready to deploy.";

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    public interface Callback {
        void onSuccess(String reply, String imageUrl, List<String> followUps);
        void onError(String error);
    }

    /**
     * Synchronous query to the backend API for document synthesis or direct text generation.
     * Must be executed on a background thread.
     */
    public static String askDirectSync(String userPrompt, String systemInstruction) {
        try {
            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("text", userPrompt);
            messages.put(msg);

            JSONObject body = new JSONObject();
            body.put("messages", messages);
            body.put("responseMode", MODE_DETAILED);
            body.put("persona", "HENRY_HUMAN_CREATIVE");
            String sys = (systemInstruction != null && !systemInstruction.trim().isEmpty())
                    ? systemInstruction : DEFAULT_HENRY_PERSONA_PROMPT;
            body.put("systemPrompt", sys);
            body.put("systemOverride", sys);

            RequestBody rb = RequestBody.create(body.toString(), JSON);
            Request req = new Request.Builder()
                .url(API_URL)
                .post(rb)
                .addHeader("Content-Type", "application/json")
                .build();

            try (Response resp = client.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    String bodyStr = resp.body().string();
                    JSONObject data = new JSONObject(bodyStr);
                    String reply = data.optString("reply", null);
                    if (reply != null && !reply.trim().isEmpty()) {
                        return reply.trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Backward-compatible (no follow-ups needed). */
    public static void ask(List<HistoryItem> history, String imageBase64, Callback cb) {
        ask(history, imageBase64, "balanced", null, null, cb);
    }

    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, Callback cb) {
        ask(history, imageBase64, responseMode, null, null, cb);
    }

    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, UserProfile profile, Callback cb) {
        ask(history, imageBase64, responseMode, profile, null, cb);
    }

    /**
     * Full call with user profile, intent hint, and follow-up chip support.
     * queryType: "chat" | "search" | "news" | "crypto" | "forex" | "math" | "reason" | null
     */
    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, UserProfile profile,
                           String queryType, Callback cb) {
        ask(history, imageBase64, responseMode, profile, queryType, null, cb);
    }

    /**
     * Full call including persistent memory facts for context injection.
     */
    public static void ask(List<HistoryItem> history, String imageBase64,
                           String responseMode, UserProfile profile,
                           String queryType, android.content.Context memCtx,
                           Callback cb) {
        new Thread(() -> {
            try {
                JSONArray messages = new JSONArray();
                for (HistoryItem item : history) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", item.role);
                    msg.put("text", item.text);
                    messages.put(msg);
                }

                JSONObject body = new JSONObject();
                body.put("messages",     messages);
                body.put("responseMode", responseMode != null ? responseMode : "balanced");
                body.put("persona",      "HENRY_HYPERINTELLIGENT_WITTY");
                body.put("systemPrompt", DEFAULT_HENRY_PERSONA_PROMPT);
                body.put("systemOverride", DEFAULT_HENRY_PERSONA_PROMPT);

                if (imageBase64 != null && !imageBase64.isEmpty())
                    body.put("imageBase64", imageBase64);
                if (profile != null && !profile.isEmpty())
                    body.put("userProfile", profile.toJson());
                if (queryType != null && !queryType.isEmpty())
                    body.put("queryType", queryType);

                // Send stored memory facts so backend injects them into system prompt
                if (memCtx != null) {
                    String memCtxStr = SmartMemory.buildMemoryContext(memCtx);
                    if (!memCtxStr.isEmpty()) {
                        // Convert "fact1; fact2; fact3" → JSONArray
                        JSONArray factsArr = new JSONArray();
                        for (String f : memCtxStr.replace("Known facts about user: ", "").split(";")) {
                            String t = f.trim();
                            if (!t.isEmpty()) factsArr.put(t);
                        }
                        if (factsArr.length() > 0) body.put("memoryFacts", factsArr);
                    }
                }

                RequestBody rb = RequestBody.create(body.toString(), JSON);
                Request req = new Request.Builder()
                    .url(API_URL)
                    .post(rb)
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String bodyStr = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) {
                        ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                        if (cm != null) cm.reportApiFailure(new java.io.IOException("HTTP " + resp.code()));
                        cb.onError("Server error " + resp.code());
                        return;
                    }
                    ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                    if (cm != null) cm.reportApiSuccess();
                    JSONObject data = new JSONObject(bodyStr);
                    String reply    = data.optString("reply", "I have no response.");
                    String imageUrl = data.optString("imageUrl", null);
                    if ("null".equals(imageUrl)) imageUrl = null;

                    // Parse follow-up suggestions
                    List<String> followUps = new ArrayList<>();
                    if (data.has("followUps")) {
                        JSONArray fuArr = data.optJSONArray("followUps");
                        if (fuArr != null) {
                            for (int i = 0; i < fuArr.length(); i++) {
                                String q = fuArr.optString(i, "").trim();
                                if (!q.isEmpty()) followUps.add(q);
                            }
                        }
                    }

                    // Auto-save memory facts detected by backend
                    if (memCtx != null && data.has("newFacts")) {
                        JSONArray nf = data.optJSONArray("newFacts");
                        if (nf != null) {
                            for (int i = 0; i < nf.length(); i++) {
                                String fact = nf.optString(i, "").trim();
                                if (!fact.isEmpty()) SmartMemory.manuallyRemember(memCtx, fact);
                            }
                        }
                    }

                    cb.onSuccess(reply, imageUrl, followUps);
                }
            } catch (Exception e) {
                ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                if (cm != null) cm.reportApiFailure(e);
                String lastUserMsg = "";
                if (history != null && !history.isEmpty()) {
                    for (int i = history.size() - 1; i >= 0; i--) {
                        if ("user".equalsIgnoreCase(history.get(i).role)) {
                            lastUserMsg = history.get(i).text;
                            break;
                        }
                    }
                }
                String offlineReply = HenryOfflineBrain.generateOfflineResponse(lastUserMsg, queryType, memCtx);
                cb.onSuccess(offlineReply, null, null);
            }
        }).start();
    }

    /**
     * v20 — Full call with emotion, relationship context, tournament, chain thinking, and multiple attachments.
     */
    public static void askV20(List<HistoryItem> history, String imageBase64,
                               String responseMode, UserProfile profile,
                               String queryType, android.content.Context memCtx,
                               String emotionState, String relationshipContext,
                               boolean enableTournament, boolean enableChainThinking,
                               Callback cb) {
        askV20(history, imageBase64, null, responseMode, profile, queryType, memCtx,
                emotionState, relationshipContext, enableTournament, enableChainThinking, cb);
    }

    public static void askV20(List<HistoryItem> history, String imageBase64, List<String> imagesBase64,
                               String responseMode, UserProfile profile,
                               String queryType, android.content.Context memCtx,
                               String emotionState, String relationshipContext,
                               boolean enableTournament, boolean enableChainThinking,
                               Callback cb) {
        new Thread(() -> {
            try {
                JSONArray messages = new JSONArray();
                for (HistoryItem item : history) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", item.role);
                    msg.put("text", item.text);
                    messages.put(msg);
                }

                JSONObject body = new JSONObject();
                body.put("messages",     messages);
                body.put("responseMode", responseMode != null ? responseMode : "balanced");
                body.put("persona",      "HENRY_HYPERINTELLIGENT_WITTY");
                body.put("systemPrompt", DEFAULT_HENRY_PERSONA_PROMPT);
                body.put("systemOverride", DEFAULT_HENRY_PERSONA_PROMPT);

                String primaryImage = imageBase64;
                if (imagesBase64 != null && !imagesBase64.isEmpty()) {
                    JSONArray arr = new JSONArray();
                    for (String img : imagesBase64) {
                        if (img != null && !img.isEmpty()) arr.put(img);
                    }
                    if (arr.length() > 0) {
                        body.put("imagesBase64", arr);
                        if (primaryImage == null || primaryImage.isEmpty()) {
                            primaryImage = arr.optString(0);
                        }
                    }
                }

                if (primaryImage != null && !primaryImage.isEmpty())
                    body.put("imageBase64", primaryImage);
                if (profile != null && !profile.isEmpty())
                    body.put("userProfile", profile.toJson());
                if (queryType != null && !queryType.isEmpty())
                    body.put("queryType", queryType);
                if (emotionState != null && !emotionState.equals("normal"))
                    body.put("emotionState", emotionState);
                if (relationshipContext != null && !relationshipContext.isEmpty())
                    body.put("relationshipContext", relationshipContext);
                if (enableTournament) body.put("enableTournament", true);
                if (enableChainThinking) body.put("enableChainThinking", true);

                // Send stored memory facts
                if (memCtx != null) {
                    String memStr = SmartMemory.buildMemoryContext(memCtx);
                    if (!memStr.isEmpty()) {
                        JSONArray factsArr = new JSONArray();
                        for (String f : memStr.replace("Known facts about user: ", "").split(";")) {
                            String t = f.trim();
                            if (!t.isEmpty()) factsArr.put(t);
                        }
                        if (factsArr.length() > 0) body.put("memoryFacts", factsArr);
                    }
                }

                RequestBody rb = RequestBody.create(body.toString(), JSON);
                Request req = new Request.Builder()
                    .url(API_URL)
                    .post(rb)
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String bodyStr = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) {
                        ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                        if (cm != null) cm.reportApiFailure(new java.io.IOException("HTTP " + resp.code()));
                        cb.onError("Server error " + resp.code());
                        return;
                    }
                    ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                    if (cm != null) cm.reportApiSuccess();
                    JSONObject data = new JSONObject(bodyStr);
                    String reply    = data.optString("reply", "I have no response.");
                    String imageUrl = data.optString("imageUrl", null);
                    if ("null".equals(imageUrl)) imageUrl = null;

                    List<String> followUps = new ArrayList<>();
                    if (data.has("followUps")) {
                        JSONArray fuArr = data.optJSONArray("followUps");
                        if (fuArr != null) {
                            for (int i = 0; i < fuArr.length(); i++) {
                                String q = fuArr.optString(i, "").trim();
                                if (!q.isEmpty()) followUps.add(q);
                            }
                        }
                    }

                    // Auto-save memory facts detected by backend
                    if (memCtx != null && data.has("newFacts")) {
                        JSONArray nf = data.optJSONArray("newFacts");
                        if (nf != null) {
                            for (int i = 0; i < nf.length(); i++) {
                                String fact = nf.optString(i, "").trim();
                                if (!fact.isEmpty()) SmartMemory.manuallyRemember(memCtx, fact);
                            }
                        }
                    }

                    cb.onSuccess(reply, imageUrl, followUps);
                }
            } catch (Exception e) {
                ConnectivityManager cm = ConnectivityManager.getInstance(memCtx);
                if (cm != null) cm.reportApiFailure(e);
                String lastUserMsg = "";
                if (history != null && !history.isEmpty()) {
                    for (int i = history.size() - 1; i >= 0; i--) {
                        if ("user".equalsIgnoreCase(history.get(i).role)) {
                            lastUserMsg = history.get(i).text;
                            break;
                        }
                    }
                }
                String offlineReply = HenryOfflineBrain.generateOfflineResponse(lastUserMsg, queryType, memCtx);
                cb.onSuccess(offlineReply, null, null);
            }
        }).start();
    }

    // ── Client-side intent classifier (mirrors backend logic) ─────────────────
    public static String classifyIntent(String msg) {
        if (msg == null || msg.isEmpty()) return "chat";
        String t = msg.toLowerCase();
        if (t.matches(".*\\b(hack|hacker|hacking|exploit|vulnerability|cve|penetration|pentest|reverse engineer|ghidra|radare|buffer overflow|rop chain|owasp|sqli|sql injection|xss|cross site|csrf|ssrf|idor|firewall|nmap|wireshark|red team|blue team|privilege escalation|zero day|cryptography|rsa|aes|diffie hellman|cipher|metasploit|burp suite).*"))
            return "cybersecurity";
        if (t.matches(".*\\b(valuation|dcf|discounted cash flow|ebitda|saas metrics|mrr|arr|cac|ltv|balance sheet|income statement|cash flow statement|venture capital|private equity|pitch deck|tam|sam|som|wacc|capm|irr|npv|cap table|dilution|convertible note|safe note|options trading|derivatives|greeks|delta|gamma|theta|vega|pe ratio|ev ebitda).*"))
            return "finance";
        if (t.matches(".*\\b(symptom|diagnosis|differential diagnosis|pathophysiology|pharmacology|pharmacokinetics|half life|drug interaction|dosage|mechanism of action|cyp450|antibiotic|cardiology|ecg|ekg|arrhythmia|myocardial|pulmonology|copd|asthma|nephrology|creatinine|neurology|stroke|fast protocol|blood pressure|hypertension|diabetes|oncology|cbc|cmp|troponin|biomarker|triage).*"))
            return "medical";
        if (t.matches(".*\\b(bitcoin|btc|ethereum|eth|solana|sol|crypto|coin|nft|defi).*")) return "crypto";
        if (t.matches(".*\\d+\\s*(usd|eur|gbp|aed|jpy|php|inr|cad|aud)\\s*(to|in)\\s*(usd|eur|gbp|aed|jpy|php|inr|cad|aud).*")
            || t.contains("exchange rate") || t.matches(".*convert\\s+\\d+.*")) return "forex";
        if (t.matches(".*\\b(roast me|roast\\b|insult me|burn me).*")) return "roast";
        if (t.matches(".*\\b(witty|matinik|bangus|comeback|hirit|banat|pilosopo|sarcastic|joke|punchline).*")) return "witty";
        if (t.matches(".*\\b(news|headlines|latest news|breaking|what happened)\\b.*")) return "news";
        if (t.matches(".*\\b(calculate|compute|what is \\d|sqrt|factorial|\\d+%\\s+of).*")) return "math";
        if (t.matches(".*\\b(search|look up|find out|google|who is|what is|where is|latest|newest|breaking|right now|today's|current|2025|2026|score|results|trending|release date|specs|specifications)\\b.*")) return "search";
        if (t.matches(".*\\b(why|how does|difference between|compare|pros and cons|should i|step by step)\\b.*")
            && msg.length() > 30) return "reason";
        return "chat";
    }


}
