package com.example.telepathy.utils;

public class Constants {
    // Firebase database paths
    public static final String USERS_PATH = "users";
    public static final String LOBBIES_PATH = "lobbies";
    public static final String GAMES_PATH = "games";

    // Game constants
    public static final int DEFAULT_TIME_LIMIT = 30; // seconds
    public static final int DEFAULT_MAX_PLAYERS = 8;
    public static final int DEFAULT_LIVES = 3;

    // Word categories
    public static final String CATEGORY_ANIMALS = "Animals";
    public static final String CATEGORY_COUNTRIES = "Countries";
    public static final String CATEGORY_FOODS = "Foods";
    public static final String CATEGORY_SPORTS = "Sports";

    // Game status
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ROUND_END = "roundEnd";
    public static final String STATUS_GAME_END = "gameEnd";
}
