package com.example.jerapp;

import android.os.Bundle;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // REQUIREMENT 6: Force light mode for consistency during presentation
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // REQUIREMENT 5: Enable screen sharing by clearing the secure flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
    }
}
