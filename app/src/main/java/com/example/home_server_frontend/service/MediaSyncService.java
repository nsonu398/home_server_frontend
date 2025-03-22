package com.example.home_server_frontend.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.repository.ImageRepository;
import com.example.home_server_frontend.ui.MainActivity;
import com.example.home_server_frontend.utils.ImageUtils;
import com.example.home_server_frontend.utils.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MediaSyncService extends Service {
    private static final String TAG = "MediaSyncService";
    private static final String CHANNEL_ID = "MediaSyncChannel";
    private static final int NOTIFICATION_ID = 1002;

    private ImageRepository imageRepository;
    private PreferenceManager preferenceManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();

        imageRepository = new ImageRepository(this);
        preferenceManager = new PreferenceManager(this);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Checking for new images...").build());

        disposables.add(imageRepository
                .getMostRecentImageTimestamp()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::syncNewImages, error -> {
                    Log.d(TAG, "onCreate: ");
                }));
    }

    private void syncNewImages(Long lastSyncTime) {
        Log.d(TAG, "Checking for images newer than: " + new Date(lastSyncTime));
        disposables.add(Single.fromCallable(() -> getNewImagesAfter(lastSyncTime)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(newImages -> {
            if (!newImages.isEmpty()) {
                updateNotification("Processing " + newImages.size() + " new images...");
                Log.d(TAG, "Found " + newImages.size() + " new images");

                // Add images to database
                addImagesListToDatabase(newImages, () -> {
                    // Update last sync time after successful processing
                    long currentTime = System.currentTimeMillis();
                    preferenceManager.setLastImageSyncTime(currentTime);

                    // Show completion notification and stop service
                    updateNotification("Added " + newImages.size() + " new images");
                    stopSelf();
                });
            } else {
                // No new images, stop service
                Log.d(TAG, "No new images found");
                stopSelf();
            }
        }, error -> {
            Log.e(TAG, "Error checking for new images", error);
            stopSelf();
        }));
    }

    private List<MainActivity.ImageData> getNewImagesAfter(long timestamp) {
        List<MainActivity.ImageData> newImages = new ArrayList<>();

        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_MODIFIED};

        // Only get images with DATE_MODIFIED newer than our timestamp
        String selection = MediaStore.Images.Media.DATE_MODIFIED + " > ?";
        String[] selectionArgs = {String.valueOf(timestamp / 1000)}; // Convert to seconds for query

        try (Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, MediaStore.Images.Media.DATE_MODIFIED + " ASC")) {
            if (cursor != null && cursor.moveToFirst()) {
                int pathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int dateModifiedColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);

                do {
                    String path = cursor.getString(pathColumnIndex);
                    String imageId = cursor.getString(idColumnIndex);
                    long updatedTime = cursor.getLong(dateModifiedColumnIndex) * 1000; // Convert to milliseconds

                    // Skip if file doesn't exist or isn't a valid image
                    if (!new File(path).exists() || !ImageUtils.isValidImageFile(path)) {
                        continue;
                    }

                    // Store image data
                    MainActivity.ImageData imageData = new MainActivity.ImageData(path, imageId, updatedTime);
                    newImages.add(imageData);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying new images", e);
        }

        return newImages;
    }

    private void addImagesListToDatabase(List<MainActivity.ImageData> imageList, Runnable onComplete) {
        List<ImageEntity> entities = new ArrayList<>();

        // Process each image
        for (MainActivity.ImageData imageData : imageList) {
            if (!ImageUtils.isValidImageFile(imageData.getPath())) {
                continue;
            }

            File file = new File(imageData.getPath());
            if (!file.exists()) {
                continue;
            }

            long fileSize = ImageUtils.getImageSize(imageData.getPath());
            String resolution = ImageUtils.getImageResolution(imageData.getPath());

            ImageEntity entity = new ImageEntity(imageData.getPath(), preferenceManager.isAutoUploadEnabled() ? "PENDING" : "LOCAL", fileSize, resolution, file.getName(), imageData.getId(), imageData.getUpdatedTime());
            entities.add(entity);
        }

        if (entities.isEmpty()) {
            onComplete.run();
            return;
        }

        // Insert all entities
        disposables.add(imageRepository.insertImages(entities).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(ids -> {
            Log.d(TAG, "Added " + ids.size() + " images to database");
            // If auto-upload is enabled, start the upload service
            if (preferenceManager.isAutoUploadEnabled()) {
                startUploadService();
            }
            onComplete.run();
        }, error -> {
            Log.e(TAG, "Error adding images to database", error);
            onComplete.run();
        }));
    }

    private void startUploadService() {
        Intent serviceIntent = new Intent(this, UploadService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // Notification methods
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Media Sync Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Syncs new images from device to app");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private NotificationCompat.Builder createNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Media Sync Service").setContentText(content).setSmallIcon(R.drawable.ic_launcher_foreground).setPriority(NotificationCompat.PRIORITY_LOW);
    }

    private void updateNotification(String content) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationCompat.Builder builder = createNotification(content);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Already started sync in onCreate, just return
        return START_NOT_STICKY; // Don't restart if killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }
}