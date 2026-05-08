package com.example.jerapp;

public class User {
    public String name;
    public String email;
    public String role;
    public String phone;

    public User() {} // Required for Firebase

    public User(String name, String email, String role, String phone) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.phone = phone;
    }
}
