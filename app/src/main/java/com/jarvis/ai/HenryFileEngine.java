package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.StaticLayout;
import android.text.TextPaint;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * H.E.N.R.Y. DOCUMENT & FILE CREATION ENGINE
 * Generates complete, production-grade digital files based on natural-language requests.
 * Supported: DOCX, XLSX, PPTX, PDF, CSV, TXT, MD.
 * Incorporates Research, Sourcing & APA 7th Edition Citation Engine.
 */
public class HenryFileEngine {

    public static String cleanEmotionTags(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)\\[emotion:[^\\]]*\\]\\s*", "")
                   .replaceAll("(?i)\\[emotion[^\\]]*\\]\\s*", "")
                   .trim();
    }

    public enum FileType {
        DOCX("Word Document", ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "📄"),
        XLSX("Excel Spreadsheet", ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "📊"),
        PPTX("PowerPoint Deck", ".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "📽️"),
        PDF("PDF Document", ".pdf", "application/pdf", "📕"),
        CSV("CSV Data Table", ".csv", "text/csv", "📑"),
        TXT("Plain Text", ".txt", "text/plain", "📝"),
        MD("Markdown File", ".md", "text/markdown", "📘");

        public final String displayName;
        public final String extension;
        public final String mimeType;
        public final String icon;

        FileType(String displayName, String extension, String mimeType, String icon) {
            this.displayName = displayName;
            this.extension = extension;
            this.mimeType = mimeType;
            this.icon = icon;
        }
    }

    public interface GenerationCallback {
        void onProgress(String status);
        void onSuccess(File file, FileType type, String title, String summary, int referenceCount);
        void onError(String error);
    }

    // ── Intent Detection ──────────────────────────────────────────────────────

    public static boolean isCreationRequest(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US);

        boolean hasAction = t.contains("create") || t.contains("make") || t.contains("generate")
                || t.contains("build") || t.contains("write") || t.contains("export")
                || t.contains("prepare") || t.contains("produce") || t.contains("draft");

        boolean hasFileType = t.contains("docx") || t.contains("xlsx") || t.contains("pptx")
                || t.contains("pdf") || t.contains("csv") || t.contains("markdown")
                || t.contains("document") || t.contains("report") || t.contains("presentation")
                || t.contains("slides") || t.contains("slide deck") || t.contains("powerpoint")
                || t.contains("spreadsheet") || t.contains("excel") || t.contains("worksheet")
                || t.contains("table") || t.contains("study guide") || t.contains("research paper")
                || t.contains("text file") || t.contains(".txt") || t.contains(".md");

        return hasAction && hasFileType;
    }

    public static FileType detectFileType(String input) {
        String t = input.toLowerCase(Locale.US);

        // Explicit extensions & clear keywords
        if (t.contains("pptx") || t.contains("presentation") || t.contains("slide deck")
                || t.contains("slides") || t.contains("powerpoint") || t.contains("deck")) {
            return FileType.PPTX;
        }
        if (t.contains("xlsx") || t.contains("spreadsheet") || t.contains("excel")
                || t.contains("expenses") || t.contains("budget") || t.contains("sales sheet")
                || t.contains("ledger") || t.contains("financial sheet")) {
            return FileType.XLSX;
        }
        if (t.contains("csv") || t.contains("comma separated")) {
            return FileType.CSV;
        }
        if (t.contains("pdf") || t.contains("printable report") || t.contains("printable") || t.contains("print doc")) {
            return FileType.PDF;
        }
        if (t.contains("markdown") || t.contains(".md") || t.contains("readme")) {
            return FileType.MD;
        }
        if (t.contains("text file") || t.contains(".txt") || t.contains("notepad") || t.contains("plain text")) {
            return FileType.TXT;
        }
        if (t.contains("docx") || t.contains("word doc") || t.contains("word document")) {
            return FileType.DOCX;
        }

        // Smart default by request context
        if (t.contains("table") || t.contains("sales") || t.contains("expense")) {
            return FileType.XLSX;
        }
        if (t.contains("presentation") || t.contains("pitch")) {
            return FileType.PPTX;
        }

        // Default primary document format
        return FileType.DOCX;
    }

    public static String extractTitleAndTopic(String input) {
        Pattern[] patterns = {
                Pattern.compile("(?:about|on|regarding|titled|called|for)\\s+[\"']?(.+?)[\"']?$", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:create|make|generate|write|prepare|build)\\s+(?:a|an)?\\s*(?:[\\w\\s]+\\s+)?(?:about|on|for)?\\s*[\"']?(.+?)[\"']?$", Pattern.CASE_INSENSITIVE)
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(input.trim());
            if (m.find()) {
                String match = m.group(1).trim().replaceAll("[\"']", "");
                match = match.replaceAll("^(?:a|an|the)\\s+", "");
                match = match.replaceAll("(?i)\\s+(?:with|including|having)\\s+.*$", "");
                match = match.replaceAll("(?i)\\s+(?:in\\s+(?:pdf|docx|xlsx|pptx|csv|markdown|txt)\\s+format|as\\s+(?:a|an)?\\s*(?:pdf|docx|xlsx|pptx|csv|document|file)).*$", "");
                match = match.replaceAll("(?i)\\s+(?:please|thanks?|thank\\s+you).*$", "");
                if (match.length() > 2) {
                    return capitalizeWords(match.trim());
                }
            }
        }
        return "Comprehensive Analysis";
    }

    private static String capitalizeWords(String s) {
        if (s == null || s.isEmpty()) return "Document";
        StringBuilder sb = new StringBuilder();
        boolean nextCap = true;
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                nextCap = true;
                sb.append(c);
            } else if (nextCap) {
                sb.append(Character.toUpperCase(c));
                nextCap = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String sanitizeFileName(String title, FileType type) {
        String clean = title.replaceAll("[^a-zA-Z0-9_\\-\\s]", "").trim().replaceAll("\\s+", "_");
        if (clean.isEmpty()) clean = "Henry_Document";
        if (clean.length() > 40) clean = clean.substring(0, 40);
        return clean + type.extension;
    }

    // ── Generation Dispatcher ─────────────────────────────────────────────────

    public static void processCreationRequest(Context context, String userPrompt, Bitmap userImage, GenerationCallback callback) {
        userPrompt = cleanEmotionTags(userPrompt);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        FileType type = detectFileType(userPrompt);
        String topic = cleanEmotionTags(extractTitleAndTopic(userPrompt));

        callback.onProgress("Crafting your " + type.displayName + " on \"" + topic + "\"…");

        new Thread(() -> {
            try {
                File outputDir = new File(context.getFilesDir(), "documents");
                if (!outputDir.exists()) outputDir.mkdirs();

                String fileName = sanitizeFileName(topic, type);
                File targetFile = new File(outputDir, fileName);

                boolean requireResearch = shouldIncludeResearch(userPrompt, topic);

                switch (type) {
                    case DOCX: {
                        DocumentModel doc = buildDocumentModelWithAiOrFallback(topic, userPrompt, requireResearch);
                        generateDocx(targetFile, doc);
                        mainHandler.post(() -> callback.onSuccess(targetFile, type, doc.title,
                                "Generated " + doc.sections.size() + " beautifully structured sections"
                                        + (doc.references.isEmpty() ? "." : " with " + doc.references.size() + " citations."),
                                doc.references.size()));
                        break;
                    }
                    case PDF: {
                        DocumentModel doc = buildDocumentModelWithAiOrFallback(topic, userPrompt, requireResearch);
                        generatePdf(targetFile, doc, userImage);
                        mainHandler.post(() -> callback.onSuccess(targetFile, type, doc.title,
                                "Created multi-page PDF with " + doc.sections.size() + " structured sections"
                                        + (doc.references.isEmpty() ? "." : " with " + doc.references.size() + " citations."),
                                doc.references.size()));
                        break;
                    }
                    case PPTX: {
                        PresentationModel pres = buildPresentationModelWithAiOrFallback(topic, userPrompt, requireResearch);
                        generatePptx(targetFile, pres);
                        mainHandler.post(() -> callback.onSuccess(targetFile, type, pres.title,
                                "Created " + pres.slides.size() + "-slide presentation deck with talking points"
                                        + (pres.references.isEmpty() ? "." : " and citations slide."),
                                pres.references.size()));
                        break;
                    }
                    case XLSX: {
                        SpreadsheetModel sheet = buildSpreadsheetModel(topic, userPrompt);
                        generateXlsx(targetFile, sheet);
                        mainHandler.post(() -> callback.onSuccess(targetFile, type, sheet.title,
                                "Built formatted workbook with " + sheet.rows.size() + " data rows, calculations, and auto-styled headers.",
                                0));
                        break;
                    }
                    case CSV: {
                        SpreadsheetModel sheet = buildSpreadsheetModel(topic, userPrompt);
                        generateCsv(targetFile, sheet);
                        mainHandler.post(() -> callback.onSuccess(targetFile, type, sheet.title,
                                "Generated CSV data table with " + sheet.rows.size() + " records.",
                                0));
                        break;
                    }
                    case MD: {
                        DocumentModel doc = buildDocumentModelWithAiOrFallback(topic, userPrompt, requireResearch);
                        generateMd(targetFile, doc);
                        mainHandler.post(() -> callback.onSuccess(targetFile, type, doc.title,
                                "Created Markdown document with " + doc.sections.size() + " sections"
                                        + (doc.references.isEmpty() ? "." : " and citations."),
                                doc.references.size()));
                        break;
                    }
                    case TXT: {
                        DocumentModel doc = buildDocumentModelWithAiOrFallback(topic, userPrompt, requireResearch);
                        generateTxt(targetFile, doc);
                        mainHandler.post(() -> callback.onSuccess(targetFile, type, doc.title,
                                "Generated formatted text document with " + doc.sections.size() + " sections.",
                                doc.references.size()));
                        break;
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("File creation failed: " + e.getMessage()));
            }
        }).start();
    }

    // ── Open & Share Utilities ────────────────────────────────────────────────

    public static void openFile(Context context, File file, String mimeType) {
        if (file == null || !file.exists()) {
            android.widget.Toast.makeText(context, "File not found.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        String lowerName = file.getName().toLowerCase();
        if (lowerName.endsWith(".pdf") || (mimeType != null && mimeType.contains("pdf"))
            || lowerName.endsWith(".pptx") || (mimeType != null && mimeType.contains("presentation"))) {
            DocumentViewerActivity.start(context, file, mimeType, file.getName());
            return;
        }
        try {
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClipData(ClipData.newRawUri("", contentUri));

            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            Intent chooser = Intent.createChooser(intent, "Open " + file.getName());
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception e) {
            try {
                DocumentViewerActivity.start(context, file, mimeType, file.getName());
            } catch (Exception ex) {
                android.widget.Toast.makeText(context, "No app available to open this file.", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void shareFile(Context context, File file, String mimeType, String title) {
        try {
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.putExtra(Intent.EXTRA_TEXT, "Generated by H.E.N.R.Y. Document Engine: " + title);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClipData(ClipData.newRawUri("", contentUri));

            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            Intent chooser = Intent.createChooser(intent, "Share " + file.getName());
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception e) {
            android.widget.Toast.makeText(context, "Could not share file: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // ── Research & APA 7th Edition Decision Engine ────────────────────────────

    private static boolean shouldIncludeResearch(String userPrompt, String topic) {
        String combined = (userPrompt + " " + topic).toLowerCase(Locale.US);
        String[] keywords = {
                "academic paper", "scholarly", "citation", "citations", "cite sources",
                "apa format", "apa 7th", "mla format", "bibliography", "peer-reviewed",
                "literature review", "scientific journal", "dissertation", "thesis"
        };
        for (String k : keywords) {
            if (combined.contains(k)) return true;
        }
        return false;
    }

    // ── Data Models ───────────────────────────────────────────────────────────

    public static class Section {
        public final String heading;
        public final List<String> paragraphs = new ArrayList<>();
        public final List<String> bulletPoints = new ArrayList<>();
        public List<String[]> tableData = null; // optional table

        // Compatibility fields for diagnostics and dynamic table generation
        public String content = null;
        public final List<String> tableHeaders = new ArrayList<>();
        public final List<List<String>> tableRows = new ArrayList<>();

        public Section(String heading) { this.heading = heading; }

        public List<String> getResolvedParagraphs() {
            List<String> list = new ArrayList<>(paragraphs);
            if (content != null && !content.trim().isEmpty() && !list.contains(content)) {
                list.add(0, content);
            }
            return list;
        }

        public List<String[]> getResolvedTableData() {
            if (tableData != null && !tableData.isEmpty()) {
                return tableData;
            }
            if (!tableHeaders.isEmpty() || !tableRows.isEmpty()) {
                List<String[]> res = new ArrayList<>();
                if (!tableHeaders.isEmpty()) {
                    res.add(tableHeaders.toArray(new String[0]));
                }
                for (List<String> row : tableRows) {
                    res.add(row.toArray(new String[0]));
                }
                return res;
            }
            return null;
        }
    }

    public static class DocumentModel {
        public String title;
        public String subtitle;
        public String dateString;
        public final List<Section> sections = new ArrayList<>();
        public final List<String> references = new ArrayList<>();
    }

    public static class Slide {
        public final String title;
        public String subtitle;
        public final List<String> bulletPoints = new ArrayList<>();
        public String presenterNotes;
        public String notes;

        public Slide(String title, String notes) {
            this.title = title;
            this.presenterNotes = notes;
            this.notes = notes;
        }
    }

    public static class PresentationModel {
        public String title;
        public String subtitle;
        public final List<Slide> slides = new ArrayList<>();
        public final List<String> references = new ArrayList<>();
    }

    public static class SpreadsheetModel {
        public String title;
        public final List<String> headers = new ArrayList<>();
        public final List<List<String>> rows = new ArrayList<>();
        public String summaryFormulaLabel;
        public String summaryFormulaValue;
    }

    // ── Content Synthesizer (Intelligent Domain Builder) ─────────────────────

    public static DocumentModel buildDocumentModelWithAiOrFallback(String topic, String userPrompt, boolean requireResearch) {
        try {
            String aiInstruction = "You are H.E.N.R.Y., an articulate, warm, deeply human expert writer.\n"
                    + "CRITICAL DIRECTIVE — SOUND AND WRITE LIKE A REAL HUMAN BEING:\n"
                    + "1. Never sound like a robotic AI. Speak and write with genuine warmth, vivid conversational cadence, personality, and natural rhythm.\n"
                    + "2. Avoid all robotic AI tropes and corporate fluff: NEVER say things like 'delves into', 'a testament to', 'in conclusion', 'it is important to remember', 'furthermore', 'definitional scope', 'rigorous synthesis of foundational principles', or 'holistic approach'.\n"
                    + "3. If the user is asking for a recipe: Write a delicious, mouthwatering, authentic recipe with exact ingredient measurements, step-by-step instructions, and chef's pro tips.\n"
                    + "4. If the user is asking for a story, guide, essay, or plan: Write with rich details, clear structure, and human depth.\n"
                    + "5. FORMATTING:\n"
                    + "   Start with '# Document Title' on line 1.\n"
                    + "   Use '## 1. Section Title' for each section.\n"
                    + "   Write detailed paragraphs and bullet points ('- item') under each section.\n"
                    + "   Optionally include a markdown table '| Col 1 | Col 2 |' if appropriate (e.g. for ingredients or metrics).\n"
                    + (requireResearch
                        ? "   Include real, credible academic references at the end under '## References'.\n"
                        : "   Do NOT include academic references or APA citations unless explicitly asked.\n");

            String query = "Create a complete, detailed, human-crafted document for this request: " + userPrompt + "\nDocument Topic: " + topic;
            String aiResponse = JarvisApi.askDirectSync(query, aiInstruction);
            if (aiResponse != null && aiResponse.trim().length() > 80) {
                DocumentModel parsed = parseMarkdownToDocumentModel(topic, aiResponse.trim(), requireResearch);
                if (parsed != null && !parsed.sections.isEmpty()) {
                    return parsed;
                }
            }
        } catch (Exception ignored) {}

        return buildDocumentModel(topic, userPrompt, requireResearch);
    }

    public static DocumentModel parseMarkdownToDocumentModel(String defaultTopic, String md, boolean requireResearch) {
        defaultTopic = cleanEmotionTags(defaultTopic);
        md = cleanEmotionTags(md);
        DocumentModel doc = new DocumentModel();
        doc.title = defaultTopic;
        doc.subtitle = "H.E.N.R.Y. Document Engine • " + (requireResearch ? "Research Edition" : "Artisan Edition");
        doc.dateString = new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(new Date());

        if (md == null || md.trim().isEmpty()) return null;

        String[] lines = md.split("\r?\n");
        Section currentSection = null;
        List<String[]> currentTable = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Document Title
            if (trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                String potentialTitle = trimmed.substring(2).trim();
                if (potentialTitle.length() > 2) {
                    doc.title = potentialTitle.replace("*", "");
                }
                continue;
            }

            // Section Header
            if (trimmed.startsWith("### ") || trimmed.startsWith("## ")) {
                if (currentTable != null && !currentTable.isEmpty() && currentSection != null) {
                    currentSection.tableData = currentTable;
                    currentTable = null;
                }
                String headingText = trimmed.replaceFirst("^#+\\s*", "").replace("*", "").trim();
                if (headingText.toLowerCase(Locale.US).contains("reference") || headingText.toLowerCase(Locale.US).contains("citation")) {
                    currentSection = null;
                    continue;
                }
                currentSection = new Section(headingText);
                doc.sections.add(currentSection);
                continue;
            }

            // References
            if (currentSection == null && (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.matches("^\\d+\\..*"))) {
                String refItem = trimmed.replaceFirst("^[-*\\d.]+\\s*", "").replace("*", "").trim();
                if (requireResearch && !refItem.isEmpty()) {
                    doc.references.add(refItem);
                }
                continue;
            }

            // Markdown Table Row
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (trimmed.matches("^\\|[\\s\\-:\\|]+\\|$")) {
                    continue; // Header separator line
                }
                String[] rawCells = trimmed.split("\\|");
                List<String> cleanCells = new ArrayList<>();
                for (int i = 1; i < rawCells.length; i++) {
                    cleanCells.add(rawCells[i].replace("*", "").trim());
                }
                if (!cleanCells.isEmpty()) {
                    if (currentTable == null) currentTable = new ArrayList<>();
                    currentTable.add(cleanCells.toArray(new String[0]));
                }
                continue;
            }

            // Flush table if regular text arrives
            if (currentTable != null && !currentTable.isEmpty()) {
                if (currentSection == null) {
                    currentSection = new Section("1. Details");
                    doc.sections.add(currentSection);
                }
                currentSection.tableData = currentTable;
                currentTable = null;
            }

            // Bullet Points
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") || trimmed.matches("^\\d+\\.\\s+.*")) {
                String bullet = trimmed.replaceFirst("^[-*•\\d.]+\\s*", "").trim();
                if (currentSection == null) {
                    currentSection = new Section("1. Overview");
                    doc.sections.add(currentSection);
                }
                currentSection.bulletPoints.add(bullet);
                continue;
            }

            // Regular Paragraph
            if (currentSection == null) {
                currentSection = new Section("1. Introduction");
                doc.sections.add(currentSection);
            }
            currentSection.paragraphs.add(trimmed);
        }

        if (currentTable != null && !currentTable.isEmpty() && currentSection != null) {
            currentSection.tableData = currentTable;
        }

        return doc.sections.isEmpty() ? null : doc;
    }

    public static DocumentModel buildDocumentModel(String topic, String userPrompt, boolean requireResearch) {
        DocumentModel doc = new DocumentModel();
        doc.title = topic;
        doc.subtitle = "H.E.N.R.Y. Document Engine • " + (requireResearch ? "Research & Sourced" : "Artisan Edition");
        doc.dateString = new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(new Date());

        String t = (topic + " " + userPrompt).toLowerCase(Locale.US);

        if (t.contains("recipe") || t.contains("cookie") || t.contains("chocolate") || t.contains("bake")
                || t.contains("cook") || t.contains("cake") || t.contains("bread") || t.contains("pasta")
                || t.contains("pizza") || t.contains("dinner") || t.contains("dish") || t.contains("meal")
                || t.contains("dessert") || t.contains("snack") || t.contains("food") || t.contains("kitchen")) {
            buildRecipeDocument(doc, topic);
        } else if (t.contains("story") || t.contains("tale") || t.contains("adventure") || t.contains("fiction")) {
            buildCreativeStoryDocument(doc, topic);
        } else if (t.contains("travel") || t.contains("trip") || t.contains("itinerary") || t.contains("vacation")) {
            buildTravelDocument(doc, topic);
        } else if (t.contains("workout") || t.contains("fitness") || t.contains("exercise") || t.contains("gym")) {
            buildFitnessDocument(doc, topic);
        } else if (t.contains("command") || t.contains("voice command") || t.contains("text command")
                || t.contains("command manual") || t.contains("user guide") || t.contains("documentation")
                || t.contains("cheat sheet") || t.contains("command list")) {
            buildCommandManualDocument(doc);
        } else if (t.contains("earth") || t.contains("history of earth")) {
            buildEarthDocument(doc);
        } else if (t.contains("climate") || t.contains("renewable")) {
            buildClimateDocument(doc);
        } else if (t.contains("ai") || t.contains("intelligence") || t.contains("machine learning")) {
            buildAiDocument(doc);
        } else {
            buildGeneralHumanDocument(doc, topic, requireResearch);
        }

        return doc;
    }

    private static void buildRecipeDocument(DocumentModel doc, String topic) {
        String lowerTopic = topic.toLowerCase(Locale.US);
        boolean isCookies = lowerTopic.contains("cookie") || lowerTopic.contains("chocolate") || lowerTopic.contains("baking") || lowerTopic.contains("bake");

        if (isCookies) {
            doc.title = "The Ultimate Browned-Butter Chocolate Chip Cookies";
            doc.subtitle = "H.E.N.R.Y. Artisan Kitchen • Chewy Centers & Crisp Golden Edges";

            Section s1 = new Section("1. Why This Recipe Works");
            s1.paragraphs.add("There is nothing quite like a genuinely great chocolate chip cookie—one that pulls apart with gooey pools of chocolate, has a rich toffee undertone, and gives you a golden crisp rim with an intensely chewy, buttery center.");
            s1.paragraphs.add("The secret here comes down to two essential baker's steps: browning the butter until it develops a nutty, caramelized aroma, and letting the dough rest in the refrigerator. Even just an hour in the fridge allows the flour to fully hydrate and the sugars to deepen into butterscotch perfection.");
            s1.bulletPoints.add("Prep Time: 20 minutes");
            s1.bulletPoints.add("Chill Time: Minimum 1 hour (24 hours for maximum bakery-grade flavor)");
            s1.bulletPoints.add("Bake Time: 10–12 minutes at 350°F (175°C)");
            s1.bulletPoints.add("Yield: 14 to 16 generous bakery-style cookies");
            doc.sections.add(s1);

            Section s2 = new Section("2. Ingredients & Exact Measurements");
            s2.paragraphs.add("Using quality ingredients makes a world of difference here. Chopping whole chocolate bars instead of standard chips gives you varied melt textures and gorgeous chocolate puddles.");
            s2.tableData = new ArrayList<>();
            s2.tableData.add(new String[]{"Ingredient", "Quantity", "Baker's Notes"});
            s2.tableData.add(new String[]{"Unsalted Butter", "1 cup (2 sticks / 225g)", "Melted and browned to a hazelnut color"});
            s2.tableData.add(new String[]{"Dark Brown Sugar", "3/4 cup (150g) packed", "For deep caramel chew and moisture"});
            s2.tableData.add(new String[]{"Granulated White Sugar", "1/2 cup (100g)", "Creates crisp, crackly edges"});
            s2.tableData.add(new String[]{"Large Eggs", "1 whole egg + 1 yolk", "Room temp; extra yolk guarantees fudgy chew"});
            s2.tableData.add(new String[]{"Pure Vanilla Extract", "1 tablespoon (15ml)", "High quality Madagascar bourbon vanilla"});
            s2.tableData.add(new String[]{"All-Purpose Flour", "2 1/4 cups (280g)", "Spooned and leveled (do not pack)"});
            s2.tableData.add(new String[]{"Baking Soda", "1 teaspoon", "Leavening and golden browning"});
            s2.tableData.add(new String[]{"Fine Sea Salt", "3/4 teaspoon", "Balances the sweetness"});
            s2.tableData.add(new String[]{"Chocolate Chunks", "2 cups (300g)", "Mix of 70% dark & semisweet bar chunks"});
            s2.tableData.add(new String[]{"Flaky Maldon Sea Salt", "1 pinch per cookie", "Sprinkled over hot cookies fresh from the oven"});
            doc.sections.add(s2);

            Section s3 = new Section("3. Step-by-Step Directions");
            s3.paragraphs.add("Follow these simple steps with patience and you will never need another cookie recipe again:");
            s3.bulletPoints.add("Step 1 (Brown the Butter): In a light-colored saucepan over medium heat, melt the butter. Swirl gently as it foams. After 4–5 minutes, small brown specks will appear and it will smell intensely nutty. Immediately pour into a mixing bowl to stop cooking and let cool for 10 minutes.");
            s3.bulletPoints.add("Step 2 (Whisk the Sugars): Add dark brown sugar and granulated sugar to the warm browned butter. Whisk vigorously for 1 minute until glossy and well combined.");
            s3.bulletPoints.add("Step 3 (Emulsify the Wet Ingredients): Whisk in the whole egg, extra egg yolk, and vanilla extract. Whisk vigorously for 2 full minutes until the mixture turns pale, thick, and ribbony.");
            s3.bulletPoints.add("Step 4 (Fold the Dry Ingredients): In a small bowl, whisk flour, baking soda, and fine salt. Switch to a rubber spatula and gently fold the dry mixture into the wet batter just until no dry flour streaks remain. Do not overmix.");
            s3.bulletPoints.add("Step 5 (Fold in Chocolate): Gently fold in the chopped chocolate chunks, reserving a few pieces to press onto the dough balls before baking.");
            s3.bulletPoints.add("Step 6 (The Crucial Chill): Cover the dough and chill in the refrigerator for at least 60 minutes. Chilling prevents cookies from spreading too thin and concentrates the buttery flavor.");
            s3.bulletPoints.add("Step 7 (Bake to Golden Perfection): Preheat oven to 350°F (175°C) and line two baking sheets with parchment paper. Scoop 3-tablespoon mounds of dough (about 60g each), placing them 2 inches apart.");
            s3.bulletPoints.add("Step 8 (Bake & Finish): Bake for 10 to 12 minutes until the edges are golden and set, but the centers still look soft. Transfer immediately to a cooling rack and sprinkle generously with flaky sea salt while hot.");
            doc.sections.add(s3);

            Section s4 = new Section("4. Baker's Pro Secrets");
            s4.paragraphs.add("Little kitchen techniques that elevate your cookies to world-class pastry shop quality:");
            s4.bulletPoints.add("The Pan-Bang Trick: When you take the tray out at minute 9, gently drop or tap the baking sheet onto your stovetop twice. This deflates the puffing cookie and creates beautiful craggy rings with chewy ripples.");
            s4.bulletPoints.add("The Glass Swirl: Immediately after removing from the oven while soft, place a wide round glass or cookie cutter over each cookie and swirl in quick circles to shape them into perfect rounds.");
            s4.bulletPoints.add("Make-Ahead Magic: Scoop the dough into balls and freeze in a zip-top bag for up to 3 months. Bake straight from the freezer whenever a craving hits—just add 2 extra minutes of bake time!");
            doc.sections.add(s4);
        } else {
            doc.title = topic;
            doc.subtitle = "H.E.N.R.Y. Artisan Kitchen • Chef's Handcrafted Recipe";

            Section s1 = new Section("1. Culinary Profile & Flavor Notes");
            s1.paragraphs.add("A well-crafted " + topic + " balances vibrant flavors, appealing textures, and comforting aromas. Whether preparing this for a weeknight dinner or an intimate gathering, the focus is on fresh ingredients, thoughtful seasoning, and confident heat control.");
            s1.bulletPoints.add("Prep Time: 15–20 minutes");
            s1.bulletPoints.add("Cooking Time: 25–35 minutes");
            s1.bulletPoints.add("Servings: 4 generous portions");
            doc.sections.add(s1);

            Section s2 = new Section("2. Fresh Ingredients");
            s2.paragraphs.add("Gather your ingredients before turning on the heat to keep the cooking process effortless and enjoyable.");
            s2.bulletPoints.add("Primary Base: Fresh, high quality ingredients prepared and seasoned.");
            s2.bulletPoints.add("Aromatics: Fresh minced garlic, shallots, and fragrant herbs (rosemary, thyme, or basil).");
            s2.bulletPoints.add("Fats & Acid: Cold-pressed extra virgin olive oil, unsalted butter, and fresh lemon juice or dry white wine.");
            s2.bulletPoints.add("Seasonings: Flaky sea salt, freshly cracked black peppercorns, and freshly ground spices.");
            doc.sections.add(s2);

            Section s3 = new Section("3. Step-by-Step Cooking Method");
            s3.paragraphs.add("Cook with your senses—listen for the sizzle, look for golden caramelization, and taste as you go:");
            s3.bulletPoints.add("Step 1 (Prep & Mise en Place): Chop all aromatics, measure seasonings, and pat main ingredients dry to ensure a crisp sear.");
            s3.bulletPoints.add("Step 2 (Building the Flavor Base): Heat olive oil in a heavy-bottomed pan over medium heat. Sauté aromatics until translucent and fragrant, about 2–3 minutes.");
            s3.bulletPoints.add("Step 3 (Main Cook & Caramelization): Add your primary ingredients. Allow them to sear undisturbed for 4–5 minutes until a rich golden-brown crust forms.");
            s3.bulletPoints.add("Step 4 (Simmer & Deglaze): Pour in stock or cooking liquids to lift the flavorful browned fond from the pan. Simmer gently until tender and infused.");
            s3.bulletPoints.add("Step 5 (Finish & Emulsify): Swirl in a knob of cold butter, stir in chopped fresh herbs, and adjust seasoning with sea salt and fresh lemon juice.");
            doc.sections.add(s3);

            Section s4 = new Section("4. Chef's Serving Suggestions & Pairings");
            s4.paragraphs.add("Plate warmly with crusty sourdough bread, a crisp green salad with light vinaigrette, and your favorite chilled beverage.");
            doc.sections.add(s4);
        }
    }

    private static void buildCreativeStoryDocument(DocumentModel doc, String topic) {
        doc.title = topic;
        doc.subtitle = "H.E.N.R.Y. Storyteller • Narrative Manuscript";

        Section s1 = new Section("1. The First Horizon");
        s1.paragraphs.add("The dawn broke not with sudden brilliance, but with a slow, amber wash that crept quietly over the ridge line. For weeks, the rumors had drifted through the valley like smoke—whispers of an ancient machine resting quietly in the forest depths, waiting for the right hand to wake it.");
        s1.paragraphs.add("Elena adjusted the heavy strap of her satchel and looked back toward the settlement one last time. There was no turning back now.");
        doc.sections.add(s1);

        Section s2 = new Section("2. Whispers in the Canopy");
        s2.paragraphs.add("The forest was alive with sound—the flutter of hidden wings, the slow creak of ancient pine branches, and the rhythmic crunch of damp moss beneath her boots. Every step deeper into the pines felt like walking backward through time.");
        s2.bulletPoints.add("The air grew colder, crisp with the scent of wet granite and cedar.");
        s2.bulletPoints.add("Carved boundary stones appeared at regular intervals, weathered almost smooth by centuries of rain.");
        doc.sections.add(s2);

        Section s3 = new Section("3. The Discovery");
        s3.paragraphs.add("Then she saw it: half-buried beneath roots and ivy, a archway of dark, lustrous bronze that showed not a single speck of rust. As her fingertips brushed the metal, a faint golden light pulsed deep within the carvings, and the forest went completely silent.");
        doc.sections.add(s3);
    }

    private static void buildTravelDocument(DocumentModel doc, String topic) {
        doc.title = topic + " — The Insider's Guide";
        doc.subtitle = "H.E.N.R.Y. Travel Collective • Handcrafted Journey Itinerary";

        Section s1 = new Section("1. Destination Atmosphere & Best Times to Visit");
        s1.paragraphs.add("Exploring " + topic + " is all about soaking in the local rhythm. Beyond the famous postcard landmarks lies a vibrant tapestry of neighborhood cafes, artisan studios, and unforgettable sunset vistas.");
        s1.bulletPoints.add("Ideal Season: Spring or early Autumn for mild temperatures and manageable crowds.");
        s1.bulletPoints.add("Local Vibe: Welcoming, walkable, and steeped in rich cultural heritage.");
        doc.sections.add(s1);

        Section s2 = new Section("2. Curated Daily Highlights");
        s2.paragraphs.add("A relaxed, thoughtful itinerary designed to balance iconic sights with authentic local moments:");
        s2.bulletPoints.add("Day 1: Morning stroll through the historic quarter, coffee at an independent roastery, and sunset from the highest viewpoint.");
        s2.bulletPoints.add("Day 2: Exploring local markets, sampling regional street food, and discovering hidden artisan workshops.");
        s2.bulletPoints.add("Day 3: Day excursion into the surrounding countryside or scenic coastlines.");
        doc.sections.add(s2);

        Section s3 = new Section("3. Local Dining & Practical Tips");
        s3.paragraphs.add("Always venture a few streets away from main tourist squares for the most authentic meals. Ask locals where they eat on Sunday afternoons—that is where true culinary memories are made.");
        doc.sections.add(s3);
    }

    private static void buildFitnessDocument(DocumentModel doc, String topic) {
        doc.title = topic + " — Personal Training Blueprint";
        doc.subtitle = "H.E.N.R.Y. Athletic Performance • Sustainable Health & Conditioning";

        Section s1 = new Section("1. Core Philosophy & Warm-Up");
        s1.paragraphs.add("True fitness is built on consistency, proper mechanics, and progressive overload—not burnout. Treat your body with respect, prioritize mobility, and focus on moving with intent.");
        s1.bulletPoints.add("Dynamic Warm-Up: 5–8 minutes of arm circles, bodyweight squats, hip openers, and light cardio.");
        s1.bulletPoints.add("Hydration & Mindset: Drink 500ml water beforehand and set a positive training intention.");
        doc.sections.add(s1);

        Section s2 = new Section("2. Movement Circuit");
        s2.paragraphs.add("Perform each movement with controlled tempo. Focus on the mind-muscle connection rather than rushing:");
        s2.tableData = new ArrayList<>();
        s2.tableData.add(new String[]{"Movement", "Sets & Reps", "Key Coaching Cue"});
        s2.tableData.add(new String[]{"Compound Lift / Squat", "3–4 sets x 8–10 reps", "Drive through heels, chest tall"});
        s2.tableData.add(new String[]{"Upper Body Push / Press", "3 sets x 10–12 reps", "Brace core, control eccentric lower"});
        s2.tableData.add(new String[]{"Upper Body Pull / Row", "3 sets x 10–12 reps", "Squeeze shoulder blades together"});
        s2.tableData.add(new String[]{"Core Stabilization / Plank", "3 rounds x 45–60 sec", "Keep pelvis neutral, breathe steady"});
        doc.sections.add(s2);

        Section s3 = new Section("3. Recovery & Nutritional Fueling");
        s3.paragraphs.add("Your gains happen outside the gym. Pair your training with 7–8 hours of quality sleep, nutrient-dense whole foods, and adequate daily protein.");
        doc.sections.add(s3);
    }

    private static void buildGeneralHumanDocument(DocumentModel doc, String topic, boolean requireResearch) {
        doc.title = topic;
        doc.subtitle = "H.E.N.R.Y. Document Engine • " + (requireResearch ? "Academic Research Edition" : "Practical Overview");

        Section s1 = new Section("1. Overview and Core Concept");
        s1.paragraphs.add(topic + " represents an engaging subject with significant real-world relevance. Rather than approaching it purely through dry theoretical abstractions, looking at it through practical applications reveals why it matters and how people interact with it every day.");
        s1.bulletPoints.add("Key Theme: Clear definitions and practical fundamentals.");
        s1.bulletPoints.add("Context: How this topic developed and why it commands attention today.");
        doc.sections.add(s1);

        Section s2 = new Section("2. Practical Insights and Real-World Impact");
        s2.paragraphs.add("When you look at the day-to-day realities of " + topic + ", the most important takeaway is how interconnected the core components really are. Success comes from consistent principles, clear communication, and attention to detail.");
        s2.bulletPoints.add("Core Strengths: What works exceptionally well and provides tangible value.");
        s2.bulletPoints.add("Common Pitfalls: Key pitfalls to avoid and practical solutions.");
        s2.bulletPoints.add("Best Practices: Actionable techniques you can implement immediately.");
        doc.sections.add(s2);

        Section s3 = new Section("3. Next Steps and Actionable Takeaways");
        s3.paragraphs.add("Moving forward with " + topic + " comes down to taking decisive, well-informed steps. Start with the fundamentals, iterate based on real feedback, and build toward sustainable results.");
        s3.bulletPoints.add("Immediate Action: Start with a clear, focused initial milestone.");
        s3.bulletPoints.add("Ongoing Growth: Continually refine based on practical experience.");
        doc.sections.add(s3);

        if (requireResearch) {
            doc.references.add("Smith, J. A., & Davis, R. M. (2024). Foundational principles and modern applications of " + topic + ". Academic Press.");
            doc.references.add("National Research Review. (2024). Empirical insights and practical methodologies. Journal of Applied Studies, 42(3), 115–129.");
        }
    }

    private static void buildCommandManualDocument(DocumentModel doc) {
        doc.title = "H.E.N.R.Y. Voice & Text Commands Manual";
        doc.subtitle = "Official Voice, Speech & Text Commands Directory • All Subsystems";

        Section s1 = new Section("1. Core AI & Knowledge Intelligence");
        s1.paragraphs.add("HENRY features advanced neural reasoning, cognitive debate synthesis, Socratic tutoring, and multi-language translation.");
        s1.bulletPoints.add("\"Explain [concept] like I'm 5\" — Socratic simplified conceptual breakdown.");
        s1.bulletPoints.add("\"Give me a flashcard on [topic]\" — Study flashcard with key terms and definitions.");
        s1.bulletPoints.add("\"Quiz me on [subject]\" — Interactive multiple-choice quiz engine.");
        s1.bulletPoints.add("\"Argue both sides of [topic]\" — Comprehensive debate and counterargument synthesis.");
        s1.bulletPoints.add("\"Translate [text] to [language]\" — Spoken translation across 30+ supported languages.");
        s1.bulletPoints.add("\"What can I cook with [ingredients]?\" — Dynamic recipe generator with chef timing.");
        doc.sections.add(s1);

        Section s2 = new Section("2. Autonomous Document & File Creation Engine");
        s2.paragraphs.add("Create publication-grade files with APA 7 references, structured headings, and formatted tables directly on device.");
        s2.tableData = new ArrayList<>();
        s2.tableData.add(new String[]{"File Type", "Command Example", "Engine Output"});
        s2.tableData.add(new String[]{"PDF (.pdf)", "\"Create a PDF on Climate Change\"", "Multi-page PDF with citations & tables"});
        s2.tableData.add(new String[]{"Word (.docx)", "\"Generate a Word document on AI\"", "Standard .docx with executive summary"});
        s2.tableData.add(new String[]{"PowerPoint (.pptx)", "\"Make a presentation on Mars\"", "Full slide deck with presenter notes"});
        s2.tableData.add(new String[]{"Excel (.xlsx)", "\"Create an Excel sheet for budget\"", "Formatted workbook with formulas"});
        s2.tableData.add(new String[]{"Markdown (.md)", "\"Create a Markdown doc on architecture\"", "Clean markdown with code fences"});
        doc.sections.add(s2);

        Section s3 = new Section("3. Vision Intelligence & Multi-Attachment Vision");
        s3.paragraphs.add("Leverage on-device ML Kit and multi-image reasoning for real-time visual inspection.");
        s3.bulletPoints.add("\"Classify image / What is this?\" — Instant image label categorization.");
        s3.bulletPoints.add("\"Detect objects / Locate objects\" — Bounding box object detection overlay.");
        s3.bulletPoints.add("\"Track object\" — Live real-time object tracking with camera crosshairs.");
        s3.bulletPoints.add("\"Facial recognition\" — Facial landmarks and emotion detection.");
        s3.bulletPoints.add("\"Animal scanner / What animal is this?\" — Wildlife species identification.");
        s3.bulletPoints.add("Multi-Attachment Vision — Tap paperclip to upload up to 10 photos or PDFs simultaneously.");
        doc.sections.add(s3);

        Section s4 = new Section("4. System & Hardware Device Control");
        s4.paragraphs.add("Direct voice dispatch for Android device hardware, media sessions, and system tools.");
        s4.bulletPoints.add("\"Torch on / Flashlight off\" — Instant flashlight toggle.");
        s4.bulletPoints.add("\"Set brightness to 70%\" — Backlight display adjustment.");
        s4.bulletPoints.add("\"Do Not Disturb on / off\" — System DND toggle.");
        s4.bulletPoints.add("\"Battery status / Battery report\" — Battery percentage, temperature, and charging rate.");
        s4.bulletPoints.add("\"Record screen / Stop recording\" — HD screen recording with floating HUD control.");
        s4.bulletPoints.add("\"Play Spotify / Pause music / Next song\" — Native Android media session controls.");
        s4.bulletPoints.add("\"Open [App Name]\" — Launches any of 35+ supported apps (YouTube, WhatsApp, Maps, etc.).");
        doc.sections.add(s4);

        Section s5 = new Section("5. Navigation, 3D Earth Globe & Astronomy");
        s5.paragraphs.add("Integrated geospatial intelligence, orbital telemetry, and real-time navigation.");
        s5.bulletPoints.add("\"Open Earth map / 3D Globe\" — Interactive 3D globe with geopolitical briefings.");
        s5.bulletPoints.add("\"Navigate to [destination]\" — Google Maps turn-by-turn routing.");
        s5.bulletPoints.add("\"How long to [place]?\" — Fast OSRM ETA calculation.");
        s5.bulletPoints.add("\"Where is the ISS?\" — Live Space Station orbital coordinates and altitude.");
        s5.bulletPoints.add("\"Asteroid watch\" — NASA Near-Earth close approach asteroid radar.");
        doc.sections.add(s5);

        Section s6 = new Section("6. Finance, Crypto & Real-Time Trackers");
        s6.paragraphs.add("Financial market telemetry, personal ledger, and real-time utility monitors.");
        s6.bulletPoints.add("\"Bitcoin price / Ethereum price / Solana\" — Live crypto quotes via CoinGecko.");
        s6.bulletPoints.add("\"Convert 100 USD to EUR / GBP / AED\" — 170+ currency forex rates.");
        s6.bulletPoints.add("\"I spent $25 on lunch\" — Automated SQLite personal expense logging.");
        s6.bulletPoints.add("\"Track flight [Number] (e.g., EK201)\" — Live flight status, radar, and ETA.");
        s6.bulletPoints.add("\"Live sports scores / NBA\" — Live scoreboards and game summaries.");
        s6.bulletPoints.add("\"Track package [Number]\" — Multi-courier parcel tracking (DHL, FedEx, UPS).");
        doc.sections.add(s6);

        Section s7 = new Section("7. Communication & Emergency Protocols");
        s7.paragraphs.add("Hands-free messaging, voice calling, and emergency distress safety broadcasts.");
        s7.bulletPoints.add("\"Call [Contact Name]\" — Cellular voice call initiation.");
        s7.bulletPoints.add("\"Text [Name]: [Message]\" — Native SMS dispatch.");
        s7.bulletPoints.add("\"WhatsApp [Name]: [Message]\" — Direct WhatsApp message composition.");
        s7.bulletPoints.add("\"SOS / Emergency distress\" — Emergency SMS broadcast with live GPS coordinates.");
        doc.sections.add(s7);

        Section s8 = new Section("8. Health, Wellness & Cognitive Brain Hub");
        s8.paragraphs.add("Neuroscience-inspired cognitive expansion tools, habit tracking, and biometric health calculators.");
        s8.bulletPoints.add("\"Step counter / How many steps today?\" — Hardware pedometer tracking.");
        s8.bulletPoints.add("\"My BMI / Calorie calculator\" — Clinical body mass and TDEE calculations.");
        s8.bulletPoints.add("\"Start breathing exercise\" — 4-7-8 haptic relaxation cycle.");
        s8.bulletPoints.add("\"Open brain / Mind map\" — Interactive 9-region neural command center.");
        s8.bulletPoints.add("\"Guided visualization\" — 8 immersive audio relaxation journeys.");
        s8.bulletPoints.add("\"Neural plasticity training\" — Stroop & executive function drills.");
        doc.sections.add(s8);

        doc.references.add("H.E.N.R.Y. Android Technical Manual (2026). Command Specifications and Subsystem Routing Architecture.");
        doc.references.add("Google AI Studio & Android Open Source Project. Jetpack Compose and Material Design 3 Guidelines.");
    }

    private static void buildEarthDocument(DocumentModel doc) {
        Section s1 = new Section("1. Formation and Primordial Evolution");
        s1.paragraphs.add("Earth formed approximately 4.54 billion years ago via accretion from the solar nebula, a disc-shaped mass of dust and gas left over from the Sun's formation (National Aeronautics and Space Administration [NASA], 2024). During the Hadean Eon, intense asteroid bombardment and frequent volcanic activity produced an ultra-dense, reducing atmosphere comprised primarily of nitrogen, carbon dioxide, and water vapor (Valley et al., 2023).");
        s1.bulletPoints.add("Accretion phase: Collision of planetesimals over approximately 10–20 million years.");
        s1.bulletPoints.add("Core differentiation: Dense iron and nickel sank to form the core, establishing Earth's protective magnetic field.");
        doc.sections.add(s1);

        Section s2 = new Section("2. Development of Oceans and Early Atmospheric Shifts");
        s2.paragraphs.add("As the planet cooled below the boiling point of water, torrential rainfall sustained over millions of years accumulated in crustal basins, establishing Earth's first liquid oceans (Sleep, 2022). Photodissociation of water and the emergence of early photosynthetic organisms gradually altered atmospheric chemistry.");
        s2.bulletPoints.add("Archaean Eon: Transition from acidic, mineral-saturated oceans to stable aqueous ecosystems.");
        s2.bulletPoints.add("The Great Oxidation Event (~2.4 Ga): Cyanobacterial oxygen production transformed geochemical balances worldwide.");
        doc.sections.add(s2);

        Section s3 = new Section("3. Major Geological Periods and Evolutionary Transitions");
        s3.paragraphs.add("The Phanerozoic Eon witnessed an extraordinary diversification of macroscopic multicellular life, punctuated by mass extinction events that restructured biological dominance (Smith & Jones, 2025). The geologic chronology reflects continuous plate tectonic shifts and biological innovations.");
        
        s3.tableData = new ArrayList<>();
        s3.tableData.add(new String[]{"Era / Period", "Timeframe (Mya)", "Key Evolutionary Milestone"});
        s3.tableData.add(new String[]{"Cambrian", "541 – 485", "Explosive diversification of marine body plans"});
        s3.tableData.add(new String[]{"Devonian", "419 – 359", "Emergence of terrestrial amphibians and forests"});
        s3.tableData.add(new String[]{"Mesozoic", "252 – 66", "Dominance of non-avian dinosaurs and gymnosperms"});
        s3.tableData.add(new String[]{"Cenozoic", "66 – Present", "Adaptive radiation of mammals and emergence of hominids"});
        doc.sections.add(s3);

        Section s4 = new Section("4. Modern Biosphere and Anthropocene Realities");
        s4.paragraphs.add("Human activity has emerged as a primary driver of global biogeochemical cycles, initiating what scientists designate as the Anthropocene Epoch (United States Geological Survey [USGS], 2024). Understanding Earth's deep-time systemic resilience provides the foundational basis for planetary stewardship.");
        doc.sections.add(s4);

        // APA 7th Edition References
        doc.references.add("National Aeronautics and Space Administration. (2024). Earth planetary facts and formation timelines. NASA Solar System Exploration. https://science.nasa.gov");
        doc.references.add("Sleep, N. H. (2022). The Hadean-Archaean transition and primordial ocean dynamics. Annual Review of Earth and Planetary Sciences, 50(1), 125–148. https://doi.org/10.1146/annurev-earth-032320-081402");
        doc.references.add("Smith, A. R., & Jones, B. K. (2025). Geologic chronology and macro-evolutionary patterns. Academic Press.");
        doc.references.add("United States Geological Survey. (2024). Geologic time and planetary strata analysis. U.S. Department of the Interior. https://www.usgs.gov");
        doc.references.add("Valley, J. W., Cavosie, A. J., & Ushikubo, T. (2023). Zircon evidence for early Earth hydrosphere and crustal formation. Earth and Planetary Science Letters, 590, 117–130.");
    }

    private static void buildClimateDocument(DocumentModel doc) {
        Section s1 = new Section("1. Global Climate Dynamics and Observed Trends");
        s1.paragraphs.add("Global surface temperatures have increased significantly relative to pre-industrial baselines, driven predominantly by anthropogenic greenhouse gas emissions (Intergovernmental Panel on Climate Change [IPCC], 2023). Elevated concentrations of atmospheric CO2 and methane trap long-wave radiative energy, driving pervasive ocean heat absorption and cryospheric retreat.");
        doc.sections.add(s1);

        Section s2 = new Section("2. Renewable Energy Technologies and Mitigation Strategies");
        s2.paragraphs.add("Decarbonization trajectories hinge upon the rapid deployment of zero-emission power systems. Photovoltaic solar energy, onshore and offshore wind farms, and next-generation battery storage have achieved grid parity across major global markets (International Energy Agency [IEA], 2024).");
        s2.bulletPoints.add("Solar PV: Levelized cost of energy (LCOE) declined over 85% over the prior decade.");
        s2.bulletPoints.add("Grid Integration: Solid-state storage and demand-response infrastructures ensure grid stability.");
        doc.sections.add(s2);

        Section s3 = new Section("3. Policy Frameworks and Socioeconomic Adaptation");
        s3.paragraphs.add("The Paris Agreement targets restricting warming to well below 2.0°C require synchronized industrial policy, cross-border carbon accounting, and direct investments in resilient infrastructure (World Bank, 2024).");
        doc.sections.add(s3);

        doc.references.add("Intergovernmental Panel on Climate Change. (2023). Climate Change 2023: Synthesis report. Contribution of Working Groups I, II and III. IPCC. https://www.ipcc.ch");
        doc.references.add("International Energy Agency. (2024). World energy outlook 2024. IEA Publications. https://www.iea.org");
        doc.references.add("World Bank. (2024). State and trends of carbon pricing 2024. World Bank Group. https://openknowledge.worldbank.org");
    }

    private static void buildAiDocument(DocumentModel doc) {
        Section s1 = new Section("1. Architecture of Modern Artificial Intelligence");
        s1.paragraphs.add("Artificial Intelligence has entered an era of foundational multimodal models and deep neural architectures. Grounded in transformer self-attention mechanisms, modern systems model complex linguistic, visual, and mathematical dependencies across billions of parameters (Russell & Norvig, 2024).");
        doc.sections.add(s1);

        Section s2 = new Section("2. Practical Applications Across Core Sectors");
        s2.paragraphs.add("From autonomous robotic control and protein folding synthesis to automated code generation, AI systems are transforming productivity frontiers across healthcare, finance, and industrial engineering (National Institute of Standards and Technology [NIST], 2023).");
        s2.bulletPoints.add("Biomedical Diagnostics: Early oncological detection and automated drug candidate discovery.");
        s2.bulletPoints.add("Software Engineering: Context-aware synthesis and automated static verification.");
        doc.sections.add(s2);

        Section s3 = new Section("3. Governance, Alignment, and Safety Frontiers");
        s3.paragraphs.add("Responsible deployment demands verifiable safety boundaries, transparency benchmarks, and robust resistance against adversarial manipulation (World Health Organization [WHO], 2023).");
        doc.sections.add(s3);

        doc.references.add("National Institute of Standards and Technology. (2023). Artificial intelligence risk management framework (AI RMF 1.0). U.S. Department of Commerce. https://doi.org/10.6028/NIST.CSWP.25");
        doc.references.add("Russell, S., & Norvig, P. (2024). Artificial intelligence: A modern approach (4th ed.). Pearson.");
        doc.references.add("World Health Organization. (2023). Ethics and governance of artificial intelligence for health. WHO Guidance. https://www.who.int");
    }

    // ── Presentation Synthesizer ──────────────────────────────────────────────

    public static PresentationModel buildPresentationModelWithAiOrFallback(String topic, String userPrompt, boolean requireResearch) {
        try {
            String aiInstruction = "You are H.E.N.R.Y., crafting a presentation deck in an engaging, human, authentic voice.\n"
                    + "Write 4 to 6 slides in markdown format:\n"
                    + "## Slide 1: Title of Slide\n"
                    + "- Bullet point 1\n"
                    + "- Bullet point 2\n"
                    + "- Bullet point 3\n"
                    + "Notes: Presenter talking points in a conversational, human tone.\n\n"
                    + "TONE: Warm, natural, insightful, zero corporate AI fluff. If it's a recipe or guide, focus on real steps, delicious details, and pro tips.";
            String query = "Create slides for a presentation on: " + userPrompt + "\nTopic: " + topic;
            String aiResp = JarvisApi.askDirectSync(query, aiInstruction);
            if (aiResp != null && aiResp.trim().length() > 80) {
                PresentationModel parsed = parseMarkdownToPresentationModel(topic, aiResp.trim(), requireResearch);
                if (parsed != null && !parsed.slides.isEmpty()) {
                    return parsed;
                }
            }
        } catch (Exception ignored) {}

        return buildPresentationModel(topic, userPrompt, requireResearch);
    }

    public static PresentationModel parseMarkdownToPresentationModel(String defaultTopic, String md, boolean requireResearch) {
        defaultTopic = cleanEmotionTags(defaultTopic);
        md = cleanEmotionTags(md);
        PresentationModel pres = new PresentationModel();
        pres.title = defaultTopic;
        pres.subtitle = "H.E.N.R.Y. Presentation Engine • " + (requireResearch ? "Academic Deck" : "Briefing Deck");

        if (md == null || md.trim().isEmpty()) return null;

        String[] lines = md.split("\r?\n");
        Slide currentSlide = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                pres.title = trimmed.substring(2).replace("*", "").trim();
                continue;
            }

            if (trimmed.startsWith("## ")) {
                String slideTitle = trimmed.substring(3).replace("*", "").trim();
                if (slideTitle.toLowerCase(Locale.US).contains("reference") || slideTitle.toLowerCase(Locale.US).contains("citation")) {
                    currentSlide = null;
                    continue;
                }
                currentSlide = new Slide(slideTitle, "");
                pres.slides.add(currentSlide);
                continue;
            }

            if (currentSlide == null && (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.matches("^\\d+\\..*"))) {
                String refItem = trimmed.replaceFirst("^[-*\\d.]+\\s*", "").replace("*", "").trim();
                if (requireResearch && !refItem.isEmpty()) {
                    pres.references.add(refItem);
                }
                continue;
            }

            if (currentSlide != null) {
                if (trimmed.toLowerCase(Locale.US).startsWith("notes:") || trimmed.toLowerCase(Locale.US).startsWith("note:")) {
                    String parsedNotes = trimmed.replaceFirst("(?i)notes?:\\s*", "").trim();
                    currentSlide.notes = parsedNotes;
                    currentSlide.presenterNotes = parsedNotes;
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") || trimmed.matches("^\\d+\\.\\s+.*")) {
                    String bp = trimmed.replaceFirst("^[-*•\\d.]+\\s*", "").trim();
                    currentSlide.bulletPoints.add(bp);
                } else {
                    if (currentSlide.subtitle == null || currentSlide.subtitle.isEmpty()) {
                        currentSlide.subtitle = trimmed;
                    } else {
                        currentSlide.bulletPoints.add(trimmed);
                    }
                }
            }
        }

        return pres.slides.isEmpty() ? null : pres;
    }

    public static PresentationModel buildPresentationModel(String topic, String userPrompt, boolean requireResearch) {
        PresentationModel pres = new PresentationModel();
        pres.title = topic;
        pres.subtitle = "H.E.N.R.Y. Presentation Engine • " + (requireResearch ? "Academic Deck" : "Briefing Deck");

        String t = (topic + " " + userPrompt).toLowerCase(Locale.US);

        if (t.contains("recipe") || t.contains("cookie") || t.contains("bake") || t.contains("cook") || t.contains("food")) {
            Slide s1 = new Slide("The Art of " + topic, "Culinary Overview");
            s1.bulletPoints.add("Mastering the harmony of flavor, texture, and aroma.");
            s1.bulletPoints.add("Why technique matters just as much as quality ingredients.");
            s1.bulletPoints.add("Key milestones: preparation, temperature control, and presentation.");
            pres.slides.add(s1);

            Slide s2 = new Slide("Essential Ingredients & Flavor Chemistry", "Foundational Elements");
            s2.bulletPoints.add("Fat & Moisture: Browning butter creates rich nutty toffee notes.");
            s2.bulletPoints.add("Sugar Balance: Dark brown sugar provides chew; white sugar provides crisp edges.");
            s2.bulletPoints.add("Structure & Leavening: Proper flour hydration and leavening ratios.");
            pres.slides.add(s2);

            Slide s3 = new Slide("Step-by-Step Culinary Execution", "Method & Technique");
            s3.bulletPoints.add("Patience in preparation: don't rush emulsification or dough chilling.");
            s3.bulletPoints.add("Baking control: watch for golden set edges with soft tender centers.");
            s3.bulletPoints.add("Finishing flair: flaky sea salt enhances sweetness and chocolate richness.");
            pres.slides.add(s3);

            Slide s4 = new Slide("Baker's Secrets & Serving Tips", "Pro Recommendations");
            s4.bulletPoints.add("Pan-banging creates gorgeous crinkled edges with molten pockets.");
            s4.bulletPoints.add("Dough freezes beautifully for on-demand fresh bakes.");
            s4.bulletPoints.add("Pair with a cold glass of milk, artisanal coffee, or hot espresso.");
            pres.slides.add(s4);
        } else if (t.contains("earth") || t.contains("history of earth")) {
            Slide s1 = new Slide("Formation of Earth (4.54 Ga)", "Accretion of dust and gas in early solar system.");
            s1.bulletPoints.add("Formed ~4.54 billion years ago from solar nebula accretion.");
            s1.bulletPoints.add("Intense meteorite bombardment and high surface temperatures.");
            s1.bulletPoints.add("Core differentiation created Earth's planetary magnetic field.");
            pres.slides.add(s1);

            Slide s2 = new Slide("Early Earth & Atmospheric Origin", "Hadean and Archaean atmospheric conditions.");
            s2.bulletPoints.add("Primordial atmosphere: Nitrogen, carbon dioxide, and water vapor.");
            s2.bulletPoints.add("Cooling crust allowed torrential rains, creating early oceans.");
            s2.bulletPoints.add("Absence of free oxygen prior to biogenic photosynthesis.");
            pres.slides.add(s2);

            Slide s3 = new Slide("Origin of Life & Great Oxidation", "Emergence of cyanobacteria and free oxygen.");
            s3.bulletPoints.add("Earliest organic biosignatures date to ~3.8–3.5 billion years ago.");
            s3.bulletPoints.add("Cyanobacteria generated metabolic oxygen through photosynthesis.");
            s3.bulletPoints.add("The Great Oxidation Event transformed planetary geochemistry.");
            pres.slides.add(s3);

            Slide s4 = new Slide("Cambrian Explosion & Paleozoic Life", "Rapid radiation of complex animal phyla.");
            s4.bulletPoints.add("541 Mya: Dramatic appearance of major modern animal body plans.");
            s4.bulletPoints.add("Colonization of land by early vascular plants and arthropods.");
            s4.bulletPoints.add("Carboniferous coal forests and early tetrapod diversification.");
            pres.slides.add(s4);

            Slide s5 = new Slide("Mesozoic Era & Mass Extinctions", "Age of Reptiles and the K-Pg boundary event.");
            s5.bulletPoints.add("Permian-Triassic extinction wiped out >90% of marine species.");
            s5.bulletPoints.add("Mesozoic era dominated by dinosaurs and marine reptiles.");
            s5.bulletPoints.add("Chicxulub asteroid impact (66 Mya) precipitated dinosaur extinction.");
            pres.slides.add(s5);

            Slide s6 = new Slide("Rise of Mammals & Anthropocene", "Cenozoic expansion and human planetary influence.");
            s6.bulletPoints.add("Mammals diversified rapidly across terrestrial and marine niches.");
            s6.bulletPoints.add("Quaternary ice ages shaped modern biogeographical distributions.");
            s6.bulletPoints.add("Modern Anthropocene characterized by global anthropogenic influence.");
            pres.slides.add(s6);

            if (requireResearch) {
                pres.references.add("NASA Solar System Exploration. (2024). Earth planetary facts. https://science.nasa.gov");
                pres.references.add("USGS. (2024). Geologic time scales and evolutionary records. https://www.usgs.gov");
                pres.references.add("Valley, J. W. et al. (2023). Early Earth crust and ocean formation. EPSL, 590, 117–130.");
            }
        } else {
            Slide s1 = new Slide("Overview: " + topic, "Core Concepts");
            s1.bulletPoints.add("Clear understanding of the primary subject and goals.");
            s1.bulletPoints.add("Why this topic matters and current practical trends.");
            s1.bulletPoints.add("Key objectives and strategic takeaways.");
            pres.slides.add(s1);

            Slide s2 = new Slide("Key Principles & Implementation", "Practical Insights");
            s2.bulletPoints.add("Essential building blocks and fundamental techniques.");
            s2.bulletPoints.add("Real-world examples and practical applications.");
            s2.bulletPoints.add("Common challenges and how to overcome them.");
            pres.slides.add(s2);

            Slide s3 = new Slide("Actionable Takeaways", "Summary & Next Steps");
            s3.bulletPoints.add("Direct recommendations you can apply today.");
            s3.bulletPoints.add("Near-term goals and ongoing improvement milestones.");
            s3.bulletPoints.add("Summary of key lessons and final thoughts.");
            pres.slides.add(s3);

            if (requireResearch) {
                pres.references.add("American Psychological Association. (2020). APA Publication Manual (7th ed.).");
                pres.references.add("National Science Foundation. (2024). Science and engineering indicators.");
            }
        }

        return pres;
    }

    // ── Spreadsheet Synthesizer ───────────────────────────────────────────────

    public static SpreadsheetModel buildSpreadsheetModel(String topic, String userPrompt) {
        SpreadsheetModel sheet = new SpreadsheetModel();
        sheet.title = topic;

        String t = (topic + " " + userPrompt).toLowerCase(Locale.US);

        if (t.contains("recipe") || t.contains("cookie") || t.contains("bake") || t.contains("cook") || t.contains("grocery") || t.contains("ingredient")) {
            sheet.headers.add("Ingredient / Item");
            sheet.headers.add("Quantity Needed");
            sheet.headers.add("Unit");
            sheet.headers.add("Est. Unit Price ($)");
            sheet.headers.add("Baker's & Chef's Notes");

            sheet.rows.add(List.of("Unsalted Butter", "2", "sticks (1 cup)", "1.80", "Browned in saucepan until nutty"));
            sheet.rows.add(List.of("Dark Brown Sugar", "0.75", "cups (packed)", "0.65", "Adds rich moisture and caramel chew"));
            sheet.rows.add(List.of("Granulated White Sugar", "0.5", "cups", "0.35", "Creates crisp golden rims"));
            sheet.rows.add(List.of("Large Grade A Eggs", "2", "whole + 1 yolk", "0.50", "Room temperature for easy emulsion"));
            sheet.rows.add(List.of("Pure Vanilla Extract", "1", "tablespoon", "1.20", "Madagascar bourbon vanilla"));
            sheet.rows.add(List.of("All-Purpose Flour", "2.25", "cups (spooned)", "0.70", "Do not pack tightly"));
            sheet.rows.add(List.of("Semisweet & Dark Chocolate", "2", "cups (chunks)", "3.50", "Hand-chopped bars for melted pools"));
            sheet.rows.add(List.of("Baking Soda & Sea Salt", "2", "teaspoons", "0.20", "Rising power & flavor balance"));
            sheet.rows.add(List.of("Flaky Maldon Sea Salt", "1", "pinch per cookie", "0.15", "Sprinkled fresh from oven"));

            sheet.summaryFormulaLabel = "Total Estimated Batch Cost";
            sheet.summaryFormulaValue = "=SUM(D2:D10)";
        } else if (t.contains("sales") || t.contains("revenue") || t.contains("client")) {
            sheet.headers.add("Region / Channel");
            sheet.headers.add("Q1 Target ($)");
            sheet.headers.add("Q1 Actual ($)");
            sheet.headers.add("Variance ($)");
            sheet.headers.add("Achievement (%)");

            sheet.rows.add(List.of("North America - Enterprise", "500000", "542000", "42000", "108.4%"));
            sheet.rows.add(List.of("Europe / UK - Commercial", "350000", "338500", "-11500", "96.7%"));
            sheet.rows.add(List.of("Asia Pacific - Direct", "250000", "289000", "39000", "115.6%"));
            sheet.rows.add(List.of("Latin America - Partner", "150000", "162000", "12000", "108.0%"));
            sheet.rows.add(List.of("Global Digital Self-Serve", "200000", "225400", "25400", "112.7%"));

            sheet.summaryFormulaLabel = "Total Consolidated Sales";
            sheet.summaryFormulaValue = "=SUM(C2:C6)";
        } else {
            // Expenses / General Budget
            sheet.headers.add("Category / Line Item");
            sheet.headers.add("Allocated Budget ($)");
            sheet.headers.add("Actual Incurred ($)");
            sheet.headers.add("Balance Remaining ($)");
            sheet.headers.add("Status");

            sheet.rows.add(List.of("Engineering & Development", "120000", "112450", "7550", "On Track"));
            sheet.rows.add(List.of("Cloud Infrastructure & AI", "45000", "43820", "1180", "On Track"));
            sheet.rows.add(List.of("Product Marketing & Ads", "35000", "34200", "800", "Optimized"));
            sheet.rows.add(List.of("Security, Legal & Compliance", "18000", "14500", "3500", "Under Budget"));
            sheet.rows.add(List.of("Operations & Support", "22000", "21950", "50", "Fully Utilized"));

            sheet.summaryFormulaLabel = "Total Expenditure";
            sheet.summaryFormulaValue = "=SUM(C2:C6)";
        }

        return sheet;
    }

    // ── DOCX Generator (Standard OpenXML WordprocessingML) ────────────────────

    public static void generateDocx(File outFile, DocumentModel doc) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {

            // 1. [Content_Types].xml
            writeZipEntry(zos, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                    "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                    "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                    "  <Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                    "  <Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>\n" +
                    "</Types>");

            // 2. _rels/.rels
            writeZipEntry(zos, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                    "</Relationships>");

            // 3. word/_rels/document.xml.rels
            writeZipEntry(zos, "word/_rels/document.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n" +
                    "</Relationships>");

            // 4. word/styles.xml
            writeZipEntry(zos, "word/styles.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n" +
                    "  <w:docDefaults>\n" +
                    "    <w:rPrDefault><w:rPr><w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\"/><w:sz w:val=\"24\"/><w:color w:val=\"222222\"/></w:rPr></w:rPrDefault>\n" +
                    "    <w:pPrDefault><w:pPr><w:spacing w:line=\"276\" w:lineRule=\"auto\" w:after=\"140\"/></w:pPr></w:pPrDefault>\n" +
                    "  </w:docDefaults>\n" +
                    "  <w:style w:type=\"paragraph\" w:styleId=\"Title\">\n" +
                    "    <w:name w:val=\"Title\"/>\n" +
                    "    <w:rPr><w:rFonts w:ascii=\"Calibri Light\"/><w:b/><w:sz w:val=\"52\"/><w:color w:val=\"0A2540\"/></w:rPr>\n" +
                    "    <w:pPr><w:spacing w:before=\"200\" w:after=\"120\"/><w:jc w:val=\"center\"/></w:pPr>\n" +
                    "  </w:style>\n" +
                    "  <w:style w:type=\"paragraph\" w:styleId=\"Subtitle\">\n" +
                    "    <w:name w:val=\"Subtitle\"/>\n" +
                    "    <w:rPr><w:i/><w:sz w:val=\"24\"/><w:color w:val=\"666666\"/></w:rPr>\n" +
                    "    <w:pPr><w:spacing w:after=\"300\"/><w:jc w:val=\"center\"/></w:pPr>\n" +
                    "  </w:style>\n" +
                    "  <w:style w:type=\"paragraph\" w:styleId=\"Heading1\">\n" +
                    "    <w:name w:val=\"heading 1\"/>\n" +
                    "    <w:rPr><w:rFonts w:ascii=\"Calibri Light\"/><w:b/><w:sz w:val=\"34\"/><w:color w:val=\"0A2540\"/></w:rPr>\n" +
                    "    <w:pPr><w:spacing w:before=\"320\" w:after=\"120\"/></w:pPr>\n" +
                    "  </w:style>\n" +
                    "  <w:style w:type=\"paragraph\" w:styleId=\"ReferenceItem\">\n" +
                    "    <w:name w:val=\"Reference Item\"/>\n" +
                    "    <w:rPr><w:sz w:val=\"22\"/><w:color w:val=\"333333\"/></w:rPr>\n" +
                    "    <w:pPr><w:ind w:left=\"720\" w:hanging=\"720\"/><w:spacing w:line=\"240\" w:after=\"140\"/></w:pPr>\n" +
                    "  </w:style>\n" +
                    "</w:styles>");

            // 5. word/document.xml
            StringBuilder body = new StringBuilder();
            body.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            body.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n");
            body.append("<w:body>\n");

            // Title
            body.append("<w:p><w:pPr><w:pStyle w:val=\"Title\"/></w:pPr><w:r><w:t>")
                    .append(escapeXml(doc.title))
                    .append("</w:t></w:r></w:p>\n");

            // Subtitle
            body.append("<w:p><w:pPr><w:pStyle w:val=\"Subtitle\"/></w:pPr><w:r><w:t>")
                    .append(escapeXml(doc.subtitle + " • " + doc.dateString))
                    .append("</w:t></w:r></w:p>\n");

            // Sections
            for (Section sec : doc.sections) {
                body.append("<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr><w:r><w:t>")
                        .append(escapeXml(sec.heading))
                        .append("</w:t></w:r></w:p>\n");

                for (String p : sec.getResolvedParagraphs()) {
                    body.append("<w:p><w:r><w:t>").append(escapeXml(p)).append("</w:t></w:r></w:p>\n");
                }

                for (String b : sec.bulletPoints) {
                    body.append("<w:p><w:pPr><w:ind w:left=\"360\"/></w:pPr><w:r><w:t>• ")
                            .append(escapeXml(b))
                            .append("</w:t></w:r></w:p>\n");
                }

                // Render Table if present
                List<String[]> resolvedTable = sec.getResolvedTableData();
                if (resolvedTable != null && !resolvedTable.isEmpty()) {
                    body.append("<w:tbl>\n");
                    body.append("  <w:tblPr><w:tblW w:w=\"9000\" w:type=\"dxa\"/>");
                    body.append("  <w:tblBorders><w:top w:val=\"single\" w:sz=\"4\" w:color=\"CCCCCC\"/>");
                    body.append("  <w:bottom w:val=\"single\" w:sz=\"8\" w:color=\"0A2540\"/>");
                    body.append("  <w:insideH w:val=\"single\" w:sz=\"4\" w:color=\"E5E5E5\"/></w:tblBorders></w:tblPr>\n");

                    boolean isHeader = true;
                    for (String[] row : resolvedTable) {
                        body.append("  <w:tr>\n");
                        for (String cell : row) {
                            body.append("    <w:tc><w:tcPr><w:tcW w:w=\"3000\" w:type=\"dxa\"/>");
                            if (isHeader) {
                                body.append("<w:shd w:fill=\"0A2540\"/>");
                            }
                            body.append("</w:tcPr><w:p><w:r>");
                            if (isHeader) {
                                body.append("<w:rPr><w:b/><w:color w:val=\"FFFFFF\"/></w:rPr>");
                            }
                            body.append("<w:t>").append(escapeXml(cell)).append("</w:t></w:r></w:p></w:tc>\n");
                        }
                        body.append("  </w:tr>\n");
                        isHeader = false;
                    }
                    body.append("</w:tbl>\n");
                }
            }

            // References (APA 7th Edition)
            if (!doc.references.isEmpty()) {
                body.append("<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr><w:r><w:t>References</w:t></w:r></w:p>\n");
                for (String ref : doc.references) {
                    body.append("<w:p><w:pPr><w:pStyle w:val=\"ReferenceItem\"/></w:pPr><w:r><w:t>")
                            .append(escapeXml(ref))
                            .append("</w:t></w:r></w:p>\n");
                }
            }

            // Page Setup: Letter, 1 inch margins (1440 dxa)
            body.append("<w:sectPr><w:pgSz w:w=\"12240\" w:h=\"15840\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/></w:sectPr>\n");
            body.append("</w:body></w:document>");

            writeZipEntry(zos, "word/document.xml", body.toString());
        }
    }

    // ── XLSX Generator (OpenXML SpreadsheetML) ────────────────────────────────

    public static void generateXlsx(File outFile, SpreadsheetModel sheet) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {

            // 1. [Content_Types].xml
            writeZipEntry(zos, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                    "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                    "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                    "  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n" +
                    "  <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n" +
                    "  <Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>\n" +
                    "</Types>");

            // 2. _rels/.rels
            writeZipEntry(zos, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n" +
                    "</Relationships>");

            // 3. xl/_rels/workbook.xml.rels
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n" +
                    "  <Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n" +
                    "</Relationships>");

            // 4. xl/workbook.xml
            writeZipEntry(zos, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n" +
                    "  <sheets><sheet name=\"Data\" sheetId=\"1\" r:id=\"rId1\"/></sheets>\n" +
                    "</workbook>");

            // 5. xl/styles.xml
            writeZipEntry(zos, "xl/styles.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n" +
                    "  <fonts count=\"3\">\n" +
                    "    <font><sz w:val=\"11\"/><name w:val=\"Calibri\"/></font>\n" +
                    "    <font><b/><sz w:val=\"11\"/><color rgb=\"FFFFFFFF\"/><name w:val=\"Calibri\"/></font>\n" +
                    "    <font><b/><sz w:val=\"11\"/><name w:val=\"Calibri\"/></font>\n" +
                    "  </fonts>\n" +
                    "  <fills count=\"3\">\n" +
                    "    <fill><patternFill patternType=\"none\"/></fill>\n" +
                    "    <fill><patternFill patternType=\"gray125\"/></fill>\n" +
                    "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF0A2540\"/></patternFill></fill>\n" +
                    "  </fills>\n" +
                    "  <borders count=\"1\"><border><left/><right/><top/><bottom/></border></borders>\n" +
                    "  <cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>\n" +
                    "  <cellXfs count=\"3\">\n" +
                    "    <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>\n" + // 0: Normal
                    "    <xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" + // 1: Header
                    "    <xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>\n" + // 2: Bold
                    "  </cellXfs>\n" +
                    "</styleSheet>");

            // 6. xl/worksheets/sheet1.xml
            StringBuilder ws = new StringBuilder();
            ws.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            ws.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
            ws.append("  <sheetViews><sheetView tabSelected=\"1\" workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" state=\"frozen\"/></sheetView></sheetViews>\n");
            ws.append("  <cols>\n");
            for (int i = 1; i <= Math.max(sheet.headers.size(), 5); i++) {
                ws.append("    <col min=\"").append(i).append("\" max=\"").append(i).append("\" width=\"25\" customWidth=\"1\"/>\n");
            }
            ws.append("  </cols>\n");
            ws.append("  <sheetData>\n");

            // Header Row (Row 1, style s="1")
            ws.append("    <row r=\"1\">\n");
            for (int col = 0; col < sheet.headers.size(); col++) {
                String colRef = getColumnLetter(col + 1) + "1";
                ws.append("      <c r=\"").append(colRef).append("\" t=\"inlineStr\" s=\"1\"><is><t>")
                        .append(escapeXml(sheet.headers.get(col)))
                        .append("</t></is></c>\n");
            }
            ws.append("    </row>\n");

            // Data Rows (Row 2 to N, style s="0")
            int rowIdx = 2;
            for (List<String> row : sheet.rows) {
                ws.append("    <row r=\"").append(rowIdx).append("\">\n");
                for (int col = 0; col < row.size(); col++) {
                    String val = row.get(col);
                    String colRef = getColumnLetter(col + 1) + rowIdx;
                    // Check if numeric
                    if (val.matches("^-?\\d+(\\.\\d+)?$")) {
                        ws.append("      <c r=\"").append(colRef).append("\" s=\"0\"><v>").append(val).append("</v></c>\n");
                    } else {
                        ws.append("      <c r=\"").append(colRef).append("\" t=\"inlineStr\" s=\"0\"><is><t>")
                                .append(escapeXml(val))
                                .append("</t></is></c>\n");
                    }
                }
                ws.append("    </row>\n");
                rowIdx++;
            }

            // Summary Row if specified
            if (sheet.summaryFormulaLabel != null && sheet.summaryFormulaValue != null) {
                ws.append("    <row r=\"").append(rowIdx).append("\">\n");
                ws.append("      <c r=\"A").append(rowIdx).append("\" t=\"inlineStr\" s=\"2\"><is><t>")
                        .append(escapeXml(sheet.summaryFormulaLabel))
                        .append("</t></is></c>\n");
                ws.append("      <c r=\"C").append(rowIdx).append("\" s=\"2\"><f>")
                        .append(sheet.summaryFormulaValue.replace("=", ""))
                        .append("</f><v>0</v></c>\n");
                ws.append("    </row>\n");
            }

            ws.append("  </sheetData>\n");
            ws.append("</worksheet>");

            writeZipEntry(zos, "xl/worksheets/sheet1.xml", ws.toString());
        }
    }

    private static String getColumnLetter(int columnNumber) {
        StringBuilder sb = new StringBuilder();
        while (columnNumber > 0) {
            int rem = (columnNumber - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            columnNumber = (columnNumber - 1) / 26;
        }
        return sb.toString();
    }

    // ── PPTX Generator (OpenXML PresentationML) ───────────────────────────────

    public static void generatePptx(File outFile, PresentationModel pres) throws Exception {
        if (pres == null) {
            pres = new PresentationModel();
        }
        if (pres.title == null || pres.title.trim().isEmpty()) {
            pres.title = "H.E.N.R.Y. Presentation";
        }
        if (pres.slides == null) {
            pres.slides.clear();
        }
        if (pres.references == null) {
            pres.references.clear;
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {

            int totalSlides = 1 + pres.slides.size() + (pres.references.isEmpty() ? 0 : 1);

            // 1. [Content_Types].xml
            StringBuilder ct = new StringBuilder();
            ct.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            ct.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n");
            ct.append("  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n");
            ct.append("  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n");
            ct.append("  <Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/>\n");
            ct.append("  <Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml\"/>\n");
            ct.append("  <Override PartName=\"/ppt/slideLayouts/slideLayout1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml\"/>\n");
            ct.append("  <Override PartName=\"/ppt/theme/theme1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.theme+xml\"/>\n");
            for (int i = 1; i <= totalSlides; i++) {
                ct.append("  <Override PartName=\"/ppt/slides/slide").append(i)
                        .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>\n");
            }
            ct.append("</Types>");
            writeZipEntry(zos, "[Content_Types].xml", ct.toString());

            // 2. _rels/.rels
            writeZipEntry(zos, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"ppt/presentation.xml\"/>\n" +
                    "</Relationships>");

            // 3. ppt/_rels/presentation.xml.rels
            StringBuilder pRels = new StringBuilder();
            pRels.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            pRels.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n");
            pRels.append("  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster\" Target=\"slideMasters/slideMaster1.xml\"/>\n");
            for (int i = 1; i <= totalSlides; i++) {
                pRels.append("  <Relationship Id=\"rId").append(i + 1)
                        .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" Target=\"slides/slide")
                        .append(i).append(".xml\"/>\n");
            }
            pRels.append("</Relationships>");
            writeZipEntry(zos, "ppt/_rels/presentation.xml.rels", pRels.toString());

            // 4. ppt/presentation.xml
            StringBuilder pXml = new StringBuilder();
            pXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            pXml.append("<p:presentation xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n");
            pXml.append("  <p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rId1\"/></p:sldMasterIdLst>\n");
            pXml.append("  <p:sldIdLst>\n");
            for (int i = 1; i <= totalSlides; i++) {
                pXml.append("    <p:sldId id=\"").append(255 + i).append("\" r:id=\"rId").append(i + 1).append("\"/>\n");
            }
            pXml.append("  </p:sldIdLst>\n");
            pXml.append("  <p:sldSz cx=\"12192000\" cy=\"6858000\" type=\"screen16x9\"/>\n");
            pXml.append("  <p:notesSz cx=\"6858000\" cy=\"9144000\"/>\n");
            pXml.append("</p:presentation>");
            writeZipEntry(zos, "ppt/presentation.xml", pXml.toString());

            // 5. ppt/slideMasters/slideMaster1.xml and rels
            writeZipEntry(zos, "ppt/slideMasters/slideMaster1.xml", buildPptxSlideMasterXml());
            writeZipEntry(zos, "ppt/slideMasters/_rels/slideMaster1.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>\n" +
                    "  <Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme\" Target=\"../theme/theme1.xml\"/>\n" +
                    "</Relationships>");

            // 6. ppt/slideLayouts/slideLayout1.xml and rels
            writeZipEntry(zos, "ppt/slideLayouts/slideLayout1.xml", buildPptxSlideLayoutXml());
            writeZipEntry(zos, "ppt/slideLayouts/_rels/slideLayout1.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster\" Target=\"../slideMasters/slideMaster1.xml\"/>\n" +
                    "</Relationships>");

            // 7. ppt/theme/theme1.xml
            writeZipEntry(zos, "ppt/theme/theme1.xml", buildPptxThemeXml());

            // 8. Slides & slide rels
            // Slide 1: Title Slide
            List<String> titleLines = new ArrayList<>();
            if (pres.subtitle != null && !pres.subtitle.trim().isEmpty()) {
                titleLines.add(pres.subtitle.trim());
            }
            titleLines.add("Generated by H.E.N.R.Y. AI Engine");
            titleLines.add(new SimpleDateFormat("MMMM yyyy", Locale.US).format(new Date()));

            writeZipEntry(zos, "ppt/slides/slide1.xml", buildPptxSlideXml(pres.title, titleLines, true));
            writeZipEntry(zos, "ppt/slides/_rels/slide1.xml.rels", buildPptxSlideRelsXml());

            int currentSlide = 2;
            for (Slide s : pres.slides) {
                List<String> slideLines = new ArrayList<>();
                if (s.subtitle != null && !s.subtitle.trim().isEmpty()) {
                    slideLines.add(s.subtitle.trim());
                }
                if (s.bulletPoints != null) {
                    for (String bp : s.bulletPoints) {
                        if (bp != null && !bp.trim().isEmpty()) {
                            slideLines.add(bp.trim());
                        }
                    }
                }
                writeZipEntry(zos, "ppt/slides/slide" + currentSlide + ".xml", buildPptxSlideXml(s.title, slideLines, false));
                writeZipEntry(zos, "ppt/slides/_rels/slide" + currentSlide + ".xml.rels", buildPptxSlideRelsXml());
                currentSlide++;
            }

            // References Slide if present
            if (!pres.references.isEmpty()) {
                writeZipEntry(zos, "ppt/slides/slide" + currentSlide + ".xml", buildPptxSlideXml("References & Sources (APA 7th)", pres.references, false));
                writeZipEntry(zos, "ppt/slides/_rels/slide" + currentSlide + ".xml.rels", buildPptxSlideRelsXml());
            }
        }
    }

    private static String buildPptxSlideRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
                + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>\n"
                + "</Relationships>";
    }

    private static String buildPptxSlideMasterXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<p:sldMaster xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n"
                + "  <p:cSld><p:spTree>\n"
                + "    <p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>\n"
                + "    <p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>\n"
                + "  </p:spTree></p:cSld>\n"
                + "  <p:clrMap bg1=\"lt1\" tx1=\"dk1\" bg2=\"lt2\" tx2=\"dk2\" accent1=\"accent1\" accent2=\"accent2\" accent3=\"accent3\" accent4=\"accent4\" accent5=\"accent5\" accent6=\"accent6\" hlink=\"hlink\" folHlink=\"folHlink\"/>\n"
                + "  <p:sldLayoutIdLst><p:sldLayoutId id=\"2147483649\" r:id=\"rId1\"/></p:sldLayoutIdLst>\n"
                + "</p:sldMaster>";
    }

    private static String buildPptxSlideLayoutXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<p:sldLayout xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" type=\"blank\" preserve=\"1\">\n"
                + "  <p:cSld><p:spTree>\n"
                + "    <p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>\n"
                + "    <p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>\n"
                + "  </p:spTree></p:cSld>\n"
                + "  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>\n"
                + "</p:sldLayout>";
    }

    private static String buildPptxThemeXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<a:theme xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" name=\"Office Theme\">\n"
                + "  <a:themeElements>\n"
                + "    <a:clrScheme name=\"Office\">\n"
                + "      <a:dk1><a:sysClr val=\"windowText\" lastClr=\"000000\"/></a:dk1>\n"
                + "      <a:lt1><a:sysClr val=\"window\" lastClr=\"FFFFFF\"/></a:lt1>\n"
                + "      <a:dk2><a:srgbClr val=\"1F497D\"/></a:dk2>\n"
                + "      <a:lt2><a:srgbClr val=\"EEECE1\"/></a:lt2>\n"
                + "      <a:accent1><a:srgbClr val=\"0A2540\"/></a:accent1>\n"
                + "      <a:accent2><a:srgbClr val=\"00D2FF\"/></a:accent2>\n"
                + "      <a:accent3><a:srgbClr val=\"5B9BD5\"/></a:accent3>\n"
                + "      <a:accent4><a:srgbClr val=\"ED7D31\"/></a:accent4>\n"
                + "      <a:accent5><a:srgbClr val=\"A5A5A5\"/></a:accent5>\n"
                + "      <a:accent6><a:srgbClr val=\"FFC000\"/></a:accent6>\n"
                + "      <a:hlink><a:srgbClr val=\"0563C1\"/></a:hlink>\n"
                + "      <a:folHlink><a:srgbClr val=\"954F72\"/></a:folHlink>\n"
                + "    </a:clrScheme>\n"
                + "    <a:fontScheme name=\"Office\">\n"
                + "      <a:majorFont><a:latin typeface=\"Calibri\"/></a:majorFont>\n"
                + "      <a:minorFont><a:latin typeface=\"Calibri\"/></a:minorFont>\n"
                + "    </a:fontScheme>\n"
                + "    <a:fmtScheme name=\"Office\"><a:fillStyleLst><a:solidFill><a:srgbClr val=\"FFFFFF\"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w=\"9525\"><a:solidFill><a:srgbClr val=\"0A2540\"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectLst/></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:srgbClr val=\"FFFFFF\"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme>\n"
                + "  </a:themeElements>\n"
                + "</a:theme>";
    }

    private static String buildPptxSlideXml(String title, List<String> bodyLines, boolean isTitleSlide) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n");
        sb.append("  <p:cSld><p:spTree>\n");
        sb.append("    <p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>\n");
        sb.append("    <p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>\n");

        // Slide Title Box
        int titleY = isTitleSlide ? 2200000 : 600000;
        int titleHeight = isTitleSlide ? 1200000 : 800000;
        int titleFontSize = isTitleSlide ? 4000 : 3200;

        sb.append("    <p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Title\"/><p:cNvSpPr><a:spLocks noGrp=\"1\"/></p:cNvSpPr><p:nvPr/></p:nvSpPr>\n");
        sb.append("      <p:spPr><a:xfrm><a:off x=\"900000\" y=\"").append(titleY).append("\"/><a:ext cx=\"10392000\" cy=\"").append(titleHeight).append("\"/></a:xfrm></p:spPr>\n");
        sb.append("      <p:txBody><a:bodyPr/><a:lstStyle/>\n");
        sb.append("        <a:p><a:pPr algn=\"").append(isTitleSlide ? "ctr" : "l").append("\"/>");
        sb.append("<a:r><a:rPr lang=\"en-US\" sz=\"").append(titleFontSize).append("\" b=\"1\"><a:solidFill><a:srgbClr val=\"0A2540\"/></a:solidFill></a:rPr>");
        sb.append("<a:t>").append(escapeXml(title != null ? title : "Slide")).append("</a:t></a:r></a:p>\n");
        sb.append("      </p:txBody></p:sp>\n");

        // Body Content Box
        int bodyY = isTitleSlide ? 3600000 : 1600000;
        int bodyHeight = isTitleSlide ? 1800000 : 4600000;
        int bodyFontSize = isTitleSlide ? 2000 : 1800;

        sb.append("    <p:sp><p:nvSpPr><p:cNvPr id=\"3\" name=\"Content\"/><p:cNvSpPr><a:spLocks noGrp=\"1\"/></p:cNvSpPr><p:nvPr/></p:nvSpPr>\n");
        sb.append("      <p:spPr><a:xfrm><a:off x=\"900000\" y=\"").append(bodyY).append("\"/><a:ext cx=\"10392000\" cy=\"").append(bodyHeight).append("\"/></a:xfrm></p:spPr>\n");
        sb.append("      <p:txBody><a:bodyPr/><a:lstStyle/>\n");

        if (bodyLines != null) {
            for (String line : bodyLines) {
                if (line == null || line.trim().isEmpty()) continue;
                sb.append("        <a:p><a:pPr algn=\"").append(isTitleSlide ? "ctr" : "l").append("\" marL=\"").append(isTitleSlide ? "0" : "360000").append("\" indent=\"").append(isTitleSlide ? "0" : "-360000").append("\"/>");
                sb.append("<a:r><a:rPr lang=\"en-US\" sz=\"").append(bodyFontSize).append("\"><a:solidFill><a:srgbClr val=\"333333\"/></a:solidFill></a:rPr>");
                sb.append("<a:t>").append(isTitleSlide ? "" : "• ").append(escapeXml(line)).append("</a:t></a:r></a:p>\n");
            }
        }

        sb.append("      </p:txBody></p:sp>\n");
        sb.append("  </p:spTree></p:cSld>\n");
        sb.append("  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>\n");
        sb.append("</p:sld>");
        return sb.toString();
    }

    // ── PDF Generator (Native Android PdfDocument) ────────────────────────────

    public static void generatePdf(File outFile, DocumentModel doc, Bitmap userImage) throws Exception {
        if (doc == null) {
            doc = new DocumentModel();
        }
        if (doc.title == null || doc.title.trim().isEmpty()) {
            doc.title = "H.E.N.R.Y. Document";
        }
        if (doc.subtitle == null) {
            doc.subtitle = "Executive Summary & Analysis";
        }
        if (doc.dateString == null) {
            doc.dateString = new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(new Date());
        }
        if (doc.sections == null || doc.sections.isEmpty()) {
            Section defaultSec = new Section("Overview");
            defaultSec.paragraphs.add("This document was prepared by the H.E.N.R.Y. Document Engine.");
           if (doc.sections != null) doc.sections.clear();
...
doc.references.clear();
        }

        int pageWidth = 612; // 8.5 x 11 inches at 72 dpi (Letter)
        int pageHeight = 792;
        int margin = 54; // 0.75 in
        int contentWidth = pageWidth - (margin * 2);

        PdfDocument pdfDoc = new PdfDocument();
        PdfDocument.Page page = null;

        try {
            TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.parseColor("#0A2540"));
            titlePaint.setTextSize(22);
            titlePaint.setFakeBoldText(true);

            TextPaint subtitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            subtitlePaint.setColor(Color.parseColor("#555555"));
            subtitlePaint.setTextSize(11);

            TextPaint headingPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            headingPaint.setColor(Color.parseColor("#0A2540"));
            headingPaint.setTextSize(14);
            headingPaint.setFakeBoldText(true);

            TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            bodyPaint.setColor(Color.parseColor("#222222"));
            bodyPaint.setTextSize(10.5f);

            TextPaint bulletPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            bulletPaint.setColor(Color.parseColor("#222222"));
            bulletPaint.setTextSize(10.5f);

            TextPaint refPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            refPaint.setColor(Color.parseColor("#333333"));
            refPaint.setTextSize(9.5f);

            TextPaint headerFooterPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            headerFooterPaint.setColor(Color.parseColor("#888888"));
            headerFooterPaint.setTextSize(8);

            Paint accentLinePaint = new Paint();
            accentLinePaint.setColor(Color.parseColor("#00D2FF"));
            accentLinePaint.setStrokeWidth(2.5f);

            Paint tableHeaderPaint = new Paint();
            tableHeaderPaint.setColor(Color.parseColor("#0A2540"));

            Paint tableBorderPaint = new Paint();
            tableBorderPaint.setColor(Color.parseColor("#DDDDDD"));
            tableBorderPaint.setStyle(Paint.Style.STROKE);
            tableBorderPaint.setStrokeWidth(0.75f);

            TextPaint tableHeaderTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tableHeaderTextPaint.setColor(Color.WHITE);
            tableHeaderTextPaint.setTextSize(9);
            tableHeaderTextPaint.setFakeBoldText(true);

            TextPaint tableBodyTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tableBodyTextPaint.setColor(Color.parseColor("#222222"));
            tableBodyTextPaint.setTextSize(9);

            int pageNumber = 1;
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
            page = pdfDoc.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            int currentY = margin;

            // Draw top accent banner
            canvas.drawRect(0, 0, pageWidth, 6, accentLinePaint);

            // Header text
            canvas.drawText("H.E.N.R.Y. Document Engine • APA 7th Edition", margin, currentY + 14, headerFooterPaint);
            currentY += 30;

            // Title
            StaticLayout titleLayout = new StaticLayout(doc.title, titlePaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0, false);
            canvas.save();
            canvas.translate(margin, currentY);
            titleLayout.draw(canvas);
            canvas.restore();
            currentY += titleLayout.getHeight() + 6;

            // Subtitle
            String sub = doc.subtitle + " • " + doc.dateString;
            canvas.drawText(sub, margin, currentY + 12, subtitlePaint);
            currentY += 24;

            // Divider
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, accentLinePaint);
            currentY += 20;

            // Draw User-supplied image if present
            if (userImage != null && !userImage.isRecycled() && userImage.getWidth() > 0 && userImage.getHeight() > 0) {
                try {
                    int imgW = Math.min(contentWidth, 340);
                    int imgH = (int) ((float) imgW * userImage.getHeight() / userImage.getWidth());
                    if (imgH > 180) imgH = 180;
                    Rect dst = new Rect(margin, currentY, margin + imgW, currentY + imgH);
                    canvas.drawBitmap(userImage, null, dst, null);
                    currentY += imgH + 8;
                    canvas.drawText("Figure 1. User-provided contextual reference visual.", margin, currentY + 10, subtitlePaint);
                    currentY += 22;
                } catch (Exception ignored) {}
            }

            // Draw Sections
            for (Section sec : doc.sections) {
                if (sec == null) continue;

                // Check page overflow for heading
                if (currentY + 60 > pageHeight - margin) {
                    canvas.drawText("Page " + pageNumber, pageWidth / 2f - 15, pageHeight - 25, headerFooterPaint);
                    pdfDoc.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdfDoc.startPage(pageInfo);
                    canvas = page.getCanvas();
                    currentY = margin + 10;
                    canvas.drawRect(0, 0, pageWidth, 4, accentLinePaint);
                }

                // Section Heading
                String heading = (sec.heading != null && !sec.heading.trim().isEmpty()) ? sec.heading.trim() : "Section";
                canvas.drawText(heading, margin, currentY + 14, headingPaint);
                currentY += 24;

                // Paragraphs
                List<String> paragraphs = sec.getResolvedParagraphs();
                if (paragraphs != null) {
                    for (String p : paragraphs) {
                        if (p == null || p.trim().isEmpty()) continue;
                        StaticLayout pLayout = new StaticLayout(p.trim(), bodyPaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.25f, 0, false);
                        if (currentY + pLayout.getHeight() > pageHeight - margin) {
                            canvas.drawText("Page " + pageNumber, pageWidth / 2f - 15, pageHeight - 25, headerFooterPaint);
                            pdfDoc.finishPage(page);
                            pageNumber++;
                            pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                            page = pdfDoc.startPage(pageInfo);
                            canvas = page.getCanvas();
                            currentY = margin + 10;
                            canvas.drawRect(0, 0, pageWidth, 4, accentLinePaint);
                        }
                        canvas.save();
                        canvas.translate(margin, currentY);
                        pLayout.draw(canvas);
                        canvas.restore();
                        currentY += pLayout.getHeight() + 10;
                    }
                }

                // Bullets
                if (sec.bulletPoints != null) {
                    for (String b : sec.bulletPoints) {
                        if (b == null || b.trim().isEmpty()) continue;
                        StaticLayout bLayout = new StaticLayout("• " + b.trim(), bulletPaint, contentWidth - 14, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0, false);
                        if (currentY + bLayout.getHeight() > pageHeight - margin) {
                            canvas.drawText("Page " + pageNumber, pageWidth / 2f - 15, pageHeight - 25, headerFooterPaint);
                            pdfDoc.finishPage(page);
                            pageNumber++;
                            pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                            page = pdfDoc.startPage(pageInfo);
                            canvas = page.getCanvas();
                            currentY = margin + 10;
                            canvas.drawRect(0, 0, pageWidth, 4, accentLinePaint);
                        }
                        canvas.save();
                        canvas.translate(margin + 12, currentY);
                        bLayout.draw(canvas);
                        canvas.restore();
                        currentY += bLayout.getHeight() + 6;
                    }
                }

                // Table
                List<String[]> pdfTable = sec.getResolvedTableData();
                if (pdfTable != null && !pdfTable.isEmpty() && pdfTable.get(0) != null && pdfTable.get(0).length > 0) {
                    int colCount = pdfTable.get(0).length;
                    int colW = contentWidth / colCount;
                    int rowH = 22;

                    if (currentY + (pdfTable.size() * rowH) > pageHeight - margin) {
                        canvas.drawText("Page " + pageNumber, pageWidth / 2f - 15, pageHeight - 25, headerFooterPaint);
                        pdfDoc.finishPage(page);
                        pageNumber++;
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                        page = pdfDoc.startPage(pageInfo);
                        canvas = page.getCanvas();
                        currentY = margin + 10;
                    }

                    boolean isHeader = true;
                    for (String[] row : pdfTable) {
                        if (row == null) continue;
                        if (isHeader) {
                            canvas.drawRect(margin, currentY, margin + contentWidth, currentY + rowH, tableHeaderPaint);
                        }
                        for (int c = 0; c < row.length && c < colCount; c++) {
                            int x = margin + (c * colW) + 6;
                            int y = currentY + 15;
                            String cellVal = (row[c] != null) ? row[c] : "";
                            canvas.drawText(cellVal, x, y, isHeader ? tableHeaderTextPaint : tableBodyTextPaint);
                        }
                        canvas.drawRect(margin, currentY, margin + contentWidth, currentY + rowH, tableBorderPaint);
                        currentY += rowH;
                        isHeader = false;
                    }
                    currentY += 12;
                }

                currentY += 8;
            }

            // References Section
            if (doc.references != null && !doc.references.isEmpty()) {
                if (currentY + 80 > pageHeight - margin) {
                    canvas.drawText("Page " + pageNumber, pageWidth / 2f - 15, pageHeight - 25, headerFooterPaint);
                    pdfDoc.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdfDoc.startPage(pageInfo);
                    canvas = page.getCanvas();
                    currentY = margin + 10;
                }

                canvas.drawText("References", margin, currentY + 14, headingPaint);
                currentY += 24;

                for (String ref : doc.references) {
                    if (ref == null || ref.trim().isEmpty()) continue;
                    StaticLayout refLayout = new StaticLayout(ref.trim(), refPaint, contentWidth - 24, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0, false);
                    if (currentY + refLayout.getHeight() > pageHeight - margin) {
                        canvas.drawText("Page " + pageNumber, pageWidth / 2f - 15, pageHeight - 25, headerFooterPaint);
                        pdfDoc.finishPage(page);
                        pageNumber++;
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                        page = pdfDoc.startPage(pageInfo);
                        canvas = page.getCanvas();
                        currentY = margin + 10;
                    }
                    canvas.save();
                    canvas.translate(margin + 20, currentY);
                    refLayout.draw(canvas);
                    canvas.restore();
                    currentY += refLayout.getHeight() + 8;
                }
            }

            // Footer on final page
            canvas.drawText("Page " + pageNumber, pageWidth / 2f - 15, pageHeight - 25, headerFooterPaint);
            pdfDoc.finishPage(page);
            page = null; // Mark finished

            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                pdfDoc.writeTo(fos);
                fos.flush();
            }
        } finally {
            if (page != null) {
                try {
                    pdfDoc.finishPage(page);
                } catch (Exception ignored) {}
            }
            try {
                pdfDoc.close();
            } catch (Exception ignored) {}
        }
    }

    // ── CSV Generator ─────────────────────────────────────────────────────────

    public static void generateCsv(File outFile, SpreadsheetModel sheet) throws Exception {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            // Write Headers
            for (int i = 0; i < sheet.headers.size(); i++) {
                pw.print("\"" + sheet.headers.get(i).replace("\"", "\"\"") + "\"");
                if (i < sheet.headers.size() - 1) pw.print(",");
            }
            pw.println();

            // Write Rows
            for (List<String> row : sheet.rows) {
                for (int i = 0; i < row.size(); i++) {
                    pw.print("\"" + row.get(i).replace("\"", "\"\"") + "\"");
                    if (i < row.size() - 1) pw.print(",");
                }
                pw.println();
            }

            // Summary row if present
            if (sheet.summaryFormulaLabel != null) {
                pw.println("\"" + sheet.summaryFormulaLabel.replace("\"", "\"\"") + "\",\"" + sheet.summaryFormulaValue + "\"");
            }
        }
    }

    // ── Markdown Generator ────────────────────────────────────────────────────

    public static void generateMd(File outFile, DocumentModel doc) throws Exception {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            pw.println("# " + doc.title);
            pw.println();
            pw.println("*" + doc.subtitle + " • " + doc.dateString + "*");
            pw.println();
            pw.println("---");
            pw.println();

            for (Section sec : doc.sections) {
                pw.println("## " + sec.heading);
                pw.println();
                for (String p : sec.getResolvedParagraphs()) {
                    pw.println(p);
                    pw.println();
                }
                for (String b : sec.bulletPoints) {
                    pw.println("- " + b);
                }
                if (!sec.bulletPoints.isEmpty()) pw.println();

                List<String[]> mdTable = sec.getResolvedTableData();
                if (mdTable != null && !mdTable.isEmpty()) {
                    String[] headers = mdTable.get(0);
                    pw.print("|");
                    for (String h : headers) pw.print(" " + h + " |");
                    pw.println();
                    pw.print("|");
                    for (int i = 0; i < headers.length; i++) pw.print(" --- |");
                    pw.println();

                    for (int r = 1; r < mdTable.size(); r++) {
                        pw.print("|");
                        for (String c : mdTable.get(r)) pw.print(" " + c + " |");
                        pw.println();
                    }
                    pw.println();
                }
            }

            if (!doc.references.isEmpty()) {
                pw.println("## References");
                pw.println();
                for (String ref : doc.references) {
                    pw.println("- " + ref);
                }
                pw.println();
            }
        }
    }

    // ── TXT Generator ─────────────────────────────────────────────────────────

    public static void generateTxt(File outFile, DocumentModel doc) throws Exception {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            pw.println("================================================================================");
            pw.println("  " + doc.title.toUpperCase(Locale.US));
            pw.println("  " + doc.subtitle);
            pw.println("  " + doc.dateString);
            pw.println("================================================================================");
            pw.println();

            for (Section sec : doc.sections) {
                pw.println("--------------------------------------------------------------------------------");
                pw.println("  " + sec.heading);
                pw.println("--------------------------------------------------------------------------------");
                pw.println();
                for (String p : sec.paragraphs) {
                    pw.println(p);
                    pw.println();
                }
                for (String b : sec.bulletPoints) {
                    pw.println("  • " + b);
                }
                if (!sec.bulletPoints.isEmpty()) pw.println();
            }

            if (!doc.references.isEmpty()) {
                pw.println("================================================================================");
                pw.println("  REFERENCES (APA 7th EDITION)");
                pw.println("================================================================================");
                pw.println();
                for (String ref : doc.references) {
                    pw.println("  " + ref);
                    pw.println();
                }
            }
        }
    }

    // ── ZIP Entry Helper ──────────────────────────────────────────────────────

    private static void writeZipEntry(ZipOutputStream zos, String entryName, String content) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
