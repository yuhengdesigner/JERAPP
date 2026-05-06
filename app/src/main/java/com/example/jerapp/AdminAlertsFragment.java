package com.example.jerapp;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
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

public class AdminAlertsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<AlertModel> alertList = new ArrayList<>();
    private int lastAlertCount = 0;
    private TextView statusText;
    private String departmentFilter;

    // Static factory method to pass the department name from Activity to Fragment
    public static AdminAlertsFragment newInstance(String dept) {
        AdminAlertsFragment fragment = new AdminAlertsFragment();
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
        return inflater.inflate(R.layout.fragment_admin_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.adminAlertRecyclerView);
        statusText = view.findViewById(R.id.adminStatus);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AdminAdapter(alertList, new AdminAdapter.OnAlertClickListener() {
            @Override
            public void onDetailsClick(AlertModel alert) {
                Intent intent = new Intent(requireContext(), EmergencyDetailActivity.class);
                intent.putExtra("alert_key", alert.getKey());
                startActivity(intent);
            }

            // Fixed: Added the String parameter to match your Interface
            @Override
            public void onResolve(AlertModel alert, String status) {
                // If you want to update the status to "Resolved" before moving
                alert.status = "Resolved";
                moveAlertToHistory(alert);
            }

            @Override
            public void onLocate(AlertModel alert) {
                String uri = "geo:" + alert.getLatitude() + "," + alert.getLongitude() +
                        "?q=" + alert.getLatitude() + "," + alert.getLongitude() + "(Emergency Location)";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);
        listenForAlerts();
    }

    private void listenForAlerts() {
        // If departmentFilter is null (failsafe), we don't query to avoid showing everyone's data
        if (departmentFilter == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ActiveAlerts");

        // Only listen for alerts assigned to THIS specific department
        ref.orderByChild("assignedDept").equalTo(departmentFilter)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        alertList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            AlertModel alert = ds.getValue(AlertModel.class);
                            if (alert != null) {
                                alert.setKey(ds.getKey());
                                alertList.add(alert);
                            }
                        }

                        // Play sound only if new alerts come in
                        if (snapshot.getChildrenCount() > lastAlertCount) {
                            playSound();
                        }
                        lastAlertCount = (int) snapshot.getChildrenCount();

                        // Update UI status
                        if (alertList.isEmpty()) {
                            statusText.setText("No active alerts for " + departmentFilter);
                        } else {
                            statusText.setText("Listening for " + departmentFilter + "...");
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void moveAlertToHistory(AlertModel alert) {
        DatabaseReference activeRef = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(alert.getKey());
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("ResolvedAlerts").child(alert.getKey());

        // 1. Copy data to ResolvedAlerts node
        historyRef.setValue(alert).addOnSuccessListener(aVoid -> {
            // 2. Remove from ActiveAlerts node
            activeRef.removeValue().addOnSuccessListener(unused -> {
                Toast.makeText(requireContext(), "Case resolved and archived", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void playSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE); // TYPE_RINGTONE is louder than NOTIFICATION
            Ringtone r = RingtoneManager.getRingtone(requireContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}