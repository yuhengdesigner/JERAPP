package com.example.jerapp;

public class DepartmentModel {
    public String place_name, type, district, category, full_address, contact, fee_detail, status;
    public double latitude, longitude;
    public double distance;

    public DepartmentModel() {}

    public DepartmentModel(String place_name, String type, String district,
                           String category, String full_address, String status,
                           String contact, String fee_detail,
                           double latitude, double longitude) {
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