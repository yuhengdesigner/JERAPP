package com.example.jerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {
    private List<String> urls;
    private OnVideoClickListener listener;

    public interface OnVideoClickListener { void onVideoClick(String url); }

    public VideoAdapter(List<String> urls, OnVideoClickListener listener) {
        this.urls = urls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setOnClickListener(v -> listener.onVideoClick(urls.get(position)));
    }

    @Override
    public int getItemCount() { return urls.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View v) { super(v); }
    }
}