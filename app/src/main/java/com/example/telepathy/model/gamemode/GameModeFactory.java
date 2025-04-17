package com.example.telepathy.model.gamemode;

public class GameModeFactory {
    public static GameMode createGameMode(String mode) {
        if (mode == null) {
            return new ClassicMode(); // Default to classic mode
        }

        switch (mode.toLowerCase()) {
            case "matching":
            case "matching mode":
                return new MatchingMode();
            case "classic":
            case "classic mode":
            default:
                return new ClassicMode();
        }
    }
}