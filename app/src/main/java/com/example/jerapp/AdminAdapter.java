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
        void onLocate(AlertModel alert);
        void onConfirmArrival(AlertModel alert);
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

        holder.tvVictimName.setText(alert.getUserName());
        holder.tvEmergencyTag.setText(alert.getEmergencyType().toUpperCase());
        holder.tvVictimPhone.setText("Phone: " + alert.getUserPhone());
        holder.tvVictimEmail.setText("Email: " + alert.userEmail);
        holder.tvVictimAddress.setText("Location: " + alert.textAddress);
        holder.tvCoordinates.setText("Lat: " + alert.userLat + " | Lng: " + alert.userLng);
        holder.btnArrived.setOnClickListener(v -> listener.onConfirmArrival(alert));

        // Inside AdminAdapter's onBindViewHolder
        if (listener instanceof AdminHistoryFragment) {
            holder.btnResolveAlert.setVisibility(View.GONE); // Hide resolve for history
        } else {
            holder.btnResolveAlert.setVisibility(View.VISIBLE); // Show for active alerts
        }

        // Click listeners using your new button IDs
        holder.btnViewDetails.setOnClickListener(v -> listener.onDetailsClick(alert));

        holder.btnResolveAlert.setOnClickListener(v ->
                listener.onResolve(alert, alert.getKey())
        );

        // If you want the whole card to open details:
        holder.itemView.setOnClickListener(v -> listener.onDetailsClick(alert));
        holder.btnArrived.setOnClickListener(v -> listener.onConfirmArrival(alert));
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // 1. Add btnArrived here
        TextView tvVictimName, tvEmergencyTag, tvVictimPhone, tvVictimEmail, tvVictimAddress, tvCoordinates;
        MaterialButton btnResolveAlert, btnViewDetails, btnArrived;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVictimName = itemView.findViewById(R.id.tvVictimName);
            tvEmergencyTag = itemView.findViewById(R.id.tvEmergencyTag);
            tvVictimPhone = itemView.findViewById(R.id.tvVictimPhone);
            tvVictimEmail = itemView.findViewById(R.id.tvVictimEmail);
            tvVictimAddress = itemView.findViewById(R.id.tvVictimAddress);
            btnResolveAlert = itemView.findViewById(R.id.btnResolveAlert);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            tvCoordinates = itemView.findViewById(R.id.tvCoordinates);

            // 2. Link it to the XML ID
            btnArrived = itemView.findViewById(R.id.btnArrived);
        }
    }
}