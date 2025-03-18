package com.example.home_server_frontend.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.api.ApiClient;
import com.example.home_server_frontend.api.models.RegistrationResponse;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.repository.ImageRepository;
import com.example.home_server_frontend.utils.PreferenceManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UploadService extends Service {
    private static final String CHANNEL_ID = "UploadServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private ImageRepository imageRepository;
    private PreferenceManager preferenceManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isUploading = false;

    @Override
    public void onCreate() {
        super.onCreate();

        imageRepository = new ImageRepository(this);
        preferenceManager = new PreferenceManager(this);

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
        disposables.add(
                imageRepository.getPendingUploads()
                        .firstElement()
                        .subscribe(this::processPendingUploads)
        );
    }

    private void processPendingUploads(List<ImageEntity> pendingImages) {
        if (pendingImages.isEmpty()) {
            return;
        }

        isUploading = true;
        updateNotification("Uploading Images", "Processing " + pendingImages.size() + " images");

        // For demonstration purposes, we're just updating the status without actual upload
        // In a real app, you would implement the actual upload logic here
        for (ImageEntity image : pendingImages) {
            // Update status to UPLOADING
            disposables.add(
                    imageRepository.updateImageStatus(image.getId(), "UPLOADING")
                            .andThen(
                                    // Simulate upload delay
                                    Observable.timer(2, TimeUnit.SECONDS)
                                            .ignoreElements()
                            )
                            .andThen(
                                    // Set as UPLOADED with a fake remote URL
                                    imageRepository.setImageUploaded(
                                            image.getId(),
                                            "https://server.example.com/images/" + new File(image.getLocalUrl()).getName()
                                    )
                            )
                            .subscribe(
                                    () -> updateNotification("Upload Complete", "Uploaded " + image.getFileName()),
                                    error -> {
                                        // Set status to FAILED if there's an error
                                        imageRepository.updateImageStatus(image.getId(), "FAILED")
                                                .subscribe();
                                        updateNotification("Upload Failed", "Failed to upload " + image.getFileName());
                                    }
                            )
            );
        }

        isUploading = false;
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