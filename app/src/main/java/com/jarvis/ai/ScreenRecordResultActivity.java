package com.jarvis.ai;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Locale;

public class ScreenRecordResultActivity extends AppCompatActivity {

    private static final String TAG = "ScreenRecordStudio";
    public static final String EXTRA_VIDEO_PATH = "extra_video_path";

    private String videoPath;
    private File videoFile;

    private VideoView videoPreview;
    private ImageView btnPlayOverlay;
    private TextView tvDurationBadge;
    private TextView tvVideoDetails;
    private TextView btnDeleteVideo;
    private ImageButton btnBack;

    private TextView chipTikTok;
    private TextView chipYouTube;
    private TextView chipReels;

    private TextView toneViral;
    private TextView toneTech;
    private TextView toneGaming;
    private TextView toneAesthetic;

    private EditText etVideoTopic;
    private Button btnGenerateCaption;
    private EditText etCaptionOutput;
    private TextView tvHashtagsPreview;
    private TextView btnCopyCaption;

    private Button btnPublishPlatform;
    private Button btnPublishYouTube;
    private Button btnShareAll;
    private Button btnOpenGallery;

    // Mic audio track
    private androidx.cardview.widget.CardView cardMicAudioTrack;
    private TextView tvTempAudioPath;
    private TextView tvAudioStatusBadge;
    private Button btnPlayAudioTrack;
    private Button btnShareAudioTrack;
    private File tempAudioFile;
    private MediaPlayer audioPlayer;
    private boolean isAudioPlaying = false;

    private String selectedPlatform = "TikTok";
    private String selectedTone = "Viral";
    private boolean isPlaying = false;
    private int videoDurationSec = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_record_result);

        videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        if (videoPath == null || videoPath.isEmpty()) {
            videoPath = ScreenRecorderService.currentRecordingPath;
        }

        if (videoPath != null) {
            videoFile = new File(videoPath);
        }

        initViews();
        setupVideoPlayer();
        setupMicAudioTrack();
        setupPlatformSelectors();
        setupToneSelectors();
        setupCaptionGenerator();
        setupPublishActions();

        // Generate default starter caption
        generateCaption(false);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvDurationBadge = findViewById(R.id.tvDurationBadge);
        videoPreview = findViewById(R.id.videoPreview);
        btnPlayOverlay = findViewById(R.id.btnPlayOverlay);
        tvVideoDetails = findViewById(R.id.tvVideoDetails);
        btnDeleteVideo = findViewById(R.id.btnDeleteVideo);

        cardMicAudioTrack = findViewById(R.id.cardMicAudioTrack);
        tvTempAudioPath = findViewById(R.id.tvTempAudioPath);
        tvAudioStatusBadge = findViewById(R.id.tvAudioStatusBadge);
        btnPlayAudioTrack = findViewById(R.id.btnPlayAudioTrack);
        btnShareAudioTrack = findViewById(R.id.btnShareAudioTrack);

        chipTikTok = findViewById(R.id.chipTikTok);
        chipYouTube = findViewById(R.id.chipYouTube);
        chipReels = findViewById(R.id.chipReels);

        toneViral = findViewById(R.id.toneViral);
        toneTech = findViewById(R.id.toneTech);
        toneGaming = findViewById(R.id.toneGaming);
        toneAesthetic = findViewById(R.id.toneAesthetic);

        etVideoTopic = findViewById(R.id.etVideoTopic);
        btnGenerateCaption = findViewById(R.id.btnGenerateCaption);
        etCaptionOutput = findViewById(R.id.etCaptionOutput);
        tvHashtagsPreview = findViewById(R.id.tvHashtagsPreview);
        btnCopyCaption = findViewById(R.id.btnCopyCaption);

        btnPublishPlatform = findViewById(R.id.btnPublishPlatform);
        btnPublishYouTube = findViewById(R.id.btnPublishYouTube);
        btnShareAll = findViewById(R.id.btnShareAll);
        btnOpenGallery = findViewById(R.id.btnOpenGallery);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupVideoPlayer() {
        if (videoFile == null || !videoFile.exists()) {
            tvVideoDetails.setText("Video file not found");
            return;
        }

        long fileSizeBytes = videoFile.length();
        double fileSizeMb = fileSizeBytes / (1024.0 * 1024.0);

        try {
            videoPreview.setVideoPath(videoFile.getAbsolutePath());
            videoPreview.setOnPreparedListener(mp -> {
                int durationMs = mp.getDuration();
                videoDurationSec = durationMs / 1000;
                int min = videoDurationSec / 60;
                int sec = videoDurationSec % 60;
                String durStr = String.format(Locale.US, "%02d:%02d", min, sec);
                tvDurationBadge.setText(durStr);

                tvVideoDetails.setText(String.format(Locale.US,
                    "Duration: %s  •  Size: %.1f MB  •  1080p MP4",
                    durStr, fileSizeMb));

                // Show first frame
                mp.seekTo(100);
            });

            videoPreview.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlayOverlay.setVisibility(View.VISIBLE);
                btnPlayOverlay.setImageResource(android.R.drawable.ic_media_play);
            });

            View.OnClickListener togglePlay = v -> {
                if (isPlaying) {
                    videoPreview.pause();
                    isPlaying = false;
                    btnPlayOverlay.setImageResource(android.R.drawable.ic_media_play);
                    btnPlayOverlay.setVisibility(View.VISIBLE);
                } else {
                    videoPreview.start();
                    isPlaying = true;
                    btnPlayOverlay.setVisibility(View.GONE);
                }
            };

            videoPreview.setOnClickListener(togglePlay);
            btnPlayOverlay.setOnClickListener(togglePlay);

        } catch (Exception e) {
            Log.e(TAG, "Video player error: " + e.getMessage());
            tvVideoDetails.setText(String.format(Locale.US, "Size: %.1f MB (Ready to share)", fileSizeMb));
        }

        btnDeleteVideo.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete Recording?")
                .setMessage("Are you sure you want to delete this screen recording?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        if (videoFile != null && videoFile.exists()) {
                            videoFile.delete();
                        }
                        Toast.makeText(this, "Recording deleted.", Toast.LENGTH_SHORT).show();
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void setupPlatformSelectors() {
        chipTikTok.setOnClickListener(v -> selectPlatform("TikTok"));
        chipYouTube.setOnClickListener(v -> selectPlatform("YouTube"));
        chipReels.setOnClickListener(v -> selectPlatform("Reels"));
    }

    private void selectPlatform(String platform) {
        selectedPlatform = platform;

        chipTikTok.setBackgroundColor(platform.equals("TikTok") ? 0xFF00FFCC : 0xFF111B2C);
        chipTikTok.setTextColor(platform.equals("TikTok") ? 0xFF000000 : 0xFFA0B8D8);

        chipYouTube.setBackgroundColor(platform.equals("YouTube") ? 0xFFFF0000 : 0xFF111B2C);
        chipYouTube.setTextColor(platform.equals("YouTube") ? 0xFFFFFFFF : 0xFFA0B8D8);

        chipReels.setBackgroundColor(platform.equals("Reels") ? 0xFFE1306C : 0xFF111B2C);
        chipReels.setTextColor(platform.equals("Reels") ? 0xFFFFFFFF : 0xFFA0B8D8);

        if (platform.equals("TikTok")) {
            btnPublishPlatform.setText("🎵 POST DIRECTLY TO TIKTOK");
            btnPublishPlatform.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFE2C55));
        } else if (platform.equals("YouTube")) {
            btnPublishPlatform.setText("▶️ POST TO YOUTUBE / SHORTS");
            btnPublishPlatform.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF0000));
        } else {
            btnPublishPlatform.setText("📸 POST TO INSTAGRAM REELS");
            btnPublishPlatform.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE1306C));
        }

        generateCaption(false);
    }

    private void setupToneSelectors() {
        toneViral.setOnClickListener(v -> selectTone("Viral"));
        toneTech.setOnClickListener(v -> selectTone("Tech"));
        toneGaming.setOnClickListener(v -> selectTone("Gaming"));
        toneAesthetic.setOnClickListener(v -> selectTone("Aesthetic"));
    }

    private void selectTone(String tone) {
        selectedTone = tone;
        toneViral.setBackgroundColor(tone.equals("Viral") ? 0xFF1E3250 : 0xFF0F1A2A);
        toneViral.setTextColor(tone.equals("Viral") ? 0xFF00FFCC : 0xFF7A92B4);

        toneTech.setBackgroundColor(tone.equals("Tech") ? 0xFF1E3250 : 0xFF0F1A2A);
        toneTech.setTextColor(tone.equals("Tech") ? 0xFF00FFCC : 0xFF7A92B4);

        toneGaming.setBackgroundColor(tone.equals("Gaming") ? 0xFF1E3250 : 0xFF0F1A2A);
        toneGaming.setTextColor(tone.equals("Gaming") ? 0xFF00FFCC : 0xFF7A92B4);

        toneAesthetic.setBackgroundColor(tone.equals("Aesthetic") ? 0xFF1E3250 : 0xFF0F1A2A);
        toneAesthetic.setTextColor(tone.equals("Aesthetic") ? 0xFF00FFCC : 0xFF7A92B4);

        generateCaption(false);
    }

    private void setupCaptionGenerator() {
        btnGenerateCaption.setOnClickListener(v -> generateCaption(true));

        btnCopyCaption.setOnClickListener(v -> {
            String text = etCaptionOutput.getText().toString().trim();
            if (!text.isEmpty()) {
                copyToClipboard(text, "Caption & hashtags copied to clipboard! 📋");
            }
        });
    }

    private void generateCaption(boolean showToast) {
        String topic = etVideoTopic.getText().toString().trim();
        if (topic.isEmpty()) {
            topic = "Screen recording walkthrough and demo with H.E.N.R.Y. AI Assistant";
        }

        String caption;
        String tags;

        if (selectedPlatform.equals("TikTok")) {
            if (selectedTone.equals("Viral")) {
                caption = "POV: You turned your phone into Tony Stark's actual lab 🦾⚡ Wait till you see what happens at the end… 👀👇";
                tags = "#TikTok #TechTok #Jarvis #IronMan #Android #ScreenRecording #ViralTech #FYP #ForYouPage #AI #FutureTech";
            } else if (selectedTone.equals("Tech")) {
                caption = "Full screen walkthrough of the high-speed AI interface running native on Android 🚀📱 Check this workflow:";
                tags = "#Tech #AndroidDev #TechTutorial #Software #ScreenRecord #Productivity #Coding #AI #Innovation";
            } else if (selectedTone.equals("Gaming")) {
                caption = "Cleanest screen capture clip you'll see all day! 🎮🔥 Rate this play 1-10 in the comments 👇";
                tags = "#Gaming #GamerTok #Clips #Gameplay #MobileGaming #Highlights #ScreenRecord #ViralGaming #FYP";
            } else {
                caption = "Smooth visuals and pure futuristic vibes ✨📱 Crafted with H·E·N·R·Y.";
                tags = "#Aesthetic #TechAesthetic #CleanUI #Cyberpunk #Futuristic #AndroidSetup #FYP #Satisfying";
            }
        } else if (selectedPlatform.equals("YouTube")) {
            if (selectedTone.equals("Viral")) {
                caption = "How I Built Tony Stark's AI System on Android! 🚀 (Full Screen Recording)\n\n" +
                    "Watch this live walkthrough demonstration. Drop a like and subscribe for more futuristic tech builds!";
                tags = "#Shorts #YouTubeShorts #Tech #IronMan #Android #AI #ScreenRecording #Viral";
            } else if (selectedTone.equals("Tech")) {
                caption = "Android Screen Capture & AI Workflow Demonstration ⚡\n\n" +
                    "Step-by-step UI and architecture walkthrough. Make sure to hit Subscribe for daily tech tutorials.";
                tags = "#Shorts #TechTutorial #Android #Coding #SoftwareEngineering #ScreenRecord #AI";
            } else if (selectedTone.equals("Gaming")) {
                caption = "Insane Screen Recording Highlight! 🔥🎮\n\n" +
                    "Recorded at 60 FPS in 1080p. Don't forget to Like and Subscribe!";
                tags = "#Shorts #Gaming #Gameplay #GamingClips #MobileGaming #YouTubeGaming";
            } else {
                caption = "Futuristic UI Screen Capture — Aesthetic Showcase ✨\n\n" +
                    "High-definition interface preview powered by H·E·N·R·Y.";
                tags = "#Shorts #Tech #Aesthetic #UIUX #Design #Android";
            }
        } else { // Reels
            if (selectedTone.equals("Viral")) {
                caption = "This changed the entire way I use my phone 🤯⚡ Save this for later! 📌";
                tags = "#Reels #ExplorePage #TechReels #TrendingReels #Android #AI #TechHacks #ViralReels";
            } else {
                caption = "Futuristic Android screen recording in action ✨ Which feature would you use most?";
                tags = "#Reels #Tech #Innovation #FutureTech #AndroidCommunity #Productivity #ReelsInstagram";
            }
        }

        String fullPost = caption + "\n\n" + tags;
        etCaptionOutput.setText(fullPost);
        tvHashtagsPreview.setText(tags);

        if (showToast) {
            Toast.makeText(this, "✨ AI Caption & Hashtags updated for " + selectedPlatform, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupPublishActions() {
        btnPublishPlatform.setOnClickListener(v -> {
            if (selectedPlatform.equals("TikTok")) {
                shareToTikTok();
            } else if (selectedPlatform.equals("YouTube")) {
                shareToYouTube();
            } else {
                shareToInstagram();
            }
        });

        btnPublishYouTube.setOnClickListener(v -> shareToYouTube());

        btnShareAll.setOnClickListener(v -> shareToAnyApp());

        btnOpenGallery.setOnClickListener(v -> openInGallery());
    }

    private void shareToTikTok() {
        String caption = etCaptionOutput.getText().toString().trim();
        copyToClipboard(caption, "✅ Caption & hashtags copied! Paste into TikTok description.");

        if (videoFile == null || !videoFile.exists()) {
            Toast.makeText(this, "Video file not found to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(this,
                getPackageName() + ".provider", videoFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/mp4");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.putExtra(Intent.EXTRA_TEXT, caption);
            intent.setPackage("com.zhiliaoapp.musically");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Open TikTok or standard share sheet
                Intent chooser = Intent.createChooser(new Intent(Intent.ACTION_SEND)
                    .setType("video/mp4")
                    .putExtra(Intent.EXTRA_STREAM, contentUri)
                    .putExtra(Intent.EXTRA_TEXT, caption)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Post Video to TikTok");
                startActivity(chooser);
            }
        } catch (Exception e) {
            Log.e(TAG, "TikTok share error: " + e.getMessage());
            Toast.makeText(this, "Could not open TikTok directly: " + e.getMessage(), Toast.LENGTH_LONG).show();
            shareToAnyApp();
        }
    }

    private void shareToYouTube() {
        String caption = etCaptionOutput.getText().toString().trim();
        copyToClipboard(caption, "✅ YouTube Title/Description copied to clipboard!");

        if (videoFile == null || !videoFile.exists()) {
            Toast.makeText(this, "Video file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(this,
                getPackageName() + ".provider", videoFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/mp4");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Screen Recording with H.E.N.R.Y.");
            intent.putExtra(Intent.EXTRA_TEXT, caption);
            intent.setPackage("com.google.android.youtube");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Intent chooser = Intent.createChooser(new Intent(Intent.ACTION_SEND)
                    .setType("video/mp4")
                    .putExtra(Intent.EXTRA_STREAM, contentUri)
                    .putExtra(Intent.EXTRA_TEXT, caption)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Post to YouTube Shorts");
                startActivity(chooser);
            }
        } catch (Exception e) {
            Log.e(TAG, "YouTube share error: " + e.getMessage());
            shareToAnyApp();
        }
    }

    private void shareToInstagram() {
        String caption = etCaptionOutput.getText().toString().trim();
        copyToClipboard(caption, "✅ Caption & hashtags copied for Instagram!");

        if (videoFile == null || !videoFile.exists()) {
            Toast.makeText(this, "Video file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(this,
                getPackageName() + ".provider", videoFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/mp4");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.putExtra(Intent.EXTRA_TEXT, caption);
            intent.setPackage("com.instagram.android");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                shareToAnyApp();
            }
        } catch (Exception e) {
            shareToAnyApp();
        }
    }

    private void shareToAnyApp() {
        if (videoFile == null || !videoFile.exists()) {
            Toast.makeText(this, "Video file not available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String caption = etCaptionOutput.getText().toString().trim();
            Uri contentUri = FileProvider.getUriForFile(this,
                getPackageName() + ".provider", videoFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("video/mp4");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, caption);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Screen Recording"));
        } catch (Exception e) {
            Toast.makeText(this, "Share error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openInGallery() {
        if (videoFile == null || !videoFile.exists()) {
            Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri contentUri = FileProvider.getUriForFile(this,
                getPackageName() + ".provider", videoFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "video/mp4");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open video player: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMicAudioTrack() {
        tempAudioFile = MicAudioRecordService.getTempAudioFile();
        if (tempAudioFile == null || !tempAudioFile.exists() || tempAudioFile.length() == 0) {
            if (cardMicAudioTrack != null) {
                cardMicAudioTrack.setVisibility(View.GONE);
            }
            return;
        }

        if (cardMicAudioTrack != null) {
            cardMicAudioTrack.setVisibility(View.VISIBLE);
        }

        if (tvTempAudioPath != null) {
            long sizeKb = tempAudioFile.length() / 1024;
            tvTempAudioPath.setText(tempAudioFile.getName() + " (" + sizeKb + " KB)");
        }

        if (btnPlayAudioTrack != null) {
            btnPlayAudioTrack.setOnClickListener(v -> {
                if (isAudioPlaying) {
                    if (audioPlayer != null && audioPlayer.isPlaying()) {
                        audioPlayer.pause();
                    }
                    isAudioPlaying = false;
                    btnPlayAudioTrack.setText("▶ Listen to Mic Audio");
                } else {
                    try {
                        if (audioPlayer == null) {
                            audioPlayer = new MediaPlayer();
                            audioPlayer.setDataSource(tempAudioFile.getAbsolutePath());
                            audioPlayer.prepare();
                            audioPlayer.setOnCompletionListener(mp -> {
                                isAudioPlaying = false;
                                btnPlayAudioTrack.setText("▶ Listen to Mic Audio");
                            });
                        }
                        audioPlayer.start();
                        isAudioPlaying = true;
                        btnPlayAudioTrack.setText("⏸ Pause Mic Audio");
                    } catch (Exception e) {
                        Log.e(TAG, "Audio playback error: " + e.getMessage());
                        Toast.makeText(this, "Could not play audio track", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnShareAudioTrack != null) {
            btnShareAudioTrack.setOnClickListener(v -> {
                try {
                    Uri contentUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".provider", tempAudioFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("audio/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Microphone Commentary Track");
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Export Mic Audio"));
                } catch (Exception e) {
                    Toast.makeText(this, "Export audio error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (audioPlayer != null) {
            try {
                if (audioPlayer.isPlaying()) {
                    audioPlayer.stop();
                }
                audioPlayer.release();
            } catch (Exception ignored) {}
            audioPlayer = null;
        }
        super.onDestroy();
    }

    private void copyToClipboard(String text, String successMsg) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            ClipData clip = ClipData.newPlainText("HENRY Caption", text);
            cm.setPrimaryClip(clip);
            Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
        }
    }
}
