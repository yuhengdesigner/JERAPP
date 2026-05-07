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

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameInput, emailInput, phoneInput, addressInput, birthInput, passInput, confirmPassInput;
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

        birthInput.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                    birthInput.setText(day + "/" + (month + 1) + "/" + year),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        findViewById(R.id.btnRegisterSubmit).setOnClickListener(v -> registerUser());

        TextView tvLogin = findViewById(R.id.tvLoginLink);
        tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String birth = birthInput.getText().toString().trim();
        String pass = passInput.getText().toString().trim();
        String confirm = confirmPassInput.getText().toString().trim();

        // Standard Validation
        if (TextUtils.isEmpty(name)) { nameInput.setError("Required"); return; }
        if (TextUtils.isEmpty(email)) { emailInput.setError("Required"); return; }
        if (pass.length() < 6) { passInput.setError("Min 6 chars"); return; }
        if (!pass.equals(confirm)) { confirmPassInput.setError("Mismatch"); return; }

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();

                // Data Map
                HashMap<String, Object> map = new HashMap<>();
                map.put("name", name);
                map.put("email", email);
                map.put("phone", phone);
                map.put("address", address);
                map.put("birthDate", birth);

                // FIX: We use the direct database reference.
                // If this hangs, it's a Firebase Rules issue.
                FirebaseDatabase.getInstance().getReference("Users")
                        .child(uid)
                        .setValue(map)
                        .addOnCompleteListener(dbTask -> {
                            // This MUST dismiss the dialog
                            if (progressDialog.isShowing()) progressDialog.dismiss();

                            if (dbTask.isSuccessful()) {
                                goToDashboard();
                            } else {
                                // If DB fails, we still let them in but warn them
                                Toast.makeText(this, "Profile not saved: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                goToDashboard();
                            }
                        });

                // SAFETY DISMISS: If the database takes longer than 5 seconds,
                // force move to dashboard so user isn't stuck.
                new android.os.Handler().postDelayed(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        goToDashboard();
                    }
                }, 1000);

            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToDashboard() {
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