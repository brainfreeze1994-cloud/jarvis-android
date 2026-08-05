package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.util.*;

/**
 * BusinessTools — AI-powered business document generation
 * Invoice, contract, pitch deck, business plan, email templates
 * All via HENRY AI — no extra API needed
 */
public class BusinessTools {

    public interface Callback {
        void onResult(String document);
        void onError(String msg);
    }

    private static final Handler H = new Handler(Looper.getMainLooper());

    public static boolean isBusinessQuery(String input) {
        String t = input.toLowerCase();
        return t.contains("invoice") || t.contains("contract") || t.contains("pitch")
            || t.contains("proposal") || t.contains("business plan") || t.contains("report")
            || t.contains("press release") || t.contains("nda") || t.contains("agreement")
            || t.contains("letter of intent") || t.contains("cover letter")
            || t.contains("terms and conditions") || t.contains("privacy policy")
            || t.contains("executive summary") || t.contains("meeting agenda")
            || t.contains("swot") || t.contains("kpi") || t.contains("okr");
    }

    public static String buildInvoicePrompt(String details) {
        return "Generate a professional invoice in plain text format with the following details: " + details +
            "\n\nInclude: Invoice number (auto-generate), Date (today), Due date (30 days), " +
            "Line items with quantities and prices, Subtotal, VAT (5% UAE standard), Total. " +
            "Format cleanly so it can be copied and pasted. Include H·E·N·R·Y™ branding at the bottom.";
    }

    public static String buildContractPrompt(String type, String details) {
        return "Generate a professional " + type + " contract/agreement in plain text with the following details: " + details +
            "\n\nInclude standard clauses: parties involved, scope, payment terms, confidentiality, " +
            "termination, governing law (UAE law, Dubai courts), signatures section. " +
            "Note: This is a template — parties should have it reviewed by a legal professional.";
    }

    public static String buildPitchDeckPrompt(String business, String details) {
        return "Create a complete pitch deck outline for: " + business + "\n\nAdditional context: " + details +
            "\n\nStructure:\n1. Problem\n2. Solution\n3. Market Size (TAM/SAM/SOM)\n4. Product\n" +
            "5. Business Model\n6. Traction\n7. Competition\n8. Team\n9. Financials\n10. The Ask\n\n" +
            "For each slide: give a title, 3-5 bullet points, and a key metric or visual suggestion. " +
            "Make it compelling, investor-ready, and concise.";
    }

    public static String buildBusinessPlanPrompt(String business, String details) {
        return "Write a comprehensive business plan for: " + business + "\n\nContext: " + details +
            "\n\nInclude: Executive Summary, Company Description, Market Analysis, " +
            "Organization & Management, Products/Services, Marketing Strategy, " +
            "Financial Projections (3-year), Funding Requirements. " +
            "Be specific, realistic, and Dubai/UAE market aware.";
    }

    public static String buildSWOTPrompt(String business) {
        return "Perform a detailed SWOT analysis for: " + business +
            "\n\nFormat as a clear table with Strengths, Weaknesses, Opportunities, Threats. " +
            "Be specific, insightful, and include at least 5 points per quadrant. " +
            "Follow with 3 strategic recommendations based on the analysis.";
    }

    public static String buildMeetingAgendaPrompt(String meeting, String details) {
        return "Create a professional meeting agenda for: " + meeting + "\n\nDetails: " + details +
            "\n\nInclude: Date/time/location, attendees list placeholder, " +
            "agenda items with time allocations, objectives for each item, " +
            "action items section, next meeting placeholder. " +
            "Keep it concise and actionable.";
    }

    public static String buildPressReleasePrompt(String announcement, String details) {
        return "Write a professional press release for: " + announcement + "\n\nDetails: " + details +
            "\n\nFormat: Headline, Dateline (Dubai, UAE), Body (inverted pyramid), " +
            "Quote from spokesperson, Boilerplate, Contact information. " +
            "Make it newsworthy, factual, and suitable for media distribution.";
    }

    public static String detectDocumentType(String input) {
        String t = input.toLowerCase();
        if (t.contains("invoice") || t.contains("bill")) return "invoice";
        if (t.contains("contract") || t.contains("agreement") || t.contains("nda")) return "contract";
        if (t.contains("pitch") || t.contains("pitch deck") || t.contains("investor")) return "pitch";
        if (t.contains("business plan")) return "business_plan";
        if (t.contains("swot")) return "swot";
        if (t.contains("agenda") || t.contains("meeting")) return "agenda";
        if (t.contains("press release") || t.contains("announcement")) return "press_release";
        if (t.contains("proposal")) return "proposal";
        if (t.contains("report")) return "report";
        return "document";
    }
}
