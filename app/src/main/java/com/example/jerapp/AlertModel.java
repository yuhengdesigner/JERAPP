package com.example.jerapp;

import com.google.firebase.database.Exclude;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.PropertyName;

public class AlertModel {
    // Firebase Fields
    public String userId, userName, gender, userPhone, userEmail, emergencyType, status;
    public String textAddress, videoUrl, deptName, deptPhone;
    public String assignedDept, dept_id;
    public double userLat, userLng, deptLat, deptLng;
    public long timestamp;
    private List<String> videoUrls = new ArrayList<>();
    private String key;

    public AlertModel() {}

    // --- KEY MANAGEMENT ---
    @Exclude public String getKey() { return key; }
    @Exclude public void setKey(String key) { this.key = key; }

    public List<String> getVideoUrls() { return videoUrls == null ? new ArrayList<>() : videoUrls; }
    public void setVideoUrls(List<String> videoUrls) { this.videoUrls = videoUrls; }

    // --- GETTERS ---
    @PropertyName("user_name")
    public String getUserName() { return userName != null ? userName : "Unknown User"; }
    public String getGender() { return gender != null ? gender : "N/A"; }
    public String getUserPhone() { return userPhone != null ? userPhone : "N/A"; }
    public String getUserEmail() { return userEmail != null ? userEmail : "No Email Provided"; }
    public String getEmergencyType() { return emergencyType != null ? emergencyType : "EMERGENCY"; }
    public String getTextAddress() { return textAddress != null ? textAddress : "No address provided"; }
    public String getStatus() { return status != null ? status : "Pending"; }
    public String getVideoUrl() { return videoUrl; }
    public String getDeptName() { return deptName != null ? deptName : "Unknown Department"; }
    public String getDeptPhone() { return deptPhone != null ? deptPhone : "N/A"; }
    public long getTimestamp() { return timestamp; }
    public double getDeptLat() { return deptLat; }
    public double getDeptLng() { return deptLng; }

    public String getUserAddress() { return textAddress != null ? textAddress : "N/A"; }
    public String getDeptAddress() { return "Address not available"; }
    public String getUserGender() { return gender != null ? gender : "N/A"; }
    public double getUserLat() { return userLat; }
    public double getUserLng() { return userLng; }

    public String getAssignedDept() {
        if (dept_id != null && !dept_id.isEmpty()) return dept_id;
        return assignedDept != null ? assignedDept : "";
    }

    // --- SETTERS ---
    @PropertyName("user_name")
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setGender(String gender) { this.gender = gender; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setEmergencyType(String emergencyType) { this.emergencyType = emergencyType; }
    public void setStatus(String status) { this.status = status; }
    public void setTextAddress(String textAddress) { this.textAddress = textAddress; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public void setDeptPhone(String deptPhone) { this.deptPhone = deptPhone; }
    public void setAssignedDept(String assignedDept) { this.assignedDept = assignedDept; }
    public void setDept_id(String dept_id) { this.dept_id = dept_id; }
    public void setUserLat(double userLat) { this.userLat = userLat; }
    public void setUserLng(double userLng) { this.userLng = userLng; }
    public void setDeptLat(double deptLat) { this.deptLat = deptLat; }
    public void setDeptLng(double deptLng) { this.deptLng = deptLng; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}