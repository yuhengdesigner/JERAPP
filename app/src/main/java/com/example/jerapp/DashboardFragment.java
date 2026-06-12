package com.example.jerapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private EmergencyAdapter adapter;
    private List<AlertModel> ongoingAlertList = new ArrayList<>();
    private ValueEventListener ongoingAlertsListener;
    private DatabaseReference userHistoryRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.dashboardRecyclerView);

        // 1. Static grid items
        List<EmergencyModel> emergencyList = new ArrayList<>();
        emergencyList.add(new EmergencyModel("Fire & Explosion", R.drawable.ic_firelogo, "fire"));
        emergencyList.add(new EmergencyModel("Natural Disaster", R.drawable.ic_disasterlogo, "disaster"));
        emergencyList.add(new EmergencyModel("Medical Emergency", R.drawable.ic_medicallogo, "medical"));
        emergencyList.add(new EmergencyModel("Police & Crime", R.drawable.ic_policelogo, "police"));
        emergencyList.add(new EmergencyModel("Gas Leaking", R.drawable.ic_gaslogo, "gas"));
        emergencyList.add(new EmergencyModel("Wild Animals", R.drawable.ic_wildlogo, "wild"));

        // 2. Initialize Adapter
        adapter = new EmergencyAdapter(emergencyList, ongoingAlertList, new EmergencyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(EmergencyModel model) {
                Intent intent = new Intent(requireActivity(), DepartmentListActivity.class);
                intent.putExtra("emergency_type", model.getType());
                startActivity(intent);
            }

            @Override
            public void onOngoingClick(AlertModel alert) {
                Intent intent = new Intent(requireActivity(), TrackingActivity.class);
                intent.putExtra("alert_key", alert.getKey());
                intent.putExtra("dept_name", alert.getDeptName());
                intent.putExtra("dept_phone", alert.getDeptPhone());
                intent.putExtra("dept_id", alert.getDept_id());
                intent.putExtra("dept_lat", alert.getDeptLat());
                intent.putExtra("dept_lng", alert.getDeptLng());
                startActivity(intent);
            }
        });

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = adapter.getItemViewType(position);
                if (viewType == EmergencyAdapter.TYPE_HEADER || viewType == EmergencyAdapter.TYPE_ONGOING) {
                    return 2;
                }
                return 1;
            }
        });

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        listenForOngoingAlerts();
    }

    private void listenForOngoingAlerts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        // REQUIREMENT: Strict session isolation. No user = No emergency card.
        if (user == null) {
            ongoingAlertList.clear();
            if (adapter != null) adapter.setOngoingAlerts(new ArrayList<>());
            return;
        }

        String uid = user.getUid();
        userHistoryRef = FirebaseDatabase.getInstance().getReference("UserHistory").child(uid);
        
        ongoingAlertsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ongoingAlertList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        AlertModel alert = ds.getValue(AlertModel.class);
                        if (alert != null) {
                            alert.setKey(ds.getKey());
                            String status = alert.getStatus();
                            
                            // Only display cards that the user hasn't confirmed/finished yet
                            if (!"Confirmed".equalsIgnoreCase(status) && 
                                !"Resolved".equalsIgnoreCase(status) && 
                                !"Failed".equalsIgnoreCase(status) &&
                                !"Completed".equalsIgnoreCase(status)) {
                                ongoingAlertList.add(alert);
                            }
                        }
                    }
                }
                if (adapter != null) adapter.setOngoingAlerts(ongoingAlertList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseActive", error.getMessage());
            }
        };

        userHistoryRef.addValueEventListener(ongoingAlertsListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevent "Request Timeout" and leaks by detaching the listener correctly
        if (userHistoryRef != null && ongoingAlertsListener != null) {
            userHistoryRef.removeEventListener(ongoingAlertsListener);
        }
    }
}
