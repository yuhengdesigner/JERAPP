package com.example.jerapp;

import com.google.firebase.database.Exclude;

public class AlertModel {
    public String userId, userName, userPhone, userEmail, emergencyType, assignedDept, textAddress, status;
    public String videoUrl, deptName, deptPhone;
    public double userLat, userLng, deptLat, deptLng;
    public long timestamp;
    private String key;

    public AlertModel() {} // Required for Firebase

    // Getters and Setters for Firebase and Fragment logic
    public String getVideoUrl() { return videoUrl; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserPhone() { return userPhone; }
    public String getEmergencyType() { return emergencyType; }
    public String getAssignedDept() { return assignedDept; }
    public String getStatus() { return status; }

    // These match the onLocate logic
    public double getLatitude() { return userLat; }
    public double getLongitude() { return userLng; }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDeptPhone() {
        return deptPhone;
    }

    public double getDeptLat() {
        return deptLat;
    }

    public double getDeptLng() {
        return deptLng;
    }

    @Exclude
    public String getKey() { return key; }
    @Exclude
    public void setKey(String key) { this.key = key; }
}