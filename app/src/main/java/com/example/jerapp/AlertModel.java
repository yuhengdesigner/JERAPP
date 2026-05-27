package com.example.jerapp;

import com.google.firebase.database.Exclude;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.PropertyName;

public class AlertModel {

    private String userId, userName, gender, userPhone, userEmail, emergencyType, status, textAddress, assignedDept, dept_id, deptName, deptPhone, videoUrl, key;
    private double userLat, userLng, deptLat, deptLng;
    private long timestamp;
    private List<String> videoUrls = new ArrayList<>();

    public AlertModel() {}

    // --- KEY MANAGEMENT ---
    @Exclude public String getKey() { return key; }
    @Exclude public void setKey(String key) { this.key = key; }

    @PropertyName("video_urls") List<String> getVideoUrls() { return videoUrls == null ? new ArrayList<>() : videoUrls; }
    @PropertyName("video_urls") public void setVideoUrls(List<String> videoUrls) { this.videoUrls = videoUrls; }

    // --- GETTERS ---
    @PropertyName("user_name") public String getUserName() { return userName != null ? userName : "Unknown User"; }
    @PropertyName("gender") public String getGender() { return gender != null ? gender : "N/A"; }
    @PropertyName("user_phone") public String getUserPhone() { return userPhone != null ? userPhone : "N/A"; }
    @PropertyName("user_email") public String getUserEmail() { return userEmail != null ? userEmail : "No Email Provided"; }
    @PropertyName("emergency_type") public String getEmergencyType() { return emergencyType != null ? emergencyType : "EMERGENCY"; }
    @PropertyName("text_address") public String getTextAddress() { return textAddress != null ? textAddress : "No address provided"; }
    @PropertyName("status") public String getStatus() { return status != null ? status : "Pending"; }
    @PropertyName("video_url") public String getVideoUrl() { return videoUrl; }
    @PropertyName("dept_name") public String getDeptName() { return deptName != null ? deptName : "Unknown Department"; }
    @PropertyName("dept_phone") public String getDeptPhone() { return deptPhone != null ? deptPhone : "N/A"; }
    @PropertyName("timestamp") long getTimestamp() { return timestamp; }
    @PropertyName("dept_lat") public double getDeptLat() { return deptLat; }
    @PropertyName("dept_lng") public double getDeptLng() { return deptLng; }

    @PropertyName("user_address") public String getUserAddress() { return textAddress != null ? textAddress : "N/A"; }
    @PropertyName("dept_address") public String getDeptAddress() { return "Address not available"; }
    @PropertyName("user_gender") public String getUserGender() { return gender != null ? gender : "N/A"; }
    @PropertyName("user_lat") public double getUserLat() { return userLat; }
    @PropertyName("user_lng") public double getUserLng() { return userLng; }

    @PropertyName("assigned_dept") public String getAssignedDept() {
        if (dept_id != null && !dept_id.isEmpty()) return dept_id;
        return assignedDept != null ? assignedDept : "";
    }

    // --- SETTERS ---
    @PropertyName("user_name") public void setUserName(String userName) { this.userName = userName; }
    @PropertyName("user_id") void setUserId(String userId) { this.userId = userId; }
    @PropertyName("gender")public void setGender(String gender) { this.gender = gender; }
    @PropertyName("user_phone")public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    @PropertyName("user_email")public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    @PropertyName("emergency_type")public void setEmergencyType(String emergencyType) { this.emergencyType = emergencyType; }
    @PropertyName("status")public void setStatus(String status) { this.status = status; }
    @PropertyName("text_address")public void setTextAddress(String textAddress) { this.textAddress = textAddress; }
    @PropertyName("video_url")public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    @PropertyName("dept_name")public void setDeptName(String deptName) { this.deptName = deptName; }
    @PropertyName("dept_phone")public void setDeptPhone(String deptPhone) { this.deptPhone = deptPhone; }
    @PropertyName("assigned_dept")public void setAssignedDept(String assignedDept) { this.assignedDept = assignedDept; }
    @PropertyName("dept_id")public void setDept_id(String dept_id) { this.dept_id = dept_id; }
    @PropertyName("user_lat")public void setUserLat(double userLat) { this.userLat = userLat; }
    @PropertyName("user_lng")public void setUserLng(double userLng) { this.userLng = userLng; }
    @PropertyName("dept_lat")public void setDeptLat(double deptLat) { this.deptLat = deptLat; }
    @PropertyName("dept_lng")public void setDeptLng(double deptLng) { this.deptLng = deptLng; }
    @PropertyName("timestamp")public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}