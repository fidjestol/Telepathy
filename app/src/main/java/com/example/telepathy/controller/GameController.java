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

            // Extract used words from Firebase
            Object usedWordsObj = gameData.get("usedWords");
            Set<String> firebaseUsedWords = new HashSet<>();
            if (usedWordsObj instanceof Map) {
                Map<String, Object> usedWordsMap = (Map<String, Object>) usedWordsObj;
                firebaseUsedWords.addAll(usedWordsMap.keySet());
                System.out.println("TELEPATHY: Loaded " + firebaseUsedWords.size() + " used words from Firebase");
            }

            // Track eliminated players before updating game state
            Set<String> previouslyActivePlayers = new HashSet<>();
            if (currentGame != null) {
                for (Player player : currentGame.getPlayers()) {
                    if (!player.isEliminated()) {
                        previouslyActivePlayers.add(player.getId());
                    }
                }
            }

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

            // Check if all players have submitted words - ADD DEBUGGING
            boolean shouldEndRound = false;
            if ("active".equals(status)) {
                shouldEndRound = checkAllPlayersSubmitted(players);
                System.out.println("TELEPATHY_DEBUG: All players submitted: " + shouldEndRound);
            }

            // Check if this is a new round
            boolean isNewRound = isNewRound(round);

            // Update game state
            updateGameState(config, players, round, status);

            // Update the game's usedWords set with words from Firebase
            if (currentGame != null) {
                currentGame.setUsedWords(firebaseUsedWords);
            }

            // Check for newly eliminated players
            if (currentGame != null) {
                for (Player player : players) {
                    if (player.isEliminated() && previouslyActivePlayers.contains(player.getId())) {
                        // This player was just eliminated
                        if (updateListener != null) {
                            updateListener.onPlayerEliminated(player);
                        }
                    }
                }
            }

            // If this is a new round, clear the processed rounds for the previous round
            if (isNewRound && "active".equals(status)) {
                // Only keep the current round ID in the set
                processedRounds.clear();
                processedPlayers.clear(); // Also clear processed players set
            }

            // Notify listeners of state changes
            notifyStateChanges(status, round, players, isNewRound);

            // End round if everyone has submitted
            if (shouldEndRound) {
                System.out.println("TELEPATHY: All players have submitted words, ending round");
                endCurrentRound();
            }
        } catch (Exception e) {
            System.out.println("TELEPATHY_ERROR: " + e.getMessage());
            e.printStackTrace();
            if (updateListener != null) {
                updateListener.onError("Error processing game update: " + e.getMessage());
            }
        }
    }

    // Process duplicate words and update Firebase
    private void processDuplicateWords(List<Player> players, GameRound round) {
        // Reset the processed players set for this round
        processedPlayers.clear();

        // Find duplicate words
        Map<String, List<String>> duplicates = findDuplicateWords(players);

        if (duplicates.isEmpty()) {
            System.out.println("TELEPATHY: No duplicate words found in this round");
            return;
        }

        System.out.println("TELEPATHY: Found " + duplicates.size() + " duplicate words");

        // IMPORTANT: Don't modify the database directly from here
        // Instead, just update the UI to reflect the duplicate submissions
        // The FirebaseController should handle the actual life reduction

        // For each duplicate word, just log and track UI updates
        for (Map.Entry<String, List<String>> entry : duplicates.entrySet()) {
            String duplicateWord = entry.getKey();
            List<String> playerIds = entry.getValue();

            System.out.println("TELEPATHY: Word '" + duplicateWord + "' was submitted by " +
                    playerIds.size() + " players: " + String.join(", ", playerIds));

            processedPlayers.addAll(playerIds);
        }

        // CRITICAL: Do not make any Firebase calls to update lives from here
        // Just notify the UI components that need updating

        // If we need to update the UI to show which players submitted duplicates,
        // we can do that here, but without modifying the Firebase data
        if (updateListener != null) {
            updateListener.onGameStateChanged(currentGame);
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
            if (player.isEliminated())
                continue;

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
        int submittedCount = 0;

        // Add better debugging
        System.out.println("TELEPATHY_DEBUG: Checking player submissions:");

        for (Player player : players) {
            if (!player.isEliminated()) {
                activePlayers++;
                String word = player.getCurrentWord();
                boolean hasSubmitted = word != null && !word.isEmpty();

                System.out.println("TELEPATHY_DEBUG: Player " + player.getUsername() +
                        " - Submitted: " + hasSubmitted +
                        (hasSubmitted ? " (word: " + word + ")" : ""));

                if (hasSubmitted) {
                    submittedCount++;
                } else {
                    allPlayersSubmitted = false;
                }
            }
        }

        System.out.println("TELEPATHY_DEBUG: " + submittedCount + " of " + activePlayers +
                " active players have submitted words. All submitted: " + allPlayersSubmitted);

        // We need at least one active player and all must have submitted
        return allPlayersSubmitted && activePlayers > 0;
    }

    // Helper method to end the current round
    private void endCurrentRound() {
        System.out.println("TELEPATHY_DEBUG: Ending current round for game " + gameId);
        firebaseController.endCurrentRound(gameId, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                System.out.println("TELEPATHY_DEBUG: Round ended successfully");
            }

            @Override
            public void onFailure(String error) {
                System.out.println("TELEPATHY_DEBUG: Failed to end round: " + error);
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
            System.out.println("TELEPATHY_DEBUG: Raw config data from Firebase: " + configData);

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

            // Add matchingMode extraction
            Object matchingModeObj = configData.get("matchingMode");
            if (matchingModeObj instanceof Boolean) {
                config.setMatchingMode((Boolean) matchingModeObj);
            }
            System.out.println("TELEPATHY_DEBUG: Extracted matchingMode: " + config.isMatchingMode());

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

    // Handle timer expiration
    public void handleTimerExpired() {
        // Only end the round if it's still active
        if (currentGame != null && "active".equals(currentGame.getStatus())) {
            System.out.println("TELEPATHY_DEBUG: Timer expired, ending round");
            endCurrentRound();
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
                if (currentGame.getConfig().isMatchingMode()) {
                    // In matching mode, the current player is a winner if they matched
                    boolean isWinner = false;
                    String currentWord = null;

                    // Get current player's word
                    for (Player player : players) {
                        if (player.getId().equals(currentPlayerId)) {
                            currentWord = player.getCurrentWord();
                            break;
                        }
                    }

                    // Check if any other player has the same word
                    if (currentWord != null && !currentWord.isEmpty()) {
                        for (Player player : players) {
                            if (!player.getId().equals(currentPlayerId) &&
                                    currentWord.equals(player.getCurrentWord())) {
                                isWinner = true;
                                break;
                            }
                        }
                    }

                    if (isWinner) {
                        // Current player matched, show they won
                        updateListener.onGameEnd(null); // null indicates matching win
                    } else {
                        // Current player didn't match, show they lost
                        Player winner = null; // No specific winner to show
                        updateListener.onGameEnd(winner);
                    }
                } else {
                    // Classic mode - find the last player standing
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
    }

    public void submitWord(String word) {
        firebaseController.submitWord(gameId, currentPlayerId, word, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                // Word submitted successfully
                System.out.println("TELEPATHY: Word successfully submitted: " + word);
            }

            @Override
            public void onFailure(String error) {
                if (updateListener != null) {
                    updateListener.onError("Failed to submit word: " + error);
                }
            }
        });
    }

    public void validateWord(String word, ValidationCallback callback) {
        System.out.println("TELEPATHY_DEBUG: what mode are we in? " + currentGame.getConfig().isMatchingMode());
        // Create default callback if none provided
        ValidationCallback actualCallback = callback != null ? callback : new ValidationCallback() {
            @Override
            public void onSuccess() {
                // Default success does nothing
            }

            @Override
            public void onError(String errorMessage) {
                // Default error handling uses updateListener
                if (updateListener != null) {
                    updateListener.onError(errorMessage);
                }
            }
        };

        if (word == null || word.trim().isEmpty()) {
            actualCallback.onError("Word cannot be empty");
            return;
        }

        // Normalize the word
        String normalizedWord = word.trim().toLowerCase();

        // Handle matching mode
        if (currentGame != null && currentGame.getConfig().isMatchingMode()) {
            System.out.println("TELEPATHY_DEBUG: letting every word through");
            submitWord(normalizedWord);
            System.out.println("TELEPATHY: Player submitted word in matching mode: " + normalizedWord);
            actualCallback.onSuccess();
            return;
        }

        // Regular mode validation
        if (currentGame != null && currentGame.getCurrentRound() != null) {
            System.out.println("TELEPATHY_DEBUG: Normal mode validation");
            List<String> validWords = currentGame.getCurrentRound().getWords();

            if (validWords != null) {
                // Check if word has already been used by ANY player in any round
                if (currentGame.isWordAlreadyUsed(normalizedWord)) {
                    actualCallback.onError("This word has already been used in a previous round!");
                    return;
                }

                // Check if word is in valid words list
                if (!validWords.contains(normalizedWord)) {
                    actualCallback.onError("Word is not in the valid word list for this round");
                    return;
                }
            } else {
                actualCallback.onError("No valid words available");
                return;
            }
        }

        // If validation passes, submit the word
        // IMPORTANT: Don't add to usedWords yet - it will be added at round end
        submitWord(normalizedWord);

        if (currentGame != null) {
            // We don't add to used words here anymore
            System.out.println("TELEPATHY: Player submitted word: " + normalizedWord);
        }

        actualCallback.onSuccess();
    }

    public interface ValidationCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    public void cleanup() {
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