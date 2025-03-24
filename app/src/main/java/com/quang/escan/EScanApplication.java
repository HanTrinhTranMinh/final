package com.quang.escan;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.quang.escan.utils.ThemeUtils;

/**
 * Application class to initialize Firebase and other app-wide configurations
 */
public class EScanApplication extends Application {

    private static final String TAG = "EScanApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Apply the saved theme
        ThemeUtils.applyTheme(this);
        
        // Initialize Firebase
        initializeFirebase();
    }
    
    /**
     * Initialize Firebase SDK
     */
    private void initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this);
            // Initialize Firebase Auth
            FirebaseAuth.getInstance();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
        }
    }
} 