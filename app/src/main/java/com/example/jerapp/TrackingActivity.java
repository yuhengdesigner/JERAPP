package com.example.jerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.maps.android.PolyUtil;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String deptName, deptPhone, deptId, alertKey;
    private double deptLat, deptLng;
    private ActivityResultLauncher<Intent> videoLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLoc;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Generate ID immediately so videos can be uploaded during transit
        alertKey = getIntent().getStringExtra("alert_key");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermissionAndFetch();

        // Data from intent
        deptName = getIntent().getStringExtra("dept_name");
        deptPhone = getIntent().getStringExtra("dept_phone");
        deptId = getIntent().getStringExtra("dept_id");
        deptLat = getIntent().getDoubleExtra("dept_lat", 0);
        deptLng = getIntent().getDoubleExtra("dept_lng", 0);

        TextView tvDetail = findViewById(R.id.deptDetail);
        if (tvDetail != null) tvDetail.setText("Responding: " + deptName);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        videoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        uploadVideoToFirebase(result.getData().getData());
                    }
                });

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            // Navigate back to the list (Update "DepartmentListActivity.class" to your actual list activity name)
            Intent intent = new Intent(this, DepartmentListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
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

        startActivity(new Intent(TrackingActivity.this, UserHistoryActivity.class));
        finish();
    }

    private void uploadVideoToFirebase(Uri videoUri) {
        if (alertKey == null) return;

        StorageReference ref = FirebaseStorage.getInstance().getReference("Evidence/" + alertKey + "/" + System.currentTimeMillis() + ".mp4");
        ref.putFile(videoUri).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            DatabaseReference alertRef = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(deptId).child(alertKey).child("videoUrls");

            // Inside TrackingActivity.java
            alertRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    // 1. Get the current value as a list
                    GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {};
                    List<String> list = mutableData.getValue(t);

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
                        Toast.makeText(TrackingActivity.this, "Video added!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }));
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
            // Permission already granted, proceed
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
}