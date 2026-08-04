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
    public  static final int REQUEST_CODE  = 301;
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

        int matchParent = LinearLayout.LayoutParams.MATCH_PARENT;
        int wrapContent = LinearLayout.LayoutParams.WRAP_CONTENT;
        float density   = getResources().getDisplayMetrics().density;

        // Root
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF020C1B);
        setContentView(root);

        // ── Top bar ──────────────────────────────────────────────────────────
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(0xFF020C1B);
        top.setPadding(dp(16,density), dp(44,density), dp(16,density), dp(12,density));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🐾  HENRY ANIMAL SCANNER");
        tvTitle.setTextColor(0xFF00FF99);
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, wrapContent, 1f);
        top.addView(tvTitle, titleLp);

        Button btnClose = new Button(this);
        btnClose.setText("✕  CLOSE");
        btnClose.setTextColor(0xFF00D4FF);
        btnClose.setBackgroundColor(0xFF051828);
        btnClose.setTextSize(11f);
        btnClose.setAllCaps(false);
        btnClose.setOnClickListener(v -> finish());
        top.addView(btnClose);
        root.addView(top);

        // Divider
        View div1 = new View(this);
        div1.setBackgroundColor(0xFF0A2040);
        root.addView(div1, new LinearLayout.LayoutParams(matchParent, 1));

        // ── Scrollable body ──────────────────────────────────────────────────
        ScrollView sv = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16,density), dp(16,density), dp(16,density), dp(32,density));

        tvHint = new TextView(this);
        tvHint.setText("Snap a photo or choose from gallery.\nHENRY identifies the animal, its habitat, diet & more.");
        tvHint.setTextColor(0xFF3A7AA0);
        tvHint.setTextSize(13.5f);
        tvHint.setLineSpacing(dp(4,density), 1f);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(matchParent, wrapContent);
        hintLp.bottomMargin = dp(14,density);
        body.addView(tvHint, hintLp);

        // Preview
        ivPreview = new ImageView(this);
        ivPreview.setBackgroundColor(0xFF051828);
        ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivPreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(matchParent, dp(220,density));
        prevLp.bottomMargin = dp(14,density);
        body.addView(ivPreview, prevLp);

        // Camera / Gallery row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnCamera  = new Button(this);
        btnCamera.setText("📷  CAMERA");
        btnCamera.setTextColor(0xFF00D4FF);
        btnCamera.setBackgroundColor(0xFF051828);
        btnCamera.setTextSize(12f);
        btnCamera.setAllCaps(false);
        btnGallery = new Button(this);
        btnGallery.setText("🖼  GALLERY");
        btnGallery.setTextColor(0xFF00D4FF);
        btnGallery.setBackgroundColor(0xFF051828);
        btnGallery.setTextSize(12f);
        btnGallery.setAllCaps(false);
        LinearLayout.LayoutParams camLp = new LinearLayout.LayoutParams(0, dp(46,density), 1f);
        camLp.rightMargin = dp(8,density);
        LinearLayout.LayoutParams galLp = new LinearLayout.LayoutParams(0, dp(46,density), 1f);
        btnRow.addView(btnCamera, camLp);
        btnRow.addView(btnGallery, galLp);
        body.addView(btnRow, new LinearLayout.LayoutParams(matchParent, wrapContent));

        // Scan button
        btnScan = new Button(this);
        btnScan.setText("🔬  IDENTIFY ANIMAL");
        btnScan.setTextColor(0xFF000000);
        btnScan.setBackgroundColor(0xFF00FF99);
        btnScan.setTextSize(13f);
        btnScan.setAllCaps(false);
        btnScan.setEnabled(false);
        btnScan.setAlpha(0.35f);
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(matchParent, dp(52,density));
        scanLp.topMargin = dp(12,density);
        body.addView(btnScan, scanLp);

        // Progress bar
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(matchParent, wrapContent);
        pbLp.topMargin = dp(10,density);
        body.addView(progressBar, pbLp);

        // Result divider
        View resDivider = new View(this);
        resDivider.setBackgroundColor(0xFF0A2040);
        LinearLayout.LayoutParams rdLp = new LinearLayout.LayoutParams(matchParent, 1);
        rdLp.topMargin = dp(20,density);
        rdLp.bottomMargin = dp(14,density);
        body.addView(resDivider, rdLp);

        // Result block
        layoutResult = new LinearLayout(this);
        layoutResult.setOrientation(LinearLayout.VERTICAL);
        layoutResult.setVisibility(View.GONE);
        layoutResult.setBackgroundColor(0xFF030F22);
        layoutResult.setPadding(dp(14,density), dp(14,density), dp(14,density), dp(14,density));

        tvResultTitle = new TextView(this);
        tvResultTitle.setTextColor(0xFF00FF99);
        tvResultTitle.setTextSize(16f);
        tvResultTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams rtLp = new LinearLayout.LayoutParams(matchParent, wrapContent);
        rtLp.bottomMargin = dp(8,density);
        layoutResult.addView(tvResultTitle, rtLp);

        tvResultBody = new TextView(this);
        tvResultBody.setTextColor(0xFFC8E8F8);
        tvResultBody.setTextSize(14f);
        tvResultBody.setLineSpacing(dp(5,density), 1f);
        layoutResult.addView(tvResultBody, new LinearLayout.LayoutParams(matchParent, wrapContent));

        btnSendChat = new Button(this);
        btnSendChat.setText("💬  SEND TO HENRY CHAT");
        btnSendChat.setTextColor(0xFF00D4FF);
        btnSendChat.setBackgroundColor(0xFF051828);
        btnSendChat.setTextSize(13f);
        btnSendChat.setAllCaps(false);
        btnSendChat.setVisibility(View.GONE);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(matchParent, dp(48,density));
        scLp.topMargin = dp(14,density);
        layoutResult.addView(btnSendChat, scLp);

        body.addView(layoutResult, new LinearLayout.LayoutParams(matchParent, wrapContent));
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(matchParent, 0, 1f));

        // ── Listeners ────────────────────────────────────────────────────────
        btnCamera.setOnClickListener(v -> doCamera());
        btnGallery.setOnClickListener(v -> doGallery());
        btnScan.setOnClickListener(v -> doScan());
        btnSendChat.setOnClickListener(v -> sendToChat());
    }

    // ── Camera ───────────────────────────────────────────────────────────────
    private void doCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_PERM);
            return;
        }
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File dir  = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File file = File.createTempFile("ANIMAL_" + stamp, ".jpg", dir);
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
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
                btnScan.setEnabled(true);
                btnScan.setAlpha(1f);
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
        if (req == REQ_PERM && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED)
            doCamera();
    }

    // ── Scan ─────────────────────────────────────────────────────────────────
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
                Bitmap scaled = scaleBitmap(pickedBitmap, 768);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 72, baos);
                String b64 = android.util.Base64.encodeToString(
                        baos.toByteArray(), android.util.Base64.NO_WRAP);

                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("text", "What animal is in this photo? Provide: "
                        + "1) Common name + scientific name, "
                        + "2) Where it lives (continents/regions/habitats), "
                        + "3) Diet and behaviour, "
                        + "4) Conservation status, "
                        + "5) Size and weight, "
                        + "6) Three amazing fun facts. "
                        + "Write like a nature documentary narrator — engaging and vivid.");
                msgs.put(msg);

                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("imageBase64", b64);
                body.put("overrideSystem", "You are an expert zoologist and wildlife biologist. When shown an animal photo, identify it in detail. Describe: common name, scientific name, habitat, diet, conservation status, size, and 3 fun facts. Write like a nature documentary narrator. Never talk about anything other than the animal in the photo.");

                Request request = new Request.Builder()
                        .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                        .post(RequestBody.create(body.toString(),
                                MediaType.parse("application/json")))
                        .build();
                Response response = http.newCall(request).execute();
                String raw = response.body() != null ? response.body().string() : "{}";
                JSONObject json = new JSONObject(raw);
                String reply = json.optString("reply",
                        "Could not identify this animal. Please try a clearer photo, sir.");
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
        tvHint.setText("Identified! Tap below to continue in HENRY Chat.");
        layoutResult.setVisibility(View.VISIBLE);
        btnSendChat.setVisibility(View.VISIBLE);
    }

    private void sendToChat() {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT, lastResult);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Bitmap scaleBitmap(Bitmap src, int maxPx) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxPx && h <= maxPx) return src;
        float ratio = Math.min((float) maxPx / w, (float) maxPx / h);
        return Bitmap.createScaledBitmap(src,
                Math.round(w * ratio), Math.round(h * ratio), true);
    }

    private int dp(int val, float density) {
        return Math.round(val * density);
    }
}
