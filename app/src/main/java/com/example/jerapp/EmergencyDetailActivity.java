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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.media.MediaPlayer;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Map;



public class EmergencyDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String alertKey;
    private GoogleMap mMap;
    private double userLat, userLng;
    private TextView tvName, tvPhone, tvEmail, tvAddress, tvType, tvCoords, tvGender, tvTimestamp;
    private VideoView videoView;
    private String deptId;
    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private VideoGalleryAdapter adapter;
    private List<String> currentVideoUrls;
    private ValueEventListener currentListener;

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
        tvGender = findViewById(R.id.detGender);
        tvTimestamp = findViewById(R.id.detTimestamp);
        deptId = getIntent().getStringExtra("dept_id");
        alertKey = getIntent().getStringExtra("alert_key");
        playerView = findViewById(R.id.playerView);

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

        // Use a reference specifically for this path
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(paths[index])
                .child(deptId)
                .child(alertKey);

        // Use SingleValueEvent to get the initial data without it constantly refreshing
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Log the raw snapshot structure
                            Log.d("FIREBASE_DEBUG", "Raw JSON: " + snapshot.getValue().toString());

                            AlertModel alert = snapshot.getValue(AlertModel.class);
                            // ADD THIS: If Name is null, it means Firebase couldn't map the data!
                            if (alert.getUserName() == null && alert.getUserPhone() == null) {
                                Log.e("DEBUG_DATA", "Firebase returned an empty/null object! Check your AlertModel structure.");
                            } else {
                                updateUI(alert);

                                // Inside loadAlertDetails, specifically inside the onDataChange method:
                                if (alert.getVideoUrls() != null) {
                                    setupVideoGallery(alert.getVideoUrls());
                                } else {
                                    Log.d("DEBUG_VIDEO", "Video list is null, skipping gallery setup.");
                                    // Optionally: hide the RecyclerView to avoid empty state issues
                                    findViewById(R.id.rvVideoGallery).setVisibility(View.GONE);
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

    private void setupVideoGallery(List<String> urls) {
        if (adapter == null) {
            RecyclerView rv = findViewById(R.id.rvVideoGallery);
            android.widget.ProgressBar progressBar = findViewById(R.id.videoProgressBar);
            currentVideoUrls = new ArrayList<>(urls);

            adapter = new VideoGalleryAdapter(this, currentVideoUrls, url -> {
                // 1. Ensure the container is visible
                playerView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);

                // Get screen width to calculate a 16:9 aspect ratio height
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int calculatedHeight = (int) (screenWidth * 9.0 / 16.0); // 16:9 ratio

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        calculatedHeight
                );

                playerView.setLayoutParams(params);

                // 3. Initialize ExoPlayer if null
                if (exoPlayer == null) {
                    exoPlayer = new androidx.media3.exoplayer.ExoPlayer.Builder(this).build();
                    playerView.setPlayer(exoPlayer);
                }

                // 4. Load and Play
                androidx.media3.common.MediaItem mediaItem = androidx.media3.common.MediaItem.fromUri(url);
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();

                // 5. Listener for when it's ready
                exoPlayer.addListener(new androidx.media3.common.Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == androidx.media3.common.Player.STATE_READY) {
                            progressBar.setVisibility(View.GONE);
                            exoPlayer.play();
                        }
                    }

                    @Override
                    public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EmergencyDetailActivity.this, "Playback error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });

            rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rv.setAdapter(adapter);
        } else {
            adapter.updateData(urls);
        }
    }

    private void updateUI(AlertModel alert) {
        // 1. Add a safety check: if data is missing, don't wipe existing UI
        if (alert == null) return;

        // ADD THIS LOG TO SEE IF DATA IS ACTUALLY NULL
        Log.d("DEBUG_DATA", "Name: " + alert.getUserName() + " | Phone: " + alert.getUserPhone());

        userLat = alert.getUserLat();
        userLng = alert.getUserLng();

        tvName.setText(alert.getUserName() != null ? alert.getUserName() : "Unknown");
        tvPhone.setText("Phone: " + (alert.getUserPhone() != null ? alert.getUserPhone() : "N/A"));
        tvEmail.setText("Email: " + (alert.getUserEmail() != null ? alert.getUserEmail() : "N/A"));
        tvAddress.setText("Address: " + (alert.getTextAddress() != null ? alert.getTextAddress() : "N/A"));
        tvType.setText("Emergency: " + (alert.getEmergencyType() != null ? alert.getEmergencyType().toUpperCase() : "N/A"));
        tvCoords.setText("Lat: " + userLat + " | Lng: " + userLng);
        tvGender.setText("Gender: " + (alert.getGender() != null ? alert.getGender() : "N/A"));

        if (alert.getTimestamp() > 0) {
            String time = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date(alert.getTimestamp()));
            tvTimestamp.setText("Time: " + time);
        }
        updateMapLocation();
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

    @Override
    protected void onStop() {
        super.onStop();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}