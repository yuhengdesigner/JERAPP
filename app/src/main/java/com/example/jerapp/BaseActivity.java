package com.example.jerapp;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

// This class will be the parent of all your future Activities
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // This is the magic line that lets the app draw behind status/nav bars
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // This makes the navigation bar background transparent so the BottomNav color shows
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
    }
}