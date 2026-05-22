package com.example.jerapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class UserHistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure you have an activity_user_history.xml with a FrameLayout container
        setContentView(R.layout.activity_user_history);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.history_container, new HistoryFragment())
                    .commit();
        }
    }
}