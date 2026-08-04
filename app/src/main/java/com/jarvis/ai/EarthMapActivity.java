package com.jarvis.ai;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

public class EarthMapActivity extends AppCompatActivity {

    public static final int    REQUEST_CODE = 302;
    public static final String EXTRA_COUNTRY = "country_name";

    private WebView     webView;
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000610);
        setContentView(root);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(0xFF00D4FF));
        root.addView(progressBar,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 6));

        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(false);

        webView.addJavascriptInterface(new GlobeBridge(), "HenryGlobe");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) progressBar.setVisibility(View.GONE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Handle voice intent
                String country = getIntent().getStringExtra("fly_to");
                if (country != null && !country.isEmpty()) {
                    webView.evaluateJavascript(
                            "if(window.flyToCountry) flyToCountry('" +
                            country.replace("'", "\\'") + "');", null);
                }
            }
        });

        root.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        webView.loadDataWithBaseURL("https://jarvis-ai-seven-dun.vercel.app",
                buildGlobeHtml(), "text/html", "UTF-8", null);
    }

    // ── JS → Android bridge ───────────────────────────────────────────────
    class GlobeBridge {
        @JavascriptInterface
        public void onCountrySelected(String countryName) {
            // Return country name to MainActivity
            Intent result = new Intent();
            result.putExtra(EXTRA_COUNTRY, countryName);
            setResult(Activity.RESULT_OK, result);
            // Don't finish — let user keep exploring; chat result is bonus
        }

        @JavascriptInterface
        public void askHenryAbout(String countryName) {
            Intent result = new Intent();
            result.putExtra(EXTRA_COUNTRY, countryName);
            setResult(Activity.RESULT_OK, result);
            runOnUiThread(() -> finish());
        }

        @JavascriptInterface
        public void fetchCountryInfo(String country, String prompt) {
            // Fetch from Vercel API and return via JS callback
            new Thread(() -> {
                try {
                    org.json.JSONArray msgs = new org.json.JSONArray();
                    org.json.JSONObject msg = new org.json.JSONObject();
                    msg.put("role", "user");
                    msg.put("text", prompt);
                    msgs.put(msg);
                    org.json.JSONObject body = new org.json.JSONObject();
                    body.put("messages", msgs);
                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(40, java.util.concurrent.TimeUnit.SECONDS)
                            .build();
                    okhttp3.Request req = new okhttp3.Request.Builder()
                            .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                            .post(okhttp3.RequestBody.create(body.toString(),
                                    okhttp3.MediaType.parse("application/json")))
                            .build();
                    okhttp3.Response resp = client.newCall(req).execute();
                    String raw = resp.body() != null ? resp.body().string() : "{}";
                    org.json.JSONObject json = new org.json.JSONObject(raw);
                    String reply = json.optString("reply", "No data available.")
                            .replaceAll("\\[EMOTION:[^\\]]+\\]", "").trim()
                            .replace("\\", "\\\\").replace("'", "\\'")
                            .replace("\n", "\\n").replace("\r", "");
                    runOnUiThread(() ->
                        webView.evaluateJavascript(
                            "if(window.showCountryInfo) showCountryInfo('" +
                            reply + "');", null));
                } catch (Exception e) {
                    String err = ("Error: " + e.getMessage()).replace("'", "\\'");
                    runOnUiThread(() ->
                        webView.evaluateJavascript(
                            "if(window.showCountryInfo) showCountryInfo('" + err + "');", null));
                }
            }).start();
        }

        @JavascriptInterface
        public void fetchCountryByLatLon(double lat, double lon) {
            // Show loading in panel immediately
            runOnUiThread(() -> webView.evaluateJavascript(
                "document.getElementById('panel-idle').style.display='none';" +
                "document.getElementById('panel-content').style.display='none';" +
                "document.getElementById('panel-loading').style.display='block';" +
                "document.getElementById('panel-loading').textContent='🔍 Identifying country…';", null));

            new Thread(() -> {
                try {
                    // Step 1: identify country from lat/lon
                    String geoPrompt = "What country or territory is at latitude " +
                        String.format("%.2f", lat) + ", longitude " +
                        String.format("%.2f", lon) + "? Reply with ONLY the country name. If ocean, reply: Ocean.";
                    org.json.JSONObject geoBody = new org.json.JSONObject();
                    geoBody.put("messages", new org.json.JSONArray().put(
                        new org.json.JSONObject().put("role","user").put("text", geoPrompt)));
                    geoBody.put("overrideSystem", "Geography expert. Reply only with the country name or Ocean.");
                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS).build();
                    okhttp3.Response r1 = client.newCall(new okhttp3.Request.Builder()
                        .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                        .post(okhttp3.RequestBody.create(geoBody.toString(),
                            okhttp3.MediaType.parse("application/json"))).build()).execute();
                    String raw1 = r1.body() != null ? r1.body().string() : "{}";
                    String country = new org.json.JSONObject(raw1).optString("reply","")
                        .replaceAll("\\[EMOTION:[^\\]]+\\]","").trim()
                        .split("\n")[0].replaceAll("[*_#]","").trim();

                    if (country.isEmpty() || country.toLowerCase().contains("ocean")) {
                        runOnUiThread(() -> webView.evaluateJavascript(
                            "document.getElementById('panel-loading').style.display='none';" +
                            "document.getElementById('panel-idle').style.display='block';" +
                            "document.getElementById('panel-idle').textContent='🌊 Ocean — tap a land area.';", null));
                        return;
                    }

                    // Step 2: send country name back to MainActivity via Intent and close
                    final String finalCountry = country;
                    runOnUiThread(() -> {
                        Intent result = new Intent();
                        result.putExtra(EXTRA_COUNTRY, finalCountry);
                        setResult(Activity.RESULT_OK, result);
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> webView.evaluateJavascript(
                        "document.getElementById('panel-loading').style.display='none';" +
                        "document.getElementById('panel-idle').style.display='block';", null));
                }
            }).start();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // ── Full Globe HTML ───────────────────────────────────────────────────
    private String buildGlobeHtml() {
        return "<!DOCTYPE html><html><head>" +
            "<meta charset='UTF-8'/>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'/>" +
            "<title>HENRY EARTH MAP</title>" +
            "<script src='https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.min.js'></script>" +
            "<style>" +
            "*{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent;}" +
            "body{background:#000610;overflow:hidden;font-family:'Segoe UI',sans-serif;color:#c8e8f8;}" +
            "#app{width:100vw;height:100vh;display:flex;flex-direction:column;}" +

            // Top bar
            "#topbar{background:#020C1B;border-bottom:1px solid #0A2040;padding:10px 14px;" +
            "display:flex;align-items:center;gap:10px;flex-shrink:0;}" +
            "#topbar::after{content:'';position:absolute;top:0;left:0;right:0;height:2px;" +
            "background:linear-gradient(90deg,transparent,#00d4ff,transparent);}" +
            "#map-title{font-size:14px;letter-spacing:3px;color:#00D4FF;font-weight:bold;flex:1;}" +
            "#search-input{flex:1;background:#051828;border:1px solid #0A2040;color:#c8e8f8;" +
            "padding:8px 12px;font-size:13px;outline:none;border-radius:2px;max-width:260px;}" +
            "#search-input:focus{border-color:#00D4FF;}" +
            "#search-btn{background:#00D4FF;color:#000;border:none;padding:8px 14px;" +
            "font-size:12px;font-weight:bold;cursor:pointer;letter-spacing:1px;border-radius:2px;}" +
            "#close-btn{background:#051828;color:#00D4FF;border:1px solid #0A2040;" +
            "padding:7px 12px;font-size:12px;cursor:pointer;border-radius:2px;}" +

            // Body
            "#body{flex:1;display:flex;overflow:hidden;}" +
            "#globe-wrap{flex:1;position:relative;}" +
            "#globe-canvas{display:block;}" +
            "#hint{position:absolute;bottom:8px;left:50%;transform:translateX(-50%);" +
            "font-size:11px;color:#1E4A66;letter-spacing:1px;pointer-events:none;" +
            "background:rgba(2,12,27,.8);padding:4px 12px;white-space:nowrap;border:1px solid #0A2040;}" +
            "#globe-loading{position:absolute;inset:0;display:flex;align-items:center;" +
            "justify-content:center;flex-direction:column;gap:12px;color:#1E4A66;" +
            "font-size:13px;letter-spacing:2px;}" +

            // Info panel
            "#info-panel{width:230px;flex-shrink:0;background:#020C1B;border-left:1px solid #0A2040;" +
            "display:flex;flex-direction:column;overflow:hidden;}" +
            "#panel-hdr{font-size:10px;letter-spacing:3px;color:#1E4A66;padding:10px 12px;" +
            "border-bottom:1px solid #0A2040;}" +
            "#panel-scroll{flex:1;overflow-y:auto;-webkit-overflow-scrolling:touch;}" +
            "#panel-idle{padding:24px 14px;text-align:center;color:#1E4A66;font-size:12px;line-height:1.8;}" +
            "#panel-loading{display:none;padding:20px 14px;text-align:center;color:#1E4A66;font-size:12px;}" +
            "#panel-content{display:none;}" +
            ".c-header{padding:12px 14px;border-bottom:1px solid #0A2040;}" +
            ".c-flag{font-size:28px;}" +
            ".c-name{font-size:14px;font-weight:bold;color:#00D4FF;letter-spacing:2px;margin-top:4px;}" +
            ".c-body{padding:12px 14px;font-size:12px;line-height:1.85;color:#c8e8f8;}" +
            ".c-ask-btn{display:block;width:calc(100% - 24px);margin:0 12px 8px;" +
            "background:#051828;border:1px solid #0A2040;color:#00D4FF;padding:8px 10px;" +
            "font-size:11px;text-align:left;cursor:pointer;letter-spacing:1px;border-radius:2px;}" +
            ".c-ask-btn:active{background:#00D4FF;color:#000;}" +
            "#panel-ask{padding:10px 12px;border-top:1px solid #0A2040;display:flex;gap:6px;}" +
            "#ask-input{flex:1;background:#051828;border:1px solid #0A2040;color:#c8e8f8;" +
            "padding:7px 10px;font-size:12px;outline:none;border-radius:2px;}" +
            "#ask-btn{background:#00D4FF;color:#000;border:none;padding:7px 12px;" +
            "font-size:11px;font-weight:bold;cursor:pointer;border-radius:2px;}" +
            "@media(max-width:500px){#info-panel{display:none;}}" +
            "</style></head><body>" +
            "<div id='app'>" +
            "<div id='topbar'>" +
            "<div id='map-title'>◈ HENRY EARTH MAP</div>" +
            "<input id='search-input' type='text' placeholder='Search country…'/>" +
            "<button id='search-btn'>GO</button>" +
            "<button id='close-btn'>✕</button>" +
            "</div>" +
            "<div id='body'>" +
            "<div id='globe-wrap'>" +
            "<div id='globe-loading'>LOADING EARTH…</div>" +
            "<div id='hint'>DRAG TO ROTATE · PINCH TO ZOOM · TAP COUNTRY</div>" +
            "</div>" +
            "<div id='info-panel'>" +
            "<div id='panel-hdr'>◈ COUNTRY INTEL</div>" +
            "<div id='panel-scroll'>" +
            "<div id='panel-idle'>Tap any country on the globe to explore its history, culture, tourism & more.</div>" +
            "<div id='panel-loading'>🔍 Loading intel…</div>" +
            "<div id='panel-content'></div>" +
            "</div>" +
            "<div id='panel-ask'>" +
            "<input id='ask-input' type='text' placeholder='Ask about this country…'/>" +
            "<button id='ask-btn'>ASK</button>" +
            "</div>" +
            "</div>" +
            "</div>" +
            "</div>" +

            "<script>" +
            buildGlobeJS() +
            "</script></body></html>";
    }

    private String buildGlobeJS() {
        return
        "var scene,camera,renderer,globe,clouds,raycaster,mouse;" +
        "var rotX=0,rotY=0,rotVX=0,rotVY=0,zoom=2.8;" +
        "var isDragging=false,prevX=0,prevY=0,touchDist=null;" +
        "var selectedCountry=null;" +

        // Country bounding boxes
        "var REGIONS=[" +
        "{n:'Philippines',f:'🇵🇭',la:[4,22],lo:[116,128]}," +
        "{n:'Japan',f:'🇯🇵',la:[24,46],lo:[129,146]}," +
        "{n:'China',f:'🇨🇳',la:[15,53],lo:[73,135]}," +
        "{n:'India',f:'🇮🇳',la:[6,36],lo:[68,97]}," +
        "{n:'Australia',f:'🇦🇺',la:[-44,-10],lo:[113,154]}," +
        "{n:'Russia',f:'🇷🇺',la:[41,82],lo:[25,190]}," +
        "{n:'United States',f:'🇺🇸',la:[24,50],lo:[-125,-66]}," +
        "{n:'Canada',f:'🇨🇦',la:[41,84],lo:[-141,-52]}," +
        "{n:'Brazil',f:'🇧🇷',la:[-34,5],lo:[-74,-34]}," +
        "{n:'Argentina',f:'🇦🇷',la:[-55,-22],lo:[-74,-53]}," +
        "{n:'Mexico',f:'🇲🇽',la:[14,33],lo:[-118,-86]}," +
        "{n:'United Kingdom',f:'🇬🇧',la:[49,61],lo:[-8,2]}," +
        "{n:'France',f:'🇫🇷',la:[41,51],lo:[-5,10]}," +
        "{n:'Germany',f:'🇩🇪',la:[47,55],lo:[6,15]}," +
        "{n:'Italy',f:'🇮🇹',la:[36,48],lo:[6,19]}," +
        "{n:'Spain',f:'🇪🇸',la:[35,44],lo:[-10,4]}," +
        "{n:'Saudi Arabia',f:'🇸🇦',la:[16,32],lo:[36,56]}," +
        "{n:'UAE',f:'🇦🇪',la:[22,27],lo:[51,57]}," +
        "{n:'Egypt',f:'🇪🇬',la:[22,32],lo:[24,37]}," +
        "{n:'South Africa',f:'🇿🇦',la:[-35,-22],lo:[16,33]}," +
        "{n:'Nigeria',f:'🇳🇬',la:[4,14],lo:[2,15]}," +
        "{n:'Kenya',f:'🇰🇪',la:[-5,5],lo:[34,42]}," +
        "{n:'Turkey',f:'🇹🇷',la:[35,42],lo:[26,45]}," +
        "{n:'Iran',f:'🇮🇷',la:[25,40],lo:[44,64]}," +
        "{n:'Pakistan',f:'🇵🇰',la:[23,37],lo:[61,77]}," +
        "{n:'Indonesia',f:'🇮🇩',la:[-11,6],lo:[95,141]}," +
        "{n:'Thailand',f:'🇹🇭',la:[5,21],lo:[97,106]}," +
        "{n:'Vietnam',f:'🇻🇳',la:[8,24],lo:[102,110]}," +
        "{n:'Malaysia',f:'🇲🇾',la:[1,8],lo:[99,119]}," +
        "{n:'South Korea',f:'🇰🇷',la:[33,39],lo:[124,130]}," +
        "{n:'New Zealand',f:'🇳🇿',la:[-47,-34],lo:[166,178]}," +
        "{n:'Colombia',f:'🇨🇴',la:[-4,13],lo:[-79,-67]}," +
        "{n:'Peru',f:'🇵🇪',la:[-18,0],lo:[-82,-68]}," +
        "{n:'Chile',f:'🇨🇱',la:[-56,-17],lo:[-76,-66]}," +
        "{n:'Sweden',f:'🇸🇪',la:[55,70],lo:[10,26]}," +
        "{n:'Norway',f:'🇳🇴',la:[57,71],lo:[4,32]}," +
        "{n:'Poland',f:'🇵🇱',la:[49,55],lo:[14,25]}," +
        "{n:'Netherlands',f:'🇳🇱',la:[50,54],lo:[3,8]}," +
        "{n:'Greece',f:'🇬🇷',la:[35,42],lo:[20,29]}," +
        "{n:'Portugal',f:'🇵🇹',la:[36,42],lo:[-10,-6]}," +
        "{n:'Bangladesh',f:'🇧🇩',la:[20,27],lo:[88,93]}," +
        "{n:'Iraq',f:'🇮🇶',la:[29,38],lo:[38,49]}," +
        "{n:'Ethiopia',f:'🇪🇹',la:[3,15],lo:[33,48]}," +
        "{n:'Singapore',f:'🇸🇬',la:[1.1,1.5],lo:[103,104]}," +
        "{n:'Jordan',f:'🇯🇴',la:[29,33],lo:[35,39]}," +
        "{n:'Kuwait',f:'🇰🇼',la:[28,30],lo:[46,49]}," +
        "{n:'Qatar',f:'🇶🇦',la:[24,26],lo:[50,52]}," +
        "{n:'Bahrain',f:'🇧🇭',la:[25.6,26.4],lo:[50.3,50.9]}," +
        "{n:'Oman',f:'🇴🇲',la:[16,24],lo:[52,60]}," +
        "{n:'Morocco',f:'🇲🇦',la:[27,36],lo:[-14,-1]}," +
        "{n:'Algeria',f:'🇩🇿',la:[19,37],lo:[-8,12]}," +
        "{n:'Tunisia',f:'🇹🇳',la:[30,37],lo:[7,12]}," +
        "{n:'Libya',f:'🇱🇾',la:[19,33],lo:[9,25]}," +
        "{n:'Sudan',f:'🇸🇩',la:[8,23],lo:[21,38]}," +
        "{n:'Ukraine',f:'🇺🇦',la:[44,52],lo:[22,40]}," +
        "{n:'Romania',f:'🇷🇴',la:[43,48],lo:[20,30]}," +
        "{n:'Hungary',f:'🇭🇺',la:[45,49],lo:[16,23]}," +
        "];" +

        // NASA-quality Earth using real tile textures
        "function init(){" +
        "  var wrap=document.getElementById('globe-wrap');" +
        "  var W=wrap.clientWidth, H=wrap.clientHeight;" +
        "  scene=new THREE.Scene();" +
        "  camera=new THREE.PerspectiveCamera(45,W/H,0.1,1000);" +
        "  camera.position.z=zoom;" +
        "  renderer=new THREE.WebGLRenderer({antialias:true,alpha:true});" +
        "  renderer.setSize(W,H);" +
        "  renderer.setPixelRatio(Math.min(devicePixelRatio,2));" +
        "  renderer.domElement.id='globe-canvas';" +
        "  wrap.appendChild(renderer.domElement);" +
        "  raycaster=new THREE.Raycaster();" +

        // Stars
        "  var sv=new THREE.BufferGeometry();" +
        "  var sp=[];" +
        "  for(var i=0;i<8000;i++){" +
        "    var r=80+Math.random()*100,t=Math.random()*Math.PI*2,p=Math.acos(2*Math.random()-1);" +
        "    sp.push(r*Math.sin(p)*Math.cos(t),r*Math.sin(p)*Math.sin(t),r*Math.cos(p));}" +
        "  sv.setAttribute('position',new THREE.Float32BufferAttribute(sp,3));" +
        "  scene.add(new THREE.Points(sv,new THREE.PointsMaterial({color:0x9bbcdd,size:0.2,transparent:true,opacity:0.7})));" +

        // Lighting
        "  scene.add(new THREE.AmbientLight(0x111133,0.5));" +
        "  var sun=new THREE.DirectionalLight(0x7799ff,1.3); sun.position.set(5,3,5); scene.add(sun);" +
        "  var rim=new THREE.DirectionalLight(0x003355,0.5); rim.position.set(-4,-2,-4); scene.add(rim);" +

        // Load NASA earth texture
        "  var loader=new THREE.TextureLoader();" +
        "  loader.crossOrigin='anonymous';" +
        "  var earthTex=loader.load(" +
        "    'https://raw.githubusercontent.com/mrdoob/three.js/dev/examples/textures/planets/earth_atmos_2048.jpg'," +
        "    onEarthLoaded," +
        "    undefined," +
        "    function(){buildCanvasEarth();}" + // fallback if NASA texture fails
        "  );" +
        "  function onEarthLoaded(tex){" +
        "    buildGlobe(tex);" +
        "    document.getElementById('globe-loading').style.display='none';" +
        "    animate();" +
        "  }" +
        "}" +

        "function buildGlobe(earthTex){" +
        "  var geo=new THREE.SphereGeometry(1,72,72);" +
        "  var mat=new THREE.MeshPhongMaterial({map:earthTex,specular:new THREE.Color(0x112244),shininess:14});" +
        "  globe=new THREE.Mesh(geo,mat); scene.add(globe);" +

        // Cloud layer using canvas
        "  var cv=document.createElement('canvas'); cv.width=1024; cv.height=512;" +
        "  var cx2=cv.getContext('2d');" +
        "  for(var i=0;i<200;i++){" +
        "    var x=Math.random()*1024,y=0.08*512+Math.random()*0.84*512,r=15+Math.random()*50;" +
        "    var g=cx2.createRadialGradient(x,y,0,x,y,r);" +
        "    g.addColorStop(0,'rgba(255,255,255,'+(0.12+Math.random()*0.18)+')');" +
        "    g.addColorStop(1,'rgba(255,255,255,0)');" +
        "    cx2.beginPath(); cx2.arc(x,y,r,0,Math.PI*2); cx2.fillStyle=g; cx2.fill();" +
        "  }" +
        "  var cTex=new THREE.CanvasTexture(cv);" +
        "  clouds=new THREE.Mesh(new THREE.SphereGeometry(1.009,72,72)," +
        "    new THREE.MeshPhongMaterial({map:cTex,transparent:true,opacity:0.35,depthWrite:false}));" +
        "  scene.add(clouds);" +

        // Atmosphere glow
        "  scene.add(new THREE.Mesh(new THREE.SphereGeometry(1.04,72,72)," +
        "    new THREE.MeshPhongMaterial({color:0x0033aa,transparent:true,opacity:0.10,side:THREE.FrontSide,depthWrite:false})));" +
        "  scene.add(new THREE.Mesh(new THREE.SphereGeometry(1.07,72,72)," +
        "    new THREE.MeshPhongMaterial({color:0x0066ff,transparent:true,opacity:0.04,side:THREE.BackSide,depthWrite:false})));" +

        // HUD rings
        "  [[1.18,0.3],[1.26,0.15],[1.35,0.08]].forEach(function(r){" +
        "    var geo2=new THREE.BufferGeometry(); var pts=[];" +
        "    for(var i=0;i<=256;i++){var a=i/256*Math.PI*2; pts.push(Math.cos(a)*r[0],0,Math.sin(a)*r[0]);}" +
        "    geo2.setAttribute('position',new THREE.Float32BufferAttribute(pts,3));" +
        "    var ring=new THREE.Line(geo2,new THREE.LineBasicMaterial({color:0x00aaff,opacity:r[1],transparent:true}));" +
        "    ring.rotation.x=0.3; scene.add(ring);" +
        "  });" +

        "  flyToLatLon(25.2,55.3);" + // Start at UAE
        "}" +

        // Canvas earth fallback (offline)
        "function buildCanvasEarth(){" +
        "  var W=2048,H=1024,cv=document.createElement('canvas'); cv.width=W; cv.height=H;" +
        "  var c=cv.getContext('2d');" +
        "  var og=c.createLinearGradient(0,0,0,H);" +
        "  og.addColorStop(0,'#071428'); og.addColorStop(0.5,'#0d2248'); og.addColorStop(1,'#071428');" +
        "  c.fillStyle=og; c.fillRect(0,0,W,H);" +
        "  var lands=[" +
        "    {pts:[[0.078,0.23],[0.19,0.18],[0.23,0.24],[0.19,0.33],[0.10,0.48],[0.08,0.45],[0.078,0.23]],col:'#2d5e18'}," +
        "    {pts:[[0.13,0.5],[0.22,0.52],[0.22,0.62],[0.14,0.76],[0.12,0.74],[0.11,0.65],[0.13,0.5]],col:'#265518'}," +
        "    {pts:[[0.47,0.21],[0.57,0.22],[0.58,0.26],[0.52,0.3],[0.47,0.25],[0.47,0.21]],col:'#3d6820'}," +
        "    {pts:[[0.48,0.32],[0.60,0.30],[0.60,0.45],[0.54,0.56],[0.48,0.52],[0.46,0.43],[0.48,0.32]],col:'#7a5f1c'}," +
        "    {pts:[[0.52,0.14],[0.82,0.16],[0.84,0.21],[0.78,0.26],[0.56,0.26],[0.52,0.14]],col:'#345e18'}," +
        "    {pts:[[0.59,0.28],[0.66,0.36],[0.62,0.36],[0.59,0.28]],col:'#a08830'}," +
        "    {pts:[[0.68,0.28],[0.78,0.34],[0.72,0.44],[0.66,0.34],[0.68,0.28]],col:'#3d6820'}," +
        "    {pts:[[0.77,0.34],[0.88,0.36],[0.82,0.45],[0.77,0.38],[0.77,0.34]],col:'#2e5e18'}," +
        "    {pts:[[0.78,0.24],[0.92,0.24],[0.90,0.34],[0.77,0.30],[0.78,0.24]],col:'#355e18'}," +
        "    {pts:[[0.82,0.58],[0.94,0.60],[0.94,0.68],[0.84,0.72],[0.80,0.68],[0.82,0.58]],col:'#907820'}," +
        "    {pts:[[0.0,0.90],[1.0,0.90],[1.0,1.0],[0.0,1.0]],col:'#c0d8ec'}," +
        "    {pts:[[0.20,0.10],[0.28,0.13],[0.22,0.18],[0.20,0.10]],col:'#b8d0e4'}" +
        "  ];" +
        "  lands.forEach(function(l){" +
        "    c.beginPath(); c.moveTo(l.pts[0][0]*W,l.pts[0][1]*H);" +
        "    for(var i=1;i<l.pts.length;i++) c.lineTo(l.pts[i][0]*W,l.pts[i][1]*H);" +
        "    c.closePath(); c.fillStyle=l.col; c.fill();" +
        "    c.strokeStyle='rgba(180,220,100,0.2)'; c.lineWidth=1.5; c.stroke();" +
        "  });" +
        "  c.strokeStyle='rgba(0,180,255,0.04)'; c.lineWidth=1;" +
        "  for(var x=0;x<W;x+=W/24){c.beginPath();c.moveTo(x,0);c.lineTo(x,H);c.stroke();}" +
        "  for(var y=0;y<H;y+=H/12){c.beginPath();c.moveTo(0,y);c.lineTo(W,y);c.stroke();}" +
        "  buildGlobe(new THREE.CanvasTexture(cv));" +
        "  document.getElementById('globe-loading').style.display='none';" +
        "  animate();" +
        "}" +

        // Fly to lat/lon
        "function flyToLatLon(lat,lon){" +
        "  var tx=-lon*Math.PI/180, ty=lat*0.55*Math.PI/180;" +
        "  var sx=globe.rotation.x, sy=globe.rotation.y, t0=performance.now();" +
        "  function step(now){" +
        "    var p=Math.min((now-t0)/1400,1), e=1-Math.pow(1-p,3);" +
        "    globe.rotation.x=sx+(ty-sx)*e; globe.rotation.y=sy+(tx-sy)*e;" +
        "    if(clouds){clouds.rotation.x=globe.rotation.x;clouds.rotation.y=globe.rotation.y+0.02;}" +
        "    rotX=globe.rotation.x; rotY=globe.rotation.y;" +
        "    if(p<1) requestAnimationFrame(step);" +
        "  } requestAnimationFrame(step);" +
        "}" +

        // Fly to named country
        "window.flyToCountry=function(name){" +
        "  var LOCS={" +
        "    'Philippines':[12.8,121.8],'Japan':[36.2,138.3],'China':[35.9,104.2]," +
        "    'India':[20.6,78.9],'Australia':[-25.3,133.8],'Russia':[61.5,105.3]," +
        "    'United States':[37.1,-95.7],'Canada':[56.1,-106.3],'Brazil':[-14.2,-51.9]," +
        "    'United Kingdom':[55.4,-3.4],'France':[46.2,2.2],'Germany':[51.2,10.5]," +
        "    'UAE':[23.4,53.8],'Saudi Arabia':[23.9,45.1],'Egypt':[26.8,30.8]," +
        "    'Japan':[36.2,138.3],'South Korea':[35.9,127.8],'Indonesia':[-0.8,113.9]," +
        "    'Thailand':[15.9,100.9],'Vietnam':[14.1,108.3],'Malaysia':[4.2,101.9]," +
        "    'Singapore':[1.35,103.8],'Nigeria':[9.1,8.7],'Kenya':[-0.0,38.0]," +
        "    'South Africa':[-30.6,22.9],'Turkey':[39.0,35.2],'Iran':[32.4,53.7]," +
        "    'Pakistan':[30.4,69.3],'Argentina':[-38.4,-63.6],'Mexico':[23.6,-102.6]," +
        "    'Spain':[40.5,-3.7],'Italy':[41.9,12.6],'Portugal':[39.4,-8.2]," +
        "    'Netherlands':[52.1,5.3],'Sweden':[60.1,18.6],'Norway':[60.5,8.5]," +
        "    'Poland':[51.9,19.1],'Greece':[39.1,21.8],'Morocco':[31.8,-7.1]," +
        "  };" +
        "  var k=Object.keys(LOCS).find(function(k){return k.toLowerCase()===name.toLowerCase();});" +
        "  if(k){flyToLatLon(LOCS[k][0],LOCS[k][1]); loadCountryInfo(k);}"+
        "  else loadCountryInfo(name);" +
        "};" +

        // Click/tap detection
        "function onTap(clientX,clientY){" +
        "  var rect=renderer.domElement.getBoundingClientRect();" +
        "  var nx=((clientX-rect.left)/rect.width)*2-1;" +
        "  var ny=-((clientY-rect.top)/rect.height)*2+1;" +
        "  raycaster.setFromCamera({x:nx,y:ny},camera);" +
        "  var hits=raycaster.intersectObject(globe);" +
        "  if(!hits.length) return;" +
        "  var uv=hits[0].uv;" +
        "  var lon=(uv.x-0.5)*360, lat=(uv.y-0.5)*180;" +
        "  try{ HenryGlobe.fetchCountryByLatLon(lat, lon); }" +
        "  catch(e){" +
        "    var found=null;" +
        "    REGIONS.forEach(function(r){" +
        "      if(lat>=r.la[0]&&lat<=r.la[1]&&lon>=r.lo[0]&&lon<=r.lo[1]) found=r;" +
        "    });" +
        "    if(found){selectedCountry=found; loadCountryInfo(found.n);}" +
        "    else if(lat>65) loadCountryInfo('Arctic Ocean');" +
        "    else if(lat<-60) loadCountryInfo('Antarctica');" +
        "  }" +
        "}" +

        // Load country info via Android bridge
        "function loadCountryInfo(name){" +
        "  selectedCountry=selectedCountry||{n:name,f:'🌍'};" +
        "  document.getElementById('panel-idle').style.display='none';" +
        "  document.getElementById('panel-content').style.display='none';" +
        "  document.getElementById('panel-loading').style.display='block';" +
        "  var prompt='Give a concise briefing on '+name+': flag emoji, capital, population estimate, " +
        "history (2 sentences), culture highlights (2 sentences), top 3 tourist spots, top 3 traditional foods, economy (1 sentence). Use headers: HISTORY, CULTURE, TOURISM, FOOD, ECONOMY.';" +
        "  try{ HenryGlobe.fetchCountryInfo(name, prompt); }" +
        "  catch(e){ showCountryInfo('Unable to load intel: '+e.message); }" +
        "}" +

        // Called from Android bridge
        "window.showCountryInfo=function(text){" +
        "  document.getElementById('panel-loading').style.display='none';" +
        "  var pc=document.getElementById('panel-content');" +
        "  var name=selectedCountry?selectedCountry.n:'Unknown';" +
        "  var flag=selectedCountry?selectedCountry.f:'🌍';" +
        "  var formatted=text.replace(/HISTORY/g,'<b style=\"color:#00D4FF\">▶ HISTORY</b>')" +
        "    .replace(/CULTURE/g,'<b style=\"color:#00D4FF\">▶ CULTURE</b>')" +
        "    .replace(/TOURISM/g,'<b style=\"color:#00D4FF\">▶ TOURISM</b>')" +
        "    .replace(/FOOD/g,'<b style=\"color:#00D4FF\">▶ FOOD</b>')" +
        "    .replace(/ECONOMY/g,'<b style=\"color:#00D4FF\">▶ ECONOMY</b>')" +
        "    .replace(/\\\\n/g,'<br>');" +
        "  pc.innerHTML=" +
        "    '<div class=\"c-header\"><div class=\"c-flag\">'+flag+'</div><div class=\"c-name\">'+name.toUpperCase()+'</div></div>'" +
        "    +'<div class=\"c-body\">'+formatted+'</div>'" +
        "    +'<button class=\"c-ask-btn\" onclick=\"sendToChat()\">💬 Ask HENRY about '+name+'</button>';" +
        "  pc.style.display='block';" +
        "  try{ HenryGlobe.onCountrySelected(name); } catch(e){}" +
        "};" +

        "function sendToChat(){" +
        "  if(selectedCountry) try{ HenryGlobe.askHenryAbout(selectedCountry.n); }catch(e){}" +
        "}" +

        // Search
        "document.getElementById('search-btn').addEventListener('click',function(){" +
        "  var q=document.getElementById('search-input').value.trim();" +
        "  if(q) flyToCountry(q);" +
        "  document.getElementById('search-input').value='';" +
        "});" +
        "document.getElementById('search-input').addEventListener('keydown',function(e){" +
        "  if(e.key==='Enter'){document.getElementById('search-btn').click();}" +
        "});" +
        "document.getElementById('close-btn').addEventListener('click',function(){" +
        "  try{HenryGlobe.askHenryAbout('');}catch(e){} history.back();" +
        "});" +
        "document.getElementById('ask-btn').addEventListener('click',function(){" +
        "  var q=document.getElementById('ask-input').value.trim();" +
        "  if(!q||!selectedCountry) return;" +
        "  document.getElementById('ask-input').value='';" +
        "  var prompt='About '+selectedCountry.n+': '+q+' Answer in 3-4 concise sentences.';" +
        "  document.getElementById('panel-loading').style.display='block';" +
        "  document.getElementById('panel-content').style.display='none';" +
        "  try{ HenryGlobe.fetchCountryInfo(selectedCountry.n, prompt); }catch(e){}" +
        "});" +

        // Drag / pinch
        "function addGlobeEvents(){" +
        "  var el=renderer.domElement;" +
        "  var tapTime=0,tapX=0,tapY=0;" +
        "  el.addEventListener('mousedown',function(e){isDragging=false;prevX=e.clientX;prevY=e.clientY;});" +
        "  el.addEventListener('mousemove',function(e){" +
        "    if(!(e.buttons&1)) return; isDragging=true;" +
        "    rotVY+=(e.clientX-prevX)*0.006; rotVX+=(e.clientY-prevY)*0.006;" +
        "    prevX=e.clientX; prevY=e.clientY;});" +
        "  el.addEventListener('mouseup',function(e){if(!isDragging) onTap(e.clientX,e.clientY); isDragging=false;});" +
        "  el.addEventListener('wheel',function(e){zoom=Math.max(1.4,Math.min(5,zoom+e.deltaY*0.003));},{passive:true});" +
        "  el.addEventListener('touchstart',function(e){" +
        "    if(e.touches.length===1){isDragging=false;prevX=e.touches[0].clientX;prevY=e.touches[0].clientY;" +
        "      tapTime=Date.now();tapX=prevX;tapY=prevY;}" +
        "    if(e.touches.length===2){touchDist=Math.hypot(e.touches[0].clientX-e.touches[1].clientX,e.touches[0].clientY-e.touches[1].clientY);}"+
        "  },{passive:true});" +
        "  el.addEventListener('touchmove',function(e){" +
        "    if(e.touches.length===1){" +
        "      isDragging=true;" +
        "      rotVY+=(e.touches[0].clientX-prevX)*0.007; rotVX+=(e.touches[0].clientY-prevY)*0.007;" +
        "      prevX=e.touches[0].clientX; prevY=e.touches[0].clientY; e.preventDefault();}" +
        "    if(e.touches.length===2&&touchDist){" +
        "      var d=Math.hypot(e.touches[0].clientX-e.touches[1].clientX,e.touches[0].clientY-e.touches[1].clientY);" +
        "      zoom=Math.max(1.4,Math.min(5,zoom*(touchDist/d))); touchDist=d; e.preventDefault();}" +
        "  },{passive:false});" +
        "  el.addEventListener('touchend',function(e){" +
        "    if(!isDragging&&Date.now()-tapTime<300){" +
        "      var ct=e.changedTouches[0]; onTap(ct.clientX,ct.clientY);}" +
        "    isDragging=false; touchDist=null;" +
        "  });" +
        "  window.addEventListener('resize',function(){" +
        "    var w=document.getElementById('globe-wrap');" +
        "    camera.aspect=w.clientWidth/w.clientHeight; camera.updateProjectionMatrix();" +
        "    renderer.setSize(w.clientWidth,w.clientHeight);});" +
        "}" +

        // Render loop
        "function animate(){" +
        "  requestAnimationFrame(animate);" +
        "  rotX+=rotVX; rotY+=rotVY;" +
        "  rotVX*=0.88; rotVY*=0.88;" +
        "  if(Math.abs(rotVX)<0.0005&&Math.abs(rotVY)<0.0005) rotY+=0.0007;" +
        "  globe.rotation.x=rotX; globe.rotation.y=rotY;" +
        "  if(clouds){clouds.rotation.x=rotX*0.97;clouds.rotation.y=rotY+performance.now()*0.00004;}" +
        "  camera.position.z=zoom;" +
        "  renderer.render(scene,camera);" +
        "}" +

        "init(); addGlobeEvents();";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}
