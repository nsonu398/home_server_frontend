package com.example.home_server_frontend.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.api.ApiClient;
import com.example.home_server_frontend.api.ApiService;
import com.example.home_server_frontend.api.models.HybridEncryptionPackage;
import com.example.home_server_frontend.api.models.ImageUploadRequest;
import com.example.home_server_frontend.api.models.ImageUploadResponse;
import com.example.home_server_frontend.crypto.CryptoUtils;
import com.example.home_server_frontend.crypto.KeyManager;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.repository.ImageRepository;
import com.example.home_server_frontend.utils.PreferenceManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadService extends Service {
    private static final String TAG = "UploadService";
    private static final String CHANNEL_ID = "UploadServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private ImageRepository imageRepository;
    private PreferenceManager preferenceManager;
    private KeyManager keyManager;
    private ApiService apiService;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isUploading = false;

    @Override
    public void onCreate() {
        super.onCreate();

        imageRepository = new ImageRepository(this);
        preferenceManager = new PreferenceManager(this);
        keyManager = new KeyManager(this);

        // Initialize API service
        apiService = ApiClient.getApiService(preferenceManager.getBaseUrl());

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Image Upload Service", "Ready to upload images").build());

        // Set up periodic check for pending uploads
        setupUploadChecker();
    }

    private void setupUploadChecker() {
        disposables.add(
                Observable.interval(0, 30, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .subscribe(tick -> {
                            if (!isUploading) {
                                checkForPendingUploads();
                            }
                        })
        );
    }

    private void checkForPendingUploads() {
        if (!preferenceManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, skipping upload check");
            return;
        }

        disposables.add(
                imageRepository.getPendingUploads()
                        .firstElement()
                        .subscribe(this::processPendingUploads, throwable -> {
                            Log.e(TAG, "Error checking for pending uploads", throwable);
                        })
        );
    }

    private void processPendingUploads(List<ImageEntity> pendingImages) {
        if (pendingImages.isEmpty()) {
            return;
        }

        isUploading = true;
        updateNotification("Uploading Images", "Processing " + pendingImages.size() + " images");

        // Process images one by one
        for (ImageEntity image : pendingImages) {
            uploadImage(image);
        }

        isUploading = false;
    }

    private void uploadImage(ImageEntity image) {
        // Update status to UPLOADING
        disposables.add(
                imageRepository.updateImageStatus(image.getId(), "UPLOADING")
                        .subscribe(() -> {
                            // Notify user about upload start
                            updateNotification("Uploading", "Uploading " + image.getFileName());

                            // Proceed with actual upload
                            performImageUpload(image);
                        }, throwable -> {
                            Log.e(TAG, "Error updating image status", throwable);
                            isUploading = false;
                        })
        );
    }

    private void performImageUpload(ImageEntity image) {
        try {
            // Get authentication token
            String authToken = preferenceManager.getAuthToken();
            if (authToken == null) {
                Log.e(TAG, "No auth token available");
                handleUploadFailure(image, "Authentication error");
                return;
            }

            // Get server's public key
            String serverPublicKeyPem = keyManager.getServerPublicKey();
            if (serverPublicKeyPem == null) {
                Log.e(TAG, "Server public key not found");
                handleUploadFailure(image, "Encryption error");
                return;
            }

            PublicKey serverPublicKey = CryptoUtils.publicKeyFromPem(serverPublicKeyPem);

            // Prepare the file
            File imageFile = new File(image.getLocalUrl());
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: " + image.getLocalUrl());
                handleUploadFailure(image, "File not found");
                return;
            }

            // Create a JSON metadata object
            JSONObject metadata = new JSONObject();
            metadata.put("fileName", image.getFileName());
            metadata.put("size", image.getSize());
            metadata.put("resolution", image.getResolution());

            // Encrypt the metadata
            String encryptedMetadata = CryptoUtils.encryptWithPublicKey(serverPublicKey, metadata.toString());

            // Create multipart request
            MultipartBody.Part filePart = prepareFilePart("image", imageFile);
            RequestBody metadataPart = RequestBody.create(MediaType.parse("text/plain"), encryptedMetadata);
            RequestBody authPart = RequestBody.create(MediaType.parse("text/plain"), "Bearer " + authToken);

            // Execute the upload
            Call<ImageUploadResponse> call = apiService.uploadImage(authPart, metadataPart, filePart);

            call.enqueue(new Callback<ImageUploadResponse>() {
                @Override
                public void onResponse(Call<ImageUploadResponse> call, Response<ImageUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        handleUploadSuccess(image, response.body());
                    } else {
                        String errorMessage = "Server error: " + (response.code() == 401 ? "Unauthorized" :
                                response.code() == 404 ? "Not found" :
                                        "Error code " + response.code());
                        Log.e(TAG, errorMessage);
                        handleUploadFailure(image, errorMessage);
                    }
                }

                @Override
                public void onFailure(Call<ImageUploadResponse> call, Throwable t) {
                    Log.e(TAG, "Upload failed", t);
                    handleUploadFailure(image, "Network error: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing upload", e);
            handleUploadFailure(image, "Upload error: " + e.getMessage());
        }
    }

    private MultipartBody.Part prepareFilePart(String partName, File file) {
        // Create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    private void handleUploadSuccess(ImageEntity image, ImageUploadResponse response) {
        try {
            // Decrypt the server's response
            String decryptedJson = CryptoUtils.decryptHybridPackage(
                    response.getEncryptedResponse(),
                    keyManager.getPrivateKey()
            );

            if (decryptedJson == null) {
                Log.e(TAG, "Failed to decrypt server response");
                handleUploadFailure(image, "Decryption error");
                return;
            }

            // Parse the response
            JSONObject jsonResponse = new JSONObject(decryptedJson);
            boolean success = jsonResponse.getBoolean("success");

            if (success) {
                // Extract the remote URL
                String remoteUrl = jsonResponse.getString("remoteUrl");

                // Update the database
                disposables.add(
                        imageRepository.setImageUploaded(image.getId(), remoteUrl)
                                .subscribe(() -> {
                                    updateNotification("Upload Complete", "Uploaded " + image.getFileName());
                                    Log.d(TAG, "Image uploaded successfully: " + image.getFileName());
                                }, throwable -> {
                                    Log.e(TAG, "Error updating database after successful upload", throwable);
                                })
                );
            } else {
                String errorMessage = jsonResponse.optString("message", "Upload failed on server");
                Log.e(TAG, errorMessage);
                handleUploadFailure(image, errorMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing upload response", e);
            handleUploadFailure(image, "Error processing response: " + e.getMessage());
        }
    }

    private void handleUploadFailure(ImageEntity image, String errorMessage) {
        // Update status to FAILED
        disposables.add(
                imageRepository.updateImageStatus(image.getId(), "FAILED")
                        .subscribe(() -> {
                            updateNotification("Upload Failed", "Failed to upload " + image.getFileName() + ": " + errorMessage);
                        }, throwable -> {
                            Log.e(TAG, "Error updating image status to FAILED", throwable);
                        })
        );
    }

    private void updateNotification(String title, String content) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(
                NOTIFICATION_ID,
                createNotification(title, content).build()
        );
    }

    private NotificationCompat.Builder createNotification(String title, String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Image Upload Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}