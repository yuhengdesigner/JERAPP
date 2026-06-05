package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<AlertModel> historyList = new ArrayList<>();
    private TextView emptyTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false); // Make sure you have a layout with a RecyclerView
        recyclerView = view.findViewById(R.id.historyRecyclerView);
        emptyTextView = view.findViewById(R.id.tvEmptyHistory); // Optional empty indicator layout view

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(historyList, alert -> {
            Intent intent = new Intent(getActivity(), UserHistoryDetailActivity.class);
            intent.putExtra("alert_key", alert.getKey());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadUserHistory();
        return view;
    }

    private void loadUserHistory() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance().getReference("UserHistory").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            // ADD THIS CHECK: Only attempt to convert if it's an Object (Map)
                            if (ds.getValue() instanceof java.util.Map) {
                                AlertModel alert = ds.getValue(AlertModel.class);
                                if (alert != null) {
                                    String status = alert.getStatus();
                                    // Only add if it is NOT completed
                                    if (!"Completed".equalsIgnoreCase(status) && !"Resolved".equalsIgnoreCase(status)) {
                                        alert.setKey(ds.getKey());
                                        historyList.add(alert);
                                    }
                                }
                            } else {
                                // Log the error instead of crashing
                                Log.e("HistoryFragment", "Skipping non-object data at: " + ds.getKey());
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (emptyTextView != null) {
                            emptyTextView.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HISTORY_ERROR", error.getMessage());
                    }
                });
    }
}