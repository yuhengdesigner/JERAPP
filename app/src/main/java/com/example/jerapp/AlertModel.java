package com.example.jerapp;

import com.google.firebase.database.Exclude;

public class AlertModel {
    public String userName, emergencyType, assignedDept, status;
    public double userLat, userLng;
    public long timestamp;

    // This field stores the unique Firebase ID (e.g., SAMPLE_ALERT_001)
    private String key;

    // Required empty constructor for Firebase
    public AlertModel() {}

    public AlertModel(String userName, String emergencyType, String assignedDept, String status, double userLat, double userLng, long timestamp) {
        this.userName = userName;
        this.emergencyType = emergencyType;
        this.assignedDept = assignedDept;
        this.status = status;
        this.userLat = userLat;
        this.userLng = userLng;
        this.timestamp = timestamp;
    }

    // Getters and Setters for the Key
    @Exclude // We use @Exclude so the key doesn't get saved as a child inside itself in Firebase
    public String getKey() {
        return key;
    }

    @Exclude
    public void setKey(String key) {
        this.key = key;
    }
}