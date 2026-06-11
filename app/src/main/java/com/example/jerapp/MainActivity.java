package com.example.jerapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.ImageView;
import android.util.Log;
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
import android.view.View;
import android.content.SharedPreferences;
import com.google.android.material.card.MaterialCardView;
import android.widget.Toast;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;
import java.util.Locale;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseActivity implements OnMapsSdkInitializedCallback {

    private DrawerLayout drawerLayout;
    private TextView userNameHeader, userEmailHeader;
    private ImageView userProfileImage;
    private boolean isGuest = false;
    private int currentNavId = -1;
    private com.google.android.material.card.MaterialCardView cardOngoingEmergency;
    private TextView tvOngoingDeptName, tvDashboardCountdown;
    private android.os.CountDownTimer dashboardTimer;
    private static boolean guestPermissionsCheckedThisSession = false;
    private long lastBackPressedAt = 0;

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

        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, this);

        isGuest = getIntent().getBooleanExtra("isGuest", false) || SessionUtils.isGuest(this);
        drawerLayout = findViewById(R.id.drawer_layout);
        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        NavigationView navigationView = findViewById(R.id.nav_view);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        
        // REQUIREMENT 4: Allow Guest history navigation
        if (isGuest) {
            bottomNav.getMenu().findItem(R.id.nav_history).setVisible(true);
        }

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
            userProfileImage = headerView.findViewById(R.id.nav_header_image);

            headerView.setOnClickListener(v -> {
                if (isGuest) {
                    Toast.makeText(this, "Guest profile is not available. Please register or log in.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
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

        if (getIntent().hasExtra("NAVIGATE_TO")) {
            String target = getIntent().getStringExtra("NAVIGATE_TO");
            if ("HISTORY".equals(target)) {
                bottomNav.setSelectedItemId(R.id.nav_history);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndRenderOngoingSession();
    }

    private void checkAndRenderOngoingSession() {
        cardOngoingEmergency = findViewById(R.id.cardOngoingEmergency);
        tvOngoingDeptName = findViewById(R.id.tvOngoingDeptName);
        tvDashboardCountdown = findViewById(R.id.tvDashboardCountdown);

        if (cardOngoingEmergency == null || tvOngoingDeptName == null || tvDashboardCountdown == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("OngoingEmergencyPrefs", MODE_PRIVATE);
        boolean hasActive = prefs.getBoolean("has_active_emergency", false);
        String activeAlertKey = prefs.getString("active_alert_key", prefs.getString("alert_key", null));
        String ownerUid = prefs.getString("owner_uid", null);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        if (!isGuest && ownerUid != null && user != null && !ownerUid.equals(user.getUid())) {
            cardOngoingEmergency.setVisibility(View.GONE);
            return;
        }

        if (!hasActive || activeAlertKey == null) {
            cardOngoingEmergency.setVisibility(View.GONE);
            if (dashboardTimer != null) {
                dashboardTimer.cancel();
            }
            return;
        }

        String name = prefs.getString("dept_name", "Emergency Department");
        tvOngoingDeptName.setText("Responding: " + name);
        cardOngoingEmergency.setVisibility(View.VISIBLE);

        if (dashboardTimer != null) {
            dashboardTimer.cancel();
        }

        long endTimeMs = prefs.getLong("timer_end_time_ms", 0);
        if (endTimeMs > System.currentTimeMillis()) {
            long remainingMs = endTimeMs - System.currentTimeMillis();
            dashboardTimer = new android.os.CountDownTimer(remainingMs, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long mins = (millisUntilFinished / 1000) / 60;
                    long secs = (millisUntilFinished / 1000) % 60;
                    tvDashboardCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d Left", mins, secs));
                }

                @Override
                public void onFinish() {
                    tvDashboardCountdown.setText("Responders Due!");
                }
            }.start();
        } else {
            tvDashboardCountdown.setText("Tracking Active");
        }

        cardOngoingEmergency.setOnClickListener(v -> {
            Intent resumeIntent = new Intent(MainActivity.this, TrackingActivity.class);
            resumeIntent.putExtra("alert_key", prefs.getString("active_alert_key", prefs.getString("alert_key", "")));
            resumeIntent.putExtra("dept_name", prefs.getString("dept_name", ""));
            resumeIntent.putExtra("dept_phone", prefs.getString("dept_phone", ""));
            resumeIntent.putExtra("dept_id", prefs.getString("dept_id", ""));

            double lat = Double.longBitsToDouble(prefs.getLong("dept_lat_bits", 0));
            double lng = Double.longBitsToDouble(prefs.getLong("dept_lng_bits", 0));
            resumeIntent.putExtra("dept_lat", lat);
            resumeIntent.putExtra("dept_lng", lng);
            resumeIntent.putExtra("isGuestFlow", prefs.getBoolean("isGuestFlow", false));

            startActivity(resumeIntent);
        });
    }

    @Override
    public void onMapsSdkInitialized(@NonNull MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d("MAP_RENDERER", "The latest 3D renderer is being used.");
                break;
            case LEGACY:
                Log.d("MAP_RENDERER", "The legacy renderer is being used.");
                break;
        }
    }

    private void loadUserProfile() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getUid();

        if (userEmailHeader != null) {
            userEmailHeader.setText(auth.getCurrentUser().getEmail());
        }

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
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putBoolean("isGuest", isGuest);
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        findViewById(R.id.fragment_container).post(this::checkAndRenderOngoingSession);
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
                if (isGuest) {
                    Toast.makeText(this, "Guest profile is not available. Please register or log in.", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                }
            }
            else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
            else if (id == R.id.nav_logout) {
                if (isGuest && hasOngoingEmergency()) {
                    showOngoingEmergencyWarning(getString(R.string.ongoing_emergency_warning_message));
                } else {
                    performLogout();
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
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

    private void performLogout() {
        if (dashboardTimer != null) {
            dashboardTimer.cancel();
        }

        FirebaseAuth.getInstance().signOut();
        SessionUtils.clearGuestSession(this);
        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
