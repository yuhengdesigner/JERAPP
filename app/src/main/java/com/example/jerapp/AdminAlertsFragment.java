package com.example.jerapp;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class AdminAlertsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<AlertModel> alertList = new ArrayList<>();
    private TextView statusText;
    private String departmentFilter;
    private boolean isInitialLoad = true;

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
        if (getArguments() != null) departmentFilter = getArguments().getString("dept_filter");
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
                // Move to Resolved node
                moveAlert(alert, "ResolvedAlerts");
            }

            @Override
            public void onReceive(AlertModel alert) {
                // FIX: Now matches the 2-argument signature
                moveAlert(alert, "ProcessingAlerts");
            }
        });

        recyclerView.setAdapter(adapter);
        listenForAlerts();
    }

    private void moveAlert(AlertModel alert, String targetNode) {
        String deptId = alert.getAssignedDept(); // Use the dept_id from the alert
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        String key = alert.getKey();

        // Source node now needs the deptId path too!
        final String sourceNode = "ActiveAlerts/" + deptId;

        alert.setStatus(targetNode.equals("ProcessingAlerts") ? "Processing" : "Resolved");

        // Move data to: ProcessingAlerts/DEPT_ID/KEY
        root.child(targetNode).child(deptId).child(key).setValue(alert).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                root.child(sourceNode).child(key).removeValue();
            }
        });
    }

    private void updateStatus(String key, String status) {
        FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(key).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Status: " + status, Toast.LENGTH_SHORT).show());
    }

    private void listenForAlerts() {
        // Replace this with your actual way to get the logged-in admin's dept
        // If you don't have a MySession class, use SharedPreferences directly:
        String adminDeptId = requireContext().getSharedPreferences("AdminPrefs", requireContext().MODE_PRIVATE)
                .getString("dept_id", "default_dept");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(adminDeptId);

        Log.d("URGENT_DEBUG", "Listening to path: " + ref.toString());

        ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // ADD THIS LOG
                        Log.d("DEBUG_ALERT", "Snapshot exists: " + snapshot.exists());
                        Log.d("DEBUG_ALERT", "Children count: " + snapshot.getChildrenCount());

                        List<AlertModel> newList = new ArrayList<>();
                        boolean hasNewAlerts = false; // DECLARED HERE

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            AlertModel alert = ds.getValue(AlertModel.class);
                            if (alert != null) {
                                alert.setKey(ds.getKey());
                                newList.add(alert);
                                }

                                // Check if this is a new arrival
                                if (getAlertFromList(alert.getKey()) == null) {
                                    hasNewAlerts = true;
                                }
                            }


                        // Update UI
                        getActivity().runOnUiThread(() -> {
                            alertList.clear();
                            alertList.addAll(newList);
                            adapter.notifyDataSetChanged();
                            if (statusText != null) {
                                statusText.setText(alertList.isEmpty() ? "No pending alerts for " + departmentFilter : "Active Alerts");
                            }
                        });

                        if (!isInitialLoad && hasNewAlerts) playSound();
                        isInitialLoad = false;
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("DEBUG_ALERT", "Error: " + error.getMessage());
                    }
                });
    }

    private AlertModel getAlertFromList(String key) {
        for (AlertModel a : alertList) if (a.getKey().equals(key)) return a;
        return null;
    }

    private void playSound() {
        try {
            Uri soundUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + R.raw.emergency_alert_sound);
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(requireContext(), soundUri);
            mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            mp.prepare();
            mp.start();
            mp.setOnCompletionListener(MediaPlayer::release);
        } catch (Exception e) { e.printStackTrace(); }
    }
}