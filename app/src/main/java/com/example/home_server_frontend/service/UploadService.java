package com.example.home_server_frontend.service;

import android.annotation.SuppressLint;
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
import com.example.home_server_frontend.api.models.ImageUploadResponse;
import com.example.home_server_frontend.crypto.CryptoUtils;
import com.example.home_server_frontend.crypto.KeyManager;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.repository.ImageRepository;
import com.example.home_server_frontend.utils.PreferenceManager;

import org.json.JSONObject;

import java.io.File;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
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

        // Setup the upload observer
        if (preferenceManager.isLoggedIn()) {
            setupUploadObserver();
        }
    }

    private void setupUploadObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            disposables.add(
                    // Get oldest pending upload continuously
                    imageRepository.getOldestPendingUpload()
                            .subscribeOn(Schedulers.io())
                            .filter(Optional::isPresent) // Only proceed when we have an image
                            .map(Optional::get)
                            .flatMapCompletable(image ->
                                    // Process one image completely before moving to the next
                                    processImageUpload(image)
                            )
                            .repeat() // Repeat the sequence after completion
                            .subscribe(
                                    () -> Log.d(TAG, "Upload cycle completed, waiting for next image"),
                                    throwable -> {
                                        Log.e(TAG, "Error in upload observer", throwable);
                                        // Restart the observer after a short delay
                                        setupDelayedRestartOfObserver();
                                    }
                            )
            );
        }
    }

    private void setupDelayedRestartOfObserver() {
        disposables.add(
                Observable.timer(10, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .subscribe(tick -> {
                            if (preferenceManager.isLoggedIn()) {
                                setupUploadObserver();
                            }
                        })
        );
    }

    @SuppressLint("CheckResult")
    private Completable processImageUpload(ImageEntity image) {
        return Completable.create(emitter -> {
            try {
                // Update status to UPLOADING
                imageRepository.updateImageStatus(image.getId(), "UPLOADING")
                        .subscribe(
                                () -> {
                                    // Notify user about upload start
                                    updateNotification("Uploading", "Uploading " + image.getFileName());

                                    // Perform the actual upload
                                    performImageUpload(image, new UploadCallback() {
                                        @Override
                                        public void onSuccess() {
                                            emitter.onComplete();
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            emitter.onError(new Exception(error));
                                        }
                                    });
                                },
                                error -> emitter.onError(error)
                        );
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    // Interface for upload callbacks
    private interface UploadCallback {
        void onSuccess();
        void onFailure(String error);
    }

    private void performImageUpload(ImageEntity image, UploadCallback callback) {
        try {
            // Get authentication token
            String authToken = preferenceManager.getAuthToken();
            if (authToken == null) {
                Log.e(TAG, "No auth token available");
                handleUploadFailure(image, "Authentication error", callback);
                return;
            }

            // Get server's public key
            String serverPublicKeyPem = keyManager.getServerPublicKey();
            if (serverPublicKeyPem == null) {
                Log.e(TAG, "Server public key not found");
                handleUploadFailure(image, "Encryption error", callback);
                return;
            }

            PublicKey serverPublicKey = CryptoUtils.publicKeyFromPem(serverPublicKeyPem);

            // Prepare the file
            File imageFile = new File(image.getLocalUrl());
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: " + image.getLocalUrl());
                handleUploadFailure(image, "File not found", callback);
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
                        handleUploadSuccess(image, response.body(), callback);
                    } else {
                        String errorMessage = "Server error: " + (response.code() == 401 ? "Unauthorized" :
                                response.code() == 404 ? "Not found" :
                                        "Error code " + response.code());
                        Log.e(TAG, errorMessage);
                        handleUploadFailure(image, errorMessage, callback);
                    }
                }

                @Override
                public void onFailure(Call<ImageUploadResponse> call, Throwable t) {
                    Log.e(TAG, "Upload failed", t);
                    handleUploadFailure(image, "Network error: " + t.getMessage(), callback);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing upload", e);
            handleUploadFailure(image, "Upload error: " + e.getMessage(), callback);
        }
    }

    private MultipartBody.Part prepareFilePart(String partName, File file) {
        // Create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    @SuppressLint("CheckResult")
    private void handleUploadSuccess(ImageEntity image, ImageUploadResponse response, UploadCallback callback) {
        try {
            // Decrypt the server's response
            String decryptedJson = CryptoUtils.decryptHybridPackage(
                    response.getEncryptedResponse(),
                    keyManager.getPrivateKey()
            );

            if (decryptedJson == null) {
                Log.e(TAG, "Failed to decrypt server response");
                handleUploadFailure(image, "Decryption error", callback);
                return;
            }

            // Parse the response
            JSONObject jsonResponse = new JSONObject(decryptedJson);
            boolean success = jsonResponse.getBoolean("success");

            if (success) {
                // Extract the remote URL
                String remoteUrl = jsonResponse.getString("remoteUrl");

                // Update the database
                imageRepository.setImageUploaded(image.getId(), remoteUrl)
                        .subscribe(() -> {
                            updateNotification("Upload Complete", "Uploaded " + image.getFileName());
                            Log.d(TAG, "Image uploaded successfully: " + image.getFileName());
                            callback.onSuccess();
                        }, throwable -> {
                            Log.e(TAG, "Error updating database after successful upload", throwable);
                            callback.onFailure("Database error after upload: " + throwable.getMessage());
                        });
            } else {
                String errorMessage = jsonResponse.optString("message", "Upload failed on server");
                Log.e(TAG, errorMessage);
                handleUploadFailure(image, errorMessage, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing upload response", e);
            handleUploadFailure(image, "Error processing response: " + e.getMessage(), callback);
        }
    }

    @SuppressLint("CheckResult")
    private void handleUploadFailure(ImageEntity image, String errorMessage, UploadCallback callback) {
        // Update status to FAILED
        imageRepository.updateImageStatus(image.getId(), "FAILED")
                .subscribe(() -> {
                    updateNotification("Upload Failed", "Failed to upload " + image.getFileName() + ": " + errorMessage);
                    callback.onFailure(errorMessage);
                }, throwable -> {
                    Log.e(TAG, "Error updating image status to FAILED", throwable);
                    callback.onFailure("Error updating image status: " + throwable.getMessage());
                });
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
        // Check if user is logged in and setup observer if necessary
        if (preferenceManager.isLoggedIn() && disposables.size() == 0) {
            setupUploadObserver();
        }
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