package com.example.jerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.pm.PackageManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.util.Log;
import com.google.android.gms.tasks.OnFailureListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String deptName, deptPhone, deptId, alertKey;
    private double deptLat, deptLng;
    private ActivityResultLauncher<Intent> videoLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLoc;
    private TextView tvCountdownETA;
    private Ringtone alarmRingtone;
    private DatabaseReference trackingDatabaseRef, statusDatabaseRef;
    private ValueEventListener trackingListener, statusListener;
    private boolean isCardExpanded = true;
    private boolean isAdminResolved = false; // New flag for swipe restriction

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        alertKey = getIntent().getStringExtra("alert_key");
        deptName = getIntent().getStringExtra("dept_name");
        deptPhone = getIntent().getStringExtra("dept_phone");
        deptId = getIntent().getStringExtra("dept_id");
        deptLat = getIntent().getDoubleExtra("dept_lat", 0);
        deptLng = getIntent().getDoubleExtra("dept_lng", 0);

        saveActiveTrackingState();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermissionAndFetch();

        TextView tvDetail = findViewById(R.id.deptDetail);
        if (tvDetail != null) tvDetail.setText("Responding: " + deptName);

        findViewById(R.id.btnViewGoogleMapsETA).setOnClickListener(v -> launchGoogleMapsNavigation());

        final android.view.View collapsibleContent = findViewById(R.id.collapsibleContent);
        final android.widget.ImageButton btnToggleExpand = findViewById(R.id.btnToggleExpand);

        btnToggleExpand.setOnClickListener(v -> {
            if (isCardExpanded) {
                collapsibleContent.setVisibility(android.view.View.GONE);
                btnToggleExpand.setImageResource(android.R.drawable.arrow_up_float);
                isCardExpanded = false;
            } else {
                collapsibleContent.setVisibility(android.view.View.VISIBLE);
                btnToggleExpand.setImageResource(android.R.drawable.arrow_down_float);
                isCardExpanded = true;
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        videoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        if (videoUri != null) uploadVideoToFirebase(videoUri);
                    }
                });

        findViewById(R.id.btnBack).setOnClickListener(v -> navigateToDashboardHome());
        findViewById(R.id.btnCall).setOnClickListener(v -> {
            if (deptPhone != null && !deptPhone.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + deptPhone)));
            }
        });
        findViewById(R.id.btnRecord).setOnClickListener(v -> checkCameraPermission());

        SeekBar btnSwipe = findViewById(R.id.btnSwipe);
        btnSwipe.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 99) {
                    if (!isAdminResolved) {
                        // REQUIREMENT 2: Block swipe if admin hasn't resolved
                        seekBar.setProgress(0);
                        Toast.makeText(TrackingActivity.this, "Emergency has not been resolved by admin yet. Cannot swipe.", Toast.LENGTH_LONG).show();
                    } else {
                        stopActiveRingtone();
                        seekBar.setEnabled(false);
                        clearTrackingState();
                        confirmArrivalWithFirebase();
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { if (seekBar.getProgress() < 99) seekBar.setProgress(0); }
        });

        setupLiveETATracking();
        listenForAdminResolution();
    }

    private void listenForAdminResolution() {
        if (deptId == null || alertKey == null) return;
        
        // We listen to the status in ProcessingAlerts. 
        // If it moves to ResolvedAlerts, we can also check that, 
        // but typically the status "Resolved" is set before deletion from Processing.
        statusDatabaseRef = FirebaseDatabase.getInstance().getReference("ProcessingAlerts").child(deptId).child(alertKey);
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    // If status is "Resolved", we allow swipe.
                    if ("Resolved".equalsIgnoreCase(status)) {
                        isAdminResolved = true;
                    }
                } else {
                    // If it's gone from ProcessingAlerts, check if it's in ResolvedAlerts
                    checkIfInResolvedNode();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        statusDatabaseRef.addValueEventListener(statusListener);
    }

    private void checkIfInResolvedNode() {
        DatabaseReference resRef = FirebaseDatabase.getInstance().getReference("ResolvedAlerts").child(deptId).child(alertKey);
        resRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isAdminResolved = true;
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void navigateToDashboardHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void saveActiveTrackingState() {
        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("has_active_emergency", true);
        editor.putString("alert_key", alertKey);
        editor.putString("dept_id", deptId);
        editor.apply();
    }

    private void clearTrackingState() {
        getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE).edit().clear().apply();
    }

    private void setupLiveETATracking() {
        if (deptId == null || alertKey == null) return;
        trackingDatabaseRef = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(deptId).child(alertKey);
        trackingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double dLat = snapshot.child("deptLat").getValue(Double.class);
                    Double dLng = snapshot.child("deptLng").getValue(Double.class);
                    if (dLat != null && dLng != null) {
                        deptLat = dLat; deptLng = dLng;
                        updateMapWithRoute();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        trackingDatabaseRef.addValueEventListener(trackingListener);
    }

    private void confirmArrivalWithFirebase() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        root.child("ResolvedAlerts").child(deptId).child(alertKey).child("status").setValue("Confirmed");
        
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            root.child("UserHistory").child(uid).child(alertKey).child("status").setValue("Confirmed");
        }

        Toast.makeText(this, "Arrival confirmed!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void uploadVideoToFirebase(Uri videoUri) {
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://jerapp-2026.firebasestorage.app");
        StorageReference ref = storage.getReference().child("emergency_evidence/" + alertKey + "/" + System.currentTimeMillis() + ".mp4");
        ref.putFile(videoUri).addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(url -> {
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ProcessingAlerts").child(deptId).child(alertKey).child("video_urls");
                dbRef.push().setValue(url.toString());
            });
        });
    }

    private void launchGoogleMapsNavigation() {
        if (deptLat == 0 || userLoc == null) return;
        String mapUri = String.format(java.util.Locale.US, "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f", deptLat, deptLng, userLoc.latitude, userLoc.longitude);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mapUri)));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateMapWithRoute();
    }

    private void updateMapWithRoute() {
        if (mMap == null || userLoc == null || deptLat == 0) return;
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(userLoc).title("You"));
        mMap.addMarker(new MarkerOptions().position(new LatLng(deptLat, deptLng)).title(deptName));
        fitMapBounds();
    }

    private void fitMapBounds() {
        if (mMap == null || userLoc == null || deptLat == 0) return;
        LatLng deptLoc = new LatLng(deptLat, deptLng);
        com.google.android.gms.maps.model.LatLngBounds bounds = new com.google.android.gms.maps.model.LatLngBounds.Builder().include(userLoc).include(deptLoc).build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    updateMapWithRoute();
                }
            });
        }
    }

    private void checkCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            videoLauncher.launch(new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE));
        }
    }

    private void stopActiveRingtone() {
        if (alarmRingtone != null && alarmRingtone.isPlaying()) alarmRingtone.stop();
    }

    @Override
    protected void onDestroy() {
        if (trackingDatabaseRef != null && trackingListener != null) trackingDatabaseRef.removeEventListener(trackingListener);
        if (statusDatabaseRef != null && statusListener != null) statusDatabaseRef.removeEventListener(statusListener);
        super.onDestroy();
    }
}
