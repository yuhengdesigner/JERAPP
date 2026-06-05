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

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String deptName, deptPhone, deptId, alertKey;
    private double deptLat, deptLng;
    private ActivityResultLauncher<Intent> videoLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLoc;
    private TextView tvCountdownETA;
    private EditText etTimerInput;
    private android.os.CountDownTimer etaTimer;
    private Button btnStartUserTimer, btnViewGoogleMapsETA;
    private Ringtone alarmRingtone;
    private DatabaseReference trackingDatabaseRef;
    private ValueEventListener trackingListener;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private android.view.View collapsibleContent;
    private android.widget.ImageButton btnToggleExpand;
    private boolean isCardExpanded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Generate ID immediately so videos can be uploaded during transit
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

        tvCountdownETA = findViewById(R.id.tvCountdownETA);
        etTimerInput = findViewById(R.id.etTimerInput);
        btnStartUserTimer = findViewById(R.id.btnStartUserTimer);
        btnViewGoogleMapsETA = findViewById(R.id.btnViewGoogleMapsETA);

        // Bind new Expandable Container items
        collapsibleContent = findViewById(R.id.collapsibleContent);
        btnToggleExpand = findViewById(R.id.btnToggleExpand);

        // Set up the Click Listener toggle action
        btnToggleExpand.setOnClickListener(v -> {
            if (isCardExpanded) {
                // Collapse: Hide details and turn arrow UP
                collapsibleContent.setVisibility(android.view.View.GONE);
                btnToggleExpand.setImageResource(android.R.drawable.arrow_up_float);
                isCardExpanded = false;
            } else {
                // Expand: Show details and turn arrow DOWN
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
                        uploadVideoToFirebase(result.getData().getData());
                    }
                });

        // 1. Trigger Google Maps routing Intent FROM Department TO User
        btnViewGoogleMapsETA.setOnClickListener(v -> launchGoogleMapsNavigation());

        // 2. Set up local timer execution
        btnStartUserTimer.setOnClickListener(v -> processUserManualTimerInput());

        // FIX: Re-routed to navigate cleanly to MainActivity without executing finish()
        findViewById(R.id.btnBack).setOnClickListener(v -> navigateToDashboardHome());

        // Handle system back press via modern Callback handler API
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToDashboardHome();
            }
        });

        findViewById(R.id.btnCall).setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + deptPhone));
            startActivity(callIntent);
        });

        // Inside onCreate
        findViewById(R.id.btnRecord).setOnClickListener(v -> checkCameraPermission());

        SeekBar btnSwipe = findViewById(R.id.btnSwipe);
        btnSwipe.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 99) {
                    stopActiveRingtone();
                    confirmArrivalWithFirebase();
                    seekBar.setEnabled(false);

                    // Navigate to UserHistoryActivity
                    startActivity(new Intent(TrackingActivity.this, UserHistoryActivity.class));
                    finish();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { if (seekBar.getProgress() < 99) seekBar.setProgress(0); }
        });

        setupLiveETATracking();
    }

    private void navigateToDashboardHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // Finish current activity instance safely, state is backed up in SharedPrefs
    }

    private void saveActiveTrackingState() {
        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("has_active_emergency", true);
        editor.putString("alert_key", alertKey);
        editor.putString("dept_name", deptName);
        editor.putString("dept_phone", deptPhone);
        editor.putString("dept_id", deptId);
        editor.putLong("dept_lat_bits", Double.doubleToRawLongBits(deptLat));
        editor.putLong("dept_lng_bits", Double.doubleToRawLongBits(deptLng));
        editor.apply();
    }

    private void clearTrackingState() {
        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private void restoreExistingCountdown() {
        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        long endTimeMs = prefs.getLong("timer_end_time_ms", 0);
        if (endTimeMs > System.currentTimeMillis()) {
            startUserETACountdown(endTimeMs - System.currentTimeMillis());
        }
    }

    private void launchGoogleMapsNavigation() {
        if (deptLat == 0 || userLoc == null) {
            Toast.makeText(this, "Awaiting current location mapping coordinates...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construct explicit query directing: Origin = Department -> Destination = User Location
        String mapUriString = String.format(java.util.Locale.US,
                "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                deptLat, deptLng, userLoc.latitude, userLoc.longitude);

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUriString));
        mapIntent.setPackage("com.google.android.apps.maps"); // Pin targeted action explicitly to Google Maps App

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback parsing case if testing inside an open emulator without consumer Google Framework installed
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mapUriString)));
        }
    }

    private void processUserManualTimerInput() {
        String inputText = etTimerInput.getText().toString().trim();
        if (inputText.isEmpty()) {
            Toast.makeText(this, "Please enter an arrival estimate value.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long inputMinutes = Long.parseLong(inputText);
            if (inputMinutes <= 0) {
                Toast.makeText(this, "Please input a baseline duration over 0.", Toast.LENGTH_SHORT).show();
                return;
            }

            stopActiveRingtone(); // Clear sound if a previous loop is running
            long totalDurationMs = inputMinutes * 60 * 1000;
            startUserETACountdown(totalDurationMs);
            Toast.makeText(this, "Timer configured for " + inputMinutes + " mins.", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number metric formatted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startUserETACountdown(long durationMs) {
        if (etaTimer != null) {
            etaTimer.cancel();
        }

        etaTimer = new android.os.CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvCountdownETA.setText(String.format(java.util.Locale.getDefault(),
                        "Ringing in: %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvCountdownETA.setText("Time's Up! Responders Due.");
                playDeviceAlarmSound();
            }
        }.start();
    }

    private void playDeviceAlarmSound() {
        try {
            // Pull the phone's native default ALARM notification configuration stream route
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                // Fallback option in case device profile defaults are locked out
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            alarmRingtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
            if (alarmRingtone != null) {
                alarmRingtone.play();
                Toast.makeText(this, "Timer Finished! Checking arrival status.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("ALARM_SOUND_ERROR", "Could not trigger alarm: " + e.getMessage());
        }
    }

    private void stopActiveRingtone() {
        if (alarmRingtone != null && alarmRingtone.isPlaying()) {
            alarmRingtone.stop();
        }
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
                    Double uLat = snapshot.child("userLat").getValue(Double.class);
                    Double uLng = snapshot.child("userLng").getValue(Double.class);

                    if (uLat == null || uLng == null) {
                        if (userLoc != null) {
                            uLat = userLoc.latitude;
                            uLng = userLoc.longitude;
                        }
                    }

                    if (dLat != null && dLng != null && uLat != null && uLng != null) {
                        deptLat = dLat;
                        deptLng = dLng;
                        userLoc = new LatLng(uLat, uLng);

                        updateMapWithRoute();
                        fitMapBounds();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_TRACK", error.getMessage());
            }
        };

        trackingDatabaseRef.addValueEventListener(trackingListener);
    }

    private void confirmArrivalWithFirebase() {
        // Check if key or deptId is missing
        if (alertKey == null || deptId == null) {
            Toast.makeText(this, "Error: Missing Alert details. Cannot confirm.", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        // Simply update the status of the EXISTING alert
        root.child("ActiveAlerts").child(deptId).child(alertKey).child("status").setValue("Confirmed");

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            root.child("UserHistory").child(uid).child(alertKey).child("status").setValue("Confirmed");
        }

        Toast.makeText(TrackingActivity.this, "Arrival confirmed!", Toast.LENGTH_SHORT).show();

        // After the arrival logic is successful
        Intent intent = new Intent(TrackingActivity.this, MainActivity.class);

        // Clear the backstack so the user can't "back" into the tracking session
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        // Send the command to open the history tab
        intent.putExtra("NAVIGATE_TO", "HISTORY");

        startActivity(intent);
        finish(); // Close tracking page
    }

    private void uploadVideoToFirebase(Uri videoUri) {
        // Safety check: ensure alertKey isn't empty or null before handling database calls
        if (alertKey == null || alertKey.trim().isEmpty()) {
            Toast.makeText(this, "Error: Lost transaction key context. Cannot sync video.", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference ref = FirebaseStorage.getInstance().getReference("Evidence/" + alertKey + "/" + System.currentTimeMillis() + ".mp4");
        ref.putFile(videoUri).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            DatabaseReference alertRef = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(deptId).child(alertKey).child("videoUrls");

            // Inside TrackingActivity.java
            alertRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    // 1. Get the current value as a list
                    List<String> list = mutableData.getValue(new GenericTypeIndicator<List<String>>() {});

                    // 2. If it's null (first time), initialize a new ArrayList
                    if (list == null) {
                        list = new ArrayList<>();
                    }

                    // 3. Add the new video
                    list.add(uri.toString());

                    // 4. Set the list back to the database
                    mutableData.setValue(list);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    if (committed) {
                        Toast.makeText(TrackingActivity.this, "Evidence video synced successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("DATABASE_WRITE_ERROR", "Transaction failure: " + (error != null ? error.getMessage() : "unknown"));
                    }
                }
            });
        })).addOnFailureListener(e -> {
            Toast.makeText(TrackingActivity.this, "Failed uploading evidence file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void launchCamera() {
        Intent takeVideoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        // Add this line to ensure the camera app knows we want to handle the result
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            videoLauncher.launch(takeVideoIntent);
        } else {
            Toast.makeText(this, "No camera app found!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateMapWithRoute();
        fitMapBounds();
    }

    private void updateMapWithRoute() {
        // Add a null check for mMap here!
        if (mMap == null || userLoc == null || deptLat == 0) {
            return;
        }

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(userLoc).title("You"));
        mMap.addMarker(new MarkerOptions().position(new LatLng(deptLat, deptLng)).title(deptName));
    }

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    updateMapWithRoute();
                    fitMapBounds();
                }
            });
        }
    }
    private void checkCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 100);
        } else {
            launchCamera();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to record evidence.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void fitMapBounds() {
        if (mMap == null || userLoc == null || deptLat == 0) return;

        LatLng deptLoc = new LatLng(deptLat, deptLng);

        // Create a builder for the bounds
        com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
        builder.include(userLoc);
        builder.include(deptLoc);

        // Create the bounds
        com.google.android.gms.maps.model.LatLngBounds bounds = builder.build();

        // Set padding in pixels (e.g., 100px) so markers aren't right at the edge of the screen
        int padding = 100;

        // Animate the camera to fit the bounds
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    @Override
    protected void onDestroy() {
        // FIX: Detach the Firebase database listener to resolve background background execution leaks
        if (trackingDatabaseRef != null && trackingListener != null) {
            trackingDatabaseRef.removeEventListener(trackingListener);
        }
        if (etaTimer != null) {
            etaTimer.cancel();
        }
        super.onDestroy();
    }
}