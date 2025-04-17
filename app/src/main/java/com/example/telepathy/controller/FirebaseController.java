package com.example.telepathy.controller;

import android.util.Log;
import androidx.annotation.NonNull;

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
import java.util.UUID;

public class FirebaseController {
    private static FirebaseController instance;
    private FirebaseAuth auth;
    private DatabaseReference database;

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

    // Helper method to update player data
    public void updatePlayerData(String gameId, String playerId, Map<String, Object> updates,
            FirebaseCallback callback) {
        if (gameId == null || gameId.isEmpty() || playerId == null || playerId.isEmpty() || updates == null
                || updates.isEmpty()) {
            callback.onFailure("Invalid parameters for player update");
            return;
        }

        database.child("games").child(gameId).child("players").child(playerId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage()
                                : "Failed to update player data");
                    }
                });
    }

    // Helper method to update game data
    public void updateGameData(String gameId, Map<String, Object> updates, FirebaseCallback callback) {
        if (gameId == null || gameId.isEmpty() || updates == null || updates.isEmpty()) {
            callback.onFailure("Invalid parameters for game update");
            return;
        }

        database.child("games").child(gameId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage()
                                : "Failed to update game data");
                    }
                });
    }

    // Get a specific lobby by its ID
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
                        callback.onFailure(task.getException() != null ? task.getException().getMessage()
                                : "Failed to retrieve lobby");
                    }
                });
    }

    // Authentication methods
    public void registerUser(String email, String password, String username, FirebaseCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Create player profile
                            User newUser = new User(firebaseUser.getUid(), username);

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
                        callback.onFailure(
                                task.getException() != null ? task.getException().getMessage() : "Registration failed");
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
                        callback.onFailure(
                                task.getException() != null ? task.getException().getMessage() : "Login failed");
                    }
                });
    }

    public void logoutUser() {
        auth.signOut();
    }

    // Lobby methods
    public void createLobby(String lobbyName, Player host, FirebaseCallback callback) {
        Lobby lobby = new Lobby(lobbyName, host);

        database.child("lobbies").child(lobby.getId()).setValue(lobby)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(lobby);
                    } else {
                        callback.onFailure("Failed to create lobby");
                    }
                });
    }

    public void createLobbyWithConfig(Lobby lobby, FirebaseCallback callback) {
        database.child("lobbies").child(lobby.getId()).setValue(lobby)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(lobby);
                    } else {
                        callback.onFailure("Failed to create lobby");
                    }
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
        Log.d("Telepathy", "Starting game creation for lobby: " + lobbyId);
        // Get lobby data first
        database.child("lobbies").child(lobbyId).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Log.e("Telepathy", "Lobby not found: " + lobbyId);
                callback.onFailure("Lobby not found");
                return;
            }

            Map<String, Object> lobbyData = (Map<String, Object>) task.getResult().getValue();
            if (lobbyData == null) {
                Log.e("Telepathy", "Invalid lobby data for: " + lobbyId);
                callback.onFailure("Invalid lobby data");
                return;
            }

            Log.d("Telepathy", "Got lobby data, processing players...");
            // Get players
            Object playersObj = lobbyData.get("players");
            Map<String, Object> playersMap = new HashMap<>();

            if (playersObj instanceof Map) {
                playersMap = (Map<String, Object>) playersObj;
                Log.d("Telepathy", "Found " + playersMap.size() + " players in map format");
            } else if (playersObj instanceof List) {
                // Convert List to Map if needed
                List<Object> playersList = (List<Object>) playersObj;
                Log.d("Telepathy", "Found " + playersList.size() + " players in list format");
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
                Log.e("Telepathy", "Invalid players data format: "
                        + (playersObj != null ? playersObj.getClass().getName() : "null"));
                callback.onFailure("Invalid players data format");
                return;
            }

            // Get config data
            Object configObj = lobbyData.get("gameConfig");
            Map<String, Object> configData;
            if (configObj instanceof Map) {
                configData = (Map<String, Object>) configObj;
                Log.d("Telepathy", "Using custom game config");
            } else {
                // Use default config
                Log.d("Telepathy", "Using default game config");
                configData = new HashMap<>();
                configData.put("timeLimit", 30);
                configData.put("maxPlayers", 8);
                configData.put("livesPerPlayer", 3);
                configData.put("selectedCategory", "Animals");
                configData.put("gameMode", "Classic");
            }

            // Validate player count based on game mode
            String gameMode = (String) configData.getOrDefault("gameMode", "Classic");
            int maxPlayers = gameMode.equalsIgnoreCase("Matching") ? 2 : 8;
            Log.d("Telepathy", "Game mode: " + gameMode + ", Players: " + playersMap.size() + "/" + maxPlayers);

            if (gameMode.equalsIgnoreCase("Matching")) {
                if (playersMap.size() != 2) {
                    Log.e("Telepathy", "Invalid player count for Matching mode: " + playersMap.size());
                    callback.onFailure("Matching mode requires exactly 2 players");
                    return;
                }
            } else if (playersMap.size() < 1 || playersMap.size() > maxPlayers) {
                Log.e("Telepathy", "Invalid player count for " + gameMode + " mode: " + playersMap.size());
                callback.onFailure("Invalid number of players for " + gameMode + " mode");
                return;
            }

            // Create game ID
            String gameId = database.child("games").push().getKey();
            if (gameId == null) {
                Log.e("Telepathy", "Failed to generate game ID");
                callback.onFailure("Failed to generate game ID");
                return;
            }
            Log.d("Telepathy", "Generated new game ID: " + gameId);

            // Create game data
            Map<String, Object> gameData = new HashMap<>();
            gameData.put("id", gameId);
            gameData.put("config", configData);
            gameData.put("status", "active");

            // Initialize players with correct lives from config
            Map<String, Object> playersData = new HashMap<>();
            int livesPerPlayer = ((Number) configData.getOrDefault("livesPerPlayer", 3)).intValue();

            for (Map.Entry<String, Object> entry : playersMap.entrySet()) {
                Map<String, Object> playerData = (Map<String, Object>) entry.getValue();
                playerData.put("lives", livesPerPlayer);
                playerData.put("score", 0);
                playerData.put("eliminated", false);
                playerData.put("currentWord", "");
                playersData.put(entry.getKey(), playerData);
            }
            gameData.put("players", playersData);

            // Create initial round data
            Map<String, Object> roundData = new HashMap<>();
            roundData.put("roundNumber", 1);
            roundData.put("startTime", System.currentTimeMillis());
            roundData.put("endTime",
                    System.currentTimeMillis() + ((Number) configData.get("timeLimit")).intValue() * 1000);
            roundData.put("playerWords", new HashMap<>());

            // Initialize game mode specific data
            if (gameMode.equalsIgnoreCase("matching")) {
                // For matching mode, we don't need any word lists or validation
                // We just need to track the words players submit and check if they match
                roundData.put("playerWords", new HashMap<String, String>());
            } else {
                // For classic mode, get words from the selected category
                String category = (String) configData.getOrDefault("selectedCategory", "Animals");
                List<String> wordList = WordSelection.getRandomWords(category, 20);
                roundData.put("words", wordList);
            }

            gameData.put("currentRound", roundData);

            // Close the lobby
            Map<String, Object> lobbyUpdates = new HashMap<>();
            lobbyUpdates.put("open", false);
            lobbyUpdates.put("gameId", gameId);

            // Perform the updates
            Map<String, Object> updates = new HashMap<>();
            updates.put("/games/" + gameId, gameData);
            updates.put("/lobbies/" + lobbyId, lobbyUpdates);

            Log.d("Telepathy", "Attempting to create game in Firebase...");
            database.updateChildren(updates).addOnCompleteListener(updateTask -> {
                if (updateTask.isSuccessful()) {
                    Log.d("Telepathy", "Game created successfully with ID: " + gameId);
                    callback.onSuccess(gameId);
                } else {
                    Log.e("Telepathy", "Failed to create game: " +
                            (updateTask.getException() != null ? updateTask.getException().getMessage()
                                    : "unknown error"));
                    callback.onFailure("Failed to start game: " +
                            (updateTask.getException() != null ? updateTask.getException().getMessage()
                                    : "unknown error"));
                }
            });
        });
    }

    // Helper method to get words for a category
    private List<String> getWordsForCategory(String category, int count) {
        return com.example.telepathy.model.WordSelection.getRandomWords(category, count);
    }

    // Replace the endCurrentRound method in FirebaseController.java to
    // automatically schedule the next round

    public void endCurrentRound(String gameId, FirebaseCallback callback) {
        System.out.println("TELEPATHY: Ending current round for game " + gameId);

        database.child("games").child(gameId).get()
                .addOnCompleteListener(getTask -> {
                    if (getTask.isSuccessful() && getTask.getResult() != null) {
                        try {
                            DataSnapshot snapshot = getTask.getResult();
                            Map<String, Object> gameData = (Map<String, Object>) snapshot.getValue();

                            if (gameData == null) {
                                callback.onFailure("Game data is null");
                                return;
                            }

                            Map<String, Object> configData = (Map<String, Object>) gameData.get("config");
                            String gameMode = (String) configData.getOrDefault("gameMode", "Classic");

                            // Get all the players
                            Map<String, Object> playersData = (Map<String, Object>) gameData.get("players");
                            Map<String, Object> updates = new HashMap<>();

                            if (gameMode.equalsIgnoreCase("matching")) {
                                // For matching mode, check if both players submitted the same word
                                Map<String, String> submittedWords = new HashMap<>();

                                // Collect all submitted words with player IDs
                                for (Map.Entry<String, Object> entry : playersData.entrySet()) {
                                    String playerId = entry.getKey();
                                    Map<String, Object> playerData = (Map<String, Object>) entry.getValue();
                                    String currentWord = (String) playerData.get("currentWord");
                                    if (currentWord != null && !currentWord.trim().isEmpty()) {
                                        submittedWords.put(playerId, currentWord.toLowerCase().trim());
                                    }
                                }

                                // Store the words in the round data for display
                                Map<String, Object> roundData = new HashMap<>();
                                roundData.put("submittedWords", submittedWords);
                                updates.put("currentRound/submittedWords", submittedWords);

                                // If both players submitted the same word, end the game
                                if (submittedWords.size() == 2) {
                                    String[] words = submittedWords.values().toArray(new String[0]);
                                    if (words[0].equals(words[1])) {
                                        updates.put("status", "gameEnd");
                                    } else {
                                        // Continue to next round
                                        updates.put("status", "roundEnd");
                                        updates.put("nextRoundStartTime", System.currentTimeMillis() + 5000);
                                    }
                                }
                            } else {
                                // Classic mode logic
                                final int[] activePlayerCount = { 0 };

                                for (Map.Entry<String, Object> entry : playersData.entrySet()) {
                                    String playerId = entry.getKey();
                                    Map<String, Object> playerData = (Map<String, Object>) entry.getValue();

                                    boolean isEliminated = (boolean) playerData.getOrDefault("eliminated", false);

                                    if (!isEliminated) {
                                        String currentWord = (String) playerData.get("currentWord");
                                        if (currentWord == null || currentWord.isEmpty()) {
                                            int currentLives = ((Number) playerData.get("lives")).intValue();
                                            int newLives = Math.max(0, currentLives - 1);
                                            updates.put("players/" + playerId + "/lives", newLives);

                                            if (newLives <= 0) {
                                                updates.put("players/" + playerId + "/eliminated", true);
                                            } else {
                                                activePlayerCount[0]++;
                                            }
                                        } else {
                                            activePlayerCount[0]++;
                                        }
                                    }
                                }

                                if (activePlayerCount[0] <= 1) {
                                    updates.put("status", "gameEnd");
                                } else {
                                    updates.put("status", "roundEnd");
                                    updates.put("nextRoundStartTime", System.currentTimeMillis() + 5000);
                                }
                            }

                            // Apply all updates
                            database.child("games").child(gameId).updateChildren(updates)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            callback.onSuccess(null);
                                        } else {
                                            callback.onFailure("Failed to update game status");
                                        }
                                    });

                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.onFailure("Error processing round end: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Failed to get game data");
                    }
                });
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

    // Fix for startNextRound method in FirebaseController.java to prevent round
    // skipping and word list issues

    public void startNextRound(String gameId, FirebaseCallback callback) {
        // Debug logging
        System.out.println("TELEPATHY: Starting next round for game " + gameId);

        // First get the current game data
        database.child("games").child(gameId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        try {
                            DataSnapshot snapshot = task.getResult();

                            Map<String, Object> gameData = (Map<String, Object>) snapshot.getValue();
                            if (gameData == null) {
                                callback.onFailure("Game data is null");
                                return;
                            }

                            // Get current status - only proceed if status is roundEnd
                            String status = (String) gameData.get("status");
                            if (!"roundEnd".equals(status)) {
                                System.out.println("TELEPATHY: Cannot start next round - game status is " + status
                                        + " instead of roundEnd");
                                callback.onFailure("Game is not in roundEnd status");
                                return;
                            }

                            // Extract current round number - important to get the EXACT current round
                            Map<String, Object> currentRoundData = (Map<String, Object>) gameData.get("currentRound");
                            int currentRoundNumber = 1;

                            if (currentRoundData != null && currentRoundData.containsKey("roundNumber")) {
                                Object roundNumberObj = currentRoundData.get("roundNumber");
                                if (roundNumberObj instanceof Long) {
                                    currentRoundNumber = ((Long) roundNumberObj).intValue();
                                } else if (roundNumberObj instanceof Integer) {
                                    currentRoundNumber = (Integer) roundNumberObj;
                                } else if (roundNumberObj instanceof Double) {
                                    currentRoundNumber = ((Double) roundNumberObj).intValue();
                                }
                            }

                            System.out.println("TELEPATHY: Current round number: " + currentRoundNumber);

                            // IMPORTANT: Create BRAND NEW round data with correct next round number
                            int nextRoundNumber = currentRoundNumber + 1;
                            System.out.println("TELEPATHY: Next round number will be: " + nextRoundNumber);

                            // Get game config for category and mode
                            Map<String, Object> configData = (Map<String, Object>) gameData.get("config");
                            String category = "Animals"; // Default
                            String gameMode = "classic"; // Default

                            if (configData != null) {
                                if (configData.containsKey("selectedCategory")) {
                                    category = (String) configData.get("selectedCategory");
                                }
                                if (configData.containsKey("gameMode")) {
                                    gameMode = ((String) configData.get("gameMode")).toLowerCase();
                                }
                            }

                            // Create new round data
                            Map<String, Object> newRoundData = new HashMap<>();
                            newRoundData.put("roundNumber", nextRoundNumber);
                            newRoundData.put("startTime", System.currentTimeMillis());

                            // Get time limit from config or use default
                            long timeLimit = 30;
                            if (configData != null && configData.containsKey("timeLimit")) {
                                Object timeLimitObj = configData.get("timeLimit");
                                if (timeLimitObj instanceof Long) {
                                    timeLimit = (Long) timeLimitObj;
                                } else if (timeLimitObj instanceof Integer) {
                                    timeLimit = ((Integer) timeLimitObj).longValue();
                                } else if (timeLimitObj instanceof Double) {
                                    timeLimit = ((Double) timeLimitObj).longValue();
                                }
                            }

                            newRoundData.put("endTime", System.currentTimeMillis() + (timeLimit * 1000));

                            // Initialize word list based on game mode
                            if (gameMode.equalsIgnoreCase("matching")) {
                                // For matching mode, we don't need a word list
                                newRoundData.put("words", new ArrayList<>());
                                newRoundData.put("requiresWordValidation", false);
                            } else {
                                // For classic mode, get words from the selected category
                                List<String> words = WordSelection.getRandomWords(category, 20);
                                newRoundData.put("words", words);
                                newRoundData.put("requiresWordValidation", true);
                            }

                            // Create combined update map
                            Map<String, Object> updates = new HashMap<>();

                            // Clear any round starter data
                            updates.put("nextRoundStartTime", null);
                            updates.put("roundStarterId", null);

                            // Clear all player word submissions
                            Map<String, Object> playersData = (Map<String, Object>) gameData.get("players");
                            if (playersData != null) {
                                for (String playerId : playersData.keySet()) {
                                    updates.put("players/" + playerId + "/currentWord", "");
                                }
                            }

                            // Add new round data and update status
                            updates.put("currentRound", newRoundData);
                            updates.put("status", "active");

                            // Use a single atomic update operation
                            database.child("games").child(gameId).updateChildren(updates)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            System.out.println(
                                                    "TELEPATHY: Successfully started round " + nextRoundNumber);
                                            callback.onSuccess(null);
                                        } else {
                                            System.out.println("TELEPATHY: Failed to start round: " +
                                                    (updateTask.getException() != null
                                                            ? updateTask.getException().getMessage()
                                                            : "unknown error"));
                                            callback.onFailure("Failed to start next round: " +
                                                    (updateTask.getException() != null
                                                            ? updateTask.getException().getMessage()
                                                            : "unknown error"));
                                        }
                                    });

                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("TELEPATHY: Error preparing next round: " + e.getMessage());
                            callback.onFailure("Error preparing next round: " + e.getMessage());
                        }
                    } else {
                        System.out.println("TELEPATHY: Failed to get game data: " +
                                (task.getException() != null ? task.getException().getMessage() : "unknown error"));
                        callback.onFailure("Failed to get game data");
                    }
                });
    }

    public void removeGameListener(String gameId, ValueEventListener listener) {
        database.child("games").child(gameId).removeEventListener(listener);
    }
}