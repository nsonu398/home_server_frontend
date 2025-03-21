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

    public boolean isFirstInstall(){
        return sharedPreferences.getBoolean(Constants.IS_FIRST_INSALL, true);
    }

    public void firstTimeDone(){
        sharedPreferences.edit().putBoolean(Constants.IS_FIRST_INSALL, false).apply();
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
     * Save server host
     */
    public void setServerHost(String host) {
        sharedPreferences.edit().putString(Constants.PREF_SERVER_HOST, host).apply();
    }

    /**
     * Get server host
     */
    public String getServerHost() {
        return sharedPreferences.getString(Constants.PREF_SERVER_HOST, Constants.DEFAULT_HOST);
    }

    /**
     * Save server port
     */
    public void setServerPort(String port) {
        sharedPreferences.edit().putString(Constants.PREF_SERVER_PORT, port).apply();
    }

    /**
     * Get server port
     */
    public String getServerPort() {
        return sharedPreferences.getString(Constants.PREF_SERVER_PORT, Constants.DEFAULT_PORT);
    }

    /**
     * Get the base URL for API calls
     */
    public String getBaseUrl() {
        return Constants.getBaseUrl(getServerHost(), getServerPort());
    }

    /**
     * Check if automatic upload is enabled
     * @return true if automatic upload is enabled, false otherwise
     */
    public boolean isAutoUploadEnabled() {
        // Default to false if not set
        return sharedPreferences.getBoolean(Constants.PREF_AUTO_UPLOAD_ENABLED, false);
    }

    /**
     * Set automatic upload preference
     * @param enabled whether automatic upload should be enabled
     */
    public void setAutoUploadEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(Constants.PREF_AUTO_UPLOAD_ENABLED, enabled).apply();
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