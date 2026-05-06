package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.widget.Toast;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());

        loadUserData();
        setupClickListeners();
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            updateUI(snapshot);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void updateUI(DataSnapshot s) {
        // Find individual components in the included cards
        updateCard(R.id.cardName, "Full Name", s.child("name").getValue(String.class));
        updateCard(R.id.cardPhone, "Phone Number", s.child("phone").getValue(String.class));
        updateCard(R.id.cardAddress, "Address", s.child("address").getValue(String.class));
        // Update header
        ((TextView)findViewById(R.id.profileName)).setText(s.child("name").getValue(String.class));
        ((TextView)findViewById(R.id.profileEmail)).setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
    }

    private void updateCard(int cardId, String label, String value) {
        View card = findViewById(cardId);
        ((TextView)card.findViewById(R.id.itemLabel)).setText(label);
        ((TextView)card.findViewById(R.id.itemValue)).setText(value != null ? value : "Not set");
    }

    private void setupClickListeners() {
        findViewById(R.id.cardName).setOnClickListener(v -> openEditPage("name"));
        findViewById(R.id.cardPhone).setOnClickListener(v -> openEditPage("phone"));
        findViewById(R.id.cardAddress).setOnClickListener(v -> openEditPage("address"));
        findViewById(R.id.cardBirthDate).setOnClickListener(v -> openEditPage("birthDate"));
        findViewById(R.id.cardGender).setOnClickListener(v -> openEditPage("gender"));

        // Special case for Password
        findViewById(R.id.cardPassword).setOnClickListener(v -> {
            // You might want a dedicated "Change Password" dialog here
            Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().sendPasswordResetEmail(
                    FirebaseAuth.getInstance().getCurrentUser().getEmail()
            );
        });

        // Logout button at the bottom
        findViewById(R.id.btnProfileLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void openEditPage(String fieldType) {
        Intent intent = new Intent(this, EditProfileActivity.class);
        intent.putExtra("edit_type", fieldType);
        startActivity(intent);
    }
}