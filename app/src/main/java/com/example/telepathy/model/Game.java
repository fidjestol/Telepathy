package com.example.telepathy.model;

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
    }

    public GameRound getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(GameRound currentRound) {
        this.currentRound = currentRound;
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
        roundCount++;
        currentRound = new GameRound(roundCount, durationMillis, words);
        status = "active";
    }

    public void endRound() {
        status = "roundEnd";
        processDuplicateWords();
    }

    private void processDuplicateWords() {
        if (currentRound == null) return;

        // Find duplicate words and penalize players
        for (String word : currentRound.findDuplicateWords().keySet()) {
            List<String> playerIds = currentRound.findDuplicateWords().get(word);
            for (String playerId : playerIds) {
                Player player = getPlayerById(playerId);
                if (player != null) {
                    player.loseLife();
                }
            }
        }

        // Check if game is over (only one player left)
        int activePlayers = 0;
        Player lastActivePlayer = null;

        for (Player player : players) {
            if (!player.isEliminated()) {
                activePlayers++;
                lastActivePlayer = player;
            }
        }

        if (activePlayers <= 1) {
            endGame();
            if (lastActivePlayer != null) {
                lastActivePlayer.addPoints(100); // Bonus points for winning
            }
        }
    }

    public void endGame() {
        status = "gameEnd";
    }

    public boolean isGameOver() {
        return "gameEnd".equals(status);
    }

    public List<Player> getActivePlayers() {
        List<Player> activePlayers = new ArrayList<>();
        for (Player player : players) {
            if (!player.isEliminated()) {
                activePlayers.add(player);
            }
        }
        return activePlayers;
    }

    public Player getWinner() {
        if (!isGameOver()) return null;

        for (Player player : players) {
            if (!player.isEliminated()) {
                return player;
            }
        }
        return null;
    }
}