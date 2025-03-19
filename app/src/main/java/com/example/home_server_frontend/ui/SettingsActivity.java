package com.example.home_server_frontend.ui;

import android.os.Bundle;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.utils.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchAutoUpload;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);

        // Find the switch
        switchAutoUpload = findViewById(R.id.switch_auto_upload);

        // Load current setting
        boolean isAutoUploadEnabled = preferenceManager.isAutoUploadEnabled();
        switchAutoUpload.setChecked(isAutoUploadEnabled);

        // Set listener for switch changes
        switchAutoUpload.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the new setting
            preferenceManager.setAutoUploadEnabled(isChecked);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}