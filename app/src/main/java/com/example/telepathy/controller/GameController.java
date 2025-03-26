package com.example.telepathy.controller;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.telepathy.model.Game;
import com.example.telepathy.model.GameConfig;
import com.example.telepathy.model.GameRound;
import com.example.telepathy.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GameController {
    private Game currentGame;

    // Added final as requested by code analysis, hope there are no consequences
    private final FirebaseController firebaseController;
    private final String gameId;
    private final String currentPlayerId;
    private ValueEventListener gameListener;
    private final GameUpdateListener updateListener;

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
        initGameListener();
    }

    private void initGameListener() {
        gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> gameData = (Map<String, Object>) snapshot.getValue();
                if (gameData != null) {
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

        firebaseController.listenForGameUpdates(gameId, gameListener);
    }

    private void processGameUpdate(Map<String, Object> gameData) {
        try {
            String status = (String) gameData.get("status");
            Object roundObj = gameData.get("currentRound");
            int currentRound = (roundObj instanceof Long) ? ((Long) roundObj).intValue() : 1;

            Map<String, Object> configData = (Map<String, Object>) gameData.get("config");
            GameConfig config = new GameConfig();
            if (configData != null) {
                // Changed to requiredNonNull
                config.setTimeLimit(((Long) Objects.requireNonNull(configData.get("timeLimit"))).intValue());
                config.setMaxPlayers(((Long) Objects.requireNonNull(configData.get("maxPlayers"))).intValue());
                config.setLivesPerPlayer(((Long) Objects.requireNonNull(configData.get("livesPerPlayer"))).intValue());
                config.setSelectedCategory((String) configData.get("selectedCategory"));

                FirebaseDatabase.getInstance().getReference().child("category")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                List<String> categories = new ArrayList<>();
                                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                                    categories.add(categorySnapshot.getKey());
                                }
                                config.setCategories(categories);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                if (updateListener != null) {
                                    updateListener.onError("Failed to load categories: " + error.getMessage());
                                }
                            }
                        });
            }

            List<Player> players = new ArrayList<>();
            Map<String, Object> playersData = (Map<String, Object>) gameData.get("players");
            if (playersData != null) {
                for (Map.Entry<String, Object> entry : playersData.entrySet()) {
                    Map<String, Object> playerData = (Map<String, Object>) entry.getValue();
                    Player player = new Player();
                    player.setId(entry.getKey());
                    player.setUsername((String) playerData.get("username"));
                    // Changed to requireNonNull
                    player.setScore(((Long) Objects.requireNonNull(playerData.get("score"))).intValue());
                    player.setLives(((Long) Objects.requireNonNull(playerData.get("lives"))).intValue());
                    player.setEliminated((Boolean) playerData.get("eliminated"));
                    player.setCurrentWord((String) playerData.get("currentWord"));
                    players.add(player);
                }
            }

            Map<String, Object> roundData = (Map<String, Object>) gameData.get("currentRound");
            GameRound round = new GameRound();
            if (roundData != null) {
                // changed to requireNonNull
                round.setRoundNumber(((Long) Objects.requireNonNull(roundData.get("roundNumber"))).intValue());
                round.setStartTime((Long) Objects.requireNonNull(roundData.get("startTime")));
                round.setEndTime((Long) roundData.get("endTime"));

                List<String> words = new ArrayList<>();
                List<Object> wordsData = (List<Object>) roundData.get("words");
                if (wordsData != null) {
                    for (Object word : wordsData) {
                        words.add((String) word);
                    }
                }
                round.setWords(words);
            }

            if (currentGame == null) {
                currentGame = new Game(gameId, config, players);
            } else {
                currentGame.setConfig(config);
                currentGame.setPlayers(players);
            }

            currentGame.setCurrentRound(round);
            currentGame.setStatus(status);

            if ("active".equals(status)) {
                if (updateListener != null) {
                    updateListener.onGameStateChanged(currentGame);
                    if (currentGame.getCurrentRound().getRoundNumber() != currentRound) {
                        updateListener.onRoundStart(currentGame.getCurrentRound());
                    }
                }
            } else if ("roundEnd".equals(status)) {
                if (updateListener != null) {
                    updateListener.onRoundEnd(currentGame.getCurrentRound());
                    for (Player player : players) {
                        if (player.isEliminated()) {
                            updateListener.onPlayerEliminated(player);
                        }
                    }
                }
            } else if ("gameEnd".equals(status)) {
                if (updateListener != null) {
                    Player winner = null;
                    for (Player player : players) {
                        if (!player.isEliminated()) {
                            winner = player;
                            break;
                        }
                    }
                    updateListener.onGameEnd(winner);
                }
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
            public void onSuccess(Object result) {}

            @Override
            public void onFailure(String error) {
                if (updateListener != null) {
                    updateListener.onError("Failed to submit word: " + error);
                }
            }
        });
    }

    public void validateWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            if (updateListener != null) {
                updateListener.onError("Word cannot be empty");
            }
            return;
        }

        if (currentGame != null && currentGame.getCurrentRound() != null) {
            List<String> validWords = currentGame.getCurrentRound().getWords();
            if (validWords != null && !validWords.contains(word.trim().toLowerCase())) {
                if (updateListener != null) {
                    updateListener.onError("Word is not in the valid word list");
                }
                return;
            }
        }

        submitWord(word.trim().toLowerCase());
    }

    public void cleanup() {
        if (gameListener != null) {
            firebaseController.removeGameListener(gameId, gameListener);
        }
    }
}
