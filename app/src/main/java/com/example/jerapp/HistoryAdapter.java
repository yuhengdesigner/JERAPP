package com.example.jerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<AlertModel> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AlertModel alert);
    }

    public HistoryAdapter(List<AlertModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertModel alert = list.get(position);

        holder.histType.setText(alert.getEmergencyType() != null ? alert.getEmergencyType().toUpperCase() : "EMERGENCY");
        holder.histDept.setText("Responded by: " + (alert.getDeptName() != null ? alert.getDeptName() : "Unknown Department"));
        holder.histStatus.setText(alert.getStatus() != null ? alert.getStatus().toUpperCase() : "PENDING");

        // REPLACE YOUR OLD TIMESTAMP HOLDER LOGIC IN HistoryAdapter.java WITH THIS:
        Object rawTime = alert.getTimestamp();
        if (rawTime != null) {
            try {
                long timeLong = 0;

                if (rawTime instanceof Long) {
                    timeLong = (Long) rawTime;
                } else if (rawTime instanceof Double) {
                    timeLong = ((Double) rawTime).longValue();
                } else if (rawTime instanceof String) {
                    timeLong = Long.parseLong((String) rawTime);
                }

                if (timeLong > 0) {
                    String formatted = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                            .format(new java.util.Date(timeLong));
                    holder.histDate.setText(formatted);
                } else {
                    holder.histDate.setText("Time unavailable");
                }
            } catch (Exception e) {
                // If it's already written as a human-readable string in an old record, print it safely
                holder.histDate.setText(rawTime.toString());
            }
        } else {
            holder.histDate.setText("Time unavailable");
        }

        // Apply visual colors on Status elements
        String status = alert.getStatus();
        if ("Resolved".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            holder.histStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else if ("Processing".equalsIgnoreCase(status)) {
            holder.histStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"));
        } else {
            holder.histStatus.setTextColor(android.graphics.Color.parseColor("#F44336"));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(alert));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView histType, histDept, histDate, histStatus;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            histType = itemView.findViewById(R.id.histType);
            histDept = itemView.findViewById(R.id.histDept);
            histDate = itemView.findViewById(R.id.histDate);
            histStatus = itemView.findViewById(R.id.histStatus);
        }
    }
}