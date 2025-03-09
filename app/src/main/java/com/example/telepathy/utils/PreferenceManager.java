package com.example.telepathy.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.telepathy.model.Player;

public class PreferenceManager {
    private static final String PREF_NAME = "TelepathyPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_SCORE = "score";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveUserData(Player player) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, player.getId());
        editor.putString(KEY_USERNAME, player.getUsername());
        editor.putInt(KEY_SCORE, player.getScore());
        editor.apply();
    }

    public Player getUserData() {
        String userId = sharedPreferences.getString(KEY_USER_ID, "");
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        int score = sharedPreferences.getInt(KEY_SCORE, 0);

        Player player = new Player(username);
        player.setId(userId);
        player.setScore(score);
        return player;
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void clearUserData() {
        editor.clear();
        editor.apply();
    }
}
