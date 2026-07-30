package com.jarvis.ai;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MsgVH> {

    private final List<Message> items;

    public ChatAdapter(List<Message> items) { this.items = items; }

    @Override public int getItemViewType(int pos) { return items.get(pos).type; }

    @NonNull @Override
    public MsgVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        switch (viewType) {
            case Message.TYPE_USER:  layout = R.layout.item_message_user;  break;
            case Message.TYPE_IMAGE: layout = R.layout.item_message_image; break;
            default:                 layout = R.layout.item_message;       break;
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
            if (h.ivImage != null && m.imageUri != null)
                h.ivImage.setImageURI(Uri.parse(m.imageUri));
            if (h.tvMsg != null) {
                if (m.text != null && !m.text.isEmpty()) {
                    h.tvMsg.setText(m.text);
                    h.tvMsg.setVisibility(View.VISIBLE);
                } else {
                    h.tvMsg.setVisibility(View.GONE);
                }
            }
            return;
        }

        if (h.tvMsg != null) h.tvMsg.setText(m.text);
        if (h.tvAvatar != null)
            h.tvAvatar.setText(m.type == Message.TYPE_USER ? "YOU" : "AI");
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
