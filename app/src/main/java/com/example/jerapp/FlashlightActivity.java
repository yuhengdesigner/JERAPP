package com.example.jerapp;

import android.animation.LayoutTransition;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;

public class FlashlightActivity extends AppCompatActivity {

    private String cameraId;
    private CameraManager cameraManager;
    private MaterialSwitch switchFlashlight;

    private Handler sosHandler = new Handler(Looper.getMainLooper());
    private boolean isSosActive = false;

    private final int DOT = 200;
    private final int DASH = 600;
    private final int GAP = 200;
    private final int WORD_GAP = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashlight);

        ViewGroup rootLayout = (ViewGroup) findViewById(android.view.Window.ID_ANDROID_CONTENT);
        if (rootLayout != null && rootLayout.getLayoutTransition() != null) {
            rootLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

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
                    playSignal(DOT); playSignal(DOT); playSignal(DOT);
                    playSignal(DASH); playSignal(DASH); playSignal(DASH);
                    playSignal(DOT); playSignal(DOT); playSignal(DOT);

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
            switchFlashlight.setChecked(false);
        }
    }
}