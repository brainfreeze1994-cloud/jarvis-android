package com.jarvis.ai;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * HenryStudioManager — Core execution engine for specialized studios:
 * 1. Programming Studio & Coding Mentor
 * 2. Business Strategy & Financial Acumen Studio
 * 3. Ethical Hacking & Cybersecurity Intelligence Studio
 * 4. Clinical Medicine & Healthcare Intelligence Studio
 * 5. Artifact Creation Studio (Docs, Spreadsheets, Slide Decks)
 */
public class HenryStudioManager {

    public enum StudioType {
        PROGRAMMING,
        BUSINESS,
        HACKING,
        MEDICAL,
        ARTIFACT
    }

    public interface StudioActionCallback {
        void onPromptSelected(String prompt);
    }

    /**
     * Check if user input directly targets one of the five studios.
     */
    public static boolean isStudioTrigger(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.US).trim();

        if (lower.equals("studios") || lower.equals("open studio") || lower.equals("all studios") ||
            lower.contains("studio hub") || lower.contains("open studios")) {
            return true;
        }

        // Programming Studio
        if (lower.contains("programming studio") || lower.contains("coding studio") ||
            lower.contains("code studio") || lower.contains("developer studio")) {
            return true;
        }

        // Business Studio
        if (lower.contains("business studio") || lower.contains("finance studio") ||
            lower.contains("financial studio") || lower.contains("strategy studio")) {
            return true;
        }

        // Hacking Studio
        if (lower.contains("hacking studio") || lower.contains("cybersecurity studio") ||
            lower.contains("security studio") || lower.contains("pentest studio")) {
            return true;
        }

        // Medical Studio
        if (lower.contains("medical studio") || lower.contains("clinical studio") ||
            lower.contains("healthcare studio") || lower.contains("health studio")) {
            return true;
        }

        // Artifact Studio
        if (lower.contains("artifact studio") || lower.contains("document studio") ||
            lower.contains("deck studio") || lower.contains("sheet studio")) {
            return true;
        }

        return false;
    }

    /**
     * Match direct game development or common studio query requests.
     */
    public static boolean isDirectGameQuery(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.US).trim();
        return (lower.contains("tic tac toe") && (lower.contains("create") || lower.contains("build") || lower.contains("game") || lower.contains("code") || lower.contains("play")))
            || (lower.contains("snake") && (lower.contains("create") || lower.contains("build") || lower.contains("game") || lower.contains("code") || lower.contains("play")));
    }

    /**
     * Generate complete, instant working code for classic studio games.
     */
    public static String getInstantGameCode(String text) {
        String lower = text.toLowerCase(Locale.US);
        if (lower.contains("snake")) {
            return "[EMOTION:excited] Here is a complete, pristine Snake game in Python with Pygame ready for immediate execution, sir:\n\n" +
                "```python\n" +
                "# 🐍 Classic Snake Game in Python\n" +
                "import pygame\n" +
                "import time\n" +
                "import random\n\n" +
                "pygame.init()\n\n" +
                "# Screen Dimensions & Colors\n" +
                "WIDTH, HEIGHT = 600, 400\n" +
                "BLACK = (13, 17, 23)\n" +
                "GREEN = (0, 255, 128)\n" +
                "RED = (255, 68, 68)\n" +
                "WHITE = (240, 246, 252)\n\n" +
                "dis = pygame.display.set_mode((WIDTH, HEIGHT))\n" +
                "pygame.display.set_caption('HENRY Snake Studio')\n" +
                "clock = pygame.time.Clock()\n\n" +
                "def game_loop():\n" +
                "    game_over = False\n" +
                "    x, y = WIDTH // 2, HEIGHT // 2\n" +
                "    x_change, y_change = 0, 0\n" +
                "    snake_list = []\n" +
                "    snake_len = 1\n" +
                "    food_x = round(random.randrange(0, WIDTH - 10) / 10.0) * 10.0\n" +
                "    food_y = round(random.randrange(0, HEIGHT - 10) / 10.0) * 10.0\n\n" +
                "    while not game_over:\n" +
                "        for event in pygame.event.get():\n" +
                "            if event.type == pygame.QUIT:\n" +
                "                game_over = True\n" +
                "            if event.type == pygame.KEYDOWN:\n" +
                "                if event.key == pygame.K_LEFT and x_change == 0:\n" +
                "                    x_change, y_change = -10, 0\n" +
                "                elif event.key == pygame.K_RIGHT and x_change == 0:\n" +
                "                    x_change, y_change = 10, 0\n" +
                "                elif event.key == pygame.K_UP and y_change == 0:\n" +
                "                    x_change, y_change = 0, -10\n" +
                "                elif event.key == pygame.K_DOWN and y_change == 0:\n" +
                "                    x_change, y_change = 0, 10\n\n" +
                "        x += x_change\n" +
                "        y += y_change\n" +
                "        if x < 0 or x >= WIDTH or y < 0 or y >= HEIGHT:\n" +
                "            game_over = True\n\n" +
                "        dis.fill(BLACK)\n" +
                "        pygame.draw.rect(dis, RED, [food_x, food_y, 10, 10])\n\n" +
                "        snake_head = [x, y]\n" +
                "        snake_list.append(snake_head)\n" +
                "        if len(snake_list) > snake_len:\n" +
                "            del snake_list[0]\n\n" +
                "        for segment in snake_list[:-1]:\n" +
                "            if segment == snake_head:\n" +
                "                game_over = True\n\n" +
                "        for seg in snake_list:\n" +
                "            pygame.draw.rect(dis, GREEN, [seg[0], seg[1], 10, 10])\n\n" +
                "        pygame.display.update()\n\n" +
                "        if x == food_x and y == food_y:\n" +
                "            food_x = round(random.randrange(0, WIDTH - 10) / 10.0) * 10.0\n" +
                "            food_y = round(random.randrange(0, HEIGHT - 10) / 10.0) * 10.0\n" +
                "            snake_len += 1\n\n" +
                "        clock.tick(15)\n\n" +
                "    pygame.quit()\n\n" +
                "if __name__ == '__main__':\n" +
                "    game_loop()\n" +
                "```\n\n" +
                "You can run this directly in Python or ask me to modify it for Android Canvas or HTML5!";
        }

        if (lower.contains("tic tac toe")) {
            return "[EMOTION:excited] Here is a complete, interactive Tic Tac Toe engine with unbeatable Minimax AI in Python, sir:\n\n" +
                "```python\n" +
                "# ⭕ Tic Tac Toe with Minimax AI Engine\n" +
                "board = [' ' for _ in range(9)]\n\n" +
                "def print_board():\n" +
                "    print('\\n+---+---+---+')\n" +
                "    for i in range(3):\n" +
                "        row = [board[i * 3 + j] for j in range(3)]\n" +
                "        print(f'| {\" | \".join(row)} |')\n" +
                "        print('+---+---+---+')\n\n" +
                "def check_winner(b, player):\n" +
                "    wins = [[0,1,2],[3,4,5],[6,7,8],[0,3,6],[1,4,7],[2,5,8],[0,4,8],[2,4,6]]\n" +
                "    return any(all(b[pos] == player for pos in combo) for combo in wins)\n\n" +
                "def is_full(b):\n" +
                "    return ' ' not in b\n\n" +
                "def minimax(b, depth, is_max):\n" +
                "    if check_winner(b, 'O'): return 10 - depth\n" +
                "    if check_winner(b, 'X'): return depth - 10\n" +
                "    if is_full(b): return 0\n\n" +
                "    if is_max:\n" +
                "        best = -100\n" +
                "        for i in range(9):\n" +
                "            if b[i] == ' ':\n" +
                "                b[i] = 'O'\n" +
                "                best = max(best, minimax(b, depth + 1, False))\n" +
                "                b[i] = ' '\n" +
                "        return best\n" +
                "    else:\n" +
                "        best = 100\n" +
                "        for i in range(9):\n" +
                "            if b[i] == ' ':\n" +
                "                b[i] = 'X'\n" +
                "                best = min(best, minimax(b, depth + 1, True))\n" +
                "                b[i] = ' '\n" +
                "        return best\n\n" +
                "def get_best_move():\n" +
                "    best_val, best_move = -100, -1\n" +
                "    for i in range(9):\n" +
                "        if board[i] == ' ':\n" +
                "            board[i] = 'O'\n" +
                "            move_val = minimax(board, 0, False)\n" +
                "            board[i] = ' '\n" +
                "            if move_val > best_val:\n" +
                "                best_val, best_move = move_val, i\n" +
                "    return best_move\n\n" +
                "print('Tic Tac Toe initialized. You are X, Henry AI is O.')\n" +
                "print_board()\n" +
                "```\n\n" +
                "Every move calculates optimal minimax game-tree branches. Would you like this adapted for Android Compose or Web?";
        }

        return null;
    }

    /**
     * Show Studio Selector or specific studio modal.
     */
    public static void showStudioHub(Context context, StudioActionCallback callback) {
        String[] studios = {
            "💻 Programming Studio & Coding Mentor",
            "📈 Business Strategy & Financial Acumen",
            "🛡 Ethical Hacking & Cybersecurity Intelligence",
            "🩺 Clinical Medicine & Healthcare Intelligence",
            "📄 Artifact Creation Studio (Docs, Sheets, Decks)"
        };

        new AlertDialog.Builder(context, R.style.Theme_Jarvis_Dialog)
            .setTitle("⚙ HENRY Specialization Studios")
            .setItems(studios, (dialog, which) -> {
                switch (which) {
                    case 0: showProgrammingStudio(context, callback); break;
                    case 1: showBusinessStudio(context, callback); break;
                    case 2: showHackingStudio(context, callback); break;
                    case 3: showMedicalStudio(context, callback); break;
                    case 4: showArtifactStudio(context, callback); break;
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    public static void showProgrammingStudio(Context context, StudioActionCallback callback) {
        String[] options = {
            "🐍 Create Classic Snake Game (Complete Code)",
            "⭕ Create Tic Tac Toe with Minimax AI",
            "📱 Build Android Jetpack Compose Clean Architecture App",
            "🐞 Debug and Fix NullPointerException Stack Trace",
            "🎓 Learn Kotlin Coroutines and StateFlow Step-by-Step",
            "🔍 Audit Python FastAPI Microservice for Security",
            "⚙ Run Code in Sandbox (Piston Multi-language Engine)"
        };

        new AlertDialog.Builder(context, R.style.Theme_Jarvis_Dialog)
            .setTitle("💻 Programming Studio & Coding Mentor")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: callback.onPromptSelected("Create a complete working Snake game in Python with Pygame"); break;
                    case 1: callback.onPromptSelected("Create a complete Tic Tac Toe game with unbeatable Minimax AI"); break;
                    case 2: callback.onPromptSelected("Design an Android Jetpack Compose clean architecture app with ViewModel and Room database"); break;
                    case 3: callback.onPromptSelected("Diagnose and fix a complex NullPointerException and memory leak with Android Profiler"); break;
                    case 4: callback.onPromptSelected("Teach Kotlin Coroutines, Flow, and StateFlow step-by-step with practical examples"); break;
                    case 5: callback.onPromptSelected("Audit a Python FastAPI service for OWASP vulnerabilities and async concurrency bottlenecks"); break;
                    case 6: callback.onPromptSelected("Run this code: print('HENRY Programming Studio online!')"); break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public static void showBusinessStudio(Context context, StudioActionCallback callback) {
        String[] options = {
            "📊 Build a Discounted Cash Flow (DCF) Valuation Model",
            "📈 Analyze SaaS Unit Economics (CAC, LTV, Churn, Rule of 40)",
            "📑 Forensic 3-Statement Financial Analysis (P&L, Balance Sheet, Cash Flow)",
            "🤝 Venture Capital Cap Table & SAFE Dilution Modeling",
            "🚀 10-Slide Seed Pitch Deck Architecture"
        };

        new AlertDialog.Builder(context, R.style.Theme_Jarvis_Dialog)
            .setTitle("📈 Business Strategy & Financial Acumen")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: callback.onPromptSelected("Build a complete Discounted Cash Flow (DCF) model explaining UFCF, WACC, and Terminal Value"); break;
                    case 1: callback.onPromptSelected("Calculate and analyze SaaS unit economics: CAC, LTV, Payback Period, Net Revenue Retention, and Rule of 40"); break;
                    case 2: callback.onPromptSelected("Conduct a forensic 3-statement financial statement integration (Income Statement, Balance Sheet, Cash Flow)"); break;
                    case 3: callback.onPromptSelected("Model a startup venture capital Cap Table with $2M SAFE notes, pre-money valuation, and Series A dilution"); break;
                    case 4: callback.onPromptSelected("Outline an investor-ready 10-slide Seed Pitch Deck with slide objectives and financial projections"); break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public static void showHackingStudio(Context context, StudioActionCallback callback) {
        String[] options = {
            "🛡 Buffer Overflow Exploitation & Memory Safety (ASLR, Canaries)",
            "🔬 Binary Reverse Engineering with Ghidra & IDA Pro",
            "🌐 OWASP Top 10 Web Application Penetration Testing",
            "📡 Nmap Comprehensive Port Reconnaissance & NSE Scripts",
            "🦈 Wireshark Packet Inspection & BPF Filter Blueprint",
            "🔐 Cryptographic Security Audit (AES-256-GCM, ECC, RSA)"
        };

        new AlertDialog.Builder(context, R.style.Theme_Jarvis_Dialog)
            .setTitle("🛡 Ethical Hacking & Cybersecurity Intelligence")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: callback.onPromptSelected("Explain stack buffer overflow exploitation, EIP overwrite, and modern mitigations like ASLR and Stack Canaries"); break;
                    case 1: callback.onPromptSelected("Detail a reverse engineering workflow with Ghidra: decompiling x86_64 binaries and tracing control flow"); break;
                    case 2: callback.onPromptSelected("Explain how to audit and mitigate the OWASP Top 10 web vulnerabilities including SQLi, XSS, and SSRF"); break;
                    case 3: callback.onPromptSelected("Provide the essential Nmap scanning flags for stealth SYN scans, service version detection, and NSE scripts"); break;
                    case 4: callback.onPromptSelected("Explain Wireshark packet capture analysis with Berkeley Packet Filters and TLS handshake inspection"); break;
                    case 5: callback.onPromptSelected("Review modern cryptographic protocols: AES-GCM authenticated encryption, ECDH key exchange, and Argon2id hashing"); break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public static void showMedicalStudio(Context context, StudioActionCallback callback) {
        String[] options = {
            "🩺 Structured Clinical Differential Diagnosis Framework",
            "💊 Clinical Pharmacology & CYP450 Drug Interactions",
            "🩸 Diagnostic Complete Blood Count (CBC) & CMP Interpretation",
            "⚡ Acute Stroke Triage (FAST Protocol) & Emergency Workflows",
            "🦠 Antimicrobial Spectrum of Activity & Stewardship"
        };

        new AlertDialog.Builder(context, R.style.Theme_Jarvis_Dialog)
            .setTitle("🩺 Clinical Medicine & Healthcare Intelligence")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: callback.onPromptSelected("Provide a structured clinical differential diagnosis framework using the hypothetico-deductive model"); break;
                    case 1: callback.onPromptSelected("Explain clinical pharmacology, CYP450 enzyme inducers/inhibitors, and drug clearance half-lives"); break;
                    case 2: callback.onPromptSelected("Explain how to interpret a Complete Blood Count (CBC) with differential, MCV anemia classification, and platelets"); break;
                    case 3: callback.onPromptSelected("Explain the FAST stroke clinical protocol, emergency head CT indications, and thrombolysis time windows"); break;
                    case 4: callback.onPromptSelected("Outline antimicrobial mechanisms of action: beta-lactams, aminoglycosides, fluoroquinolones, and resistance patterns"); break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public static void showArtifactStudio(Context context, StudioActionCallback callback) {
        String[] options = {
            "📄 Executive Project Proposal Document Blueprint",
            "📊 Multi-Tab Financial Forecast Spreadsheet Structure",
            "🎯 10-Slide High-Impact Venture Pitch Deck Outline",
            "📐 Cloud Architecture Specification & Data Flow Diagram"
        };

        new AlertDialog.Builder(context, R.style.Theme_Jarvis_Dialog)
            .setTitle("📄 Artifact Creation Studio")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: callback.onPromptSelected("Generate a polished executive project proposal document with scope, milestones, budget, and deliverables"); break;
                    case 1: callback.onPromptSelected("Design a comprehensive multi-tab financial forecast spreadsheet with Revenue, COGS, OpEx, and Cash Flow"); break;
                    case 2: callback.onPromptSelected("Draft a complete 10-slide pitch deck presentation outline with slide titles, bullet points, and speaker notes"); break;
                    case 3: callback.onPromptSelected("Create a technical cloud architecture specification with microservices, database schemas, and API gateway routing"); break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
