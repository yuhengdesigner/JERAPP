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
    private EditText etTimerInput;
    private android.os.CountDownTimer etaTimer;
    private Button btnStartUserTimer, btnViewGoogleMapsETA;
    private Ringtone alarmRingtone;
    private DatabaseReference trackingDatabaseRef;
    private ValueEventListener trackingListener;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private android.view.View collapsibleContent;
    private android.widget.ImageButton btnToggleExpand;
    private java.io.File cameraVideoFile;
    private Uri pendingVideoUri;
    private boolean isCardExpanded = true;

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


        btnViewGoogleMapsETA = findViewById(R.id.btnViewGoogleMapsETA);

        collapsibleContent = findViewById(R.id.collapsibleContent);
        btnToggleExpand = findViewById(R.id.btnToggleExpand);

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
                    if (result.getResultCode() == RESULT_OK) {
                        if (cameraVideoFile != null && cameraVideoFile.exists() && cameraVideoFile.length() > 0) {
                            uploadVideoToFirebase(Uri.fromFile(cameraVideoFile));
                        }
                        else if (result.getData() != null && result.getData().getData() != null) {
                            Uri returnedUri = result.getData().getData();
                            copyContentUriAndUpload(returnedUri);
                        }
                        else if (pendingVideoUri != null) {
                            copyContentUriAndUpload(pendingVideoUri);
                        }
                        else {
                            Toast.makeText(this, "Could not retrieve the recorded video.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        btnViewGoogleMapsETA.setOnClickListener(v -> launchGoogleMapsNavigation());

        findViewById(R.id.btnBack).setOnClickListener(v -> navigateToDashboardHome());

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToDashboardHome();
            }
        });

        // Fixed Call Dept button for both guest and registered
        findViewById(R.id.btnCall).setOnClickListener(v -> {
            if (deptPhone != null && !deptPhone.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + deptPhone));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Department phone number unavailable.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnRecord).setOnClickListener(v -> checkCameraPermission());

        SeekBar btnSwipe = findViewById(R.id.btnSwipe);
        btnSwipe.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 99) {
                    stopActiveRingtone();
                    seekBar.setEnabled(false);
                    clearTrackingState();
                    confirmArrivalWithFirebase();
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
        finish();
    }

    private void saveActiveTrackingState() {
        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("has_active_emergency", true);
        editor.putString("alert_key", alertKey);
        editor.putString("active_alert_key", alertKey);
        editor.putString("dept_name", deptName);
        editor.putString("dept_phone", deptPhone);
        editor.putString("dept_id", deptId);
        editor.putLong("dept_lat_bits", Double.doubleToRawLongBits(deptLat));
        editor.putLong("dept_lng_bits", Double.doubleToRawLongBits(deptLng));
        editor.putBoolean("isGuestFlow", getIntent().getBooleanExtra("isGuestFlow", false));
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) editor.putString("owner_uid", uid);
        editor.apply();
    }

    private void clearTrackingState() {
        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    private void launchGoogleMapsNavigation() {
        if (deptLat == 0 || userLoc == null) {
            Toast.makeText(this, "Awaiting current location mapping coordinates...", Toast.LENGTH_SHORT).show();
            return;
        }

        String mapUriString = String.format(java.util.Locale.US,
                "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                deptLat, deptLng, userLoc.latitude, userLoc.longitude);

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUriString));
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mapUriString)));
        }
    }

    private void startUserETACountdown(long durationMs) {
        if (etaTimer != null) {
            etaTimer.cancel();
        }

        long endTimeMs = System.currentTimeMillis() + durationMs;
        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        prefs.edit().putLong("timer_end_time_ms", endTimeMs).apply();

        etaTimer = new android.os.CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                if (tvCountdownETA != null) {
                    tvCountdownETA.setText(String.format(java.util.Locale.getDefault(),
                            "Ringing in: %02d:%02d", minutes, seconds));
                }
            }

            @Override
            public void onFinish() {
                if (tvCountdownETA != null) tvCountdownETA.setText("Time's Up! Responders Due.");
                playDeviceAlarmSound();
            }
        }.start();
    }

    private void playDeviceAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
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
        if (alertKey == null || deptId == null) {
            Toast.makeText(this, "Error: Missing Alert details. Cannot confirm.", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        root.child("ActiveAlerts").child(deptId).child(alertKey).child("status").setValue("Confirmed");
        root.child("ProcessingAlerts").child(deptId).child(alertKey).child("status").setValue("Confirmed");

        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) uid = prefs.getString("owner_uid", null);
        if (uid != null) {
            root.child("UserHistory").child(uid).child(alertKey).child("status").setValue("Confirmed");
        }

        Toast.makeText(TrackingActivity.this, "Arrival confirmed!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(TrackingActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("NAVIGATE_TO", "HISTORY");
        startActivity(intent);
        finish();
    }

    private void copyContentUriAndUpload(Uri contentUri) {
        new Thread(() -> {
            try {
                java.io.File tempFile = new java.io.File(getCacheDir(), "videos/temp_upload_" + System.currentTimeMillis() + ".mp4");
                tempFile.getParentFile().mkdirs();

                InputStream in = getContentResolver().openInputStream(contentUri);
                if (in == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Could not read video file.", Toast.LENGTH_SHORT).show());
                    return;
                }

                OutputStream out = new FileOutputStream(tempFile);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.close();
                in.close();

                if (tempFile.length() == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "Recorded video is empty.", Toast.LENGTH_SHORT).show());
                    tempFile.delete();
                    return;
                }

                cameraVideoFile = tempFile;
                runOnUiThread(() -> uploadVideoToFirebase(Uri.fromFile(tempFile)));

            } catch (Exception e) {
                Log.e("VideoCopy", "Failed to copy content URI to file: ", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to process video: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void uploadVideoToFirebase(Uri videoUri) {
        if (videoUri == null || alertKey == null || deptId == null) {
            Toast.makeText(this, "Upload error: Invalid alert session details.", Toast.LENGTH_SHORT).show();
            return;
        }

        java.io.File fileToUpload = null;
        if ("file".equals(videoUri.getScheme())) {
            fileToUpload = new java.io.File(videoUri.getPath());
        }
        else if (cameraVideoFile != null && cameraVideoFile.exists() && cameraVideoFile.length() > 0) {
            fileToUpload = cameraVideoFile;
        }

        if (fileToUpload == null || !fileToUpload.exists() || fileToUpload.length() == 0) {
            Toast.makeText(this, "Upload error: Video file not found or empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading video evidence (" + (fileToUpload.length() / 1024) + " KB)...", Toast.LENGTH_SHORT).show();

        FirebaseStorage storage = FirebaseStorage.getInstance("gs://jerapp-2026.firebasestorage.app");
        StorageReference ref = storage.getReference().child("emergency_evidence/" + alertKey + "/" + System.currentTimeMillis() + ".mp4");

        final java.io.File finalFile = fileToUpload;
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(finalFile);

            ref.putStream(fis)
                    .addOnSuccessListener(taskSnapshot -> {
                        try { fis.close(); } catch (Exception ignored) {}
                        if (finalFile.getAbsolutePath().contains("cache")) {
                            finalFile.delete();
                        }
                        cameraVideoFile = null;

                        ref.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                            String videoUrlString = downloadUrl.toString();

                            // FIX: Update video URLs in ALL relevant paths (Active, Processing, History)
                            updateVideoUrlsInDatabase(videoUrlString);
                        });
                    })
                    .addOnFailureListener(e -> {
                        try { fis.close(); } catch (Exception ignored) {}
                        if (finalFile.getAbsolutePath().contains("cache")) {
                            finalFile.delete();
                        }
                        cameraVideoFile = null;
                        Log.e("FirebaseUpload", "Upload FAILED", e);
                        Toast.makeText(TrackingActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (java.io.FileNotFoundException e) {
            Log.e("FirebaseUpload", "File not found for upload", e);
            Toast.makeText(this, "Video file not found for upload.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateVideoUrlsInDatabase(String videoUrlString) {
        String[] paths = {"ActiveAlerts", "ProcessingAlerts"};
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        
        for (String path : paths) {
            DatabaseReference targetRef = root.child(path).child(deptId).child(alertKey);
            runVideoUrlTransaction(targetRef, videoUrlString);
        }

        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) uid = prefs.getString("owner_uid", null);
        if (uid != null) {
            DatabaseReference historyRef = root.child("UserHistory").child(uid).child(alertKey);
            runVideoUrlTransaction(historyRef, videoUrlString);
        }
    }

    private void runVideoUrlTransaction(DatabaseReference ref, String videoUrl) {
        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() == null) return Transaction.success(currentData);
                List<String> urlsList = new ArrayList<>();
                MutableData urlsSnapshot = currentData.child("video_urls");
                if (urlsSnapshot.getValue() != null) {
                    for (MutableData child : urlsSnapshot.getChildren()) {
                        String url = child.getValue(String.class);
                        if (url != null) urlsList.add(url);
                    }
                }
                urlsList.add(videoUrl);
                currentData.child("video_urls").setValue(urlsList);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (committed) Log.d("FirebaseUpload", "Video URL updated in: " + ref.getPath().toString());
            }
        });
    }

    private void launchCamera() {
        Intent takeVideoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        try {
            java.io.File videoDir = new java.io.File(getCacheDir(), "videos");
            videoDir.mkdirs();
            cameraVideoFile = new java.io.File(videoDir, "evidence_" + System.currentTimeMillis() + ".mp4");
            pendingVideoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", cameraVideoFile);
            takeVideoIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, pendingVideoUri);
            takeVideoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            Log.e("CameraLaunch", "Failed to create FileProvider URI", e);
            cameraVideoFile = null;
            pendingVideoUri = null;
        }

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
        if (mMap == null || userLoc == null || deptLat == 0) return;
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
        com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
        builder.include(userLoc);
        builder.include(deptLoc);
        com.google.android.gms.maps.model.LatLngBounds bounds = builder.build();
        int padding = 100;
        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (IllegalStateException e) {
            mMap.setOnMapLoadedCallback(() -> mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding)));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMap != null) mMap.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            updateMapWithRoute();
            fitMapBounds();
        }
    }

    @Override
    protected void onDestroy() {
        if (trackingDatabaseRef != null && trackingListener != null) {
            trackingDatabaseRef.removeEventListener(trackingListener);
        }
        if (etaTimer != null) etaTimer.cancel();
        super.onDestroy();
    }
}
