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

public class EmergencyDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String alertKey;
    private GoogleMap mMap;
    private double userLat, userLng;
    private TextView tvName, tvPhone, tvEmail, tvAddress, tvType, tvCoords;
    private VideoView videoView;

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

        // Back button (handle both toolbar and custom back button)
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        loadAlertDetails();

        // Single implementation for the Map Button
        findViewById(R.id.btnOpenInMaps).setOnClickListener(v -> {
            if (userLat != 0) {
                String uri = "google.navigation:q=" + userLat + "," + userLng;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        });
    }

    private void loadAlertDetails() {
        FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(alertKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        AlertModel alert = snapshot.getValue(AlertModel.class);
                        if (alert != null) {
                            userLat = alert.userLat;
                            userLng = alert.userLng;

                            tvName.setText(alert.userName);
                            tvPhone.setText("Phone: " + alert.userPhone);
                            tvEmail.setText("Email: " + alert.userEmail);
                            tvAddress.setText("Address: " + alert.textAddress);
                            tvType.setText("Emergency: " + alert.emergencyType.toUpperCase());
                            tvCoords.setText("Lat: " + userLat + " | Lng: " + userLng);

                            updateMapLocation();

                            // Setup Video with Media Controller
                            if (alert.getVideoUrl() != null && !alert.getVideoUrl().isEmpty()) {
                                MediaController mediaController = new MediaController(EmergencyDetailActivity.this);
                                mediaController.setAnchorView(videoView);
                                videoView.setMediaController(mediaController);
                                videoView.setVideoURI(Uri.parse(alert.getVideoUrl()));
                                // Optional: videoView.start(); // Auto-play might be blocked on some devices
                            }
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