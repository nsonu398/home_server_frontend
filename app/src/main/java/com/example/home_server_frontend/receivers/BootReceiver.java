package com.example.home_server_frontend.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.home_server_frontend.service.MediaSyncService;
import com.example.home_server_frontend.utils.PreferenceManager;
import com.example.home_server_frontend.workers.MediaSyncWorker;

/**
 * BroadcastReceiver that triggers on device boot completion
 * to restart our media monitoring services
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, initializing services");

            PreferenceManager preferenceManager = new PreferenceManager(context);
            if (preferenceManager.isAutoUploadEnabled()) {
                // Set up periodic sync as a fallback
                MediaSyncWorker.schedulePeriodicSync(context);

                // Do an initial sync to catch images added while device was off
                Intent serviceIntent = new Intent(context, MediaSyncService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

                Log.d(TAG, "Auto-upload enabled, services initialized");
            }
        }
    }
}