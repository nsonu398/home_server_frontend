package com.example.home_server_frontend.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "images")
public class ImageEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String localUrl;

    private String remoteUrl;

    @NonNull
    private String status; // "PENDING", "UPLOADING", "UPLOADED", "FAILED"

    private long size; // in bytes

    private String resolution; // e.g., "1920x1080"

    private long timestamp;

    private String fileName;

    // New fields
    private String imageId; // MediaStore image ID
    private long updatedTime; // Last modified time

    // Update constructor
    public ImageEntity(@NonNull String localUrl, @NonNull String status, long size,
                       String resolution, String fileName, String imageId, long updatedTime) {
        this.localUrl = localUrl;
        this.status = status;
        this.size = size;
        this.resolution = resolution;
        this.timestamp = System.currentTimeMillis();
        this.fileName = fileName;
        this.imageId = imageId;
        this.updatedTime = updatedTime;
    }

    // Add getters and setters
    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getLocalUrl() {
        return localUrl;
    }

    public void setLocalUrl(@NonNull String localUrl) {
        this.localUrl = localUrl;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}