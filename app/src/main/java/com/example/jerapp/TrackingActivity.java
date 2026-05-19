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

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String deptName, deptPhone;
    private double deptLat, deptLng;
    private ActivityResultLauncher<Intent> videoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // --- 1. SET UP THE ACTION BAR (Back Button) ---
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Emergency Tracking");
            }
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
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Evidence saved successfully!", Toast.LENGTH_SHORT).show();
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
        String alertKey = activeAlertsRef.push().getKey();

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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng location = new LatLng(deptLat, deptLng);
        mMap.addMarker(new MarkerOptions().position(location).title(deptName));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
    }

    // Handles the physical back arrow click at the top
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}