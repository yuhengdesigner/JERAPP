package com.example.jerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity; // Use this for getSupportActionBar
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import android.widget.SeekBar;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

import com.google.maps.android.PolyUtil;
import java.util.List;
import okhttp3.*; // Ensure you add OkHttp dependency to build.gradle
import org.json.JSONObject;
import java.io.IOException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String deptName, deptPhone;
    private double deptLat, deptLng;
    private ActivityResultLauncher<Intent> videoLauncher;
    private String alertKey;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermissionAndFetch();

        android.widget.ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                onBackPressed(); // This triggers your existing logic
            });
        }

        // --- 2. GET DATA FROM INTENT ---
        deptName = getIntent().getStringExtra("dept_name");
        deptPhone = getIntent().getStringExtra("dept_phone");
        deptLat = getIntent().getDoubleExtra("dept_lat", 0);
        deptLng = getIntent().getDoubleExtra("dept_lng", 0);

        TextView tvDetail = findViewById(R.id.deptDetail);
        if (tvDetail != null) {
            tvDetail.setText("Responding: " + deptName);
        }

        // --- 3. INITIALIZE GOOGLE MAPS ---
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // --- 4. VIDEO RECORDING SETUP ---
        videoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        uploadVideoToFirebase(videoUri);
                    }
                }
        );

        // --- 5. BUTTON LISTENERS ---

        // Call Button
        findViewById(R.id.btnCall).setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + deptPhone));
            startActivity(callIntent);
        });

        // Record Video Button
        findViewById(R.id.btnRecord).setOnClickListener(v -> {
            Intent takeVideoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                videoLauncher.launch(takeVideoIntent);
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        });

        // --- 6. AUTO-REPORT TO ADMIN ---
        sendAlertToAdmin();

        // --- 7. SLIDE-TO-CONFIRM LOGIC ---
        SeekBar btnSwipe = findViewById(R.id.btnSwipe);
        btnSwipe.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Since we have padding/offset, 100% might not be reached perfectly.
                // 90-95 is a safe threshold for "end of track".
                if (progress >= 99) {
                    confirmArrivalWithFirebase();
                    seekBar.setEnabled(false);
                    seekBar.setAlpha(0.5f);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 99) {
                    seekBar.setProgress(0);
                }
            }
        });
    }

    private void drawComplexRoute(LatLng origin, LatLng destination) {
        // Construct the URL for Directions API
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + origin.latitude + "," + origin.longitude
                + "&destination=" + destination.latitude + "," + destination.longitude
                + "&key=AIzaSyAbO5sV2U6P2q5e4jxVPAoTdkg5R4lhQU8";

        // Use a background thread (OkHttp or AsyncTask) to fetch the JSON
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonData = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);

                    // Log the response to Logcat to see if we get 'REQUEST_DENIED' or 'ZERO_RESULTS'
                    android.util.Log.d("DIRECTIONS_API", jsonData);

                    if (jsonObject.getString("status").equals("OK")) {
                        String encodedPath = jsonObject.getJSONArray("routes")
                                .getJSONObject(0).getJSONObject("overview_polyline").getString("points");

                        List<LatLng> decodedPath = PolyUtil.decode(encodedPath);
                        runOnUiThread(() -> {
                            mMap.addPolyline(new com.google.android.gms.maps.model.PolylineOptions()
                                    .addAll(decodedPath)
                                    .width(12)
                                    .color(android.graphics.Color.BLUE));
                        });
                    } else {
                        android.util.Log.e("DIRECTIONS_API", "Status: " + jsonObject.getString("status"));
                    }
                } catch (Exception e) {
                    android.util.Log.e("DIRECTIONS_API", "Error parsing JSON", e);
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("DIRECTIONS_API", "Network failure", e);
            }
        });
    }

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    // Refresh map with new user location
                    if (mMap != null) updateMapWithRoute();
                }
            });
        }
    }

    // Inside TrackingActivity.java

    private void sendAlertToAdmin() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;

        // Get the REAL user coordinates passed from the previous activity
        double userLat = getIntent().getDoubleExtra("user_lat", 1.4588);
        double userLng = getIntent().getDoubleExtra("user_lng", 103.7461);

        String uid = mAuth.getUid();
        DatabaseReference activeAlertsRef = FirebaseDatabase.getInstance().getReference("ActiveAlerts");
        this.alertKey = activeAlertsRef.push().getKey();

        HashMap<String, Object> alertData = new HashMap<>();
        alertData.put("userId", uid);
        alertData.put("userName", "Choy Yu Heng");
        alertData.put("userEmail", mAuth.getCurrentUser().getEmail());
        alertData.put("userPhone", "+60123456789");
        alertData.put("emergencyType", getIntent().getStringExtra("emergency_type"));
        alertData.put("assignedDept", deptName);
        alertData.put("userLat", userLat); // Updated from hardcoded
        alertData.put("userLng", userLng); // Updated from hardcoded
        alertData.put("textAddress", "UTM Faculty of Computing, Skudai");
        alertData.put("status", "Pending");
        alertData.put("timestamp", System.currentTimeMillis());

        // 1. Alert the Admin
        activeAlertsRef.child(alertKey).setValue(alertData);

        // 2. Save to User's History
        FirebaseDatabase.getInstance().getReference("UserHistory")
                .child(uid).child(alertKey).setValue(alertData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Emergency reported to " + deptName, Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmArrivalWithFirebase() {
        if (this.alertKey != null) {
            FirebaseDatabase.getInstance().getReference("ActiveAlerts")
                    .child(this.alertKey).child("status").setValue("Arrived")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Arrival Confirmed!", Toast.LENGTH_SHORT).show();

                        // Redirect to History Page
                        Intent intent = new Intent(TrackingActivity.this, UserHistoryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true); // Shows the blue dot
        }
        updateMapWithRoute();
    }

    private void updateMapWithRoute() {
        if (userLoc == null || deptLat == 0) return;

        LatLng deptLoc = new LatLng(deptLat, deptLng);
        mMap.clear();

        // 1. Add markers
        mMap.addMarker(new MarkerOptions().position(userLoc).title("Your Location"));
        mMap.addMarker(new MarkerOptions().position(deptLoc).title(deptName));

        // 2. Fetch and Draw the REAL route
        drawComplexRoute(userLoc, deptLoc);

        // 3. Adjust camera
        com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
        builder.include(userLoc);
        builder.include(deptLoc);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
    }

    private void uploadVideoToFirebase(Uri videoUri) {
        if (alertKey == null) {
            Toast.makeText(this, "Alert key not ready yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading evidence...", Toast.LENGTH_SHORT).show();

        // Ensure you have imported: com.google.firebase.storage.FirebaseStorage;
        // Ensure you have imported: com.google.firebase.storage.StorageReference;
        StorageReference ref = FirebaseStorage.getInstance().getReference("Evidence/" + alertKey);

        ref.putFile(videoUri).addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                // Save the URL to the existing alert in Firebase
                FirebaseDatabase.getInstance().getReference("ActiveAlerts")
                        .child(alertKey).child("videoUrl").setValue(uri.toString());

                Toast.makeText(this, "Evidence uploaded successfully!", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
