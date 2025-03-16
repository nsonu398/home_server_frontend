package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class RegistrationResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("encryptedVerificationCode")
    private String encryptedVerificationCode;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getEncryptedVerificationCode() {
        return encryptedVerificationCode;
    }
}
