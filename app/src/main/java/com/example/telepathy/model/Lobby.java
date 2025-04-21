package com.example.telepathy.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Lobby {
    private String id;
    private String name;
    private List<Player> players;
    private GameConfig gameConfig;
    private boolean isOpen;
    private String hostId;
    private String gameId;

    // Default constructor for Firebase
    public Lobby() {
        // Required empty constructor for Firebase
    }

    public Lobby(String name, Player host) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.players = new ArrayList<>();
        this.gameConfig = new GameConfig(); // Default configuration
        this.isOpen = true;

        // Add host as first player
        host.setHost(true);
        this.players.add(host);
        this.hostId = host.getId();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public boolean addPlayer(Player player) {
        if (players.size() < gameConfig.getMaxPlayers() && isOpen) {
            players.add(player);
            return true;
        }
        return false;
    }

    public boolean removePlayer(String playerId) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId().equals(playerId)) {
                players.remove(i);
                // If host leaves, assign a new host if there are players left
                if (playerId.equals(hostId) && !players.isEmpty()) {
                    Player newHost = players.get(0);
                    newHost.setHost(true);
                    hostId = newHost.getId();
                }
                return true;
            }
        }
        return false;
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }

    public void setGameConfig(GameConfig gameConfig) {
        this.gameConfig = gameConfig;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public boolean isLobbyFull() {
        return players.size() >= gameConfig.getMaxPlayers();
    }

    public boolean canStartGame() {
        return players.size() >= 2; // Need at least 2 players to start
    }
}