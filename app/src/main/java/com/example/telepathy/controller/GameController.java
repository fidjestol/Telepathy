package com.example.telepathy.controller;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.example.telepathy.model.Game;
import com.example.telepathy.model.GameConfig;
import com.example.telepathy.model.GameRound;
import com.example.telepathy.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameController {
    private Game currentGame;
    private FirebaseController firebaseController;
    private String gameId;
    private String currentPlayerId;
    private ValueEventListener gameListener;
    private GameUpdateListener updateListener;

    // Interface for game updates
    public interface GameUpdateListener {
        void onGameStateChanged(Game game);
        void onRoundStart(GameRound round);
        void onRoundEnd(GameRound round);
        void onPlayerEliminated(Player player);
        void onGameEnd(Player winner);
        void onError(String error);
    }

    public GameController(String gameId, String playerId, GameUpdateListener listener) {
        this.gameId = gameId;
        this.currentPlayerId = playerId;
        this.updateListener = listener;
        this.firebaseController = FirebaseController.getInstance();

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
            int currentRound = ((Long) gameData.get("currentRound")).intValue();

            // Get game config
            Map<String, Object> configData = (Map<String, Object>) gameData.get("config");
            GameConfig config = new GameConfig();
            if (configData != null) {
                config.setTimeLimit(((Long) configData.get("timeLimit")).intValue());
                config.setMaxPlayers(((Long) configData.get("maxPlayers")).intValue());
                config.setLivesPerPlayer(((Long) configData.get("livesPerPlayer")).intValue());
                config.setSelectedCategory((String) configData.get("selectedCategory"));
            }

            // Get players
            List<Player> players = new ArrayList<>();
            Map<String, Object> playersData = (Map<String, Object>) gameData.get("players");
            if (playersData != null) {
                for (Map.Entry<String, Object> entry : playersData.entrySet()) {
                    Map<String, Object> playerData = (Map<String, Object>) entry.getValue();
                    Player player = new Player();
                    player.setId(entry.getKey());
                    player.setUsername((String) playerData.get("username"));
                    player.setScore(((Long) playerData.get("score")).intValue());
                    player.setLives(((Long) playerData.get("lives")).intValue());
                    player.setEliminated((Boolean) playerData.get("eliminated"));
                    player.setCurrentWord((String) playerData.get("currentWord"));
                    players.add(player);
                }
            }

            // Get current round data
            Map<String, Object> roundData = (Map<String, Object>) gameData.get("currentRound");
            GameRound round = new GameRound();
            if (roundData != null) {
                round.setRoundNumber(((Long) roundData.get("roundNumber")).intValue());
                round.setStartTime(((Long) roundData.get("startTime")).longValue());
                round.setEndTime(((Long) roundData.get("endTime")).longValue());

                // Get words for this round
                List<String> words = new ArrayList<>();
                List<Object> wordsData = (List<Object>) roundData.get("words");
                if (wordsData != null) {
                    for (Object word : wordsData) {
                        words.add((String) word);
                    }
                }
                round.setWords(words);
            }

            // Update game state
            if (currentGame == null) {
                currentGame = new Game(gameId, config, players);
            } else {
                currentGame.setConfig(config);
                currentGame.setPlayers(players);
            }
            currentGame.setCurrentRound(round);
            currentGame.setStatus(status);

            // Check game state changes
            if ("active".equals(status)) {
                // Game is active
                if (updateListener != null) {
                    updateListener.onGameStateChanged(currentGame);

                    // Check if round has changed
                    if (currentGame.getCurrentRound().getRoundNumber() != currentRound) {
                        updateListener.onRoundStart(currentGame.getCurrentRound());
                    }
                }
            } else if ("roundEnd".equals(status)) {
                // Round has ended
                if (updateListener != null) {
                    updateListener.onRoundEnd(currentGame.getCurrentRound());

                    // Check for eliminated players
                    for (Player player : players) {
                        if (player.isEliminated()) {
                            updateListener.onPlayerEliminated(player);
                        }
                    }
                }
            } else if ("gameEnd".equals(status)) {
                // Game has ended
                if (updateListener != null) {
                    // Find winner (last player standing)
                    Player winner = null;
                    for (Player player : players) {
                        if (!player.isEliminated()) {
                            winner = player;
                            break;
                        }
                    }
                    updateListener.onGameEnd(winner);
                }

                // Clean up listener
                firebaseController.removeGameListener(gameId, gameListener);
            }
        } catch (Exception e) {
            if (updateListener != null) {
                updateListener.onError("Error processing game update: " + e.getMessage());
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
    }

    public void cleanup() {
        if (gameListener != null) {
            firebaseController.removeGameListener(gameId, gameListener);
        }
    }
}