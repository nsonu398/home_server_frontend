package com.example.home_server_frontend.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.home_server_frontend.service.MediaSyncService;
import com.example.home_server_frontend.utils.PreferenceManager;

/**
 * BroadcastReceiver that listens for new media being added to the device
 * and triggers the media sync service to process them.
 */
public class MediaContentReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaContentReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null &&
                (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action) ||
                        "android.hardware.action.NEW_PICTURE".equals(action))) {

            Log.d(TAG, "Media content change detected: " + action);

            PreferenceManager preferenceManager = new PreferenceManager(context);
            if (preferenceManager.isAutoUploadEnabled()) {
                // Start the service to handle new media
                Intent serviceIntent = new Intent(context, MediaSyncService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}