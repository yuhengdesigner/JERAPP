package com.example.jerapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

public class AdminMainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private String deptId;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        mAuth = FirebaseAuth.getInstance();

        // 1. Get Department ID passed from LoginActivity
        // If coming from Auto-Login, we might need to fetch it from DB instead (handled below)
        deptId = getIntent().getStringExtra("DEPT_ID");

        drawerLayout = findViewById(R.id.drawer_layout_admin);
        MaterialToolbar toolbar = findViewById(R.id.adminTopToolbar);
        NavigationView navView = findViewById(R.id.admin_nav_view);
        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_navigation);

        // Toolbar Menu Toggle
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 2. Setup Navigation Header and Fetch Admin Info
        setupHeader(navView);

        // 3. Style Logout Item
        MenuItem logoutItem = navView.getMenu().findItem(R.id.nav_admin_logout);
        if (logoutItem != null) {
            SpannableString s = new SpannableString("Logout");
            s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
            logoutItem.setTitle(s);
        }

        setupNavigation(navView, bottomNav);

        // 4. Initial Fragment Load
        if (savedInstanceState == null && deptId != null) {
            loadFragment(AdminAlertsFragment.newInstance(deptId));
        } else if (deptId == null) {
            // If deptId is null (e.g. session resume), fetch it from Firebase
            fetchDeptIdAndLoadInitialFragment();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private void setupHeader(NavigationView navView) {
        View headerView = navView.getHeaderView(0);
        TextView deptNameHeader = headerView.findViewById(R.id.admin_nav_dept_name);
        TextView usernameHeader = headerView.findViewById(R.id.admin_nav_username);

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String username = snapshot.child("username").getValue(String.class);
                        deptNameHeader.setText(name);
                        usernameHeader.setText("@" + username);
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {}
            });
        }
    }

    private void fetchDeptIdAndLoadInitialFragment() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("dept_id")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        deptId = snapshot.getValue(String.class);
                        // Add a log to debug if this is actually returning a value
                        Log.d("DEBUG_DEPT", "Fetched Dept ID: " + deptId);

                        if (deptId != null) {
                            loadFragment(AdminAlertsFragment.newInstance(deptId));
                        } else {
                            Toast.makeText(AdminMainActivity.this, "Dept ID not found for user!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("DEBUG_DEPT", "Database error: " + error.getMessage());
                    }
                });
    }

    private void setupNavigation(NavigationView navView, BottomNavigationView bottomNav) {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (deptId == null) {
                Toast.makeText(this, "Loading department data...", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (id == R.id.admin_nav_alerts) loadFragment(AdminAlertsFragment.newInstance(deptId));
            else if (id == R.id.admin_nav_processing) loadFragment(AdminProcessingFragment.newInstance(deptId));
            else if (id == R.id.admin_nav_history) loadFragment(AdminHistoryFragment.newInstance(deptId));
            return true;
        });

        navView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_admin_logout) {
                // --- FIX: UNSUBSCRIBE FROM ALERTS ON LOGOUT ---
                FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_alerts")
                        .addOnCompleteListener(task -> {
                            mAuth.signOut();
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, fragment)
                    .commit();
        }
    }

    // Add this to onCreate to ensure they are subscribed if they are already logged in
    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            FirebaseMessaging.getInstance().subscribeToTopic("admin_alerts");
        }
    }
}