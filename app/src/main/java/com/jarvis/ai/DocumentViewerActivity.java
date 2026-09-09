package com.jarvis.ai;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DocumentViewerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_TITLE     = "extra_title";
    public static final String EXTRA_MIME_TYPE = "extra_mime_type";

    private File targetFile;
    private String mimeType = "";
    private String docTitle = "";

    // PDF Renderer state
    private ParcelFileDescriptor pfd;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int pdfPageCount = 0;
    private int currentPdfIndex = 0;

    // Slide state
    private static class SlideData {
        String title = "";
        List<String> bullets = new ArrayList<>();
        String notes = "";
    }
    private final List<SlideData> slides = new ArrayList<>();
    private int currentSlideIndex = 0;

    // Views
    private TextView tvDocTitle;
    private TextView tvPageIndicator;
    private ImageView ivPdfPage;
    private LinearLayout slideContainer;
    private TextView tvSlideTitle;
    private LinearLayout llSlideBullets;
    private TextView tvSlideNotes;
    private Button btnPrev;
    private Button btnNext;
    private View pdfViewWrap;
    private ScrollView slideViewWrap;

    public static void start(Context context, File file, String mimeType, String title) {
        Intent intent = new Intent(context, DocumentViewerActivity.class);
        intent.putExtra(EXTRA_FILE_PATH, file.getAbsolutePath());
        intent.putExtra(EXTRA_MIME_TYPE, mimeType);
        intent.putExtra(EXTRA_TITLE, title != null ? title : file.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getIntent().getStringExtra(EXTRA_FILE_PATH);
        mimeType = getIntent().getStringExtra(EXTRA_MIME_TYPE);
        docTitle = getIntent().getStringExtra(EXTRA_TITLE);

        if (path == null) {
            Toast.makeText(this, "No document path specified.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        targetFile = new File(path);
        if (!targetFile.exists()) {
            Toast.makeText(this, "Document file not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (docTitle == null || docTitle.isEmpty()) {
            docTitle = targetFile.getName();
        }

        if (mimeType == null || mimeType.isEmpty()) {
            if (path.toLowerCase().endsWith(".pdf")) {
                mimeType = "application/pdf";
            } else if (path.toLowerCase().endsWith(".pptx")) {
                mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            } else {
                mimeType = "application/octet-stream";
            }
        }

        buildUi();

        if (isPdf()) {
            loadPdf();
        } else {
            loadSlides();
        }
    }

    private boolean isPdf() {
        return mimeType.contains("pdf") || targetFile.getName().toLowerCase().endsWith(".pdf");
    }

    @SuppressLint("SetTextI18n")
    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF030A16);
        setContentView(root);

        // ── Top Toolbar ──────────────────────────────────────────────────────────
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(16, 16, 16, 16);
        toolbar.setBackgroundColor(0xFF061528);

        Button btnBack = new Button(this);
        btnBack.setText("←");
        btnBack.setTextSize(18);
        btnBack.setTextColor(0xFF00D4FF);
        btnBack.setBackgroundColor(Color.TRANSPARENT);
        btnBack.setOnClickListener(v -> finish());
        toolbar.addView(btnBack, new LinearLayout.LayoutParams(110, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tvDocTitle = new TextView(this);
        tvDocTitle.setText(docTitle);
        tvDocTitle.setTextSize(15);
        tvDocTitle.setTextColor(0xFFFFFFFF);
        tvDocTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDocTitle.setSingleLine(true);
        titleBlock.addView(tvDocTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText(isPdf() ? "H.E.N.R.Y. PDF Engine" : "H.E.N.R.Y. Presentation Deck");
        tvSubtitle.setTextSize(11);
        tvSubtitle.setTextColor(0xFF00D4FF);
        titleBlock.addView(tvSubtitle);
        toolbar.addView(titleBlock);

        // Share button
        Button btnShare = new Button(this);
        btnShare.setText("Share");
        btnShare.setTextSize(12);
        btnShare.setTextColor(0xFF00D4FF);
        btnShare.setBackgroundColor(0xFF0A2240);
        btnShare.setPadding(16, 8, 16, 8);
        btnShare.setOnClickListener(v -> shareDocument());
        LinearLayout.LayoutParams lpShare = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpShare.setMargins(8, 0, 8, 0);
        toolbar.addView(btnShare, lpShare);

        // External app button
        Button btnExternal = new Button(this);
        btnExternal.setText("External");
        btnExternal.setTextSize(12);
        btnExternal.setTextColor(0xFF88AACC);
        btnExternal.setBackgroundColor(0xFF0A2240);
        btnExternal.setPadding(16, 8, 16, 8);
        btnExternal.setOnClickListener(v -> openInExternalApp());
        toolbar.addView(btnExternal, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── Main Content Area ────────────────────────────────────────────────────
        if (isPdf()) {
            pdfViewWrap = new LinearLayout(this);
            ((LinearLayout) pdfViewWrap).setOrientation(LinearLayout.VERTICAL);
            ((LinearLayout) pdfViewWrap).setGravity(Gravity.CENTER);
            pdfViewWrap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            pdfViewWrap.setBackgroundColor(0xFF01060E);

            ivPdfPage = new ImageView(this);
            ivPdfPage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivPdfPage.setAdjustViewBounds(true);
            ((LinearLayout) pdfViewWrap).addView(ivPdfPage, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            root.addView(pdfViewWrap);
        } else {
            slideViewWrap = new ScrollView(this);
            slideViewWrap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            slideViewWrap.setBackgroundColor(0xFF01060E);

            slideContainer = new LinearLayout(this);
            slideContainer.setOrientation(LinearLayout.VERTICAL);
            slideContainer.setPadding(28, 28, 28, 28);

            // Presentation Slide Card
            LinearLayout slideCard = new LinearLayout(this);
            slideCard.setOrientation(LinearLayout.VERTICAL);
            slideCard.setBackgroundColor(0xFF071B33);
            slideCard.setPadding(32, 32, 32, 32);

            tvSlideTitle = new TextView(this);
            tvSlideTitle.setTextSize(20);
            tvSlideTitle.setTextColor(0xFF00D4FF);
            tvSlideTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            slideCard.addView(tvSlideTitle);

            View divider = new View(this);
            divider.setBackgroundColor(0xFF00D4FF);
            LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
            lpDiv.setMargins(0, 16, 0, 24);
            slideCard.addView(divider, lpDiv);

            llSlideBullets = new LinearLayout(this);
            llSlideBullets.setOrientation(LinearLayout.VERTICAL);
            slideCard.addView(llSlideBullets);

            tvSlideNotes = new TextView(this);
            tvSlideNotes.setTextSize(12);
            tvSlideNotes.setTextColor(0xFF88BBDD);
            tvSlideNotes.setPadding(16, 16, 16, 16);
            tvSlideNotes.setBackgroundColor(0xFF040E1B);
            LinearLayout.LayoutParams lpNotes = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpNotes.setMargins(0, 24, 0, 0);
            slideCard.addView(tvSlideNotes, lpNotes);

            slideContainer.addView(slideCard);
            slideViewWrap.addView(slideContainer);
            root.addView(slideViewWrap);
        }

        // ── Bottom Navigation Controls ───────────────────────────────────────────
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER);
        navBar.setPadding(20, 16, 20, 20);
        navBar.setBackgroundColor(0xFF061528);

        btnPrev = new Button(this);
        btnPrev.setText("◀ Previous");
        btnPrev.setTextSize(13);
        btnPrev.setTextColor(0xFF00D4FF);
        btnPrev.setBackgroundColor(0xFF0A2240);
        btnPrev.setOnClickListener(v -> {
            if (isPdf()) {
                if (currentPdfIndex > 0) renderPdfPage(currentPdfIndex - 1);
            } else {
                if (currentSlideIndex > 0) showSlide(currentSlideIndex - 1);
            }
        });
        navBar.addView(btnPrev, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tvPageIndicator = new TextView(this);
        tvPageIndicator.setText("Page 1 of 1");
        tvPageIndicator.setTextSize(14);
        tvPageIndicator.setTextColor(0xFFFFFFFF);
        tvPageIndicator.setGravity(Gravity.CENTER);
        navBar.addView(tvPageIndicator, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnNext = new Button(this);
        btnNext.setText("Next ▶");
        btnNext.setTextSize(13);
        btnNext.setTextColor(0xFF00D4FF);
        btnNext.setBackgroundColor(0xFF0A2240);
        btnNext.setOnClickListener(v -> {
            if (isPdf()) {
                if (currentPdfIndex < pdfPageCount - 1) renderPdfPage(currentPdfIndex + 1);
            } else {
                if (currentSlideIndex < slides.size() - 1) showSlide(currentSlideIndex + 1);
            }
        });
        navBar.addView(btnNext, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(navBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ── PDF Rendering ────────────────────────────────────────────────────────
    private void loadPdf() {
        try {
            pfd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pfd);
            pdfPageCount = pdfRenderer.getPageCount();
            if (pdfPageCount > 0) {
                renderPdfPage(0);
            } else {
                Toast.makeText(this, "Empty PDF document.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not open PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void renderPdfPage(int index) {
        if (pdfRenderer == null || index < 0 || index >= pdfPageCount) return;

        try {
            if (currentPage != null) {
                currentPage.close();
                currentPage = null;
            }

            currentPage = pdfRenderer.openPage(index);
            currentPdfIndex = index;

            int width = currentPage.getWidth();
            int height = currentPage.getHeight();

            // Render at high resolution (2x scale for crisp text)
            float density = getResources().getDisplayMetrics().density;
            int renderWidth = (int) (width * Math.max(1.5f, density));
            int renderHeight = (int) (height * Math.max(1.5f, density));

            Bitmap bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE); // standard white PDF canvas
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            ivPdfPage.setImageBitmap(bitmap);

            tvPageIndicator.setText("Page " + (currentPdfIndex + 1) + " of " + pdfPageCount);
            btnPrev.setEnabled(currentPdfIndex > 0);
            btnNext.setEnabled(currentPdfIndex < pdfPageCount - 1);
            btnPrev.setAlpha(currentPdfIndex > 0 ? 1f : 0.4f);
            btnNext.setAlpha(currentPdfIndex < pdfPageCount - 1 ? 1f : 0.4f);
        } catch (Exception e) {
            Toast.makeText(this, "Error rendering page: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ── Presentation / Slides Rendering ──────────────────────────────────────
    private void loadSlides() {
        // Parse PPTX OpenXML slides if present
        try {
            parsePptxFile(targetFile);
        } catch (Exception ignored) {}

        if (slides.isEmpty()) {
            // Default placeholder slide deck if parsing was empty
            SlideData s1 = new SlideData();
            s1.title = docTitle;
            s1.bullets.add("H.E.N.R.Y. Presentation Deck");
            s1.bullets.add("Generated by H.E.N.R.Y. Document Engine");
            s1.notes = "Presenter talking points and executive overview.";
            slides.add(s1);
        }

        showSlide(0);
    }

    private void parsePptxFile(File file) {
        slides.clear();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                    StringBuilder sb = new StringBuilder();
                    byte[] buf = new byte[2048];
                    int len;
                    while ((len = zis.read(buf)) > 0) {
                        sb.append(new String(buf, 0, len));
                    }
                    String xml = sb.toString();
                    SlideData slide = parseSlideXml(xml);
                    if (slide != null) {
                        slides.add(slide);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private SlideData parseSlideXml(String xml) {
        SlideData s = new SlideData();
        // Extract <a:t> text nodes
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("<a:t>([^<]+)</a:t>");
        java.util.regex.Matcher m = p.matcher(xml);
        List<String> texts = new ArrayList<>();
        while (m.find()) {
            String txt = m.group(1).trim();
            if (!txt.isEmpty()) texts.add(txt);
        }
        if (texts.isEmpty()) return null;

        s.title = texts.get(0);
        for (int i = 1; i < texts.size(); i++) {
            String line = texts.get(i);
            if (line.toLowerCase().startsWith("notes:") || line.toLowerCase().startsWith("presenter:")) {
                s.notes = line;
            } else {
                s.bullets.add(line);
            }
        }
        if (s.notes.isEmpty()) {
            s.notes = "Presenter Talking Points: Cover the key takeaways cleanly and confidently.";
        }
        return s;
    }

    @SuppressLint("SetTextI18n")
    private void showSlide(int index) {
        if (slides.isEmpty() || index < 0 || index >= slides.size()) return;
        currentSlideIndex = index;
        SlideData s = slides.get(index);

        tvSlideTitle.setText(s.title);
        llSlideBullets.removeAllViews();

        for (String bullet : s.bullets) {
            LinearLayout bulletRow = new LinearLayout(this);
            bulletRow.setOrientation(LinearLayout.HORIZONTAL);
            bulletRow.setPadding(0, 8, 0, 8);

            TextView dot = new TextView(this);
            dot.setText("◆ ");
            dot.setTextSize(14);
            dot.setTextColor(0xFF00D4FF);
            bulletRow.addView(dot);

            TextView text = new TextView(this);
            text.setText(bullet);
            text.setTextSize(15);
            text.setTextColor(0xFFE2EDF8);
            text.setLineSpacing(4, 1.2f);
            bulletRow.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            llSlideBullets.addView(bulletRow);
        }

        if (s.notes != null && !s.notes.isEmpty()) {
            tvSlideNotes.setVisibility(View.VISIBLE);
            tvSlideNotes.setText("🎙 Notes: " + s.notes);
        } else {
            tvSlideNotes.setVisibility(View.GONE);
        }

        tvPageIndicator.setText("Slide " + (currentSlideIndex + 1) + " of " + slides.size());
        btnPrev.setEnabled(currentSlideIndex > 0);
        btnNext.setEnabled(currentSlideIndex < slides.size() - 1);
        btnPrev.setAlpha(currentSlideIndex > 0 ? 1f : 0.4f);
        btnNext.setAlpha(currentSlideIndex < slides.size() - 1 ? 1f : 0.4f);
    }

    // ── External Actions ─────────────────────────────────────────────────────
    private void shareDocument() {
        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", targetFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, docTitle);
            intent.putExtra(Intent.EXTRA_TEXT, "Shared from H.E.N.R.Y. Document Engine: " + docTitle);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("", contentUri));

            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo ri : resInfoList) {
                grantUriPermission(ri.activityInfo.packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(Intent.createChooser(intent, "Share " + targetFile.getName()));
        } catch (Exception e) {
            Toast.makeText(this, "Could not share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openInExternalApp() {
        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", targetFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClipData(ClipData.newRawUri("", contentUri));

            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo ri : resInfoList) {
                grantUriPermission(ri.activityInfo.packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(Intent.createChooser(intent, "Open " + targetFile.getName()));
        } catch (Exception e) {
            Toast.makeText(this, "No external app available for this format.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (currentPage != null) {
                currentPage.close();
                currentPage = null;
            }
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
            if (pfd != null) {
                pfd.close();
                pfd = null;
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}
