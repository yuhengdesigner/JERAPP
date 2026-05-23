package com.example.jerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ValueEventListener;

public class EmergencyDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String alertKey;
    private GoogleMap mMap;
    private double userLat, userLng;
    private TextView tvName, tvPhone, tvEmail, tvAddress, tvType, tvCoords, tvGender, tvTimestamp;
    private VideoView videoView;
    private String deptId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_detail);

        alertKey = getIntent().getStringExtra("alert_key");

        // Initialize Views
        tvName = findViewById(R.id.detName);
        tvPhone = findViewById(R.id.detPhone);
        tvEmail = findViewById(R.id.detEmail);
        tvAddress = findViewById(R.id.detAddress);
        tvType = findViewById(R.id.detType);
        tvCoords = findViewById(R.id.tvCoordinates);
        videoView = findViewById(R.id.videoView);
        tvGender = findViewById(R.id.detGender);
        tvTimestamp = findViewById(R.id.detTimestamp);
        deptId = getIntent().getStringExtra("dept_id");
        alertKey = getIntent().getStringExtra("alert_key");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        if (alertKey != null && deptId != null) {
            loadAlertDetails(0); // Start searching from index 0
        }

        findViewById(R.id.btnOpenInMaps).setOnClickListener(v -> {
            if (userLat != 0) {
                String uri = "google.navigation:q=" + userLat + "," + userLng;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        });
    }

    private void loadAlertDetails(int index) {
        String[] paths = {"ActiveAlerts", "ProcessingAlerts", "ResolvedAlerts"};
        if (index >= paths.length) return;

        // Correct Path: Node / DeptId / AlertKey
        FirebaseDatabase.getInstance().getReference(paths[index])
                .child(deptId)
                .child(alertKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            AlertModel alert = snapshot.getValue(AlertModel.class);
                            if (alert != null) {
                                userLat = alert.userLat;
                                userLng = alert.userLng;

                                tvName.setText(alert.userName != null ? alert.userName : "Unknown");
                                tvPhone.setText("Phone: " + (alert.userPhone != null ? alert.userPhone : "N/A"));
                                tvEmail.setText("Email: " + (alert.userEmail != null ? alert.userEmail : "N/A"));
                                tvAddress.setText("Address: " + (alert.textAddress != null ? alert.textAddress : "N/A"));
                                tvType.setText("Emergency: " + (alert.emergencyType != null ? alert.emergencyType.toUpperCase() : "N/A"));
                                tvCoords.setText("Lat: " + userLat + " | Lng: " + userLng);
                                tvGender.setText("Gender: " + (alert.getGender() != null ? alert.getGender() : "N/A"));

                                if (alert.getTimestamp() > 0) {
                                    String time = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                                            .format(new java.util.Date(alert.getTimestamp()));
                                    tvTimestamp.setText("Time: " + time);
                                }

                                updateMapLocation();

                                if (alert.getVideoUrl() != null && !alert.getVideoUrl().isEmpty()) {
                                    MediaController mediaController = new MediaController(EmergencyDetailActivity.this);
                                    mediaController.setAnchorView(videoView);
                                    videoView.setMediaController(mediaController);
                                    videoView.setVideoURI(Uri.parse(alert.getVideoUrl()));
                                }
                            }
                        } else {
                            // Recursively try the next path if not found in this one
                            loadAlertDetails(index + 1);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateMapLocation();
    }

    private void updateMapLocation() {
        if (mMap != null && userLat != 0) {
            LatLng location = new LatLng(userLat, userLng);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(location).title("Victim Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
        }
    }
}