package com.example.jerapp;

import android.os.Bundle;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

// This class will be the parent of all your future Activities
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // REQUIREMENT 6: Force light mode regardless of system settings
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // REQUIREMENT 5: Disable screen share darkness (Clear FLAG_SECURE)
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        
        // This is the magic line that lets the app draw behind status/nav bars
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // This makes the navigation bar background transparent so the BottomNav color shows
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
    }
}
