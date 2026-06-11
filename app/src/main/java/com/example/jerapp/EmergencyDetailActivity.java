package com.example.jerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import com.google.firebase.database.DatabaseReference;

public class EmergencyDetailActivity extends AppCompatActivity {

    private String alertKey;
    private GoogleMap mMap;
    private double userLat = 0, userLng = 0;
    private TextView tvName, tvPhone, tvEmail, tvAddress, tvType, tvCoords, tvGender, tvTimestamp;
    private String deptId;
    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private VideoGalleryAdapter videoGalleryAdapter;
    private RecyclerView rvVideoGallery;
    private List<String> videoUrlList = new ArrayList<>();
    private DatabaseReference detailRef;
    private ValueEventListener detailListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_detail);

        alertKey = getIntent().getStringExtra("alert_key");
        deptId = getIntent().getStringExtra("dept_id");

        // Initialize Views
        tvName = findViewById(R.id.detName);
        tvPhone = findViewById(R.id.detPhone);
        tvEmail = findViewById(R.id.detEmail);
        tvAddress = findViewById(R.id.detAddress);
        tvType = findViewById(R.id.detType);
        tvCoords = findViewById(R.id.tvCoordinates);
        tvGender = findViewById(R.id.detGender);
        tvTimestamp = findViewById(R.id.detTimestamp);
        playerView = findViewById(R.id.playerView);

        rvVideoGallery = findViewById(R.id.rvVideoGallery);
        rvVideoGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        videoGalleryAdapter = new VideoGalleryAdapter(this, videoUrlList, url -> playVideo(url));
        rvVideoGallery.setAdapter(videoGalleryAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Initialize ONE Map (Victim Location) for Admin
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mMap = googleMap;
                updateMapLocation();
            });
        }

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

    private void playVideo(String url) {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(exoPlayer);
        }
        playerView.setVisibility(View.VISIBLE);
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void loadAlertDetails(int index) {
        String[] paths = {"ActiveAlerts", "ProcessingAlerts", "ResolvedAlerts"};
        if (index >= paths.length) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(paths[index])
                .child(deptId)
                .child(alertKey);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Once found, attach a LIVE listener to this specific node
                    attachLiveDetailListener(ref);
                } else {
                    loadAlertDetails(index + 1);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void attachLiveDetailListener(DatabaseReference ref) {
        if (detailRef != null && detailListener != null) {
            detailRef.removeEventListener(detailListener);
        }

        detailRef = ref;
        detailListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    AlertModel alert = snapshot.getValue(AlertModel.class);
                    if (alert != null) updateUI(alert);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        detailRef.addValueEventListener(detailListener);
    }

    private void updateUI(AlertModel alert) {
        userLat = alert.getUserLat();
        userLng = alert.getUserLng();

        tvName.setText(alert.getUserName() != null ? alert.getUserName() : "Unknown");
        tvPhone.setText("Phone: " + (alert.getUserPhone() != null ? alert.getUserPhone() : "N/A"));
        tvEmail.setText("Email: " + (alert.getUserEmail() != null ? alert.getUserEmail() : "N/A"));
        tvAddress.setText("Address: " + (alert.getTextAddress() != null ? alert.getTextAddress() : "N/A"));
        tvType.setText("Emergency: " + (alert.getEmergencyType() != null ? alert.getEmergencyType().toUpperCase() : "N/A"));
        tvCoords.setText("Lat: " + userLat + " | Lng: " + userLng);
        tvGender.setText("Gender: " + (alert.getGender() != null ? alert.getGender() : "N/A"));

        // Timestamp parsing
        Object rawTimestamp = alert.getTimestamp();
        if (rawTimestamp != null) {
            try {
                long timeLong = 0;
                if (rawTimestamp instanceof Long) timeLong = (Long) rawTimestamp;
                else if (rawTimestamp instanceof String) timeLong = Long.parseLong((String) rawTimestamp);
                
                if (timeLong > 0) {
                    String time = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(timeLong));
                    tvTimestamp.setText("Time: " + time);
                }
            } catch (Exception e) {
                tvTimestamp.setText("Time: " + rawTimestamp.toString());
            }
        }

        // Handle Video List Updates
        if (alert.getVideoUrls() != null && !alert.getVideoUrls().isEmpty()) {
            rvVideoGallery.setVisibility(View.VISIBLE);
            videoGalleryAdapter.updateData(alert.getVideoUrls());
        } else {
            rvVideoGallery.setVisibility(View.GONE);
        }

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

    @Override
    protected void onStop() {
        super.onStop();
        if (detailRef != null && detailListener != null) {
            detailRef.removeEventListener(detailListener);
        }
        if (playerView != null) playerView.setPlayer(null);
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
