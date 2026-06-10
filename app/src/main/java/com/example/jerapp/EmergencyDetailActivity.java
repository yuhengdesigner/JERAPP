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
    private double userLat = 0, userLng = 0;
    private TextView tvName, tvPhone, tvEmail, tvAddress, tvType, tvCoords, tvGender, tvTimestamp;
    private VideoView videoView;
    private String deptId;
    private ExoPlayer player;
    private PlayerView playerView;
    private VideoGalleryAdapter adapter;
    private List<String> currentVideoUrls;
    private ValueEventListener currentListener;
    private RecyclerView rvVideoGallery;
    private VideoGalleryAdapter videoGalleryAdapter;
    private List<String> videoUrlList = new ArrayList<>();

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

        rvVideoGallery = findViewById(R.id.rvVideoGallery);
        rvVideoGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        videoGalleryAdapter = new VideoGalleryAdapter(this, videoUrlList, new VideoGalleryAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(String url) {
                playVideo(url);
            }
        });
        rvVideoGallery.setAdapter(videoGalleryAdapter);

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

    private void playVideo(String url) {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
        }
        // Make the video player box visible instantly when a item link is pressed
        playerView.setVisibility(View.VISIBLE);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void parseAlertData(DataSnapshot snapshot) {
        if (!snapshot.exists()) return;

        tvName.setText("Victim: " + snapshot.child("name").getValue(String.class));
        tvPhone.setText("Phone: " + snapshot.child("phone").getValue(String.class));
        tvEmergencyType.setText("Type: " + snapshot.child("emergencyType").getValue(String.class));

        try {
            userLat = snapshot.child("latitude").getValue(Double.class);
            userLng = snapshot.child("longitude").getValue(Double.class);
            tvLocation.setText("Coordinates: " + userLat + ", " + userLng);
        } catch (Exception e) {
            Log.e("DataError", "Error parsing coordinates");
        }

        // CLEAR AND POPULATE VIDEO URLS
        videoUrlList.clear();
        DataSnapshot videoUrlsNode = snapshot.child("video_urls");
        if (videoUrlsNode.exists()) {
            for (DataSnapshot childUrl : videoUrlsNode.getChildren()) {
                String videoUrl = childUrl.getValue(String.class);
                if (videoUrl != null) {
                    videoUrlList.add(videoUrl);
                }
            }
        }
        // Force the layout gallery list matching your XML to redraw buttons instantly
        videoGalleryAdapter.updateData(videoUrlList);

        // Handle timestamps cleanly
        Object rawTimestamp = snapshot.child("timestamp").getValue();
        if (rawTimestamp != null) {
            if (rawTimestamp instanceof String) {
                String timeString = (String) rawTimestamp;
                if (timeString.length() > 16) {
                    try {
                        timeString = timeString.substring(0, 16);
                    } catch (Exception e) {}
                }
                tvTimestamp.setText("Time: " + timeString);
            } else {
                tvTimestamp.setText("Time: " + rawTimestamp.toString());
            }
        } else {
            tvTimestamp.setText("Time: Unavailable");
        }

        updateMapLocation();
    }

    private void loadEmergencyDetails() {
        if (alertKey == null || deptId == null) return;

        // FIX: Look inside ActiveAlerts matching TrackingActivity's save location
        DatabaseReference activeAlertRef = FirebaseDatabase.getInstance()
                .getReference("ActiveAlerts")
                .child(deptId)
                .child(alertKey);

        activeAlertRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Fallback: Check if it moved to ProcessingAlerts
                    FirebaseDatabase.getInstance().getReference("ProcessingAlerts")
                            .child(deptId).child(alertKey)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot procSnapshot) {
                                    parseAlertData(procSnapshot);
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                    return;
                }
                parseAlertData(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmergencyDetailActivity.this, "Failed to load details.", Toast.LENGTH_SHORT).show();
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
                if (snapshot.exists() && snapshot.getValue() != null) {
                    Log.d("FIREBASE_DEBUG", "Raw JSON: " + snapshot.getValue().toString());

                    AlertModel alert = snapshot.getValue(AlertModel.class);

                    //  FIXED: Guard against completely empty database snapshots mapping to null objects
                    if (alert == null) {
                        Log.e("DEBUG_DATA", "AlertModel mapping failed completely.");
                        return;
                    }

                    if (alert.getUserName() == null && alert.getUserPhone() == null) {
                        Log.e("DEBUG_DATA", "Firebase returned an empty/null data structure.");
                    } else {
                        updateUI(alert);

                        if (alert.getVideoUrls() != null) {
                            setupVideoGallery(alert.getVideoUrls());
                        } else {
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
                } else {
                    exoPlayer.stop();
                    exoPlayer.clearMediaItems();
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

        // === FIXED TIMESTAMP PARSING BLOCK ===
        Object rawTimestamp = alert.getTimestamp();

        if (rawTimestamp != null) {
            if (rawTimestamp instanceof Long) {
                // Case A: The timestamp is stored as a numerical Epoch millisecond value (Long)
                long timeLong = (Long) rawTimestamp;
                if (timeLong > 0) {
                    String time = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                            .format(new java.util.Date(timeLong));
                    tvTimestamp.setText("Time: " + time);
                } else {
                    tvTimestamp.setText("Time: Unavailable");
                }
            } else if (rawTimestamp instanceof String) {
                // Case B: The timestamp is already stored as a formatted String string value
                String timeString = (String) rawTimestamp;

                // Truncate long sub-second nanosecond precision traces for clean readability
                if (timeString.contains(".") && timeString.length() > 16) {
                    try {
                        timeString = timeString.substring(0, 16); // e.g., turns "2026-05-31 15:17:15..." to "2026-05-31 15:17"
                    } catch (Exception e) {
                        // Keep original if substring operation hits bounds exceptions
                    }
                }
                tvTimestamp.setText("Time: " + timeString);
            } else {
                // Fallback for any other data variants
                tvTimestamp.setText("Time: " + rawTimestamp.toString());
            }
        } else {
            tvTimestamp.setText("Time: Unavailable");
        }
        // =====================================

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
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}