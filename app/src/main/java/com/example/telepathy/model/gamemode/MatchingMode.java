package com.example.telepathy.model.gamemode;

import com.example.telepathy.model.GameRound;
import com.example.telepathy.model.Player;
import java.util.List;
import java.util.Map;

public class MatchingMode extends BaseGameMode {
    private static final String GAME_MODE_TYPE = "matching";
    private static final int MAX_PLAYERS = 2;

    @Override
    public void startRound() {
        roundCount++;
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
        if (currentRound == null)
            return false;

        // Round is complete when both players have submitted words
        Map<String, List<String>> playerWords = currentRound.getPlayerWords();
        return playerWords != null && playerWords.size() == MAX_PLAYERS;
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
        if (!validateWord(word) || currentRound == null) {
            return false;
        }

        // Add word to current round
        currentRound.addPlayerWord(playerId, word);
        return true;
    }

    @Override
    public void processRoundResults() {
        if (currentRound == null)
            return;

        Map<String, List<String>> playerWords = currentRound.getPlayerWords();
        if (playerWords == null || playerWords.size() != MAX_PLAYERS)
            return;

        // Get words from both players
        String word1 = null;
        String word2 = null;

        for (Map.Entry<String, List<String>> entry : playerWords.entrySet()) {
            List<String> words = entry.getValue();
            if (words != null && !words.isEmpty()) {
                if (word1 == null) {
                    word1 = words.get(0);
                } else {
                    word2 = words.get(0);
                }
            }
        }

        // Check if words match
        if (word1 != null && word2 != null && word1.equalsIgnoreCase(word2)) {
            // Words match - game ends
            status = "gameEnd";
            // Both players win
            for (Player player : players) {
                player.addPoints(50); // Award points to both players
            }
        }
        // If words don't match, continue to next round
    }

    @Override
    protected boolean validateGameSpecificWord(String word) {

        return true; // Basic validation already done in parent class
    }

    @Override
    public Player getWinner() {
        if (!isGameOver())
            return null;

        // In matching mode, both players win if they match words
        // Return the first player as both are winners
        return players.isEmpty() ? null : players.get(0);
    }
}