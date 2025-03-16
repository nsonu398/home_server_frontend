package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class RegisterClientKeyResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
