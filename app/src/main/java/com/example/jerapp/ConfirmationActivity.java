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
import com.google.firebase.database.ServerValue;
import java.util.UUID;

public class ConfirmationActivity extends AppCompatActivity {

    private String deptId, deptName, deptPhone, emergencyType;
    private double deptLat, deptLng, userLat, userLng;
    private boolean isGuestFlow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        Intent incomingIntent = getIntent();
        isGuestFlow = incomingIntent.getBooleanExtra("isGuestFlow", false);

        deptId = incomingIntent.getStringExtra("dept_id");
        deptName = incomingIntent.getStringExtra("dept_name");
        deptPhone = incomingIntent.getStringExtra("dept_phone");
        emergencyType = incomingIntent.getStringExtra("emergency_type");
        deptLat = incomingIntent.getDoubleExtra("dept_lat", 0);
        deptLng = incomingIntent.getDoubleExtra("dept_lng", 0);
        userLat = incomingIntent.getDoubleExtra("user_lat", 0);
        userLng = incomingIntent.getDoubleExtra("user_lng", 0);

        // 1. Handle Buttons
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        // 2. Confirm Button
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            v.setEnabled(false); // Lock click execution

            DatabaseReference alertsDbRef = FirebaseDatabase.getInstance().getReference("ActiveAlerts").child(deptId);
            String generatedAlertKey = alertsDbRef.push().getKey();

            if (generatedAlertKey == null) {
                v.setEnabled(true);
                return;
            }

            String computedAddress = getAddressFromLocation(userLat, userLng);

            if (isGuestFlow) {
                // Read directly from the intent variables populated by GuestInfoActivity
                String guestName = incomingIntent.getStringExtra("guest_name");
                String guestPhone = incomingIntent.getStringExtra("guest_phone");
                String guestGender = incomingIntent.getStringExtra("guest_gender");

                AlertModel guestAlert = new AlertModel();
                guestAlert.setKey(generatedAlertKey);
                guestAlert.setUserId("GUEST_" + UUID.randomUUID().toString().substring(0, 8));
                guestAlert.setUserName(guestName + " (Guest)");
                guestAlert.setUserPhone(guestPhone);
                guestAlert.setGender(guestGender);
                guestAlert.setUserEmail("No Account (Guest Mode)");
                guestAlert.setEmergencyType(emergencyType);
                guestAlert.setStatus("Pending");
                guestAlert.setAssignedDept(deptId);
                guestAlert.setDept_id(deptId);
                guestAlert.setDeptName(deptName);
                guestAlert.setDeptPhone(deptPhone);
                guestAlert.setUserLat(userLat);
                guestAlert.setUserLng(userLng);
                guestAlert.setDeptLat(deptLat);
                guestAlert.setDeptLng(deptLng);
                guestAlert.setTextAddress(computedAddress);
                guestAlert.setTimestamp(ServerValue.TIMESTAMP);

                // Push payload straight out to Firebase database alerts node
                alertsDbRef.child(generatedAlertKey).setValue(guestAlert)
                        .addOnSuccessListener(aVoid -> {
                            Intent intent = new Intent(ConfirmationActivity.this, TrackingActivity.class);
                            intent.putExtra("alert_key", generatedAlertKey);
                            intent.putExtra("dept_id", deptId);
                            intent.putExtra("dept_name", deptName);
                            intent.putExtra("dept_phone", deptPhone);
                            intent.putExtra("dept_lat", deptLat);
                            intent.putExtra("dept_lng", deptLng);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            v.setEnabled(true);
                            Toast.makeText(ConfirmationActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });

            } else {
                // AUTHENTICATED ACCOUNT USER CODE PATHWAY
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    v.setEnabled(true);
                    return;
                }
                String uid = currentUser.getUid();

                FirebaseDatabase.getInstance().getReference("Users").child(uid)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String uName = snapshot.child("name").getValue(String.class);
                                String uPhone = snapshot.child("phone").getValue(String.class);
                                String uGender = snapshot.child("gender").getValue(String.class);
                                String uEmail = snapshot.child("email").getValue(String.class);

                                AlertModel userAlert = new AlertModel();
                                userAlert.setKey(generatedAlertKey);
                                userAlert.setUserId(uid);
                                userAlert.setUserName(uName);
                                userAlert.setUserPhone(uPhone);
                                userAlert.setGender(uGender);
                                userAlert.setUserEmail(uEmail);
                                userAlert.setEmergencyType(emergencyType);
                                userAlert.setStatus("Pending");
                                userAlert.setAssignedDept(deptId);
                                userAlert.setDept_id(deptId);
                                userAlert.setDeptName(deptName);
                                userAlert.setDeptPhone(deptPhone);
                                userAlert.setUserLat(userLat);
                                userAlert.setUserLng(userLng);
                                userAlert.setDeptLat(deptLat);
                                userAlert.setDeptLng(deptLng);
                                userAlert.setTextAddress(computedAddress);
                                userAlert.setTimestamp(ServerValue.TIMESTAMP);

                                alertsDbRef.child(generatedAlertKey).setValue(userAlert)
                                        .addOnSuccessListener(aVoid -> {

                                            // 2. CRITICAL FIX: Instantly copy the complete object payload over to UserHistory node
                                            FirebaseDatabase.getInstance().getReference("UserHistory")
                                                    .child(uid)
                                                    .child(generatedAlertKey)
                                                    .setValue(userAlert)
                                                    .addOnCompleteListener(task -> {
                                                        // Even if tracking history logs fail momentarily, proceed to tracking because the active alert is live
                                                        Intent intent = new Intent(ConfirmationActivity.this, TrackingActivity.class);
                                                        intent.putExtra("alert_key", generatedAlertKey);
                                                        intent.putExtra("dept_id", deptId);
                                                        intent.putExtra("dept_name", deptName);
                                                        intent.putExtra("dept_phone", deptPhone);
                                                        intent.putExtra("dept_lat", deptLat);
                                                        intent.putExtra("dept_lng", deptLng);
                                                        startActivity(intent);
                                                        finish();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            v.setEnabled(true);
                                            Toast.makeText(ConfirmationActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                v.setEnabled(true);
                            }
                        });
            }
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