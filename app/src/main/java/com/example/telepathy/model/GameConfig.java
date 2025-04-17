package com.example.telepathy.model;

import java.util.ArrayList;
import java.util.List;

public class GameConfig {
    private int timeLimit; // Time limit for word selection in seconds
    private int maxPlayers;
    private int livesPerPlayer;
    private List<String> categories;
    private String selectedCategory;
    private String gameMode;

    // Default constructor for Firebase
    public GameConfig() {
        // Initialize with default values
        this.timeLimit = 30;
        this.maxPlayers = 8;
        this.livesPerPlayer = 3;
        this.categories = new ArrayList<>();
        this.categories.add("Animals");
        this.categories.add("Countries");
        this.categories.add("Foods");
        this.categories.add("Sports");
        this.selectedCategory = "Animals";
        this.gameMode = "classic"; // Default game mode in lowercase
    }

    // Constructor with custom parameters
    public GameConfig(int timeLimit, int maxPlayers, int livesPerPlayer, String selectedCategory, String gameMode) {
        this.timeLimit = timeLimit;
        this.maxPlayers = maxPlayers;
        this.livesPerPlayer = livesPerPlayer;
        this.categories = new ArrayList<>();
        this.categories.add("Animals");
        this.categories.add("Countries");
        this.categories.add("Foods");
        this.categories.add("Sports");
        this.selectedCategory = selectedCategory;
        this.gameMode = gameMode;
    }

    // Getters and setters
    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getLivesPerPlayer() {
        return livesPerPlayer;
    }

    public void setLivesPerPlayer(int livesPerPlayer) {
        this.livesPerPlayer = livesPerPlayer;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public void addCategory(String category) {
        if (!this.categories.contains(category)) {
            this.categories.add(category);
        }
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }
}