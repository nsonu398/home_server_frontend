package com.example.home_server_frontend.ui;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
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
import com.example.home_server_frontend.ui.adapters.ImageAdapter;
import com.example.home_server_frontend.utils.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PAGE_SIZE = 50; // Number of images to load per page
    private static final String TAG = "MainActivity";

    private GridView gridView;
    private ImageAdapter imageAdapter;
    private ProgressBar progressBar;
    private ProgressBar bottomProgressBar;
    private PreferenceManager preferenceManager;

    // Image loading variables
    private List<Bitmap> deviceImages = new ArrayList<>();
    private boolean isLoading = false;
    private int currentPage = 0;
    private boolean hasMoreImages = true;
    private long lastImageId = Long.MAX_VALUE; // To track pagination

    // Permissions
    private String currentPermission;

    // Activity result launcher for permission handling
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your app.
                    loadInitialDeviceImages();
                } else {
                    // Explain to the user that the feature is unavailable
                    handlePermissionDenied();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        gridView = findViewById(R.id.gridView);
        progressBar = findViewById(R.id.progress_bar);
        bottomProgressBar = findViewById(R.id.bottom_progress_bar);

        // Initialize preference manager
        preferenceManager = new PreferenceManager(this);

        // Setup GridView with scroll listener
        setupGridViewScrollListener();

        // Check and request permissions
        checkStoragePermission();
    }

    private void setupGridViewScrollListener() {
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                // Check if we need to load more images
                if (!isLoading && hasMoreImages &&
                        (firstVisibleItem + visibleItemCount >= totalItemCount)) {
                    loadMoreDeviceImages();
                }
            }
        });
    }

    private void checkStoragePermission() {
        // Determine the appropriate permission based on Android version
        currentPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(this, currentPermission)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            loadInitialDeviceImages();
        } else {
            // Request permission
            requestStoragePermission();
        }
    }

    private void requestStoragePermission() {
        // Provide rationale before requesting permission
        if (shouldShowRequestPermissionRationale(currentPermission)) {
            // Show an explanation to the user
            new AlertDialog.Builder(this)
                    .setTitle("Storage Access Needed")
                    .setMessage("This app needs access to your device storage to display images.")
                    .setPositiveButton("OK", (dialog, which) ->
                            requestPermissionLauncher.launch(currentPermission))
                    .setNegativeButton("Cancel", (dialog, which) ->
                            handlePermissionDenied())
                    .create()
                    .show();
        } else {
            // Directly request permission
            requestPermissionLauncher.launch(currentPermission);
        }
    }

    private void handlePermissionDenied() {
        // Show dialog to guide user to app settings
        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("Storage permission is required to display images. Would you like to go to app settings to grant permission?")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null)
                    );
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Optionally finish the activity or show a message
                    Toast.makeText(this,
                            "Cannot display images without storage permission",
                            Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity if no permission
                })
                .create()
                .show();
    }

    private void loadInitialDeviceImages() {
        // Reset pagination variables
        currentPage = 0;
        deviceImages.clear();
        hasMoreImages = true;
        lastImageId = Long.MAX_VALUE;

        // Show initial progress
        progressBar.setVisibility(View.VISIBLE);

        // Run image loading in a background thread
        new Thread(() -> {
            List<Bitmap> initialImages = fetchImagesFromDevice();

            // Update UI on main thread
            runOnUiThread(() -> {
                // Hide progress
                progressBar.setVisibility(View.GONE);

                // If no images found
                if (initialImages.isEmpty()) {
                    hasMoreImages = false;
                    Toast.makeText(this, "No images found", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "No images found in initial load");
                    return;
                }

                Log.d(TAG, "Found " + initialImages.size() + " images in initial load");

                // Add initial images
                deviceImages.addAll(initialImages);

                // Setup GridView
                imageAdapter = new ImageAdapter(this, deviceImages);
                gridView.setAdapter(imageAdapter);

                // Set item click listener
                gridView.setOnItemClickListener((parent, view, position, id) -> {
                    // Handle image click if needed
                    Toast.makeText(this,
                            "Image " + (position + 1) + " clicked",
                            Toast.LENGTH_SHORT).show();
                });

                // Increment page
                currentPage++;
            });
        }).start();
    }

    private void loadMoreDeviceImages() {
        // Check if already loading or no more images
        if (isLoading || !hasMoreImages) return;

        // Set loading flag
        isLoading = true;

        // Show bottom progress
        bottomProgressBar.setVisibility(View.VISIBLE);

        // Run image loading in a background thread
        new Thread(() -> {
            List<Bitmap> moreImages = fetchImagesFromDevice();

            // Update UI on main thread
            runOnUiThread(() -> {
                // Hide bottom progress
                bottomProgressBar.setVisibility(View.GONE);

                // If no more images found
                if (moreImages.isEmpty()) {
                    hasMoreImages = false;
                    isLoading = false;
                    Toast.makeText(this, "No more images", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "No more images found");
                    return;
                }

                Log.d(TAG, "Found " + moreImages.size() + " more images");

                // Add more images
                deviceImages.addAll(moreImages);

                // Notify adapter of data change
                imageAdapter.updateImages(deviceImages);

                // Increment page
                currentPage++;

                // Reset loading flag
                isLoading = false;
            });
        }).start();
    }

    private List<Bitmap> fetchImagesFromDevice() {
        List<Bitmap> imageList = new ArrayList<>();

        // Projection for image query
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA
        };

        try {
            // Query external storage for images
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_MODIFIED + " DESC"
            );

            if (cursor != null) {
                int columnIndexId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                Log.d(TAG, "Total cursor count: " + cursor.getCount());

                int count = 0;
                while (cursor.moveToNext() && count < PAGE_SIZE) {
                    // Get image ID and path
                    long imageId = cursor.getLong(columnIndexId);
                    String imagePath = cursor.getString(columnIndexData);

                    // Log image details
                    Log.d(TAG, "Processing image: " + imagePath);

                    // Check if file exists
                    File imageFile = new File(imagePath);
                    if (!imageFile.exists()) {
                        Log.w(TAG, "Image file does not exist: " + imagePath);
                        continue;
                    }

                    // Decode image with optimal bitmap size
                    Bitmap bitmap = decodeSampledBitmapFromFile(imagePath, 200, 200);

                    if (bitmap != null) {
                        imageList.add(bitmap);
                        count++;
                    } else {
                        Log.w(TAG, "Failed to decode bitmap: " + imagePath);
                    }
                }
                cursor.close();

                Log.d(TAG, "Loaded " + count + " images");
            } else {
                Log.e(TAG, "Cursor is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching images", e);
        }

        return imageList;
    }

    // Bitmap decoding methods remain the same
    public static Bitmap decodeSampledBitmapFromFile(
            String filepath, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filepath, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}