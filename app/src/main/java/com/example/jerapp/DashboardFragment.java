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
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.dashboardRecyclerView);

        // 1. Populate data
        List<EmergencyModel> emergencyList = new ArrayList<>();
        emergencyList.add(new EmergencyModel("Fire & Explosion", R.drawable.ic_jerlogo, "fire"));
        emergencyList.add(new EmergencyModel("Natural Disaster", R.drawable.ic_disasterlogo, "disaster"));
        emergencyList.add(new EmergencyModel("Medical Emergency", R.drawable.ic_medicallogo, "medical"));
        emergencyList.add(new EmergencyModel("Police & Crime", R.drawable.ic_policelogo, "police"));
        emergencyList.add(new EmergencyModel("Gas Leaking", R.drawable.ic_gaslogo, "gas"));
        emergencyList.add(new EmergencyModel("Wild Animals", R.drawable.ic_wildlogo, "wild"));

        // 2. Initialize Adapter
        EmergencyAdapter adapter = new EmergencyAdapter(emergencyList, model -> {
            Intent intent = new Intent(requireActivity(), DepartmentListActivity.class);
            intent.putExtra("emergency_type", model.getType());

            // Example coordinates (replace with your location logic if needed)
            intent.putExtra("user_lat", 1.4588);
            intent.putExtra("user_lng", 103.7461);

            startActivity(intent);
        });

        // 3. Configure Grid Layout Manager (The key for scrolling everything)
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // position 0 is our Header (Clock + Title) -> spans 2 columns
                // other positions are the grid items -> span 1 column
                return (adapter.getItemViewType(position) == EmergencyAdapter.TYPE_HEADER) ? 2 : 1;
            }
        });

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }
}