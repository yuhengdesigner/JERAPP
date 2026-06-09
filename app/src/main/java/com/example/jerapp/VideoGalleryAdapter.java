package com.example.jerapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VideoGalleryAdapter extends RecyclerView.Adapter<VideoGalleryAdapter.ViewHolder> {
    private List<String> urls;
    private OnVideoClickListener listener;

    public interface OnVideoClickListener {
        void onVideoClick(String url);
    }

    public VideoGalleryAdapter(Context context, List<String> urls, OnVideoClickListener listener) {
        this.urls = urls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate default android simple list layout structure
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.btn.setText("Evidence Video " + (position + 1));
        holder.btn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoClick(urls.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return urls != null ? urls.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView btn;
        public ViewHolder(View v) {
            super(v);
            btn = v.findViewById(android.R.id.text1);
        }
    }

    public void updateData(List<String> newUrls) {
        this.urls = newUrls;
        notifyDataSetChanged(); // Tells the UI list to redraw elements live
    }
}