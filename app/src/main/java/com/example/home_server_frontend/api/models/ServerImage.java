// Create a new file: app/src/main/java/com/example/home_server_frontend/api/models/ServerImage.java
package com.example.home_server_frontend.api.models;

import com.google.gson.annotations.SerializedName;

public class ServerImage {
    @SerializedName("id")
    private int id;

    @SerializedName("original_filename")
    private String originalFilename;

    @SerializedName("storage_filename")
    private String storageFilename;

    @SerializedName("path")
    private String path;

    @SerializedName("size")
    private long size;

    @SerializedName("resolution")
    private String resolution;

    @SerializedName("upload_date")
    private long uploadDate;

    @SerializedName("image_id")
    private String imageId;

    @SerializedName("updated_time")
    private long updatedTime;

    // Getters
    public int getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageFilename() { return storageFilename; }
    public String getPath() { return path; }
    public long getSize() { return size; }
    public String getResolution() { return resolution; }
    public long getUploadDate() { return uploadDate; }
    public String getImageId() { return imageId; }
    public long getUpdatedTime() { return updatedTime; }
}