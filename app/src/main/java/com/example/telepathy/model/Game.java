package com.example.telepathy.model;

import com.example.telepathy.model.gamemode.GameMode;
import com.example.telepathy.model.gamemode.GameModeFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Game {
    private String gameId;
    private String lobbyId;
    private GameConfig config;
    private List<Player> players;
    private GameRound currentRound;
    private int roundCount;
    private String status; // "active", "roundEnd", "gameEnd"
    private GameMode gameMode;

    // Default constructor for Firebase
    public Game() {
        this.players = new ArrayList<>();
        this.roundCount = 0;
        this.status = "active";
    }

    public Game(String gameId, GameConfig config, List<Player> players) {
        this.gameId = gameId;
        this.config = config != null ? config : new GameConfig();
        this.players = players != null ? players : new ArrayList<>();
        this.roundCount = 0;
        this.status = "active";
        initializeGameMode();
    }

    public void initializeGameMode() {
        if (config != null) {
            this.gameMode = GameModeFactory.createGameMode(config.getGameMode());
            if (this.gameMode != null) {
                this.gameMode.initializeGame(gameId, config);
                for (Player player : players) {
                    this.gameMode.addPlayer(player);
                }
            }
        }
    }

    private Set<String> usedWords = new HashSet<>();

    public void addUsedWord(String word) {
        if (word != null && !word.isEmpty()) {
            usedWords.add(word.toLowerCase());
        }
    }

    public boolean isWordAlreadyUsed(String word) {
        return word != null && usedWords.contains(word.toLowerCase());
    }

    // Getters and setters
    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    public GameConfig getConfig() {
        return config;
    }

    public void setConfig(GameConfig config) {
        this.config = config;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
        // Update game mode with new players if it exists
        if (this.gameMode != null) {
            for (Player player : players) {
                this.gameMode.addPlayer(player);
            }
        }
    }

    public GameRound getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(GameRound round) {
        this.currentRound = round;
        if (this.gameMode != null) {
            this.gameMode.setCurrentRound(round);
        }
    }

    public int getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(int roundCount) {
        this.roundCount = roundCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Helper methods
    public void addPlayer(Player player) {
        if (player != null && !playerExists(player.getId())) {
            players.add(player);
            if (gameMode != null) {
                gameMode.addPlayer(player);
            }
        }
    }

    public boolean playerExists(String playerId) {
        for (Player player : players) {
            if (player.getId().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    public Player getPlayerById(String playerId) {
        for (Player player : players) {
            if (player.getId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    public void startNewRound(List<String> words, long durationMillis) {
        if (gameMode != null) {
            gameMode.startRound();
            this.roundCount = gameMode.getCurrentRound();
            this.currentRound = new GameRound(this.roundCount, durationMillis, words);
            gameMode.setCurrentRound(this.currentRound);
            status = gameMode.getGameStatus();
        }
    }

    public void endRound() {
        if (gameMode != null) {
            gameMode.endRound();
            status = gameMode.getGameStatus();
        }
    }

    public boolean isGameOver() {
        return gameMode != null && gameMode.isGameOver();
    }

    public List<Player> getActivePlayers() {
        return gameMode != null ? gameMode.getActivePlayers() : new ArrayList<>();
    }

    public Player getWinner() {
        return gameMode != null ? gameMode.getWinner() : null;
    }

    public boolean submitWord(String playerId, String word) {
        return gameMode != null && gameMode.submitWord(playerId, word);
    }

    public boolean isRoundComplete() {
        return gameMode != null && gameMode.isRoundComplete();
    }

    public GameMode getGameMode() {
        return gameMode;
    }
}