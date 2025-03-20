package com.example.telepathy.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoringSystem {
    // Points awarded for unique words
    private static final int POINTS_FOR_UNIQUE_WORD = 10;

    // Points deducted for duplicate words
    private static final int PENALTY_FOR_DUPLICATE = -5;

    // Bonus points for winning the game
    private static final int WINNING_BONUS = 100;

    /**
     * Calculate scores for all players based on their submitted words
     * @param round The game round with player words
     * @param players List of players in the game
     */
    public static void calculateScores(GameRound round, List<Player> players) {
        // Find duplicate words
        Map<String, List<String>> duplicateWords = round.findDuplicateWords();

        // Calculate unique words for each player
        Map<String, Integer> uniqueWordCounts = new HashMap<>();

        for (Player player : players) {
            if (player.isEliminated()) continue;

            String playerId = player.getId();
            List<String> playerWords = round.getPlayerWords().get(playerId);

            if (playerWords == null) continue;

            int uniqueCount = 0;

            // Count unique words (not in duplicate list)
            for (String word : playerWords) {
                if (!duplicateWords.containsKey(word)) {
                    uniqueCount++;
                }
            }

            uniqueWordCounts.put(playerId, uniqueCount);
        }

        // Award points for unique words
        for (Player player : players) {
            if (player.isEliminated()) continue;

            String playerId = player.getId();

            // Add points for unique words
            int uniquePoints = uniqueWordCounts.getOrDefault(playerId, 0) * POINTS_FOR_UNIQUE_WORD;
            player.addPoints(uniquePoints);

            // Deduct points for duplicate words
            List<String> playerWords = round.getPlayerWords().get(playerId);
            if (playerWords != null) {
                int duplicates = 0;
                for (String word : playerWords) {
                    if (duplicateWords.containsKey(word)) {
                        duplicates++;
                    }
                }

                int penaltyPoints = duplicates * PENALTY_FOR_DUPLICATE;
                player.addPoints(penaltyPoints);
            }
        }
    }

    /**
     * Award bonus points to the winner
     * @param winner The winning player
     */
    public static void awardWinningBonus(Player winner) {
        if (winner != null) {
            winner.addPoints(WINNING_BONUS);
        }
    }
}