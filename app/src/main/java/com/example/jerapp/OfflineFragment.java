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
        View view = inflater.inflate(R.layout.fragment_offline, container, false);

        // Bind layouts securely
        MaterialCardView cardSos = view.findViewById(R.id.cardSosFlashlight);
        MaterialCardView cardGeneralCpr = view.findViewById(R.id.cardGeneralCpr);
        MaterialCardView cardAdultCpr = view.findViewById(R.id.cardAdultCpr);
        MaterialCardView cardChildCpr = view.findViewById(R.id.cardChildCpr);
        MaterialCardView cardInfantCpr = view.findViewById(R.id.cardInfantCpr);
        MaterialCardView cardNeonateCpr = view.findViewById(R.id.cardNeonateCpr);

        // SOS Flashlight Click Execution
        cardSos.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FlashlightActivity.class);
            startActivity(intent);
        });

        // Direct isolated class navigations
        cardGeneralCpr.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprGeneralActivity.class)));

        cardAdultCpr.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprAdultActivity.class)));

        cardChildCpr.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprChildActivity.class)));

        cardInfantCpr.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprInfantActivity.class)));

        cardNeonateCpr.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprNeonateActivity.class)));

        return view;
    }
}