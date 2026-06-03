// Save as CprAdultActivity.java
package com.example.jerapp;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CprAdultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpr_adult);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}