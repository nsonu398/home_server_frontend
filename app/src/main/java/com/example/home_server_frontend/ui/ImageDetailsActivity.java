package com.example.home_server_frontend.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.home_server_frontend.R;
import com.squareup.picasso.Picasso;

import java.io.File;

public class ImageDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "image_path";
    public static final String EXTRA_IMAGE_NAME = "image_name";

    private ImageView imageView;
    private TextView imageName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Image Details");
        }

        // Initialize views
        imageView = findViewById(R.id.image_view);
        imageName = findViewById(R.id.image_name);

        // Get intent data
        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        String nameText = getIntent().getStringExtra(EXTRA_IMAGE_NAME);

        if (imagePath != null) {
            // Load the image using Picasso
            Picasso.get()
                    .load("file://" + imagePath)
                    .into(imageView);

            // Set image name
            if (nameText != null) {
                imageName.setText(nameText);
            } else {
                // Extract file name from path if name not provided
                File file = new File(imagePath);
                imageName.setText(file.getName());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}