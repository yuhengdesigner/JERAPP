package com.example.jerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class AdminHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<AlertModel> historyList = new ArrayList<>();
    private String departmentFilter;
    private TextView statusText;

    public static AdminHistoryFragment newInstance(String dept) {
        AdminHistoryFragment fragment = new AdminHistoryFragment();
        Bundle args = new Bundle();
        args.putString("dept_filter", dept);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            departmentFilter = getArguments().getString("dept_filter");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We can reuse the same layout as the Alerts fragment!
        return inflater.inflate(R.layout.fragment_admin_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.adminAlertRecyclerView);
        statusText = view.findViewById(R.id.adminStatus);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // We reuse the AdminAdapter, but we can leave the resolve button disabled or hidden
        adapter = new AdminAdapter(historyList, new AdminAdapter.OnAlertClickListener() {
            @Override
            public void onDetailsClick(AlertModel alert) {
                // View details of archived alert
            }

            @Override
            public void onResolve(AlertModel alert, String key) {
                // Already resolved, so we do nothing here
            }

            @Override
            public void onLocate(AlertModel alert) {
                // Optional: show where the incident happened
            }
        });

        recyclerView.setAdapter(adapter);
        loadHistory();
    }

    private void loadHistory() {
        if (departmentFilter == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ResolvedAlerts");

        // Filter by the same department so admins only see their own history
        ref.orderByChild("assignedDept").equalTo(departmentFilter)
                .addValueEventListener(new ValueEventListener() {
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

                        if (historyList.isEmpty()) {
                            statusText.setText("No history for " + departmentFilter);
                        } else {
                            statusText.setText("Showing resolved cases for " + departmentFilter);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}