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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

public class LoginActivity extends BaseActivity {

    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private MaterialButton btnLogin;
    private TextView btnRegister, btnForgotPassword, btnGuest;
    private FirebaseAuth mAuth;
    private boolean isAdminMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        emailInput = findViewById(R.id.emailInput);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInput);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnGuest = findViewById(R.id.btnGuestMode);
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.loginToggleGroup);

        // REQUIREMENT 1: Auto-login Guest only if they have an ongoing emergency
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            final View rootContent = findViewById(android.R.id.content);
            if (currentUser.isAnonymous()) {
                if (rootContent != null) rootContent.setVisibility(View.INVISIBLE);
                checkGuestEmergencyStatus(currentUser.getUid(), rootContent);
            } else {
                checkUserRole();
            }
        }

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnAdminMode) {
                    isAdminMode = true;
                    btnForgotPassword.setVisibility(View.GONE);
                    btnRegister.setVisibility(View.GONE);
                    btnGuest.setVisibility(View.GONE);
                    emailInputLayout.setHint("Admin Username");
                } else {
                    isAdminMode = false;
                    btnForgotPassword.setVisibility(View.VISIBLE);
                    btnRegister.setVisibility(View.VISIBLE);
                    btnGuest.setVisibility(View.VISIBLE);
                    emailInputLayout.setHint("Email Address");
                }
            }
        });

        btnGuest.setOnClickListener(v -> mAuth.signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                SessionUtils.markGuest(this, mAuth.getUid());
                startActivity(new Intent(this, MainActivity.class).putExtra("isGuest", true));
                finish();
            }
        }));

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnForgotPassword.setOnClickListener(v -> showPasswordResetDialog());
        btnLogin.setOnClickListener(v -> loginLogic());

        toggleGroup.check(R.id.btnUserMode);
    }

    private void checkGuestEmergencyStatus(String uid, View root) {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("UserHistory").child(uid);
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasActive = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    if ("Pending".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status) || "Processing".equalsIgnoreCase(status)) {
                        hasActive = true;
                        break;
                    }
                }
                if (hasActive) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class).putExtra("isGuest", true));
                    finish();
                } else {
                    mAuth.signOut();
                    SessionUtils.clearGuestSession(LoginActivity.this);
                    if (root != null) root.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (root != null) root.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loginLogic() {
        String input = emailInput.getText().toString().trim();
        String pass = passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(input) || TextUtils.isEmpty(pass)) return;

        if (isAdminMode) {
            FirebaseDatabase.getInstance().getReference("Users").orderByChild("username").equalTo(input)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                performFirebaseAuth(ds.child("email").getValue(String.class), pass);
                            }
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
        } else {
            performFirebaseAuth(input, pass);
        }
    }

    private void performFirebaseAuth(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) checkUserRole();
            else Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkUserRole() {
        String uid = mAuth.getUid();
        if (uid == null) return;
        FirebaseDatabase.getInstance().getReference("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if ("admin".equalsIgnoreCase(role)) {
                        startActivity(new Intent(LoginActivity.this, AdminMainActivity.class).putExtra("DEPT_ID", snapshot.child("dept_id").getValue(String.class)));
                    } else {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    }
                    finish();
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void showPasswordResetDialog() { /* Existed logic */ }
}
