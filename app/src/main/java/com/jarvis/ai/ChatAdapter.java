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

    private final List<Message> items;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
                final String url = m.imageUrl;
                final ImageView iv = h.ivImage;
                final ProgressBar pb = h.progressBar;

                new Thread(() -> {
                    try {
                        Bitmap bmp = null;
                        if (url.startsWith("data:image")) {
                            String b64 = url.substring(url.indexOf(',') + 1);
                            byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                            bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        } else {
                            // Try multiple times with increasing timeout
                            for (int attempt = 0; attempt < 3 && bmp == null; attempt++) {
                                try {
                                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                                    conn.setConnectTimeout(12000);
                                    conn.setReadTimeout(30000);
                                    conn.setInstanceFollowRedirects(true);
                                    conn.connect();
                                    if (conn.getResponseCode() == 200) {
                                        InputStream is = conn.getInputStream();
                                        bmp = BitmapFactory.decodeStream(is);
                                        is.close();
                                    }
                                } catch (Exception retryEx) {
                                    Thread.sleep(1500);
                                }
                            }
                        }
                        if (bmp != null) {
                            final Bitmap finalBmp = bmp;
                            mainHandler.post(() -> {
                                iv.setImageBitmap(finalBmp);
                                iv.setVisibility(View.VISIBLE);
                                if (pb != null) pb.setVisibility(View.GONE);
                            });
                        } else {
                            mainHandler.post(() -> {
                                if (pb != null) pb.setVisibility(View.GONE);
                                iv.setVisibility(View.VISIBLE);
                                iv.setImageResource(android.R.drawable.ic_menu_gallery);
                                if (h.tvMsg != null) h.tvMsg.setText("Image could not be loaded. Tap to open in browser.");
                            });
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (pb != null) pb.setVisibility(View.GONE);
                            iv.setVisibility(View.VISIBLE);
                            iv.setImageResource(android.R.drawable.ic_menu_gallery);
                            if (h.tvMsg != null) h.tvMsg.setText("Image failed to load. Tap to open in browser.");
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
