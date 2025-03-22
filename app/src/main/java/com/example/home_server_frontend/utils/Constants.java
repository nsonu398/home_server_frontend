package com.example.home_server_frontend.utils;

public class Constants {
    // Existing constants
    public static final String DEFAULT_HOST = "10.0.2.2";
    public static final String DEFAULT_PORT = "3000";

    // Shared Preferences keys
    public static final String PREF_NAME = "home_server_prefs";
    public static final String PREF_AUTH_TOKEN = "auth_token";
    public static final String PREF_USERNAME = "username";
    public static final String PREF_SERVER_HOST = "server_host";
    public static final String PREF_SERVER_PORT = "server_port";
    public static final String PREF_AUTO_UPLOAD_ENABLED = "auto_upload_enabled";
    public static final String IS_FIRST_INSALL = "is_first_install";

    // New preference for last image sync time
    public static final String PREF_LAST_IMAGE_SYNC_TIME = "last_image_sync_time";

    // Request codes
    public static final int REQUEST_CODE_REGISTER = 100;
    public static final String ARE_SERVER_IMAGES_FETCHED = "are_server_images_fetched";

    // Base API URL method
    public static String getBaseUrl(String host, String port) {
        return "http://" + host + ":" + port + "/";
    }
}