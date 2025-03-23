package com.example.home_server_frontend.ui;

import android.Manifest;
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
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private GridView gridView;
    private ImageAdapter imageAdapter;
    private ProgressBar progressBar;
    private PreferenceManager preferenceManager;
    private ImageRepository imageRepository;
    private List<ImageEntity> imageEntities = new ArrayList<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        gridView = findViewById(R.id.gridView);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize preference manager
        preferenceManager = new PreferenceManager(this);

        //initialize imageRepo
        imageRepository = new ImageRepository(this);

        imageAdapter = new ImageAdapter(this, imageEntities, bottomReached);
        gridView.setAdapter(imageAdapter);

        // Check and request permissions
        checkStoragePermission();

        //get all images from server
        fetchServerImages();

        //load local images that are present in the roomDB
        loadLocalImages();

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
        preferenceManager.serverImageFetchIsCompleted(new PreferenceManager.isCompleteListener() {
            @Override
            public void isComplete(Object object) {
                progressBar.setVisibility(View.VISIBLE);
                if (preferenceManager.isFirstInstall()) {
                    compositeDisposable.add(Single.fromCallable(()-> getAllImagePaths())
                            .subscribeOn(Schedulers.io()) // Runs on a background thread
                            .observeOn(AndroidSchedulers.mainThread()) // Switches to main thread for UI update
                            .subscribe(result -> {
                                if (preferenceManager.isFirstInstall()) {
                                    // Update UI on main thread
                                    addAllImagesToDatabase(result);
                                }
                            }, error -> {
                                Log.d(TAG, "error: ");
                            }));
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
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
                                    Log.d(TAG, "loadLocalImages: ");
                                }
                        )
        );
        progressBar.setVisibility(View.GONE);
    }

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();


    private void addAllImagesToDatabase(List<ImageData> imageList) {
        progressBar.setVisibility(View.VISIBLE);
        List<ImageEntity> list = new ArrayList<>();
        for (ImageData imageData : imageList) {
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
            list.add(imageEntity);
        }
        Log.d(TAG, "addAllImagesToDatabase: ");
        compositeDisposable.add(
                imageRepository
                        .insertImages(list)
                        .subscribe(id -> {
                                    Log.d(TAG, "addAllImagesToDatabase: " + id);
                                    if (!id.isEmpty()) {
                                        preferenceManager.firstTimeDone();
                                        setInitialLastSyncTime();
                                    }
                                    progressBar.setVisibility(View.GONE);
                                }, error -> {
                                    Log.d(TAG, "addAllImagesToDatabase: " + error);
                                    progressBar.setVisibility(View.GONE);
                                }
                        )
        );

    }

    private List<ImageData> getAllImagePaths() {
        List<ImageData> imageDataList = new ArrayList<>();

        // Update projection to include ID and last modified date
        String[] projection = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_MODIFIED
        };

        // Query external storage for images
        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_MODIFIED + " ASC"
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int pathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int dateModifiedColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);
                do {
                    String path = cursor.getString(pathColumnIndex);
                    String imageId = cursor.getString(idColumnIndex);
                    long updatedTime = cursor.getLong(dateModifiedColumnIndex) * 1000; // Convert to milliseconds

                    // Store as a tuple of path, id, and modified time
                    ImageData imageData = new ImageData(path, imageId, updatedTime);
                    imageDataList.add(imageData);
                } while (cursor.moveToNext());
            } else {
                Log.e(TAG, "No images found or cursor is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying images", e);
        }

        Log.d(TAG, "Found " + imageDataList.size() + " image paths");
        return imageDataList;
    }

    private final BottomReached bottomReached = new BottomReached() {
        @Override
        public void onBottomReached() {
            //loadLocalImages();
        }
    };

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
}