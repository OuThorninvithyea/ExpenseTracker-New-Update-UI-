package com.example.myapplication.models;

public class SignupResult {
    public boolean success;
    public User user;
    public String error;

    public SignupResult(boolean success, User user, String error) {
        this.success = success;
        this.user = user;
        this.error = error;
    }
}
