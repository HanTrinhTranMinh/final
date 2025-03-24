package com.quang.escan.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Helper class for file operations
 */
public class FileHelper {

    private static final String TAG = "FileHelper";
    public static final String FILE_PROVIDER_AUTHORITY = "com.quang.escan.fileprovider";

    /**
     * Gets a content URI for a file using FileProvider
     * @param context The context
     * @param file The file to get URI for
     * @return The content URI, or null if there was an error
     */
    public static Uri getFileProviderUri(Context context, File file) {
        if (context == null || file == null || !file.exists()) {
            return null;
        }
        
        try {
            return FileProvider.getUriForFile(
                    context,
                    FILE_PROVIDER_AUTHORITY,
                    file);
        } catch (Exception e) {
            Log.e(TAG, "Error getting content URI for file: " + file.getAbsolutePath(), e);
            return null;
        }
    }
} 