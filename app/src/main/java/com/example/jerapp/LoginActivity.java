package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View; // Required for GONE/VISIBLE
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import android.app.AlertDialog;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailInputLayout;
    private MaterialButton btnLogin;
    private TextView btnRegister, btnForgotPassword, btnGuest;
    private FirebaseAuth mAuth;
    private boolean isAdminMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // 1. Bind all views
        emailInput = findViewById(R.id.emailInput);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInput);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnGuest = findViewById(R.id.btnGuestMode);
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.loginToggleGroup);

        // 2. The Logic: Admin mode hides features, User mode shows them
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnAdminMode) {
                    isAdminMode = true;

                    // ADMIN MODE: HIDE EVERYTHING
                    btnForgotPassword.setVisibility(View.GONE);
                    btnRegister.setVisibility(View.GONE);
                    btnGuest.setVisibility(View.GONE);

                    emailInputLayout.setHint("Admin Username");
                    emailInput.setInputType(InputType.TYPE_CLASS_TEXT);

                } else if (checkedId == R.id.btnUserMode) {
                    isAdminMode = false;

                    // USER MODE: SHOW EVERYTHING
                    btnForgotPassword.setVisibility(View.VISIBLE);
                    btnRegister.setVisibility(View.VISIBLE);
                    btnGuest.setVisibility(View.VISIBLE);

                    emailInputLayout.setHint("Email");
                    emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT);
                }
            }
        });

        // Clickable forgot password
        btnForgotPassword.setOnClickListener(v -> showPasswordResetDialog());

        // 3. Default to User Mode so features are visible initially
        toggleGroup.check(R.id.btnUserMode);

        // 4. Click Listeners
        btnGuest.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("isGuest", true);
            startActivity(intent);
            finish();
        });

        btnLogin.setOnClickListener(v -> loginLogic());
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        // btnForgotPassword click listener logic goes here...
    }

    private void loginLogic() {
        String input = emailInput.getText().toString().trim();
        String pass = passwordInput.getText().toString().trim();

        if (isAdminMode) {
            // Hardcoded Admin Credentials
            if (input.equals("admin") && pass.equals("admin123")) {
                startActivity(new Intent(this, AdminMainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Admin access denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Firebase User Login
            mAuth.signInWithEmailAndPassword(input, pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "User Login Failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showPasswordResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        // Create an EditText for the email input
        final EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your registered email");
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Add padding so it looks good inside the dialog
        float density = getResources().getDisplayMetrics().density;
        int padding = (int)(20 * density);
        emailInput.setPadding(padding, padding, padding, padding);

        builder.setView(emailInput);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Reset link sent to your email.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to send reset link: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Please enter an email address.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}