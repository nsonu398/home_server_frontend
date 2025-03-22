package com.example.home_server_frontend.workers;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import com.example.home_server_frontend.service.MediaSyncService;
import com.example.home_server_frontend.utils.PreferenceManager;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager worker that periodically checks for new images
 * This serves as a fallback mechanism in case broadcast receivers fail
 */
public class MediaSyncWorker extends Worker {
    private static final String TAG = "MediaSyncWorker";

    public MediaSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());

        // Only proceed if auto-upload is enabled
        if (!preferenceManager.isAutoUploadEnabled()) {
            return Result.success();
        }

        Log.d(TAG, "Periodic work triggered - starting media sync service");

        // Start the sync service
        Intent intent = new Intent(getApplicationContext(), MediaSyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(intent);
        } else {
            getApplicationContext().startService(intent);
        }

        return Result.success();
    }

    /**
     * Schedule periodic sync as a fallback mechanism
     */
    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                MediaSyncWorker.class,
                15, TimeUnit.MINUTES)  // Check every 15 minutes
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "media_sync_periodic",
                ExistingPeriodicWorkPolicy.REPLACE,
                syncRequest);

        Log.d(TAG, "Scheduled periodic media sync work");
    }

    /**
     * Cancel periodic sync
     */
    public static void cancelPeriodicSync(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork("media_sync_periodic");
        Log.d(TAG, "Cancelled periodic media sync work");
    }
}