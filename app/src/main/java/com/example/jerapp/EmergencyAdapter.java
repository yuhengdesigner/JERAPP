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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EmergencyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    private final List<EmergencyModel> emergencyList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(EmergencyModel model);
    }

    public EmergencyAdapter(List<EmergencyModel> emergencyList, OnItemClickListener listener) {
        this.emergencyList = emergencyList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emergency_card, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind();
        } else if (holder instanceof ItemViewHolder) {
            // Position 0 is header, so list index is position - 1
            EmergencyModel model = emergencyList.get(position - 1);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            itemHolder.title.setText(model.getTitle());
            itemHolder.icon.setImageResource(model.getIconRes());
            itemHolder.itemView.setOnClickListener(v -> listener.onItemClick(model));
        }
    }

    @Override
    public int getItemCount() {
        return emergencyList.size() + 1; // +1 for the header
    }

    // --- VIEW HOLDERS ---

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
                    textDay.setText(new SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.getTime()));
                    textDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime()));
                    textTime.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(cal.getTime()));
                    handler.postDelayed(this, 1000);
                }
            };
            handler.post(runnable);
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