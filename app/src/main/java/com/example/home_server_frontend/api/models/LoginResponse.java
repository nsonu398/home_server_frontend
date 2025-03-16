package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("encryptedResponse")
    private String encryptedResponse;

    public String getEncryptedResponse() {
        return encryptedResponse;
    }
}
