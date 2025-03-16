package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("encryptedData")
    private String encryptedData;

    public LoginRequest(String encryptedData) {
        this.encryptedData = encryptedData;
    }
}
