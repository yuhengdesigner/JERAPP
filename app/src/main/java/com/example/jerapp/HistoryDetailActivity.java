//package com.example.jerapp;
//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.net.Uri;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import androidx.media3.common.MediaItem;
//import androidx.media3.exoplayer.ExoPlayer;
//import androidx.media3.ui.PlayerView;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Locale;
//
//public class HistoryDetailActivity extends AppCompatActivity {
//
//    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
//
//    private TextView tvDept, tvDate, tvTitle;
//    private Button btnCall, btnNavigate, btnCallAgain;
//    private PlayerView playerView;
//    private ExoPlayer player;
//    private TextView tvUserName, tvUserGender, tvUserContact, tvUserEmail, tvUserAddress, tvUserTimestamp;
//    private TextView tvDetailType, tvDetailAddress, tvDetailDate;
//
//    // Cache the alert model content mapping locally for dynamic button actions
//    private AlertModel currentAlert;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_history_detail);
//
//        // Bind layout views
//        playerView = findViewById(R.id.playerView);
//        tvDept = findViewById(R.id.tvDetailDept);
//        tvDate = findViewById(R.id.tvDetailDate);
//        btnCall = findViewById(R.id.btnCall);
//        btnNavigate = findViewById(R.id.btnNavigate);
//        btnCallAgain = findViewById(R.id.btnCallAgain); // Bound to "Call Emergency Again"
//
//        tvUserName = findViewById(R.id.tvUserName);
//        tvUserGender = findViewById(R.id.tvUserGender);
//        tvUserContact = findViewById(R.id.tvUserContact);
//        tvUserEmail = findViewById(R.id.tvUserEmail);
//        tvUserAddress = findViewById(R.id.tvUserAddress);
//        tvUserTimestamp = findViewById(R.id.tvUserTimestamp);
//
//        tvDetailType = findViewById(R.id.tvDetailType);
//        tvDetailAddress = findViewById(R.id.tvDetailAddress);
//        tvDetailDate = findViewById(R.id.tvDetailDate);
//
//        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
//
//        String alertKey = getIntent().getStringExtra("alert_key");
//        loadAlertDetails(alertKey);
//    }
//
//    private void loadAlertDetails(String key) {
//        if (key == null) return;
//
//        // Fetching details from the User's resolved alert nodes
//        FirebaseDatabase.getInstance().getReference("ResolvedAlerts").child(key)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        if (!snapshot.exists() || snapshot.getValue() == null) return;
//
//                        currentAlert = snapshot.getValue(AlertModel.class);
//                        if (currentAlert == null) return;
//
//                        // Map fields securely
//                        tvUserName.setText("Name: " + currentAlert.getUserName());
//                        tvUserGender.setText("Gender: " + currentAlert.getGender());
//                        tvUserContact.setText("Contact: " + currentAlert.getUserPhone());
//                        tvUserEmail.setText("Email: " + currentAlert.getUserEmail());
//                        tvUserAddress.setText("Address: " + currentAlert.getTextAddress());
//
//                        tvDetailType.setText("Emergency Type: " + (currentAlert.getEmergencyType() != null ? currentAlert.getEmergencyType().toUpperCase() : "N/A"));
//                        tvDetailAddress.setText("Address: " + currentAlert.getTextAddress());
//                        tvDept.setText("Department: " + currentAlert.getDeptName());
//
//                        // === FIXED TIMESTAMP PARSING BLOCK ===
//                        String formattedTime = "Unavailable";
//                        Object rawTimestamp = currentAlert.getTimestamp();
//
//                        if (rawTimestamp != null) {
//                            if (rawTimestamp instanceof Long) {
//                                long timeLong = (Long) rawTimestamp;
//                                if (timeLong > 0) {
//                                    formattedTime = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(timeLong));
//                                }
//                            } else if (rawTimestamp instanceof String) {
//                                String timeString = (String) rawTimestamp;
//                                if (timeString.contains(".") && timeString.length() > 16) {
//                                    try {
//                                        timeString = timeString.substring(0, 16);
//                                    } catch (Exception ignored) {}
//                                }
//                                formattedTime = timeString;
//                            } else {
//                                formattedTime = rawTimestamp.toString();
//                            }
//                        }
//
//                        if (tvDetailDate != null) tvDetailDate.setText("Timestamp: " + formattedTime);
//                        if (tvDate != null) tvDate.setText("Date: " + formattedTime);
//                        if (tvUserTimestamp != null) tvUserTimestamp.setText("Time: " + formattedTime);
//
//                        // Status Color Mapping
//                        String status = currentAlert.getStatus();
//                        TextView tvStatusView = findViewById(R.id.tvStatus);
//                        if (tvStatusView != null) {
//                            tvStatusView.setText(status != null ? status.toUpperCase() : "PENDING");
//                            if ("COMPLETED".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status)) {
//                                tvStatusView.setTextColor(ContextCompat.getColor(HistoryDetailActivity.this, android.R.color.holo_green_dark));
//                            } else if ("FAILED".equalsIgnoreCase(status)) {
//                                tvStatusView.setTextColor(ContextCompat.getColor(HistoryDetailActivity.this, android.R.color.holo_red_dark));
//                            } else {
//                                tvStatusView.setTextColor(ContextCompat.getColor(HistoryDetailActivity.this, android.R.color.holo_orange_dark));
//                            }
//                        }
//
//                        // === REQUIREMENT 3: VIDEO VIEW CONTAINER AS EMPTY PLACEHOLDER ===
//                        if (currentAlert.getVideoUrl() != null && !currentAlert.getVideoUrl().isEmpty()) {
//                            // If video exists, configure ExoPlayer natively
//                            player = new ExoPlayer.Builder(HistoryDetailActivity.this).build();
//                            playerView.setPlayer(player);
//                            MediaItem mediaItem = MediaItem.fromUri(currentAlert.getVideoUrl());
//                            player.setMediaItem(mediaItem);
//                            player.prepare();
//                        } else {
//                            // If no video, force display layout container to stay visible as a dark empty box placeholder
//                            playerView.setVisibility(View.VISIBLE);
//                            playerView.setPlayer(null); // Attach no player engine so it renders a clean black canvas
//                        }
//
//                        // Action Listeners
//                        setupActionListeners();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {}
//                });
//    }
//
//    private void setupActionListeners() {
//        if (currentAlert == null) return;
//
//        // Dial Department Contact
//        btnCall.setOnClickListener(v -> {
//            if (currentAlert.getDeptPhone() != null && !currentAlert.getDeptPhone().isEmpty()) {
//                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentAlert.getDeptPhone()));
//                startActivity(intent);
//            } else {
//                Toast.makeText(this, "Department phone number unavailable.", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // === REQUIREMENT 1: VIEW DEPT LOCATION & NAVIGATE EASILY ===
//        btnNavigate.setOnClickListener(v -> {
//            if (currentAlert.getDeptLat() != 0 && currentAlert.getDeptLng() != 0) {
//                // Creates a precise navigation URI pointing from user current spot to target Department Coordinates
//                String navigationUrl = "google.navigation:q=" + currentAlert.getDeptLat() + "," + currentAlert.getDeptLng();
//                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(navigationUrl));
//                mapIntent.setPackage("com.google.android.apps.maps"); // Forces native Google Maps Application routing
//
//                if (mapIntent.resolveActivity(getPackageManager()) != null) {
//                    startActivity(mapIntent);
//                } else {
//                    // Universal Web Fallback if application missing on target test layout hardware
//                    String webFallbackUrl = "https://www.google.com/maps/dir/?api=1&destination=" + currentAlert.getDeptLat() + "," + currentAlert.getDeptLng();
//                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webFallbackUrl)));
//                }
//            } else {
//                Toast.makeText(this, "Department coordinates are missing for this record.", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // === REQUIREMENT 2: CALL EMERGENCY AGAIN WITH LOCATION CHECK ===
//        btnCallAgain.setOnClickListener(v -> checkLocationPermissionAndProceed());
//    }
//
//    private void checkLocationPermissionAndProceed() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            // Permission already granted -> proceed directly to confirm emergency screen
//            navigateToConfirmationScreen();
//        } else {
//            // Request permissions at runtime
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                    LOCATION_PERMISSION_REQUEST_CODE);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // User clicked "Allow" -> forward to destination activity flow
//                navigateToConfirmationScreen();
//            } else {
//                // User denied access
//                Toast.makeText(this, "Location permission is required to report an emergency.", Toast.LENGTH_LONG).show();
//            }
//        }
//    }
//
//    private void navigateToConfirmationScreen() {
//        if (currentAlert == null) return;
//
//        Intent intent = new Intent(HistoryDetailActivity.this, ConfirmationActivity.class);
//        // Forward historical setup fields ahead to pre-populate selected categories on confirmation page
//        intent.putExtra("emergency_type", currentAlert.getEmergencyType());
//        intent.putExtra("dept_id", currentAlert.getDept_id());
//        intent.putExtra("dept_name", currentAlert.getDeptName());
//        intent.putExtra("dept_phone", currentAlert.getDeptPhone());
//        intent.putExtra("dept_lat", currentAlert.getDeptLat());
//        intent.putExtra("dept_lng", currentAlert.getDeptLng());
//
//        startActivity(intent);
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (player != null) {
//            player.stop();
//            player.release();
//            player = null;
//        }
//    }
//}