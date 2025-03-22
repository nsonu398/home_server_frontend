package com.example.home_server_frontend.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.service.MediaSyncService;
import com.example.home_server_frontend.utils.PreferenceManager;
import com.example.home_server_frontend.workers.MediaSyncWorker;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

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

            if (isChecked) {
                Log.d(TAG, "Auto-upload enabled, setting up services");

                // Enable periodic background sync as fallback
                MediaSyncWorker.schedulePeriodicSync(this);

                // Do an initial sync immediately
                Intent intent = new Intent(this, MediaSyncService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            } else {
                Log.d(TAG, "Auto-upload disabled, cancelling services");

                // Disable periodic sync
                MediaSyncWorker.cancelPeriodicSync(this);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}