package com.example.telepathy.utils;
import com.example.telepathy.model.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
        Log.d("Prefs", "Saving user: " + user.getUsername() + " with ID: " + user.getId());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putInt(KEY_SCORE, user.getTotalScore());
        editor.apply();
        Log.d("Prefs", "Saving user: " + user.getUsername() + " with ID: " + user.getId());
    }

    public User getUserData() {
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String username = sharedPreferences.getString(KEY_USERNAME, null);
        int totalScore = sharedPreferences.getInt(KEY_SCORE, 0);

        Log.d("Prefs", "Loaded userId=" + userId + ", username=" + username);

        if (userId != null && username != null) {
            // Create temporary User object, to be used in the application
            return new User(userId, username, totalScore);
        }
        return null;
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void clearUserData() {
        editor.clear();
        editor.apply();
    }
}
