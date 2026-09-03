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
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(true)
            .build();

    private final List<Message> items;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static String sanitizeImageUrl(String rawUrl) {
        if (rawUrl == null) return "";
        return rawUrl.replace("model=flux", "model=turbo")
                     .replace("&enhance=true", "")
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
            default:                     layout = R.layout.item_message;           break;
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MsgVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MsgVH h, int pos) {
        Message m = items.get(pos);

        if (m.type == Message.TYPE_TYPING) {
            if (h.tvMsg != null) h.tvMsg.setText("● ● ●");
            return;
        }

        if (m.type == Message.TYPE_IMAGE) {
            if (h.ivImage != null && m.imageUri != null) {
                h.ivImage.setImageURI(Uri.parse(m.imageUri));
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
                                    okhttp3.Request req = new okhttp3.Request.Builder()
                                            .url(url)
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
                                    Thread.sleep(800);
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

    static class MsgVH extends RecyclerView.ViewHolder {
        TextView    tvMsg, tvAvatar;
        ImageView   ivImage;
        ProgressBar progressBar;

        MsgVH(View v) {
            super(v);
            tvMsg       = v.findViewById(R.id.tv_message);
            tvAvatar    = v.findViewById(R.id.tv_avatar);
            ivImage     = v.findViewById(R.id.iv_image);
            progressBar = v.findViewById(R.id.pb_loading);
        }
    }
}
