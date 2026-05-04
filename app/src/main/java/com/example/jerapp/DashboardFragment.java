package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflates the layout with the Red/White theme and Safe Padding
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.dashboardRecyclerView);
        List<EmergencyModel> emergencyList = new ArrayList<>();

        // 1. Populate your full list of categories
        emergencyList.add(new EmergencyModel("Fire & Explosion", R.drawable.ic_jerlogo, "fire"));
        emergencyList.add(new EmergencyModel("Natural Disaster", R.drawable.ic_disasterlogo, "disaster"));
        emergencyList.add(new EmergencyModel("Medical Emergency", R.drawable.ic_medicallogo, "medical"));
        emergencyList.add(new EmergencyModel("Police & Crime", R.drawable.ic_policelogo, "police"));
        emergencyList.add(new EmergencyModel("Gas Leaking", R.drawable.ic_gaslogo, "gas"));
        emergencyList.add(new EmergencyModel("Wild Animals", R.drawable.ic_wildlogo, "wild"));

        // 2. Setup the Click Listener
        EmergencyAdapter adapter = new EmergencyAdapter(emergencyList, model -> {
            Intent intent = new Intent(requireActivity(), DepartmentListActivity.class);

            // Pass the emergency type
            intent.putExtra("emergency_type", model.getType());

            // IMPORTANT: Pass the GPS coords from MainActivity if they exist
            if (getActivity() instanceof MainActivity) {
                MainActivity main = (MainActivity) getActivity();
                // These names must match what DepartmentListActivity is looking for
                intent.putExtra("user_lat", 1.4588); // Replace with real variable if stored in MainActivity
                intent.putExtra("user_lng", 103.7461);
            }

            startActivity(intent);
        });

        // 3. Initialize the Grid
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
    }
}