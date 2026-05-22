package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class ConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        Intent incomingIntent = getIntent();

        // 1. Handle the Header Back Button
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 2. Handle the Cancel Button
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        // 3. Handle the Confirm Button
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            // Move to Dispatch
            Intent intent = new Intent(ConfirmationActivity.this, DispatchActivity.class);
            intent.putExtras(incomingIntent); // Pass the data forward
            startActivity(intent);
            finish(); // Finish this activity so the user doesn't return here
        });
    }
}