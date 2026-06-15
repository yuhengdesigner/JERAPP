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
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        adapter = new AdminAdapter(alertList, new AdminAdapter.OnAlertClickListener() {
            @Override
            public void onDetailsClick(AlertModel alert) {
                Intent intent = new Intent(requireContext(), EmergencyDetailActivity.class);
                intent.putExtra("alert_key", alert.getKey());
                intent.putExtra("dept_id", alert.getAssignedDept()); 
                startActivity(intent);
            }

            @Override
            public void onResolve(AlertModel alert, String alertKey) {
                moveAlert(alert, "ResolvedAlerts");
            }

            @Override
            public void onReceive(AlertModel alert) {
                stopAlarm();
                moveAlert(alert, "ProcessingAlerts");
            }

            @Override
            public void onDelete(AlertModel alert) {
                String deptId = alert.getAssignedDept();
                String alertKey = alert.getKey();
                String path = (deptId == null || deptId.isEmpty()) ? "UnknownAlerts" : "ActiveAlerts/" + deptId;
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path).child(alertKey);
                ref.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            listenForAlerts();
            swipeRefresh.setRefreshing(false);
        });

        recyclerView.setAdapter(adapter);
        listenForAlerts();
    }

    private void moveAlert(AlertModel alert, String targetNode) {
        if (alert == null || alert.getKey() == null) return;

        String deptId = alert.getAssignedDept();
        if (deptId == null || deptId.isEmpty()) deptId = "default_dept";

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        String key = alert.getKey();

        final String sourcePath = "ActiveAlerts/" + deptId + "/" + key;
        final String targetPath = targetNode + "/" + deptId + "/" + key;

        alert.setStatus(targetNode.equals("ProcessingAlerts") ? "Processing" : "Resolved");

        root.child(targetPath).setValue(alert).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                root.child(sourcePath).removeValue();
            }
        });
    }

    private void listenForAlerts() {
        // FIX: Use departmentFilter passed via arguments instead of uninitialized SharedPreferences
        if (departmentFilter == null || departmentFilter.isEmpty()) {
            Log.e("AdminAlertsFragment", "No department filter provided for listener");
            return;
        }

        if (listenerRef != null && valueEventListener != null) {
            listenerRef.removeEventListener(valueEventListener);
        }

        listenerRef = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(departmentFilter);
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AlertModel> newList = new ArrayList<>();
                boolean hasNewAlerts = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    AlertModel alert = ds.getValue(AlertModel.class);
                    if (alert != null) {
                        alert.setKey(ds.getKey());
                        newList.add(alert);
                        // Detect if this is a brand new alert (not in current list)
                        if (!isInitialLoad && getAlertFromList(alert.getKey()) == null) {
                            hasNewAlerts = true;
                        }
                    }
                }

                if (isAdded() && getActivity() != null) {
                    final boolean shouldAlert = hasNewAlerts;
                    alertList.clear();
                    alertList.addAll(newList);
                    adapter.notifyDataSetChanged();
                    
                    if (statusText != null) {
                        statusText.setText(alertList.isEmpty() ? "No pending alerts" : "Active Alerts (" + alertList.size() + ")");
                    }
                    
                    if (!isInitialLoad && shouldAlert) {
                        playSound();
                    }
                    isInitialLoad = false;
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Alerts listener cancelled: " + error.getMessage());
            }
        };
        listenerRef.addValueEventListener(valueEventListener);
    }

    private AlertModel getAlertFromList(String key) {
        for (AlertModel a : alertList) {
            if (a.getKey() != null && a.getKey().equals(key)) return a;
        }
        return null;
    }

    private void playSound() {
        try {
            stopAlarm();
            mediaPlayer = new MediaPlayer();
            // Ensure this sound file exists in res/raw/emergency_alert_sound.mp3 or .wav
            Uri soundUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + R.raw.emergency_alert_sound);
            mediaPlayer.setDataSource(requireContext(), soundUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e("SOUND_ERROR", "Error playing sound: " + e.getMessage());
            // Fallback to default notification sound if custom one fails
            try {
                Uri defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
                mediaPlayer = MediaPlayer.create(requireContext(), defaultUri);
                mediaPlayer.start();
            } catch (Exception e2) {}
        }
    }
    
    private void stopAlarm() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            }
        } catch (Exception e) {} finally { mediaPlayer = null; }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAlarm();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRef != null && valueEventListener != null) {
            listenerRef.removeEventListener(valueEventListener);
        }
    }
}
