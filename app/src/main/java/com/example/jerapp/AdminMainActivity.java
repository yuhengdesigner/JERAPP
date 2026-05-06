package com.example.jerapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class AdminMainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private String departmentName;
    private String adminUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // 1. Get data from Intent passed from LoginActivity
        departmentName = getIntent().getStringExtra("admin_dept");
        adminUsername = getIntent().getStringExtra("admin_user");

        drawerLayout = findViewById(R.id.drawer_layout_admin);
        MaterialToolbar toolbar = findViewById(R.id.adminTopToolbar);
        NavigationView navView = findViewById(R.id.admin_nav_view);
        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_navigation);

        // Toolbar Menu Toggle
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 2. Setup Navigation Header (Username/Dept)
        View headerView = navView.getHeaderView(0);
        TextView deptNameHeader = headerView.findViewById(R.id.admin_nav_dept_name);
        TextView usernameHeader = headerView.findViewById(R.id.admin_nav_username);

        deptNameHeader.setText(departmentName);
        usernameHeader.setText("Admin: " + adminUsername);

        // 3. Style Logout Item
        MenuItem logoutItem = navView.getMenu().findItem(R.id.nav_admin_logout);
        if (logoutItem != null) {
            SpannableString s = new SpannableString("Logout");
            s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
            logoutItem.setTitle(s);
        }

        setupNavigation(navView, bottomNav);

        // 4. Initial Fragment Load - Pass the departmentName!
        if (savedInstanceState == null) {
            loadFragment(AdminAlertsFragment.newInstance(departmentName));
        }
    }

    private void setupNavigation(NavigationView navView, BottomNavigationView bottomNav) {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.admin_nav_alerts) {
                // IMPORTANT: Always use newInstance to pass the filter!
                loadFragment(AdminAlertsFragment.newInstance(departmentName));
            } else if (id == R.id.admin_nav_history) {
                // Pass departmentName to history too so they only see their own archives
                loadFragment(AdminHistoryFragment.newInstance(departmentName));
            }
            return true;
        });

        navView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_admin_logout) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
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
}