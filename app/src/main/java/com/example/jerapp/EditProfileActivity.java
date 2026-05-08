package com.example.jerapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.appcompat.app.AlertDialog;
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
    private RadioButton rbMale, rbFemale;
    private TextView tvEditTitle, tvEditDescription, tvForgotPwdInEdit; // Added variable here
    private FirebaseAuth mAuth;
    private TextView tvCurrentDisplay;
    private String currentValue;
    private static final String DB_URL = "https://jerapp-2026-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();

        editType = getIntent().getStringExtra("edit_type");
        currentValue = getIntent().getStringExtra("current_value");

        // 1. Bind Views (CRITICAL: Added the missing tvForgotPwdInEdit binding)
        inputLayout = findViewById(R.id.editInputLayout);
        confirmLayout = findViewById(R.id.confirmInputLayout);
        currentLayout = findViewById(R.id.currentPwdLayout);
        inputEditText = findViewById(R.id.etEditValue);
        confirmEditText = findViewById(R.id.etConfirmValue);
        currentEditText = findViewById(R.id.etCurrentValue);
        rgGender = findViewById(R.id.rgGender);
        tvCurrentDisplay = findViewById(R.id.tvCurrentDisplay);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        tvEditTitle = findViewById(R.id.tvEditTitle);
        tvEditDescription = findViewById(R.id.tvEditDescription);
        tvForgotPwdInEdit = findViewById(R.id.tvForgotPwdInEdit); // FIX: Bind it here

        // 2. Setup Logic
        configureUI();

        // 3. Click Listeners
        findViewById(R.id.btnBackEdit).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveEdit).setOnClickListener(v -> validateAndSave());

        // Forgot password logic
        // Inside onCreate, find this section and update it:
        tvForgotPwdInEdit.setOnClickListener(v -> {
            showPasswordResetDialog(); // Call the dialog method here
        });
    }

    private void configureUI() {
        if (editType == null) return;

        // Reset visibility
        inputLayout.setVisibility(View.VISIBLE);
        rgGender.setVisibility(View.GONE);
        confirmLayout.setVisibility(View.GONE);
        currentLayout.setVisibility(View.GONE);
        tvForgotPwdInEdit.setVisibility(View.GONE);
        tvCurrentDisplay.setVisibility(View.VISIBLE); // Most fields will use this

        // Default label text
        String currentLabel = "Current " + editType + ": " + (currentValue != null ? currentValue : "Not set");

        switch (editType) {
            case "name":
                tvEditTitle.setText("Edit Name");
                tvCurrentDisplay.setText("Current Name: " + currentValue);
                inputLayout.setHint("Enter New Full Name");
                // Disable toggle and show as text
                inputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                inputEditText.setTransformationMethod(null);
                break;

            case "email":
                tvEditTitle.setText("Change Email");
                tvCurrentDisplay.setText("Current Email: " + currentValue);
                inputLayout.setHint("New Email Address");
                // Disable toggle and show as text
                inputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                inputEditText.setTransformationMethod(null);
                // Enable toggle ONLY for the current password confirmation field
                currentLayout.setVisibility(View.VISIBLE);
                currentLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                currentEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                // Ensure the password field DOES show dots
                currentEditText.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                break;

            case "phone":
                tvEditTitle.setText("Edit Phone Number");
                tvCurrentDisplay.setText("Current Phone: " + currentValue);
                inputLayout.setHint("New Phone Number");
                // Disable toggle and show as numbers
                inputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
                inputEditText.setInputType(InputType.TYPE_CLASS_PHONE);
                inputEditText.setTransformationMethod(null);
                break;

            case "address":
                tvEditTitle.setText("Edit Address");
                tvCurrentDisplay.setText("Current Address: " + currentValue);
                inputLayout.setHint("New Home Address");
                // Disable toggle and show as multiline text
                inputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                inputEditText.setTransformationMethod(null);
                break;

            case "birthDate":
                tvEditTitle.setText("Edit Birth Date");
                tvCurrentDisplay.setText("Current DOB: " + currentValue);
                inputLayout.setHint("Select New Date");
                // Disable toggle and disable keyboard
                inputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
                inputEditText.setFocusable(false);
                inputEditText.setOnClickListener(v -> showDatePicker());
                inputEditText.setTransformationMethod(null);
                break;

            case "password":
                tvEditTitle.setText("Change Password");
                tvCurrentDisplay.setVisibility(View.GONE);
                currentLayout.setVisibility(View.VISIBLE);
                confirmLayout.setVisibility(View.VISIBLE);
                tvForgotPwdInEdit.setVisibility(View.VISIBLE);

                inputLayout.setHint("New Password");
                confirmLayout.setHint("Confirm New Password");

                // ENABLE toggle for all three password fields
                inputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                confirmLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                currentLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

                // Set all to password dots
                inputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                confirmEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                currentEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                inputEditText.setSelection(inputEditText.getText().length());
                confirmEditText.setSelection(confirmEditText.getText().length());
                break;

            case "gender":
                tvEditTitle.setText("Update Gender");
                tvCurrentDisplay.setVisibility(View.GONE);
                inputLayout.setVisibility(View.GONE);
                rgGender.setVisibility(View.VISIBLE);

                // Logic to auto-tick the radio button based on current value
                if (currentValue != null) {
                    if (currentValue.equalsIgnoreCase("Male")) {
                        rbMale.setChecked(true);
                    } else if (currentValue.equalsIgnoreCase("Female")) {
                        rbFemale.setChecked(true);
                    }
                }
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
        } else if ("email".equals(editType)) {
            changeEmailLogic(); // Call this exclusively for email
        } else {
            String val = inputEditText.getText().toString().trim();
            if (val.isEmpty()) {
                inputLayout.setError("Required");
                return;
            }
            updateDatabase(val);
        }
    }

    private void changeEmailLogic() {
        String currentPwd = currentEditText.getText().toString().trim();
        String newEmail = inputEditText.getText().toString().trim();

        if (currentPwd.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            inputLayout.setError("Invalid email format");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Re-authenticate is mandatory before sensitive operations
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPwd);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Use verifyBeforeUpdateEmail to send the link to the NEW email
                    user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(verifyTask -> {
                        if (verifyTask.isSuccessful()) {
                            // Success: The email isn't changed yet, but the link is sent
                            new AlertDialog.Builder(this)
                                    .setTitle("Verification Sent")
                                    .setMessage("A verification link has been sent to " + newEmail +
                                            ". Your email will be updated automatically once you click the link in your inbox.")
                                    .setPositiveButton("OK", (dialog, which) -> finish())
                                    .show();
                        } else {
                            Toast.makeText(this, "Error: " + verifyTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    currentLayout.setError("Incorrect current password");
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

    private void showPasswordResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final TextInputEditText resetEmail = new TextInputEditText(this);
        resetEmail.setHint("Enter registered email");
        resetEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Pre-fill the email since the user is already logged in
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            resetEmail.setText(user.getEmail());
        }

        // Styling the dialog input to match your Login page look
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(padding, (int)(8 * getResources().getDisplayMetrics().density), padding, 0);
        resetEmail.setLayoutParams(params);
        container.addView(resetEmail);

        builder.setView(container);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = resetEmail.getText().toString().trim();
            if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
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