package com.example.jerapp;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;
import java.util.HashMap;

import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.Color;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameInput, emailInput, phoneInput, addressInput, birthInput, passInput, confirmPassInput;
    private TextView hintLength, hintUpper, hintLower, hintNumber, hintSymbol;
    private RadioGroup genderGroup;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Back");
        }

        mAuth = FirebaseAuth.getInstance();

        // Setup Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating your account...");
        progressDialog.setCancelable(false);

        // Bind Views
        nameInput = findViewById(R.id.nameRegister);
        emailInput = findViewById(R.id.emailRegister);
        phoneInput = findViewById(R.id.regPhone);
        addressInput = findViewById(R.id.regAddress);
        birthInput = findViewById(R.id.regBirthDate);
        passInput = findViewById(R.id.passwordRegister);
        confirmPassInput = findViewById(R.id.regConfirmPassword);
        genderGroup = findViewById(R.id.genderGroup);

        hintLength = findViewById(R.id.hintLength);
        hintUpper = findViewById(R.id.hintUpper);
        hintLower = findViewById(R.id.hintLower);
        hintNumber = findViewById(R.id.hintNumber);
        hintSymbol = findViewById(R.id.hintSymbol);

        passInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String p = s.toString();
                boolean empty = p.isEmpty();
                updateHintColor(hintLength, p.length() >= 8, empty);
                updateHintColor(hintUpper, p.matches(".*[A-Z].*"), empty);
                updateHintColor(hintLower, p.matches(".*[a-z].*"), empty);
                updateHintColor(hintNumber, p.matches(".*\\d.*"), empty);
                updateHintColor(hintSymbol, p.matches(".*[@$!%*?&#^()_+\\-\\[\\]{}|;:'\",.<>/?\\\\].*"), empty);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        birthInput.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, day) ->
                    birthInput.setText(day + "/" + (month + 1) + "/" + year),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMaxDate(System.currentTimeMillis());
            dpd.show();
        });

        findViewById(R.id.btnRegisterSubmit).setOnClickListener(v -> registerUser());

        // REQUIREMENT: Link "Login" button to always navigate to LoginActivity correctly
        TextView tvLogin = findViewById(R.id.tvLoginLink);
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            // Clear top ensures we don't keep multiple login screens in the backstack
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void updateHintColor(TextView tv, boolean isValid, boolean isEmpty) {
        if (isEmpty) {
            tv.setTextColor(Color.parseColor("#757575")); // Gray
        } else if (isValid) {
            tv.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            tv.setTextColor(Color.parseColor("#F44336")); // Red
        }
    }

    private void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String birth = birthInput.getText().toString().trim();
        String pass = passInput.getText().toString().trim();
        String confirm = confirmPassInput.getText().toString().trim();

        String gender = "Prefer Not to Say";
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton selectedGender = findViewById(selectedGenderId);
            gender = selectedGender.getText().toString();
        }

        if (TextUtils.isEmpty(name)) { nameInput.setError("Required"); return; }
        if (!name.matches("^[a-zA-Z\\s]+$")) { nameInput.setError("No numbers or symbols allowed"); return; }
        
        if (TextUtils.isEmpty(email)) { emailInput.setError("Required"); return; }
        if (!email.matches("^[a-zA-Z].*")) { emailInput.setError("Cannot start with numbers, symbols or @"); return; }
        
        if (pass.length() < 8) { passInput.setError("Min 8 chars"); return; }
        if (!pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-\\[\\]{}|;:'\",.<>/?\\\\])[A-Za-z\\d@$!%*?&#^()_+\\-\\[\\]{}|;:'\",.<>/?\\\\]{8,}$")) {
            passInput.setError("Must contain uppercase, lowercase, number, and symbol");
            return;
        }
        if (!pass.equals(confirm)) { confirmPassInput.setError("Mismatch"); return; }

        progressDialog.show();
        
        final String finalGender = gender;

        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();

                HashMap<String, Object> map = new HashMap<>();
                map.put("name", name);
                map.put("email", email);
                map.put("phone", phone);
                map.put("address", address);
                map.put("birthDate", birth);
                map.put("gender", finalGender);
                map.put("role", "customer");

                FirebaseDatabase.getInstance().getReference("Users")
                        .child(uid)
                        .setValue(map)
                        .addOnCompleteListener(dbTask -> {
                            if (progressDialog.isShowing()) progressDialog.dismiss();
                            
                            mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(verifTask -> {
                                if (verifTask.isSuccessful()) {
                                    androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Verify Email")
                                        .setMessage("Registration successful. A verification email has been sent to " + email + ".\n\nPlease click the link in the email to verify, then click 'I've Verified' below to proceed.")
                                        .setPositiveButton("I've Verified", null)
                                        .setNegativeButton("Cancel", (d, which) -> {
                                            com.google.firebase.auth.FirebaseUser currentUser = mAuth.getCurrentUser();
                                            if (currentUser != null) {
                                                FirebaseDatabase.getInstance().getReference("Users").child(uid).removeValue();
                                                currentUser.delete();
                                            }
                                        })
                                        .setCancelable(false)
                                        .create();
                                        
                                    dialog.setOnShowListener(d -> {
                                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                                            com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
                                            if (user != null) {
                                                user.reload().addOnCompleteListener(reloadTask -> {
                                                    if (user.isEmailVerified()) {
                                                        dialog.dismiss();
                                                        transferGuestHistoryAndGoToDashboard(uid);
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Email not verified yet. Please check your inbox and spam folder.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        });
                                    });
                                    dialog.show();
                                } else {
                                    Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                    finish();
                                }
                            });
                        });
            } else {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                Toast.makeText(this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void transferGuestHistoryAndGoToDashboard(String uid) {
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String guestUid = prefs.getString("guest_uid", null);
        if (guestUid != null) {
            com.google.firebase.database.DatabaseReference oldRef = FirebaseDatabase.getInstance().getReference("UserHistory").child(guestUid);
            com.google.firebase.database.DatabaseReference newRef = FirebaseDatabase.getInstance().getReference("UserHistory").child(uid);
            
            oldRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snap) {
                    if (snap.exists()) {
                        for (com.google.firebase.database.DataSnapshot ds : snap.getChildren()) {
                            newRef.child(ds.getKey()).setValue(ds.getValue());
                        }
                        oldRef.removeValue();
                    }
                    SessionUtils.clearGuestSession(RegisterActivity.this);
                    goToDashboard();
                }

                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                    goToDashboard();
                }
            });
        } else {
            goToDashboard();
        }
    }

    private void goToDashboard() {
        SessionUtils.markRegistered(this);
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.putExtra("isGuest", false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
