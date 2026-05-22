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

            @Override
            public void onResolve(AlertModel alert, String alertKey) {
                resolveAlert(alert, alertKey);
            }

            @Override
            public void onConfirmArrival(AlertModel alert) {
                confirmArrival(alert.getKey());
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

    // Inside AdminAlertsFragment.java

    private void confirmArrival(String alertKey) {
        FirebaseDatabase.getInstance().getReference("ActiveAlerts")
                .child(alertKey).child("status").setValue("Arrived")
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Status: Arrived", Toast.LENGTH_SHORT).show());
    }

    private void resolveAlert(AlertModel alert, String alertKey) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        // 1. Save to ResolvedAlerts
        root.child("ResolvedAlerts").child(alertKey).setValue(alert);

        // 2. Remove from ActiveAlerts
        root.child("ActiveAlerts").child(alertKey).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Incident Closed & Archived.", Toast.LENGTH_SHORT).show();
                });
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

    // Inside AdminAlertsFragment.java

    private void moveAlertToHistory(AlertModel alert) {
        DatabaseReference activeRef = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(alert.getKey());
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("ResolvedAlerts").child(alert.getKey());

        alert.status = "Resolved"; // Update the status

        // Simply setting the whole object copies all fields (including videoUrl, if it exists)
        historyRef.setValue(alert).addOnSuccessListener(aVoid -> {
            activeRef.removeValue();
            Toast.makeText(requireContext(), "Case archived.", Toast.LENGTH_SHORT).show();
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