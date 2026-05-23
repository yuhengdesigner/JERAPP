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
import android.content.Context;

public class AdminAlertsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<AlertModel> alertList = new ArrayList<>();
    private TextView statusText;
    private String departmentFilter;
    private boolean isInitialLoad = true;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private MediaPlayer mediaPlayer;
    private DatabaseReference listenerRef;
    private ValueEventListener valueEventListener;

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
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

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
                stopAlarm();
                moveAlert(alert, "ProcessingAlerts");
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            // Manual refresh: just reload the listener
            listenForAlerts();
            swipeRefresh.setRefreshing(false); // Stop the animation
        });

        recyclerView.setAdapter(adapter);
        listenForAlerts();
    }

    private void moveAlert(AlertModel alert, String targetNode) {
        if (alert == null || alert.getKey() == null) {
            Log.e("ADMIN_ERROR", "Move failed: Alert or Key is null");
            return;
        }

        String deptId = alert.getAssignedDept();
        if (deptId == null || deptId.isEmpty()) {
            deptId = "default_dept"; // Fallback to avoid null path
        }

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        String key = alert.getKey();

        // Source path: ActiveAlerts/{deptId}/{key}
        final String sourcePath = "ActiveAlerts/" + deptId + "/" + key;
        final String targetPath = targetNode + "/" + deptId + "/" + key;

        alert.setStatus(targetNode.equals("ProcessingAlerts") ? "Processing" : "Resolved");

        // Perform the move
        root.child(targetPath).setValue(alert).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                root.child(sourcePath).removeValue().addOnFailureListener(e ->
                        Log.e("ADMIN_ERROR", "Removal failed: " + e.getMessage()));
            } else {
                Log.e("ADMIN_ERROR", "Move failed: " + task.getException().getMessage());
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
        String adminDeptId = requireContext().getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
                .getString("dept_id", "default_dept");

//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(adminDeptId);
//        Log.d("URGENT_DEBUG", "Listening to path: " + ref.toString());

        listenerRef = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(adminDeptId);
        valueEventListener = new ValueEventListener() {
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
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                                alertList.clear();
                                alertList.addAll(newList);
                                adapter.notifyDataSetChanged();
                                if (statusText != null) {
                                    statusText.setText(alertList.isEmpty() ? "No pending alerts for " + departmentFilter : "Active Alerts");
                                }
                            });
                        }

                        if (!isInitialLoad && hasNewAlerts) playSound();
                        isInitialLoad = false;
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("DEBUG_ALERT", "Error: " + error.getMessage());
                    }
                };
        listenerRef.addValueEventListener(valueEventListener);
    }

    private AlertModel getAlertFromList(String key) {
        for (AlertModel a : alertList) if (a.getKey().equals(key)) return a;
        return null;
    }

    private void playSound() {
        try {
            stopAlarm(); // Clear old instance
            mediaPlayer = new MediaPlayer(); // Initialize it here!
            Uri soundUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + R.raw.emergency_alert_sound);

            mediaPlayer.setDataSource(requireContext(), soundUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setLooping(true);
        } catch (Exception e) {
            Log.e("SOUND_ERROR", "Error: " + e.getMessage());
        }
    }
    private void stopAlarm() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            }
        } catch (Exception e) {
            Log.e("SOUND_ERROR", "Could not stop alarm safely: " + e.getMessage());
        } finally {
            mediaPlayer = null;
        }
    }
    // Add this to ensure sound stops if the admin leaves the screen
    @Override
    public void onPause() {
        super.onPause();
        stopAlarm();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRef != null && valueEventListener != null) {
            listenerRef.removeEventListener(valueEventListener); // You would need to store the listener as a field
        }
    }
}