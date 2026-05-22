package com.example.jerapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class DepartmentListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView statusText;
    private String selectedType;
    private DeptAdapter adapter;
    private List<DepartmentModel> fullListPool = new ArrayList<>();
    private List<DepartmentModel> nearestList = new ArrayList<>();

    private String currentScope = "All";
    private String currentCategory = "All";

    // Track current coordinates globally
    private double userLat;
    private double userLng;

    // Fused Location Provider to retrieve live device coordinates
    private FusedLocationProviderClient fusedLocationClient;

    private Chip chipAllScope, chipNearestScope;
    private Chip chipAll, chipPublic, chipPrivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_department_list);

        // Initialize Fused Location Provider Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        recyclerView = findViewById(R.id.deptRecyclerView);
        statusText = findViewById(R.id.statusText);
        View btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeptAdapter(nearestList, new DeptAdapter.OnDeptClickListener() {
            @Override
            public void onSelect(DepartmentModel dept) {
                moveToEmergencyPage(dept);
            }

            @Override
            public void onNavigate(DepartmentModel dept) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + dept.latitude + "," + dept.longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });
        recyclerView.setAdapter(adapter);

        selectedType = getIntent().getStringExtra("emergency_type");

        // Use UTM Skudai as the backup baseline coordinate if GPS fails or is turned off
        userLat = getIntent().getDoubleExtra("user_lat", 1.5612);
        userLng = getIntent().getDoubleExtra("user_lng", 103.6378);

        // Chips configuration
        chipAllScope = findViewById(R.id.chipAllScope);
        chipNearestScope = findViewById(R.id.chipNearestScope);

        if (chipAllScope != null && chipNearestScope != null) {
            chipAllScope.setOnClickListener(v -> {
                chipAllScope.setChecked(true);
                chipNearestScope.setChecked(false);
                currentScope = "All";
                applyFilters();
            });

            chipNearestScope.setOnClickListener(v -> {
                chipNearestScope.setChecked(true);
                chipAllScope.setChecked(false);
                currentScope = "Nearest";
                applyFilters();
            });
        }

        chipAll = findViewById(R.id.chipAll);
        chipPublic = findViewById(R.id.chipPublic);
        chipPrivate = findViewById(R.id.chipPrivate);

        if (chipAll != null && chipPublic != null && chipPrivate != null) {
            chipAll.setOnClickListener(v -> {
                chipAll.setChecked(true);
                chipPublic.setChecked(false);
                chipPrivate.setChecked(false);
                currentCategory = "All";
                applyFilters();
            });

            chipPublic.setOnClickListener(v -> {
                chipAll.setChecked(false);
                chipPublic.setChecked(true);
                chipPrivate.setChecked(false);
                currentCategory = "Public";
                applyFilters();
            });

            chipPrivate.setOnClickListener(v -> {
                chipAll.setChecked(false);
                chipPublic.setChecked(false);
                chipPrivate.setChecked(true);
                currentCategory = "Private";
                applyFilters();
            });
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getUserLiveLocation();
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted: Get live location
                getUserLiveLocation();
            } else {
                // Permission denied: Use default fallback location and warn user
                Toast.makeText(this, "Location permission denied. Using default location for accuracy.",
                        Toast.LENGTH_LONG).show();
                // Proceed with existing default coordinates (already initialized in onCreate)
                findNearestDepartments(selectedType, userLat, userLng);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getUserLiveLocation() {
        // 1. Double-check permission before calling API
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            findNearestDepartments(selectedType, userLat, userLng);
            return;
        }

        // 2. Fetch the location
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLng = location.getLongitude();
            } else {
                Toast.makeText(this, "Unable to get live location. Using default.", Toast.LENGTH_SHORT).show();
            }
            findNearestDepartments(selectedType, userLat, userLng);
        }).addOnFailureListener(e -> {
            // Fallback on failure
            findNearestDepartments(selectedType, userLat, userLng);
        });
    }

    private void findNearestDepartments(String type, double lat, double lng) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("emergency_departments");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullListPool.clear();
                String targetType = (type != null) ? type.trim().toLowerCase() : "";

                for (DataSnapshot ds : snapshot.getChildren()) {
                    DepartmentModel dept = ds.getValue(DepartmentModel.class);

                    if (dept != null) {
                        // MANUALLY SET THE ID HERE
                        dept.id = ds.getKey();

                        String dbType = (dept.type != null) ? dept.type.trim().toLowerCase() : "";
                        String dbCategory = (dept.category != null) ? dept.category.trim().toLowerCase() : "";
                        String dbName = (dept.place_name != null) ? dept.place_name.trim().toLowerCase() : "";

                        boolean isMatch = false;

                        // 1. Fire & Explosion Mapping (CRITICAL FIX: Explicitly exclude gas leaks)
                        if (targetType.contains("fire") || targetType.contains("bomba")) {
                            // Make sure it matches fire or bomba, but IS NOT a dedicated gas leak type
                            isMatch = (dbType.contains("fire") || dbType.contains("bomba") || dbName.contains("bomba"))
                                    && !dbType.contains("gas") && !dbType.contains("leak");
                        }
                        else if (targetType.contains("medical") || targetType.contains("hospital") || targetType.contains("ambulance") || targetType.contains("clinic")) {
                            isMatch = dbType.contains("medical") || dbType.contains("hospital") || dbCategory.contains("medical") || dbName.contains("hospital") || dbName.contains("klinik");
                        }
                        else if (targetType.contains("gas") || targetType.contains("leak")) {
                            isMatch = dbType.contains("gas") || dbType.contains("gasleak") || dbName.contains("gas");
                        }
                        else if (targetType.contains("disaster") || targetType.contains("natural") || targetType.contains("flood")) {
                            isMatch = dbType.contains("civildefense") || dbType.contains("disaster") || dbType.contains("flood") || dbName.contains("banjir") || dbName.contains("pertahanan awam") || dbName.contains("jpam");
                        }
                        else if (targetType.contains("police") || targetType.contains("police & crime")) {
                            isMatch = dbType.contains("police") || dbType.contains("polis") || dbName.contains("polis") || dbName.contains("police");
                        }
                        else if (targetType.contains("wild") || targetType.contains("animal")) {
                            isMatch = dbType.contains("animals") || dbType.contains("wild") || dbName.contains("wildlife") || dbName.contains("perhilitan");
                        }
                        else {
                            isMatch = dbType.equalsIgnoreCase(targetType) || dbCategory.equalsIgnoreCase(targetType);
                        }

                        if (isMatch) {
                            // Compute exact distance based on live coordinates
                            dept.distance = calculateDistance(lat, lng, dept.latitude, dept.longitude);
                            fullListPool.add(dept);
                        }
                    }
                }

                Collections.sort(fullListPool, (d1, d2) -> Double.compare(d1.distance, d2.distance));
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilters() {
        nearestList.clear();

        for (DepartmentModel dept : fullListPool) {
            boolean matchesCategory = currentCategory.equals("All") ||
                    dept.category.equalsIgnoreCase(currentCategory);

            boolean matchesScope = true;
            if (currentScope.equals("Nearest")) {
                matchesScope = (dept.distance <= 5.0);
            }

            if (matchesCategory && matchesScope) {
                nearestList.add(dept);
            }
        }

        String categoryLabel = currentCategory.equals("All") ? "" : currentCategory + " ";
        if (nearestList.isEmpty()) {
            statusText.setText("No " + categoryLabel + "responders found.");
        } else {
            if (currentScope.equals("Nearest")) {
                statusText.setText("Showing " + categoryLabel + "Responders (within 5km):");
            } else {
                statusText.setText("Showing All " + categoryLabel + "Responders:");
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void moveToEmergencyPage(DepartmentModel dept) {
        Intent intent = new Intent(this, ConfirmationActivity.class);

        // CRITICAL: You must extract the ID from the 'dept' object and put it in the intent
        // Ensure "dept.id" matches the key in your JSON file (e.g., "id_1")
        intent.putExtra("dept_id", dept.id);

        intent.putExtra("dept_name", dept.place_name);
        intent.putExtra("dept_phone", dept.contact);
        intent.putExtra("dept_lat", dept.latitude);
        intent.putExtra("dept_lng", dept.longitude);
        intent.putExtra("dept_address", dept.full_address);
        intent.putExtra("emergency_type", selectedType);

        intent.putExtra("user_lat", userLat);
        intent.putExtra("user_lng", userLng);

        startActivity(intent);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000;
    }
}