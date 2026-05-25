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
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class HistoryDetailActivity extends AppCompatActivity {
    private VideoView videoView;
    private TextView tvDept, tvDate, tvTitle;
    private Button btnCall, btnNavigate;
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView tvUserName, tvUserGender, tvUserContact, tvUserEmail, tvUserAddress, tvUserTimestamp;
    private TextView tvDetailType, tvDetailAddress, tvDetailDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        // Bind views
        playerView = findViewById(R.id.playerView);
        TextView tvStatus = findViewById(R.id.tvStatus);
        tvDept = findViewById(R.id.tvDetailDept);
        tvDate = findViewById(R.id.tvDetailDate);
        btnCall = findViewById(R.id.btnCall);
        btnNavigate = findViewById(R.id.btnNavigate);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserGender = findViewById(R.id.tvUserGender);
        tvUserContact = findViewById(R.id.tvUserContact);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserAddress = findViewById(R.id.tvUserAddress);
        tvUserTimestamp = findViewById(R.id.tvUserTimestamp);

        tvDetailType = findViewById(R.id.tvDetailType);
        tvDetailAddress = findViewById(R.id.tvDetailAddress);
        tvDetailDate = findViewById(R.id.tvDetailDate);

        String alertKey = getIntent().getStringExtra("alert_key");
        loadAlertDetails(alertKey);
    }

    private void loadAlertDetails(String key) {
        FirebaseDatabase.getInstance().getReference("ResolvedAlerts").child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        AlertModel alert = snapshot.getValue(AlertModel.class);

                        // DEBUG: Check what is actually coming back
                        android.util.Log.d("DEBUG_DATA", "Snapshot value: " + snapshot.getValue().toString());

                        if (alert == null) return;

                        tvUserName.setText("Name: " + alert.getUserName());
                        tvUserGender.setText("Gender: " + alert.getUserGender());
                        tvUserContact.setText("Contact: " + alert.getUserPhone());
                        tvUserEmail.setText("Email: " + alert.getUserEmail());
                        tvUserAddress.setText("Address: " + alert.getUserAddress());

                        tvDetailType.setText("Emergency Type: " + alert.getEmergencyType());
                        tvDetailAddress.setText("Address: " + alert.getDeptAddress());
                        tvDetailDate.setText("Timestamp: " + new java.util.Date(alert.getTimestamp()).toString());

                        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

                        String status = alert.getStatus();
                        TextView tvStatus = findViewById(R.id.tvStatus);
                        tvStatus.setText(status != null ? status.toUpperCase() : "PENDING");

                        // Change color based on status
                        if ("COMPLETED".equalsIgnoreCase(status)) {
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        } else if ("FAILED".equalsIgnoreCase(status)) {
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        } else {
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        }

                        tvDept.setText("Department: " + alert.getAssignedDept());
                        tvDate.setText("Date: " + new java.util.Date(alert.getTimestamp()).toString());

                        // Video Loading with ExoPlayer
                        if (alert.getVideoUrl() != null && !alert.getVideoUrl().isEmpty()) {
                            // FIX: Explicitly specify the Activity context
                            player = new ExoPlayer.Builder(HistoryDetailActivity.this).build();
                            playerView.setPlayer(player);

                            MediaItem mediaItem = MediaItem.fromUri(alert.getVideoUrl());
                            player.setMediaItem(mediaItem);
                            player.prepare();
                            player.play();
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

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}