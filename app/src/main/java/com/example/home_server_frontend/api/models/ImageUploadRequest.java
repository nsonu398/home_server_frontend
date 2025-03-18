package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for image upload
 */
public class ImageUploadRequest {
    @SerializedName("metadata")
    private String encryptedMetadata;

    public ImageUploadRequest(String encryptedMetadata) {
        this.encryptedMetadata = encryptedMetadata;
    }

    public String getEncryptedMetadata() {
        return encryptedMetadata;
    }

    public void setEncryptedMetadata(String encryptedMetadata) {
        this.encryptedMetadata = encryptedMetadata;
    }
}