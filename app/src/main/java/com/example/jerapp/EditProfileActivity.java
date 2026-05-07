package com.example.jerapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class EditProfileActivity extends AppCompatActivity {

    private String editType;
    private TextInputLayout inputLayout, confirmLayout, currentLayout;
    private TextInputEditText inputEditText, confirmEditText, currentEditText;
    private RadioGroup rgGender;
    private TextView tvEditTitle, tvEditDescription, tvForgotPwdInEdit; // Added variable here
    private FirebaseAuth mAuth;
    private static final String DB_URL = "https://jerapp-2026-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        editType = getIntent().getStringExtra("edit_type");

        // 1. Bind Views (CRITICAL: Added the missing tvForgotPwdInEdit binding)
        inputLayout = findViewById(R.id.editInputLayout);
        confirmLayout = findViewById(R.id.confirmInputLayout);
        currentLayout = findViewById(R.id.currentPwdLayout);
        inputEditText = findViewById(R.id.etEditValue);
        confirmEditText = findViewById(R.id.etConfirmValue);
        currentEditText = findViewById(R.id.etCurrentValue);
        rgGender = findViewById(R.id.rgGender);
        tvEditTitle = findViewById(R.id.tvEditTitle);
        tvEditDescription = findViewById(R.id.tvEditDescription);
        tvForgotPwdInEdit = findViewById(R.id.tvForgotPwdInEdit); // FIX: Bind it here

        // 2. Setup Logic
        configureUI();

        // 3. Click Listeners
        findViewById(R.id.btnBackEdit).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveEdit).setOnClickListener(v -> validateAndSave());

        // Forgot password logic
        tvForgotPwdInEdit.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                mAuth.sendPasswordResetEmail(user.getEmail())
                        .addOnCompleteListener(task -> Toast.makeText(this, "Reset link sent to email", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void configureUI() {
        if (editType == null) return;

        // Reset visibility for re-usability
        inputLayout.setVisibility(View.VISIBLE);
        rgGender.setVisibility(View.GONE);
        confirmLayout.setVisibility(View.GONE);
        currentLayout.setVisibility(View.GONE);
        tvForgotPwdInEdit.setVisibility(View.GONE);

        switch (editType) {
            case "name":
                tvEditTitle.setText("Edit Name");
                inputLayout.setHint("Full Name");
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                break;

            case "email":
                tvEditTitle.setText("Change Email");
                tvEditDescription.setText("Enter your new email address. You will need to verify this change.");
                inputLayout.setHint("New Email Address");
                currentLayout.setVisibility(View.VISIBLE); // Show "Current Password" for security
                inputEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                break;

            case "phone":
                tvEditTitle.setText("Edit Phone Number");
                inputLayout.setHint("Phone Number");
                inputEditText.setInputType(InputType.TYPE_CLASS_PHONE);
                break;

            case "address":
                tvEditTitle.setText("Edit Address");
                inputLayout.setHint("Home Address"); // Hint for address set here
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                break;

            case "password":
                tvEditTitle.setText("Change Password");
                tvEditDescription.setText("Verify your identity to change password.");
                inputLayout.setHint("New Password");

                // Show password specific fields
                currentLayout.setVisibility(View.VISIBLE);
                confirmLayout.setVisibility(View.VISIBLE);
                tvForgotPwdInEdit.setVisibility(View.VISIBLE); // Shows the link

                // Mask input characters
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                break;

            case "birthDate":
                tvEditTitle.setText("Edit Birth Date");
                inputLayout.setHint("Date of Birth");
                inputEditText.setFocusable(false);
                inputEditText.setOnClickListener(v -> showDatePicker());
                break;

            case "gender":
                tvEditTitle.setText("Update Gender");
                inputLayout.setVisibility(View.GONE);
                rgGender.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = day + "/" + (month + 1) + "/" + year;
            inputEditText.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void validateAndSave() {
        if ("gender".equals(editType)) {
            int selectedId = rgGender.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
                return;
            }
            String gender = (selectedId == R.id.rbMale) ? "Male" : "Female";
            updateDatabase(gender);
        } else if ("password".equals(editType)) {
            changePasswordLogic();
        } else {
            String val = inputEditText.getText().toString().trim();
            if (val.isEmpty()) {
                inputLayout.setError("Required");
                return;
            }
            updateDatabase(val);
        }
        if ("email".equals(editType)) {
            changeEmailLogic();
        }
    }

    private void changeEmailLogic() {
        String currentPwd = currentEditText.getText().toString().trim();
        String newEmail = inputEditText.getText().toString().trim();

        if (currentPwd.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPwd);

            // 1. Re-authenticate
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 2. Update Email in Auth
                    user.updateEmail(newEmail).addOnCompleteListener(emailTask -> {
                        if (emailTask.isSuccessful()) {
                            // 3. Sync with Realtime Database
                            updateDatabase(newEmail);
                        } else {
                            Toast.makeText(this, "Error: " + emailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    currentLayout.setError("Incorrect password");
                }
            });
        }
    }

    private void changePasswordLogic() {
        String currentPwd = currentEditText.getText().toString().trim();
        String newPwd = inputEditText.getText().toString().trim();
        String confirmPwd = confirmEditText.getText().toString().trim();

        if (currentPwd.isEmpty()) {
            currentLayout.setError("Current password required");
            return;
        }
        if (newPwd.length() < 6) {
            inputLayout.setError("Minimum 6 characters");
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            confirmLayout.setError("Passwords do not match");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPwd);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPwd).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(this, "Password Updated", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } else {
                    currentLayout.setError("Incorrect current password");
                }
            });
        }
    }

    private void updateDatabase(String value) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid)
                .child(editType).setValue(value)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }
}