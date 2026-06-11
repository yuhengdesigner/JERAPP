package com.example.jerapp;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import java.util.LinkedHashMap;
import java.util.Map;

public class OfflineGuideActivity extends AppCompatActivity {

    private static final Map<String, Guide> GUIDES = new LinkedHashMap<>();

    static {
        add("fire_evacuation", "Evacuate From Fire", R.drawable.ic_firelogo,
                "WHAT: Leave smoke, heat, and flames quickly before breathing becomes dangerous.",
                "WHY: Smoke can overcome a person in minutes, even before flames reach them.",
                "WHO: Everyone in the building, especially children, elderly people, and anyone with breathing problems.",
                "WHEN: Evacuate immediately when you smell smoke, hear an alarm, see fire, or feel unsafe.",
                "WHERE: Move to the nearest safe exit, then gather far away from the building.",
                "HOW: Crawl low under smoke, touch doors before opening, use stairs, close doors behind you, call 999, and never re-enter.");
        add("choking", "Choking", R.drawable.ic_medicallogo,
                "WHAT: A blocked airway can stop breathing and oxygen flow.",
                "WHY: Fast action can clear the object before the person collapses.",
                "WHO: Anyone who cannot cough, speak, cry, or breathe normally.",
                "WHEN: Start help immediately if the person is silent, clutching the throat, or turning blue.",
                "WHERE: Keep them standing or supported with space around the body.",
                "HOW: Give 5 back blows, then 5 abdominal thrusts for adults and children. For infants, use 5 back blows and 5 chest thrusts. Call 999 if it does not clear.");
        add("wild_animal_attack", "Wild Animal Attack", R.drawable.ic_wildlogo,
                "WHAT: An animal attack can cause bleeding, shock, infection, and venom injury.",
                "WHY: Distance and bleeding control reduce further harm.",
                "WHO: The injured person and anyone nearby who may still be in danger.",
                "WHEN: Help only after the animal is no longer close enough to attack.",
                "WHERE: Move to a secure building, vehicle, or open area away from the animal.",
                "HOW: Do not chase the animal. Call 999, control bleeding with firm pressure, keep the person still, and note the animal type for responders.");
        add("aed", "Find And Use AED", R.drawable.ic_cpr_kit,
                "WHAT: An AED checks heart rhythm and can deliver a shock during cardiac arrest.",
                "WHY: Early defibrillation gives the person a better chance of survival.",
                "WHO: Use it for an unresponsive person who is not breathing normally.",
                "WHEN: Use as soon as an AED arrives while CPR continues.",
                "WHERE: Look near entrances, security counters, malls, airports, sports centers, schools, and offices.",
                "HOW: Turn it on, bare the chest, attach pads as shown, follow voice prompts, keep everyone clear during analysis and shock.");
        add("bystander_emergency", "Seeing A Person In Emergency", R.drawable.ic_alert,
                "WHAT: A bystander should protect the scene, call help, and give simple first aid.",
                "WHY: Early action prevents delays before professional responders arrive.",
                "WHO: You, nearby helpers, the injured person, and emergency services.",
                "WHEN: Act immediately, but only when the scene is safe.",
                "WHERE: Stay visible to responders and move hazards away if safe.",
                "HOW: Check danger, call 999, check response and breathing, start CPR if needed, control bleeding, and keep the person warm.");
        add("active_fire", "Seeing Fire", R.drawable.ic_firelogo,
                "WHAT: Fire can spread through heat, smoke, fuel, and electricity.",
                "WHY: Small fires become dangerous quickly.",
                "WHO: Alert everyone nearby and protect anyone unable to leave alone.",
                "WHEN: Call 999 immediately for any spreading fire or smoke.",
                "WHERE: Stay outside and upwind once evacuated.",
                "HOW: Trigger the alarm, evacuate, close doors, avoid elevators, use an extinguisher only for a very small fire and only with a clear exit behind you.");
        add("severe_bleeding", "Urgent Bleeding", R.drawable.ic_medicallogo,
                "WHAT: Severe bleeding is blood loss that does not stop quickly or soaks clothing.",
                "WHY: Major blood loss can cause shock and death.",
                "WHO: Anyone with deep cuts, crushed limbs, amputations, or heavy bleeding.",
                "WHEN: Apply pressure immediately and call 999.",
                "WHERE: Press directly on the wound with cloth, gauze, or clothing.",
                "HOW: Keep firm pressure, add layers if soaked, do not remove embedded objects, raise the limb if possible, and keep the person lying down.");
        add("snakebite", "Snakebite", R.drawable.ic_wildlogo,
                "WHAT: A snakebite may inject venom and cause swelling, pain, weakness, or breathing problems.",
                "WHY: Calm, still movement slows venom spread.",
                "WHO: The bitten person needs urgent medical assessment even if symptoms seem mild.",
                "WHEN: Call 999 immediately after moving away from the snake.",
                "WHERE: Keep the bitten limb below heart level if possible.",
                "HOW: Keep still, remove rings or tight items, mark swelling edges, do not cut, suck, ice, or tourniquet the bite.");
        add("car_accident", "Car Accident", R.drawable.ic_policelogo,
                "WHAT: Vehicle crashes can involve hidden injuries, fuel leaks, electricity, and traffic danger.",
                "WHY: Scene safety prevents more victims.",
                "WHO: Drivers, passengers, pedestrians, and nearby helpers.",
                "WHEN: Call 999 if anyone is injured, trapped, unconscious, or traffic is unsafe.",
                "WHERE: Stand away from traffic, leaking fuel, and unstable vehicles.",
                "HOW: Turn on hazard lights if safe, do not move injured people unless there is immediate danger, control bleeding, and guide responders to the location.");
        add("earthquake", "Earthquake", R.drawable.ic_disasterlogo,
                "WHAT: Ground shaking can drop objects, break glass, and damage buildings.",
                "WHY: Most injuries happen from falling objects and unsafe exits.",
                "WHO: Everyone indoors or near buildings.",
                "WHEN: Protect yourself during shaking, evacuate only after it stops.",
                "WHERE: Drop under sturdy furniture or beside an interior wall away from windows.",
                "HOW: Drop, cover, hold on. After shaking, check injuries, avoid damaged structures, and expect aftershocks.");
        add("tornado", "Tornado", R.drawable.ic_disasterlogo,
                "WHAT: A tornado brings violent rotating wind and flying debris.",
                "WHY: Shelter reduces impact from debris.",
                "WHO: Anyone outdoors, in vehicles, or near windows.",
                "WHEN: Move when warnings, sirens, or funnel clouds appear.",
                "WHERE: Go to a small interior room on the lowest floor.",
                "HOW: Stay away from windows, cover your head and neck, avoid bridges, and do not stay in a vehicle if sturdy shelter is available.");
        add("tsunami", "Tsunami", R.drawable.ic_disasterlogo,
                "WHAT: A tsunami is a series of powerful sea waves after an earthquake or sea disturbance.",
                "WHY: The first wave may not be the largest.",
                "WHO: Anyone near beaches, rivers, or low coastal areas.",
                "WHEN: Evacuate after strong coastal shaking, unusual sea withdrawal, or official warning.",
                "WHERE: Move inland and to higher ground.",
                "HOW: Leave immediately on foot if roads are blocked, follow evacuation signs, and return only when authorities say it is safe.");
        add("electrocution", "Someone Electrocuted", R.drawable.ic_gaslogo,
                "WHAT: Electric shock can stop breathing, disturb heart rhythm, and burn tissue.",
                "WHY: Touching the person before power is off can shock you too.",
                "WHO: The victim and all helpers near the electrical source.",
                "WHEN: Call 999 immediately for high voltage, unconsciousness, burns, or breathing problems.",
                "WHERE: Stay clear of water, wires, outlets, and metal objects touching the source.",
                "HOW: Switch off power first. If safe, use a dry non-metal object to separate them, check breathing, start CPR if needed, and cover burns loosely.");
        add("electrical_safety", "Protect From Electric Shock", R.drawable.ic_gaslogo,
                "WHAT: Electrical protection means avoiding contact with live current.",
                "WHY: Current can travel through water, metal, damaged cables, and the body.",
                "WHO: Anyone using appliances, chargers, extension cords, or outdoor power.",
                "WHEN: Stop using electricity during flooding, sparks, burning smell, or damaged wiring.",
                "WHERE: Keep devices away from water and overloaded sockets.",
                "HOW: Dry your hands, unplug damaged devices, avoid exposed wires, use proper circuit breakers, and call qualified help for repairs.");
    }

    private static void add(String key, String title, int imageRes, String... sections) {
        GUIDES.put(key, new Guide(title, imageRes, sections));
    }

    public static Map<String, Guide> getGuides() {
        return GUIDES;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String key = getIntent().getStringExtra("guide_key");
        Guide guide = GUIDES.get(key);
        if (guide == null) {
            finish();
            return;
        }

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(10), dp(16), dp(8));
        toolbar.setBackgroundColor(getColor(R.color.primary));

        ImageButton back = new ImageButton(this);
        back.setImageResource(R.drawable.ic_back);
        back.setColorFilter(getColor(R.color.white));
        back.setBackgroundResource(android.R.color.transparent);
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        back.setOnClickListener(v -> finish());

        TextView title = new TextView(this);
        title.setText(guide.title);
        title.setTextColor(getColor(R.color.white));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        page.addView(toolbar);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));
        scrollView.addView(content);
        page.addView(scrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        MaterialCardView hero = new MaterialCardView(this);
        hero.setRadius(dp(8));
        hero.setCardElevation(dp(2));
        ImageView image = new ImageView(this);
        image.setImageResource(guide.imageRes);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        image.setPadding(dp(24), dp(24), dp(24), dp(24));
        hero.addView(image, new MaterialCardView.LayoutParams(MaterialCardView.LayoutParams.MATCH_PARENT, dp(160)));
        content.addView(hero, matchWrapWithBottomMargin());

        for (String section : guide.sections) {
            addSection(content, section);
        }

        setContentView(page);
    }

    private void addSection(LinearLayout content, String section) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(dp(2));
        card.setUseCompatPadding(true);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView heading = new TextView(this);
        String[] parts = section.split(":", 2);
        heading.setText(parts[0]);
        heading.setTextColor(getColor(R.color.primary));
        heading.setTextSize(17);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        inner.addView(heading);

        TextView body = new TextView(this);
        body.setText(Html.fromHtml(parts.length > 1 ? parts[1].trim() : section, Html.FROM_HTML_MODE_COMPACT));
        body.setTextColor(android.graphics.Color.parseColor("#424242"));
        body.setTextSize(15);
        body.setLineSpacing(dp(2), 1f);
        body.setVisibility(View.GONE);
        inner.addView(body);

        card.addView(inner);
        card.setOnClickListener(v -> body.setVisibility(body.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        content.addView(card, matchWrapWithBottomMargin());
    }

    private LinearLayout.LayoutParams matchWrapWithBottomMargin() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    public static class Guide {
        public final String title;
        public final int imageRes;
        public final String[] sections;

        Guide(String title, int imageRes, String[] sections) {
            this.title = title;
            this.imageRes = imageRes;
            this.sections = sections;
        }
    }
}
