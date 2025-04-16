package com.example.telepathy.model.gamemode;

import com.example.telepathy.model.GameConfig;
import com.example.telepathy.model.Player;
import java.util.List;

public interface GameMode {
    // Game State
    void initializeGame(String gameId, GameConfig config);

    void startRound();

    void endRound();

    boolean isRoundComplete();

    boolean isGameOver();

    // Player Management
    boolean addPlayer(Player player);

    boolean removePlayer(String playerId);

    List<Player> getActivePlayers();

    int getMaxPlayers();

    // Word Processing
    boolean submitWord(String playerId, String word);

    boolean validateWord(String word);

    void processRoundResults();

    // Game Information
    int getCurrentRound();

    long getRoundTimeLimit();

    String getGameStatus();

    Player getWinner();

    // Configuration
    GameConfig getGameConfig();

    String getGameModeType(); // To identify different game modes
}
