package com.example.jerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter; // Defined the adapter type
    private List<AlertModel> historyList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = view.findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        historyList = new ArrayList<>();

        // 1. Initialize the adapter with the context and the list
        HistoryAdapter adapter = new HistoryAdapter(getContext(), historyList);
        recyclerView.setAdapter(adapter);

        loadUserHistory(adapter);

        return view;
    }

    private void loadUserHistory(HistoryAdapter adapter) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("UserHistory").child(uid);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AlertModel alert = ds.getValue(AlertModel.class);
                    if (alert != null) {
                        alert.setKey(ds.getKey());
                        historyList.add(alert);
                    }
                }
                Collections.reverse(historyList);
                adapter.notifyDataSetChanged(); // Update the UI
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}