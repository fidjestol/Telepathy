package com.example.telepathy.model;

public class User {
    private String id;
    private String username;

    private int totalScore;

    //Firebase empty constructor
    public User() {}

    // Constructor for new users - for storing in Firebase
    public User(String id, String username) {
        this.id = id;
        this.username = username;
        this.totalScore = 0;
    }

    // Constructor for fetching user from storage
    public User(String id, String username, int totalScore) {
        this.id = id;
        this.username = username;
        this.totalScore = totalScore;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public int getTotalScore() {
        return totalScore;
    }
}