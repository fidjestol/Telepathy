package com.example.telepathy.model.gamemode;

import com.example.telepathy.model.GameRound;
import com.example.telepathy.model.Player;
import com.example.telepathy.utils.WordListProvider;
import java.util.List;
import java.util.Map;

public class ClassicMode extends BaseGameMode {
    private static final String GAME_MODE_TYPE = "classic";
    private static final int POINTS_FOR_UNIQUE_WORD = 10;
    private static final int PENALTY_FOR_DUPLICATE = -5;
    private static final int WINNING_BONUS = 100;

    @Override
    public void startRound() {
        roundCount++;
        // Get word list for the selected category
        List<String> wordList = WordListProvider.getWordsForCategory(config.getSelectedCategory());
        currentRound = new GameRound(roundCount, getRoundTimeLimit(), wordList);
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

        // Round is complete when all active players have submitted words
        int activePlayerCount = 0;
        int submittedWordCount = 0;
        Map<String, List<String>> playerWords = currentRound.getPlayerWords();

        for (Player player : players) {
            if (!player.isEliminated()) {
                activePlayerCount++;
                if (playerWords != null && playerWords.containsKey(player.getId()) &&
                        !playerWords.get(player.getId()).isEmpty()) {
                    submittedWordCount++;
                }
            }
        }

        return activePlayerCount > 0 && submittedWordCount == activePlayerCount;
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

        Map<String, List<String>> duplicateWords = currentRound.findDuplicateWords();

        // Process duplicate words and penalize players
        for (Map.Entry<String, List<String>> entry : duplicateWords.entrySet()) {
            for (String playerId : entry.getValue()) {
                Player player = getPlayerById(playerId);
                if (player != null && !player.isEliminated()) {
                    player.loseLife();
                    player.addPoints(PENALTY_FOR_DUPLICATE);
                }
            }
        }

        // Award points for unique words
        Map<String, List<String>> playerWords = currentRound.getPlayerWords();
        if (playerWords != null) {
            for (Map.Entry<String, List<String>> entry : playerWords.entrySet()) {
                String playerId = entry.getKey();
                List<String> words = entry.getValue();
                Player player = getPlayerById(playerId);

                if (player != null && !player.isEliminated() && words != null) {
                    for (String word : words) {
                        if (!duplicateWords.containsKey(word)) {
                            player.addPoints(POINTS_FOR_UNIQUE_WORD);
                        }
                    }
                }
            }
        }

        // Check if game is over
        int activePlayers = 0;
        Player lastActivePlayer = null;

        for (Player player : players) {
            if (!player.isEliminated()) {
                activePlayers++;
                lastActivePlayer = player;
            }
        }

        if (activePlayers <= 1) {
            status = "gameEnd";
            if (lastActivePlayer != null) {
                lastActivePlayer.addPoints(WINNING_BONUS);
            }
        }
    }

    @Override
    protected boolean validateGameSpecificWord(String word) {
        return true; // Basic validation already done in parent class
    }

    @Override
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