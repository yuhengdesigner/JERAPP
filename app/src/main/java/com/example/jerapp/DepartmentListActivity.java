package com.example.jerapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
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

        // --- BLEND FIX: Enable edge-to-edge logic ---
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        window.setStatusBarColor(Color.TRANSPARENT);
        // --------------------------------------------

        setContentView(R.layout.activity_department_list);

        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nearby Responders");
        }

        recyclerView = findViewById(R.id.deptRecyclerView);
        statusText = findViewById(R.id.statusText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        selectedType = getIntent().getStringExtra("emergency_type");
        double lat = getIntent().getDoubleExtra("user_lat", 1.4588);
        double lng = getIntent().getDoubleExtra("user_lng", 103.7461);

        findNearestDepartments(selectedType, lat, lng);
    }

    private void findNearestDepartments(String type, double userLat, double userLng) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Departments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                nearestList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    DepartmentModel dept = ds.getValue(DepartmentModel.class);
                    if (dept != null && dept.type != null && dept.type.equalsIgnoreCase(type) && "active".equals(dept.status)) {
                        double distance = LocationUtils.calculateDistance(userLat, userLng, dept.lat, dept.lng);
                        if (distance <= 5.0) nearestList.add(dept);
                    }
                }
                updateUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                statusText.setText("Error: " + error.getMessage());
            }
        });
    }

    private void updateUI() {
        if (nearestList == null || nearestList.isEmpty()) {
            statusText.setText("No active " + selectedType + " within 5km.");
        } else {
            statusText.setText("Found " + nearestList.size() + " responders nearby:");
            adapter = new DeptAdapter(nearestList, this::moveToEmergencyPage);
            recyclerView.setAdapter(adapter);
        }
    }

    private void moveToEmergencyPage(DepartmentModel dept) {
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra("dept_name", dept.name);
        intent.putExtra("dept_phone", dept.contact);
        intent.putExtra("dept_lat", dept.lat);
        intent.putExtra("dept_lng", dept.lng);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}