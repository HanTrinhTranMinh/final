package com.quang.escan.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class for managing dark/light theme
 */
public class ThemeUtils {
    
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    // Theme mode constants
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    
    /**
     * Set the theme mode (light, dark, or system)
     */
    public static void setThemeMode(Context context, int themeMode) {
        // Save the theme preference
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        
        // Apply the theme
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
    
    /**
     * Get the current theme mode
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, getDefaultThemeMode());
    }
    
    /**
     * Apply the saved theme mode or the default one
     */
    public static void applyTheme(Context context) {
        int themeMode = getThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
    
    /**
     * Get the default theme mode based on system version
     */
    private static int getDefaultThemeMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? 
                MODE_SYSTEM : MODE_LIGHT;
    }
    
    /**
     * Toggle between light and dark themes
     * If the current theme is system, it will switch to light
     */
    public static void toggleTheme(Context context) {
        int currentMode = getThemeMode(context);
        int newMode;
        
        if (currentMode == MODE_DARK) {
            newMode = MODE_LIGHT;
        } else {
            newMode = MODE_DARK;
        }
        
        setThemeMode(context, newMode);
    }
} 