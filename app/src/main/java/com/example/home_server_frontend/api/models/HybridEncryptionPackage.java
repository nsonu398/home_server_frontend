package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

// Add this to handle hybrid encryption packages
public class HybridEncryptionPackage {
    @SerializedName("encryptedKey")
    private String encryptedKey;

    @SerializedName("iv")
    private String iv;

    @SerializedName("encryptedData")
    private String encryptedData;

    @SerializedName("authTag")
    private String authTag;

    // Getters and setters
    public String getEncryptedKey() { return encryptedKey; }
    public String getIv() { return iv; }
    public String getEncryptedData() { return encryptedData; }
    public String getAuthTag() { return authTag; }
}