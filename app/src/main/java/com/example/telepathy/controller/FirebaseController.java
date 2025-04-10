package com.example.telepathy.controller;

import androidx.annotation.NonNull;

import com.example.telepathy.model.Database;
import com.example.telepathy.model.User;
import com.example.telepathy.model.WordSelection;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.telepathy.model.GameConfig;
import com.example.telepathy.model.Lobby;
import com.example.telepathy.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FirebaseController {
    private static FirebaseController instance;
    private final FirebaseAuth auth;
    private final DatabaseReference database;

    /**
     * Get a specific lobby by its ID
     * @param lobbyId The ID of the lobby to retrieve
     * @param callback Callback to handle the result
     */
    public void getLobbyById(String lobbyId, FirebaseCallback callback) {
        if (lobbyId == null || lobbyId.isEmpty()) {
            callback.onFailure("Invalid lobby ID");
            return;
        }

        database.child("lobbies").child(lobbyId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Lobby lobby = task.getResult().getValue(Lobby.class);
                        if (lobby != null) {
                            callback.onSuccess(lobby);
                        } else {
                            callback.onFailure("Lobby not found");
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Failed to retrieve lobby");
                    }
                });
    }

    // Interface for callbacks
    public interface FirebaseCallback {
        void onSuccess(Object result);
        void onFailure(String error);
    }

    private FirebaseController() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized FirebaseController getInstance() {
        if (instance == null) {
            instance = new FirebaseController();
        }
        return instance;
    }

    // Authentication methods
    public void registerUser(String email, String password, String username, FirebaseCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Create player profile
                            User newUser = new User(firebaseUser.getUid(),username);

                            // Save to database
                            database.child("users").child(firebaseUser.getUid()).setValue(newUser)
                                    .addOnCompleteListener(saveTask -> {
                                        if (saveTask.isSuccessful()) {
                                            callback.onSuccess(newUser);
                                        } else {
                                            callback.onFailure("Failed to save user data");
                                        }
                                    });
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Registration failed");
                    }
                });
    }

    public void loginUser(String email, String password, FirebaseCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            database.child("users").child(firebaseUser.getUid()).get()
                                    .addOnCompleteListener(dataTask -> {
                                        if (dataTask.isSuccessful() && dataTask.getResult() != null) {
                                            User user = dataTask.getResult().getValue(User.class);
                                            callback.onSuccess(user);
                                        } else {
                                            callback.onFailure("Failed to get user data");
                                        }
                                    });
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Login failed");
                    }
                });
    }

    public void logoutUser() {
        auth.signOut();
    }

    // Lobby methods
    public void createLobby(String lobbyName, Player host, @NonNull String requestedCategory, FirebaseCallback callback) {
        GameConfig config = new GameConfig();

        config.loadCategoriesFromDatabase(Database.getInstance(), () -> {
            List<String> categories = config.getCategories();

            // Check if the requested category is valid
            if (requestedCategory != null && categories.contains(requestedCategory)) {
                config.setSelectedCategory(requestedCategory);
            } else if (!categories.isEmpty()) {
                // Fallback: pick a random category
                int randomIndex = new Random().nextInt(categories.size());
                config.setSelectedCategory(categories.get(randomIndex));
            }

            // Create and save the lobby
            Lobby lobby = new Lobby(lobbyName, host);
            lobby.setGameConfig(config);

            database.child("lobbies").child(lobby.getId()).setValue(lobby)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess(lobby);
                        } else {
                            callback.onFailure("Failed to create lobby");
                        }
                    });
        });
    }



    public void getLobbyList(FirebaseCallback callback) {
        database.child("lobbies").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Lobby> lobbies = new ArrayList<>();
                for (DataSnapshot lobbySnapshot : snapshot.getChildren()) {
                    Lobby lobby = lobbySnapshot.getValue(Lobby.class);
                    if (lobby != null && lobby.isOpen()) {
                        lobbies.add(lobby);
                    }
                }
                callback.onSuccess(lobbies);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    public void joinLobby(String lobbyId, Player player, FirebaseCallback callback) {
        database.child("lobbies").child(lobbyId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Lobby lobby = task.getResult().getValue(Lobby.class);
                        if (lobby != null && lobby.isOpen() && !lobby.isLobbyFull()) {
                            // Add player to lobby
                            lobby.addPlayer(player);

                            // Update lobby in database
                            database.child("lobbies").child(lobbyId).setValue(lobby)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            callback.onSuccess(lobby);
                                        } else {
                                            callback.onFailure("Failed to join lobby");
                                        }
                                    });
                        } else {
                            callback.onFailure("Lobby is full or closed");
                        }
                    } else {
                        callback.onFailure("Lobby not found");
                    }
                });
    }

    public void leaveLobby(String lobbyId, String playerId, FirebaseCallback callback) {
        database.child("lobbies").child(lobbyId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Lobby lobby = task.getResult().getValue(Lobby.class);
                        if (lobby != null) {
                            // Remove player from lobby
                            boolean removed = lobby.removePlayer(playerId);

                            if (removed) {
                                // If no players left, delete lobby
                                if (lobby.getPlayers().isEmpty()) {
                                    database.child("lobbies").child(lobbyId).removeValue()
                                            .addOnCompleteListener(deleteTask -> {
                                                if (deleteTask.isSuccessful()) {
                                                    callback.onSuccess(null);
                                                } else {
                                                    callback.onFailure("Failed to delete empty lobby");
                                                }
                                            });
                                } else {
                                    // Update lobby in database
                                    database.child("lobbies").child(lobbyId).setValue(lobby)
                                            .addOnCompleteListener(updateTask -> {
                                                if (updateTask.isSuccessful()) {
                                                    callback.onSuccess(lobby);
                                                } else {
                                                    callback.onFailure("Failed to update lobby");
                                                }
                                            });
                                }
                            } else {
                                callback.onFailure("Player not found in lobby");
                            }
                        } else {
                            callback.onFailure("Lobby not found");
                        }
                    } else {
                        callback.onFailure("Lobby not found");
                    }
                });
    }

    public void startGame(String lobbyId, FirebaseCallback callback) {
        database.child("lobbies").child(lobbyId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        try {
                            // Get lobby data
                            DataSnapshot snapshot = task.getResult();
                            Object lobbyDataObj = snapshot.getValue();

                            // Add debug info
                            System.out.println("LOBBY DATA TYPE: " + (lobbyDataObj != null ? lobbyDataObj.getClass().getName() : "null"));

                            // Check data type before casting
                            if (!(lobbyDataObj instanceof Map)) {
                                callback.onFailure("Invalid lobby data format: " +
                                        (lobbyDataObj != null ? lobbyDataObj.getClass().getName() : "null"));
                                return;
                            }

                            Map<String, Object> lobbyData = (Map<String, Object>) lobbyDataObj;

                            // Get players data - CHECK THE TYPE!
                            Object playersObj = lobbyData.get("players");

                            // Debug info
                            System.out.println("PLAYERS DATA TYPE: " + (playersObj != null ? playersObj.getClass().getName() : "null"));

                            // Handle players data based on its actual type
                            Map<String, Object> playersMap;
                            if (playersObj instanceof Map) {
                                playersMap = (Map<String, Object>) playersObj;
                            } else if (playersObj instanceof List) {
                                // Convert List to Map if needed
                                List<Object> playersList = (List<Object>) playersObj;
                                playersMap = new HashMap<>();
                                for (int i = 0; i < playersList.size(); i++) {
                                    Object player = playersList.get(i);
                                    if (player instanceof Map) {
                                        Map<String, Object> playerMap = (Map<String, Object>) player;
                                        String id = (String) playerMap.get("id");
                                        if (id != null) {
                                            playersMap.put(id, player);
                                        } else {
                                            playersMap.put("player" + i, player);
                                        }
                                    }
                                }
                            } else {
                                callback.onFailure("Invalid players data format: " +
                                        (playersObj != null ? playersObj.getClass().getName() : "null"));
                                return;
                            }

                            if (playersMap.size() < 2) {
                                callback.onFailure("Cannot start game: not enough players");
                                return;
                            }

                            // Close the lobby
                            Map<String, Object> lobbyUpdates = new HashMap<>();
                            lobbyUpdates.put("open", false);

                            // Create game ID
                            String gameId = database.child("games").push().getKey();
                            if (gameId == null) {
                                callback.onFailure("Failed to generate game ID");
                                return;
                            }

                            // 👇 Use dynamic word loading from Firebase
                            com.example.telepathy.model.WordSelection.getRandomWords(
                                    lobby.getGameConfig().getSelectedCategory(), 20, words -> {

                                        // Game metadata
                                        Map<String, Object> gameData = new HashMap<>();
                                        gameData.put("lobbyId", lobbyId);
                                        gameData.put("players", lobby.getPlayers());
                                        gameData.put("config", lobby.getGameConfig());
                                        gameData.put("status", "active");

                                        // Round metadata
                                        Map<String, Object> roundData = new HashMap<>();
                                        roundData.put("roundNumber", 1);
                                        roundData.put("startTime", System.currentTimeMillis());
                                        roundData.put("endTime", System.currentTimeMillis() + (lobby.getGameConfig().getTimeLimit() * 1000L));
                                        roundData.put("words", words);

                                        gameData.put("currentRound", roundData);

                                        // Save game data
                                        database.child("games").child(gameId).setValue(gameData)
                                                .addOnCompleteListener(gameTask -> {
                                                    if (gameTask.isSuccessful()) {
                                                        // Update lobby
                                                        lobbyUpdates.put("gameId", gameId);
                                                        database.child("lobbies").child(lobbyId)
                                                                .updateChildren(lobbyUpdates)
                                                                .addOnCompleteListener(lobbyTask -> {
                                                                    if (lobbyTask.isSuccessful()) {
                                                                        callback.onSuccess(gameId);
                                                                    } else {
                                                                        callback.onFailure("Failed to update lobby");
                                                                    }
                                                                });
                                                    } else {
                                                        callback.onFailure("Failed to create game");
                                                    }
                                                });
                                    });

                        } else {
                            callback.onFailure("Cannot start game: not enough players");
                        }
                    } else {
                        callback.onFailure("Lobby not found");
                    }
                });
    }    // Helper method to get words for a category
    private List<String> getWordsForCategory(String category, int count) {
        // This should be implemented in your WordSelection class
        // For now, adding a temporary implementation
        return com.example.telepathy.model.WordSelection.getRandomWords(category, count);
    }
    // Game methods
    public void submitWord(String gameId, String playerId, String word, FirebaseCallback callback) {
        database.child("games").child(gameId).child("players").child(playerId).child("currentWord")
                .setValue(word)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("Failed to submit word");
                    }
                });
    }

    public void listenForGameUpdates(String gameId, ValueEventListener listener) {
        database.child("games").child(gameId).addValueEventListener(listener);
    }

    public void removeGameListener(String gameId, ValueEventListener listener) {
        database.child("games").child(gameId).removeEventListener(listener);
    }
}