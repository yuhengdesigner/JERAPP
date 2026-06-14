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

        view.findViewById(R.id.cardSosFlashlight).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), FlashlightActivity.class)));

        view.findViewById(R.id.cardGeneralCpr).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprGeneralActivity.class)));

        view.findViewById(R.id.cardAdultCpr).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprAdultActivity.class)));

        view.findViewById(R.id.cardChildCpr).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprChildActivity.class)));

        view.findViewById(R.id.cardInfantCpr).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprInfantActivity.class)));

        view.findViewById(R.id.cardNeonateCpr).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CprNeonateActivity.class)));

        setupExpandableCard(view, R.id.cardFire, R.id.tvFireContent, R.id.ivFireIcon, "<b>Fire Emergency</b><br>1. Stop, Drop, and Roll if your clothes catch fire.<br>2. Crawl low under smoke to escape.<br>3. Evacuate immediately and call 999.<br>4. Do not use elevators.");
        setupExpandableCard(view, R.id.cardBleeding, R.id.tvBleedingContent, R.id.ivBleedingIcon, "<b>Severe Bleeding</b><br>1. Apply direct pressure to the wound with a clean cloth.<br>2. Elevate the injured area if possible.<br>3. Do not remove the cloth if it becomes soaked; add more layers.<br>4. Call emergency services immediately.");
        setupExpandableCard(view, R.id.cardSnakebites, R.id.tvSnakebitesContent, R.id.ivSnakebitesIcon, "<b>Snakebites</b><br>1. Keep the victim calm and still to slow the spread of venom.<br>2. Remove rings and constricting items.<br>3. Do NOT attempt to suck out the venom or cut the wound.<br>4. Immobilize the bitten limb and seek medical help immediately.");

        bindCardClick(view, R.id.cardFindUseAed, AedActivity.class);
        bindCardClick(view, R.id.cardSeeingPersonEmergency, SeeingEmergencyActivity.class);
        bindCardClick(view, R.id.cardSeeingFire, SeeingFireActivity.class);
        bindCardClick(view, R.id.cardUrgentBleeding, UrgentBleedingActivity.class);
        bindCardClick(view, R.id.cardChoking, ChokingActivity.class);
        bindCardClick(view, R.id.cardEvacuateFire, EvacuateFireActivity.class);
        bindCardClick(view, R.id.cardSnakebite, SnakebiteActivity.class);
        bindCardClick(view, R.id.cardCarAccident, CarAccidentActivity.class);
        bindCardClick(view, R.id.cardWildAnimal, WildAnimalActivity.class);
        bindCardClick(view, R.id.cardEarthquake, EarthquakeActivity.class);
        bindCardClick(view, R.id.cardTornado, TornadoActivity.class);
        bindCardClick(view, R.id.cardTsunami, TsunamiActivity.class);
        bindCardClick(view, R.id.cardSomeoneElectrocuted, SomeoneElectrocutedActivity.class);
        bindCardClick(view, R.id.cardProtectElectricalShock, ProtectShockActivity.class);

        return view;
    }

    private void bindCardClick(View root, int cardResId, final Class<?> targetActivity) {
        MaterialCardView card = root.findViewById(cardResId);
        if (card != null) {
            card.setOnClickListener(v -> startActivity(new Intent(getActivity(), targetActivity)));
        }
    }

    private void setupExpandableCard(View root, int cardId, int contentId, int iconId, String htmlContent) {
        MaterialCardView card = root.findViewById(cardId);
        android.widget.TextView tvContent = root.findViewById(contentId);
        android.widget.ImageView ivIcon = root.findViewById(iconId);

        if (card == null || tvContent == null || ivIcon == null) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvContent.setText(android.text.Html.fromHtml(htmlContent, android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvContent.setText(android.text.Html.fromHtml(htmlContent));
        }

        card.setOnClickListener(v -> {
            boolean isExpanded = tvContent.getVisibility() == View.VISIBLE;
            tvContent.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            ivIcon.setImageResource(isExpanded ? android.R.drawable.arrow_down_float : android.R.drawable.arrow_up_float);
        });
    }
}