package com.example.jerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import android.util.Log;
import android.content.Intent;
import android.widget.ImageView;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

    private List<AlertModel> alertList;
    private OnAlertClickListener listener;

    public interface OnAlertClickListener {
        void onDetailsClick(AlertModel alert);
        void onResolve(AlertModel alert, String alertKey);
        void onReceive(AlertModel alert);
        void onDelete(AlertModel alert);
    }

    public AdminAdapter(List<AlertModel> alertList, OnAlertClickListener listener) {
        this.alertList = alertList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure this matches your new XML filename
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            AlertModel alert = alertList.get(position);


            String status = alert.getStatus();

            // Using the getters from the AlertModel
            holder.tvVictimName.setText(alert.getUserName() != null ? alert.getUserName() : "Unknown User");
            holder.tvVictimGender.setText("⚧ Gender: " + alert.getGender());
            holder.tvEmergencyTag.setText(alert.getEmergencyType().toUpperCase());
            holder.tvVictimPhone.setText("📞 " + (alert.getUserPhone() != null ? alert.getUserPhone() : "N/A"));
            holder.tvVictimEmail.setText("✉️ " + (alert.getUserEmail() != null ? alert.getUserEmail() : "N/A"));
            holder.tvVictimAddress.setText("📍 " + (alert.getTextAddress() != null ? alert.getTextAddress() : "N/A"));
            holder.tvCoordinates.setText("Lat: " + alert.getUserLat() + " | Lng: " + alert.getUserLng());

            // Buttons
            holder.btnReceive.setOnClickListener(v -> listener.onReceive(alert));
            holder.btnResolve.setOnClickListener(v -> listener.onResolve(alert, alert.getKey()));
            holder.ivDelete.setOnClickListener(v -> listener.onDelete(alert));

            // Inside AdminAdapter.java, onBindViewHolder:
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), EmergencyDetailActivity.class);
                intent.putExtra("alert_key", alert.getKey());
                intent.putExtra("dept_id", alert.getAssignedDept()); // Pass this!
                v.getContext().startActivity(intent);
            });

            // === CORRECTED TIMESTAMP PARSING FOR ADMIN ADAPTER ===
            Object rawTimestamp = alert.getTimestamp();

            if (rawTimestamp != null) {
                if (rawTimestamp instanceof Long) {
                    // Case A: The timestamp is a numerical Epoch millisecond value (Long)
                    long timeLong = (Long) rawTimestamp;
                    if (timeLong > 0) {
                        String dateString = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                                .format(new java.util.Date(timeLong));
                        holder.tvTimestamp.setText("🕒 " + dateString);
                    } else {
                        holder.tvTimestamp.setText("🕒 Time unavailable");
                    }
                } else if (rawTimestamp instanceof String) {
                    // Case B: The timestamp is already a formatted String (e.g., "2026-05-31 15:17:15...")
                    String timeString = (String) rawTimestamp;

                    // Clean up the string length if it contains nanoseconds/timezone traces
                    if (timeString.contains(".") && timeString.length() > 16) {
                        try {
                            timeString = timeString.substring(0, 16); // Shortens down to "2026-05-31 15:17"
                        } catch (Exception e) {
                            // Fallback to original if truncation fails
                        }
                    }
                    holder.tvTimestamp.setText("🕒 " + timeString);
                } else {
                    // Fallback for any other unexpected data formats
                    holder.tvTimestamp.setText("🕒 " + rawTimestamp.toString());
                }
            } else {
                holder.tvTimestamp.setText("🕒 Time unavailable");
            }
// ====================================================

            // Button Logic
            if ("Resolved".equals(status) || "Completed".equals(status)) {
                holder.btnReceive.setVisibility(View.GONE);
                holder.btnResolve.setVisibility(View.VISIBLE);
                holder.btnResolve.setText("Completed");
                holder.btnResolve.setEnabled(false);
                holder.btnResolve.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
            } else if ("Processing".equals(status)) {
                holder.btnReceive.setVisibility(View.GONE);
                holder.btnResolve.setVisibility(View.VISIBLE);
                holder.btnResolve.setText("Resolve");
                holder.btnResolve.setEnabled(true);
                holder.btnResolve.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#388E3C")));
            } else { // Pending
                holder.btnReceive.setVisibility(View.VISIBLE);
                holder.btnResolve.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e("BINDING_ERROR", "Error binding card at position " + position + ": " + e.getMessage());
            // Optionally hide this card if it's broken
            holder.itemView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // 1. Add btnArrived here
        TextView tvVictimName, tvEmergencyTag, tvVictimPhone, tvVictimEmail, tvVictimAddress, tvCoordinates, tvVictimGender, tvTimestamp;
        MaterialButton btnReceive, btnResolve;
        public ImageView ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVictimName = itemView.findViewById(R.id.tvVictimName);
            tvEmergencyTag = itemView.findViewById(R.id.tvEmergencyTag);
            tvVictimPhone = itemView.findViewById(R.id.tvVictimPhone);
            tvVictimEmail = itemView.findViewById(R.id.tvVictimEmail);
            tvVictimAddress = itemView.findViewById(R.id.tvVictimAddress);
            tvCoordinates = itemView.findViewById(R.id.tvCoordinates);
            tvVictimGender = itemView.findViewById(R.id.tvVictimGender);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnReceive = itemView.findViewById(R.id.btnReceive);
            btnResolve = itemView.findViewById(R.id.btnResolve);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
        private TextView findSafely(View parent, int id) {
            TextView tv = parent.findViewById(id);
            return tv; // Remove the Log.e if it's annoying, but keep the logic
        }
    }
}