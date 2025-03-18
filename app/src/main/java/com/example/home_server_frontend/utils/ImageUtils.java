package com.example.home_server_frontend.utils;

import android.graphics.BitmapFactory;
import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;

public class ImageUtils {

    /**
     * Get the resolution of an image in the format "widthxheight"
     * @param imagePath Path to the image file
     * @return Resolution string in format "widthxheight" or null if cannot be determined
     */
    public static String getImageResolution(String imagePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            if (options.outWidth > 0 && options.outHeight > 0) {
                return options.outWidth + "x" + options.outHeight;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the size of an image file in bytes
     * @param imagePath Path to the image file
     * @return Size in bytes or 0 if file does not exist
     */
    public static long getImageSize(String imagePath) {
        File file = new File(imagePath);
        return file.exists() ? file.length() : 0;
    }

    /**
     * Format file size to a human-readable format (KB, MB, etc.)
     * @param size Size in bytes
     * @return Formatted size string
     */
    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}