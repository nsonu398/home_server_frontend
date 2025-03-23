package com.example.home_server_frontend.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.database.AppDatabase;
import com.example.home_server_frontend.database.ImageDao;
import com.example.home_server_frontend.service.MediaSyncService;
import com.example.home_server_frontend.service.UploadService;
import com.example.home_server_frontend.utils.PreferenceManager;
import com.example.home_server_frontend.workers.MediaSyncWorker;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private Switch switchAutoUpload;
    private PreferenceManager preferenceManager;
    private ImageDao imageDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
        imageDao = AppDatabase.getInstance(this).imageDao();

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

                ChangeStatusOfAllTheRows("", "PENDING");

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
                ChangeStatusOfAllTheRows("PENDING", "");

            }
        });
    }

    @SuppressLint("CheckResult")
    private void ChangeStatusOfAllTheRows(String fromStatus, String toStatus) {
        imageDao.updateAllRowsToStatus(fromStatus, toStatus)  // Directly subscribe to Completable
                .subscribeOn(Schedulers.io())                // Run on background thread
                .observeOn(AndroidSchedulers.mainThread())   // Observe on UI thread
                .subscribe(() -> {
                    Log.d(TAG, "Status updated successfully");
                    startUploadService();
                }, error -> {
                    Log.e(TAG, "Error updating status", error);
                });

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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}