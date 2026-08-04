package com.jarvis.ai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;
import com.google.mlkit.vision.objects.*;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.label.*;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

/**
 * VisionActivity — HENRY Vision Intelligence Hub
 *
 * Modes:
 * 1. IMAGE CLASSIFICATION — ML Kit labels (what is this?)
 * 2. OBJECT DETECTION    — ML Kit bounding boxes (where are things?)
 * 3. OBJECT TRACKING     — Live camera + continuous ML Kit detection
 * 4. FACIAL RECOGNITION  — ML Kit face detection + attributes
 * 5. IMAGE RETRIEVAL     — Find visually similar content (Groq AI description → Google Images)
 */
public class VisionActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    public static final String EXTRA_MODE = "vision_mode";
    public static final int MODE_CLASSIFY  = 1;
    public static final int MODE_DETECT    = 2;
    public static final int MODE_TRACK     = 3;
    public static final int MODE_FACE      = 4;
    public static final int MODE_RETRIEVE  = 5;

    private static final int REQ_GALLERY = 901;
    private static final int REQ_CAMERA  = 902;
    private static final int REQ_PERM    = 903;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ttsReady = false;

    // UI
    private PreviewView    previewView;
    private VisionOverlayView overlayView;
    private TextView       tvTitle, tvResult, tvMode;
    private Button         btnGallery, btnCamera, btnSwitch, btnBack;
    private ProgressBar    progressBar;
    private LinearLayout   menuLayout, cameraLayout;
    private ScrollView     resultScroll;

    // Camera
    private ImageCapture     imageCapture;
    private ImageAnalysis    imageAnalysis;
    private ExecutorService  cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean          frontCamera = false;
    private boolean          trackingActive = false;

    // Mode
    private int currentMode = 0;

    // ML Kit
    private ImageLabeler      labeler;
    private ObjectDetector    objectDetector;
    private FaceDetector      faceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vision);

        previewView  = findViewById(R.id.vision_preview);
        overlayView  = findViewById(R.id.vision_overlay);
        tvTitle      = findViewById(R.id.vision_title);
        tvResult     = findViewById(R.id.vision_result);
        tvMode       = findViewById(R.id.vision_mode_label);
        btnGallery   = findViewById(R.id.vision_btn_gallery);
        btnCamera    = findViewById(R.id.vision_btn_camera);
        btnSwitch    = findViewById(R.id.vision_btn_switch);
        btnBack      = findViewById(R.id.vision_btn_back);
        progressBar  = findViewById(R.id.vision_progress);
        menuLayout   = findViewById(R.id.vision_menu);
        cameraLayout = findViewById(R.id.vision_camera_layout);
        resultScroll = findViewById(R.id.vision_result_scroll);

        tts           = new TextToSpeech(this, this);
        cameraExecutor= Executors.newSingleThreadExecutor();

        initMLKit();

        int mode = getIntent().getIntExtra(EXTRA_MODE, 0);
        if (mode > 0) {
            startMode(mode);
        } else {
            showMenu();
        }

        if (btnBack != null) btnBack.setOnClickListener(v -> {
            stopCamera();
            if (currentMode > 0) { currentMode = 0; showMenu(); }
            else finish();
        });

        if (btnGallery != null) btnGallery.setOnClickListener(v -> pickGallery());
        if (btnCamera  != null) btnCamera.setOnClickListener(v -> captureFromCamera());
        if (btnSwitch  != null) btnSwitch.setOnClickListener(v -> {
            frontCamera = !frontCamera;
            startCameraPreview();
        });
    }

    // ── ML Kit init ────────────────────────────────────────────────────────────
    private void initMLKit() {
        labeler = ImageLabeling.getClient(
            new ImageLabelerOptions.Builder().setConfidenceThreshold(0.55f).build());

        objectDetector = ObjectDetection.getClient(
            new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build());

        faceDetector = FaceDetection.getClient(
            new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build());
    }

    // ── Menu ───────────────────────────────────────────────────────────────────
    private void showMenu() {
        currentMode = 0;
        if (menuLayout   != null) menuLayout.setVisibility(View.VISIBLE);
        if (cameraLayout != null) cameraLayout.setVisibility(View.GONE);
        if (resultScroll != null) resultScroll.setVisibility(View.GONE);
        if (tvTitle      != null) tvTitle.setText("◈ VISION INTELLIGENCE");

        Object[][] modes = {
            { "🏷 Image Classification",     MODE_CLASSIFY, "Identify what's in any image with confidence scores" },
            { "📦 Object Detection",         MODE_DETECT,   "Detect and locate multiple objects with bounding boxes" },
            { "🎯 Object Tracking",          MODE_TRACK,    "Live camera: track objects in real time" },
            { "👤 Facial Recognition",       MODE_FACE,     "Detect faces, expressions, and facial attributes" },
            { "🔍 Image Retrieval",          MODE_RETRIEVE, "Describe image content and find similar images online" },
        };

        if (menuLayout == null) return;
        menuLayout.removeAllViews();
        for (Object[] m : modes) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(0xFF040F1D);
            card.setPadding(20, 16, 20, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 8);
            card.setLayoutParams(lp);

            TextView title = new TextView(this);
            title.setText((String) m[0]);
            title.setTextColor(0xFF00D4FF);
            title.setTextSize(16f);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            TextView desc = new TextView(this);
            desc.setText((String) m[2]);
            desc.setTextColor(0xFF2A6A8A);
            desc.setTextSize(12f);
            desc.setPadding(0, 4, 0, 0);

            card.addView(title);
            card.addView(desc);
            final int modeId = (int) m[1];
            card.setOnClickListener(v -> startMode(modeId));
            menuLayout.addView(card);

            View div = new View(this);
            div.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
            div.setBackgroundColor(0xFF0A2A3A);
            menuLayout.addView(div);
        }
    }

    // ── Start a mode ───────────────────────────────────────────────────────────
    private void startMode(int mode) {
        currentMode = mode;
        if (menuLayout != null) menuLayout.setVisibility(View.GONE);

        String[] labels = { "", "IMAGE CLASSIFICATION", "OBJECT DETECTION",
                            "OBJECT TRACKING", "FACIAL RECOGNITION", "IMAGE RETRIEVAL" };
        if (tvTitle != null && mode > 0 && mode <= labels.length - 1)
            tvTitle.setText("◈ " + labels[mode]);
        if (tvMode  != null) tvMode.setText(labels[mode > 0 && mode <= labels.length - 1 ? mode : 0]);

        if (mode == MODE_TRACK) {
            // Live camera mode — no gallery option
            if (cameraLayout != null) cameraLayout.setVisibility(View.VISIBLE);
            if (resultScroll != null) resultScroll.setVisibility(View.VISIBLE);
            if (btnGallery   != null) btnGallery.setVisibility(View.GONE);
            if (btnCamera    != null) btnCamera.setText("⏹ STOP TRACKING");
            if (tvResult     != null) tvResult.setText("Point camera at objects to track them live…");
            requestCameraAndStart(true);
        } else {
            // Still-image mode
            if (cameraLayout != null) cameraLayout.setVisibility(View.GONE);
            if (resultScroll != null) resultScroll.setVisibility(View.VISIBLE);
            if (btnGallery   != null) { btnGallery.setVisibility(View.VISIBLE); }
            if (btnCamera    != null) { btnCamera.setText("📷 CAMERA"); }
            if (tvResult     != null) tvResult.setText("Choose an image from gallery or camera.");
        }
    }

    // ── Camera permission + start ─────────────────────────────────────────────
    private void requestCameraAndStart(boolean trackingMode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (trackingMode) startTrackingCamera(); else startCameraPreview();
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, REQ_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQ_PERM && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (currentMode == MODE_TRACK) startTrackingCamera(); else startCameraPreview();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Mode 1 & 2 & 4 & 5: still image capture ──────────────────────────────
    private void pickGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        startActivityForResult(i, REQ_GALLERY);
    }

    private void captureFromCamera() {
        if (currentMode == MODE_TRACK) {
            trackingActive = !trackingActive;
            btnCamera.setText(trackingActive ? "⏹ STOP TRACKING" : "▶ START TRACKING");
            return;
        }
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) != null)
            startActivityForResult(i, REQ_CAMERA);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null && req != REQ_CAMERA) return;
        Bitmap bmp = null;
        try {
            if (req == REQ_GALLERY && data != null && data.getData() != null) {
                bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
            } else if (req == REQ_CAMERA && data != null && data.getExtras() != null) {
                bmp = (Bitmap) data.getExtras().get("data");
            }
            if (bmp != null) analyseImage(bmp);
        } catch (Exception e) {
            showResult("Error loading image: " + e.getMessage());
        }
    }

    // ── Analyse still image based on current mode ─────────────────────────────
    private void analyseImage(Bitmap bmp) {
        setLoading(true);
        if (overlayView != null) overlayView.clearBoxes();

        switch (currentMode) {
            case MODE_CLASSIFY: runClassification(bmp); break;
            case MODE_DETECT:   runObjectDetection(bmp, false); break;
            case MODE_FACE:     runFaceDetection(bmp); break;
            case MODE_RETRIEVE: runImageRetrieval(bmp); break;
        }
    }

    // ── Mode 1: Image Classification ──────────────────────────────────────────
    private void runClassification(Bitmap bmp) {
        InputImage img = InputImage.fromBitmap(bmp, 0);
        labeler.process(img)
            .addOnSuccessListener(labels -> {
                StringBuilder sb = new StringBuilder("**◈ IMAGE CLASSIFICATION**\n\n");
                for (ImageLabel l : labels) {
                    int bar = (int)(l.getConfidence() * 20);
                    sb.append(String.format("%-22s %.0f%%  %s\n",
                        l.getText(),
                        l.getConfidence() * 100,
                        "█".repeat(bar) + "░".repeat(20 - bar)));
                }
                if (labels.isEmpty()) sb.append("No labels detected.");
                else {
                    // Ask HENRY to elaborate on top result
                    String top = labels.get(0).getText();
                    sb.append("\n**Top match:** ").append(top);
                    askHenryAbout(top, sb.toString());
                    return;
                }
                showResult(sb.toString());
                speak("I detected " + (labels.isEmpty() ? "nothing clearly" : labels.get(0).getText()), "neutral");
            })
            .addOnFailureListener(e -> { setLoading(false); showResult("Classification failed: " + e.getMessage()); });
    }

    // ── Mode 2 & Tracking: Object Detection ───────────────────────────────────
    private void runObjectDetection(Bitmap bmp, boolean isTracking) {
        InputImage img = InputImage.fromBitmap(bmp, 0);
        // For tracking, use streaming detector
        ObjectDetector detector = isTracking ? ObjectDetection.getClient(
            new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()) : objectDetector;

        detector.process(img)
            .addOnSuccessListener(objects -> {
                StringBuilder sb = new StringBuilder(isTracking ? "" : "**◈ OBJECT DETECTION**\n\n");
                List<VisionOverlayView.Box> boxes = new ArrayList<>();
                int i = 1;
                for (DetectedObject obj : objects) {
                    String label = obj.getLabels().isEmpty() ? "Object" :
                        obj.getLabels().get(0).getText();
                    float conf = obj.getLabels().isEmpty() ? 0f :
                        obj.getLabels().get(0).getConfidence();
                    int trackId = obj.getTrackingId() != null ? obj.getTrackingId() : i;
                    sb.append(String.format("#%d  %-18s  %.0f%%\n", trackId, label, conf * 100));
                    RectF r = new RectF(obj.getBoundingBox());
                    int color = TRACK_COLORS[trackId % TRACK_COLORS.length];
                    boxes.add(new VisionOverlayView.Box(r, label, conf, color));
                    i++;
                }
                if (objects.isEmpty()) sb.append("No objects detected.");

                final String result = sb.toString();
                handler.post(() -> {
                    setLoading(false);
                    showResult(result);
                    if (overlayView != null && bmp != null) {
                        overlayView.setImageSize(bmp.getWidth(), bmp.getHeight());
                        overlayView.setBoxes(boxes);
                    }
                    if (!isTracking && !objects.isEmpty()) {
                        String top = objects.get(0).getLabels().isEmpty() ? "object" :
                            objects.get(0).getLabels().get(0).getText();
                        speak("I detected " + objects.size() + " object" + (objects.size() > 1 ? "s" : "") +
                            ". Top: " + top, "neutral");
                    }
                });
            })
            .addOnFailureListener(e -> handler.post(() -> {
                setLoading(false);
                if (!isTracking) showResult("Detection failed: " + e.getMessage());
            }));
    }

    private static final int[] TRACK_COLORS = {
        0xFF00D4FF, 0xFF00FF99, 0xFFFF9944, 0xFFCC88FF, 0xFFFFDD00,
        0xFFFF4444, 0xFF44FFAA, 0xFF4488FF
    };

    // ── Mode 3: Object Tracking (live camera) ─────────────────────────────────
    private ObjectDetector streamDetector;

    private void startTrackingCamera() {
        trackingActive = true;
        streamDetector = ObjectDetection.getClient(
            new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraAndStart(true); return;
        }

        ListenableFuture<ProcessCameraProvider> future =
            ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                if (previewView != null) preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cs = new CameraSelector.Builder()
                    .requireLensFacing(frontCamera ? CameraSelector.LENS_FACING_FRONT
                                                   : CameraSelector.LENS_FACING_BACK)
                    .build();

                imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (!trackingActive) { imageProxy.close(); return; }
                    Bitmap bmp = imageProxyToBitmap(imageProxy);
                    imageProxy.close();
                    if (bmp == null) return;
                    processTrackingFrame(bmp);
                });

                if (cameraLayout != null)
                    handler.post(() -> cameraLayout.setVisibility(View.VISIBLE));

                cameraProvider.bindToLifecycle(this, cs, preview, imageAnalysis);
            } catch (Exception e) {
                handler.post(() -> showResult("Camera error: " + e.getMessage()));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processTrackingFrame(Bitmap bmp) {
        InputImage img = InputImage.fromBitmap(bmp, 0);
        streamDetector.process(img)
            .addOnSuccessListener(objects -> handler.post(() -> {
                List<VisionOverlayView.Box> boxes = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                for (DetectedObject obj : objects) {
                    String label = obj.getLabels().isEmpty() ? "?" :
                        obj.getLabels().get(0).getText();
                    float conf = obj.getLabels().isEmpty() ? 0f :
                        obj.getLabels().get(0).getConfidence();
                    int tid = obj.getTrackingId() != null ? obj.getTrackingId() : 0;
                    sb.append("ID:").append(tid).append(" ").append(label)
                      .append(" ").append((int)(conf*100)).append("%\n");
                    boxes.add(new VisionOverlayView.Box(new RectF(obj.getBoundingBox()),
                        label + " #" + tid, conf, TRACK_COLORS[tid % TRACK_COLORS.length]));
                }
                if (tvResult != null) tvResult.setText(sb.length() > 0 ? sb.toString() : "Scanning…");
                if (overlayView != null) {
                    overlayView.setImageSize(bmp.getWidth(), bmp.getHeight());
                    overlayView.setBoxes(boxes);
                }
            }))
            .addOnFailureListener(e -> {});
    }

    // ── Mode 4: Facial Recognition ─────────────────────────────────────────────
    private void runFaceDetection(Bitmap bmp) {
        InputImage img = InputImage.fromBitmap(bmp, 0);
        faceDetector.process(img)
            .addOnSuccessListener(faces -> {
                StringBuilder sb = new StringBuilder("**◈ FACIAL RECOGNITION**\n\n");
                List<VisionOverlayView.Box> boxes = new ArrayList<>();
                if (faces.isEmpty()) {
                    sb.append("No faces detected in this image.");
                } else {
                    sb.append("Detected **").append(faces.size()).append("** face")
                      .append(faces.size() > 1 ? "s" : "").append("\n\n");
                    int i = 1;
                    for (Face face : faces) {
                        sb.append("**Face #").append(i).append("**\n");
                        if (face.getSmilingProbability() != null)
                            sb.append("  Smiling:    ").append(pct(face.getSmilingProbability())).append("\n");
                        if (face.getLeftEyeOpenProbability() != null)
                            sb.append("  Left eye:   ").append(face.getLeftEyeOpenProbability() > 0.5 ? "Open" : "Closed").append("\n");
                        if (face.getRightEyeOpenProbability() != null)
                            sb.append("  Right eye:  ").append(face.getRightEyeOpenProbability() > 0.5 ? "Open" : "Closed").append("\n");
                        sb.append("  Head Y:     ").append(String.format("%.1f°", face.getHeadEulerAngleY())).append("\n");
                        sb.append("  Head Z:     ").append(String.format("%.1f°", face.getHeadEulerAngleZ())).append("\n");
                        if (face.getTrackingId() != null)
                            sb.append("  Track ID:   #").append(face.getTrackingId()).append("\n");
                        sb.append("\n");
                        // Infer likely emotion
                        String emotion = inferEmotion(face);
                        sb.append("  Expression: ").append(emotion).append("\n\n");
                        boxes.add(new VisionOverlayView.Box(new RectF(face.getBoundingBox()),
                            "Face #" + i + " · " + emotion, 1f, 0xFFCC88FF));
                        i++;
                    }
                }
                final String result = sb.toString();
                handler.post(() -> {
                    setLoading(false);
                    showResult(result);
                    if (overlayView != null) {
                        overlayView.setImageSize(bmp.getWidth(), bmp.getHeight());
                        overlayView.setBoxes(boxes);
                    }
                    speak(faces.size() + " face" + (faces.size() != 1 ? "s" : "") + " detected.", "neutral");
                });
            })
            .addOnFailureListener(e -> { setLoading(false); showResult("Face detection failed: " + e.getMessage()); });
    }

    private String inferEmotion(Face face) {
        if (face.getSmilingProbability() != null && face.getSmilingProbability() > 0.7f) return "😊 Happy";
        if (face.getSmilingProbability() != null && face.getSmilingProbability() < 0.2f) {
            if (face.getHeadEulerAngleY() != 0) return "😐 Neutral / Thinking";
            return "😐 Neutral";
        }
        if (face.getLeftEyeOpenProbability()  != null && face.getLeftEyeOpenProbability()  < 0.3f &&
            face.getRightEyeOpenProbability() != null && face.getRightEyeOpenProbability() < 0.3f)
            return "😴 Eyes Closed / Tired";
        return "🙂 Relaxed";
    }

    private String pct(float v) { return String.format("%.0f%%", v * 100); }

    // ── Mode 5: Content-Based Image Retrieval ──────────────────────────────────
    private void runImageRetrieval(Bitmap bmp) {
        if (tvResult != null) tvResult.setText("Analysing image for retrieval…");
        // Step 1: Classify labels
        InputImage img = InputImage.fromBitmap(bmp, 0);
        labeler.process(img)
            .addOnSuccessListener(labels -> {
                String topLabels = "";
                for (int i = 0; i < Math.min(5, labels.size()); i++) {
                    topLabels += labels.get(i).getText() + (i < Math.min(4, labels.size()-1) ? ", " : "");
                }
                final String labelStr = topLabels.isEmpty() ? "image content" : topLabels;

                // Step 2: Encode and ask HENRY AI for full description
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                Bitmap scaled = Bitmap.createScaledBitmap(bmp, 512, 512, true);
                scaled.compress(Bitmap.CompressFormat.JPEG, 72, bos);
                String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);

                List<HistoryItem> h = new ArrayList<>();
                h.add(new HistoryItem("user", "Describe this image in detail for content-based retrieval: what objects, scenes, colors, textures, style, mood, and any text visible. Be precise."));

                JarvisApi.ask(h, "data:image/jpeg;base64," + b64, "detailed", null, new JarvisApi.Callback() {
                    @Override public void onSuccess(String reply, String imageUrl, List<String> fu) {
                        String desc = reply.replaceAll("\\[EMOTION:[^]]+]", "").trim();
                        String searchQuery = labelStr;

                        // Step 3: Build search URL
                        String googleUrl = "https://www.google.com/search?tbm=isch&q=" +
                            Uri.encode(searchQuery);
                        String lensUrl   = "https://lens.google.com/uploadbyurl?url=";

                        handler.post(() -> {
                            setLoading(false);
                            String result = "**◈ IMAGE RETRIEVAL**\n\n" +
                                "**AI Description:**\n" + desc + "\n\n" +
                                "**Detected Labels:** " + labelStr + "\n\n" +
                                "**Find similar images:**\n" +
                                "• [Google Images →](" + googleUrl + ")\n" +
                                "• [Google Lens →](https://lens.google.com/)";
                            showResult(result);
                            speak("Image analysed. I found " + labelStr + ". Opening similar image search.", "excited");

                            // Offer to open Google Images
                            new android.app.AlertDialog.Builder(VisionActivity.this)
                                .setTitle("Find Similar Images")
                                .setMessage("Search Google Images for: " + searchQuery + "?")
                                .setPositiveButton("Open Google Images", (d, w) -> {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl)));
                                })
                                .setNegativeButton("Open Google Lens", (d, w) -> {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com/")));
                                })
                                .setNeutralButton("Close", null)
                                .show();
                        });
                    }
                    @Override public void onError(String e) {
                        handler.post(() -> {
                            setLoading(false);
                            String googleUrl2 = "https://www.google.com/search?tbm=isch&q=" + Uri.encode(labelStr);
                            showResult("**Labels detected:** " + labelStr + "\n\n[Search Google Images →](" + googleUrl2 + ")");
                        });
                    }
                });
            })
            .addOnFailureListener(e -> { setLoading(false); showResult("Retrieval failed: " + e.getMessage()); });
    }

    // ── HENRY AI elaboration ───────────────────────────────────────────────────
    private void askHenryAbout(String subject, String labelResult) {
        List<HistoryItem> h = new ArrayList<>();
        h.add(new HistoryItem("user", "I detected this in an image: " + subject +
            ". Give me 2-3 fascinating facts about it in HENRY's style. Be brief and interesting."));
        JarvisApi.ask(h, null, "brief", null, new JarvisApi.Callback() {
            @Override public void onSuccess(String reply, String imageUrl, List<String> fu) {
                String aiComment = reply.replaceAll("\\[EMOTION:[^]]+]", "").trim();
                handler.post(() -> {
                    setLoading(false);
                    showResult(labelResult + "\n\n**HENRY says:**\n" + aiComment);
                    speak(aiComment, "excited");
                });
            }
            @Override public void onError(String e) {
                handler.post(() -> { setLoading(false); showResult(labelResult); });
            }
        });
    }

    // ── Camera preview (for capture) ──────────────────────────────────────────
    private void startCameraPreview() {
        // Not needed for still-image modes — use gallery/camera intent
    }

    private void stopCamera() {
        trackingActive = false;
        if (cameraProvider != null) try { cameraProvider.unbindAll(); } catch (Exception ignored) {}
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) cameraExecutor.shutdown();
    }

    // ── ImageProxy → Bitmap ────────────────────────────────────────────────────
    private Bitmap imageProxyToBitmap(ImageProxy proxy) {
        try {
            ImageProxy.PlaneProxy plane = proxy.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────
    private void showResult(String text) {
        handler.post(() -> {
            setLoading(false);
            if (tvResult != null) tvResult.setText(text);
        });
    }

    private void setLoading(boolean loading) {
        handler.post(() -> {
            if (progressBar != null)
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US); ttsReady = true;
        }
    }

    private void speak(String text, String emotion) {
        if (ttsReady && tts != null)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vision");
    }

    @Override protected void onDestroy() {
        stopCamera();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
