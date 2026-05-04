package com.example.jerapp;

import android.app.DatePickerDialog;
import android.content.Intent; // Added missing import
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView; // Added missing import
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // --- 1. Combined ActionBar logic here ---
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Back");
        }

        mAuth = FirebaseAuth.getInstance();

        // Bind all IDs
        nameInput = findViewById(R.id.nameRegister);
        emailInput = findViewById(R.id.emailRegister);
        phoneInput = findViewById(R.id.regPhone);
        addressInput = findViewById(R.id.regAddress);
        birthInput = findViewById(R.id.regBirthDate);
        passInput = findViewById(R.id.passwordRegister);
        confirmPassInput = findViewById(R.id.regConfirmPassword);
        genderGroup = findViewById(R.id.genderGroup);

        // Date Picker for Birth Date
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
        String email = emailInput.getText().toString();
        String pass = passInput.getText().toString();
        String confirm = confirmPassInput.getText().toString();

        if (!pass.equals(confirm)) {
            confirmPassInput.setError("Passwords do not match!");
            return;
        }

        if (TextUtils.isEmpty(email) || pass.length() < 6) {
            Toast.makeText(this, "Valid email and 6-char password required", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                int selectedId = genderGroup.getCheckedRadioButtonId();
                RadioButton rb = findViewById(selectedId);

                HashMap<String, Object> map = new HashMap<>();
                map.put("name", nameInput.getText().toString());
                map.put("phone", phoneInput.getText().toString());
                map.put("address", addressInput.getText().toString());
                map.put("birthDate", birthInput.getText().toString());
                map.put("gender", rb != null ? rb.getText().toString() : "Not Specified");

                FirebaseDatabase.getInstance().getReference("Users").child(uid).setValue(map)
                        .addOnSuccessListener(aVoid -> {
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
            } else {
                Toast.makeText(this, "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Handles the top-left back arrow click
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}