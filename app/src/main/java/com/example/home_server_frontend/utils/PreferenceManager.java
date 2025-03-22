package com.example.home_server_frontend.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


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
     * Save the timestamp of the last image sync
     * @param timestamp Timestamp in milliseconds
     */
    public void setLastImageSyncTime(long timestamp) {
        sharedPreferences.edit().putLong(Constants.PREF_LAST_IMAGE_SYNC_TIME, timestamp).apply();
    }

    /**
     * Get the timestamp of the last image sync
     * @return Timestamp in milliseconds, or 0 if no sync has been done
     */
    public long getLastImageSyncTime() {
        return sharedPreferences.getLong(Constants.PREF_LAST_IMAGE_SYNC_TIME, 0);
    }

    /**
     * Clear all user data
     */
    public void clearUserData() {
        sharedPreferences.edit()
                .remove(Constants.PREF_AUTH_TOKEN)
                .apply();
    }

    public boolean isAllServerImagesFetched() {
        return sharedPreferences.getBoolean(Constants.ARE_SERVER_IMAGES_FETCHED, false);
    }

    private Map<String, isCompleteListener> liveMap = new HashMap<>();

    public void serverImageFetchIsCompleted(isCompleteListener listener){
        liveMap.put(Constants.ARE_SERVER_IMAGES_FETCHED, listener);
        if(isAllServerImagesFetched()){
            Objects.requireNonNull(liveMap.get(Constants.ARE_SERVER_IMAGES_FETCHED)).isComplete(null);
        }
    }

    public void setAllServerImagesFetched(){
        sharedPreferences.edit().putBoolean(Constants.ARE_SERVER_IMAGES_FETCHED, true).apply();
        if(liveMap.containsKey(Constants.ARE_SERVER_IMAGES_FETCHED)){
            Objects.requireNonNull(liveMap.get(Constants.ARE_SERVER_IMAGES_FETCHED)).isComplete(null);
        }
    }

    public interface isCompleteListener{
        void isComplete(Object object);
    }
}