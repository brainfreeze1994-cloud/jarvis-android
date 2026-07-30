package com.jarvis.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String JARVIS_URL       = "https://jarvis-ai-seven-dun.vercel.app";
    private static final int FILE_CHOOSER_CODE   = 102;
    private static final int PERMISSIONS_CODE    = 104;

    private WebView             webView;
    private ProgressBar         progressBar;
    private SwipeRefreshLayout  swipeRefresh;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri                 cameraImageUri;
    private TextToSpeech        tts;
    private boolean             ttsReady = false;
    private final Handler       mainHandler = new Handler(Looper.getMainLooper());

    // ════════════════════════════════════════════════════════════════════════
    //  JavaScript Bridge  (window.Android.xxx  called from index.html)
    // ════════════════════════════════════════════════════════════════════════
    public class JarvisBridge {

        /** Speak text via native Android TTS */
        @JavascriptInterface
        public void speak(String text) {
            if (!ttsReady || tts == null) return;
            String plain = stripMarkdown(text);
            if (plain.length() > 600) plain = plain.substring(0, 600);
            tts.stop();
            tts.speak(plain, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_UTTERANCE");
        }

        /** Stop TTS immediately */
        @JavascriptInterface
        public void stopSpeaking() {
            if (tts != null) tts.stop();
        }

        /** Returns true once TTS engine is ready */
        @JavascriptInterface
        public boolean isTtsReady() {
            return ttsReady;
        }

        /** Open the native file / photo / camera chooser */
        @JavascriptInterface
        public void openFileChooser() {
            mainHandler.post(MainActivity.this::showFileChooserDialog);
        }

        /** Persist chat history JSON to SharedPreferences */
        @JavascriptInterface
        public void saveHistory(String json) {
            getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
                .edit()
                .putString("chat_history", json)
                .apply();
        }

        /** Return saved chat history JSON (empty array if none) */
        @JavascriptInterface
        public String loadHistory() {
            return getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
                .getString("chat_history", "[]");
        }

        /** Wipe saved chat history */
        @JavascriptInterface
        public void clearHistory() {
            getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
                .edit()
                .remove("chat_history")
                .apply();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY  |
            View.SYSTEM_UI_FLAG_FULLSCREEN         |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        setContentView(R.layout.activity_main);

        webView      = findViewById(R.id.webview);
        progressBar  = findViewById(R.id.progress_bar);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        tts = new TextToSpeech(this, this);

        setupWebView();
        requestAllPermissions();

        swipeRefresh.setColorSchemeColors(0xFF3B82F6);
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFF0D1B2A);
        swipeRefresh.setOnRefreshListener(() -> webView.reload());

        webView.loadUrl(JARVIS_URL);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TTS initialisation
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) return;

        // Prefer British English
        int res = tts.setLanguage(Locale.UK);
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED)
            tts.setLanguage(Locale.US);

        // Pick the best available voice: British male offline → any British → any English
        Set<Voice> voices = tts.getVoices();
        Voice best = null;
        if (voices != null) {
            // Pass 1 – British male offline
            for (Voice v : voices) {
                String n = v.getName().toLowerCase();
                String l = v.getLocale().getLanguage() + "-" + v.getLocale().getCountry();
                boolean brit = l.equalsIgnoreCase("en-GB") || n.contains("en-gb");
                boolean male = n.contains("male") || n.contains("daniel") ||
                               n.contains("james") || n.contains("george") ||
                               !n.contains("female");
                if (brit && male && !v.isNetworkConnectionRequired()) { best = v; break; }
            }
            // Pass 2 – any British offline
            if (best == null) {
                for (Voice v : voices) {
                    String l = v.getLocale().getLanguage() + "-" + v.getLocale().getCountry();
                    if (l.equalsIgnoreCase("en-GB") && !v.isNetworkConnectionRequired()) { best = v; break; }
                }
            }
            // Pass 3 – any English offline
            if (best == null) {
                for (Voice v : voices) {
                    if (v.getLocale().getLanguage().equals("en") && !v.isNetworkConnectionRequired()) { best = v; break; }
                }
            }
        }
        if (best != null) tts.setVoice(best);
        tts.setSpeechRate(0.90f);   // authoritative pace
        tts.setPitch(0.75f);         // deep masculine tone

        // Callback → JS when speech ends so UI state resets
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id)  { notifyJsTtsFinished(); }
            @Override public void onError(String id) { notifyJsTtsFinished(); }
        });

        ttsReady = true;
        mainHandler.post(() -> {
            if (webView != null)
                webView.evaluateJavascript("window._androidTtsReady = true;", null);
        });
    }

    private void notifyJsTtsFinished() {
        mainHandler.post(() -> {
            if (webView != null)
                webView.evaluateJavascript("if(window._ttsFinished) window._ttsFinished();", null);
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  WebView setup
    // ════════════════════════════════════════════════════════════════════════
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13; JARVIS) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        );

        // Register the JS bridge as  window.Android
        webView.addJavascriptInterface(new JarvisBridge(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.contains("jarvis-ai-seven-dun.vercel.app") ||
                    url.startsWith("javascript:")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                catch (Exception ignored) {}
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                webView.evaluateJavascript("document.body.style.background='#070d1a';", null);
            }

            @Override
            public void onReceivedError(WebView view, int code, String desc, String failUrl) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                webView.loadData(buildOfflinePage(), "text/html", "UTF-8");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                if (p < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(p);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback cb) {
                cb.invoke(origin, true, false);
            }

            /** Called by WebView when <input type="file"> is tapped — or from our JS bridge */
            @Override
            public boolean onShowFileChooser(WebView wv,
                                             ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                // Cancel any previous pending callback to avoid leaking
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                showFileChooserDialog();
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String msg, JsResult r) {
                r.confirm(); return true;
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  File / Camera chooser  — camera + gallery + any file in one dialog
    // ════════════════════════════════════════════════════════════════════════
    private void showFileChooserDialog() {
        // 1. Any file
        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("*/*");
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        // 2. Gallery images
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        // 3. Camera capture
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraImageUri = null;
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File photo = createImageFile();
                cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photo
                );
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            } catch (IOException e) {
                cameraImageUri = null;
            }
        }

        // Combine into a single chooser dialog
        Intent chooser = Intent.createChooser(fileIntent, "Attach: file, photo or camera");
        List<Intent> extras = new ArrayList<>();
        extras.add(galleryIntent);
        if (cameraImageUri != null) extras.add(cameraIntent);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extras.toArray(new Intent[0]));

        try {
            startActivityForResult(chooser, FILE_CHOOSER_CODE);
        } catch (Exception e) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(null);
                filePathCallback = null;
            }
        }
    }

    /** Creates a temp JPEG file in the app cache directory for camera output */
    private File createImageFile() throws IOException {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return File.createTempFile("JARVIS_" + stamp, ".jpg", getCacheDir());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Activity result — receive file / photo from chooser
    // ════════════════════════════════════════════════════════════════════════
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_CODE) {
            if (filePathCallback == null) return;

            Uri[] results = null;

            if (resultCode == RESULT_OK) {
                if (data == null || data.getData() == null) {
                    // Camera capture path — data is null, use cameraImageUri
                    if (cameraImageUri != null) results = new Uri[]{ cameraImageUri };
                } else if (data.getClipData() != null) {
                    // Multiple files selected
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++)
                        results[i] = data.getClipData().getItemAt(i).getUri();
                } else {
                    // Single file selected
                    results = new Uri[]{ data.getData() };
                }
            }

            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            cameraImageUri   = null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Runtime permissions  (mic + camera + storage, API-level aware)
    // ════════════════════════════════════════════════════════════════════════
    private void requestAllPermissions() {
        List<String> needed = new ArrayList<>();
        needed.add(Manifest.permission.RECORD_AUDIO);
        needed.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // API 33+
            needed.add(Manifest.permission.READ_MEDIA_IMAGES);
            needed.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        List<String> toRequest = new ArrayList<>();
        for (String p : needed) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                toRequest.add(p);
        }
        if (!toRequest.isEmpty())
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), PERMISSIONS_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int code,
                                           @NonNull String[] perms,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(code, perms, grantResults);
        if (code == PERMISSIONS_CODE) {
            boolean micOk = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED;
            if (micOk) webView.reload();
            else Toast.makeText(this,
                "Microphone permission is needed for voice commands.", Toast.LENGTH_LONG).show();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Standard overrides
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack(); return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onResume()  { super.onResume();  webView.onResume(); }
    @Override protected void onPause()   { super.onPause();   webView.onPause();  if (tts != null) tts.stop(); }
    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        webView.destroy();
        super.onDestroy();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════

    /** Strip markdown so TTS reads clean text */
    private String stripMarkdown(String text) {
        return text
            .replaceAll("```[\\s\\S]*?```", "code block.")
            .replaceAll("`([^`]+)`",           "$1")
            .replaceAll("\\*\\*(.*?)\\*\\*",   "$1")
            .replaceAll("\\*(.*?)\\*",         "$1")
            .replaceAll("#{1,6}\\s",           "")
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
            .replaceAll("(?m)^\\s*[-*+]\\s",   "")
            .replaceAll("(?m)^\\s*\\d+\\.\\s", "");
    }

    /** Minimal offline error page */
    private String buildOfflinePage() {
        return "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<style>" +
            "* { margin:0; padding:0; box-sizing:border-box }" +
            "body { background:#070d1a; color:#e2e8f0; font-family:sans-serif;" +
            "       display:flex; flex-direction:column; align-items:center;" +
            "       justify-content:center; height:100vh; gap:1rem }" +
            "h1 { font-size:2rem; letter-spacing:0.4em; color:#93c5fd }" +
            "p  { color:#64748b; font-size:0.85rem; letter-spacing:0.2em }" +
            "button { background:#1d4ed8; border:none; color:white;" +
            "         padding:0.75rem 2rem; border-radius:2rem;" +
            "         font-size:0.9rem; cursor:pointer; margin-top:1rem }" +
            "</style></head><body>" +
            "<h1>J.A.R.V.I.S</h1>" +
            "<p>NO NETWORK CONNECTION</p>" +
            "<button onclick='location.reload()'>RETRY</button>" +
            "</body></html>";
    }
}
