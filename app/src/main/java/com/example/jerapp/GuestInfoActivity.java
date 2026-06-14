package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class GuestInfoActivity extends BaseActivity { // Inherit from BaseActivity for dark mode fix

    private EditText etGuestName, etGuestPhone, etGuestEmail;
    private RadioGroup rgGuestGender;

    private String deptId, deptName, deptPhone, emergencyType;
    private double deptLat, deptLng, userLat, userLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_info);

        Intent fromIntent = getIntent();
        deptId = fromIntent.getStringExtra("dept_id");
        deptName = fromIntent.getStringExtra("dept_name");
        deptPhone = fromIntent.getStringExtra("dept_phone");
        emergencyType = fromIntent.getStringExtra("emergency_type");
        deptLat = fromIntent.getDoubleExtra("dept_lat", 0);
        deptLng = fromIntent.getDoubleExtra("dept_lng", 0);
        userLat = fromIntent.getDoubleExtra("user_lat", 0);
        userLng = fromIntent.getDoubleExtra("user_lng", 0);

        etGuestName = findViewById(R.id.etGuestName);
        etGuestPhone = findViewById(R.id.etGuestPhone);
        etGuestEmail = findViewById(R.id.etGuestEmail);
        rgGuestGender = findViewById(R.id.rgGuestGender);
        Button btnProceed = findViewById(R.id.btnProceedToConfirm);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnProceed.setOnClickListener(v -> {
            String name = etGuestName.getText().toString().trim();
            String phone = etGuestPhone.getText().toString().trim();
            String email = etGuestEmail.getText().toString().trim();
            String gender = "Male";

            if (rgGuestGender.getCheckedRadioButtonId() == R.id.rbFemale) {
                gender = "Female";
            }

            // REQUIREMENT 4: Strict Validations
            
            // 1. Name: No symbols and numbers
            if (TextUtils.isEmpty(name)) {
                etGuestName.setError("Name is required");
                return;
            }
            if (!name.matches("^[a-zA-Z\\s]+$")) {
                etGuestName.setError("Name must contain only letters and spaces");
                return;
            }

            // 2. Phone: Invalid or single digit
            if (TextUtils.isEmpty(phone)) {
                etGuestPhone.setError("Phone number is required");
                return;
            }
            if (!Patterns.PHONE.matcher(phone).matches() || phone.length() < 7) {
                etGuestPhone.setError("Please enter a valid phone number");
                return;
            }

            // 3. Email: Invalid format or starts with digit/symbol
            if (TextUtils.isEmpty(email)) {
                etGuestEmail.setError("Email is required");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etGuestEmail.setError("Invalid email format");
                return;
            }
            if (Character.isDigit(email.charAt(0)) || !Character.isLetter(email.charAt(0))) {
                etGuestEmail.setError("Email cannot start with numbers or symbols");
                return;
            }

            Intent intent = new Intent(GuestInfoActivity.this, ConfirmationActivity.class);
            intent.putExtra("isGuestFlow", true);
            intent.putExtra("guest_name", name);
            intent.putExtra("guest_phone", phone);
            intent.putExtra("guest_email", email);
            intent.putExtra("guest_gender", gender);

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