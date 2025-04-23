package com.example.home_server_frontend.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.repository.ImageRepository;
import com.example.home_server_frontend.service.MediaSyncService;
import com.example.home_server_frontend.service.UploadService;
import com.example.home_server_frontend.ui.adapters.ImageAdapter;
import com.example.home_server_frontend.utils.ImageUtils;
import com.example.home_server_frontend.utils.PreferenceManager;
import com.example.home_server_frontend.workers.MediaSyncWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int BATCH_SIZE = 10;

    private GridView gridView;
    private ImageAdapter imageAdapter;
    private ProgressBar progressBar;
    private PreferenceManager preferenceManager;
    private ImageRepository imageRepository;
    private List<ImageEntity> imageEntities = new ArrayList<>();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private boolean isLoadingBatch = false;
    private long lastLoadedTimestamp = Long.MAX_VALUE; // Start with max value to get the newest images first

    // Activity result launcher for storage permission handling
    private final ActivityResultLauncher<String> requestStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    syncDeviceImages();
                    checkNotificationPermission();
                    startMediaSync();
                } else {
                    handleStoragePermissionDenied();
                }
            });

    // Activity result launcher for notification permission handling
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startUploadService();
                } else {
                    // We can still start the service, but warn the user about limited functionality
                    Toast.makeText(this,
                            "Upload service will run with limited notifications",
                            Toast.LENGTH_LONG).show();
                    startUploadService();
                }
            });

    private final BottomReached bottomReached = new BottomReached() {
        @Override
        public void onBottomReached() {
            if (preferenceManager.isFirstInstall() && !isLoadingBatch) {
                loadImageBatch();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        gridView = findViewById(R.id.gridView);
        progressBar = findViewById(R.id.progress_bar);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize preference manager
        preferenceManager = new PreferenceManager(this);

        // Initialize imageRepo
        imageRepository = new ImageRepository(this);

        getlastLoadedTimestampValue();

        imageAdapter = new ImageAdapter(this, imageEntities, bottomReached);
        gridView.setAdapter(imageAdapter);

        // Get all images from server
        fetchServerImages();

        // Load local images that are present in the roomDB
        loadLocalImages();
    }

    @SuppressLint("CheckResult")
    private void getlastLoadedTimestampValue() {
        imageRepository.getLastImageEntity()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        oldestUpdatedTime -> {
                            // Success callback - use the value here
                            Log.d("ImageCheck", "Oldest updated time: " + oldestUpdatedTime);
                            // You can use the value in this block
                            // For example, assign to a field:
                            this.lastLoadedTimestamp = oldestUpdatedTime;
                            // Check and request permissions
                            checkStoragePermission();
                        },
                        throwable -> {
                            // Error callback
                            Log.e("ImageCheck", "Failed to get oldest updated time", throwable);
                        }
                );
    }

    private void fetchServerImages() {
        imageRepository.startSync();
        if (!preferenceManager.isAllServerImagesFetched()) {
            //imageRepository.startSync();
        }
    }

    //this is to start the process of fetching newly added images in a periodic way
    private void startMediaSync() {
        // Initialize media sync if auto-upload is enabled
        if (preferenceManager.isAutoUploadEnabled()) {
            // Schedule periodic sync as fallback mechanism
            MediaSyncWorker.schedulePeriodicSync(this);

            // Check if we need to perform initial sync
            if (preferenceManager.getLastImageSyncTime() == 0) {
                // We've never synced before, start an initial sync
                Intent mediaServiceIntent = new Intent(this, MediaSyncService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(mediaServiceIntent);
                } else {
                    startService(mediaServiceIntent);
                }
                Log.d(TAG, "Started initial media sync");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startUploadService() {
        Intent serviceIntent = new Intent(this, UploadService.class);

        // Starting service as foreground service for Android O and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Log.d(TAG, "Upload service started");
    }

    private void checkNotificationPermission() {
        // Only need to request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                // Permission already granted
                startUploadService();
            } else {
                // Request notification permission
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Notification permission not required for older versions
            startUploadService();
        }
    }

    private void checkStoragePermission() {
        // Determine the appropriate permission based on Android version
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            checkNotificationPermission();
            syncDeviceImages();
            startMediaSync();
        } else {
            // Request permission
            requestStoragePermissionLauncher.launch(permission);
        }
    }

    private void syncDeviceImages() {
        progressBar.setVisibility(View.VISIBLE);
        loadImageBatch();
    }

    private void loadImageBatch() {
        if (isLoadingBatch) return;
        isLoadingBatch = true;
        compositeDisposable.add(Single.fromCallable(() -> getImageBatch(lastLoadedTimestamp, BATCH_SIZE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(batch -> {
                    if (!batch.isEmpty()) {
                        addBatchToDatabase(batch);
                    } else {
                        // No more images to load
                        preferenceManager.firstTimeDone();
                        setInitialLastSyncTime();
                        progressBar.setVisibility(View.GONE);
                        isLoadingBatch = false;
                    }
                }, error -> {
                    Log.e(TAG, "Error loading image batch", error);
                    progressBar.setVisibility(View.GONE);
                    isLoadingBatch = false;
                }));
    }

    private List<ImageData> getImageBatch(long beforeTimestamp, int batchSize) {
        List<ImageData> batchImages = new ArrayList<>();

        String[] projection = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_MODIFIED
        };

        String selection = MediaStore.Images.Media.DATE_MODIFIED + " < ?";
        String[] selectionArgs = { String.valueOf(beforeTimestamp / 1000) }; // Convert to seconds for query
        String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC";

        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int pathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int dateModifiedColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);
                int count = 0;
                do {
                    String path = cursor.getString(pathColumnIndex);
                    String imageId = cursor.getString(idColumnIndex);
                    long updatedTime = cursor.getLong(dateModifiedColumnIndex) * 1000; // Convert to milliseconds

                    // Skip if file doesn't exist or isn't a valid image
                    if (!new File(path).exists() || !ImageUtils.isValidImageFile(path)) {
                        continue;
                    }

                    ImageData imageData = new ImageData(path, imageId, updatedTime);
                    batchImages.add(imageData);

                    // Update the lastLoadedTimestamp to the oldest timestamp in this batch
                    if (updatedTime < lastLoadedTimestamp) {
                        lastLoadedTimestamp = updatedTime;
                    }
                    count++;
                } while (cursor.moveToNext() && count < batchSize);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying image batch", e);
        }

        return batchImages;
    }

    private void addBatchToDatabase(List<ImageData> batch) {
        progressBar.setVisibility(View.VISIBLE);
        List<ImageEntity> entities = new ArrayList<>();

        for (ImageData imageData : batch) {
            if (!ImageUtils.isValidImageFile(imageData.path)) {
                continue;
            }

            File file = new File(imageData.path);
            long fileSize = ImageUtils.getImageSize(imageData.path);
            String resolution = ImageUtils.getImageResolution(imageData.path);

            ImageEntity imageEntity = new ImageEntity(
                    imageData.path,
                    "",  // Initial status
                    fileSize,
                    resolution,
                    file.getName(),
                    imageData.id,
                    imageData.updatedTime
            );
            entities.add(imageEntity);
        }

        if (!entities.isEmpty()) {
            compositeDisposable.add(
                    imageRepository.insertImages(entities)
                            .subscribe(ids -> {
                                Log.d(TAG, "Added batch of " + ids.size() + " images to database");
                                isLoadingBatch = false;
                                // Load next batch
                                loadImageBatch();
                            }, error -> {
                                Log.e(TAG, "Error adding batch to database", error);
                                isLoadingBatch = false;
                                progressBar.setVisibility(View.GONE);
                            })
            );
        } else {
            // No valid entities in this batch, try next batch
            isLoadingBatch = false;
            loadImageBatch();
        }
    }

    private void handleStoragePermissionDenied() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Storage permission is needed to display images. Go to app settings?")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null)
                    );
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this,
                            "Cannot display images without permission",
                            Toast.LENGTH_SHORT).show();
                    //finish();
                })
                .create()
                .show();
    }

    private void loadLocalImages() {
        compositeDisposable.add(
                imageRepository
                        .getAllImages()
                        .subscribe(images -> {
                            imageEntities.clear();
                            imageEntities.addAll(images);
                            imageAdapter.notifyDataSetChanged();
                        }, error -> {
                            Log.d(TAG, "loadLocalImages: " + error);
                        })
        );
    }

    // In MainActivity.java, after successful bulk sync
    private void setInitialLastSyncTime() {
        // Query for the most recent image timestamp in our database
        compositeDisposable.add(
                imageRepository.getMostRecentImageTimestamp()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                maxTimestamp -> {
                                    // If we found a timestamp, use it
                                    if (maxTimestamp > 0) {
                                        preferenceManager.setLastImageSyncTime(maxTimestamp);
                                        Log.d(TAG, "Set initial last sync time to: " + new Date(maxTimestamp));
                                    } else {
                                        // If no images in DB, just use current time
                                        long currentTime = System.currentTimeMillis();
                                        preferenceManager.setLastImageSyncTime(currentTime);
                                        Log.d(TAG, "No images found, setting last sync time to now");
                                    }
                                },
                                error -> {
                                    // On error, just use current time
                                    Log.e(TAG, "Error getting max timestamp", error);
                                    preferenceManager.setLastImageSyncTime(System.currentTimeMillis());
                                }
                        )
        );
    }

    public static class ImageData {
        public String path;
        public String id;
        public long updatedTime;

        public ImageData(String path, String id, long updatedTime) {
            this.path = path;
            this.id = id;
            this.updatedTime = updatedTime;
        }

        public long getUpdatedTime() {
            return updatedTime;
        }

        public String getId() {
            return id;
        }

        public String getPath() {
            return path;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}