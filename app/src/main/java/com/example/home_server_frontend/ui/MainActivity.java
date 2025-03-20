package com.example.home_server_frontend.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.Observable;
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
import com.example.home_server_frontend.service.UploadService;
import com.example.home_server_frontend.ui.adapters.ImageAdapter;
import com.example.home_server_frontend.utils.ImageUtils;
import com.example.home_server_frontend.utils.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
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

    // Activity result launcher for storage permission handling
    private final ActivityResultLauncher<String> requestStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    loadDeviceImages();
                    loadAllDeviceImages();
                    checkNotificationPermission();
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

    private int index = 1;
    private int PAGE_SIZE = 50;

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

        // Check and request permissions
        checkStoragePermission();
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
            loadDeviceImages();
            checkNotificationPermission();
            loadAllDeviceImages();
        } else {
            // Request permission
            requestStoragePermissionLauncher.launch(permission);
        }
    }

    private void loadAllDeviceImages() {
        if (preferenceManager.isAutoUploadEnabled() && preferenceManager.isFirstInstall()){
            compositeDisposable.add(disposable);
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
                    finish();
                })
                .create()
                .show();
    }

    private void loadDeviceImages() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Run image loading in a background thread
        new Thread(() -> {
            List<String> imagePaths = getImagePaths();

            // Update UI on main thread
            runOnUiThread(() -> {
                // Hide progress
                progressBar.setVisibility(View.GONE);

                // Handle images
                if (imagePaths.isEmpty()) {
                    Toast.makeText(this, "No images found", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "No images found on device");
                    return;
                }

                Log.d(TAG, "Loaded " + imagePaths.size() + " images");

                // Setup GridView
                imageAdapter = new ImageAdapter(this, imagePaths, bottomReached);
                gridView.setAdapter(imageAdapter);

                // Set item click listener
                gridView.setOnItemClickListener((parent, view, position, id) -> {
                    String imagePath = imagePaths.get(position);

                    // Create intent to open image details
                    Intent intent = new Intent(MainActivity.this, ImageDetailsActivity.class);
                    intent.putExtra(ImageDetailsActivity.EXTRA_IMAGE_PATH, imagePath);

                    // Extract file name from path
                    File file = new File(imagePath);
                    intent.putExtra(ImageDetailsActivity.EXTRA_IMAGE_NAME, file.getName());

                    startActivity(intent);
                });
            });
        }).start();
    }

    private List<String> getImagePaths() {
        List<String> imagePaths = new ArrayList<>();

        // Projection for image query
        String[] projection = {
                MediaStore.Images.Media.DATA
        };

        // Query external storage for images
        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_MODIFIED + " DESC"
        )) {
            if (cursor != null && cursor.move(index)) {
                int pathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                do {
                    String path = cursor.getString(pathColumnIndex);
                    imagePaths.add(path);
                    index++;

                    // Limit to 50 images to prevent memory issues
                    if (index >= PAGE_SIZE) {
                        PAGE_SIZE += 50;
                        break;
                    }
                } while (cursor.moveToNext());
            } else {
                Log.e(TAG, "No images found or cursor is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying images", e);
        }

        Log.d(TAG, "Found " + imagePaths.size() + " image paths");
        return imagePaths;
    }

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // Background task (e.g., uploading an image)
    Disposable disposable = Single.fromCallable(this::getAllImagePaths)
            .subscribeOn(Schedulers.io()) // Runs on a background thread
            .observeOn(AndroidSchedulers.mainThread()) // Switches to main thread for UI update
            .subscribe(result -> {
                if(preferenceManager.isFirstInstall()){
                    // Update UI on main thread
                    addAllImagesToDatabase(result);
                }
            }, error -> {
                Log.d(TAG, "error: ");
            });

    private void addAllImagesToDatabase(List<String> imageList) {
        List<ImageEntity> list = new ArrayList<>();
        for (String imagePath : imageList){
            if(!ImageUtils.isValidImageFile(imagePath)){
                continue;
            }
            File file = new File(imagePath);
            long fileSize = ImageUtils.getImageSize(imagePath);
            String resolution = ImageUtils.getImageResolution(imagePath);
            ImageEntity imageEntity = new ImageEntity(
                    imagePath,
                    "PENDING",  // Initial status
                    fileSize,
                    resolution,
                    file.getName()
            );
            list.add(imageEntity);
        }
        Log.d(TAG, "addAllImagesToDatabase: ");
        compositeDisposable.add(
                imageRepository
                        .insertImages(list)
                        .subscribe(id -> {
                            Log.d(TAG, "addAllImagesToDatabase: "+id);
                            if(!id.isEmpty()){
                                preferenceManager.firstTimeDone();
                            }
                        }, error->{
                            Log.d(TAG, "addAllImagesToDatabase: "+error);
                        }
                )
        );

    }


    private List<String> getAllImagePaths() {
        List<String> imagePaths = new ArrayList<>();

        // Projection for image query
        String[] projection = {
                MediaStore.Images.Media.DATA
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

                do {
                    String path = cursor.getString(pathColumnIndex);
                    imagePaths.add(path);
                } while (cursor.moveToNext());
            } else {
                Log.e(TAG, "No images found or cursor is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying images", e);
        }

        Log.d(TAG, "Found " + imagePaths.size() + " image paths");
        return imagePaths;
    }


    private final BottomReached bottomReached = new BottomReached() {
        @Override
        public void onBottomReached() {
            List<String> imagePaths = getImagePaths();
            imageAdapter.addImages(imagePaths);
        }
    };

    @Override
    protected void onDestroy() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        super.onDestroy();
    }
}