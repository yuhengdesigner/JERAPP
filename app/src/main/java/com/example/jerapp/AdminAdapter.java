package com.example.jerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

    private List<AlertModel> alertList;
    private OnAlertClickListener listener;

    public interface OnAlertClickListener {
        void onLocate(AlertModel alert);
        void onResolve(AlertModel alert, String alertKey); // Add alertKey to identify which one to delete
    }

    public AdminAdapter(List<AlertModel> alertList, OnAlertClickListener listener) {
        this.alertList = alertList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertModel alert = alertList.get(position);

        holder.userName.setText("Victim: " + alert.userName);
        holder.emergencyType.setText("Emergency: " + alert.emergencyType.toUpperCase());
        holder.assignedDept.setText("Assigned: " + alert.assignedDept);

        holder.btnLocate.setOnClickListener(v -> listener.onLocate(alert));
        holder.btnResolve.setOnClickListener(v ->
                listener.onResolve(alert, alertList.get(position).getKey())
        );
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View btnResolve;
        TextView userName, emergencyType, assignedDept;
        Button btnLocate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.adminAlertUser);
            emergencyType = itemView.findViewById(R.id.adminAlertType);
            assignedDept = itemView.findViewById(R.id.adminAlertDept);
            btnLocate = itemView.findViewById(R.id.btnViewOnMap);
        }
    }
}