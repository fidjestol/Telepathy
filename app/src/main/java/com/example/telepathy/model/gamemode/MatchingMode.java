package com.example.telepathy.model.gamemode;

import com.example.telepathy.model.GameRound;
import com.example.telepathy.model.Player;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class MatchingMode extends BaseGameMode {
    private static final String GAME_MODE_TYPE = "matching";
    private static final int MAX_PLAYERS = 2;
    private static final int POINTS_FOR_MATCH = 50;

    @Override
    public void startRound() {
        roundCount++;
        // Pass null to indicate no word validation is needed
        currentRound = new GameRound(roundCount, getRoundTimeLimit(), null);
        status = "active";
    }

    @Override
    public void endRound() {
        status = "roundEnd";
        processRoundResults();
    }

    @Override
    public boolean isRoundComplete() {
        if (currentRound == null) return false;

        // Round is complete when both players have submitted words
        Map<String, List<String>> playerWords = currentRound.getPlayerWords();
        if (playerWords == null) return false;

        int submittedCount = 0;
        for (List<String> words : playerWords.values()) {
            if (words != null && !words.isEmpty()) {
                submittedCount++;
            }
        }
        return submittedCount >= MAX_PLAYERS;
    }

    @Override
    public boolean isGameOver() {
        return "gameEnd".equals(status);
    }

    @Override
    public String getGameModeType() {
        return GAME_MODE_TYPE;
    }

    @Override
    public int getMaxPlayers() {
        return MAX_PLAYERS;
    }

    @Override
    public boolean submitWord(String playerId, String word) {
        if (currentRound == null) {
            return false;
        }

        // Basic validation - just check if word is not empty
        if (word == null || word.trim().isEmpty()) {
            return false;
        }

        // In matching mode, we just store the word directly
        Map<String, List<String>> playerWords = currentRound.getPlayerWords();
        if (playerWords == null) {
            playerWords = new HashMap<>();
            currentRound.setPlayerWords(playerWords);
        }

        // Store the word in a single-item list
        List<String> words = new ArrayList<>();
        words.add(word.toLowerCase().trim());
        playerWords.put(playerId, words);
        return true;
    }

    @Override
    public void processRoundResults() {
        if (currentRound == null) return;

        Map<String, List<String>> playerWords = currentRound.getPlayerWords();
        if (playerWords == null || playerWords.size() != MAX_PLAYERS) return;

        // Get words from both players
        String word1 = null;
        String word2 = null;
        String player1Id = null;
        String player2Id = null;

        for (Map.Entry<String, List<String>> entry : playerWords.entrySet()) {
            List<String> words = entry.getValue();
            if (words != null && !words.isEmpty()) {
                if (word1 == null) {
                    word1 = words.get(0).toLowerCase().trim();
                    player1Id = entry.getKey();
                } else {
                    word2 = words.get(0).toLowerCase().trim();
                    player2Id = entry.getKey();
                }
            }
        }

        // Check if words match
        if (word1 != null && word2 != null && word1.equals(word2)) {
            // Words match - game ends
            status = "gameEnd";
            // Award points to both players
            Player player1 = getPlayerById(player1Id);
            Player player2 = getPlayerById(player2Id);
            if (player1 != null) player1.addPoints(POINTS_FOR_MATCH);
            if (player2 != null) player2.addPoints(POINTS_FOR_MATCH);
        }
        // If words don't match, continue to next round
    }

    @Override
    protected boolean validateGameSpecificWord(String word) {
        // In matching mode, any non-empty word is valid
        return true;
    }

    @Override
    public Player getWinner() {
        if (!isGameOver()) return null;

        // In matching mode, both players win if they match words
        // Return the player with the highest score, or the first player if tied
        Player winner = null;
        int highestScore = -1;

        for (Player player : players) {
            if (player.getScore() > highestScore) {
                highestScore = player.getScore();
                winner = player;
            }
        }

        return winner;
    }
}