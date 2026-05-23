package com.example.jerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import android.content.Intent;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

    private List<AlertModel> alertList;
    private OnAlertClickListener listener;

    public interface OnAlertClickListener {
        void onDetailsClick(AlertModel alert);
        void onResolve(AlertModel alert, String alertKey);
        void onReceive(AlertModel alert);
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
        AlertModel alert = alertList.get(position);

        // Using the getters from the AlertModel
        holder.tvVictimName.setText(alert.getUserName());
        holder.tvVictimGender.setText("⚧ Gender: " + alert.getGender());
        holder.tvEmergencyTag.setText(alert.getEmergencyType().toUpperCase());
        holder.tvVictimPhone.setText("📞 " + alert.getUserPhone());
        holder.tvVictimEmail.setText("✉️ " + alert.getUserEmail());
        holder.tvVictimAddress.setText("📍 " + alert.getTextAddress());
        holder.tvCoordinates.setText("Lat: " + alert.userLat + " | Lng: " + alert.userLng);

        // Buttons
        holder.btnReceive.setOnClickListener(v -> listener.onReceive(alert));
        holder.btnResolve.setOnClickListener(v -> listener.onResolve(alert, alert.getKey()));

        // Inside AdminAdapter.java, onBindViewHolder:
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EmergencyDetailActivity.class);
            intent.putExtra("alert_key", alert.getKey());
            intent.putExtra("dept_id", alert.getAssignedDept()); // Pass this!
            v.getContext().startActivity(intent);
        });

        // 2. Button Logic based on Status
        String status = alert.getStatus() != null ? alert.getStatus() : "Pending";

        // UI Logic
        if ("Resolved".equals(status)) {
            // UI for Resolved
            holder.btnReceive.setVisibility(View.GONE);
            holder.btnResolve.setText("Resolved");
            holder.btnResolve.setEnabled(false);
            holder.btnResolve.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
        }
        else if ("Processing".equals(status)) {
            // UI for Processing
            holder.btnReceive.setVisibility(View.VISIBLE);
            holder.btnReceive.setEnabled(false);
            holder.btnReceive.setText("Received");

            holder.btnResolve.setVisibility(View.VISIBLE);
            holder.btnResolve.setEnabled(true);
            holder.btnResolve.setText("Resolve");
            // Set to your green color or default
            holder.btnResolve.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#388E3C")));
        }
        else {
            // UI for Pending
            holder.btnReceive.setVisibility(View.VISIBLE);
            holder.btnReceive.setEnabled(true);
            holder.btnReceive.setText("Receive");
            holder.btnReceive.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D32F2F")));

            holder.btnResolve.setVisibility(View.VISIBLE);
            holder.btnResolve.setEnabled(false); // Can't resolve until received
            holder.btnResolve.setText("Resolve");
            holder.btnResolve.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
        }

        // Bind Timestamp
        if (alert.getTimestamp() > 0) {
            String dateString = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date(alert.getTimestamp()));
            holder.tvTimestamp.setText("🕒 " + dateString);
        } else {
            holder.tvTimestamp.setText("🕒 Time unavailable");
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
        }
    }
}