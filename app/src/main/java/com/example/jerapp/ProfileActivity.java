package com.example.jerapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private static final String DB_URL = "https://jerapp-2026-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen UI configuration
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_profile);

        // 1. Initialize Visuals (Labels and Icons)
        setupInitialCards();

        // 2. Setup Buttons
        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());
        setupClickListeners();

        // 3. Load Dynamic Data
        loadUserData();
    }

    private void setupInitialCards() {
        // Passing null ensures the updateCard logic displays "Change [Label]" immediately
        updateCard(R.id.cardName, "Full Name", null, R.drawable.ic_user);
        updateCard(R.id.cardEmail, "Email Address", null, R.drawable.ic_email);
        updateCard(R.id.cardPhone, "Phone Number", null, R.drawable.ic_phone);
        updateCard(R.id.cardAddress, "Address", null, R.drawable.ic_location);
        updateCard(R.id.cardBirthDate, "Date of Birth", null, R.drawable.ic_calendar);
        updateCard(R.id.cardGender, "Gender", null, R.drawable.ic_gender);
        updateCard(R.id.cardPassword, "Password", null, R.drawable.ic_lock);
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Safety check
        if (user == null) return;

        // Handle Guest User immediately without calling Database
        if (user.isAnonymous()) {
            updateGuestUI();
            return;
        }

        // Registered user: Load from Database
        String uid = user.getUid();
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("Users").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            updateUI(snapshot);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateGuestUI() {
        TextView tvHeaderName = findViewById(R.id.profileName);
        TextView tvHeaderEmail = findViewById(R.id.profileEmail);
        if (tvHeaderName != null) tvHeaderName.setText("Guest User");
        if (tvHeaderEmail != null) tvHeaderEmail.setText("No account registered");

        com.google.android.material.button.MaterialButton btnDelete = findViewById(R.id.btnDeleteAccount);
        if (btnDelete != null) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setText("Create an account");
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));

            // This is the ONLY place the click listener for guests should be defined
            btnDelete.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, RegisterActivity.class));
            });
        }
    }

    private void updateUI(DataSnapshot s) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = s.child("name").getValue(String.class);
        String phone = s.child("phone").getValue(String.class);
        String address = s.child("address").getValue(String.class);
        String dob = s.child("birthDate").getValue(String.class);
        String gender = s.child("gender").getValue(String.class);

        // Header Updates
        TextView tvHeaderName = findViewById(R.id.profileName);
        TextView tvHeaderEmail = findViewById(R.id.profileEmail);

        tvHeaderName.setText(name != null ? name : "User Name");

        String email = (user != null) ? user.getEmail() : "";
        if (tvHeaderEmail != null) tvHeaderEmail.setText(email);

        // Card Updates
        updateCard(R.id.cardName, "Name", name, R.drawable.ic_user);
        updateCard(R.id.cardEmail, "Email", email, R.drawable.ic_email);
        updateCard(R.id.cardPhone, "Phone", phone, R.drawable.ic_phone);
        updateCard(R.id.cardAddress, "Address", address, R.drawable.ic_location);
        updateCard(R.id.cardBirthDate, "Birth Date", dob, R.drawable.ic_calendar);
        updateCard(R.id.cardGender, "Gender", gender, R.drawable.ic_gender);

        // Show Delete button for Registered Users
        View btnDelete = findViewById(R.id.btnDeleteAccount);
        if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);

        com.google.android.material.button.MaterialButton btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        if (btnDeleteAccount != null) {
            btnDeleteAccount.setVisibility(View.VISIBLE);
            btnDeleteAccount.setText("Delete Account");
            // Set background to Red
            btnDeleteAccount.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));

            btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmation());
        }
    }

    private void updateCard(int cardId, String label, String value, int iconRes) {
        View card = findViewById(cardId);
        if (card != null) {
            TextView tvLabel = card.findViewById(R.id.tvLabel);
            TextView tvValue = card.findViewById(R.id.tvValue);
            ImageView ivIcon = card.findViewById(R.id.ivIcon);

            if (tvLabel != null) tvLabel.setVisibility(View.GONE);
            if (ivIcon != null) ivIcon.setImageResource(iconRes);

            if (tvValue != null) {
                // This is the part that handles your request:
                if (value == null || value.isEmpty()) {
                    tvValue.setText("Change " + label);
                    tvValue.setAlpha(0.5f);
                } else {
                    tvValue.setText("Change " + value);
                    tvValue.setAlpha(1.0f);
                }
            }
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.cardName).setOnClickListener(v -> openEditPage("name"));
        findViewById(R.id.cardEmail).setOnClickListener(v -> openEditPage("email"));
        findViewById(R.id.cardPhone).setOnClickListener(v -> openEditPage("phone"));
        findViewById(R.id.cardAddress).setOnClickListener(v -> openEditPage("address"));
        findViewById(R.id.cardBirthDate).setOnClickListener(v -> openEditPage("birthDate"));
        findViewById(R.id.cardGender).setOnClickListener(v -> openEditPage("gender"));
        findViewById(R.id.cardPassword).setOnClickListener(v -> openEditPage("password"));

        findViewById(R.id.btnProfileLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure? This will permanently delete your profile and data. This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = FirebaseAuth.getInstance().getUid();

        if (user == null || uid == null) return;

        // 1. Delete from Database
        FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 2. Delete from Auth
                        user.delete().addOnCompleteListener(authTask -> {
                            if (authTask.isSuccessful()) {
                                Toast.makeText(this, "Account Deleted", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                String error = authTask.getException() != null ? authTask.getException().getMessage() : "Re-login required";
                                Toast.makeText(this, "Security: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
    }

    private void openEditPage(String fieldType) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.isAnonymous()) {
            // If guest tries to edit, show a snackbar or toast
            new AlertDialog.Builder(this)
                    .setTitle("Feature Locked")
                    .setMessage("Please create an account to customize your profile details.")
                    .setPositiveButton("Register Now", (dialog, which) -> {
                        startActivity(new Intent(ProfileActivity.this, RegisterActivity.class));
                    })
                    .setNegativeButton("Later", null)
                    .show();
        } else {
            // Proceed for registered users
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("edit_type", fieldType);
            startActivity(intent);
        }
    }
}