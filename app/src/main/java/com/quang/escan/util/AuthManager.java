package com.quang.escan.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.quang.escan.auth.SignInActivity;

/**
 * Manages authentication state and tracks feature usage for anonymous users
 */
public class AuthManager {
    private static final String TAG = "AuthManager";
    
    // Constants
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_EXTRACT_TEXT_COUNT = "extract_text_count";
    private static final int MAX_ANONYMOUS_EXTRACTS = 3;
    
    // Singleton instance
    private static volatile AuthManager instance;
    
    private final FirebaseAuth firebaseAuth;
    private final SharedPreferences prefs;
    
    /**
     * Get singleton instance of AuthManager
     */
    public static AuthManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AuthManager.class) {
                if (instance == null) {
                    instance = new AuthManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    private AuthManager(Context context) {
        firebaseAuth = FirebaseAuth.getInstance();
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if user is signed in
     */
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }
    
    /**
     * Check if current user is anonymous
     */
    public boolean isAnonymousUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null && user.isAnonymous();
    }
    
    /**
     * Sign in anonymously
     */
    public void signInAnonymously(Context context, OnAuthCompleteListener listener) {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Anonymous auth successful");
                        if (listener != null) {
                            listener.onAuthComplete(true, null);
                        }
                    } else {
                        Log.e(TAG, "Anonymous auth failed", task.getException());
                        if (listener != null) {
                            listener.onAuthComplete(false, task.getException().getMessage());
                        }
                    }
                });
    }
    
    /**
     * Sign out current user
     */
    public void signOut() {
        firebaseAuth.signOut();
    }
    
    /**
     * Reset usage counters
     * Should be called when user signs in with a regular account
     */
    public void resetUsageCounts() {
        prefs.edit().putInt(KEY_EXTRACT_TEXT_COUNT, 0).apply();
    }
    
    /**
     * Check if anonymous user can use extract text feature
     * @return true if user can use feature, false if limit reached
     */
    public boolean canUseExtractFeature(Context context) {
        if (!isAnonymousUser()) {
            return true; // Regular users have unlimited access
        }
        
        int currentCount = getExtractFeatureCount();
        return currentCount < MAX_ANONYMOUS_EXTRACTS;
    }
    
    /**
     * Get current count of extract text feature usage
     */
    public int getExtractFeatureCount() {
        return prefs.getInt(KEY_EXTRACT_TEXT_COUNT, 0);
    }
    
    /**
     * Get remaining uses for extract text feature
     */
    public int getRemainingExtractUses() {
        if (!isAnonymousUser()) {
            return Integer.MAX_VALUE; // Regular users have unlimited access
        }
        
        int currentCount = getExtractFeatureCount();
        return Math.max(0, MAX_ANONYMOUS_EXTRACTS - currentCount);
    }
    
    /**
     * Increment the extract text feature usage counter
     * @return The number of remaining uses
     */
    public int incrementExtractFeatureCount() {
        int currentCount = getExtractFeatureCount();
        currentCount++;
        prefs.edit().putInt(KEY_EXTRACT_TEXT_COUNT, currentCount).apply();
        return MAX_ANONYMOUS_EXTRACTS - currentCount;
    }
    
    /**
     * Check if anonymous usage is exhausted (all available uses consumed)
     */
    public boolean isAnonymousUsageExhausted() {
        return isAnonymousUser() && getExtractFeatureCount() >= MAX_ANONYMOUS_EXTRACTS;
    }
    
    /**
     * Show dialog for anonymous users with feature limits
     */
    public void showAnonymousLimitDialog(Context context) {
        int remaining = getRemainingExtractUses();
        
        new AlertDialog.Builder(context)
                .setTitle("Feature Restrictions")
                .setMessage("As an anonymous user, you can only use the Extract Text feature " + 
                        MAX_ANONYMOUS_EXTRACTS + " times.\n\n" +
                        "You have " + remaining + " uses remaining.\n\n" +
                        "Sign in or create an account to unlock all features without limits.")
                .setPositiveButton("Sign In", (dialog, which) -> {
                    // Sign out before navigating to sign in screen
                    signOut();
                    Intent intent = new Intent(context, SignInActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }
    
    /**
     * Show dialog when anonymous user has reached usage limit
     */
    public void showLimitReachedDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Usage Limit Reached")
                .setMessage("You've reached the maximum number of uses for this feature as an anonymous user.\n\n" +
                        "Sign in or create an account to continue using all features without limits.")
                .setPositiveButton("Sign In", (dialog, which) -> {
                    // Sign out before navigating to sign in screen
                    signOut();
                    Intent intent = new Intent(context, SignInActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }
    
    public interface OnAuthCompleteListener {
        void onAuthComplete(boolean success, String errorMessage);
    }
} 