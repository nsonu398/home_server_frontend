package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("encryptedData")
    private String encryptedData;

    @SerializedName("publicKey")
    private String publicKey;

    public LoginRequest(String encryptedData, String publicKey) {
        this.encryptedData = encryptedData;
        this.publicKey = publicKey;
    }
}