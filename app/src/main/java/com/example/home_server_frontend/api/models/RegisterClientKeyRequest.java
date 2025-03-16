package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class RegisterClientKeyRequest {
    @SerializedName("username")
    private String username;

    @SerializedName("publicKey")
    private String publicKey;

    public RegisterClientKeyRequest(String username, String publicKey) {
        this.username = username;
        this.publicKey = publicKey;
    }
}
