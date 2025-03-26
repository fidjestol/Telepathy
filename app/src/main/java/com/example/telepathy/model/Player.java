package com.example.telepathy.model;

import java.util.UUID;

public class Player {
    private String id;
    private String username;
    private int score;
    private int lives;
    private boolean isHost;
    private String currentWord;
    private boolean isEliminated;

    // Default constructor for Firebase
    public Player() {}

    public Player(String id, String username) {
        this.id = id;
        this.username = username;
        this.score = 0;
        this.lives = 3; // Default value, configurable
        this.isHost = false;
        this.isEliminated = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void addPoints(int points) {
        this.score += points;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public void loseLife() {
        this.lives--;
        if (this.lives <= 0) {
            this.isEliminated = true;
        }
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public void setCurrentWord(String currentWord) {
        this.currentWord = currentWord;
    }

    public boolean isEliminated() {
        return isEliminated;
    }

    public void setEliminated(boolean eliminated) {
        isEliminated = eliminated;
    }

    public static Player fromUser(User user) {
        return new Player(user.getId(), user.getUsername());
    }

}