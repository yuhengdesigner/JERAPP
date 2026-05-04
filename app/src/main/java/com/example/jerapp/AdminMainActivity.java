package com.example.jerapp;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class AdminMainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<AlertModel> alertList;
    private AdminAdapter adminAdapter;
    private int lastAlertCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // 1. Initialize UI
        recyclerView = findViewById(R.id.adminAlertRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        alertList = new ArrayList<>();

        // 2. Start listening to Firebase
        listenForAlerts();
    }

    private void listenForAlerts() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ActiveAlerts");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear the list before adding new data
                alertList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    AlertModel alert = ds.getValue(AlertModel.class);
                    if (alert != null) {
                        alert.setKey(ds.getKey());
                        alertList.add(alert);
                    }
                }

                // Play notification sound if a new alert arrives
                if (snapshot.getChildrenCount() > lastAlertCount) {
                    playNotificationSound();
                }
                lastAlertCount = (int) snapshot.getChildrenCount();

                // 3. Setup the Adapter with click logic
                adminAdapter = new AdminAdapter(alertList, new AdminAdapter.OnAlertClickListener() {
                    @Override
                    public void onLocate(AlertModel alert) {
                        // Open Google Maps to show the victim's location
                        String uri = "geo:" + alert.userLat + "," + alert.userLng +
                                "?q=" + alert.userLat + "," + alert.userLng + "(Victim)";
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        startActivity(intent);
                    }

                    @Override
                    public void onResolve(AlertModel alert, String alertKey) {
                        // Remove from Firebase - this updates the list automatically for everyone
                        FirebaseDatabase.getInstance().getReference("ActiveAlerts")
                                .child(alertKey)
                                .removeValue()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(AdminMainActivity.this, "Emergency Resolved and Cleared", Toast.LENGTH_SHORT).show()
                                );
                    }
                });

                recyclerView.setAdapter(adminAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminMainActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}