package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class GuestInfoActivity extends AppCompatActivity {

    private EditText etGuestName, etGuestPhone;
    private RadioGroup rgGuestGender;

    // Hold incoming department parameters from DepartmentListActivity routing
    private String deptId, deptName, deptPhone, emergencyType;
    private double deptLat, deptLng, userLat, userLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_info);

        // Extract parameters passed from DepartmentListActivity selection
        Intent fromIntent = getIntent();
        deptId = fromIntent.getStringExtra("dept_id");
        deptName = fromIntent.getStringExtra("dept_name");
        deptPhone = fromIntent.getStringExtra("dept_phone");
        emergencyType = fromIntent.getStringExtra("emergency_type");
        deptLat = fromIntent.getDoubleExtra("dept_lat", 0);
        deptLng = fromIntent.getDoubleExtra("dept_lng", 0);
        userLat = fromIntent.getDoubleExtra("user_lat", 0);
        userLng = fromIntent.getDoubleExtra("user_lng", 0);

        // Bind layout controls
        etGuestName = findViewById(R.id.etGuestName);
        etGuestPhone = findViewById(R.id.etGuestPhone);
        rgGuestGender = findViewById(R.id.rgGuestGender);
        Button btnProceed = findViewById(R.id.btnProceedToConfirm);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnProceed.setOnClickListener(v -> {
            String name = etGuestName.getText().toString().trim();
            String phone = etGuestPhone.getText().toString().trim();
            String gender = "Male";

            if (rgGuestGender.getCheckedRadioButtonId() == R.id.rbFemale) {
                gender = "Female";
            }

            // Quick validation to prevent empty submissions to admin
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Forward everything safely to ConfirmationActivity
            Intent intent = new Intent(GuestInfoActivity.this, ConfirmationActivity.class);
            intent.putExtra("isGuestFlow", true);
            intent.putExtra("guest_name", name);
            intent.putExtra("guest_phone", phone);
            intent.putExtra("guest_gender", gender);

            // Re-bundle original department metadata properties
            intent.putExtra("dept_id", deptId);
            intent.putExtra("dept_name", deptName);
            intent.putExtra("dept_phone", deptPhone);
            intent.putExtra("emergency_type", emergencyType);
            intent.putExtra("dept_lat", deptLat);
            intent.putExtra("dept_lng", deptLng);
            intent.putExtra("user_lat", userLat);
            intent.putExtra("user_lng", userLng);

            startActivity(intent);
            finish();
        });
    }
}