package com.example.jerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

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
        holder.itemView.setOnClickListener(v -> listener.onDetailsClick(alert));

        // UI Logic
        if ("Processing".equals(alert.getStatus())) {
            holder.btnReceive.setEnabled(false);
            holder.btnReceive.setText("Received");
        } else {
            holder.btnReceive.setEnabled(true);
            holder.btnReceive.setText("Receive");
        }
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // 1. Add btnArrived here
        TextView tvVictimName, tvEmergencyTag, tvVictimPhone, tvVictimEmail, tvVictimAddress, tvCoordinates, tvVictimGender;
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
            btnReceive = itemView.findViewById(R.id.btnReceive);
            btnResolve = itemView.findViewById(R.id.btnResolve);
        }
    }
}