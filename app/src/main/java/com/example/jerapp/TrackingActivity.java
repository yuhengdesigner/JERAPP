package com.example.jerapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private DatabaseReference trackingDatabaseRef, statusDatabaseRef;
    private ValueEventListener trackingListener, statusListener;

    private View collapsibleContent;
    private ImageButton btnToggleExpand;
    private File cameraVideoFile;
    private Uri pendingVideoUri;
    private boolean isCardExpanded = true;
    private boolean isAdminResolved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // 1. Retrieve data
        alertKey = getIntent().getStringExtra("alert_key");
        deptName = getIntent().getStringExtra("dept_name");
        deptPhone = getIntent().getStringExtra("dept_phone");
        deptId = getIntent().getStringExtra("dept_id");
        deptLat = getIntent().getDoubleExtra("dept_lat", 0);
        deptLng = getIntent().getDoubleExtra("dept_lng", 0);

        saveActiveTrackingState();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermissionAndFetch();

        // 2. Bind Views
        TextView tvDetail = findViewById(R.id.deptDetail);
        if (tvDetail != null) tvDetail.setText("Responding: " + deptName);

        btnViewGoogleMapsETA = findViewById(R.id.btnViewGoogleMapsETA);
        collapsibleContent = findViewById(R.id.collapsibleContent);
        btnToggleExpand = findViewById(R.id.btnToggleExpand);

        // 3. Header Toggle
        if (btnToggleExpand != null) {
            btnToggleExpand.setOnClickListener(v -> {
                isCardExpanded = !isCardExpanded;
                if (collapsibleContent != null) {
                    collapsibleContent.setVisibility(isCardExpanded ? View.VISIBLE : View.GONE);
                }
                btnToggleExpand.setImageResource(isCardExpanded ? android.R.drawable.arrow_down_float : android.R.drawable.arrow_up_float);
            });
        }

        // 4. Map Setup
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // 5. Video Launcher
        videoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (cameraVideoFile != null && cameraVideoFile.exists() && cameraVideoFile.length() > 0) {
                            uploadVideoToFirebase(Uri.fromFile(cameraVideoFile));
                        } else if (result.getData() != null && result.getData().getData() != null) {
                            copyContentUriAndUpload(result.getData().getData());
                        } else if (pendingVideoUri != null) {
                            copyContentUriAndUpload(pendingVideoUri);
                        } else {
                            Toast.makeText(this, "Could not retrieve the recorded video.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 6. Action Listeners
        if (btnViewGoogleMapsETA != null) {
            btnViewGoogleMapsETA.setOnClickListener(v -> launchGoogleMapsNavigation());
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> navigateToDashboardHome());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToDashboardHome();
            }
        });

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

        if (btnStartUserTimer != null) {
            btnStartUserTimer.setOnClickListener(v -> {
                if (etTimerInput != null) {
                    String input = etTimerInput.getText().toString().trim();
                    if (!input.isEmpty()) {
                        try {
                            long minutes = Long.parseLong(input);
                            startUserETACountdown(minutes * 60 * 1000);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid duration", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        // 7. Swipe Logic with Admin Resolution Check
        SeekBar btnSwipe = findViewById(R.id.btnSwipe);
        if (btnSwipe != null) {
            btnSwipe.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress >= 99) {
                        if (!isAdminResolved) {
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
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.getProgress() < 99) seekBar.setProgress(0);
                }
            });
        }

        setupLiveETATracking();
        listenForAdminResolution();
    }

    private void listenForAdminResolution() {
        if (deptId == null || alertKey == null) return;
        statusDatabaseRef = FirebaseDatabase.getInstance().getReference("ProcessingAlerts").child(deptId).child(alertKey);
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    if ("Resolved".equalsIgnoreCase(status)) {
                        isAdminResolved = true;
                    }
                } else {
                    checkIfInResolvedNode();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        statusDatabaseRef.addValueEventListener(statusListener);
    }

    private void checkIfInResolvedNode() {
        FirebaseDatabase.getInstance().getReference("ResolvedAlerts").child(deptId).child(alertKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            isAdminResolved = true;
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void saveActiveTrackingState() {
        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("has_active_emergency", true);
        editor.putString("alert_key", alertKey);
        editor.putString("dept_id", deptId);
        editor.putString("dept_name", deptName);
        editor.putString("dept_phone", deptPhone);
        editor.putLong("dept_lat_bits", Double.doubleToRawLongBits(deptLat));
        editor.putLong("dept_lng_bits", Double.doubleToRawLongBits(deptLng));
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) editor.putString("owner_uid", uid);
        editor.apply();
    }

    private void clearTrackingState() {
        getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE).edit().clear().apply();
    }

    private void navigateToDashboardHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void launchGoogleMapsNavigation() {
        if (deptLat == 0 || userLoc == null) {
            Toast.makeText(this, "Awaiting current location coordinates...", Toast.LENGTH_SHORT).show();
            return;
        }
        String mapUriString = String.format(Locale.US,
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
        if (etaTimer != null) etaTimer.cancel();
        etaTimer = new android.os.CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvCountdownETA != null) {
                    tvCountdownETA.setText(String.format(Locale.getDefault(),
                            "Ringing in: %02d:%02d", (millisUntilFinished / 1000) / 60, (millisUntilFinished / 1000) % 60));
                }
            }

            @Override
            public void onFinish() {
                if (tvCountdownETA != null) tvCountdownETA.setText("Time's Up!");
                playDeviceAlarmSound();
            }
        }.start();
    }

    private void playDeviceAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            alarmRingtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
            if (alarmRingtone != null) alarmRingtone.play();
        } catch (Exception e) {
            Log.e("ALARM", e.getMessage());
        }
    }

    private void stopActiveRingtone() {
        if (alarmRingtone != null && alarmRingtone.isPlaying()) alarmRingtone.stop();
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
                        deptLat = dLat;
                        deptLng = dLng;
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

    private void copyContentUriAndUpload(Uri contentUri) {
        new Thread(() -> {
            try {
                File tempFile = new File(getCacheDir(), "videos/temp_" + System.currentTimeMillis() + ".mp4");
                tempFile.getParentFile().mkdirs();
                InputStream in = getContentResolver().openInputStream(contentUri);
                if (in == null) return;
                OutputStream out = new FileOutputStream(tempFile);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                out.close(); in.close();
                runOnUiThread(() -> uploadVideoToFirebase(Uri.fromFile(tempFile)));
            } catch (Exception e) {
                Log.e("VIDEO_COPY", e.getMessage());
            }
        }).start();
    }

    private void uploadVideoToFirebase(Uri videoUri) {
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("emergency_evidence/" + alertKey + "/" + System.currentTimeMillis() + ".mp4");
        ref.putFile(videoUri).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(url -> updateVideoUrlsInDatabase(url.toString())));
    }

    private void updateVideoUrlsInDatabase(String videoUrl) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        String[] paths = {"ActiveAlerts", "ProcessingAlerts", "ResolvedAlerts"};
        for (String path : paths) {
            DatabaseReference targetRef = root.child(path).child(deptId).child(alertKey);
            runVideoUrlTransaction(targetRef, videoUrl);
        }
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            runVideoUrlTransaction(root.child("UserHistory").child(uid).child(alertKey), videoUrl);
        }
    }

    private void runVideoUrlTransaction(DatabaseReference ref, String videoUrl) {
        ref.runTransaction(new Transaction.Handler() {
            @NonNull @Override
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
            @Override public void onComplete(@Nullable DatabaseError e, boolean c, @Nullable DataSnapshot s) {}
        });
    }

    private void launchCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        try {
            File dir = new File(getCacheDir(), "videos");
            dir.mkdirs();
            cameraVideoFile = new File(dir, "evidence_" + System.currentTimeMillis() + ".mp4");
            pendingVideoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", cameraVideoFile);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, pendingVideoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            videoLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("CAMERA", e.getMessage());
        }
    }

    @Override public void onMapReady(@NonNull GoogleMap googleMap) {
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
        LatLngBounds bounds = new LatLngBounds.Builder().include(userLoc).include(new LatLng(deptLat, deptLng)).build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
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
            launchCamera();
        }
    }

    @Override protected void onDestroy() {
        if (trackingDatabaseRef != null && trackingListener != null) trackingDatabaseRef.removeEventListener(trackingListener);
        if (statusDatabaseRef != null && statusListener != null) statusDatabaseRef.removeEventListener(statusListener);
        if (etaTimer != null) etaTimer.cancel();
        super.onDestroy();
    }
}
