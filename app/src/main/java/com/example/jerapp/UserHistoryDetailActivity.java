package com.example.jerapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserHistoryDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private String alertKey;
    private GoogleMap mMap;
    private double userLat, userLng;
    private String deptPhoneNumber = "";

    private TextView tvStatus, tvUserName, tvUserGender, tvUserContact, tvUserEmail, tvUserAddress, tvUserTimestamp;
    private TextView tvDetailType, tvDetailDept, tvDetailAddress, tvDetailDate;
    private Button btnCall, btnNavigate, btnCallAgain;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;

    // Cached model to assist button click redirections
    private AlertModel currentAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        alertKey = getIntent().getStringExtra("alert_key");

        // Initialize UI Elements
        tvStatus = findViewById(R.id.tvStatus);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserGender = findViewById(R.id.tvUserGender);
        tvUserContact = findViewById(R.id.tvUserContact);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserAddress = findViewById(R.id.tvUserAddress);
        tvUserTimestamp = findViewById(R.id.tvUserTimestamp);

        tvDetailType = findViewById(R.id.tvDetailType);
        tvDetailDept = findViewById(R.id.tvDetailDept);
        tvDetailAddress = findViewById(R.id.tvDetailAddress);
        tvDetailDate = findViewById(R.id.tvDetailDate);

        playerView = findViewById(R.id.playerView);
        btnCall = findViewById(R.id.btnCall);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnCallAgain = findViewById(R.id.btnCallAgain);

        // Core Actions
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Initialize Embedded Map
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.mapContainer, mapFragment).commit();
        mapFragment.getMapAsync(this);

        loadHistoryDetails();
    }

    private void loadHistoryDetails() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || alertKey == null) return;

        // Point this path to where your main history items are saved
        FirebaseDatabase.getInstance().getReference("UserHistory")
                .child(uid)
                .child(alertKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentAlert = snapshot.getValue(AlertModel.class);
                        if (currentAlert == null) {
                            Toast.makeText(UserHistoryDetailActivity.this, "Data no longer exists.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        updateUI(currentAlert);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("DETAIL_ERR", error.getMessage());
                    }
                });
    }

    private void updateUI(AlertModel alert) {
        userLat = alert.getUserLat();
        userLng = alert.getUserLng();
        deptPhoneNumber = alert.getDeptPhone();

        // Bind Text Fields safely
        tvStatus.setText(alert.getStatus() != null ? alert.getStatus().toUpperCase() : "PENDING");
        tvUserName.setText("Name: " + alert.getUserName());
        tvUserGender.setText("Gender: " + alert.getGender());
        tvUserContact.setText("Contact: " + alert.getUserPhone());
        tvUserEmail.setText("Email: " + alert.getUserEmail());
        tvUserAddress.setText("Address: " + alert.getTextAddress());

        tvDetailType.setText("Emergency Type: " + (alert.getEmergencyType() != null ? alert.getEmergencyType().toUpperCase() : "N/A"));
        tvDetailDept.setText("Dept: " + alert.getDeptName());
        tvDetailAddress.setText("Address: " + alert.getTextAddress());

        // Sync Time Stamp Display Format
        String displayTime = "Unavailable";
        Object timeObj = alert.getTimestamp();
        if (timeObj instanceof Long) {
            long timeLong = (Long) timeObj;
            if (timeLong > 0) {
                displayTime = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(timeLong));
            }
        } else if (timeObj instanceof String) {
            displayTime = (String) timeObj;
        }
        tvUserTimestamp.setText("Time: " + displayTime);
        tvDetailDate.setText("Timestamp: " + displayTime);

        // Status Color Coding Logic
        String status = alert.getStatus();
        if ("COMPLETED".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status)) {
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else if ("FAILED".equalsIgnoreCase(status)) {
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else {
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }

        // Action 1: Navigation to Department Location via external Google Maps App
        btnNavigate.setOnClickListener(v -> {
            if (alert.getDeptLat() != 0 && alert.getDeptLng() != 0) {
                String navigationUrl = "google.navigation:q=" + alert.getDeptLat() + "," + alert.getDeptLng();
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(navigationUrl));
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    String webFallbackUrl = "https://www.google.com/maps/dir/?api=1&destination=" + alert.getDeptLat() + "," + alert.getDeptLng();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webFallbackUrl)));
                }
            } else {
                Toast.makeText(this, "Department coordinates unavailable.", Toast.LENGTH_SHORT).show();
            }
        });

        // Call Department
        btnCall.setOnClickListener(v -> {
            if (deptPhoneNumber != null && !deptPhoneNumber.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + deptPhoneNumber)));
            } else {
                Toast.makeText(this, "Department phone unavailable.", Toast.LENGTH_SHORT).show();
            }
        });

        // Action 2: Call Emergency Again (Guarded by Runtime Permission Routine)
        btnCallAgain.setOnClickListener(v -> checkLocationPermissionAndProceed());

        // Action 3: Empty Video Container Box Placeholder Visibility Setup
        if (alert.getVideoUrl() != null && !alert.getVideoUrl().isEmpty()) {
            setupVideoPlayback(alert.getVideoUrl());
        } else {
            playerView.setVisibility(View.VISIBLE); // Keeps container rendering as a dark box placeholder
            if (exoPlayer != null) {
                exoPlayer.stop();
            }
            playerView.setPlayer(null);
        }

        updateMapLocation();
    }

    private void checkLocationPermissionAndProceed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            navigateToConfirmationScreen();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                navigateToConfirmationScreen();
            } else {
                Toast.makeText(this, "Location access denied. Cannot proceed with report re-submission.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void navigateToConfirmationScreen() {
        if (currentAlert == null) return;
        Intent intent = new Intent(UserHistoryDetailActivity.this, ConfirmationActivity.class);
        intent.putExtra("emergency_type", currentAlert.getEmergencyType());
        intent.putExtra("dept_id", currentAlert.getDept_id());
        intent.putExtra("dept_name", currentAlert.getDeptName());
        intent.putExtra("dept_phone", currentAlert.getDeptPhone());
        intent.putExtra("dept_lat", currentAlert.getDeptLat());
        intent.putExtra("dept_lng", currentAlert.getDeptLng());
        startActivity(intent);
    }

    private void setupVideoPlayback(String videoUrl) {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(exoPlayer);
        } else {
            exoPlayer.stop();
            exoPlayer.clearMediaItems();
        }
        MediaItem item = MediaItem.fromUri(videoUrl);
        exoPlayer.setMediaItem(item);
        exoPlayer.prepare();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateMapLocation();
    }

    private void updateMapLocation() {
        if (mMap != null && userLat != 0 && userLng != 0) {
            LatLng pos = new LatLng(userLat, userLng);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(pos).title("Emergency Occurrence Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (playerView != null) playerView.setPlayer(null);
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}