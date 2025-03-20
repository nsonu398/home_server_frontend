package com.example.home_server_frontend.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.repository.ImageRepository;
import com.example.home_server_frontend.service.UploadService;
import com.example.home_server_frontend.utils.ImageUtils;
import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.Picasso;

import java.io.File;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ImageDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "image_path";
    public static final String EXTRA_IMAGE_NAME = "image_name";
    public static final String EXTRA_IMAGE_ID = "image_id";
    public static final String EXTRA_IMAGE_UPDATE_TIME = "image_update_time";

    private PhotoView imageView;
    private TextView imageName;
    private ImageRepository imageRepository;
    private String imagePath;
    private String imageFileName;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String imageID;
    private Long imageUpdateTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        // Set up ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Image Details");
        }

        // Initialize views
        imageView = findViewById(R.id.image_view);
        imageName = findViewById(R.id.image_name);

        // Initialize repository
        imageRepository = new ImageRepository(this);

        // Get intent data
        imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        imageFileName = getIntent().getStringExtra(EXTRA_IMAGE_NAME);
        imageID = getIntent().getStringExtra(EXTRA_IMAGE_ID);
        imageUpdateTime = getIntent().getLongExtra(EXTRA_IMAGE_UPDATE_TIME, 0L);

        if (imagePath != null) {
            // Load the image using Picasso
            Picasso.get()
                    .load("file://" + imagePath)
                    .into(imageView);

            // Set image name
            if (imageFileName != null) {
                imageName.setText(imageFileName);
            } else {
                // Extract file name from path if name not provided
                File file = new File(imagePath);
                imageFileName = file.getName();
                imageName.setText(imageFileName);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_upload) {
            prepareImageForUpload();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void prepareImageForUpload() {
        if (imagePath == null) {
            Toast.makeText(this, "No image to upload", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get image metadata
        long fileSize = ImageUtils.getImageSize(imagePath);
        String resolution = ImageUtils.getImageResolution(imagePath);

        // Create a new image entity for the database
        ImageEntity imageEntity = new ImageEntity(
                imagePath,
                "PENDING",  // Initial status
                fileSize,
                resolution,
                imageFileName,
                imageID,
                imageUpdateTime
        );

        // Save to database
        disposables.add(
                imageRepository.getImageByLocalUrl(imagePath)
                        .subscribe(
                                existingImage -> {
                                    // Image already exists in database, show message
                                    Toast.makeText(this, "Image already queued for upload", Toast.LENGTH_SHORT).show();
                                },
                                error -> {
                                    // Image doesn't exist, insert it
                                    addImageToUploadQueue(imageEntity);
                                }
                        )
        );
    }

    private void addImageToUploadQueue(ImageEntity imageEntity) {
        disposables.add(
                imageRepository.insertImage(imageEntity)
                        .subscribe(
                                id -> {
                                    Toast.makeText(this, "Image added to upload queue", Toast.LENGTH_SHORT).show();
                                    // Here you could trigger the actual upload service if desired
                                    //startUploadService();
                                },
                                error -> {
                                    Toast.makeText(this, "Failed to add image to upload queue", Toast.LENGTH_SHORT).show();
                                    error.printStackTrace();
                                }
                        )
        );
    }

    private void startUploadService() {
        Intent serviceIntent = new Intent(this, UploadService.class);

        // Starting service as foreground service for Android O and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}