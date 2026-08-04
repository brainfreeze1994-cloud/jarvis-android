package com.jarvis.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AnimalScannerActivity extends AppCompatActivity {

    private static final int REQ_CAMERA  = 101;
    private static final int REQ_GALLERY = 102;
    private static final int REQ_PERM    = 201;
    public  static final int REQUEST_CODE = 301;
    public  static final String EXTRA_RESULT = "animal_result";

    private ImageView   ivPreview;
    private Button      btnCamera, btnGallery, btnScan, btnSendChat;
    private ProgressBar progressBar;
    private TextView    tvHint, tvResultTitle, tvResultBody;
    private LinearLayout layoutResult;

    private Uri    cameraUri;
    private Bitmap pickedBitmap;
    private String lastResult = "";

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Full programmatic UI ──────────────────────────────────────────
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF020C1B);
        setContentView(root);

        // Top bar
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(0xFF020C1B);
        top.setPadding(px(16), px(44), px(16), px(12));

        TextView title = tv("🐾  HENRY ANIMAL SCANNER", 0xFF00FF99, 15f, true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LP.WRAP_CONTENT, 1f));
        Button btnClose = btn("✕ CLOSE", 0xFF051828, 0xFF00D4FF);
        btnClose.setOnClickListener(v -> finish());
        top.addView(title); top.addView(btnClose);
        root.addView(top);

        root.addView(divider());

        // Scroll body
        ScrollView sv = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(px(16), px(16), px(16), px(32));

        tvHint = tv("Snap a photo or choose from gallery.\nHENRY identifies the animal, its habitat, diet & more.", 0xFF3A7AA0, 13.5f, false);
        tvHint.setLineSpacing(px(4), 1f);
        LP hp = new LP(LP.MATCH_PARENT, LP.WRAP_CONTENT); hp.bottomMargin = px(14);
        body.addView(tvHint, hp);

        // Image preview
        ivPreview = new ImageView(this);
        ivPreview.setBackgroundColor(0xFF051828);
        ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivPreview.setVisibility(View.GONE);
        LP ip = new LP(LP.MATCH_PARENT, px(220)); ip.bottomMargin = px(14);
        body.addView(ivPreview, ip);

        // Camera / Gallery row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnCamera  = btn("📷  CAMERA",  0xFF051828, 0xFF00D4FF);
        btnGallery = btn("🖼  GALLERY", 0xFF051828, 0xFF00D4FF);
        LP bp1 = new LP(0, px(46), 1f); bp1.rightMargin = px(8);
        LP bp2 = new LP(0, px(46), 1f);
        btnRow.addView(btnCamera, bp1);
        btnRow.addView(btnGallery, bp2);
        body.addView(btnRow, new LP(LP.MATCH_PARENT, LP.WRAP_CONTENT));

        // Scan button
        btnScan = btn("🔬  IDENTIFY ANIMAL", 0xFF00FF99, 0xFF000000);
        btnScan.setEnabled(false); btnScan.setAlpha(0.35f);
        LP sp = new LP(LP.MATCH_PARENT, px(52)); sp.topMargin = px(12);
        body.addView(btnScan, sp);

        // Progress
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        LP pp = new LP(LP.MATCH_PARENT, LP.WRAP_CONTENT); pp.topMargin = px(10);
        body.addView(progressBar, pp);

        // Divider
        View rd = divider(); rd.setVisibility(View.GONE);
        LP rp = new LP(LP.MATCH_PARENT, px(1)); rp.topMargin = px(20); rp.bottomMargin = px(14);
        body.addView(rd, rp);

        // Result block
        layoutResult = new LinearLayout(this);
        layoutResult.setOrientation(LinearLayout.VERTICAL);
        layoutResult.setVisibility(View.GONE);
        layoutResult.setBackgroundColor(0xFF030F22);
        layoutResult.setPadding(px(14), px(14), px(14), px(14));

        tvResultTitle = tv("", 0xFF00FF99, 16f, true);
        tvResultTitle.setPadding(0, 0, 0, px(8));

        tvResultBody = tv("", 0xFFC8E8F8, 14f, false);
        tvResultBody.setLineSpacing(px(5), 1f);

        btnSendChat = btn("💬  SEND TO HENRY CHAT", 0xFF051828, 0xFF00D4FF);
        btnSendChat.setVisibility(View.GONE);
        LP scp = new LP(LP.MATCH_PARENT, px(48)); scp.topMargin = px(14);

        layoutResult.addView(tvResultTitle);
        layoutResult.addView(tvResultBody);
        layoutResult.addView(btnSendChat, scp);
        body.addView(rd);
        body.addView(layoutResult, new LP(LP.MATCH_PARENT, LP.WRAP_CONTENT));

        sv.addView(body);
        root.addView(sv, new LP(LP.MATCH_PARENT, 0, 1f));

        // ── Listeners ─────────────────────────────────────────────────────
        btnCamera.setOnClickListener(v -> doCamera());
        btnGallery.setOnClickListener(v -> doGallery());
        btnScan.setOnClickListener(v -> doScan());
        btnSendChat.setOnClickListener(v -> sendToChat());
        rd.setVisibility(View.VISIBLE); // always show divider
    }

    // ── Camera ────────────────────────────────────────────────────────────
    private void doCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_PERM);
            return;
        }
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File dir  = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File file = File.createTempFile("ANIMAL_" + stamp, ".jpg", dir);
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            startActivityForResult(i, REQ_CAMERA);
        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void doGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        startActivityForResult(i, REQ_GALLERY);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != Activity.RESULT_OK) return;
        try {
            if (req == REQ_CAMERA) {
                InputStream is = getContentResolver().openInputStream(cameraUri);
                pickedBitmap = BitmapFactory.decodeStream(is);
            } else if (req == REQ_GALLERY && data != null) {
                InputStream is = getContentResolver().openInputStream(data.getData());
                pickedBitmap = BitmapFactory.decodeStream(is);
            }
            if (pickedBitmap != null) {
                ivPreview.setImageBitmap(pickedBitmap);
                ivPreview.setVisibility(View.VISIBLE);
                btnScan.setEnabled(true); btnScan.setAlpha(1f);
                layoutResult.setVisibility(View.GONE);
                btnSendChat.setVisibility(View.GONE);
                tvHint.setText("Photo loaded. Tap IDENTIFY ANIMAL to scan.");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not load image.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] g) {
        super.onRequestPermissionsResult(req, p, g);
        if (req == REQ_PERM && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) doCamera();
    }

    // ── Scan ──────────────────────────────────────────────────────────────
    private void doScan() {
        if (pickedBitmap == null) return;
        btnScan.setEnabled(false);
        btnScan.setText("⏳  ANALYZING…");
        progressBar.setVisibility(View.VISIBLE);
        tvHint.setText("HENRY vision engine is analyzing the photo…");
        layoutResult.setVisibility(View.GONE);
        btnSendChat.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                Bitmap scaled = scaleBm(pickedBitmap, 768);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 72, baos);
                String b64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);

                JSONArray msgs = new JSONArray();
                JSONObject m = new JSONObject();
                m.put("role", "user");
                m.put("text", "What animal is in this photo? Provide: 1) Common name + scientific name, 2) Where it lives (continent/region/habitat), 3) Diet and behavior, 4) Conservation status, 5) Size and weight, 6) Three amazing fun facts. Write like a nature documentary narrator — engaging and vivid.");
                msgs.put(m);

                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("imageBase64", b64);

                Request req = new Request.Builder()
                        .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .build();
                Response resp = http.newCall(req).execute();
                String raw = resp.body() != null ? resp.body().string() : "{}";
                JSONObject json = new JSONObject(raw);
                String reply = json.optString("reply", "Could not identify this animal. Please try a clearer photo, sir.");
                reply = reply.replaceAll("\\[EMOTION:[^\\]]+\\]", "").trim();

                String finalReply = reply;
                runOnUiThread(() -> showResult(finalReply));
            } catch (Exception e) {
                runOnUiThread(() -> showResult("Scanner error: " + e.getMessage()));
            }
        }).start();
    }

    private void showResult(String text) {
        progressBar.setVisibility(View.GONE);
        btnScan.setEnabled(true);
        btnScan.setText("🔬  SCAN AGAIN");
        lastResult = text;

        String[] lines = text.split("\n");
        String titleLine = lines[0].replaceAll("[#*_]", "").trim();
        String bodyText  = text.length() > titleLine.length()
                ? text.substring(titleLine.length()).trim()
                        .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                        .replaceAll("#+\\s*", "• ")
                        .replaceAll("\\*", "")
                : text;

        tvResultTitle.setText("🐾  " + titleLine);
        tvResultBody.setText(bodyText);
        tvHint.setText("Identified! Tap below to send to HENRY Chat for follow-up questions.");
        layoutResult.setVisibility(View.VISIBLE);
        btnSendChat.setVisibility(View.VISIBLE);
    }

    private void sendToChat() {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT, lastResult);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Bitmap scaleBm(Bitmap src, int max) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= max && h <= max) return src;
        float r = Math.min((float) max / w, (float) max / h);
        return Bitmap.createScaledBitmap(src, Math.round(w * r), Math.round(h * r), true);
    }

    private int px(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    private TextView tv(String text, int color, float sizeSp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextColor(color); t.setTextSize(sizeSp);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button btn(String label, int bg, int fg) {
        Button b = new Button(this);
        b.setText(label); b.setBackgroundColor(bg); b.setTextColor(fg);
        b.setTextSize(13f); b.setPadding(px(12), px(8), px(12), px(8));
        b.setAllCaps(false);
        return b;
    }

    private View divider() {
        View v = new View(this); v.setBackgroundColor(0xFF0A2040); return v;
    }

    private static class LP extends LinearLayout.LayoutParams {
        LP(int w, int h) { super(w, h); }
        LP(int w, int h, float weight) { super(w, h, weight); }
    }
}
