package com.example.telepathy.controller;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.telepathy.model.Game;
import com.example.telepathy.model.GameConfig;
import com.example.telepathy.model.GameRound;
import com.example.telepathy.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameController {
    private Game currentGame;
    private FirebaseController firebaseController;
    private String gameId;
    private String currentPlayerId;
    private ValueEventListener gameListener;
    private GameUpdateListener updateListener;

    private String currentRoundStarterId = null;
    private Handler roundStartHandler = new Handler();
    private Runnable roundStartRunnable = null;
    private Set<String> processedRounds = new HashSet<>();
    private Set<String> processedPlayers = new HashSet<>();

    // Interface for game updates
    public interface GameUpdateListener {
        void onGameStateChanged(Game game);
        void onRoundStart(GameRound round);
        void onRoundEnd(GameRound round);
        void onPlayerEliminated(Player player);
        void onGameEnd(Player winner);
        void onError(String error);
    }


    private DatabaseReference database;

    // Then update your constructor to initialize it:
    public GameController(String gameId, String playerId, GameUpdateListener listener) {
        this.gameId = gameId;
        this.currentPlayerId = playerId;
        this.updateListener = listener;
        this.firebaseController = FirebaseController.getInstance();

        // Initialize database reference
        this.database = FirebaseDatabase.getInstance().getReference();

        // Initialize game state listener
        initGameListener();
    }

    private void initGameListener() {
        gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Convert Firebase data to Game object
                Map<String, Object> gameData = (Map<String, Object>) snapshot.getValue();
                if (gameData != null) {
                    // Process game data
                    processGameUpdate(gameData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (updateListener != null) {
                    updateListener.onError("Game update failed: " + error.getMessage());
                }
            }
        };

        // Start listening for game updates
        firebaseController.listenForGameUpdates(gameId, gameListener);
    }

    private void processGameUpdate(Map<String, Object> gameData) {
        try {
            String status = (String) gameData.get("status");

            // Extract data from Firebase
            GameConfig config = extractGameConfig(gameData);
            List<Player> players = extractPlayers(gameData);
            GameRound round = extractRoundData(gameData);

            // Generate a unique ID for this round's state to prevent duplicate processing
            String roundId = "";
            if (round != null) {
                roundId = "round" + round.getRoundNumber() + "_" + status;
            }

            // Check if this is the round end status
            if ("roundEnd".equals(status)) {
                // Only process duplicate words once per round
                if (!processedRounds.contains(roundId)) {
                    System.out.println("TELEPATHY: Processing duplicate words for round " + roundId);
                    processedRounds.add(roundId);
                    processDuplicateWords(players, round);
                } else {
                    System.out.println("TELEPATHY: Already processed duplicates for round " + roundId);
                }
            }

            // Round starter logic
            // Check for scheduled next round start
            Object nextRoundStartTimeObj = gameData.get("nextRoundStartTime");
            Object roundStarterIdObj = gameData.get("roundStarterId");

            if (nextRoundStartTimeObj instanceof Long && roundStarterIdObj instanceof String) {
                long nextRoundStartTime = (Long) nextRoundStartTimeObj;
                String roundStarterId = (String) roundStarterIdObj;

                // Check if we already have a scheduled start for this round
                if (!isWaitingForRoundStart(roundStarterId)) {
                    // Get current time
                    long currentTime = System.currentTimeMillis();

                    // If start time is in the future, schedule the start
                    if (nextRoundStartTime > currentTime) {
                        long delayMillis = nextRoundStartTime - currentTime;

                        // Store that we're handling this round start
                        scheduleRoundStart(roundStarterId, delayMillis);
                    } else {
                        // Start time has passed but round hasn't started yet
                        // Only the first client to process this will actually trigger the start
                        tryStartNextRound(roundStarterId);
                    }
                }
            }

            // Check if all players have submitted words
            boolean shouldEndRound = checkAllPlayersSubmitted(players) && "active".equals(status);

            // Check if this is a new round
            boolean isNewRound = isNewRound(round);

            // Update game state
            updateGameState(config, players, round, status);

            // If this is a new round, clear the processed rounds for the previous round
            if (isNewRound && "active".equals(status)) {
                // Only keep the current round ID in the set
                processedRounds.clear();
            }

            // Notify listeners of state changes
            notifyStateChanges(status, round, players, isNewRound);

            // End round if everyone has submitted
            if (shouldEndRound) {
                endCurrentRound();
            }
        } catch (Exception e) {
            if (updateListener != null) {
                updateListener.onError("Error processing game update: " + e.getMessage());
            }
        }
    }
    // Process duplicate words and update Firebase
    private void processDuplicateWords(List<Player> players, GameRound round) {
        // Reset the processed players set
        processedPlayers.clear();

        // Find duplicate words
        Map<String, List<String>> duplicates = findDuplicateWords(players);

        if (duplicates.isEmpty()) {
            System.out.println("TELEPATHY: No duplicate words found in this round");
            return;
        }

        System.out.println("TELEPATHY: Found " + duplicates.size() + " duplicate words");

        // For each duplicate word, identify players who need to lose a life
        for (Map.Entry<String, List<String>> entry : duplicates.entrySet()) {
            String duplicateWord = entry.getKey();
            List<String> playerIds = entry.getValue();

            System.out.println("TELEPATHY: Word '" + duplicateWord + "' was submitted by " +
                    playerIds.size() + " players: " + String.join(", ", playerIds));

            // Process each player separately to avoid race conditions
            for (String playerId : playerIds) {
                // Skip if we've already processed this player for this round
                if (processedPlayers.contains(playerId)) {
                    System.out.println("TELEPATHY: Skipping already processed player: " + playerId);
                    continue;
                }

                // Mark this player as processed
                processedPlayers.add(playerId);

                // CRITICAL: Get the LATEST player data from Firebase to ensure accurate lives count
                database.child("games").child(gameId).child("players").child(playerId).get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DataSnapshot playerSnapshot = task.getResult();
                                if (!playerSnapshot.exists()) {
                                    System.out.println("TELEPATHY: Player " + playerId + " no longer exists in the game");
                                    return;
                                }

                                // Read current player data directly from Firebase
                                Map<String, Object> playerData = (Map<String, Object>) playerSnapshot.getValue();
                                if (playerData == null) {
                                    System.out.println("TELEPATHY: Player data is null for " + playerId);
                                    return;
                                }

                                // Get current lives value directly from the database
                                Object livesObj = playerData.get("lives");
                                int currentLives = 3; // Default if not found

                                if (livesObj instanceof Long) {
                                    currentLives = ((Long) livesObj).intValue();
                                } else if (livesObj instanceof Integer) {
                                    currentLives = (Integer) livesObj;
                                } else if (livesObj instanceof Double) {
                                    currentLives = ((Double) livesObj).intValue();
                                }

                                // Deduct exactly one life
                                int newLives = Math.max(0, currentLives - 1);

                                // Get username for logging
                                String username = (String) playerData.get("username");

                                System.out.println("TELEPATHY: Player " + username + " had " +
                                        currentLives + " lives, reducing to " + newLives +
                                        " for duplicate word: " + duplicateWord);

                                // Create a dedicated update for just lives and eliminated status
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("lives", newLives);

                                // Only set eliminated if lives reach zero
                                if (newLives <= 0) {
                                    updates.put("eliminated", true);
                                    System.out.println("TELEPATHY: Player " + username + " has been eliminated!");
                                }

                                // Update ONLY these specific fields in Firebase
                                database.child("games").child(gameId).child("players").child(playerId)
                                        .updateChildren(updates)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                System.out.println("TELEPATHY: Successfully updated lives for " +
                                                        username + " to " + newLives);

                                                // Notify listeners if player was eliminated
                                                if (newLives <= 0 && updateListener != null) {
                                                    // Create a player object for the notification
                                                    Player eliminatedPlayer = new Player();
                                                    eliminatedPlayer.setId(playerId);
                                                    eliminatedPlayer.setUsername(username);
                                                    eliminatedPlayer.setLives(0);
                                                    eliminatedPlayer.setEliminated(true);

                                                    updateListener.onPlayerEliminated(eliminatedPlayer);
                                                }
                                            } else {
                                                System.out.println("TELEPATHY: Failed to update lives for " +
                                                        username + ": " +
                                                        (updateTask.getException() != null ?
                                                                updateTask.getException().getMessage() : "unknown error"));
                                            }
                                        });
                            } else {
                                System.out.println("TELEPATHY: Failed to get current player data for " + playerId);
                            }
                        });
            }
        }
    }

    private boolean isWaitingForRoundStart(String roundStarterId) {
        return currentRoundStarterId != null && currentRoundStarterId.equals(roundStarterId);
    }

    private void scheduleRoundStart(String roundStarterId, long delayMillis) {
        // Cancel any previous scheduled start
        if (roundStartRunnable != null) {
            roundStartHandler.removeCallbacks(roundStartRunnable);
        }

        // Store the current starter ID
        currentRoundStarterId = roundStarterId;

        // Schedule the round start
        roundStartRunnable = () -> tryStartNextRound(roundStarterId);
        roundStartHandler.postDelayed(roundStartRunnable, delayMillis);

        System.out.println("TELEPATHY: Scheduled round start in " + (delayMillis / 1000) + " seconds");
    }

    private void tryStartNextRound(String roundStarterId) {
        // First, check if we are still the designated starter
        if (!isWaitingForRoundStart(roundStarterId)) {
            return;
        }

        // Clear the current starter ID to prevent duplicate starts
        currentRoundStarterId = null;

        // Start the round
        System.out.println("TELEPATHY: Triggering next round start");
        firebaseController.startNextRound(gameId, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                System.out.println("TELEPATHY: Successfully started next round");
            }

            @Override
            public void onFailure(String error) {
                System.out.println("TELEPATHY: Failed to start next round: " + error);
            }
        });
    }

    // Helper method to find duplicate words among players
    private Map<String, List<String>> findDuplicateWords(List<Player> players) {
        // First map words to players who submitted them
        Map<String, List<String>> wordToPlayers = new HashMap<>();

        for (Player player : players) {
            if (player.isEliminated()) continue;

            String word = player.getCurrentWord();
            if (word != null && !word.isEmpty()) {
                if (!wordToPlayers.containsKey(word)) {
                    wordToPlayers.put(word, new ArrayList<>());
                }
                wordToPlayers.get(word).add(player.getId());
            }
        }

        // Filter to keep only words with multiple players
        Map<String, List<String>> duplicateWords = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : wordToPlayers.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateWords.put(entry.getKey(), entry.getValue());
            }
        }

        return duplicateWords;
    }

    // Check if all active players have submitted words
    private boolean checkAllPlayersSubmitted(List<Player> players) {
        boolean allPlayersSubmitted = true;
        int activePlayers = 0;

        for (Player player : players) {
            if (!player.isEliminated()) {
                activePlayers++;
                // If an active player hasn't submitted a word, not everyone is done
                if (player.getCurrentWord() == null || player.getCurrentWord().isEmpty()) {
                    allPlayersSubmitted = false;
                    break;
                }
            }
        }

        // We need at least one active player and all must have submitted
        return allPlayersSubmitted && activePlayers > 0;
    }

    // Helper method to end the current round
    private void endCurrentRound() {
        firebaseController.endCurrentRound(gameId, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                // Round ended successfully in Firebase
            }

            @Override
            public void onFailure(String error) {
                if (updateListener != null) {
                    updateListener.onError("Failed to end round: " + error);
                }
            }
        });
    }

    // Extract game configuration from Firebase data
    private GameConfig extractGameConfig(Map<String, Object> gameData) {
        GameConfig config = new GameConfig();
        Map<String, Object> configData = (Map<String, Object>) gameData.get("config");
        if (configData != null) {
            // Safely convert numeric values which might be Long from Firebase
            Object timeLimitObj = configData.get("timeLimit");
            if (timeLimitObj instanceof Long) {
                config.setTimeLimit(((Long) timeLimitObj).intValue());
            }

            Object maxPlayersObj = configData.get("maxPlayers");
            if (maxPlayersObj instanceof Long) {
                config.setMaxPlayers(((Long) maxPlayersObj).intValue());
            }

            Object livesObj = configData.get("livesPerPlayer");
            if (livesObj instanceof Long) {
                config.setLivesPerPlayer(((Long) livesObj).intValue());
            }

            config.setSelectedCategory((String) configData.get("selectedCategory"));
        }
        return config;
    }

    // Extract player data from Firebase
    private List<Player> extractPlayers(Map<String, Object> gameData) {
        List<Player> players = new ArrayList<>();
        Map<String, Object> playersData = (Map<String, Object>) gameData.get("players");
        if (playersData != null) {
            for (Map.Entry<String, Object> entry : playersData.entrySet()) {
                Map<String, Object> playerData = (Map<String, Object>) entry.getValue();
                Player player = new Player();
                player.setId(entry.getKey());
                player.setUsername((String) playerData.get("username"));

                // Safe conversion of numeric types
                Object scoreObj = playerData.get("score");
                if (scoreObj instanceof Long) {
                    player.setScore(((Long) scoreObj).intValue());
                }

                Object livesObj = playerData.get("lives");
                if (livesObj instanceof Long) {
                    player.setLives(((Long) livesObj).intValue());
                }

                Object eliminatedObj = playerData.get("eliminated");
                if (eliminatedObj instanceof Boolean) {
                    player.setEliminated((Boolean) eliminatedObj);
                }

                player.setCurrentWord((String) playerData.get("currentWord"));
                players.add(player);
            }
        }
        return players;
    }

    // Add this method to GameController.java
    public void handleTimerExpired() {
        // Only end the round if it's still active
        if (currentGame != null && "active".equals(currentGame.getStatus())) {
            // End the round
            firebaseController.endCurrentRound(gameId, new FirebaseController.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d("TELEPATHY", "Round ended due to timer expiration");
                }

                @Override
                public void onFailure(String error) {
                    if (updateListener != null) {
                        updateListener.onError("Failed to end round: " + error);
                    }
                }
            });
        }
    }

    private void checkGameEndCondition(List<Player> players) {
        int activePlayers = 0;
        Player lastActivePlayer = null;

        for (Player player : players) {
            if (!player.isEliminated()) {
                activePlayers++;
                lastActivePlayer = player;
            }
        }

        // Game is over when only one player remains or all players are eliminated
        if (activePlayers <= 1) {
            // Update game status to gameEnd
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "gameEnd");

            // If there's a winner (one player left), mark them as winner
            if (activePlayers == 1 && lastActivePlayer != null) {
                updates.put("winnerId", lastActivePlayer.getId());
            }

            // Update game status in Firebase
            database.child("games").child(gameId).updateChildren(updates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("TELEPATHY", "Game ended successfully");
                        } else {
                            Log.e("TELEPATHY", "Failed to end game: " +
                                    (task.getException() != null ? task.getException().getMessage() : "unknown error"));
                        }
                    });
        }
    }

    // Extract round data from Firebase
    private GameRound extractRoundData(Map<String, Object> gameData) {
        GameRound round = new GameRound();
        Map<String, Object> roundData = (Map<String, Object>) gameData.get("currentRound");
        if (roundData != null) {
            Object roundNumberObj = roundData.get("roundNumber");
            if (roundNumberObj instanceof Long) {
                round.setRoundNumber(((Long) roundNumberObj).intValue());
            }

            Object startTimeObj = roundData.get("startTime");
            if (startTimeObj instanceof Long) {
                round.setStartTime((Long) startTimeObj);
            }

            Object endTimeObj = roundData.get("endTime");
            if (endTimeObj instanceof Long) {
                round.setEndTime((Long) endTimeObj);
            }

            // Get words for this round
            List<String> words = new ArrayList<>();
            Object wordsObj = roundData.get("words");
            if (wordsObj instanceof List) {
                List<Object> wordsData = (List<Object>) wordsObj;
                for (Object word : wordsData) {
                    if (word instanceof String) {
                        words.add((String) word);
                    }
                }
            }
            round.setWords(words);
        }
        return round;
    }

    // Check if this is a new round
    private boolean isNewRound(GameRound round) {
        return currentGame == null ||
                currentGame.getCurrentRound() == null ||
                currentGame.getCurrentRound().getRoundNumber() != round.getRoundNumber();
    }

    // Update the game state with new data
    private void updateGameState(GameConfig config, List<Player> players, GameRound round, String status) {
        if (currentGame == null) {
            currentGame = new Game(gameId, config, players);
        } else {
            currentGame.setConfig(config);
            currentGame.setPlayers(players);
        }
        currentGame.setCurrentRound(round);
        currentGame.setStatus(status);
    }

    private boolean isProcessingRoundEnd = false;

    private void notifyStateChanges(String status, GameRound round, List<Player> players, boolean isNewRound) {
        if (updateListener != null) {
            updateListener.onGameStateChanged(currentGame);

            // Notify about new round if applicable
            if (isNewRound && "active".equals(status)) {
                updateListener.onRoundStart(round);
            } else if ("roundEnd".equals(status) && !isProcessingRoundEnd) {
                isProcessingRoundEnd = true;
                updateListener.onRoundEnd(round);
                // Add a delay before allowing another round end to be processed
                new Handler().postDelayed(() -> isProcessingRoundEnd = false, 3000);
            } else if ("gameEnd".equals(status)) {
                // Find winner
                Player winner = null;
                for (Player player : players) {
                    if (!player.isEliminated()) {
                        winner = player;
                        break;
                    }
                }
                updateListener.onGameEnd(winner);
            }
        }
    }

    public void submitWord(String word) {
        firebaseController.submitWord(gameId, currentPlayerId, word, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                // Word submitted successfully
            }

            @Override
            public void onFailure(String error) {
                if (updateListener != null) {
                    updateListener.onError("Failed to submit word: " + error);
                }
            }
        });
    }

    public void validateWord(String word) {
        // Simple word validation
        if (word == null || word.trim().isEmpty()) {
            if (updateListener != null) {
                updateListener.onError("Word cannot be empty");
            }
            return;
        }

        // Check if word is in the current round's word list
        if (currentGame != null && currentGame.getCurrentRound() != null) {
            List<String> validWords = currentGame.getCurrentRound().getWords();
            if (validWords != null && !validWords.contains(word.trim().toLowerCase())) {
                if (updateListener != null) {
                    updateListener.onError("Word is not in the valid word list");
                }
                return;
            }
        }

        // If validation passes, submit the word
        submitWord(word.trim().toLowerCase());
    }    public void cleanup() {
        if (gameListener != null) {
            firebaseController.removeGameListener(gameId, gameListener);
        }

        // Remove any pending round start
        if (roundStartRunnable != null) {
            roundStartHandler.removeCallbacks(roundStartRunnable);
            roundStartRunnable = null;
        }
    }
}