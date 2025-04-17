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
import com.example.telepathy.model.gamemode.GameMode;
import com.example.telepathy.model.gamemode.GameModeFactory;

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
        this.database = FirebaseDatabase.getInstance().getReference();
        initGameListener();

        // Immediately fetch the current game state
        database.child("games").child(gameId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                processGameUpdate(snapshot);
            }
        });
    }

    private void initGameListener() {
        gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                processGameUpdate(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (updateListener != null) {
                    updateListener.onError("Database error: " + error.getMessage());
                }
            }
        };

        firebaseController.listenForGameUpdates(gameId, gameListener);
    }

    private void processGameUpdate(DataSnapshot snapshot) {
        try {
            Map<String, Object> gameData = (Map<String, Object>) snapshot.getValue();
            if (gameData == null)
                return;

            String status = (String) gameData.get("status");
            Map<String, Object> roundData = (Map<String, Object>) gameData.get("currentRound");
            Map<String, Object> playersData = (Map<String, Object>) gameData.get("players");
            Map<String, Object> configData = (Map<String, Object>) gameData.get("config");

            // Create GameConfig first
            GameConfig config = new GameConfig();
            if (configData != null) {
                config.setTimeLimit(((Number) configData.getOrDefault("timeLimit", 30)).intValue());
                config.setMaxPlayers(((Number) configData.getOrDefault("maxPlayers", 8)).intValue());
                config.setLivesPerPlayer(((Number) configData.getOrDefault("livesPerPlayer", 3)).intValue());
                config.setSelectedCategory((String) configData.getOrDefault("selectedCategory", "Animals"));
                config.setGameMode((String) configData.getOrDefault("gameMode", "Classic"));
            }

            // Update current game state
            if (currentGame == null) {
                currentGame = new Game();
                currentGame.setGameId(gameId);
                currentGame.setConfig(config);
            }

            // Process players
            List<Player> playerList = new ArrayList<>();
            if (playersData != null) {
                for (Map.Entry<String, Object> entry : playersData.entrySet()) {
                    Map<String, Object> playerData = (Map<String, Object>) entry.getValue();
                    Player player = new Player();
                    player.setId(entry.getKey());
                    player.setUsername((String) playerData.get("username"));
                    player.setLives(((Number) playerData.get("lives")).intValue());
                    player.setScore(((Number) playerData.get("score")).intValue());
                    player.setEliminated((Boolean) playerData.getOrDefault("eliminated", false));
                    player.setCurrentWord((String) playerData.get("currentWord"));
                    playerList.add(player);
                }
            }
            currentGame.setPlayers(playerList);

            // Process round data
            GameRound round = null;
            if (roundData != null) {
                round = new GameRound();
                round.setRoundNumber(((Number) roundData.get("roundNumber")).intValue());
                round.setStartTime(((Number) roundData.get("startTime")).longValue());
                round.setEndTime(((Number) roundData.get("endTime")).longValue());

                // Handle submitted words
                Map<String, String> submittedWords = (Map<String, String>) roundData.get("submittedWords");
                if (submittedWords != null) {
                    round.setSubmittedWords(submittedWords);
                }

                // Handle word list for classic mode
                List<String> words = (List<String>) roundData.get("words");
                if (words != null) {
                    round.setWords(words);
                }
            }

            boolean isNewRound = false;
            if (round != null && (currentGame.getCurrentRound() == null ||
                    currentGame.getCurrentRound().getRoundNumber() != round.getRoundNumber())) {
                isNewRound = true;
            }

            currentGame.setCurrentRound(round);
            currentGame.setStatus(status);

            // Initialize game mode if needed
            if (currentGame.getGameMode() == null) {
                currentGame.initializeGameMode();
            }

            // Notify listeners
            notifyStateChanges(status, round, playerList, isNewRound);

        } catch (Exception e) {
            e.printStackTrace();
            if (updateListener != null) {
                updateListener.onError("Error processing game update: " + e.getMessage());
            }
        }
    }

    private void notifyStateChanges(String status, GameRound round, List<Player> players, boolean isNewRound) {
        if (updateListener != null) {
            updateListener.onGameStateChanged(currentGame);

            if (isNewRound && "active".equals(status)) {
                if (round != null && !processedRounds.contains("start_" + round.getRoundNumber())) {
                    processedRounds.add("start_" + round.getRoundNumber());
                    updateListener.onRoundStart(round);
                }
            } else if ("roundEnd".equals(status) && !isProcessingRoundEnd) {
                if (round != null && !processedRounds.contains("end_" + round.getRoundNumber())) {
                    processedRounds.add("end_" + round.getRoundNumber());
                    isProcessingRoundEnd = true;
                    updateListener.onRoundEnd(round);
                    new Handler().postDelayed(() -> isProcessingRoundEnd = false, 3000);
                }
            } else if ("gameEnd".equals(status)) {
                Player winner = currentGame.getWinner();
                updateListener.onGameEnd(winner);
            }
        }
    }

    public void submitWord(String word, FirebaseController.FirebaseCallback callback) {
        if (currentGame == null || word == null || word.trim().isEmpty()) {
            if (callback != null) {
                callback.onFailure("Invalid game state or word");
            }
            return;
        }

        // Submit word through game mode
        if (currentGame.submitWord(currentPlayerId, word)) {
            // Update word in Firebase
            firebaseController.submitWord(gameId, currentPlayerId, word, callback);
        } else {
            if (callback != null) {
                callback.onFailure("Word submission failed");
            }
        }
    }

    private boolean isProcessingRoundEnd = false;

    // Helper methods remain unchanged
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

    private List<Player> extractPlayers(Map<String, Object> gameData) {
        List<Player> players = new ArrayList<>();
        Object playersObj = gameData.get("players");

        if (playersObj instanceof Map) {
            Map<String, Object> playersMap = (Map<String, Object>) playersObj;
            for (Map.Entry<String, Object> entry : playersMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> playerData = (Map<String, Object>) entry.getValue();
                    Player player = new Player();
                    player.setId((String) playerData.get("id"));
                    player.setUsername((String) playerData.get("username"));
                    player.setHost((Boolean) playerData.getOrDefault("isHost", false));

                    Object livesObj = playerData.get("lives");
                    if (livesObj instanceof Number) {
                        player.setLives(((Number) livesObj).intValue());
                    }

                    Object scoreObj = playerData.get("score");
                    if (scoreObj instanceof Number) {
                        player.setScore(((Number) scoreObj).intValue());
                    }

                    Object eliminatedObj = playerData.get("isEliminated");
                    if (eliminatedObj instanceof Boolean) {
                        player.setEliminated((Boolean) eliminatedObj);
                    }

                    players.add(player);
                }
            }
        }

        return players;
    }

    private GameRound extractRoundData(Map<String, Object> gameData) {
        Object roundObj = gameData.get("currentRound");
        if (!(roundObj instanceof Map)) {
            return null;
        }

        Map<String, Object> roundData = (Map<String, Object>) roundObj;

        int roundNumber = 0;
        Object roundNumberObj = roundData.get("roundNumber");
        if (roundNumberObj instanceof Number) {
            roundNumber = ((Number) roundNumberObj).intValue();
        }

        long timeLimit = 30000; // Default 30 seconds
        Object timeLimitObj = roundData.get("timeLimit");
        if (timeLimitObj instanceof Number) {
            timeLimit = ((Number) timeLimitObj).longValue();
        }

        List<String> words = new ArrayList<>();
        Object wordsObj = roundData.get("words");
        if (wordsObj instanceof List) {
            words = (List<String>) wordsObj;
        }

        GameRound round = new GameRound(roundNumber, timeLimit, words);

        // Set player words
        Object playerWordsObj = roundData.get("playerWords");
        if (playerWordsObj instanceof Map) {
            Map<String, List<String>> playerWords = (Map<String, List<String>>) playerWordsObj;
            for (Map.Entry<String, List<String>> entry : playerWords.entrySet()) {
                for (String word : entry.getValue()) {
                    round.addPlayerWord(entry.getKey(), word);
                }
            }
        }

        return round;
    }

    private void endCurrentRound() {
        if (currentGame == null || currentGame.getCurrentRound() == null ||
                processedRounds.contains("end_" + currentGame.getCurrentRound().getRoundNumber())) {
            return; // Don't end the round if it's already been processed
        }

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