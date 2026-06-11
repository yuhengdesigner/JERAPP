package com.example.jerapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        // REQUIREMENT: Allow user to auto login
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isAnonymous()) {
                mAuth.signOut();
                SessionUtils.clearGuestSession(this);
            } else {
                // If a registered user is already authenticated, attempt auto-login
                checkUserRole();
                // We don't finish() here yet because checkUserRole is asynchronous
            }
        }

        // 2. Mode Toggle Logic
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
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
                    emailInputLayout.setHint("Email Address");
                    emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT);
                }
            }
        });

        // 3. Click Listeners
        btnGuest.setOnClickListener(v -> {
            mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                    SessionUtils.markGuest(this, uid);
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("isGuest", true);
                    startActivity(intent);
                    finish();
                }
            });
        });

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnForgotPassword.setOnClickListener(v -> showPasswordResetDialog());
        btnLogin.setOnClickListener(v -> {
            hideKeyboard();
            loginLogic();
        });

        toggleGroup.check(R.id.btnUserMode);
    }

    private void loginLogic() {
        String input = (emailInput.getText() != null) ? emailInput.getText().toString().trim() : "";
        String pass = (passwordInput.getText() != null) ? passwordInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(input)) {
            emailInputLayout.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            passwordInputLayout.setError("Required");
            return;
        }
        if (!isAdminMode && !Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            emailInputLayout.setError("Invalid email format");
            return;
        }

        if (isAdminMode) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
            usersRef.orderByChild("username").equalTo(input).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String adminEmail = null;
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            adminEmail = userSnap.child("email").getValue(String.class);
                        }
                        if (!TextUtils.isEmpty(adminEmail)) {
                            performFirebaseAuth(adminEmail, pass);
                        } else {
                            Toast.makeText(LoginActivity.this, "Email not found for this admin", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Admin Username not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            performFirebaseAuth(input, pass);
        }
    }

    private void performFirebaseAuth(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        if (!isAdminMode && mAuth.getCurrentUser() != null && !mAuth.getCurrentUser().isEmailVerified()) {
                            mAuth.signOut();
                            showErrorDialog("Email Not Verified", "Please verify your email before logging in.");
                            return;
                        }
                        checkUserRole();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Auth failed";
                        Toast.makeText(this, "Login Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRole() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    String deptId = snapshot.child("dept_id").getValue(String.class);

                    // Robust Check: Handle navigation based on role from database
                    if ("admin".equalsIgnoreCase(role)) {
                        getSharedPreferences("AdminPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("dept_id", deptId)
                                .apply();

                        Intent intent = new Intent(LoginActivity.this, AdminMainActivity.class);
                        intent.putExtra("DEPT_ID", deptId);
                        startActivity(intent);
                        finish();
                    } else if ("customer".equalsIgnoreCase(role) || role == null) {
                        SessionUtils.markRegistered(LoginActivity.this);
                        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        String guestUid = prefs.getString("guest_uid", null);
                        
                        // Transfer history if user was a guest on this device
                        if (guestUid != null && !uid.equals(guestUid)) {
                            DatabaseReference oldRef = FirebaseDatabase.getInstance().getReference("UserHistory").child(guestUid);
                            DatabaseReference newRef = FirebaseDatabase.getInstance().getReference("UserHistory").child(uid);
                            
                            oldRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snap) {
                                    if (snap.exists()) {
                                        for (DataSnapshot ds : snap.getChildren()) {
                                            newRef.child(ds.getKey()).setValue(ds.getValue());
                                        }
                                        oldRef.removeValue();
                                    }
                                    prefs.edit().remove("guest_uid").apply();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }
                            });
                        } else {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        // Unknown role
                        mAuth.signOut();
                        showErrorDialog("Access Denied", "Invalid account role.");
                    }
                } else {
                    // This is for users who have no database record (e.g. deleted but still have auth session)
                    mAuth.signOut();
                    // Don't show error dialog during auto-login to avoid popping it up on start if session is invalid
                    if (findViewById(android.R.id.content).isShown()) {
                         showErrorDialog("Database Error", "User data not found.");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (findViewById(android.R.id.content).isShown()) {
                    showErrorDialog("Firebase Error", error.getMessage());
                }
            }
        });
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showPasswordResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        final TextInputEditText resetEmail = new TextInputEditText(this);
        resetEmail.setHint("Enter registered email");
        resetEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(padding, (int)(8 * getResources().getDisplayMetrics().density), padding, 0);
        resetEmail.setLayoutParams(params);
        container.addView(resetEmail);
        builder.setView(container);
        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = resetEmail.getText().toString().trim();
            if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) Toast.makeText(this, "Reset link sent!", Toast.LENGTH_SHORT).show();
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
