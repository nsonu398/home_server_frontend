package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for image upload
 */
public class ImageUploadResponse {
    @SerializedName("encryptedResponse")
    private HybridEncryptionPackage encryptedResponse;

    public HybridEncryptionPackage getEncryptedResponse() {
        return encryptedResponse;
    }
}