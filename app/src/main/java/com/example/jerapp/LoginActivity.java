package com.example.jerapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private MaterialButton btnLogin;
    private TextView btnRegister, btnForgotPassword, btnGuest;
    private FirebaseAuth mAuth;
    private boolean isAdminMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // 1. Bind views
        emailInput = findViewById(R.id.emailInput);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInput);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnGuest = findViewById(R.id.btnGuestMode);
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.loginToggleGroup);

        // 2. Mode Toggle Logic
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                // Clear errors and text when switching modes
                emailInputLayout.setError(null);
                passwordInputLayout.setError(null);
                emailInput.setText("");
                passwordInput.setText("");

                if (checkedId == R.id.btnAdminMode) {
                    isAdminMode = true;
                    btnForgotPassword.setVisibility(View.GONE);
                    btnRegister.setVisibility(View.GONE);
                    btnGuest.setVisibility(View.GONE);
                    emailInputLayout.setHint("Admin Username");
                    emailInput.setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    isAdminMode = false;
                    btnForgotPassword.setVisibility(View.VISIBLE);
                    btnRegister.setVisibility(View.VISIBLE);
                    btnGuest.setVisibility(View.VISIBLE);
                    emailInputLayout.setHint("Email");
                    emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT);
                }
            }
        });

        // 3. Click Listeners
        btnGuest.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("isGuest", true);
            startActivity(intent);
            finish();
        });

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnForgotPassword.setOnClickListener(v -> showPasswordResetDialog());
        btnLogin.setOnClickListener(v -> loginLogic());

        // Default selection
        toggleGroup.check(R.id.btnUserMode);
    }

    private void loginLogic() {
        String input = emailInput.getText().toString().trim();
        String pass = passwordInput.getText().toString().trim();

        // --- VALIDATION (Prevents "Jumping out" crash) ---
        boolean hasError = false;

        if (TextUtils.isEmpty(input)) {
            emailInputLayout.setError(isAdminMode ? "Username required" : "Email required");
            hasError = true;
        } else {
            emailInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(pass)) {
            passwordInputLayout.setError("Password required");
            hasError = true;
        } else {
            passwordInputLayout.setError(null);
        }

        if (hasError) return; // Exit if validation fails

        // --- LOGIN PROCESS ---
        if (isAdminMode) {
            if (input.equals("admin") && pass.equals("admin123")) {
                startActivity(new Intent(this, AdminMainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Admin access denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            mAuth.signInWithEmailAndPassword(input, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra("isGuest", false);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorMsg = task.getException() != null ?
                                    task.getException().getMessage() : "User Login Failed";
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void showPasswordResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        // Use a TextInputLayout/TextInputEditText for consistent UI
        final TextInputEditText resetEmail = new TextInputEditText(this);
        resetEmail.setHint("Enter registered email");
        resetEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Styling the container for the EditText
        float density = getResources().getDisplayMetrics().density;
        int padding = (int) (24 * density);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(padding, (int)(8 * density), padding, 0);
        resetEmail.setLayoutParams(params);
        container.addView(resetEmail);
        builder.setView(container);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = resetEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firebase sends the email based on the template you edited in the console
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Check your email to reset password!", Toast.LENGTH_LONG).show();
                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}