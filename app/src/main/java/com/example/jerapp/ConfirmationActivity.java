package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        Intent incomingIntent = getIntent();

        // 1. Handle Buttons
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        // 2. Confirm Button
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            String deptId = incomingIntent.getStringExtra("dept_id");
            String deptName = incomingIntent.getStringExtra("dept_name");

            if (deptId == null || deptId.isEmpty()) {
                Toast.makeText(this, "Error: Department ID missing!", Toast.LENGTH_LONG).show();
                return;
            }

            // Fetch Real User Data from Firebase
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // Extract actual user data
                            String name = snapshot.child("name").getValue(String.class);
                            String phone = snapshot.child("phone").getValue(String.class);
                            String gender = snapshot.child("gender").getValue(String.class);
                            String email = user.getEmail();

                            // Build the Alert Model
                            AlertModel newAlert = new AlertModel();
                            newAlert.setUserName(name != null ? name : "Unknown User");
                            newAlert.setUserPhone(phone != null ? phone : "N/A");
                            newAlert.setUserEmail(email != null ? email : "N/A");
                            newAlert.setGender(gender != null ? gender : "N/A");

                            newAlert.setEmergencyType(incomingIntent.getStringExtra("emergency_type"));
                            newAlert.setAssignedDept(deptId);
                            newAlert.setStatus("Pending");
                            newAlert.setTextAddress(incomingIntent.getStringExtra("dept_address"));
                            newAlert.setUserLat(incomingIntent.getDoubleExtra("user_lat", 0.0));
                            newAlert.setUserLng(incomingIntent.getDoubleExtra("user_lng", 0.0));
                            newAlert.setVideoUrl(incomingIntent.getStringExtra("video_url"));
                            newAlert.setTimestamp(System.currentTimeMillis());

                            // Push to Firebase
                            FirebaseDatabase.getInstance().getReference("ActiveAlerts")
                                    .child(deptId)
                                    .push()
                                    .setValue(newAlert)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ConfirmationActivity.this, "Alert sent to " + deptName, Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(ConfirmationActivity.this, DispatchActivity.class);
                                        intent.putExtras(incomingIntent);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(ConfirmationActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(ConfirmationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}