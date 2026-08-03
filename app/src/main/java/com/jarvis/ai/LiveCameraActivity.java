package com.jarvis.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Live camera view — tap "Analyse" to capture frame and send to HENRY backend.
 * Result is returned to MainActivity via setResult().
 */
public class LiveCameraActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT      = "analysis_result";
    public static final String EXTRA_QUESTION    = "question";
    private static final int   PERM_CAMERA       = 301;

    private PreviewView    previewView;
    private TextView       tvResult;
    private Button         btnAnalyse, btnClose;
    private ImageCapture   imageCapture;
    private ExecutorService cameraExecutor;
    private OkHttpClient   httpClient;
    private String         userQuestion = "What do you see in this image? Describe in detail.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build a simple layout programmatically
        android.widget.RelativeLayout root = new android.widget.RelativeLayout(this);
        root.setBackgroundColor(0xFF0d0d0d);

        previewView = new PreviewView(this);
        previewView.setId(View.generateViewId());
        android.widget.RelativeLayout.LayoutParams pvlp =
            new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
        previewView.setLayoutParams(pvlp);
        root.addView(previewView);

        // Result overlay
        tvResult = new TextView(this);
        tvResult.setId(View.generateViewId());
        tvResult.setText("Point camera at something, then tap Analyse.");
        tvResult.setTextColor(0xFFc9a84c);
        tvResult.setBackgroundColor(0xCC000000);
        tvResult.setPadding(24, 16, 24, 16);
        tvResult.setTextSize(13f);
        android.widget.RelativeLayout.LayoutParams tvlp =
            new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
        tvlp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
        tvResult.setLayoutParams(tvlp);
        root.addView(tvResult);

        // Analyse button
        btnAnalyse = new Button(this);
        btnAnalyse.setText("◆ ANALYSE");
        btnAnalyse.setBackgroundColor(0xFFc9a84c);
        btnAnalyse.setTextColor(0xFF0d0d0d);
        android.widget.RelativeLayout.LayoutParams anlp =
            new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
        anlp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
        anlp.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
        anlp.bottomMargin = 80;
        btnAnalyse.setLayoutParams(anlp);
        btnAnalyse.setOnClickListener(v -> captureAndAnalyse());
        root.addView(btnAnalyse);

        // Close button
        btnClose = new Button(this);
        btnClose.setText("✕");
        btnClose.setBackgroundColor(0xCC333333);
        btnClose.setTextColor(0xFFc9a84c);
        android.widget.RelativeLayout.LayoutParams clp =
            new android.widget.RelativeLayout.LayoutParams(120, 120);
        clp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
        clp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        clp.bottomMargin = 80; clp.rightMargin = 40;
        btnClose.setLayoutParams(clp);
        btnClose.setOnClickListener(v -> finish());
        root.addView(btnClose);

        setContentView(root);

        if (getIntent() != null && getIntent().getStringExtra(EXTRA_QUESTION) != null)
            userQuestion = getIntent().getStringExtra(EXTRA_QUESTION);

        httpClient   = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60,  java.util.concurrent.TimeUnit.SECONDS).build();
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
            startCamera();
        else
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, PERM_CAMERA);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
            ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();

                provider.unbindAll();
                provider.bindToLifecycle(this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, imageCapture);
            } catch (Exception e) {
                tvResult.setText("Camera error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureAndAnalyse() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show(); return;
        }
        btnAnalyse.setEnabled(false);
        tvResult.setText("Capturing…");

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bmp = imageProxyToBitmap(image);
                image.close();
                if (bmp == null) {
                    runOnUiThread(() -> { tvResult.setText("Capture failed."); btnAnalyse.setEnabled(true); });
                    return;
                }
                // Resize
                int w = bmp.getWidth(), h = bmp.getHeight(), maxPx = 768;
                if (w > maxPx || h > maxPx) {
                    float s = Math.min((float) maxPx / w, (float) maxPx / h);
                    bmp = Bitmap.createScaledBitmap(bmp, (int)(w*s), (int)(h*s), true);
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 72, baos);
                String b64 = "data:image/jpeg;base64," +
                    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                runOnUiThread(() -> tvResult.setText("Analysing with H.E.N.R.Y…"));
                sendToHenry(b64);
            }

            @Override
            public void onError(@NonNull ImageCaptureException e) {
                runOnUiThread(() -> { tvResult.setText("Error: " + e.getMessage()); btnAnalyse.setEnabled(true); });
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    private void sendToHenry(String imageB64) {
        new Thread(() -> {
            try {
                JSONArray msgs = new JSONArray();
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", userQuestion);
                msgs.put(userMsg);

                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("image", imageB64);
                body.put("responseMode", "balanced");

                RequestBody rb = RequestBody.create(
                    body.toString(), MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                    .url("https://jarvis-ai-seven-dun.vercel.app/api/jarvis")
                    .post(rb)
                    .addHeader("Content-Type", "application/json").build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        runOnUiThread(() -> { tvResult.setText("Server error. Try again."); btnAnalyse.setEnabled(true); });
                        return;
                    }
                    String raw  = resp.body().string();
                    JSONObject j = new JSONObject(raw);
                    String reply = j.optString("reply", raw);
                    // Strip emotion tag
                    reply = reply.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                    final String result = reply;
                    runOnUiThread(() -> {
                        tvResult.setText(result);
                        btnAnalyse.setEnabled(true);
                        // Also return result to MainActivity
                        Intent data = new Intent();
                        data.putExtra(EXTRA_RESULT, result);
                        setResult(Activity.RESULT_OK, data);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> { tvResult.setText("Error: " + e.getMessage()); btnAnalyse.setEnabled(true); });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERM_CAMERA && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            startCamera();
        else finish();
    }

    @Override protected void onDestroy() {
        cameraExecutor.shutdown();
        super.onDestroy();
    }
}
