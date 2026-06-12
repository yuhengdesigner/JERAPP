package com.example.jerapp;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EmergencyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ONGOING = 1;
    public static final int TYPE_ITEM = 2;

    private final List<EmergencyModel> emergencyList;
    private List<AlertModel> ongoingAlerts;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(EmergencyModel model);
        void onOngoingClick(AlertModel alert);
    }

    public EmergencyAdapter(List<EmergencyModel> emergencyList, List<AlertModel> ongoingAlerts, OnItemClickListener listener) {
        this.emergencyList = emergencyList;
        this.ongoingAlerts = (ongoingAlerts != null) ? ongoingAlerts : new ArrayList<>();
        this.listener = listener;
    }

    public void setOngoingAlerts(List<AlertModel> alerts) {
        this.ongoingAlerts = (alerts != null) ? alerts : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        if (position <= ongoingAlerts.size()) return TYPE_ONGOING;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_dashboard_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == TYPE_ONGOING) {
            View view = inflater.inflate(R.layout.item_ongoing_emergency, parent, false);
            return new OngoingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_emergency_card, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind();
        } else if (holder instanceof OngoingViewHolder) {
            if (position - 1 < ongoingAlerts.size()) {
                AlertModel alert = ongoingAlerts.get(position - 1);
                ((OngoingViewHolder) holder).bind(alert, listener);
            }
        } else if (holder instanceof ItemViewHolder) {
            int gridPos = position - 1 - ongoingAlerts.size();
            if (gridPos >= 0 && gridPos < emergencyList.size()) {
                EmergencyModel model = emergencyList.get(gridPos);
                ItemViewHolder itemHolder = (ItemViewHolder) holder;
                itemHolder.title.setText(model.getTitle());
                itemHolder.icon.setImageResource(model.getIconRes());
                itemHolder.itemView.setOnClickListener(v -> listener.onItemClick(model));
            }
        }
    }

    @Override
    public int getItemCount() {
        return 1 + ongoingAlerts.size() + (emergencyList != null ? emergencyList.size() : 0);
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDay, textDate, textTime;
        private final Handler handler = new Handler();
        private Runnable runnable;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.textCurrentDay);
            textDate = itemView.findViewById(R.id.textCurrentDate);
            textTime = itemView.findViewById(R.id.textCurrentTime);
        }

        public void bind() {
            if (runnable != null) handler.removeCallbacks(runnable);
            runnable = new Runnable() {
                @Override
                public void run() {
                    Calendar cal = Calendar.getInstance();
                    if (textDay != null) textDay.setText(new SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.getTime()));
                    if (textDate != null) textDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime()));
                    if (textTime != null) textTime.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(cal.getTime()));
                    handler.postDelayed(this, 1000);
                }
            };
            handler.post(runnable);
        }
    }

    public static class OngoingViewHolder extends RecyclerView.ViewHolder {
        TextView tvDept, tvStatus;
        public OngoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDept = itemView.findViewById(R.id.tvOngoingDeptName);
            tvStatus = itemView.findViewById(R.id.tvDashboardCountdown);
        }
        public void bind(AlertModel alert, OnItemClickListener listener) {
            tvDept.setText("Responding: " + (alert.getDeptName() != null ? alert.getDeptName() : "Department"));
            tvStatus.setText("Status: " + (alert.getStatus() != null ? alert.getStatus() : "Active"));
            itemView.setOnClickListener(v -> listener.onOngoingClick(alert));
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.cardIcon);
            title = itemView.findViewById(R.id.cardTitle);
        }
    }
}
