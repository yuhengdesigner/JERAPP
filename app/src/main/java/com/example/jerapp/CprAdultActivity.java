// Save as CprAdultActivity.java
package com.example.jerapp;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CprAdultActivity extends AppCompatActivity {

    // 1. Declare variables at the top of your class
    private com.google.android.material.card.MaterialCardView cprSwitchContainer;
    private android.view.View cprSwitchBg;
    private android.view.View cprSwitchThumb;
    private android.widget.TextView tvCprStatusText;
    private android.media.MediaPlayer cprPlayer;

    private boolean isAudioPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpr_adult);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        cprSwitchContainer = findViewById(R.id.cprSwitchContainer);
        cprSwitchBg = findViewById(R.id.cprSwitchBg);
        cprSwitchThumb = findViewById(R.id.cprSwitchThumb);
        tvCprStatusText = findViewById(R.id.tvCprStatusText);

        cprSwitchContainer.setOnClickListener(v -> {
            // Toggle the flag state
            isAudioPlaying = !isAudioPlaying;

            // Calculate travel distance: container width minus thumb width minus padding
            float travelDistance = cprSwitchContainer.getWidth() - cprSwitchThumb.getWidth() - 8; // 8 accounts for 4dp padding on each side

            if (isAudioPlaying) {
                // Smoothly slide to the right side
                cprSwitchThumb.animate()
                        .translationX(travelDistance)
                        .setDuration(250)
                        .start();

                // Transform layout styling to ON (Green) State
                cprSwitchBg.setBackgroundColor(getResources().getColor(R.color.switch_on));
                tvCprStatusText.setText("METRONOME ON");
                tvCprStatusText.setTextColor(android.graphics.Color.WHITE);

                startCprAudio();
            } else {
                // Smoothly slide back to the left start position
                cprSwitchThumb.animate()
                        .translationX(0)
                        .setDuration(250)
                        .start();

                // Transform layout styling back to OFF (Grey/Red) State
                cprSwitchBg.setBackgroundColor(getResources().getColor(R.color.switch_off));
                tvCprStatusText.setText("TAP TO START");
                tvCprStatusText.setTextColor(android.graphics.Color.parseColor("#757575"));

                stopCprAudio();
            }
        });
    }

    // 3. Audio helper methods
    private void startCprAudio() {
        try {
            if (cprPlayer == null) {
                cprPlayer = android.media.MediaPlayer.create(this, R.raw.cpr_metronome);
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