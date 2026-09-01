package com.jarvis.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying fetched NASA Near-Earth Asteroid data,
 * including asteroid name, close approach date, and estimated diameter/size.
 */
public class AsteroidAdapter extends RecyclerView.Adapter<AsteroidAdapter.AsteroidViewHolder> {

    public interface OnAsteroidClickListener {
        void onAsteroidClick(int position, AsteroidOrbitView.AsteroidOrbital asteroid);
    }

    private final List<AsteroidOrbitView.AsteroidOrbital> items = new ArrayList<>();
    private OnAsteroidClickListener clickListener;

    public AsteroidAdapter() {}

    public AsteroidAdapter(List<AsteroidOrbitView.AsteroidOrbital> list, OnAsteroidClickListener listener) {
        if (list != null) {
            this.items.addAll(list);
        }
        this.clickListener = listener;
    }

    public void setAsteroids(List<AsteroidOrbitView.AsteroidOrbital> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setOnAsteroidClickListener(OnAsteroidClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public AsteroidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asteroid, parent, false);
        return new AsteroidViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AsteroidViewHolder holder, int position) {
        AsteroidOrbitView.AsteroidOrbital ast = items.get(position);
        holder.bind(ast, position, clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class AsteroidViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvDate;
        private final TextView tvSize;
        private final TextView tvDistance;
        private final TextView tvVelocity;
        private final TextView tvHazard;
        private final TextView tvIcon;

        public AsteroidViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName     = itemView.findViewById(R.id.item_ast_name);
            tvDate     = itemView.findViewById(R.id.item_ast_date);
            tvSize     = itemView.findViewById(R.id.item_ast_size);
            tvDistance = itemView.findViewById(R.id.item_ast_distance);
            tvVelocity = itemView.findViewById(R.id.item_ast_velocity);
            tvHazard   = itemView.findViewById(R.id.item_ast_hazard);
            tvIcon     = itemView.findViewById(R.id.item_ast_icon);
        }

        public void bind(AsteroidOrbitView.AsteroidOrbital ast, int position, OnAsteroidClickListener listener) {
            tvName.setText(ast.name);
            tvDate.setText(ast.closeApproachDate);

            // Estimated size with feet conversion
            float meters = ast.sizeMeters;
            float feet = meters * 3.28084f;
            tvSize.setText(String.format(Locale.US, "%.1f m (%.1f ft)", meters, feet));

            // Miss distance & velocity
            tvDistance.setText(String.format(Locale.US, "%,.0f km", ast.missDistanceKm));
            tvVelocity.setText(String.format(Locale.US, "%,.0f km/h", ast.velocityKmh));

            if (ast.isHazardous) {
                tvHazard.setText("⚠️ HAZARDOUS");
                tvHazard.setTextColor(0xFFFF4444);
                tvHazard.setBackgroundColor(0x20FF4444);
                tvIcon.setTextColor(0xFFFF4444);
            } else {
                tvHazard.setText("SAFE");
                tvHazard.setTextColor(0xFF00FF99);
                tvHazard.setBackgroundColor(0x1500FF99);
                tvIcon.setTextColor(0xFF00FF99);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAsteroidClick(getAdapterPosition(), ast);
                }
            });
        }
    }
}
