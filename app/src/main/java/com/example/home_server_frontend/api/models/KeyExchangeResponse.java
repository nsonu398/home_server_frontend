package com.example.home_server_frontend.api.models;// KeyExchangeResponse.java

import com.google.gson.annotations.SerializedName;

public class KeyExchangeResponse {
    @SerializedName("publicKey")
    private String publicKey;

    public String getPublicKey() {
        return publicKey;
    }
}


