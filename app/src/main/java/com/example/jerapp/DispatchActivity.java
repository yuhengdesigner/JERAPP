package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class DispatchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatch); // Create this layout (see below)

        // Get the Bundle of extras specifically
        Bundle extras = getIntent().getExtras();

        // Automatically move to TrackingActivity after 1.5 seconds
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(DispatchActivity.this, TrackingActivity.class);
            // Pass the extras bundle if it's not null
            if (extras != null) {
                intent.putExtras(extras);
            }
            startActivity(intent);
            finish(); // Close this activity so user can't go back to the progress screen
        }, 1500);
    }
}