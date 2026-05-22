package com.example.jerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HistoryDetailActivity extends AppCompatActivity {
    private VideoView videoView;
    private TextView tvDept, tvDate, tvTitle;
    private Button btnCall, btnNavigate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        // Bind views
        videoView = findViewById(R.id.videoViewEvidence);
        tvDept = findViewById(R.id.tvDetailDept);
        tvDate = findViewById(R.id.tvDetailDate);
        btnCall = findViewById(R.id.btnCallDept);
        btnNavigate = findViewById(R.id.btnNavigate);

        String alertKey = getIntent().getStringExtra("alert_key");
        loadAlertDetails(alertKey);
    }

    private void loadAlertDetails(String key) {
        FirebaseDatabase.getInstance().getReference("ResolvedAlerts").child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        AlertModel alert = snapshot.getValue(AlertModel.class);
                        if (alert == null) return;

                        tvDept.setText("Department: " + alert.getAssignedDept());
                        tvDate.setText("Date: " + new java.util.Date(alert.getTimestamp()).toString());

                        // Video Loading
                        if (alert.getVideoUrl() != null && !alert.getVideoUrl().isEmpty()) {
                            videoView.setVideoPath(alert.getVideoUrl());
                            videoView.start();
                        }

                        // Call Button
                        btnCall.setOnClickListener(v -> {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + alert.getDeptPhone()));
                            startActivity(intent);
                        });

                        // Navigation Button
                        btnNavigate.setOnClickListener(v -> {
                            // Correct format for Google Maps URI
                            String geoUri = "geo:" + alert.getDeptLat() + "," + alert.getDeptLng() +
                                    "?q=" + alert.getDeptLat() + "," + alert.getDeptLng() + "(" + alert.getDeptName() + ")";

                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
                            mapIntent.setPackage("com.google.android.apps.maps"); // Ensures it opens in Google Maps

                            // Check if Google Maps is installed
                            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(mapIntent);
                            } else {
                                // Fallback to browser if Maps app isn't installed
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + alert.getDeptLat() + "," + alert.getDeptLng())));
                            }
                        });
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}