package com.example.jerapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends BaseActivity implements OnMapsSdkInitializedCallback {

    private DrawerLayout drawerLayout;
    private TextView userNameHeader, userEmailHeader;
    private boolean isGuest = false;
    private int currentNavId = -1;
    private long lastBackPressedAt = 0;
    private static boolean guestPermissionsCheckedThisSession = false;

    private final ActivityResultLauncher<String[]> permissionPicker =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean locationGranted = Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_FINE_LOCATION));
                boolean cameraGranted = Boolean.TRUE.equals(result.get(android.Manifest.permission.CAMERA));
                
                savePermissionCheckDone();
                if (!locationGranted || !cameraGranted) showEncouragementDialog();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, this);

        isGuest = getIntent().getBooleanExtra("isGuest", false) || SessionUtils.isGuest(this);
        drawerLayout = findViewById(R.id.drawer_layout);
        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        NavigationView navigationView = findViewById(R.id.nav_view);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        
        bottomNav.getMenu().findItem(R.id.nav_history).setVisible(true);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    long now = System.currentTimeMillis();
                    if (now - lastBackPressedAt < 2000) {
                        finishAffinity();
                    } else {
                        lastBackPressedAt = now;
                        Toast.makeText(MainActivity.this, "Back again to exit", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        if (navigationView.getHeaderCount() > 0) {
            View headerView = navigationView.getHeaderView(0);
            userNameHeader = headerView.findViewById(R.id.nav_header_name);
            userEmailHeader = headerView.findViewById(R.id.nav_header_email);

            headerView.setOnClickListener(v -> {
                if (isGuest) {
                    Toast.makeText(this, "Guest profile is not available.", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.nav_logout);
        if (logoutItem != null) {
            SpannableString s = new SpannableString("Logout");
            s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
            logoutItem.setTitle(s);
        }

        if (isGuest) {
            if (userNameHeader != null) userNameHeader.setText(R.string.guest_user_name);
            if (userEmailHeader != null) userEmailHeader.setText(R.string.guest_user_email);
        } else {
            loadUserProfile();
        }

        setupNavigation(navigationView, bottomNav);
        checkPermissionsOnStartup();

        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        if (getIntent().hasExtra("NAVIGATE_TO")) {
            String target = getIntent().getStringExtra("NAVIGATE_TO");
            if ("HISTORY".equals(target)) {
                bottomNav.setSelectedItemId(R.id.nav_history);
            }
        }
    }

    @Override
    public void onMapsSdkInitialized(@NonNull MapsInitializer.Renderer renderer) {
        Log.d("MAP_RENDERER", "Map initialized.");
    }

    private void loadUserProfile() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        if (userEmailHeader != null) userEmailHeader.setText(auth.getCurrentUser().getEmail());

        String uid = auth.getUid();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(uid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String name = snapshot.child("name").getValue(String.class);
                                if (userNameHeader != null && name != null) userNameHeader.setText(name);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void checkPermissionsOnStartup() {
        if (!isGuest && !getSharedPreferences("UserPrefs", MODE_PRIVATE).getBoolean("perm_check_done", false)) {
            requestEmergencyPermissions();
        } else if (isGuest && !guestPermissionsCheckedThisSession) {
            requestEmergencyPermissions();
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
                .setMessage("Location and Camera access are required for real-time tracking.")
                .setPositiveButton("Settings", (d, w) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Later", (d, w) -> {
                    savePermissionCheckDone();
                    d.dismiss();
                })
                .show();
    }

    private void savePermissionCheckDone() {
        if (isGuest) guestPermissionsCheckedThisSession = true;
        else getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putBoolean("perm_check_done", true).apply();
    }

    private void loadFragment(Fragment fragment) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) return;

        Bundle bundle = new Bundle();
        bundle.putBoolean("isGuest", isGuest);
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
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
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                if (isGuest) Toast.makeText(this, "Guest profile is not available.", Toast.LENGTH_SHORT).show();
                else startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_logout) {
                if (isGuest && getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE).getBoolean("has_active_emergency", false)) {
                    showOngoingEmergencyWarning();
                } else {
                    performLogout();
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void showOngoingEmergencyWarning() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.ongoing_emergency_warning_title))
                .setMessage(getString(R.string.ongoing_emergency_warning_message))
                .setPositiveButton("OK", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        SessionUtils.clearGuestSession(this);
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
