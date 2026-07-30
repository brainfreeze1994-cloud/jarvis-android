package com.jarvis.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String JARVIS_URL = "https://jarvis-ai-seven-dun.vercel.app";
    private static final int MIC_PERMISSION_CODE = 101;
    private static final int FILE_CHOOSER_CODE   = 102;

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ValueCallback<Uri[]> filePathCallback;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public class JarvisBridge {

        @JavascriptInterface
        public void speak(String text) {
            if (!ttsReady || tts == null) return;
            String plain = text
                .replaceAll("```[\\s\\S]*?```", "code block.")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("#{1,6}\\s", "")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                .replaceAll("(?m)^\\s*[-*+]\\s", "")
                .replaceAll("(?m)^\\s*\\d+\\.\\s", "");
            if (plain.length() > 600) plain = plain.substring(0, 600);
            tts.stop();
            tts.speak(plain, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_UTTERANCE");
            final String js = "if(window._jarvisSpeakStart) window._jarvisSpeakStart();";
            mainHandler.post(() -> webView.evaluateJavascript(js, null));
        }

        @JavascriptInterface
        public void stopSpeaking() {
            if (tts != null) tts.stop();
        }

        @JavascriptInterface
        public boolean isTtsReady() {
            return ttsReady;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        setContentView(R.layout.activity_main);

        webView      = findViewById(R.id.webview);
        progressBar  = findViewById(R.id.progress_bar);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        tts = new TextToSpeech(this, this);

        setupWebView();
        requestMicPermission();

        swipeRefresh.setColorSchemeColors(0xFF3B82F6);
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFF0D1B2A);
        swipeRefresh.setOnRefreshListener(() -> webView.reload());

        webView.loadUrl(JARVIS_URL);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.UK);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }

            Set<Voice> voices = tts.getVoices();
            Voice bestVoice = null;

            if (voices != null) {
                // 1st priority: British male offline voice
                for (Voice v : voices) {
                    String name = v.getName().toLowerCase();
                    String lang = v.getLocale().getLanguage() + "-" + v.getLocale().getCountry();
                    boolean isBritish = lang.equalsIgnoreCase("en-GB") || name.contains("en-gb");
                    boolean isMale = name.contains("male") || name.contains("daniel") ||
                                     name.contains("james") || name.contains("george") ||
                                     !name.contains("female");
                    if (isBritish && isMale && !v.isNetworkConnectionRequired()) {
                        bestVoice = v;
                        break;
                    }
                }
                // 2nd priority: any British offline voice
                if (bestVoice == null) {
                    for (Voice v : voices) {
                        String lang = v.getLocale().getLanguage() + "-" + v.getLocale().getCountry();
                        if (lang.equalsIgnoreCase("en-GB") && !v.isNetworkConnectionRequired()) {
                            bestVoice = v;
                            break;
                        }
                    }
                }
                // 3rd priority: any English offline voice
                if (bestVoice == null) {
                    for (Voice v : voices) {
                        if (v.getLocale().getLanguage().equals("en") && !v.isNetworkConnectionRequired()) {
                            bestVoice = v;
                            break;
                        }
                    }
                }
            }

            if (bestVoice != null) tts.setVoice(bestVoice);

            tts.setSpeechRate(0.90f);  // slower = authoritative
            tts.setPitch(0.75f);        // lower = deep male voice

            ttsReady = true;
            mainHandler.post(this::injectTtsBridge);
        }
    }

    private void injectTtsBridge() {
        String js =
            "(function() {" +
            "  if (!window.Android) return;" +
            "  window._nativeTtsReady = true;" +
            "  var origSpeak = window.speak;" +
            "  if (typeof origSpeak === 'function') {" +
            "    window.speak = function(text) {" +
            "      Android.speak(text);" +
            "      setTimeout(function() {" +
            "        if (window.wakeEnabled) setState('wake'); else setState('idle');" +
            "      }, Math.min(text.length * 60, 15000));" +
            "    };" +
            "  }" +
            "})();";
        webView.evaluateJavascript(js, null);
    }

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

        webView.addJavascriptInterface(new JarvisBridge(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("jarvis-ai-seven-dun.vercel.app") || url.startsWith("javascript:")) {
                    return false;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) {}
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                webView.evaluateJavascript("document.body.style.background='#070d1a';", null);
                if (ttsReady) injectTtsBridge();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                webView.loadData(buildOfflinePage(), "text/html", "UTF-8");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback2,
                                             FileChooserParams fileChooserParams) {
                filePathCallback = filePathCallback2;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_CODE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                result.confirm();
                return true;
            }
        });
    }

    private void requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                webView.reload();
            } else {
                Toast.makeText(this, "Microphone access needed for voice commands", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_CODE && filePathCallback != null) {
            filePathCallback.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            );
            filePathCallback = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onResume()  { super.onResume();  webView.onResume(); }
    @Override protected void onPause()   { super.onPause();   webView.onPause(); if (tts != null) tts.stop(); }
    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        webView.destroy();
        super.onDestroy();
    }

    private String buildOfflinePage() {
        return "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<style>*{margin:0;padding:0;box-sizing:border-box}" +
            "body{background:#070d1a;color:#e2e8f0;font-family:sans-serif;" +
            "display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;gap:1rem}" +
            "h1{font-size:2rem;letter-spacing:0.4em;color:#93c5fd}" +
            "p{color:#64748b;font-size:0.85rem;letter-spacing:0.2em}" +
            "button{background:#1d4ed8;border:none;color:white;padding:0.75rem 2rem;" +
            "border-radius:2rem;font-size:0.9rem;cursor:pointer;margin-top:1rem}" +
            "</style></head><body>" +
            "<h1>J.A.R.V.I.S</h1><p>NO NETWORK CONNECTION</p>" +
            "<button onclick='location.reload()'>RETRY</button>" +
            "</body></html>";
    }
}
