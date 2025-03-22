// Create a new file: app/src/main/java/com/example/home_server_frontend/api/models/ImageListResponse.java
package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class ImageListResponse {
    @SerializedName("encryptedResponse")
    private HybridEncryptionPackage encryptedResponse;

    public HybridEncryptionPackage getEncryptedResponse() {
        return encryptedResponse;
    }
}