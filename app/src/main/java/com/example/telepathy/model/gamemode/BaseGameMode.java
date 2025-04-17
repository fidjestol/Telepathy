package com.example.telepathy.model.gamemode;

import com.example.telepathy.model.GameConfig;
import com.example.telepathy.model.GameRound;
import com.example.telepathy.model.Player;
import com.example.telepathy.model.gamemode.GameMode;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseGameMode implements GameMode {
    protected String gameId;
    protected GameConfig config;
    protected List<Player> players;
    protected GameRound currentRound;
    protected int roundCount;
    protected String status; // "active", "roundEnd", "gameEnd"

    // Constructor
    public BaseGameMode() {
        this.players = new ArrayList<>();
        this.roundCount = 0;
        this.status = "active";
    }

    @Override
    public void initializeGame(String gameId, GameConfig config) {
        this.gameId = gameId;
        this.config = config != null ? config : new GameConfig();
        this.status = "active";
    }

    @Override
    public boolean addPlayer(Player player) {
        if (player != null && !playerExists(player.getId()) && players.size() < getMaxPlayers()) {
            players.add(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean removePlayer(String playerId) {
        return players.removeIf(player -> player.getId().equals(playerId));
    }

    @Override
    public List<Player> getActivePlayers() {
        List<Player> activePlayers = new ArrayList<>();
        for (Player player : players) {
            if (!player.isEliminated()) {
                activePlayers.add(player);
            }
        }
        return activePlayers;
    }

    @Override
    public int getMaxPlayers() {
        return config != null ? config.getMaxPlayers() : 8;
    }

    @Override
    public long getRoundTimeLimit() {
        return config != null ? config.getTimeLimit() * 1000L : 30000L;
    }

    @Override
    public String getGameStatus() {
        return status;
    }

    @Override
    public GameConfig getGameConfig() {
        return config;
    }

    @Override
    public int getCurrentRound() {
        return roundCount;
    }

    @Override
    public void setCurrentRound(GameRound round) {
        this.currentRound = round;
    }

    // Abstract methods from GameMode interface that must be implemented by specific
    // game modes
    @Override
    public abstract void startRound();

    @Override
    public abstract void endRound();

    @Override
    public abstract boolean isRoundComplete();

    @Override
    public abstract boolean isGameOver();

    @Override
    public abstract boolean submitWord(String playerId, String word);

    @Override
    public abstract void processRoundResults();

    @Override
    public abstract Player getWinner();

    @Override
    public abstract String getGameModeType();

    // Helper methods
    protected boolean playerExists(String playerId) {
        for (Player player : players) {
            if (player.getId().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    protected Player getPlayerById(String playerId) {
        for (Player player : players) {
            if (player.getId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    // Abstract method for specific game modes
    protected abstract boolean validateGameSpecificWord(String word);

    // Common word validation that can be extended
    @Override
    public boolean validateWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        return validateGameSpecificWord(word);
    }
}
