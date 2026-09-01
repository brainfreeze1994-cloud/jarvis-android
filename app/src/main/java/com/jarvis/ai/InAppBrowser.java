package com.jarvis.ai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * In-App Browser — WebView with blue HENRY theme.
 * Opens any URL, has back/forward/refresh, address bar.
 * "Open browser" / "Browse [url]" / "Open [url]" / tap any link in chat
 */
public class InAppBrowser extends AppCompatActivity {

    public static final String EXTRA_URL   = "browser_url";
    public static final String EXTRA_TITLE = "browser_title";

    private WebView    webView;
    private EditText   etUrl;
    private ProgressBar progressBar;
    private TextView   tvTitle;

    public static boolean isBrowserCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("open browser") || lower.startsWith("browse ") ||
               lower.startsWith("go to http") || lower.startsWith("open http") ||
               lower.startsWith("visit ") || lower.contains("open website") ||
               lower.contains("in browser") || lower.contains("show website");
    }

    public static String parseUrl(String text) {
        String lower = text.toLowerCase(Locale.US);
        String url = text
            .replaceAll("(?i)^(open browser|browse|open|visit|go to)\\s*", "")
            .replaceAll("(?i)\\s*(in browser|in the browser)$", "")
            .trim();
        if (!url.startsWith("http")) url = "https://" + url;
        return url;
    }

    public static void open(Context ctx, String url) {
        if (!url.startsWith("http")) url = "https://" + url;
        Intent i = new Intent(ctx, InAppBrowser.class);
        i.putExtra(EXTRA_URL, url);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = getIntent() != null ? getIntent().getStringExtra(EXTRA_URL) : "https://google.com";
        if (url == null) url = "https://google.com";

        // Build UI
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF020C1B);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0xFF041828);
        topBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        topBar.setPadding(8, 8, 8, 8);

        // Back
        TextView btnBack = new TextView(this);
        btnBack.setText("←");
        btnBack.setTextColor(0xFF00BEFF);
        btnBack.setTextSize(20f);
        btnBack.setPadding(16, 8, 16, 8);
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        topBar.addView(btnBack);

        // URL bar
        etUrl = new EditText(this);
        etUrl.setTextColor(0xFF80DFFF);
        etUrl.setHintTextColor(0xFF004466);
        etUrl.setHint("Enter URL…");
        etUrl.setTextSize(12f);
        etUrl.setBackgroundColor(0xFF020C1B);
        etUrl.setPadding(12, 4, 12, 4);
        etUrl.setSingleLine(true);
        etUrl.setText(url);
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            String u = etUrl.getText().toString().trim();
            if (!u.startsWith("http")) u = "https://" + u;
            webView.loadUrl(u);
            return true;
        });
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        etUrl.setLayoutParams(urlLp);
        topBar.addView(etUrl);

        // Refresh
        TextView btnRefresh = new TextView(this);
        btnRefresh.setText("↻");
        btnRefresh.setTextColor(0xFF00BEFF);
        btnRefresh.setTextSize(20f);
        btnRefresh.setPadding(16, 8, 16, 8);
        btnRefresh.setOnClickListener(v -> webView.reload());
        topBar.addView(btnRefresh);

        // External
        TextView btnExt = new TextView(this);
        btnExt.setText("↗");
        btnExt.setTextColor(0xFF004466);
        btnExt.setTextSize(18f);
        btnExt.setPadding(8, 8, 8, 8);
        btnExt.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl()))); }
            catch (Exception ignored) {}
        });
        topBar.addView(btnExt);

        // Close
        TextView btnClose = new TextView(this);
        btnClose.setText("✕");
        btnClose.setTextColor(0xFFCC3030);
        btnClose.setTextSize(18f);
        btnClose.setPadding(12, 8, 12, 8);
        btnClose.setOnClickListener(v -> finish());
        topBar.addView(btnClose);

        root.addView(topBar);

        // Progress bar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF00BEFF));
        progressBar.setBackgroundColor(0xFF020C1B);
        root.addView(progressBar);

        // WebView
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Safari/537.36");
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                etUrl.setText(url);
                progressBar.setVisibility(View.GONE);
            }
            @Override
            public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                if (view != null) {
                    ViewGroup parent = (ViewGroup) view.getParent();
                    if (parent != null) {
                        parent.removeView(view);
                    }
                    view.destroy();
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int progress) {
                progressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
                progressBar.setProgress(progress);
            }
            @Override public void onReceivedTitle(WebView view, String title) {
                setTitle(title);
            }
        });
        webView.loadUrl(url);
        root.addView(webView);
        setContentView(root);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
