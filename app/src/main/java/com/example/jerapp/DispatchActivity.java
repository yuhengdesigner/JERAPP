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

        // Receive the data from the previous activity
        Intent incomingIntent = getIntent();

        // Automatically move to TrackingActivity after 1.5 seconds
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(DispatchActivity.this, TrackingActivity.class);
            intent.putExtras(incomingIntent); // Pass all data forward
            startActivity(intent);
            finish(); // Close this activity so user can't go back to the progress screen
        }, 1500);
    }
}