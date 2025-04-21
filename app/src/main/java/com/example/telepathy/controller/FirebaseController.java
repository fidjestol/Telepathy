package com.example.telepathy.controller;

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
    public void updatePlayerData(String gameId, String playerId, Map<String, Object> updates, FirebaseCallback callback) {
        if (gameId == null || gameId.isEmpty() || playerId == null || playerId.isEmpty() || updates == null || updates.isEmpty()) {
            callback.onFailure("Invalid parameters for player update");
            return;
        }

        database.child("games").child(gameId).child("players").child(playerId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Failed to update player data");
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
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Failed to update game data");
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
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Failed to retrieve lobby");
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

    public void deleteLobby(String lobbyId, FirebaseCallback callback) {
        if (lobbyId == null || lobbyId.isEmpty()) {
            callback.onFailure("Lobby ID is invalid");
            return;
        }

        database.child("lobbies").child(lobbyId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("Failed to delete lobby");
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

                            // Get config data - CHECK THE TYPE!
                            Object configObj = lobbyData.get("gameConfig");
                            Map<String, Object> configData;

                            if (configObj instanceof Map) {
                                configData = (Map<String, Object>) configObj;
                            } else {
                                // Use default config
                                configData = new HashMap<>();
                                configData.put("timeLimit", 30);
                                configData.put("maxPlayers", 8);
                                configData.put("livesPerPlayer", 3);
                                configData.put("selectedCategory", "Animals");
                            }

                            // Create game data
                            Map<String, Object> gameData = new HashMap<>();
                            gameData.put("lobbyId", lobbyId);
                            gameData.put("players", playersMap);
                            gameData.put("config", configData);
                            gameData.put("status", "active");

                            // Create first round
                            Map<String, Object> roundData = new HashMap<>();
                            roundData.put("roundNumber", 1);
                            roundData.put("startTime", System.currentTimeMillis());

                            // Get time limit
                            long timeLimit = 30;
                            Object timeLimitObj = configData.get("timeLimit");
                            if (timeLimitObj instanceof Number) {
                                timeLimit = ((Number) timeLimitObj).longValue();
                            }

                            roundData.put("endTime", System.currentTimeMillis() + (timeLimit * 1000));

                            // Get category for word selection
                            String category = "Animals";
                            Object categoryObj = configData.get("selectedCategory");
                            if (categoryObj instanceof String) {
                                category = (String) categoryObj;
                            }

                            // Get ALL words for the round (not just random subset)
                            List<String> words = WordSelection.getAllWordsForCategory(category);
                            roundData.put("words", words);
                            System.out.println("TELEPATHY: Added " + words.size() + " words for category: " + category);

                            gameData.put("currentRound", roundData);

                            // Save game data
                            database.child("games").child(gameId).setValue(gameData)
                                    .addOnCompleteListener(gameTask -> {
                                        if (gameTask.isSuccessful()) {
                                            // Update lobby with game reference
                                            lobbyUpdates.put("gameId", gameId);

                                            database.child("lobbies").child(lobbyId)
                                                    .updateChildren(lobbyUpdates)
                                                    .addOnCompleteListener(lobbyTask -> {
                                                        if (lobbyTask.isSuccessful()) {
                                                            callback.onSuccess(gameId);
                                                        } else {
                                                            callback.onFailure("Failed to update lobby: " +
                                                                    (lobbyTask.getException() != null ?
                                                                            lobbyTask.getException().getMessage() : "unknown error"));
                                                        }
                                                    });
                                        } else {
                                            callback.onFailure("Failed to create game: " +
                                                    (gameTask.getException() != null ?
                                                            gameTask.getException().getMessage() : "unknown error"));
                                        }
                                    });
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("START GAME ERROR: " + e.getMessage());
                            callback.onFailure("Error starting game: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Lobby not found");
                    }
                });
    }

    // Helper method to get words for a category
    private List<String> getWordsForCategory(String category, int count) {
        return com.example.telepathy.model.WordSelection.getRandomWords(category, count);
    }

    // Replace the endCurrentRound method in FirebaseController.java to automatically schedule the next round

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

                            // First, get all the players
                            Map<String, Object> playersData = (Map<String, Object>) gameData.get("players");

                            // Keep track of updates to make
                            Map<String, Object> updates = new HashMap<>();
                            // Use an array to track active players (can be modified inside the lambda)
                            final int[] activePlayerCount = {0};
                            final int[] playersThatWillBeEliminated = {0}; // Track players that will be eliminated this round

                            // Track word frequencies to identify unique words
                            Map<String, List<String>> wordToPlayers = new HashMap<>();

                            // Process each player to count word frequencies and active players
                            if (playersData != null) {
                                for (Map.Entry<String, Object> entry : playersData.entrySet()) {
                                    String playerId = entry.getKey();
                                    Map<String, Object> playerData = (Map<String, Object>) entry.getValue();

                                    // Skip already eliminated players
                                    Object eliminatedObj = playerData.get("eliminated");
                                    boolean isEliminated = (eliminatedObj instanceof Boolean && (Boolean) eliminatedObj);
                                    if (isEliminated) continue;

                                    // Count active players
                                    activePlayerCount[0]++;

                                    // Track word frequencies
                                    Object currentWordObj = playerData.get("currentWord");
                                    String currentWord = (currentWordObj instanceof String) ? (String) currentWordObj : "";

                                    if (currentWord != null && !currentWord.isEmpty()) {
                                        if (!wordToPlayers.containsKey(currentWord)) {
                                            wordToPlayers.put(currentWord, new ArrayList<>());
                                        }
                                        wordToPlayers.get(currentWord).add(playerId);
                                    }
                                }
                            }

                            System.out.println("TELEPATHY_DEBUG: Initial active player count: " + activePlayerCount[0]);

                            // Process each player again to apply score updates and penalties
                            if (playersData != null) {
                                for (Map.Entry<String, Object> entry : playersData.entrySet()) {
                                    String playerId = entry.getKey();
                                    Map<String, Object> playerData = (Map<String, Object>) entry.getValue();

                                    // Check if player is already eliminated
                                    Object eliminatedObj = playerData.get("eliminated");
                                    boolean isEliminated = (eliminatedObj instanceof Boolean && (Boolean) eliminatedObj);

                                    if (!isEliminated) {
                                        // Check if player submitted a word
                                        Object currentWordObj = playerData.get("currentWord");
                                        String currentWord = (currentWordObj instanceof String) ? (String) currentWordObj : "";

                                        if (currentWord == null || currentWord.isEmpty()) {
                                            // Player didn't submit - penalize them
                                            int currentLives = 3; // Default
                                            Object livesObj = playerData.get("lives");

                                            if (livesObj instanceof Long) {
                                                currentLives = ((Long) livesObj).intValue();
                                            } else if (livesObj instanceof Integer) {
                                                currentLives = (Integer) livesObj;
                                            }

                                            // Reduce lives by 1
                                            int newLives = Math.max(0, currentLives - 1);

                                            // Add to update batch
                                            updates.put("players/" + playerId + "/lives", newLives);

                                            // If player is eliminated, mark them
                                            if (newLives <= 0) {
                                                updates.put("players/" + playerId + "/eliminated", true);
                                                playersThatWillBeEliminated[0]++; // Count this player as soon-to-be eliminated
                                                System.out.println("TELEPATHY_DEBUG: Player " + playerData.get("username") +
                                                        " will be eliminated for not submitting a word");
                                            }
                                        } else {
                                            // Check if this word is unique (only submitted by this player)
                                            List<String> playersWithSameWord = wordToPlayers.get(currentWord);
                                            if (playersWithSameWord != null && playersWithSameWord.size() == 1) {
                                                // Word is unique! Award points to this player
                                                int currentScore = 0;
                                                Object scoreObj = playerData.get("score");
                                                if (scoreObj instanceof Long) {
                                                    currentScore = ((Long) scoreObj).intValue();
                                                } else if (scoreObj instanceof Integer) {
                                                    currentScore = (Integer) scoreObj;
                                                }

                                                // Award 10 points for a unique word
                                                int newScore = currentScore + 10;
                                                updates.put("players/" + playerId + "/score", newScore);
                                            }
                                        }
                                    }
                                }
                            }

                            // Process duplicate words and eliminate players if needed
                            if (playersData != null) {
                                for (Map.Entry<String, List<String>> wordEntry : wordToPlayers.entrySet()) {
                                    String word = wordEntry.getKey();
                                    List<String> playerIds = wordEntry.getValue();

                                    // Only process words submitted by multiple players
                                    if (playerIds.size() > 1) {
                                        System.out.println("TELEPATHY_DEBUG: Duplicate word found: " + word +
                                                " submitted by " + playerIds.size() + " players");

                                        // Process each player with this duplicate word
                                        for (String playerId : playerIds) {
                                            Map<String, Object> playerData = (Map<String, Object>) playersData.get(playerId);
                                            if (playerData == null) continue;

                                            // Skip already eliminated players
                                            Object eliminatedObj = playerData.get("eliminated");
                                            boolean isEliminated = (eliminatedObj instanceof Boolean && (Boolean) eliminatedObj);
                                            if (isEliminated) continue;

                                            // Get current lives
                                            int currentLives = 3; // Default
                                            Object livesObj = playerData.get("lives");
                                            if (livesObj instanceof Long) {
                                                currentLives = ((Long) livesObj).intValue();
                                            } else if (livesObj instanceof Integer) {
                                                currentLives = (Integer) livesObj;
                                            }

                                            // Reduce lives by 1 for duplicate word
                                            int newLives = Math.max(0, currentLives - 1);
                                            updates.put("players/" + playerId + "/lives", newLives);

                                            // If player is eliminated, mark them
                                            if (newLives <= 0) {
                                                updates.put("players/" + playerId + "/eliminated", true);
                                                playersThatWillBeEliminated[0]++; // Count this player as soon-to-be eliminated
                                                System.out.println("TELEPATHY_DEBUG: Player " + playerData.get("username") +
                                                        " will be eliminated for submitting duplicate word: " + word);
                                            }
                                        }
                                    }
                                }
                            }

                            // Update game status
                            updates.put("status", "roundEnd");

                            // Calculate how many players will remain after this round
                            int playersRemaining = activePlayerCount[0] - playersThatWillBeEliminated[0];
                            System.out.println("TELEPATHY_DEBUG: Active players: " + activePlayerCount[0] +
                                    ", Players being eliminated: " + playersThatWillBeEliminated[0] +
                                    ", Players that will remain: " + playersRemaining);

                            // Apply all updates in a single batch
                            database.child("games").child(gameId).updateChildren(updates)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            System.out.println("TELEPATHY: Round ended with " + playersRemaining + " players remaining");

                                            // Add logic to handle game end or schedule next round
                                            if (playersRemaining <= 1) {
                                                // Game over - one or zero players left
                                                System.out.println("TELEPATHY_DEBUG: Game should end now - only " + playersRemaining + " player(s) remaining");
                                                database.child("games").child(gameId).child("status").setValue("gameEnd")
                                                        .addOnCompleteListener(endTask -> {
                                                            if (endTask.isSuccessful()) {
                                                                System.out.println("TELEPATHY_DEBUG: Game status successfully set to gameEnd");
                                                            } else {
                                                                System.out.println("TELEPATHY_DEBUG: Failed to set game status to gameEnd: " +
                                                                        (endTask.getException() != null ? endTask.getException().getMessage() : "unknown error"));
                                                            }
                                                        });
                                            } else {
                                                // Schedule next round
                                                Map<String, Object> roundUpdates = new HashMap<>();
                                                roundUpdates.put("nextRoundStartTime", System.currentTimeMillis() + 5000);
                                                roundUpdates.put("roundStarterId", UUID.randomUUID().toString());
                                                database.child("games").child(gameId).updateChildren(roundUpdates);
                                            }

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

// Fix for startNextRound method in FirebaseController.java to prevent round skipping and word list issues

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
                                System.out.println("TELEPATHY: Cannot start next round - game status is " + status + " instead of roundEnd");
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

                            // Get game config for category
                            Map<String, Object> configData = (Map<String, Object>) gameData.get("config");
                            String category = "Animals"; // Default

                            if (configData != null && configData.containsKey("selectedCategory")) {
                                category = (String) configData.get("selectedCategory");
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

                            // Get ALL words for this round (not just random subset)
                            List<String> words = WordSelection.getAllWordsForCategory(category);
                            newRoundData.put("words", words);

                            System.out.println("TELEPATHY: Using " + words.size() + " words for round " + nextRoundNumber);

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
                                            System.out.println("TELEPATHY: Successfully started round " + nextRoundNumber);
                                            callback.onSuccess(null);
                                        } else {
                                            System.out.println("TELEPATHY: Failed to start round: " +
                                                    (updateTask.getException() != null ?
                                                            updateTask.getException().getMessage() : "unknown error"));
                                            callback.onFailure("Failed to start next round: " +
                                                    (updateTask.getException() != null ?
                                                            updateTask.getException().getMessage() : "unknown error"));
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
    }    public void removeGameListener(String gameId, ValueEventListener listener) {
        database.child("games").child(gameId).removeEventListener(listener);
    }


}