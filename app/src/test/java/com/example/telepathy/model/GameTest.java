package com.example.telepathy.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameTest {

    private Game game;
    private Player player1;
    private Player player2;
    private Player player3;
    private GameConfig config;

    @Before
    public void setUp() {
        // Create test players
        player1 = new Player("Player1");
        player2 = new Player("Player2");
        player3 = new Player("Player3");

        // Create config
        config = new GameConfig(30, 5, 3, "Animals");

        // Create a game
        List<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);

        game = new Game("test-game-id", config, players);
    }

    @Test
    public void testDefaultConstructor() {
        Game defaultGame = new Game();

        assertNotNull("Players list should be initialized", defaultGame.getPlayers());
        assertEquals("Round count should be 0", 0, defaultGame.getRoundCount());
        assertEquals("Status should be active", "active", defaultGame.getStatus());
    }

    @Test
    public void testParameterizedConstructor() {
        assertEquals("Game ID should match", "test-game-id", game.getGameId());
        assertEquals("Config should match", config, game.getConfig());
        assertEquals("Players size should be 2", 2, game.getPlayers().size());
        assertEquals("Round count should be 0", 0, game.getRoundCount());
        assertEquals("Status should be active", "active", game.getStatus());
    }

    @Test
    public void testAddPlayer() {
        // Start with 2 players
        assertEquals("Initial player count should be 2", 2, game.getPlayers().size());

        // Add a new player
        game.addPlayer(player3);
        assertEquals("Player count should be 3", 3, game.getPlayers().size());

        // Try to add the same player again
        game.addPlayer(player3);
        assertEquals("Player count should still be 3", 3, game.getPlayers().size());

        // Try to add a null player
        game.addPlayer(null);
        assertEquals("Player count should still be 3", 3, game.getPlayers().size());
    }

    @Test
    public void testPlayerExists() {
        assertTrue("Player1 should exist", game.playerExists(player1.getId()));
        assertTrue("Player2 should exist", game.playerExists(player2.getId()));
        assertFalse("Player3 should not exist", game.playerExists(player3.getId()));
        assertFalse("Random ID should not exist", game.playerExists("random-id"));
    }

    @Test
    public void testGetPlayerById() {
        assertEquals("Should get Player1", player1, game.getPlayerById(player1.getId()));
        assertEquals("Should get Player2", player2, game.getPlayerById(player2.getId()));
        assertNull("Should return null for non-existent player", game.getPlayerById(player3.getId()));
        assertNull("Should return null for random ID", game.getPlayerById("random-id"));
    }

    @Test
    public void testStartNewRound() {
        List<String> words = Arrays.asList("cat", "dog", "elephant");
        long duration = 30000; // 30 seconds

        game.startNewRound(words, duration);

        assertEquals("Round count should be 1", 1, game.getRoundCount());
        assertNotNull("Current round should not be null", game.getCurrentRound());
        assertEquals("Round number should be 1", 1, game.getCurrentRound().getRoundNumber());
        assertEquals("Status should be active", "active", game.getStatus());
    }

    @Test
    public void testEndRound_NoDuplicates() {
        // Set up a round
        List<String> words = Arrays.asList("cat", "dog", "elephant");
        game.startNewRound(words, 30000);

        // Add player words - no duplicates
        GameRound round = game.getCurrentRound();
        round.addPlayerWord(player1.getId(), "cat");
        round.addPlayerWord(player2.getId(), "dog");

        // End the round
        game.endRound();

        assertEquals("Status should be roundEnd", "roundEnd", game.getStatus());
        assertEquals("Player1 should have 3 lives", 3, player1.getLives());
        assertEquals("Player2 should have 3 lives", 3, player2.getLives());
        assertFalse("Player1 should not be eliminated", player1.isEliminated());
        assertFalse("Player2 should not be eliminated", player2.isEliminated());
    }

    @Test
    public void testEndRound_WithDuplicates() {
        // Set up a round
        List<String> words = Arrays.asList("cat", "dog", "elephant");
        game.startNewRound(words, 30000);

        // Add player words - with duplicate "cat"
        GameRound round = game.getCurrentRound();
        round.addPlayerWord(player1.getId(), "cat");
        round.addPlayerWord(player2.getId(), "cat");

        // End the round
        game.endRound();

        assertEquals("Status should be roundEnd", "roundEnd", game.getStatus());
        assertEquals("Player1 should have 2 lives", 2, player1.getLives());
        assertEquals("Player2 should have 2 lives", 2, player2.getLives());
        assertFalse("Player1 should not be eliminated", player1.isEliminated());
        assertFalse("Player2 should not be eliminated", player2.isEliminated());
    }

    @Test
    public void testGameOver_WhenOnePlayerRemains() {
        // Set up a round
        List<String> words = Arrays.asList("cat", "dog", "elephant");
        game.startNewRound(words, 30000);

        // Eliminate player1 by setting lives to 0
        player1.setLives(0);
        player1.setEliminated(true);

        // End the round
        game.endRound();

        assertTrue("Game should be over", game.isGameOver());
        assertEquals("Status should be gameEnd", "gameEnd", game.getStatus());
        assertEquals("Winner should be player2", player2, game.getWinner());
        assertEquals("Player2 should have bonus points", 100, player2.getScore());
    }

    @Test
    public void testGetActivePlayers() {
        assertEquals("Active players should be 2", 2, game.getActivePlayers().size());

        // Eliminate player1
        player1.setLives(0);
        player1.setEliminated(true);

        assertEquals("Active players should be 1", 1, game.getActivePlayers().size());
        assertEquals("Active player should be player2", player2, game.getActivePlayers().get(0));
    }

    @Test
    public void testGetWinner_NoWinnerYet() {
        assertNull("No winner while game is active", game.getWinner());
    }

    @Test
    public void testGetWinner_WithWinner() {
        // Eliminate player1
        player1.setLives(0);
        player1.setEliminated(true);

        // End the game
        game.endGame();

        assertEquals("Winner should be player2", player2, game.getWinner());
    }

    @Test
    public void testGetWinner_NoWinnerWhenAllEliminated() {
        // Eliminate both players
        player1.setLives(0);
        player1.setEliminated(true);
        player2.setLives(0);
        player2.setEliminated(true);

        // End the game
        game.endGame();

        assertNull("No winner when all eliminated", game.getWinner());
    }

    @Test
    public void testEndGame() {
        game.endGame();

        assertTrue("Game should be over", game.isGameOver());
        assertEquals("Status should be gameEnd", "gameEnd", game.getStatus());
    }

    @Test
    public void testIsGameOver() {
        assertFalse("New game should not be over", game.isGameOver());

        game.setStatus("roundEnd");
        assertFalse("Game in roundEnd status should not be over", game.isGameOver());

        game.setStatus("gameEnd");
        assertTrue("Game in gameEnd status should be over", game.isGameOver());
    }
}