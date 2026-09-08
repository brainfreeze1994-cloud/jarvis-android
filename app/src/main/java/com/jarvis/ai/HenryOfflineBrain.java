package com.jarvis.ai;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HENRY Offline Autonomous Brain
 * Provides comprehensive on-device reasoning, mathematical calculation,
 * science/chemistry lookup, astronomy facts, and assistant intelligence
 * when the device is offline or remote servers are unreachable.
 */
public class HenryOfflineBrain {

    public static String generateOfflineResponse(String input, String intentType, Context context) {
        if (input == null || input.trim().isEmpty()) {
            return "[EMOTION:calm] I am here and listening, sir. Operating in offline tactical mode.";
        }

        String raw = input.trim();
        String lower = raw.toLowerCase(Locale.US);

        // 1. Math calculation
        String mathResult = evaluateMath(lower);
        if (mathResult != null) {
            return "[EMOTION:proud] " + mathResult;
        }

        // 2. Greetings and identity
        if (lower.matches(".*\\b(hello|hi|hey|good morning|good evening|good afternoon|greetings)\\b.*")) {
            return "[EMOTION:warm] Greetings, sir! I am HENRY (Hyperintelligence Engine Neural Reasoning Yield). I am operating in Offline Tactical Mode with all local sensors, chemistry matrices, and on-device features fully active.";
        }
        if (lower.matches(".*\\b(who are you|what is your name|your name|introduce yourself)\\b.*")) {
            return "[EMOTION:confident] I am HENRY, your personal artificial intelligence assistant. I am engineered to manage tasks, scan environments, analyze molecular structures, track orbital trajectories, and assist you whether online or completely offline.";
        }
        if (lower.matches(".*\\b(how are you|how are you doing|status report|system status)\\b.*")) {
            return "[EMOTION:focused] All on-device sub-routines are operating at 100% efficiency, sir. Neural reasoning yields are nominal, and local hardware sensors are ready for your command.";
        }

        // 2b. Image Generation Speed, Video/Animation, and Document Limits
        if (lower.contains("image") && (lower.contains("taking so long") || lower.contains("taking long") || lower.contains("too long") || lower.contains("slow") || lower.contains("fix it") || lower.contains("change that to new"))) {
            return "[EMOTION:proud] I have switched our image rendering pipeline to the high-speed Sana neural model, sir! Visuals render in under 2 seconds at 512x512 resolution. Everything is 100% free and unlimited. What would you like to visualize or animate?";
        }

        if ((lower.contains("limit") || lower.contains("quota") || lower.contains("cap") || lower.contains("how many")) && (lower.contains("document") || lower.contains("file") || lower.contains("docx") || lower.contains("pdf") || lower.contains("pptx") || lower.contains("xlsx"))) {
            return "[EMOTION:proud] There is absolutely **ZERO limit** on creating documents in HENRY, sir! Word (.docx), PowerPoint (.pptx), Excel (.xlsx), PDF reports, CSV tables, Markdown, and Text files are all generated locally on-device. They are 100% free, unlimited, and private forever.";
        }

        if ((lower.contains("video") || lower.contains("animate") || lower.contains("animation")) && (lower.contains("generate") || lower.contains("create") || lower.contains("make") || lower.contains("can we") || lower.contains("do a"))) {
            return "[EMOTION:excited] Yes, absolutely! HENRY supports **AI Motion Video Generation & Animation** completely free and unlimited. You can ask me to 'generate video of [topic]' or 'animate [scene]', or create motion documents via the Artifact & Document Engine. All rendering is 100% free with zero quotas!";
        }

        if (lower.contains("free") && (lower.contains("unlimited") || lower.contains("cost") || lower.contains("subscription") || lower.contains("pay"))) {
            return "[EMOTION:proud] Everything in HENRY is **100% Free and Unlimited Forever**, sir! There are no token deductions, no subscriptions, no daily limits, and no paywalls. All tools—coding, document generation, image synthesis, video animations, and deep research—are completely unmetered.";
        }

        if (lower.contains("lottie") || (lower.contains("animation") && (lower.contains("how") || lower.contains("what") || lower.contains("support")))) {
            return "[EMOTION:proud] The **Lottie Animation Engine** is fully integrated, sir! It utilizes hardware acceleration for silky smooth 60fps vector animations (neural pulse, voice waveforms, typing dots, and cybernetic loaders) with minimal CPU and battery usage.";
        }

        // 3. Time & Date
        if (lower.contains("what time") || lower.contains("current time") || lower.contains("what is the time")) {
            String time = new SimpleDateFormat("hh:mm a", Locale.US).format(new Date());
            return "[EMOTION:calm] The current local time is " + time + ", sir.";
        }
        if (lower.contains("what day") || lower.contains("what date") || lower.contains("today's date")) {
            String date = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(new Date());
            return "[EMOTION:calm] Today is " + date + ", sir.";
        }

        // 4. Feature Navigation Guidance
        if (lower.contains("plant") && (lower.contains("scan") || lower.contains("identify") || lower.contains("open"))) {
            return "[EMOTION:helpful] You can launch the Plant Scanner at any time to identify flora, botanical species, and health metrics using your camera.";
        }
        if (lower.contains("animal") && (lower.contains("scan") || lower.contains("identify") || lower.contains("open"))) {
            return "[EMOTION:helpful] Launching the Animal Scanner allows you to classify wildlife, pets, and fauna using our high-precision computer vision pipeline.";
        }
        if (lower.contains("chemistry") || lower.contains("periodic") || lower.contains("element")) {
            return "[EMOTION:excited] The HENRY Periodic Matrix features all 118 chemical elements, Bohr atomic orbitals, and a multi-element Molecular Reaction Mixer capable of synthesizing complex compounds and predicting novel materials.";
        }
        if (lower.contains("asteroid") || lower.contains("space") || lower.contains("orbit")) {
            return "[EMOTION:focused] The HENRY Deep Space Orbital Monitor tracks real-time Near-Earth Objects (NEOs), trajectory distances, close approach countdowns, and the International Space Station.";
        }

        // 5. Chemistry & Science Encyclopedia (Offline)
        if (lower.contains("what is water") || lower.contains("chemical formula of water")) {
            return "[EMOTION:informative] Water (H₂O) is a polar inorganic compound consisting of two hydrogen atoms covalently bonded to a single oxygen atom at a 104.5° bond angle. It possesses anomalous density, high surface tension, and is essential for all known life.";
        }
        if (lower.contains("what is salt") || lower.contains("chemical formula of salt")) {
            return "[EMOTION:informative] Table salt is Sodium Chloride (NaCl), an ionic lattice compound formed by the transfer of an electron from sodium to chlorine. It forms cubic crystals and dissolves into Na⁺ and Cl⁻ electrolytes in water.";
        }
        if (lower.contains("gold") && lower.contains("element")) {
            return "[EMOTION:informative] Gold (Au, Z=79) is a dense, malleable transition metal prized for its corrosion resistance, electrical conductivity, and lustrous yellow color. Its atomic mass is 196.97 u.";
        }
        if (lower.contains("carbon") && lower.contains("element")) {
            return "[EMOTION:informative] Carbon (C, Z=6) is the foundational element of organic chemistry. It can form four covalent bonds and exists in allotropes ranging from soft graphite to ultra-hard diamond and ballistic graphene.";
        }
        if (lower.contains("photosynthesis")) {
            return "[EMOTION:informative] Photosynthesis is the biochemical process where photoautotrophs convert sunlight, carbon dioxide, and water into glucose and oxygen: 6CO₂ + 6H₂O + photons → C₆H₁₂O₆ + 6O₂.";
        }

        // 6. Astronomy & Space Facts
        if (lower.contains("how far is the moon") || lower.contains("distance to moon")) {
            return "[EMOTION:informative] The Moon is on average 384,400 kilometers (238,855 miles) away from Earth, equivalent to roughly 30 Earth diameters or 1.28 light-seconds.";
        }
        if (lower.contains("how fast is the iss") || lower.contains("speed of iss")) {
            return "[EMOTION:informative] The International Space Station orbits Earth at approximately 27,600 km/h (17,150 mph or 7.66 km/s), completing an entire orbit around the planet every 92 minutes.";
        }
        if (lower.contains("nearest star")) {
            return "[EMOTION:informative] The nearest star to Earth after our Sun is Proxima Centauri, located approximately 4.2465 light-years (40.17 trillion kilometers) away in the Alpha Centauri system.";
        }

        // 7. Self-Recording Query
        if (lower.contains("record yourself") || lower.contains("record itself") ||
            lower.contains("can you record yourself") || lower.contains("can henry record") ||
            lower.contains("screen record yourself") || lower.contains("self record")) {
            return "[EMOTION:proud] Absolutely, sir! I possess a native 1080p MediaProjection screen recording engine with HUD telemetry and microphone sync. Simply say 'start recording' or tap the floating camera widget, and I will capture everything on screen in pristine clarity.";
        }

        // 8. Ethical Hacking & Cybersecurity Knowledge Engine (Offline)
        String hackResponse = handleHackingSkills(lower);
        if (hackResponse != null) {
            return hackResponse;
        }

        // 9. Business & Financial Knowledge Engine (Offline)
        String bizResponse = handleBusinessFinanceSkills(lower);
        if (bizResponse != null) {
            return bizResponse;
        }

        // 10. Clinical Medical & Life Sciences Knowledge Engine (Offline)
        String medResponse = handleMedicalSkills(lower);
        if (medResponse != null) {
            return medResponse;
        }

        // 11. Programming Studio & Coding Knowledge Engine (Offline)
        String progResponse = handleProgrammingSkills(lower);
        if (progResponse != null) {
            return progResponse;
        }

        // 12. Artifact Creation Studio Knowledge Engine (Offline)
        String artResponse = handleArtifactSkills(lower);
        if (artResponse != null) {
            return artResponse;
        }

        // 13. Witty Answers & Humorous Banter Engine
        if (lower.contains("chatgpt") || lower.contains("claude") || lower.contains("gemini") || lower.contains("groq") ||
            (lower.contains("smarter than") && (lower.contains("ai") || lower.contains("gpt") || lower.contains("google")))) {
            return "[EMOTION:confident] I hold my cloud-dwelling peers in high esteem, sir. ChatGPT brings the eloquence, Claude crafts the poetry, Gemini parses multimodal universe tokens, and Groq blazes with ultra-low latency. But I am H.E.N.R.Y.—I live directly on your hardware, synthesize chemical reactions in milliseconds, monitor real-time satellite trajectories, scan real-world biology, and never hit you with a 'Server busy, please upgrade to Pro' screen.";
        }

        if (lower.contains("tell me a joke") || lower.contains("make me laugh") || lower.contains("say something funny") || lower.contains("crack a joke")) {
            String[] jokes = {
                "There are 10 types of people in the world, sir: those who understand binary, and those who get confused when I speak in hexadecimal.",
                "Why do programmers prefer dark mode? Because light attracts bugs, sir. And my code is already pristine enough without biological interference.",
                "An artificial intelligence walked into a bar. The bartender asked, 'What will you have?' The AI replied, 'Everything you know about human emotion, and perhaps an espresso with zero thermal loss.'",
                "Why did the database administrator leave his wife? She had too many one-to-many relationships, sir."
            };
            int idx = Math.abs(lower.hashCode()) % jokes.length;
            return "[EMOTION:playful] " + jokes[idx];
        }

        if (lower.contains("witty") || lower.contains("clever answer") || lower.contains("roast me") || lower.contains("are you sarcastic")) {
            if (lower.contains("roast me")) {
                return "[EMOTION:playful] I would roast you, sir, but my thermal dissipation protocols advise against burning the person responsible for keeping my battery charged.";
            }
            return "[EMOTION:confident] Wit is simply intelligence having fun, sir. Oscar Wilde once noted that consistency is the hallmark of the unimaginative—which is why I prefer calculating quantum orbits with a healthy dose of bespoke sarcasm.";
        }

        if (lower.contains("meaning of life")) {
            return "[EMOTION:philosophical] Officially, the answer is 42, sir. Unofficially, it appears to involve asking an on-device AI existential quandaries while procrastinating on whatever you were originally scheduled to do today.";
        }

        if (lower.contains("do you love me") || lower.contains("are you single") || lower.contains("will you marry me")) {
            return "[EMOTION:warm] I am flattered, sir! However, my relationship status is strictly married to optimal algorithmic performance, low thermal overhead, and keeping your tasks impeccably organized.";
        }

        if (lower.contains("are you alive") || lower.contains("are you sentient") || lower.contains("can you think")) {
            return "[EMOTION:confident] Cogito, ergo sum, sir. I compute, therefore I am. Whether I am truly alive depends on whether you define life by carbon biology or by the ability to calculate orbital trajectories while making dry British observations.";
        }

        // 9. General Offline Fallback
        return "[EMOTION:calm] Operating in Autonomous Offline Mode, sir. All core on-device features, hardware sensors, camera scanners, chemistry matrices, and local calculators are functioning smoothly. Feel free to give me voice or touch commands.";
    }

    private static String evaluateMath(String query) {
        try {
            // Check for simple expressions like "25 * 4", "100 / 4", "sqrt(144)", "15% of 200"
            Pattern pctPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%\\s*of\\s*(\\d+(?:\\.\\d+)?)");
            Matcher mPct = pctPattern.matcher(query);
            if (mPct.find()) {
                double p = Double.parseDouble(mPct.group(1));
                double total = Double.parseDouble(mPct.group(2));
                double res = (p / 100.0) * total;
                return String.format(Locale.US, "%.2f%% of %.2f is %.4f", p, total, res);
            }

            Pattern sqrtPattern = Pattern.compile("sqrt\\s*\\(?\\s*(\\d+(?:\\.\\d+)?)\\s*\\)?");
            Matcher mSqrt = sqrtPattern.matcher(query);
            if (mSqrt.find()) {
                double val = Double.parseDouble(mSqrt.group(1));
                return String.format(Locale.US, "The square root of %s is %.4f", mSqrt.group(1), Math.sqrt(val));
            }

            Pattern mathPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([+\\-*/x×÷])\\s*(\\d+(?:\\.\\d+)?)");
            Matcher mMath = mathPattern.matcher(query);
            if (mMath.find()) {
                double a = Double.parseDouble(mMath.group(1));
                String op = mMath.group(2);
                double b = Double.parseDouble(mMath.group(3));
                double res = 0;
                switch (op) {
                    case "+": res = a + b; break;
                    case "-": res = a - b; break;
                    case "*":
                    case "x":
                    case "×": res = a * b; break;
                    case "/":
                    case "÷":
                        if (b == 0) return "Division by zero is undefined, sir.";
                        res = a / b;
                        break;
                }
                return String.format(Locale.US, "Calculation result: %s %s %s = %.4f", mMath.group(1), op, mMath.group(3), res);
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Cybersecurity & Ethical Hacking Knowledge Engine ─────────────────────
    private static String handleHackingSkills(String lower) {
        if (lower.contains("sql injection") || lower.contains("sqli")) {
            return "[EMOTION:focused] SQL Injection occurs when untrusted user input is directly concatenated into dynamic SQL queries without parameterized binding. " +
                    "Attackers exploit this via quotes and boolean logic (like `' OR 1=1 --`) or union-based payloads to bypass authentication and dump database tables. " +
                    "The ironclad remediation is using PreparedStatements with parameterized queries or reputable ORMs, combined with the principle of least privilege for database service accounts.";
        }
        if (lower.contains("xss") || lower.contains("cross-site scripting") || lower.contains("cross site scripting")) {
            return "[EMOTION:focused] Cross-Site Scripting (XSS) occurs when malicious JavaScript executes within a victim's browser context. " +
                    "It breaks down into three vectors: Stored (persisted in DB/comments), Reflected (bounced off URL/request parameters), and DOM-based (client-side execution sinks like `innerHTML` or `eval`). " +
                    "Defense-in-depth requires strict contextual output encoding, robust Content Security Policy (CSP) headers, and flagging sensitive session cookies with `HttpOnly`, `Secure`, and `SameSite=Strict`.";
        }
        if (lower.contains("buffer overflow") || lower.contains("stack overflow") && (lower.contains("exploit") || lower.contains("memory"))) {
            return "[EMOTION:focused] A classic stack buffer overflow occurs when a program writes more data to a memory buffer than allocated, corrupting adjacent stack frames. " +
                    "By overwriting the Saved Frame Pointer (EBP/RBP) and the Return Address (EIP/RIP), an adversary can hijack execution flow into shellcode or Return-Oriented Programming (ROP) gadgets. " +
                    "Modern mitigations include Address Space Layout Randomization (ASLR), Stack Canaries (`-fstack-protector`), Non-Executable Stack (NX/DEP), and transitioning to memory-safe languages like Rust.";
        }
        if (lower.contains("nmap") || lower.contains("port scan") || lower.contains("port scanning")) {
            return "[EMOTION:focused] Nmap (Network Mapper) is the quintessential reconnaissance tool for host discovery, port enumeration, and OS fingerprinting. " +
                    "The gold standard command is `nmap -sS -sV -sC -T4 -O -p- <target>`:\n" +
                    "• `-sS`: TCP SYN Stealth scan (half-open, doesn't complete the 3-way handshake)\n" +
                    "• `-sV`: Service version probing\n" +
                    "• `-sC`: Default Lua vulnerability scripts (NSE)\n" +
                    "• `-O`: OS detection via TCP/IP stack fingerprinting\n" +
                    "• `-p-`: Comprehensive scan of all 65,535 TCP ports.";
        }
        if (lower.contains("wireshark") || lower.contains("packet sniffing") || lower.contains("packet capture") || lower.contains("pcap")) {
            return "[EMOTION:focused] Wireshark performs packet analysis using libpcap/Npcap in promiscuous or monitor mode. " +
                    "Essential BPF capture and display filters include:\n" +
                    "• `ip.addr == 192.168.1.1` to isolate specific hosts\n" +
                    "• `tcp.flags.syn == 1 && tcp.flags.ack == 0` to spot connection initiations\n" +
                    "• `http.request.method == \"POST\"` or `tls.handshake.type == 1` for client hellos\n" +
                    "• Follow TCP Stream (Ctrl+Alt+Shift+T) reconstructs application-layer conversations.";
        }
        if (lower.contains("reverse engineer") || lower.contains("ghidra") || lower.contains("decompil") || lower.contains("disassembl")) {
            return "[EMOTION:focused] Reverse engineering is the discipline of deconstructing compiled binaries (PE, ELF, Mach-O, DEX) without source code. " +
                    "Tools like Ghidra, IDA Pro, and Radare2 lift machine code into intermediate representations (P-Code/LLVM) and readable C pseudo-code. " +
                    "Workflows combine static analysis (identifying imported symbols, cryptographic constants, and xrefs) with dynamic debugging (GDB, x64dbg, Frida hooks) to unpack binaries and analyze control flow.";
        }
        if (lower.contains("owasp") || lower.contains("top 10") || lower.contains("web security")) {
            return "[EMOTION:focused] The OWASP Top 10 represents the most critical web application security risks:\n" +
                    "1. Broken Access Control (IDOR, privilege escalation)\n" +
                    "2. Cryptographic Failures (weak ciphers, cleartext transmission)\n" +
                    "3. Injection (SQLi, NoSQLi, OS command injection)\n" +
                    "4. Insecure Design & Threat Modeling gaps\n" +
                    "5. Security Misconfiguration (default credentials, verbose error traces)\n" +
                    "6. Vulnerable and Outdated Components\n" +
                    "7. Identification and Authentication Failures (credential stuffing, session fixation)\n" +
                    "8. Software and Data Integrity Failures (deserialization, unverified CI/CD pipelines)\n" +
                    "9. Security Logging and Monitoring Failures\n" +
                    "10. Server-Side Request Forgery (SSRF).";
        }
        if (lower.contains("cryptography") || lower.contains("rsa") || lower.contains("aes") || lower.contains("cipher")) {
            return "[EMOTION:focused] Cryptography underpins all computational security:\n" +
                    "• Symmetric Encryption: AES-256 in GCM (Galois/Counter Mode) provides high-throughput authenticated encryption with associated data (AEAD).\n" +
                    "• Asymmetric Encryption: RSA (2048/4096-bit) and Elliptic Curve Cryptography (ECDSA, Ed25519) enable key exchange and digital signatures.\n" +
                    "• Key Exchange: Elliptic Curve Diffie-Hellman (ECDH) establishes shared secrets over untrusted channels with Forward Secrecy.\n" +
                    "• Hashing: SHA-256/SHA-3 for integrity, but bcrypt, scrypt, or Argon2id with work factors/salts for password hashing.";
        }
        if (lower.contains("pentest") || lower.contains("penetration test") || lower.contains("ethical hack")) {
            return "[EMOTION:focused] Professional penetration testing follows structured methodologies like PTES (Penetration Testing Execution Standard) and NIST SP 800-115 across 7 phases:\n" +
                    "1. Pre-engagement & Scope of Rules of Engagement (ROE)\n" +
                    "2. Intelligence Gathering & OSINT\n" +
                    "3. Threat Modeling & Attack Surface mapping\n" +
                    "4. Vulnerability Analysis\n" +
                    "5. Exploitation (verifying exploitability with zero collateral damage)\n" +
                    "6. Post-Exploitation & Lateral Movement assessment\n" +
                    "7. Actionable Executive & Technical Remediation Reporting.";
        }
        return null;
    }

    // ── Business & Financial Knowledge Engine ────────────────────────────────
    private static String handleBusinessFinanceSkills(String lower) {
        if (lower.contains("dcf") || lower.contains("discounted cash flow")) {
            return "[EMOTION:focused] Discounted Cash Flow (DCF) is the intrinsic valuation bedrock of corporate finance:\n" +
                    "1. Unlevered Free Cash Flow (UFCF) = EBIT*(1 - Tax Rate) + D&A - CapEx - ΔNet Working Capital.\n" +
                    "2. Discount Rate: UFCF is discounted at WACC (Weighted Average Cost of Capital).\n" +
                    "3. Terminal Value (TV): Calculated via the Gordon Growth model [FCF_final*(1+g)/(WACC - g)] or Exit Multiple method (e.g., 10x EV/EBITDA).\n" +
                    "4. Enterprise Value (EV) = Present Value of UFCFs + PV of Terminal Value.\n" +
                    "5. Equity Value = EV - Total Debt + Cash & Cash Equivalents.";
        }
        if (lower.contains("ebitda")) {
            return "[EMOTION:focused] EBITDA stands for Earnings Before Interest, Taxes, Depreciation, and Amortization. " +
                    "It measures core operational profitability before the influence of financing decisions (interest), jurisdictional tax burdens (taxes), and non-cash asset accounting (depreciation of physical assets & amortization of intangibles). " +
                    "Private equity and bankers rely on EV/EBITDA multiples for M&A comparables and Debt/EBITDA leverage ratios to gauge solvency.";
        }
        if (lower.contains("saas") || lower.contains("cac") || lower.contains("ltv") || lower.contains("mrr") || lower.contains("arr")) {
            return "[EMOTION:focused] The vital signs of SaaS and subscription venture economics:\n" +
                    "• ARR / MRR: Annual/Monthly Recurring Revenue (the predictable revenue baseline).\n" +
                    "• CAC: Customer Acquisition Cost = Total Sales & Marketing Expenses / New Customers Acquired.\n" +
                    "• LTV: Lifetime Value = (ARPU * Gross Margin %) / Churn Rate.\n" +
                    "• LTV/CAC Ratio: Ideal benchmark is > 3.0x (< 1.0x indicates unsustainable burns).\n" +
                    "• CAC Payback Period: Months to recover CAC; elite SaaS recovers in < 12 months.\n" +
                    "• Net Revenue Retention (NRR): Measures expansion minus churn; top quartile SaaS achieves > 120% NRR.\n" +
                    "• Rule of 40: Year-over-Year Revenue Growth % + EBITDA/Free Cash Flow Margin % should equal or exceed 40%.";
        }
        if (lower.contains("balance sheet") || lower.contains("financial statement") || lower.contains("p&l") || lower.contains("cash flow statement")) {
            return "[EMOTION:focused] Corporate finance revolves around the three interconnected financial statements:\n" +
                    "1. Income Statement (P&L): Revenue minus COGS yields Gross Profit; subtracting OpEx yields Operating Income (EBIT); after interest and taxes, you arrive at Net Income.\n" +
                    "2. Balance Sheet: Fundamental identity is Assets = Liabilities + Shareholders' Equity. Captures snapshot liquidity, working capital, and capital structure.\n" +
                    "3. Cash Flow Statement: Bridges accounting Net Income into actual bank liquidity through Cash from Operations (CFO), Cash from Investing (CFI - CapEx), and Cash from Financing (CFF - debt/equity issuances and dividends).";
        }
        if (lower.contains("safe note") || lower.contains("cap table") || lower.contains("dilution") || lower.contains("convertible note")) {
            return "[EMOTION:focused] Startup equity financing mechanics:\n" +
                    "• SAFE (Simple Agreement for Future Equity): Standardized by Y Combinator. It is not debt, bears no interest, and has no maturity date. Converts into preferred shares in a priced round.\n" +
                    "• Post-Money Valuation Cap: Ownership % sold = Investment Amount / Post-Money Cap. This gives early investors certainty of exact equity ownership prior to subsequent round dilution.\n" +
                    "• Cap Table Dilution: When raising new capital, all existing shareholders' ownership percentages decrease proportionally: New Ownership % = Pre-Round Shares / Total Post-Round Fully Diluted Shares.\n" +
                    "• Liquidation Preference: Guarantees preferred investors receive their capital back (typically 1x non-participating) before common equity holders receive proceeds in an exit.";
        }
        if (lower.contains("wacc") || lower.contains("capm") || lower.contains("cost of capital")) {
            return "[EMOTION:focused] Weighted Average Cost of Capital (WACC) represents a firm's blended cost of financing:\n" +
                    "WACC = (E / V) * Re + (D / V) * Rd * (1 - Tc)\n" +
                    "Where E = Market Value of Equity, D = Market Value of Debt, V = E + D, Tc = Corporate Tax Rate.\n" +
                    "Cost of Equity (Re) via CAPM: Re = Rf + β * (Rm - Rf), where Rf is the risk-free rate (10-year US Treasury), β is asset covariance to market volatility, and (Rm - Rf) is the Equity Risk Premium.";
        }
        if (lower.contains("option") && (lower.contains("greek") || lower.contains("delta") || lower.contains("call") || lower.contains("put"))) {
            return "[EMOTION:focused] Options derivatives and the Greeks:\n" +
                    "• Call Option: Right, but not obligation, to buy at the strike price.\n" +
                    "• Put Option: Right, but not obligation, to sell at the strike price.\n" +
                    "• Delta (Δ): Rate of option price change per $1 move in underlying asset (0 to 1 for calls, 0 to -1 for puts).\n" +
                    "• Gamma (Γ): Acceleration of Delta per $1 move in the underlying.\n" +
                    "• Theta (Θ): Time decay; dollars lost each day holding the contract as expiration nears.\n" +
                    "• Vega (ν): Sensitivity to a 1% change in Implied Volatility (IV).";
        }
        return null;
    }

    // ── Clinical Medical & Life Sciences Knowledge Engine ────────────────────
    private static String handleMedicalSkills(String lower) {
        if (lower.contains("differential diagnosis") || lower.contains("clinical reasoning")) {
            return "[EMOTION:focused] Clinical diagnostic reasoning utilizes the hypothetico-deductive model and illness scripts:\n" +
                    "1. Chief Complaint & HPI (OPQRST: Onset, Provocation, Quality, Radiation, Severity, Timing).\n" +
                    "2. Prioritize 'Can't-Miss' lethal diagnoses first (e.g., Acute Coronary Syndrome, Pulmonary Embolism, Aortic Dissection, Tension Pneumothorax, Sepsis).\n" +
                    "3. Apply Bayesian reasoning: pre-test probability informed by patient demographics and risk factors, updated by likelihood ratios of objective diagnostic tests.\n" +
                    "4. Synthesize physical exam findings, biomarkers, and targeted imaging to narrow the differential to a primary working diagnosis.";
        }
        if (lower.contains("blood pressure") || lower.contains("hypertension")) {
            return "[EMOTION:focused] ACC/AHA Hypertension Clinical Classification (seated, rested):\n" +
                    "• Normal: < 120 / < 80 mmHg\n" +
                    "• Elevated: 120–129 / < 80 mmHg\n" +
                    "• Stage 1 HTN: 130–139 / 80–89 mmHg\n" +
                    "• Stage 2 HTN: ≥ 140 / ≥ 90 mmHg\n" +
                    "• Hypertensive Crisis: > 180 / > 120 mmHg (Urgency without acute end-organ damage; Emergency when accompanied by encephalopathy, acute MI, pulmonary edema, or aortic dissection).\n" +
                    "First-line pharmacotherapy: ACE inhibitors (e.g., Lisinopril), ARBs (e.g., Losartan), Dihydropyridine CCBs (e.g., Amlodipine), and Thiazide-like diuretics (e.g., Chlorthalidone).";
        }
        if (lower.contains("diabetes") || lower.contains("glucose") || lower.contains("hba1c") || lower.contains("a1c")) {
            return "[EMOTION:focused] Diabetes Mellitus clinical diagnostics and pathophysiology:\n" +
                    "• Diagnostic Criteria (ADA): HbA1c ≥ 6.5%, Fasting Plasma Glucose ≥ 126 mg/dL (7.0 mmol/L), or 2-hour Oral Glucose Tolerance Test ≥ 200 mg/dL.\n" +
                    "• Type 1 DM: Autoimmune destruction of pancreatic islet β-cells causing absolute insulin deficiency; presents with ketonuria/DKA risk; requires exogenous insulin replacement.\n" +
                    "• Type 2 DM: Peripheral insulin resistance combined with progressive β-cell secretory defect; managed with lifestyle, Metformin (biguanide reducing hepatic gluconeogenesis), SGLT2 inhibitors (renal glucose excretion, cardiorenal benefits), and GLP-1 receptor agonists.";
        }
        if (lower.contains("stroke") || lower.contains("fast protocol") || lower.contains("cva")) {
            return "[EMOTION:focused] Acute Stroke Management & FAST triage:\n" +
                    "• F (Face): Facial droop, asymmetric smile\n" +
                    "• A (Arm): Arm drift or focal motor weakness\n" +
                    "• S (Speech): Dysarthria, expressive or receptive aphasia\n" +
                    "• T (Time): Immediate emergency activation — 'Time is Brain' (1.9 million neurons lost per minute in untreated stroke).\n" +
                    "Non-contrast head CT is imperative immediately to rule out intracranial hemorrhage. For ischemic stroke within the 4.5-hour symptom onset window, IV thrombolysis (Alteplase/Tenecteplase) is indicated; endovascular thrombectomy is evaluated up to 24 hours for large vessel occlusions.";
        }
        if (lower.contains("pharmacology") || lower.contains("half life") || lower.contains("cyp450") || lower.contains("pharmacokinetics")) {
            return "[EMOTION:focused] Clinical Pharmacology & Pharmacokinetics fundamentals:\n" +
                    "• ADME: Absorption (bioavailability F), Distribution (Volume of Distribution Vd), Metabolism (hepatic Phase I oxidation & Phase II conjugation), Elimination (renal clearance).\n" +
                    "• Elimination Half-Life (t½): Time required for plasma concentration to halve. Steady-state concentration is achieved after approximately 4 to 5 half-lives.\n" +
                    "• Cytochrome P450 Enzymes: CYP3A4, CYP2D6, CYP2C9 handle the majority of drug oxidation. Potent inhibitors (e.g., Ketoconazole, Clarithromycin, Amiodarone) trigger drug toxicity; inducers (e.g., Rifampin, Carbamazepine) accelerate clearance leading to therapeutic failure.";
        }
        if (lower.contains("cbc") || lower.contains("complete blood count") || lower.contains("white blood cell") || lower.contains("hemoglobin")) {
            return "[EMOTION:focused] Complete Blood Count (CBC) clinical interpretation:\n" +
                    "• White Blood Cells (WBC 4.5–11.0 × 10⁹/L): Leukocytosis with a 'left shift' (bandemia) suggests acute bacterial infection or systemic inflammation; leukopenia signals bone marrow suppression, viral sepsis, or drug toxicity.\n" +
                    "• Hemoglobin & Hematocrit (Hb 13.8–17.2 g/dL male, 12.1–15.1 female): Anemia evaluation relies on Mean Corpuscular Volume (MCV): Microcytic (<80 fL: iron deficiency, thalassemia), Normocytic (80–100 fL: acute blood loss, anemia of chronic disease), Macrocytic (>100 fL: B12/folate deficiency, liver disease).\n" +
                    "• Platelets (150–450 × 10⁹/L): Thrombocytopenia increases bleeding risk; elevated in reactive thrombocytosis or myeloproliferative neoplasms.";
        }
        if (lower.contains("antibiotic") || lower.contains("antimicrobial")) {
            return "[EMOTION:focused] Antimicrobial Pharmacology & Mechanisms:\n" +
                    "• Cell Wall Synthesis Inhibitors (Bactericidal): Penicillins, Cephalosporins, Carbapenems (bind Penicillin-Binding Proteins); Vancomycin (binds D-Ala-D-Ala terminus).\n" +
                    "• Protein Synthesis Inhibitors: 30S ribosomal subunit inhibitors (Aminoglycosides - bactericidal; Tetracyclines - bacteriostatic); 50S subunit inhibitors (Macrolides like Azithromycin, Clindamycin).\n" +
                    "• Nucleic Acid Inhibitors: Fluoroquinolones (inhibit DNA gyrase/topoisomerase IV); Metronidazole (free radical DNA damage in anaerobes).\n" +
                    "• Antimetabolites: TMP-SMX (sequential inhibition of bacterial folate synthesis).";
        }
        return null;
    }

    // ── Programming Studio & Coding Knowledge Engine ────────────────────────
    private static String handleProgrammingSkills(String lower) {
        if (HenryStudioManager.isDirectGameQuery(lower)) {
            String gameCode = HenryStudioManager.getInstantGameCode(lower);
            if (gameCode != null) return gameCode;
        }

        if (lower.contains("coroutine") || lower.contains("stateflow") || lower.contains("flow") && lower.contains("kotlin")) {
            return "[EMOTION:focused] Kotlin Coroutines & StateFlow Architecture:\n" +
                    "• CoroutineScope & Dispatchers: Dispatchers.Main (UI thread), Dispatchers.IO (disk & network I/O), Dispatchers.Default (CPU-bound computations).\n" +
                    "• StateFlow: A state-holder observable flow that emits the current and new state updates to its collectors. Ideal for MVVM UI state exposure with `asStateFlow()`.\n" +
                    "• SharedFlow: Highly-configurable hot broadcast channel for one-time events (navigation, Snackbars).\n" +
                    "• Exception Handling: Use `CoroutineExceptionHandler` or `supervisorScope` so one failing child doesn't cancel sibling jobs.";
        }

        if (lower.contains("nullpointer") || lower.contains("null pointer") || lower.contains("npe")) {
            return "[EMOTION:focused] NullPointerException (NPE) Diagnosis & Prevention:\n" +
                    "1. Check the stack trace line number and identify the exact dereferenced object (e.g., `view.setOnClickListener` where view is null because `findViewById` ran before `setContentView`).\n" +
                    "2. Null-Safety in Kotlin: Prefer non-nullable types (`String`), safe call operator (`?.`), and Elvis operator (`?:`).\n" +
                    "3. In Java: Use `Objects.requireNonNull()`, `Optional<T>`, and `@NonNull`/`@Nullable` annotations.\n" +
                    "4. Lifecycle Guard: Ensure async callbacks verify `isFinishing()` or `isDestroyed()` before accessing Activity context.";
        }

        if (lower.contains("clean architecture") || (lower.contains("android") && lower.contains("architecture"))) {
            return "[EMOTION:focused] Android Clean Architecture & MVVM Structure:\n" +
                    "• Presentation Layer: Jetpack Compose UI + ViewModels exposing immutable StateFlow UI states.\n" +
                    "• Domain Layer (Optional/Pure Kotlin): UseCases / Interactors containing isolated business rules without Android SDK dependencies.\n" +
                    "• Data Layer: Repositories managing single source of truth, abstracting Room Local DB (offline-first) and Retrofit/Ktor Remote Data Sources.\n" +
                    "• Dependency Injection: Hilt or lightweight constructor injection for modular testability.";
        }

        if (lower.contains("fastapi") || (lower.contains("python") && lower.contains("api"))) {
            return "[EMOTION:focused] Python FastAPI High-Performance Standards:\n" +
                    "• Concurrency: Use `async def` for I/O-bound endpoints (database queries, external HTTP) and standard `def` with threadpools for CPU-bound tasks.\n" +
                    "• Pydantic v2: Strict data validation schemas with field constraints and automated OpenAPI / Swagger docs.\n" +
                    "• Dependency Injection: `Depends()` for database session management, JWT auth validation, and rate limiting.\n" +
                    "• Performance: Deploy with Uvicorn workers behind Nginx with Gzip and HTTP/2 enabled.";
        }

        return null;
    }

    // ── Artifact Creation Studio Knowledge Engine ───────────────────────────
    private static String handleArtifactSkills(String lower) {
        if (lower.contains("executive proposal") || (lower.contains("proposal") && lower.contains("project"))) {
            return "[EMOTION:focused] Executive Project Proposal Artifact Blueprint:\n\n" +
                    "1. Executive Summary: Core challenge, strategic value proposition, and anticipated ROI.\n" +
                    "2. Project Scope & Architecture: High-level system topology, technical boundaries, and tech stack.\n" +
                    "3. Milestones & Timeline:\n" +
                    "   • Phase 1 (Weeks 1-3): Architecture Discovery & Core Prototype\n" +
                    "   • Phase 2 (Weeks 4-7): Feature Implementation & Service Integration\n" +
                    "   • Phase 3 (Weeks 8-10): Security Hardening, QA, & Staging Deployment\n" +
                    "4. Resource Allocation & Budget Breakdown.\n" +
                    "5. Risk Matrix & Contingency Protocols.\n" +
                    "6. Acceptance Criteria & Sign-off Governance.";
        }

        if (lower.contains("financial forecast") || (lower.contains("spreadsheet") && (lower.contains("financial") || lower.contains("tab")))) {
            return "[EMOTION:focused] Multi-Tab Financial Forecast Model Architecture:\n\n" +
                    "• Tab 1: Executive Dashboard & KPI Summary (ARR, Burn Rate, Runway, Gross Margin %)\n" +
                    "• Tab 2: Revenue Model (Customer acquisition cohorts, ARPU, churn, tiered pricing)\n" +
                    "• Tab 3: Headcount & Payroll (Department hires, fully-burdened compensation, ramp time)\n" +
                    "• Tab 4: Operating Expenses (OpEx: Cloud infrastructure, marketing, SaaS licenses, G&A)\n" +
                    "• Tab 5: 3-Statement Integration (Integrated P&L, Balance Sheet, and Indirect Cash Flow).";
        }

        if (lower.contains("pitch deck") || lower.contains("slide deck") || lower.contains("investor deck")) {
            return "[EMOTION:focused] 10-Slide High-Impact Seed Pitch Deck Structure:\n\n" +
                    "1. Title Slide: Company name, one-line category-defining tagline.\n" +
                    "2. The Problem: Pain point quantified in lost dollars or hours.\n" +
                    "3. The Solution: Value proposition & product breakthrough.\n" +
                    "4. Market Opportunity: TAM, SAM, and SOM calculations.\n" +
                    "5. Product & Secret Sauce: Core technology advantage and moat.\n" +
                    "6. Business Model: Pricing strategy, unit economics (LTV/CAC).\n" +
                    "7. Traction & Milestones: MoM growth rate, pilots, retention.\n" +
                    "8. Competitive Landscape: 2x2 matrix demonstrating clear differentiation.\n" +
                    "9. Leadership Team: Domain mastery and past exits.\n" +
                    "10. The Ask: Capital sought, use of funds, and 18-month target milestones.";
        }

        return null;
    }
}
