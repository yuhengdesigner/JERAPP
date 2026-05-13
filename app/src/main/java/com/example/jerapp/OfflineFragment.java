package com.example.jerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;

public class OfflineFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_offline, container, false);

        // Find the single SOS card
        MaterialCardView cardSos = view.findViewById(R.id.cardSosFlashlight);

        // Set click listener to open the FlashlightActivity
        cardSos.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FlashlightActivity.class);
            startActivity(intent);
        });

        return view;
    }
}