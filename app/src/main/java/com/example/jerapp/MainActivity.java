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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class MainActivity extends BaseActivity {

    private DrawerLayout drawerLayout;
    private TextView userNameHeader, userEmailHeader;
    private ImageView userProfileImage;
    private boolean isGuest = false;
    private int currentNavId = -1;

    private static boolean guestPermissionsCheckedThisSession = false;

    private final ActivityResultLauncher<String[]> permissionPicker =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean locationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                boolean cameraGranted = result.getOrDefault(android.Manifest.permission.CAMERA, false);
                savePermissionCheckDone();
                if (!locationGranted || !cameraGranted) showEncouragementDialog();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isGuest = getIntent().getBooleanExtra("isGuest", false);

        drawerLayout = findViewById(R.id.drawer_layout);
        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        NavigationView navigationView = findViewById(R.id.nav_view);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

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

        // Initialize Header Views
        if (navigationView.getHeaderCount() > 0) {
            userNameHeader = navigationView.getHeaderView(0).findViewById(R.id.nav_header_name);
            userEmailHeader = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);
            userProfileImage = navigationView.getHeaderView(0).findViewById(R.id.nav_header_image);
        }

        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.nav_logout);
        if (logoutItem != null) {
            SpannableString s = new SpannableString("Logout");
            s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
            logoutItem.setTitle(s);
        }

        // Logic to load info
        if (isGuest) {
            if (userNameHeader != null) userNameHeader.setText("Guest User");
            if (userEmailHeader != null) userEmailHeader.setText("guest@example.com");
        } else {
            loadUserProfile();
        }

        setupNavigation(navigationView, bottomNav);
        checkPermissionsOnStartup();

        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
    }

    private void loadUserProfile() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getUid();

        // 1. Set Email immediately from Auth session
        if (userEmailHeader != null) {
            userEmailHeader.setText(auth.getCurrentUser().getEmail());
        }

        // 2. Fetch Name from Database with a listener that updates automatically
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(uid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String name = snapshot.child("name").getValue(String.class);
                                if (userNameHeader != null && name != null) {
                                    userNameHeader.setText(name);
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void checkPermissionsOnStartup() {
        if (isGuest) {
            if (!guestPermissionsCheckedThisSession) requestEmergencyPermissions();
        } else {
            if (!isPermanentPermissionCheckDone()) requestEmergencyPermissions();
        }
    }

    private void requestEmergencyPermissions() {
        permissionPicker.launch(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.CAMERA
        });
    }

    private void showEncouragementDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Needed")
                .setMessage("Location and Camera access are required for real-time tracking. Please enable them in App Settings.")
                .setPositiveButton("Open Settings", (d, w) -> {
                    // This takes the user to the actual system settings for your app
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    android.net.Uri uri = android.net.Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Maybe Later", (d, w) -> {
                    savePermissionCheckDone();
                    d.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void savePermissionCheckDone() {
        if (isGuest) guestPermissionsCheckedThisSession = true;
        else getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putBoolean("perm_check_done", true).apply();
    }

    private boolean isPermanentPermissionCheckDone() {
        return getSharedPreferences("UserPrefs", MODE_PRIVATE).getBoolean("perm_check_done", false);
    }

    private void loadFragment(Fragment fragment) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("isGuest", isGuest);
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void setupNavigation(NavigationView navView, BottomNavigationView bottomNav) {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentNavId) return false;
            currentNavId = id;
            if (id == R.id.nav_home) loadFragment(new DashboardFragment());
            else if (id == R.id.nav_location) loadFragment(new LocationFragment());
            else if (id == R.id.nav_offline) loadFragment(new OfflineFragment());
            else if (id == R.id.nav_history) loadFragment(new HistoryFragment());
            return true;
        });

        navView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
}