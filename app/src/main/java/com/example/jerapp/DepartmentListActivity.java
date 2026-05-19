package com.example.jerapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DepartmentListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView statusText;
    private String selectedType;
    private DeptAdapter adapter;
    private List<DepartmentModel> fullListPool = new ArrayList<>();
    private List<DepartmentModel> nearestList = new ArrayList<>();

    private String currentScope = "All"; // "All" or "Nearest"
    private String currentCategory = "All"; // "All", "Public", or "Private"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Transparent Status Bar Setup
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_department_list);

        // UI References
        recyclerView = findViewById(R.id.deptRecyclerView);
        statusText = findViewById(R.id.statusText);
        View btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // RecyclerView Setup
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

        // Get Intent Data
        selectedType = getIntent().getStringExtra("emergency_type");
        double lat = getIntent().getDoubleExtra("user_lat", 1.4588);
        double lng = getIntent().getDoubleExtra("user_lng", 103.7461);

        // 1. Scope Filter Listener (All vs Nearest)
        ChipGroup scopeGroup = findViewById(R.id.scopeFilterGroup);
        scopeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipNearestScope) {
                currentScope = "Nearest";
            } else {
                currentScope = "All";
            }
            applyFilters();
        });

        // 2. Category Filter Listener (Public vs Private)
        ChipGroup categoryGroup = findViewById(R.id.categoryFilterGroup);
        categoryGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPublic) currentCategory = "Public";
            else if (checkedId == R.id.chipPrivate) currentCategory = "Private";
            else currentCategory = "All";
            applyFilters();
        });

        // Fetch Data from Firebase
        findNearestDepartments(selectedType, lat, lng);
    }

    private void findNearestDepartments(String type, double userLat, double userLng) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("emergency_departments");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullListPool.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    DepartmentModel dept = ds.getValue(DepartmentModel.class);
                    if (dept != null && dept.type.equalsIgnoreCase(type)) {
                        dept.distance = calculateDistance(userLat, userLng, dept.latitude, dept.longitude);
                        fullListPool.add(dept);
                    }
                }
                // Sort absolute nearest first
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
            // Category Logic
            boolean matchesCategory = currentCategory.equals("All") ||
                    dept.category.equalsIgnoreCase(currentCategory);

            // Scope/Distance Logic
            boolean matchesScope = true;
            if (currentScope.equals("Nearest")) {
                matchesScope = (dept.distance <= 5.0);
            }

            if (matchesCategory && matchesScope) {
                nearestList.add(dept);
            }
        }

        // Update UI Text
        String categoryLabel = currentCategory.equals("All") ? "" : currentCategory + " ";
        if (nearestList.isEmpty()) {
            statusText.setText("No " + categoryLabel + "responders found nearby.");
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
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra("dept_name", dept.place_name);
        intent.putExtra("dept_phone", dept.contact);
        intent.putExtra("dept_lat", dept.latitude);
        intent.putExtra("dept_lng", dept.longitude);
        intent.putExtra("dept_address", dept.full_address);
        intent.putExtra("emergency_type", selectedType);

        // Pass user coords for admin alert
        intent.putExtra("user_lat", getIntent().getDoubleExtra("user_lat", 1.4588));
        intent.putExtra("user_lng", getIntent().getDoubleExtra("user_lng", 103.7461));

        startActivity(intent);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000; // Meters to KM
    }
}