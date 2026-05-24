package com.example.jerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class UserHistoryDetailActivity extends AppCompatActivity {

    private TextView tvType, tvDept, tvAddress, tvStatus;
    private Button btnNavigate, btnCall;
    private RecyclerView videoRecyclerView;
    private VideoAdapter videoAdapter;
    private List<String> videoUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_history_detail);

        tvType = findViewById(R.id.histType);
        tvDept = findViewById(R.id.histDept);
        tvAddress = findViewById(R.id.histAddress);
        tvStatus = findViewById(R.id.histStatus);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnCall = findViewById(R.id.btnCall);
        videoRecyclerView = findViewById(R.id.videoRecyclerView);

        videoRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoAdapter = new VideoAdapter(videoUrls, url -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "video/mp4");
            startActivity(intent);
        });
        videoRecyclerView.setAdapter(videoAdapter);

        String alertKey = getIntent().getStringExtra("alert_key");
        String uid = FirebaseAuth.getInstance().getUid();

        if (alertKey == null || uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("UserHistory").child(uid).child(alertKey);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AlertModel alert = snapshot.getValue(AlertModel.class);
                if (alert != null) {
                    tvType.setText(alert.getEmergencyType());
                    tvDept.setText("Dept: " + alert.getDeptName());
                    tvAddress.setText(alert.getTextAddress());
                    tvStatus.setText("Status: " + alert.getStatus());

                    if (alert.getVideoUrls() != null) {
                        videoUrls.addAll(alert.getVideoUrls());
                        videoAdapter.notifyDataSetChanged();
                    }

                    btnNavigate.setOnClickListener(v -> {
                        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + alert.getUserLat() + "," + alert.getUserLng());
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        startActivity(mapIntent);
                    });

                    btnCall.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + alert.getDeptPhone()))));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}