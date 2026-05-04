package com.example.jerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EmergencyAdapter extends RecyclerView.Adapter<EmergencyAdapter.ViewHolder> {

    private List<EmergencyModel> emergencyList;
    private OnItemClickListener listener;

    // Interface for handling clicks in the Fragment/Activity
    public interface OnItemClickListener {
        void onItemClick(EmergencyModel model);
    }

    public EmergencyAdapter(List<EmergencyModel> emergencyList, OnItemClickListener listener) {
        this.emergencyList = emergencyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure R.layout.item_emergency_card matches your XML file name exactly
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emergency_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyModel model = emergencyList.get(position);

        if (model != null) {
            holder.title.setText(model.getTitle());
            holder.icon.setImageResource(model.getIconRes());

            // Handle the click
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(model);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return emergencyList != null ? emergencyList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match your item_emergency_card.xml
            icon = itemView.findViewById(R.id.cardIcon);
            title = itemView.findViewById(R.id.cardTitle);
        }
    }
}