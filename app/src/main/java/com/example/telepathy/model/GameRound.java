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
    private boolean requiresWordValidation;
    private Map<String, String> submittedWords;

    // Default constructor for Firebase
    public GameRound() {
        this.roundNumber = 1;
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + (30 * 1000); // Default 30 seconds
        this.words = new ArrayList<>();
        this.playerWords = new HashMap<>();
        this.submittedWords = new HashMap<>();
        this.requiresWordValidation = true;
    }

    public GameRound(int roundNumber, long durationMillis, List<String> words) {
        this.roundNumber = roundNumber;
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + durationMillis;
        this.words = words != null ? words : new ArrayList<>();
        this.playerWords = new HashMap<>();
        this.submittedWords = new HashMap<>();
        this.requiresWordValidation = true;
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
        this.words = words != null ? words : new ArrayList<>();
    }

    public Map<String, List<String>> getPlayerWords() {
        return playerWords;
    }

    public void setPlayerWords(Map<String, List<String>> playerWords) {
        this.playerWords = playerWords;
    }

    public Map<String, String> getSubmittedWords() {
        return submittedWords;
    }

    public void setSubmittedWords(Map<String, String> submittedWords) {
        this.submittedWords = submittedWords != null ? submittedWords : new HashMap<>();
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
        // Basic validation - word must not be null or empty
        if (word == null || word.trim().isEmpty()) {
            return false;
        }

        // If words list is null or empty, or if validation is not required, any
        // non-empty word is valid
        if (words == null || words.isEmpty() || !requiresWordValidation) {
            return true;
        }

        // Otherwise, check if the word is in the predefined list
        return words.contains(word.toLowerCase().trim());
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