package com.example.home_server_frontend.utils;

import android.util.Log;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Utility class for security-related functions
 */
public class SecurityUtils {
    private static final String TAG = "SecurityUtils";

    /**
     * Generate a random string for nonce or salt purposes
     * @param length Length of the random string
     * @return Random string
     */
    public static String generateRandomString(int length) {
        final String ALLOWED_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(ALLOWED_CHARS.length());
            sb.append(ALLOWED_CHARS.charAt(randomIndex));
        }

        return sb.toString();
    }

    /**
     * Compute SHA-256 hash of a string
     * @param input Input string
     * @return Hex string of the hash
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error computing hash", e);
            return null;
        }
    }

    /**
     * Validate token format
     * @param token Token to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidToken(String token) {
        // Simple validation - not empty and at least 10 chars
        return token != null && token.length() >= 10;
    }
}