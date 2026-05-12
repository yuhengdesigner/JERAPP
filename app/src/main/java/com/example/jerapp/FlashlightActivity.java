package com.example.jerapp;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;

public class FlashlightActivity extends AppCompatActivity {

    private String cameraId;
    private CameraManager cameraManager;
    private MaterialSwitch switchFlashlight;

    private Handler sosHandler = new Handler(Looper.getMainLooper());
    private boolean isSosActive = false;

    // Morse Code Timings (in milliseconds)
    private final int DOT = 200;    // Short flash
    private final int DASH = 600;   // Long flash
    private final int GAP = 200;    // Gap between flashes
    private final int WORD_GAP = 1000; // Gap before repeating SOS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashlight);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        switchFlashlight = findViewById(R.id.switchFlashlight);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        switchFlashlight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startSos();
            } else {
                stopSos();
            }
        });
    }

    private void startSos() {
        isSosActive = true;
        sosHandler.post(sosRunnable);
    }

    private void stopSos() {
        isSosActive = false;
        sosHandler.removeCallbacks(sosRunnable);
        try {
            cameraManager.setTorchMode(cameraId, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Runnable sosRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isSosActive) return;

            new Thread(() -> {
                try {
                    // S (... )
                    playSignal(DOT); playSignal(DOT); playSignal(DOT);
                    // O (---)
                    playSignal(DASH); playSignal(DASH); playSignal(DASH);
                    // S (... )
                    playSignal(DOT); playSignal(DOT); playSignal(DOT);

                    // Wait before repeating the whole SOS
                    Thread.sleep(WORD_GAP);

                    if (isSosActive) sosHandler.post(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    };

    private void playSignal(int duration) throws Exception {
        if (!isSosActive) return;
        cameraManager.setTorchMode(cameraId, true);
        Thread.sleep(duration);
        cameraManager.setTorchMode(cameraId, false);
        Thread.sleep(GAP);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (switchFlashlight.isChecked()) {
            switchFlashlight.setChecked(false); // This triggers stopSos()
        }
    }
}