package com.example.jerapp;

import com.google.firebase.database.PropertyName;

public class DepartmentModel {
    // 1. Add the ID field
    public String id;

    public String place_name, type, district, category, full_address, contact, fee_detail, status;

    @PropertyName("latitude")
    public double latitude;

    @PropertyName("longitude")
    public double longitude;

    public double distance;

    // Required empty constructor for Firebase
    public DepartmentModel() {}

    public DepartmentModel(String id, String place_name, String type, String district,
                           String category, String full_address, String status,
                           String contact, String fee_detail,
                           double latitude, double longitude) {
        this.id = id;
        this.place_name = place_name;
        this.type = type;
        this.district = district;
        this.category = category;
        this.full_address = full_address;
        this.status = status;
        this.contact = contact;
        this.fee_detail = fee_detail;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}