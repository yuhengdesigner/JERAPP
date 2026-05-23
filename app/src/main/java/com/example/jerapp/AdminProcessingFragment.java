package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.firebase.database.*;
import java.util.*;

public class AdminProcessingFragment extends Fragment {
    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<AlertModel> alertList = new ArrayList<>();
    private TextView statusText;
    private String departmentFilter;

    public static AdminProcessingFragment newInstance(String dept) {
        AdminProcessingFragment fragment = new AdminProcessingFragment();
        Bundle args = new Bundle();
        args.putString("dept_filter", dept);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) departmentFilter = getArguments().getString("dept_filter");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_processing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.processingRecyclerView);
        statusText = view.findViewById(R.id.processingStatus);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AdminAdapter(alertList, new AdminAdapter.OnAlertClickListener() {
            @Override
            public void onDetailsClick(AlertModel alert) {
                Intent intent = new Intent(requireContext(), EmergencyDetailActivity.class);
                intent.putExtra("alert_key", alert.getKey());
                startActivity(intent);
            }
            @Override
            public void onResolve(AlertModel alert, String alertKey) {
                DatabaseReference root = FirebaseDatabase.getInstance().getReference();
                String deptId = alert.getAssignedDept();

                alert.setStatus("Resolved");

                // Move to Resolved node
                root.child("ResolvedAlerts").child(deptId).child(alertKey).setValue(alert)
                        .addOnSuccessListener(aVoid -> {
                            // Remove from Processing
                            root.child("ProcessingAlerts").child(deptId).child(alertKey).removeValue();
                        });
            }
            @Override
            public void onReceive(AlertModel alert) { /* Already in Processing */ }
        });
        recyclerView.setAdapter(adapter);
        listenForProcessing();
    }

    private void listenForProcessing() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ProcessingAlerts").child(departmentFilter);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                alertList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AlertModel alert = ds.getValue(AlertModel.class);
                    if (alert != null && "Processing".equals(alert.getStatus()) &&
                            alert.getAssignedDept().equalsIgnoreCase(departmentFilter)) {
                        alert.setKey(ds.getKey());
                        alertList.add(alert);
                    }
                }
                adapter.notifyDataSetChanged();
                statusText.setVisibility(alertList.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}