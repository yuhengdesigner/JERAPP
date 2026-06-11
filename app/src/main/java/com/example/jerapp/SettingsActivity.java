package com.example.jerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        setupClickListeners();
        checkUserStatus();
    }

    private void checkUserStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        com.google.android.material.button.MaterialButton btnDelete = findViewById(R.id.btnDeleteAccount);

        if (btnDelete == null) return;

        if (user != null && user.isAnonymous()) {
            // UI for Guest
            btnDelete.setText("Create an account");
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green
            btnDelete.setOnClickListener(v -> {
                // REQUIREMENT 6: Disable guest registration if emergency is ongoing
                if (hasOngoingEmergency()) {
                    showOngoingEmergencyWarning(getString(R.string.ongoing_emergency_register_message));
                } else {
                    startActivity(new Intent(SettingsActivity.this, RegisterActivity.class));
                }
            });
        } else {
            // UI for Registered
            btnDelete.setText("Delete Account");
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))); // Red
            btnDelete.setOnClickListener(v -> showDeleteAccountDialog());
        }
    }

    private void setupClickListeners() {
        View btnBack = findViewById(R.id.btnBackSettings);
        View cardProfile = findViewById(R.id.cardSettingsProfile);
        View cardTerms = findViewById(R.id.cardTerms);
        View cardAbout = findViewById(R.id.cardAbout);
        View btnLogout = findViewById(R.id.btnProfileLogout);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isAnonymous()) {
                    Toast.makeText(this, "Guest profile is not available.", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
                }
            });
        }

        if (cardTerms != null) {
            cardTerms.setOnClickListener(v ->
                    startActivity(new Intent(SettingsActivity.this, TermsActivity.class)));
        }

        if (cardAbout != null) {
            cardAbout.setOnClickListener(v ->
                    startActivity(new Intent(SettingsActivity.this, AboutActivity.class)));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // REQUIREMENT 5: Only block logout for GUESTS during emergency
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null && user.isAnonymous() && hasOngoingEmergency()) {
                    showOngoingEmergencyWarning(getString(R.string.ongoing_emergency_warning_message));
                } else {
                    showLogoutDialog();
                }
            });
        }
    }

    private boolean hasOngoingEmergency() {
        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        return prefs.getBoolean("has_active_emergency", false);
    }

    private void showOngoingEmergencyWarning(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.ongoing_emergency_warning_title))
                .setMessage(message)
                .setPositiveButton("I Understand", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout from JER APP?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    SessionUtils.clearGuestSession(this);
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This action is permanent. All your data will be removed from Johor Emergency Response. Continue?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.delete().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(SettingsActivity.this, "Account Deleted", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(SettingsActivity.this, "Error: Re-login required to delete account", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
