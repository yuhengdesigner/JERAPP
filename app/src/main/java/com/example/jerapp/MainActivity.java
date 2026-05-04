package com.example.jerapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView userNameHeader, userEmailHeader;
    private ImageView userProfileImage;
    private boolean isGuest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isGuest = getIntent().getBooleanExtra("isGuest", false);

        // 1. Initialize Views
        drawerLayout = findViewById(R.id.drawer_layout);
        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        NavigationView navigationView = findViewById(R.id.nav_view);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 2. Toolbar Menu Toggle
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 3. Back Press Logic
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    onBackPressed();
                    setEnabled(true);
                }
            }
        });

        // 4. Header Setup
        if (navigationView.getHeaderCount() > 0) {
            userNameHeader = navigationView.getHeaderView(0).findViewById(R.id.nav_header_name);
            userEmailHeader = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);
            userProfileImage = navigationView.getHeaderView(0).findViewById(R.id.nav_header_image);
        }

        // 5. Logout Color logic
        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.nav_logout);
        if (logoutItem != null) {
            SpannableString s = new SpannableString("Logout");
            s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
            logoutItem.setTitle(s);
        }

        if (isGuest) {
            if (userNameHeader != null) userNameHeader.setText("Guest");
        } else {
            loadUserProfile();
        }

        setupNavigation(navigationView, bottomNav);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void loadUserProfile() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getUid();
        // 1. Always set the email as long as we are logged in
        if (auth.getCurrentUser() != null && userEmailHeader != null) {
            userEmailHeader.setText(auth.getCurrentUser().getEmail());
        }

        // 2. Database call for the name
        if (uid != null && userNameHeader != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(uid)
                    .child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                userNameHeader.setText(snapshot.getValue(String.class));
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle potential database errors
                        }
                    });
        }
    }

    private void setupNavigation(NavigationView navView, BottomNavigationView bottomNav) {
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) loadFragment(new DashboardFragment());
            else if (id == R.id.nav_location) loadFragment(new LocationFragment());
            else if (id == R.id.nav_offline) loadFragment(new OfflineFragment());
            else if (id == R.id.nav_history) loadFragment(new HistoryFragment());
            return true;
        });
    }
}