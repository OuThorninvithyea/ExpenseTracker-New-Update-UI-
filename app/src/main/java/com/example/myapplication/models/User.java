package com.example.myapplication.models;

public class User {
    public int id;
    public String username;

    public String profilePicturePath;

    public User(int id, String username) {
        this(id, username, null);
    }

    public User(int id, String username, String profilePicturePath) {
        this.id = id;
        this.username = username;
        this.profilePicturePath = profilePicturePath;
    }
}
