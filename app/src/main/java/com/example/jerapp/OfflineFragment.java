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

        MaterialCardView cardSos = view.findViewById(R.id.cardSosFlashlight);
        MaterialCardView cardGeneralCpr = view.findViewById(R.id.cardGeneralCpr);
        MaterialCardView cardAdultCpr = view.findViewById(R.id.cardAdultCpr);
        MaterialCardView cardChildCpr = view.findViewById(R.id.cardChildCpr);
        MaterialCardView cardInfantCpr = view.findViewById(R.id.cardInfantCpr);
        MaterialCardView cardNeonateCpr = view.findViewById(R.id.cardNeonateCpr);

        cardSos.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FlashlightActivity.class);
            startActivity(intent);
        });

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

        setupExpandableCard(view, R.id.cardFire, R.id.tvFireContent, R.id.ivFireIcon, "<b>Fire Emergency</b><br>1. Stop, Drop, and Roll if your clothes catch fire.<br>2. Crawl low under smoke to escape.<br>3. Evacuate immediately and call 999.<br>4. Do not use elevators.");
        setupExpandableCard(view, R.id.cardBleeding, R.id.tvBleedingContent, R.id.ivBleedingIcon, "<b>Severe Bleeding</b><br>1. Apply direct pressure to the wound with a clean cloth.<br>2. Elevate the injured area if possible.<br>3. Do not remove the cloth if it becomes soaked; add more layers.<br>4. Call emergency services immediately.");
        setupExpandableCard(view, R.id.cardSnakebites, R.id.tvSnakebitesContent, R.id.ivSnakebitesIcon, "<b>Snakebites</b><br>1. Keep the victim calm and still to slow the spread of venom.<br>2. Remove rings and constricting items.<br>3. Do NOT attempt to suck out the venom or cut the wound.<br>4. Immobilize the bitten limb and seek medical help immediately.");

        setupGuideNavigation(view, R.id.cardSeeingPersonEmergency, "Seeing a Person in Emergency");
        setupGuideNavigation(view, R.id.cardFindUseAed, "Find and Use AED");
        setupGuideNavigation(view, R.id.cardEvacuateFire, "Evacuate From Fire");
        setupGuideNavigation(view, R.id.cardChoking, "Choking");
        setupGuideNavigation(view, R.id.cardSeeingFire, "Seeing Fire");
        setupGuideNavigation(view, R.id.cardCarAccident, "Car Accident");
        setupGuideNavigation(view, R.id.cardSomeoneElectrocuted, "Someone Electrocuted");
        setupGuideNavigation(view, R.id.cardProtectElectricalShock, "Protect from Electrical Shock");
        setupGuideNavigation(view, R.id.cardWildAnimal, "Wild Animal Attack");
        setupGuideNavigation(view, R.id.cardEarthquake, "Earthquake");
        setupGuideNavigation(view, R.id.cardTornado, "Tornado");
        setupGuideNavigation(view, R.id.cardTsunami, "Tsunami");

        return view;
    }

    private void setupGuideNavigation(View root, int cardResId, final String guideKey) {
        MaterialCardView card = root.findViewById(cardResId);
        if (card != null) {
            card.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), OfflineGuideActivity.class);
                intent.putExtra("guide_key", guideKey);
                startActivity(intent);
            });
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