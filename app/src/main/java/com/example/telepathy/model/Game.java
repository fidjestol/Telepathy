package com.example.telepathy.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Game {
    private String gameId;
    private String lobbyId;
    private GameConfig config;
    private List<Player> players;
    private GameRound currentRound;
    private int roundCount;
    private String status; // "active", "roundEnd", "gameEnd"
    private Set<String> usedWords; // Track all used words across rounds

    // Default constructor for Firebase
    public Game() {
        this.players = new ArrayList<>();
        this.roundCount = 0;
        this.status = "active";
        this.usedWords = new HashSet<>();
    }

    public Game(String gameId, GameConfig config, List<Player> players) {
        this.gameId = gameId;
        this.config = config != null ? config : new GameConfig();
        this.players = players != null ? players : new ArrayList<>();
        this.roundCount = 0;
        this.status = "active";
        this.usedWords = new HashSet<>();
    }

    // Methods for tracking used words
    public void addUsedWord(String word) {
        if (word != null && !word.isEmpty()) {
            usedWords.add(word.toLowerCase());
            System.out.println("TELEPATHY: Added word to used list: " + word.toLowerCase());
        }
    }

    public boolean isWordAlreadyUsed(String word) {
        return word != null && usedWords.contains(word.toLowerCase());
    }

    public Set<String> getUsedWords() {
        return new HashSet<>(usedWords);
    }

    // Add a setter for usedWords
    public void setUsedWords(Set<String> usedWords) {
        this.usedWords = new HashSet<>(usedWords);
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
        System.out.println("TELEPATHY: Started new round " + roundCount + " with " + words.size() + " words");
    }

    public void endRound() {
        status = "roundEnd";

        // Only process duplicate words in classic mode
        if (!config.isMatchingMode()) {
            processDuplicateWords();
        }

        // Add all submitted words from players to the usedWords set
        for (Player player : players) {
            String word = player.getCurrentWord();
            if (word != null && !word.isEmpty()) {
                addUsedWord(word);
                System.out.println("TELEPATHY: Added word to used list at round end: " + word);
            }
        }

        // In matching mode, check if any words match to end the game
        if (config.isMatchingMode()) {
            checkForMatchingWords();
        }

        System.out.println("TELEPATHY: Round ended, total used words: " + usedWords.size());
    }

    private void checkForMatchingWords() {
        if (currentRound == null)
            return;

        Map<String, List<String>> duplicates = currentRound.findDuplicateWords();
        if (!duplicates.isEmpty()) {
            // Found matching words, end the game
            status = "gameEnd";
            System.out.println("TELEPATHY: Game ended - matching words found!");

            // Award points to players who matched
            for (List<String> playerIds : duplicates.values()) {
                for (String playerId : playerIds) {
                    Player player = getPlayerById(playerId);
                    if (player != null) {
                        player.addPoints(20); // Award 20 points for matching
                        System.out.println(
                                "TELEPATHY: Player " + player.getUsername() + " awarded 20 points for matching word");
                    }
                }
            }
        }
    }

    private void processDuplicateWords() {
        if (currentRound == null)
            return;

        // Find duplicate words and penalize players (classic mode only)
        for (String word : currentRound.findDuplicateWords().keySet()) {
            List<String> playerIds = currentRound.findDuplicateWords().get(word);
            System.out
                    .println("TELEPATHY: Found duplicate word: " + word + " used by " + playerIds.size() + " players");

            for (String playerId : playerIds) {
                Player player = getPlayerById(playerId);
                if (player != null) {
                    player.loseLife();
                    System.out.println(
                            "TELEPATHY: Player " + player.getUsername() + " lost a life for duplicate word: " + word);
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

        System.out.println("TELEPATHY: Active players remaining: " + activePlayers);

        if (activePlayers <= 1) {
            endGame();
            if (lastActivePlayer != null) {
                lastActivePlayer.addPoints(100); // Bonus points for winning
                System.out.println("TELEPATHY: Player " + lastActivePlayer.getUsername() + " wins the game!");
            }
        }
    }

    public void endGame() {
        status = "gameEnd";
        System.out.println("TELEPATHY: Game ended");
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
        if (!isGameOver())
            return null;

        for (Player player : players) {
            if (!player.isEliminated()) {
                return player;
            }
        }
        return null;
    }
}