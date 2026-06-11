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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Typeface;

import java.util.Map;

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

        setupExpandableCard(view, R.id.cardFire, R.id.tvFireContent, R.id.ivFireIcon, "<b>Fire Emergency</b><br>1. Stop, Drop, and Roll if your clothes catch fire.<br>2. Crawl low under smoke to escape.<br>3. Evacuate immediately and call 999.<br>4. Do not use elevators.");
        setupExpandableCard(view, R.id.cardBleeding, R.id.tvBleedingContent, R.id.ivBleedingIcon, "<b>Severe Bleeding</b><br>1. Apply direct pressure to the wound with a clean cloth.<br>2. Elevate the injured area if possible.<br>3. Do not remove the cloth if it becomes soaked; add more layers.<br>4. Call emergency services immediately.");
        setupExpandableCard(view, R.id.cardSnakebites, R.id.tvSnakebitesContent, R.id.ivSnakebitesIcon, "<b>Snakebites</b><br>1. Keep the victim calm and still to slow the spread of venom.<br>2. Remove rings and constricting items.<br>3. Do NOT attempt to suck out the venom or cut the wound.<br>4. Immobilize the bitten limb and seek medical help immediately.");
        addGuideCards(view);

        return view;
    }

    private void addGuideCards(View root) {
        if (!(root instanceof ViewGroup)) return;
        ViewGroup outer = (ViewGroup) root;
        if (outer.getChildCount() == 0 || !(outer.getChildAt(0) instanceof android.widget.ScrollView)) return;
        android.widget.ScrollView scrollView = (android.widget.ScrollView) outer.getChildAt(0);
        if (scrollView.getChildCount() == 0 || !(scrollView.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout container = (LinearLayout) scrollView.getChildAt(0);

        TextView heading = new TextView(requireContext());
        heading.setText("Step-by-Step Emergency Tutorials");
        heading.setTextSize(20);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(getResources().getColor(R.color.primary));
        heading.setPadding(dp(12), dp(20), dp(12), dp(8));
        container.addView(heading);

        for (Map.Entry<String, OfflineGuideActivity.Guide> entry : OfflineGuideActivity.getGuides().entrySet()) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setRadius(dp(8));
            card.setCardElevation(dp(2));
            card.setClickable(true);
            card.setFocusable(true);
            card.setForeground(requireContext().obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground}).getDrawable(0));

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(16), dp(12));

            ImageView icon = new ImageView(requireContext());
            icon.setImageResource(entry.getValue().imageRes);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            row.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));

            TextView title = new TextView(requireContext());
            title.setText(entry.getValue().title);
            title.setTextSize(17);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(getResources().getColor(R.color.primary));
            title.setPadding(dp(14), 0, 0, 0);
            row.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            card.addView(row);
            card.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), OfflineGuideActivity.class);
                intent.putExtra("guide_key", entry.getKey());
                startActivity(intent);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(8), dp(6), dp(8), dp(6));
            container.addView(card, params);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void setupExpandableCard(View root, int cardId, int contentId, int iconId, String htmlContent) {
        MaterialCardView card = root.findViewById(cardId);
        android.widget.TextView tvContent = root.findViewById(contentId);
        android.widget.ImageView ivIcon = root.findViewById(iconId);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvContent.setText(android.text.Html.fromHtml(htmlContent, android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvContent.setText(android.text.Html.fromHtml(htmlContent));
        }

        card.setOnClickListener(v -> {
            boolean isExpanded = tvContent.getVisibility() == View.VISIBLE;
            tvContent.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            ivIcon.setImageResource(isExpanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float);
        });
    }
}
