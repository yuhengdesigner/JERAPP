package com.example.jerapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvAddress, tvLatLng;
    private boolean isGuestAccount = false;
    private static boolean guestDismissedTutorialThisSession = false;
    private final android.os.Handler tutorialHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable tutorialRunnable;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableMyLocation();
                } else {
                    showPermissionEncouragement();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isGuestAccount = getArguments().getBoolean("isGuest", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location, container, false);
        tvAddress = view.findViewById(R.id.tv_address);
        tvLatLng = view.findViewById(R.id.tv_lat_lng);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setBuildingsEnabled(true);

        setupFabListeners();
        checkAndRequestPermission();

        if (isGuestAccount && !guestDismissedTutorialThisSession) {
            show3DTutorial();
        } else if (!isGuestAccount && !isTutorialDone()) {
            show3DTutorial();
        }
    }

    private void setupFabListeners() {
        View view = getView();
        if (view == null) return;

        view.findViewById(R.id.fab_layer_toggle).setOnClickListener(v -> {
            if (mMap != null) {
                int nextType = (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) ?
                        GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL;
                mMap.setMapType(nextType);
            }
        });

        // When user clicks the FAB, we check permissions and trigger 2D tracking
        view.findViewById(R.id.fab_my_location).setOnClickListener(v -> checkAndRequestPermission());
    }

    private void checkAndRequestPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableMyLocation() {
        if (mMap != null && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null && isAdded()) {
                        // Pass 'false' to force 2D (0 degree tilt)
                        updateMapUI(location.getLatitude(), location.getLongitude(), false);
                    } else {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) updateMapUI(lastLoc.getLatitude(), lastLoc.getLongitude(), false);
                        });
                    }
                });
    }

    // Updated to accept a boolean for 3D or 2D
    private void updateMapUI(double lat, double lng, boolean use3D) {
        if (mMap == null) return;

        LatLng current = new LatLng(lat, lng);

        // If use3D is false (My Location tapped), tilt is 0. If true, tilt is 60.
        float targetTilt = use3D ? 60f : 0f;

        CameraPosition cp = new CameraPosition.Builder()
                .target(current)
                .zoom(18f)
                .tilt(targetTilt)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));

        tvLatLng.setText(String.format(Locale.getDefault(), "Latitude: %.5f, Longitude: %.5f", lat, lng));
        updateAddressFromLocation(lat, lng);
    }

    private void showPermissionEncouragement() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Location Access Required")
                .setMessage("To show your position on the map, please grant location access. If you denied it permanently, you can enable it in Settings.")
                .setPositiveButton("Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Not Now", null)
                .show();
    }

    private void updateAddressFromLocation(double lat, double lng) {
        if (!Geocoder.isPresent()) return;
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                tvAddress.setText(addresses.get(0).getAddressLine(0));
            }
        } catch (IOException e) { tvAddress.setText("Address currently unavailable"); }
    }

    private boolean isTutorialDone() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        return sharedPref.getBoolean("map_tutorial_done", false);
    }

    private void show3DTutorial() {
        View rootView = getView();
        if (rootView == null) return;

        tutorialRunnable = () -> {
            if (!isAdded() || getContext() == null) return;

            Snackbar snackbar = Snackbar.make(rootView, "Tip: Swipe up with two fingers for 3D view", Snackbar.LENGTH_INDEFINITE);
            View anchor = rootView.findViewById(R.id.location_card_container);
            if (anchor != null) snackbar.setAnchorView(anchor);

            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundResource(R.drawable.bg_snackbar_rounded);

            float d = getResources().getDisplayMetrics().density;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
            params.setMargins((int)(16*d), 0, (int)(16*d), (int)(10*d));
            snackbarView.setLayoutParams(params); test

            snackbar.setAction("Got it", v -> {
                if (isGuestAccount) guestDismissedTutorialThisSession = true;
                else {
                    SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    sharedPref.edit().putBoolean("map_tutorial_done", true).apply();
                }
                snackbar.dismiss();
            });

            snackbar.setActionTextColor(getResources().getColor(android.R.color.holo_blue_light));
            snackbar.show();
        };
        tutorialHandler.postDelayed(tutorialRunnable, 1500);
    }

    @Override
    public void onDestroyView() {
        if (tutorialHandler != null && tutorialRunnable != null) {
            tutorialHandler.removeCallbacks(tutorialRunnable);
        }
        mMap = null;
        super.onDestroyView();
    }
}