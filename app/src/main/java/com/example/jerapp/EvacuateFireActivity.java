package com.example.jerapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class EvacuateFireActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_guide_evacuate_fire);

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        setupDropdownCard(R.id.headerWhat, R.id.contentWhat, R.id.previewWhat, R.id.arrowWhat);
        setupDropdownCard(R.id.headerWhy, R.id.contentWhy, R.id.previewWhy, R.id.arrowWhy);
        setupDropdownCard(R.id.headerWho, R.id.contentWho, R.id.previewWho, R.id.arrowWho);
        setupDropdownCard(R.id.headerWhen, R.id.contentWhen, R.id.previewWhen, R.id.arrowWhen);
        setupDropdownCard(R.id.headerWhere, R.id.contentWhere, R.id.previewWhere, R.id.arrowWhere);
        setupDropdownCard(R.id.headerHow, R.id.contentHow, R.id.previewHow, R.id.arrowHow);
    }

    private void setupDropdownCard(int headerId, int contentId, int previewId, int arrowId) {
        View header = findViewById(headerId);
        final LinearLayout content = findViewById(contentId);
        final View preview = findViewById(previewId);
        final ImageView arrow = findViewById(arrowId);

        if (header == null || content == null || arrow == null) return;

        content.setVisibility(View.GONE);
        if (preview != null) preview.setVisibility(View.VISIBLE);
        arrow.setRotation(0);

        header.setOnClickListener(v -> {
            if (content.getVisibility() == View.VISIBLE) {
                content.setVisibility(View.GONE);
                if (preview != null) preview.setVisibility(View.VISIBLE);
                arrow.animate().rotation(0).setDuration(200).start();
            } else {
                content.setVisibility(View.VISIBLE);
                if (preview != null) preview.setVisibility(View.GONE);
                arrow.animate().rotation(180).setDuration(200).start();
            }
        });
    }
}
