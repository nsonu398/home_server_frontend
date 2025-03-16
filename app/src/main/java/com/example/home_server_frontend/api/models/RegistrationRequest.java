package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class RegistrationRequest {
    @SerializedName("encryptedData")
    private String encryptedData;

    public RegistrationRequest(String encryptedData) {
        this.encryptedData = encryptedData;
    }
}
