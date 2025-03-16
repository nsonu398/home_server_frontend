package com.example.home_server_frontend.api.models;

public class DecryptedLoginResponse {
    private boolean success;
    private String token;
    private String message;

    public DecryptedLoginResponse(boolean success, String token, String message) {
        this.success = success;
        this.token = token;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getToken() {
        return token;
    }

    public String getMessage() {
        return message;
    }
}
