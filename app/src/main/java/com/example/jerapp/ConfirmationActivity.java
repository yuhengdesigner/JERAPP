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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

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
            // LOCK BUTTON TO PREVENT MULTIPLE CLICKS
            v.setEnabled(false);

            String deptId = incomingIntent.getStringExtra("dept_id");
            String deptName = incomingIntent.getStringExtra("dept_name");
            String deptPhone = incomingIntent.getStringExtra("dept_phone");


            double lat = incomingIntent.getDoubleExtra("user_lat", 0.0);
            double lng = incomingIntent.getDoubleExtra("user_lng", 0.0);

            double deptLat = incomingIntent.getDoubleExtra("dept_lat", 0.0);
            double deptLng = incomingIntent.getDoubleExtra("dept_lng", 0.0);
            String emergencyType = incomingIntent.getStringExtra("emergency_type");

            final String addressText = getAddressFromLocation(lat, lng);

            if (deptId == null || deptId.isEmpty()) {
                Toast.makeText(this, "Error: Department ID missing!", Toast.LENGTH_LONG).show();
                v.setEnabled(true); // Re-enable so they can try again
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
                v.setEnabled(true);
                return;
            }

            FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String name = snapshot.child("name").getValue(String.class);
                            String phone = snapshot.child("phone").getValue(String.class);
                            String gender = snapshot.child("gender").getValue(String.class);
                            String email = user.getEmail();

                            AlertModel newAlert = new AlertModel();
                            newAlert.setUserName(name != null ? name : "Unknown User");
                            newAlert.setUserPhone(phone != null ? phone : "N/A");
                            newAlert.setUserEmail(email != null ? email : "N/A");
                            newAlert.setGender(gender != null ? gender : "N/A");
                            newAlert.setTextAddress(addressText);
                            newAlert.setEmergencyType(incomingIntent.getStringExtra("emergency_type"));
                            newAlert.setAssignedDept(deptId);
                            newAlert.setDeptName(incomingIntent.getStringExtra("dept_name"));
                            newAlert.setDeptPhone(incomingIntent.getStringExtra("dept_phone")); // ADD THIS
                            newAlert.setDeptLat(incomingIntent.getDoubleExtra("dept_lat", 0.0)); // ADD THIS
                            newAlert.setDeptLng(incomingIntent.getDoubleExtra("dept_lng", 0.0)); // ADD THIS
                            newAlert.setStatus("Pending");
                            newAlert.setUserLat(lat);
                            newAlert.setUserLng(lng);
                            newAlert.setVideoUrl(incomingIntent.getStringExtra("video_url"));
                            newAlert.setTimestamp(System.currentTimeMillis());

                            // Correct way to get the reference and key
                            DatabaseReference alertRef = FirebaseDatabase.getInstance()
                                    .getReference("ActiveAlerts")
                                    .child(deptId)
                                    .push();

                            String generatedKey = alertRef.getKey(); // Get the ID

                            alertRef.setValue(newAlert).addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ConfirmationActivity.this, "Alert sent to " + deptName, Toast.LENGTH_SHORT).show();

                                        // Pass the generatedKey to TrackingActivity
                                        Intent intent = new Intent(ConfirmationActivity.this, DispatchActivity.class);
                                        intent.putExtra("alert_key", generatedKey);
                                        intent.putExtra("dept_id", deptId);
                                        intent.putExtra("dept_name", deptName);
                                        intent.putExtra("dept_phone", deptPhone);
                                        intent.putExtra("dept_lat", deptLat);
                                        intent.putExtra("dept_lng", deptLng);

                                        startActivity(intent);
                                        finish();
                                    }).addOnFailureListener(e -> {
                                        v.setEnabled(true);
                                        Toast.makeText(ConfirmationActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                            // Inside onSuccessListener after alertRef.setValue(newAlert)
                            String uid = FirebaseAuth.getInstance().getUid();
                            if (uid != null) {
                                FirebaseDatabase.getInstance().getReference("UserHistory")
                                        .child(uid)
                                        .child(generatedKey)
                                        .setValue(newAlert);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            v.setEnabled(true);
                            Toast.makeText(ConfirmationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private String getAddressFromLocation(double lat, double lng) {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
            java.util.List<android.location.Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            Log.e("GEOCODER", "Error: " + e.getMessage());
        }
        return "Address unavailable";
    }
}