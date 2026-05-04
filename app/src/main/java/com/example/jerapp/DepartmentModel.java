package com.example.jerapp;

public class DepartmentModel {
    public String name, type, status, contact, capability;
    public double lat, lng;

    // Required empty constructor for Firebase
    public DepartmentModel() {}

    public DepartmentModel(String name, String type, String status, String contact, String capability, double lat, double lng) {
        this.name = name;
        this.type = type;
        this.status = status;
        this.contact = contact;
        this.capability = capability;
        this.lat = lat;
        this.lng = lng;
    }
}