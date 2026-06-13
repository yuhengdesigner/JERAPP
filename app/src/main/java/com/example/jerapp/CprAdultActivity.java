package com.example.jerapp;

import android.animation.LayoutTransition;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class CprAdultActivity extends AppCompatActivity {

    private MaterialCardView cprSwitchContainer;
    private View cprSwitchBg;
    private View cprSwitchThumb;
    private TextView tvCprStatusText;
    private MediaPlayer cprPlayer;
    private boolean isAudioPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpr_adult);

        ViewGroup rootLayout = (ViewGroup) findViewById(android.view.Window.ID_ANDROID_CONTENT);
        if (rootLayout != null && rootLayout.getLayoutTransition() != null) {
            rootLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupDropdown(
                findViewById(R.id.headerWhat),
                findViewById(R.id.contentWhat),
                findViewById(R.id.previewWhat),
                findViewById(R.id.arrowWhat)
        );

        setupDropdown(
                findViewById(R.id.headerWhy),
                findViewById(R.id.contentWhy),
                findViewById(R.id.previewWhy),
                findViewById(R.id.arrowWhy)
        );

        setupDropdown(
                findViewById(R.id.headerWho),
                findViewById(R.id.contentWho),
                findViewById(R.id.previewWho),
                findViewById(R.id.arrowWho)
        );

        setupDropdown(
                findViewById(R.id.headerWhen),
                findViewById(R.id.contentWhen),
                findViewById(R.id.previewWhen),
                findViewById(R.id.arrowWhen)
        );

        setupDropdown(
                findViewById(R.id.headerWhere),
                findViewById(R.id.contentWhere),
                findViewById(R.id.previewWhere),
                findViewById(R.id.arrowWhere)
        );

        setupDropdown(
                findViewById(R.id.headerHow),
                findViewById(R.id.contentHow),
                findViewById(R.id.previewHow),
                findViewById(R.id.arrowHow)
        );

        cprSwitchContainer = findViewById(R.id.cprSwitchContainer);
        cprSwitchBg = findViewById(R.id.cprSwitchBg);
        cprSwitchThumb = findViewById(R.id.cprSwitchThumb);
        tvCprStatusText = findViewById(R.id.tvCprStatusText);

        cprSwitchContainer.setOnClickListener(v -> {
            isAudioPlaying = !isAudioPlaying;
            float travelDistance = cprSwitchContainer.getWidth() - cprSwitchThumb.getWidth() - 8;

            if (isAudioPlaying) {
                cprSwitchThumb.animate()
                        .translationX(travelDistance)
                        .setDuration(250)
                        .start();

                cprSwitchBg.setBackgroundColor(getResources().getColor(R.color.switch_on));
                tvCprStatusText.setText("METRONOME ON");
                tvCprStatusText.setTextColor(Color.WHITE);

                startCprAudio();
            } else {
                cprSwitchThumb.animate()
                        .translationX(0)
                        .setDuration(250)
                        .start();

                cprSwitchBg.setBackgroundColor(getResources().getColor(R.color.switch_off));
                tvCprStatusText.setText("TAP TO START");
                tvCprStatusText.setTextColor(Color.parseColor("#757575"));

                stopCprAudio();
            }
        });
    }

    private void setupDropdown(View header, LinearLayout content, TextView preview, ImageView arrow) {
        if (header == null || content == null || preview == null || arrow == null) return;

        header.setOnClickListener(v -> {
            if (content.getVisibility() == View.VISIBLE) {
                content.setVisibility(View.GONE);
                preview.setVisibility(View.VISIBLE);
                arrow.animate().rotation(0).setDuration(200).start();
            } else {
                content.setVisibility(View.VISIBLE);
                preview.setVisibility(View.GONE);
                arrow.animate().rotation(180).setDuration(200).start();
            }
        });
    }

    private void startCprAudio() {
        try {
            if (cprPlayer == null) {
                cprPlayer = MediaPlayer.create(this, R.raw.cpr_metronome);
                cprPlayer.setLooping(true);
            }
            cprPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopCprAudio() {
        if (cprPlayer != null) {
            if (cprPlayer.isPlaying()) {
                cprPlayer.stop();
            }
            cprPlayer.release();
            cprPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCprAudio();
    }
}