package com.example.home_server_frontend.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages app preferences for user data and tokens
 */
public class PreferenceManager {
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Save authentication token
     */
    public void setAuthToken(String token) {
        sharedPreferences.edit().putString(Constants.PREF_AUTH_TOKEN, token).apply();
    }

    /**
     * Get authentication token
     */
    public String getAuthToken() {
        return sharedPreferences.getString(Constants.PREF_AUTH_TOKEN, null);
    }

    /**
     * Save username
     */
    public void setUsername(String username) {
        sharedPreferences.edit().putString(Constants.PREF_USERNAME, username).apply();
    }

    /**
     * Get username
     */
    public String getUsername() {
        return sharedPreferences.getString(Constants.PREF_USERNAME, null);
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return getAuthToken() != null;
    }

    /**
     * Clear all user data
     */
    public void clearUserData() {
        sharedPreferences.edit()
                .remove(Constants.PREF_AUTH_TOKEN)
                .apply();
    }
}