package com.example.telepathy.controller;

import android.os.Handler;

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

            // Extract data from Firebase
            GameConfig config = extractGameConfig(gameData);
            List<Player> players = extractPlayers(gameData);
            GameRound round = extractRoundData(gameData);

            // Check if all players have submitted words
            boolean shouldEndRound = checkAllPlayersSubmitted(players) && "active".equals(status);

            // Check if this is a new round
            boolean isNewRound = isNewRound(round);

            // Update game state
            updateGameState(config, players, round, status);

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
    }

    public void cleanup() {
        if (gameListener != null) {
            firebaseController.removeGameListener(gameId, gameListener);
        }
    }

}