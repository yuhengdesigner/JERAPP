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

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import android.location.Location;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class LocationFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvAddress, tvLatLng;
    private boolean isGuestAccount = false;
    private static boolean guestDismissedTutorialThisSession = false;
    private final android.os.Handler tutorialHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable tutorialRunnable;
    private Location lastKnownLocation;

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

        // --- Original UI Features ---
        mMap.getUiSettings().setCompassEnabled(true);       // Shows compass when map is rotated
        mMap.getUiSettings().setMapToolbarEnabled(true);   // Shows "Open in Maps/Directions" when markers clicked
        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Standard "Center on me" button

        // Allow users to naturally tilt and rotate
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);

        mMap.setBuildingsEnabled(true);

        // Coordinates for UTM (or your preferred starting point)
        LatLng utmJohor = new LatLng(1.5591, 103.6375);

        CameraPosition initial3DTopView = new CameraPosition.Builder()
                .target(utmJohor)
                .zoom(18.5f) // Buildings only appear in 3D at zoom > 17
                .tilt(45f)   // A 10 degree tilt looks "top down" but enables 3D rendering
                .build();

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(initial3DTopView));

        setupFabListeners();
        checkAndRequestPermission();

        if (isGuestAccount && !guestDismissedTutorialThisSession) {
            show3DTutorial();
        } else if (!isGuestAccount && !isTutorialDone()) {
            show3DTutorial();
        }

        loadDepartments(mMap);

        mMap.setOnMarkerClickListener(marker -> {
            Department dept = (Department) marker.getTag();
            if (dept == null) return false;

            // FIX: Add a safe check here
            if (lastKnownLocation != null) {
                Location deptLoc = new Location("");
                deptLoc.setLatitude(dept.latitude);
                deptLoc.setLongitude(dept.longitude);

                float distance = lastKnownLocation.distanceTo(deptLoc) / 1000;
                marker.setSnippet(String.format("Distance: %.2f km", distance));
            } else {
                // Handle case where location isn't ready yet
                marker.setSnippet("Distance: Location not available");
            }

            marker.showInfoWindow();
            return true; // Return true to consume the click
        });
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
                        this.lastKnownLocation = location;
                        updateMapUI(location.getLatitude(), location.getLongitude(), true);
                    } else {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) updateMapUI(lastLoc.getLatitude(), lastLoc.getLongitude(), true);
                        });
                    }
                });
    }

    private void updateMapUI(double lat, double lng, boolean use3D) {
        if (mMap == null) return;

        LatLng current = new LatLng(lat, lng);

        // If you want the "Original Look", only animate the camera if it's the first time
        // or if the user isn't currently moving the map themselves.
        CameraPosition cp = new CameraPosition.Builder()
                .target(current)
                .zoom(mMap.getCameraPosition().zoom) // Maintain current user zoom
                .tilt(mMap.getCameraPosition().tilt) // Maintain current user tilt
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));

        tvLatLng.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f", lat, lng));
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
            snackbarView.setLayoutParams(params);

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

    private void loadDepartments(GoogleMap map) {
        String json = loadJSONFromAssets();

        // FIX: Guard clause to prevent the crash
        if (json == null || json.isEmpty()) {
            Log.e("LocationFragment", "Failed to load JSON: String is empty or null");
            return;
        }

        try {
            JSONObject root = new JSONObject(json);
            Log.d("DEBUG_JSON", "Root keys: " + root.keys().toString()); // See what's actually in the root

            if (root.has("emergency_departments")) {
                JSONObject allDepts = root.getJSONObject("emergency_departments");
                Log.d("DEBUG_JSON", "Number of departments: " + allDepts.length());
                Iterator<String> keys = allDepts.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject d = allDepts.getJSONObject(key);

                    String name = d.getString("place_name");
                    double lat = d.getDouble("latitude");
                    double lng = d.getDouble("longitude");

                    Log.d("DEBUG_MARKER", "Adding marker: " + name + " at " + lat + ", " + lng);
                    Marker marker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(name));

                    // Tag with the model object
                    marker.setTag(new Department(name, lat, lng));
                }
            } else {
                Log.e("DEBUG_JSON", "Key 'emergency_departments' not found!");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFromAssets() {
        String json = null;
        try {
            java.io.InputStream is = requireContext().getAssets().open("Emergency_Departments.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return ""; // Return an empty string instead of null
        }
        return json;
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