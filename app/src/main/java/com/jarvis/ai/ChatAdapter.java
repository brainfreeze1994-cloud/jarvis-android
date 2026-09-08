package com.jarvis.ai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MsgVH> {

    private static final okhttp3.OkHttpClient IMAGE_CLIENT = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(18, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(true)
            .build();

    private final List<Message> items;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static String sanitizeImageUrl(String rawUrl) {
        if (rawUrl == null) return "";
        String s = rawUrl;
        // Replace slow flux or broken turbo with active fast sana model
        s = s.replace("model=flux", "model=sana")
             .replace("model=turbo", "model=sana");
        if (!s.contains("model=")) {
            s += (s.contains("?") ? "&" : "?") + "model=sana";
        }
        if (!s.contains("width=")) {
            s += "&width=512&height=512";
        }
        return s.replace("&enhance=true", "")
                .replace("?enhance=true&", "?")
                .replace("?enhance=true", "");
    }

    public ChatAdapter(List<Message> items) { this.items = items; }

    @Override public int getItemViewType(int pos) { return items.get(pos).type; }

    @NonNull @Override
    public MsgVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        switch (viewType) {
            case Message.TYPE_USER:      layout = R.layout.item_message_user;      break;
            case Message.TYPE_IMAGE:     layout = R.layout.item_message_image;     break;
            case Message.TYPE_URL_IMAGE: layout = R.layout.item_message_url_image; break;
            case Message.TYPE_FILE_CARD: layout = R.layout.item_message_file_card; break;
            default:                     layout = R.layout.item_message;           break;
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MsgVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MsgVH h, int pos) {
        Message m = items.get(pos);

        if (m.type == Message.TYPE_TYPING) {
            if (h.tvMsg != null) h.tvMsg.setVisibility(View.GONE);
            if (h.lottieTyping != null) {
                h.lottieTyping.setVisibility(View.VISIBLE);
                h.lottieTyping.playThinkingDots();
            } else if (h.tvMsg != null) {
                h.tvMsg.setVisibility(View.VISIBLE);
                h.tvMsg.setText("● ● ●");
            }
            return;
        } else {
            if (h.lottieTyping != null) {
                h.lottieTyping.stopAndReset();
                h.lottieTyping.setVisibility(View.GONE);
            }
        }

        if (m.type == Message.TYPE_USER) {
            if (h.tvMsg != null) {
                if (m.text != null && !m.text.isEmpty()) {
                    h.tvMsg.setText(m.text);
                    h.tvMsg.setVisibility(View.VISIBLE);
                } else {
                    h.tvMsg.setVisibility(View.GONE);
                }
            }
            if (h.containerUserImages != null && h.scrollUserImages != null) {
                h.containerUserImages.removeAllViews();
                List<String> uris = m.imageUris;
                if (uris != null && !uris.isEmpty()) {
                    h.scrollUserImages.setVisibility(View.VISIBLE);
                    int densityDp = (int) Math.max(1, h.itemView.getResources().getDisplayMetrics().density);
                    for (String uStr : uris) {
                        if (uStr == null || uStr.trim().isEmpty()) continue;
                        ImageView iv = new ImageView(h.itemView.getContext());
                        int sizeW = uris.size() == 1 ? (int)(220 * densityDp) : (int)(150 * densityDp);
                        int sizeH = uris.size() == 1 ? (int)(180 * densityDp) : (int)(120 * densityDp);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizeW, sizeH);
                        lp.setMarginEnd(8 * densityDp);
                        iv.setLayoutParams(lp);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iv.setBackgroundResource(R.drawable.bg_bubble_user);
                        iv.setClipToOutline(true);
                        try {
                            if (uStr.startsWith("data:image")) {
                                String b64 = uStr.substring(uStr.indexOf(',') + 1);
                                byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                iv.setImageBitmap(bmp);
                            } else {
                                iv.setImageURI(Uri.parse(uStr));
                            }
                        } catch (Exception ignored) {}
                        final String clickUri = uStr;
                        iv.setOnClickListener(v -> showFullScreenImage(h.itemView.getContext(), clickUri));
                        h.containerUserImages.addView(iv);
                    }
                } else {
                    h.scrollUserImages.setVisibility(View.GONE);
                }
            }
            return;
        }

        if (m.type == Message.TYPE_IMAGE) {
            if (h.ivImage != null && m.imageUri != null) {
                try {
                    if (m.imageUri.startsWith("data:image")) {
                        String b64 = m.imageUri.substring(m.imageUri.indexOf(',') + 1);
                        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        h.ivImage.setImageBitmap(bmp);
                    } else {
                        h.ivImage.setImageURI(Uri.parse(m.imageUri));
                    }
                } catch (Exception ignored) {}
                final String clickUri = m.imageUri;
                h.ivImage.setOnClickListener(v -> showFullScreenImage(h.itemView.getContext(), clickUri));
            }
            if (h.tvMsg != null && m.text != null && !m.text.isEmpty()) {
                h.tvMsg.setText(stripMarkdown(m.text));
                h.tvMsg.setVisibility(View.VISIBLE);
            } else if (h.tvMsg != null) {
                h.tvMsg.setVisibility(View.GONE);
            }
            return;
        }

        if (m.type == Message.TYPE_URL_IMAGE) {
            // Caption
            if (h.tvMsg != null) {
                String caption = (m.text != null && !m.text.isEmpty()) ? stripMarkdown(m.text) : "Here is your generated image, sir.";
                h.tvMsg.setText(caption);
            }
            // Loading spinner visible until image loads
            if (h.progressBar != null) h.progressBar.setVisibility(View.VISIBLE);
            if (h.ivImage != null) h.ivImage.setVisibility(View.INVISIBLE);

            if (h.ivImage != null && m.imageUrl != null) {
                final String rawUrl = m.imageUrl;
                final String url = sanitizeImageUrl(rawUrl);
                final ImageView iv = h.ivImage;
                final ProgressBar pb = h.progressBar;
                final View hintView = h.itemView.findViewById(R.id.tv_hint);
                iv.setTag(url);

                new Thread(() -> {
                    Bitmap bmp = null;
                    try {
                        if (url.startsWith("data:image")) {
                            String b64 = url.substring(url.indexOf(',') + 1);
                            byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                            bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        } else {
                            for (int attempt = 0; attempt < 2 && bmp == null; attempt++) {
                                try {
                                    String targetFetchUrl = (attempt == 0) ? url : url.replace("model=sana", "").replace("&&", "&");
                                    okhttp3.Request req = new okhttp3.Request.Builder()
                                            .url(targetFetchUrl)
                                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                                            .header("Accept", "image/jpeg,image/png,image/webp,image/*;q=0.8")
                                            .build();
                                    try (okhttp3.Response resp = IMAGE_CLIENT.newCall(req).execute()) {
                                        if (resp.isSuccessful() && resp.body() != null) {
                                            byte[] bytes = resp.body().bytes();
                                            if (bytes.length > 0) {
                                                bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                            }
                                        }
                                    }
                                } catch (Exception ex) {
                                    Thread.sleep(400);
                                }
                            }
                        }
                        final Bitmap finalBmp = bmp;
                        mainHandler.post(() -> {
                            if (!url.equals(iv.getTag())) return;
                            if (pb != null) pb.setVisibility(View.GONE);
                            if (finalBmp != null) {
                                iv.setImageBitmap(finalBmp);
                                iv.setVisibility(View.VISIBLE);
                                if (hintView != null) hintView.setVisibility(View.VISIBLE);
                            } else {
                                iv.setVisibility(View.VISIBLE);
                                iv.setImageResource(android.R.drawable.ic_menu_gallery);
                                if (h.tvMsg != null) h.tvMsg.setText("Tap image to view in browser.");
                                if (hintView != null) hintView.setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (!url.equals(iv.getTag())) return;
                            if (pb != null) pb.setVisibility(View.GONE);
                            iv.setVisibility(View.VISIBLE);
                            iv.setImageResource(android.R.drawable.ic_menu_gallery);
                            if (h.tvMsg != null) h.tvMsg.setText("Tap image to view in browser.");
                            if (hintView != null) hintView.setVisibility(View.VISIBLE);
                        });
                    }
                }).start();

                // Tap to open in browser
                if (!url.startsWith("data:")) {
                    h.ivImage.setOnClickListener(v2 -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            v2.getContext().startActivity(intent);
                        } catch (Exception ignored) {}
                    });
                }
            }
            return;
        }

        if (m.type == Message.TYPE_FILE_CARD) {
            if (h.tvFileIcon != null && m.fileIcon != null) h.tvFileIcon.setText(m.fileIcon);
            if (h.tvFileTitle != null && m.fileTitle != null) h.tvFileTitle.setText(m.fileTitle);
            if (h.tvFileBadge != null && m.fileBadge != null) h.tvFileBadge.setText(m.fileBadge);
            if (h.tvFileDetails != null && m.fileDetails != null) h.tvFileDetails.setText(m.fileDetails);
            if (h.tvAvatar != null) h.tvAvatar.setText("HNR");

            if (h.btnOpenFile != null && m.filePath != null) {
                h.btnOpenFile.setOnClickListener(v -> {
                    HenryFileEngine.openFile(v.getContext(), new java.io.File(m.filePath), m.fileMimeType);
                });
            }
            if (h.btnShareFile != null && m.filePath != null) {
                h.btnShareFile.setOnClickListener(v -> {
                    HenryFileEngine.shareFile(v.getContext(), new java.io.File(m.filePath), m.fileMimeType, m.fileTitle);
                });
            }
            return;
        }

        // Normal JARVIS or USER message — strip markdown for clean display
        if (h.tvMsg != null) {
            String displayText = (m.type == Message.TYPE_USER) ? m.text : stripMarkdown(m.text);
            h.tvMsg.setText(displayText);
        }
        if (h.tvAvatar != null) {
            h.tvAvatar.setText(m.type == Message.TYPE_USER ? "YOU" : "HNR");
        }
    }

    @Override public int getItemCount() { return items.size(); }

    /**
     * Strip common Markdown so Android TextView shows clean text.
     * Bold, italic, headers, code blocks, bullet lists, etc.
     */
    static String stripMarkdown(String text) {
        if (text == null) return "";
        return text
            // Code blocks (```...```)
            .replaceAll("```[\\s\\S]*?```", "[code]")
            // Inline code (`...`)
            .replaceAll("`([^`]+)`", "$1")
            // Headers (## Title → Title)
            .replaceAll("(?m)^#{1,6}\\s+", "")
            // Bold + italic (***text*** or ___text___)
            .replaceAll("\\*{3}(.+?)\\*{3}", "$1")
            .replaceAll("_{3}(.+?)_{3}", "$1")
            // Bold (**text** or __text__)
            .replaceAll("\\*{2}(.+?)\\*{2}", "$1")
            .replaceAll("_{2}(.+?)_{2}", "$1")
            // Italic (*text* or _text_)
            .replaceAll("(?<![\\*_])\\*(.+?)\\*(?![\\*_])", "$1")
            .replaceAll("(?<![\\*_])_(.+?)_(?![\\*_])", "$1")
            // Strikethrough (~~text~~)
            .replaceAll("~~(.+?)~~", "$1")
            // Links ([text](url)) → text
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
            // Images (![alt](url)) → [image]
            .replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "[image]")
            // Blockquotes (> text)
            .replaceAll("(?m)^>\\s*", "")
            // Bullet lists (- item or * item or + item)
            .replaceAll("(?m)^\\s*[-*+]\\s+", "• ")
            // Numbered lists (1. item)
            .replaceAll("(?m)^\\s*\\d+\\.\\s+", "• ")
            // Horizontal rules (---, ***, ___)
            .replaceAll("(?m)^([-*_]){3,}\\s*$", "─────")
            // Trailing spaces from markdown line breaks
            .replaceAll("  +$", "")
            .trim();
    }

    private void showFullScreenImage(android.content.Context ctx, String uriStr) {
        if (ctx == null || uriStr == null) return;
        try {
            android.app.Dialog d = new android.app.Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            android.widget.ImageView iv = new android.widget.ImageView(ctx);
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (uriStr.startsWith("data:image")) {
                String b64 = uriStr.substring(uriStr.indexOf(',') + 1);
                byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                iv.setImageBitmap(bmp);
            } else {
                iv.setImageURI(Uri.parse(uriStr));
            }
            iv.setOnClickListener(v -> d.dismiss());
            d.setContentView(iv);
            d.show();
        } catch (Exception ignored) {}
    }

    static class MsgVH extends RecyclerView.ViewHolder {
        TextView    tvMsg, tvAvatar;
        HenryLottieAnimationView lottieTyping;
        ImageView   ivImage;
        ProgressBar progressBar;
        View        scrollUserImages;
        ViewGroup   containerUserImages;

        // File Card Views
        TextView    tvFileIcon, tvFileTitle, tvFileBadge, tvFileDetails;
        TextView    btnOpenFile, btnShareFile;

        MsgVH(View v) {
            super(v);
            tvMsg       = v.findViewById(R.id.tv_message);
            tvAvatar    = v.findViewById(R.id.tv_avatar);
            lottieTyping = v.findViewById(R.id.lottie_typing);
            ivImage     = v.findViewById(R.id.iv_image);
            progressBar = v.findViewById(R.id.pb_loading);
            scrollUserImages    = v.findViewById(R.id.scroll_user_images);
            containerUserImages = v.findViewById(R.id.container_user_images);

            tvFileIcon    = v.findViewById(R.id.tv_file_icon);
            tvFileTitle   = v.findViewById(R.id.tv_file_title);
            tvFileBadge   = v.findViewById(R.id.tv_file_badge);
            tvFileDetails = v.findViewById(R.id.tv_file_details);
            btnOpenFile   = v.findViewById(R.id.btn_open_file);
            btnShareFile  = v.findViewById(R.id.btn_share_file);
        }
    }
}
