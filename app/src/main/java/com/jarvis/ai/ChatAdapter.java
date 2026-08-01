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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

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
                h.tvMsg.setText(m.text);
                h.tvMsg.setVisibility(View.VISIBLE);
            } else if (h.tvMsg != null) {
                h.tvMsg.setVisibility(View.GONE);
            }
            return;
        }

        if (m.type == Message.TYPE_URL_IMAGE) {
            if (h.tvMsg != null) {
                h.tvMsg.setText("Here is your generated image, sir.");
            }
            if (h.ivImage != null && m.imageUrl != null) {
                h.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
                final String url = m.imageUrl;
                final ImageView iv = h.ivImage;
                new Thread(() -> {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(30000);
                        conn.connect();
                        InputStream is = conn.getInputStream();
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        is.close();
                        if (bmp != null) {
                            mainHandler.post(() -> iv.setImageBitmap(bmp));
                        }
                    } catch (Exception ignored) {}
                }).start();
                h.ivImage.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    v.getContext().startActivity(intent);
                });
            }
            return;
        }

        if (h.tvMsg != null) h.tvMsg.setText(m.text);
        if (h.tvAvatar != null) {
            h.tvAvatar.setText(m.type == Message.TYPE_USER ? "YOU" : "AI");
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class MsgVH extends RecyclerView.ViewHolder {
        TextView  tvMsg, tvAvatar;
        ImageView ivImage;

        MsgVH(View v) {
            super(v);
            tvMsg    = v.findViewById(R.id.tv_message);
            tvAvatar = v.findViewById(R.id.tv_avatar);
            ivImage  = v.findViewById(R.id.iv_image);
        }
    }
}
