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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                            System.out.println("LOBBY DATA TYPE: "
                                    + (lobbyDataObj != null ? lobbyDataObj.getClass().getName() : "null"));

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
                            System.out.println("PLAYERS DATA TYPE: "
                                    + (playersObj != null ? playersObj.getClass().getName() : "null"));

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
                                                                    (lobbyTask.getException() != null
                                                                            ? lobbyTask.getException().getMessage()
                                                                            : "unknown error"));
                                                        }
                                                    });
                                        } else {
                                            callback.onFailure("Failed to create game: " +
                                                    (gameTask.getException() != null
                                                            ? gameTask.getException().getMessage()
                                                            : "unknown error"));
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

    // Replace the endCurrentRound method in FirebaseController.java to
    // automatically schedule the next round

    // In FirebaseController.java, modify the endCurrentRound method:

    public void endCurrentRound(String gameId, FirebaseCallback callback) {
        System.out.println("TELEPATHY: Ending current round for game " + gameId);

        // Add a flag to track if we're already processing this round
        final String roundEndKey = "game_" + gameId + "_round_end";
        if (processingRounds.contains(roundEndKey)) {
            System.out.println("TELEPATHY_WARNING: Already processing round end for game " + gameId);
            callback.onSuccess(null);
            return;
        }
        processingRounds.add(roundEndKey);

        database.child("games").child(gameId).get()
                .addOnCompleteListener(getTask -> {
                    if (getTask.isSuccessful() && getTask.getResult() != null) {
                        try {
                            DataSnapshot snapshot = getTask.getResult();
                            Map<String, Object> gameData = (Map<String, Object>) snapshot.getValue();

                            if (gameData == null) {
                                processingRounds.remove(roundEndKey);
                                callback.onFailure("Game data is null");
                                return;
                            }

                            // Get game configuration to check mode
                            Map<String, Object> configData = (Map<String, Object>) gameData.get("config");
                            final boolean isMatchingMode;
                            if (configData != null) {
                                Object matchingModeObj = configData.get("matchingMode");
                                isMatchingMode = matchingModeObj instanceof Boolean && (Boolean) matchingModeObj;
                            } else {
                                isMatchingMode = false;
                            }

                            // First, get all the players
                            final Map<String, Object> playersData = (Map<String, Object>) gameData.get("players");

                            // Keep track of updates to make
                            final Map<String, Object> updates = new HashMap<>();
                            final int[] activePlayerCount = { 0 };
                            final int[] remainingPlayerCount = { 0 };

                            // Track word frequencies
                            final Map<String, List<String>> wordToPlayers = new HashMap<>();

                            // First pass: Build word frequency map and count active players
                            if (playersData != null) {
                                for (Map.Entry<String, Object> entry : playersData.entrySet()) {
                                    String playerId = entry.getKey();
                                    Map<String, Object> playerData = (Map<String, Object>) entry.getValue();

                                    if (playerData == null)
                                        continue;

                                    // Check if player is eliminated
                                    boolean isEliminated = false;
                                    Object eliminatedObj = playerData.get("eliminated");
                                    if (eliminatedObj instanceof Boolean) {
                                        isEliminated = (Boolean) eliminatedObj;
                                    }

                                    if (!isEliminated) {
                                        activePlayerCount[0]++;

                                        // Get player's word
                                        String word = (String) playerData.get("currentWord");
                                        if (word != null && !word.trim().isEmpty()) {
                                            word = word.trim().toLowerCase();
                                            if (!wordToPlayers.containsKey(word)) {
                                                wordToPlayers.put(word, new ArrayList<>());
                                            }
                                            wordToPlayers.get(word).add(playerId);
                                        }
                                    }
                                }
                            }

                            final boolean hasMatchingWords = checkForMatchingWords(wordToPlayers);

                            // Process words based on game mode
                            if (isMatchingMode) {
                                processMatchingMode(wordToPlayers, playersData, updates);
                                remainingPlayerCount[0] = activePlayerCount[0];
                            } else {
                                processClassicMode(wordToPlayers, playersData, updates, remainingPlayerCount);
                            }

                            // Update game status
                            updates.put("status", "roundEnd");

                            // Apply all updates in a single batch
                            database.child("games").child(gameId).updateChildren(updates)
                                    .addOnCompleteListener(task -> {
                                        processingRounds.remove(roundEndKey);

                                        if (task.isSuccessful()) {
                                            System.out.println("TELEPATHY: Round ended with " + remainingPlayerCount[0]
                                                    + " remaining players");

                                            // Check if game should end based on mode
                                            boolean shouldEndGame = isMatchingMode ? hasMatchingWords
                                                    : remainingPlayerCount[0] <= 1;

                                            if (shouldEndGame) {
                                                // Game over - either matching words found or one player left in classic
                                                // mode
                                                System.out.println("TELEPATHY_DEBUG: Game should end - " +
                                                        (isMatchingMode ? "matching words found!"
                                                                : "only " + remainingPlayerCount[0]
                                                                        + " player(s) remaining"));
                                                database.child("games").child(gameId).child("status")
                                                        .setValue("gameEnd");
                                            } else {
                                                // Schedule next round
                                                Map<String, Object> roundUpdates = new HashMap<>();
                                                roundUpdates.put("nextRoundStartTime",
                                                        System.currentTimeMillis() + 5000);
                                                roundUpdates.put("roundStarterId", UUID.randomUUID().toString());
                                                database.child("games").child(gameId).updateChildren(roundUpdates);
                                            }

                                            callback.onSuccess(null);
                                        } else {
                                            callback.onFailure("Failed to update game status");
                                        }
                                    });
                        } catch (Exception e) {
                            processingRounds.remove(roundEndKey);
                            e.printStackTrace();
                            callback.onFailure("Error processing round end: " + e.getMessage());
                        }
                    } else {
                        processingRounds.remove(roundEndKey);
                        callback.onFailure("Failed to get game data");
                    }
                });
    }

    private boolean checkForMatchingWords(Map<String, List<String>> wordToPlayers) {
        for (List<String> playerIds : wordToPlayers.values()) {
            if (playerIds.size() > 1) {
                return true;
            }
        }
        return false;
    }

    private void processMatchingMode(
            Map<String, List<String>> wordToPlayers,
            Map<String, Object> playersData,
            Map<String, Object> updates) {
        // In matching mode:
        // - Check if any words match
        // - No life loss
        // - Game ends when words match
        for (Map.Entry<String, List<String>> entry : wordToPlayers.entrySet()) {
            List<String> playerIds = entry.getValue();
            if (playerIds.size() > 1) {
                // Award points to all players who matched
                for (String playerId : playerIds) {
                    Map<String, Object> playerData = (Map<String, Object>) playersData.get(playerId);
                    if (playerData == null)
                        continue;

                    // Get current score
                    int currentScore = 0;
                    Object scoreObj = playerData.get("score");
                    if (scoreObj instanceof Long) {
                        currentScore = ((Long) scoreObj).intValue();
                    } else if (scoreObj instanceof Integer) {
                        currentScore = (Integer) scoreObj;
                    }

                    // Award 20 points for matching
                    int newScore = currentScore + 20;
                    updates.put("players/" + playerId + "/score", newScore);
                }
            }
        }
    }

    private void processClassicMode(
            Map<String, List<String>> wordToPlayers,
            Map<String, Object> playersData,
            Map<String, Object> updates,
            int[] remainingPlayerCount) {
        // Classic mode:
        // - Players lose lives for duplicate words
        // - Points for unique words only
        // Track the last active player
        String lastActivePlayerId = null;
        Map<String, Integer> playerLives = new HashMap<>(); // Track current lives for each player
        Map<String, Boolean> playerEliminated = new HashMap<>(); // Track elimination status

        // Initialize tracking maps with current values
        for (Map.Entry<String, Object> entry : playersData.entrySet()) {
            String playerId = entry.getKey();
            Map<String, Object> playerData = (Map<String, Object>) entry.getValue();

            if (playerData == null)
                continue;

            // Get current lives
            int lives = 3; // Default
            Object livesObj = playerData.get("lives");
            if (livesObj instanceof Long) {
                lives = ((Long) livesObj).intValue();
            } else if (livesObj instanceof Integer) {
                lives = (Integer) livesObj;
            }
            playerLives.put(playerId, lives);

            // Get current elimination status
            boolean isEliminated = false;
            Object eliminatedObj = playerData.get("eliminated");
            if (eliminatedObj instanceof Boolean) {
                isEliminated = (Boolean) eliminatedObj;
            }
            playerEliminated.put(playerId, isEliminated);
        }

        // First pass: Process unique words and award points
        for (Map.Entry<String, List<String>> entry : wordToPlayers.entrySet()) {
            List<String> playerIds = entry.getValue();
            if (playerIds.size() == 1) {
                // Award points for unique words
                String playerId = playerIds.get(0);
                Map<String, Object> playerData = (Map<String, Object>) playersData.get(playerId);
                if (playerData == null)
                    continue;

                int currentScore = 0;
                Object scoreObj = playerData.get("score");
                if (scoreObj instanceof Long) {
                    currentScore = ((Long) scoreObj).intValue();
                } else if (scoreObj instanceof Integer) {
                    currentScore = (Integer) scoreObj;
                }

                int newScore = currentScore + 10;
                updates.put("players/" + playerId + "/score", newScore);
            } else {
                // Reduce lives for players with duplicate words
                for (String playerId : playerIds) {
                    if (playerEliminated.get(playerId))
                        continue; // Skip already eliminated players

                    int lives = playerLives.get(playerId);
                    lives--; // Reduce life for duplicate word

                    playerLives.put(playerId, lives);
                    updates.put("players/" + playerId + "/lives", lives);

                    if (lives <= 0) {
                        playerEliminated.put(playerId, true);
                        updates.put("players/" + playerId + "/eliminated", true);
                    }
                }
            }
        }

        // Second pass: Process players who didn't submit a word
        for (Map.Entry<String, Object> entry : playersData.entrySet()) {
            String playerId = entry.getKey();
            Map<String, Object> playerData = (Map<String, Object>) entry.getValue();

            if (playerData == null || playerEliminated.get(playerId))
                continue;

            // Check if player submitted a word
            String word = (String) playerData.get("currentWord");
            boolean submittedWord = word != null && !word.trim().isEmpty();

            if (!submittedWord) {
                // Player didn't submit a word, reduce life
                int lives = playerLives.get(playerId);
                lives--; // Reduce life for not submitting

                playerLives.put(playerId, lives);
                updates.put("players/" + playerId + "/lives", lives);

                if (lives <= 0) {
                    playerEliminated.put(playerId, true);
                    updates.put("players/" + playerId + "/eliminated", true);
                }
            }
        }

        // Count remaining players and find last active player
        remainingPlayerCount[0] = 0;
        for (Map.Entry<String, Boolean> entry : playerEliminated.entrySet()) {
            String playerId = entry.getKey();
            boolean isEliminated = entry.getValue();
            int lives = playerLives.get(playerId);

            if (!isEliminated && lives > 0) {
                remainingPlayerCount[0]++;
                lastActivePlayerId = playerId;
            }
        }

        System.out.println("TELEPATHY_DEBUG: Remaining players: " + remainingPlayerCount[0]);
        if (lastActivePlayerId != null) {
            System.out.println("TELEPATHY_DEBUG: Last active player: " + lastActivePlayerId);
        }

        // If only one player remains, they are the winner
        if (remainingPlayerCount[0] == 1 && lastActivePlayerId != null) {
            System.out.println("TELEPATHY_DEBUG: Game will end - only one player remains");
            // Award bonus points to winner
            Map<String, Object> winnerData = (Map<String, Object>) playersData.get(lastActivePlayerId);
            if (winnerData != null) {
                int currentScore = 0;
                Object scoreObj = winnerData.get("score");
                if (scoreObj instanceof Long) {
                    currentScore = ((Long) scoreObj).intValue();
                } else if (scoreObj instanceof Integer) {
                    currentScore = (Integer) scoreObj;
                }

                // Award 100 bonus points for winning
                int newScore = currentScore + 100;
                updates.put("players/" + lastActivePlayerId + "/score", newScore);
                updates.put("winnerId", lastActivePlayerId);
            }
        }
    }

    // Add at class level in FirebaseController.java:
    private Set<String> processingRounds = new HashSet<>();

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

                            System.out.println(
                                    "TELEPATHY: Using " + words.size() + " words for round " + nextRoundNumber);

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