package com.example.jerapp;

public class EmergencyModel {
    private String title;
    private int iconRes;
    private String type;

    // Constructor must match the one used in MainActivity
    public EmergencyModel(String title, int iconRes, String type) {
        this.title = title;
        this.iconRes = iconRes;
        this.type = type;
    }

    public String getTitle() { return title; }
    public int getIconRes() { return iconRes; }
    public String getType() { return type; }
}