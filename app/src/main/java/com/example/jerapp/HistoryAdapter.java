package com.example.jerapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<AlertModel> historyList;
    private Context context;

    public HistoryAdapter(Context context, List<AlertModel> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertModel alert = historyList.get(position);

        // Emergency Type
        String type = (alert.getEmergencyType() != null) ? alert.getEmergencyType().toUpperCase() : "EMERGENCY";
        holder.tvType.setText(type);

        // Department Name (Using the getter added to AlertModel)
        String deptName = (alert.getDeptName() != null) ? alert.getDeptName() : "Unknown Department";
        holder.tvDept.setText("Responded by: " + deptName);

        // Date Formatting
        if (alert.getTimestamp() > 0) {
            String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(alert.getTimestamp()));
            holder.tvDate.setText(date);
        } else {
            holder.tvDate.setText("Date unavailable");
        }

        holder.tvStatus.setText(alert.getStatus()); // e.g., "RESOLVED"

        // Handle card click to open detail view
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HistoryDetailActivity.class);
            // Pass the unique key to the detail activity
            intent.putExtra("alert_key", alert.getKey());
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDept, tvDate, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.histType);
            tvDept = itemView.findViewById(R.id.histDept);
            tvDate = itemView.findViewById(R.id.histDate);
            tvStatus = itemView.findViewById(R.id.histStatus);
        }
    }
}