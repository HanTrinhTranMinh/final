package com.quang.escan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.quang.escan.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;
    
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Request permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        
        setupNavigation();
    }
    
    /**
     * Sets up the bottom navigation with the Navigation Component.
     * Uses the recommended approach from the Android Jetpack documentation.
     */
    private void setupNavigation() {
        try {
            Log.d(TAG, "Setting up navigation");
            
            // Get NavHostFragment and NavController
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            
            if (navHostFragment == null) {
                Log.e(TAG, "NavHostFragment not found");
                return;
            }
            
            navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = binding.bottomNavView;
            
            if (bottomNav == null) {
                Log.e(TAG, "BottomNavigationView not found");
                return;
            }
            
            // Connect the bottom navigation view with the navigation controller
            NavigationUI.setupWithNavController(bottomNav, navController);
            
            // Define top-level destinations
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, 
                R.id.navigation_library, 
                R.id.navigation_settings
            ).build();
            
            // Add destination change listener for logging
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Log.d(TAG, "Navigated to: " + destination.getLabel());
            });
        } catch (Exception e) {
            Log.e(TAG, "Navigation setup error", e);
            Toast.makeText(this, "Error setting up navigation", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Checks if all required permissions are granted
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "Permissions required for app functionality", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}