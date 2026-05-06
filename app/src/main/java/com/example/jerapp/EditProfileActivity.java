package com.example.jerapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditProfileActivity extends AppCompatActivity {

    private String editType;
    private TextInputLayout inputLayout, confirmLayout;
    private TextInputEditText inputEditText, confirmEditText;
    private TextView tvForgotPassword, tvEditTitle, tvEditDescription;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();

        // 1. Bind Views
        editType = getIntent().getStringExtra("edit_type");
        inputLayout = findViewById(R.id.editInputLayout);
        confirmLayout = findViewById(R.id.confirmInputLayout);
        inputEditText = findViewById(R.id.etEditValue);
        confirmEditText = findViewById(R.id.etConfirmValue);
        tvForgotPassword = findViewById(R.id.tvForgotPwdInEdit);
        tvEditTitle = findViewById(R.id.tvEditTitle);
        tvEditDescription = findViewById(R.id.tvEditDescription);
        MaterialButton btnSave = findViewById(R.id.btnSaveEdit);

        // 2. Setup UI based on editType
        configureUI();

        // 3. Click Listeners
        findViewById(R.id.btnBackEdit).setOnClickListener(v -> finish());

        tvForgotPassword.setOnClickListener(v -> showPasswordResetDialog());

        btnSave.setOnClickListener(v -> saveData());
    }

    private void configureUI() {
        if (editType == null) return;

        switch (editType) {
            case "name":
                tvEditTitle.setText("Edit Name");
                inputLayout.setHint("Full Name");
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                break;

            case "phone":
                tvEditTitle.setText("Edit Phone Number");
                inputLayout.setHint("Phone Number");
                inputEditText.setInputType(InputType.TYPE_CLASS_PHONE);
                break;

            case "address":
                tvEditTitle.setText("Edit Address");
                inputLayout.setHint("Home Address");
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                break;

            case "password":
                tvEditTitle.setText("Change Password");
                tvEditDescription.setText("Enter and confirm your new password.");
                inputLayout.setHint("New Password");
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                inputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

                // Show extra password fields
                confirmLayout.setVisibility(View.VISIBLE);
                tvForgotPassword.setVisibility(View.VISIBLE);
                break;

            case "birthDate":
                tvEditTitle.setText("Edit Birth Date");
                inputLayout.setHint("DD/MM/YYYY");
                break;
        }
    }

    private void saveData() {
        String newValue = inputEditText.getText().toString().trim();

        if (newValue.isEmpty()) {
            inputLayout.setError("Field cannot be empty");
            return;
        }

        if ("password".equals(editType)) {
            handlePasswordUpdate(newValue);
        } else {
            handleGeneralUpdate(newValue);
        }
    }

    private void handlePasswordUpdate(String newPassword) {
        String confirmPassword = confirmEditText.getText().toString().trim();

        if (newPassword.length() < 6) {
            inputLayout.setError("Password must be at least 6 characters");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            confirmLayout.setError("Passwords do not match");
            return;
        }

        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            // If security delay is too long, Firebase requires re-authentication
                            Toast.makeText(this, "Error: Re-login required for security", Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void handleGeneralUpdate(String newValue) {
        String uid = mAuth.getUid();
        if (uid != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            ref.child(editType).setValue(newValue).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to update database", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showPasswordResetDialog() {
        if (mAuth.getCurrentUser() == null) return;

        String email = mAuth.getCurrentUser().getEmail();

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("We will send a reset link to " + email)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Check your email!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Failed to send email", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}