package com.example.jerapp;

public class Department { // Rename to Department to match your code
    public String place_name;
    public double latitude;
    public double longitude;

    public Department(String place_name, double lat, double lng) {
        this.place_name = place_name;
        this.latitude = lat;
        this.longitude = lng;
    }
}
