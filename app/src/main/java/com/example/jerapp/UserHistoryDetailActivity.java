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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;


public class UserHistoryDetailActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private String alertKey;
    private GoogleMap mMap, mDeptMap;
    private double userLat = 0, userLng = 0;
    private double deptLat = 0, deptLng = 0;
    private String deptPhoneNumber = "";

    private TextView tvStatus, tvUserName, tvUserGender, tvUserContact, tvUserEmail, tvUserAddress, tvUserTimestamp;
    private TextView tvDetailType, tvDetailDept, tvDetailAddress, tvDetailDate, tvNoVideo;
    private Button btnCall, btnNavigate, btnCallAgain;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private RecyclerView rvUserVideoGallery;
    private VideoGalleryAdapter videoGalleryAdapter;
    private List<String> videoUrlList = new ArrayList<>();

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
        tvNoVideo = findViewById(R.id.tvNoVideo);

        playerView = findViewById(R.id.playerView);
        rvUserVideoGallery = findViewById(R.id.rvUserVideoGallery);
        btnCall = findViewById(R.id.btnCall);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnCallAgain = findViewById(R.id.btnCallAgain);

        rvUserVideoGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoGalleryAdapter = new VideoGalleryAdapter(this, videoUrlList, clickedUrl -> {
            setupVideoPlayback(clickedUrl);
        });
        rvUserVideoGallery.setAdapter(videoGalleryAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Initialize maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mMap = googleMap;
                updateMapLocation();
            });
        }

        SupportMapFragment deptMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.deptMapFragment);
        if (deptMapFragment != null) {
            deptMapFragment.getMapAsync(googleMap -> {
                mDeptMap = googleMap;
                updateDeptMapLocation();
            });
        }

        loadHistoryDetails();
    }

    private void loadHistoryDetails() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().isAnonymous())) {
            uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("guest_uid", null);
        }
        
        if (uid == null || alertKey == null) return;

        FirebaseDatabase.getInstance().getReference("UserHistory")
                .child(uid)
                .child(alertKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentAlert = snapshot.getValue(AlertModel.class);
                        if (currentAlert == null) return;
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
        deptLat = alert.getDeptLat();
        deptLng = alert.getDeptLng();
        deptPhoneNumber = alert.getDeptPhone();

        tvStatus.setText(alert.getStatus() != null ? alert.getStatus().toUpperCase() : "PENDING");
        tvUserName.setText("Name: " + alert.getUserName());
        tvUserGender.setText("Gender: " + alert.getGender());
        tvUserContact.setText("Contact: " + alert.getUserPhone());
        tvUserEmail.setText("Email: " + alert.getUserEmail());
        tvUserAddress.setText("Address: " + alert.getTextAddress());

        tvDetailType.setText("Emergency Type: " + (alert.getEmergencyType() != null ? alert.getEmergencyType().toUpperCase() : "N/A"));
        tvDetailDept.setText("Dept: " + alert.getDeptName());
        tvDetailAddress.setText("Address: " + alert.getTextAddress());

        String displayTime = "Unavailable";
        Object timeObj = alert.getTimestamp();

        if (timeObj != null) {
            try {
                long timeLong = 0;
                if (timeObj instanceof Long) timeLong = (Long) timeObj;
                else if (timeObj instanceof Double) timeLong = ((Double) timeObj).longValue();
                else if (timeObj instanceof String) timeLong = Long.parseLong((String) timeObj);

                if (timeLong > 0) {
                    displayTime = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(timeLong));
                }
            } catch (Exception e) {
                displayTime = timeObj.toString();
            }
        }

        tvUserTimestamp.setText("Time: " + displayTime);
        tvDetailDate.setText("Timestamp: " + displayTime);

        String status = alert.getStatus();
        if ("COMPLETED".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status) || "ARRIVED".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status)) {
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else if ("FAILED".equalsIgnoreCase(status)) {
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else {
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }

        // Actions
        btnNavigate.setOnClickListener(v -> {
            if (deptLat != 0 && deptLng != 0) {
                String navigationUrl = "google.navigation:q=" + deptLat + "," + deptLng;
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(navigationUrl));
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        btnCall.setOnClickListener(v -> {
            if (deptPhoneNumber != null && !deptPhoneNumber.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + deptPhoneNumber)));
            }
        });

        btnCallAgain.setOnClickListener(v -> checkLocationPermissionAndProceed());

        // REQUIREMENT 1: Video availability check
        List<String> videoUrlsList = alert.getVideoUrls();
        if (videoUrlsList != null && !videoUrlsList.isEmpty()) {
            playerView.setVisibility(View.VISIBLE);
            rvUserVideoGallery.setVisibility(View.VISIBLE);
            tvNoVideo.setVisibility(View.GONE);
            videoGalleryAdapter.updateData(videoUrlsList);
            if (exoPlayer == null) setupVideoPlayback(videoUrlsList.get(0));
        } else {
            rvUserVideoGallery.setVisibility(View.GONE);
            playerView.setVisibility(View.GONE);
            tvNoVideo.setVisibility(View.VISIBLE);
        }

        if (deptLat == 0 && alert.getDept_id() != null) {
            fetchDeptLocationFromSource(alert.getDept_id());
        } else {
            updateDeptMapLocation();
        }
        
        updateMapLocation();
    }

    private void fetchDeptLocationFromSource(String id) {
        FirebaseDatabase.getInstance().getReference("emergency_departments").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            deptLat = snapshot.child("latitude").getValue(Double.class);
                            deptLng = snapshot.child("longitude").getValue(Double.class);
                            updateDeptMapLocation();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void checkLocationPermissionAndProceed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            navigateToConfirmationScreen();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void navigateToConfirmationScreen() {
        if (currentAlert == null) return;
        Intent intent = new Intent(UserHistoryDetailActivity.this, ConfirmationActivity.class);
        intent.putExtra("emergency_type", currentAlert.getEmergencyType());
        intent.putExtra("dept_id", currentAlert.getDept_id());
        intent.putExtra("dept_name", currentAlert.getDeptName());
        intent.putExtra("dept_phone", currentAlert.getDeptPhone());
        intent.putExtra("dept_lat", deptLat);
        intent.putExtra("dept_lng", deptLng);
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
        exoPlayer.setPlayWhenReady(true);
    }

    private void updateMapLocation() {
        if (mMap != null && userLat != 0 && userLng != 0) {
            LatLng pos = new LatLng(userLat, userLng);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(pos).title("Victim Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        }
    }

    private void updateDeptMapLocation() {
        if (mDeptMap != null && deptLat != 0 && deptLng != 0) {
            LatLng location = new LatLng(deptLat, deptLng);
            mDeptMap.clear();
            mDeptMap.addMarker(new MarkerOptions().position(location).title("Department Location"));
            mDeptMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
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
