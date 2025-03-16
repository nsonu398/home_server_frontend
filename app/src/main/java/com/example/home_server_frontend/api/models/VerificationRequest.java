package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class VerificationRequest {
    @SerializedName("encryptedData")
    private String encryptedData;

    public VerificationRequest(String encryptedData) {
        this.encryptedData = encryptedData;
    }
}
