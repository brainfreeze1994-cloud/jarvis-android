package com.jarvis.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        int layout = (viewType == Message.TYPE_USER)
            ? R.layout.item_message_user : R.layout.item_message;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MsgVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MsgVH h, int pos) {
        Message m = items.get(pos);
        h.tvMsg.setText(m.type == Message.TYPE_TYPING ? "● ● ●" : m.text);
        if (h.tvAvatar != null)
            h.tvAvatar.setText(m.type == Message.TYPE_USER ? "YOU" : "AI");
    }

    @Override public int getItemCount() { return items.size(); }

    static class MsgVH extends RecyclerView.ViewHolder {
        TextView tvMsg, tvAvatar;
        MsgVH(View v) {
            super(v);
            tvMsg    = v.findViewById(R.id.tv_message);
            tvAvatar = v.findViewById(R.id.tv_avatar);
        }
    }
}
