package com.example.telepathy.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRound {
    private int roundNumber;
    private long startTime;
    private long endTime;
    private List<String> words;
    private Map<String, List<String>> playerWords; // Maps player IDs to their submitted words

    // Default constructor for Firebase
    public GameRound() {
        this.roundNumber = 1;
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + (30 * 1000); // Default 30 seconds
        this.words = new ArrayList<>();
        this.playerWords = new HashMap<>();
    }

    public GameRound(int roundNumber, long durationMillis, List<String> words) {
        this.roundNumber = roundNumber;
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + durationMillis;
        this.words = words != null ? words : new ArrayList<>();
        this.playerWords = new HashMap<>();
    }

    // Getters and setters
    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    public Map<String, List<String>> getPlayerWords() {
        return playerWords;
    }

    public void setPlayerWords(Map<String, List<String>> playerWords) {
        this.playerWords = playerWords;
    }

    // Helper methods
    public void addWord(String word) {
        if (word != null && !word.isEmpty() && !words.contains(word.toLowerCase())) {
            words.add(word.toLowerCase());
        }
    }

    public void addPlayerWord(String playerId, String word) {
        if (playerId != null && word != null && !word.isEmpty()) {
            if (!playerWords.containsKey(playerId)) {
                playerWords.put(playerId, new ArrayList<>());
            }
            playerWords.get(playerId).add(word.toLowerCase());
        }
    }

    public boolean isWordValid(String word) {
        return word != null && !word.isEmpty() && words.contains(word.toLowerCase());
    }

    public boolean hasTimeExpired() {
        return System.currentTimeMillis() > endTime;
    }

    public long getRemainingTimeMillis() {
        long remainingTime = endTime - System.currentTimeMillis();
        return Math.max(0, remainingTime);
    }

    public Map<String, List<String>> findDuplicateWords() {
        // Maps words to list of player IDs who submitted them
        Map<String, List<String>> wordToPlayers = new HashMap<>();

        // Group players by submitted words
        for (Map.Entry<String, List<String>> entry : playerWords.entrySet()) {
            String playerId = entry.getKey();
            List<String> submittedWords = entry.getValue();

            for (String word : submittedWords) {
                if (!wordToPlayers.containsKey(word)) {
                    wordToPlayers.put(word, new ArrayList<>());
                }
                wordToPlayers.get(word).add(playerId);
            }
        }

        // Filter to keep only words with multiple players
        Map<String, List<String>> duplicateWords = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : wordToPlayers.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateWords.put(entry.getKey(), entry.getValue());
            }
        }

        return duplicateWords;
    }
}