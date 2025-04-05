package com.example.telepathy.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.telepathy.model.Player;
import com.example.telepathy.model.User;

public class PreferenceManager {
    private static final String PREF_NAME = "TelepathyPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_SCORE = "totalScore";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveUserData(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putInt(KEY_SCORE, user.getTotalScore());
        editor.apply();
    }

    public User getUserData() {
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String username = sharedPreferences.getString(KEY_USERNAME, null);
        int totalScore = sharedPreferences.getInt(KEY_SCORE, 0);

        if (userId != null && username != null){
            return new User(userId,username,totalScore);
        } else return null;
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void clearUserData() {
        editor.clear();
        editor.apply();
    }
}
