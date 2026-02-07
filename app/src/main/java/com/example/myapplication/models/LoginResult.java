package com.example.myapplication.models;

public class LoginResult {
    public boolean success;
    public User user;
    public String error;

    public LoginResult(boolean success, User user, String error) {
        this.success = success;
        this.user = user;
        this.error = error;
    }
}
