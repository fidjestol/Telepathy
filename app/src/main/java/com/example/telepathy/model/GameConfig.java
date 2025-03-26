package com.example.telepathy.model;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class GameConfig {
    private int timeLimit; // Time limit for word selection in seconds
    private int maxPlayers;
    private int livesPerPlayer;
    private List<String> categories;
    private String selectedCategory;

    // Default constructor for Firebase
    public GameConfig() {
        this.timeLimit = 30;
        this.maxPlayers = 8;
        this.livesPerPlayer = 3;
        this.categories = new ArrayList<>();
        this.selectedCategory = null; // Will be set later
    }

    // Constructor with custom parameters
    public GameConfig(int timeLimit, int maxPlayers, int livesPerPlayer, String selectedCategory) {
        this.timeLimit = timeLimit;
        this.maxPlayers = maxPlayers;
        this.livesPerPlayer = livesPerPlayer;
        this.categories = new ArrayList<>();
        this.selectedCategory = selectedCategory;
    }

    public void loadCategoriesFromDatabase(Database database, Runnable onComplete) {
        database.getReference().child("category").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    categories.add(child.getKey());
                }
                if (!categories.isEmpty() && selectedCategory == null) {
                    selectedCategory = categories.get(0); // fallback
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GameConfig", "Failed to load categories: " + error.getMessage());
            }
        });
    }

    // Getters and Setters
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


    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }
}
