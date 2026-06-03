// Save as CprNeonateActivity.java
package com.example.jerapp;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CprNeonateActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpr_neonate);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}