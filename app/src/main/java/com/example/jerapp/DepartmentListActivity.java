package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import for the Red/White theme toolbar
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class DepartmentListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView statusText;
    private List<DepartmentModel> nearestList;
    private String selectedType;
    private DeptAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department_list);

        // 1. Setup Red/White Toolbar & Back Arrow
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nearby Responders");
        }

        // 2. Initialize UI Components
        recyclerView = findViewById(R.id.deptRecyclerView);
        statusText = findViewById(R.id.statusText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3. Receive Data from Intent
        selectedType = getIntent().getStringExtra("emergency_type");
        double lat = getIntent().getDoubleExtra("user_lat", 1.4588);
        double lng = getIntent().getDoubleExtra("user_lng", 103.7461);

        // 4. Trigger Search Logic
        findNearestDepartments(selectedType, lat, lng);

        com.google.android.material.appbar.MaterialToolbar topToolbar = findViewById(R.id.topToolbar);
        setSupportActionBar(topToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nearby Responders");
        }
    }

    private void findNearestDepartments(String type, double userLat, double userLng) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Departments");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                nearestList = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    DepartmentModel dept = ds.getValue(DepartmentModel.class);

                    // Check type and active status
                    if (dept != null && dept.type.equals(type) && dept.status.equals("active")) {
                        // Calculate distance using your Utils class
                        double distance = LocationUtils.calculateDistance(userLat, userLng, dept.lat, dept.lng);

                        // Within 5km radius
                        if (distance <= 5.0) {
                            nearestList.add(dept);
                        }
                    }
                }
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                statusText.setText("Database Error: " + error.getMessage());
            }
        });
    }

    private void updateUI() {
        if (nearestList == null || nearestList.isEmpty()) {
            statusText.setText("No active " + selectedType + " responders within 5km.");
        } else {
            statusText.setText("Found " + nearestList.size() + " responders nearby:");
            adapter = new DeptAdapter(nearestList, this::moveToEmergencyPage);
            recyclerView.setAdapter(adapter);
        }
    }

    private void moveToEmergencyPage(DepartmentModel dept) {
        Intent intent = new Intent(DepartmentListActivity.this, TrackingActivity.class);
        intent.putExtra("dept_name", dept.name);
        intent.putExtra("dept_phone", dept.contact);
        intent.putExtra("dept_lat", dept.lat);
        intent.putExtra("dept_lng", dept.lng);
        startActivity(intent);
    }

    // Handles the back arrow click
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}