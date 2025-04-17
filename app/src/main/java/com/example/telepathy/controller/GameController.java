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
        if (gameData == null)
            return;

        // Extract game configuration
        GameConfig config = extractGameConfig(gameData);

        // Extract players
        List<Player> players = extractPlayers(gameData);

        // Extract round data
        GameRound round = extractRoundData(gameData);

        // Get game status
        String status = (String) gameData.get("status");
        if (status == null)
            status = "active";

        // Update game state
        boolean isNewRound = false;
        if (currentGame == null) {
            currentGame = new Game(gameId, config, players);
            isNewRound = round != null;
        } else {
            isNewRound = round != null && (currentGame.getCurrentRound() == null ||
                    round.getRoundNumber() > currentGame.getCurrentRound().getRoundNumber());
            currentGame.setConfig(config);
            currentGame.setPlayers(players);
        }

        if (round != null) {
            currentGame.setCurrentRound(round);
        }
        currentGame.setStatus(status);

        // Check for eliminated players
        for (Player player : players) {
            if (player.isEliminated() && !processedPlayers.contains(player.getId())) {
                processedPlayers.add(player.getId());
                if (updateListener != null) {
                    updateListener.onPlayerEliminated(player);
                }
            }
        }

        // Notify about state changes
        notifyStateChanges(status, round, players, isNewRound);

        // Check if round is complete
        if (currentGame.isRoundComplete() && "active".equals(status)) {
            endCurrentRound();
        }
    }

    private void notifyStateChanges(String status, GameRound round, List<Player> players, boolean isNewRound) {
        if (updateListener != null) {
            updateListener.onGameStateChanged(currentGame);

            if (isNewRound && "active".equals(status)) {
                updateListener.onRoundStart(round);
            } else if ("roundEnd".equals(status) && !isProcessingRoundEnd) {
                isProcessingRoundEnd = true;
                updateListener.onRoundEnd(round);
                new Handler().postDelayed(() -> isProcessingRoundEnd = false, 3000);
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