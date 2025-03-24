package com.quang.escan.ui.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.quang.escan.R;
import com.quang.escan.databinding.FragmentSettingsBinding;
import com.quang.escan.utils.ThemeUtils;

/**
 * Fragment for app settings and user profile
 * Handles user preferences, app information, and account settings
 */
public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating settings fragment view");
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        firebaseAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Setting up settings fragment");
        
        setupSettings();
        setupClickListeners();
    }

    /**
     * Initialize settings values from preferences or defaults
     */
    private void setupSettings() {
        Log.d(TAG, "Setting up settings values");
        // Set version number
        binding.versionValue.setText("1.0");
        
        // Set current theme value
        updateThemeText();
        
        // Update user information
        updateUserInfo();
    }
    
    /**
     * Update UI with current user information
     */
    private void updateUserInfo() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Set user email
            String email = currentUser.getEmail();
            binding.userEmail.setText(email != null ? email : "No email available");
            
            // Set user name (display name from Firebase)
            String displayName = currentUser.getDisplayName();
            binding.userName.setText(displayName != null && !displayName.isEmpty() ? 
                    displayName : "User");
            
            Log.d(TAG, "User info updated: " + displayName + " (" + email + ")");
        } else {
            // Not signed in - should not normally happen as this fragment should only be accessible when signed in
            binding.userName.setText("Not signed in");
            binding.userEmail.setText("");
            Log.w(TAG, "updateUserInfo: No user is signed in");
        }
    }
    
    /**
     * Update the theme text based on current theme mode
     */
    private void updateThemeText() {
        int currentTheme = ThemeUtils.getThemeMode(requireContext());
        String themeText;
        
        switch (currentTheme) {
            case ThemeUtils.MODE_DARK:
                themeText = "Dark";
                break;
            case ThemeUtils.MODE_LIGHT:
                themeText = "Light";
                break;
            case ThemeUtils.MODE_SYSTEM:
            default:
                themeText = "System default";
                break;
        }
        
        binding.themeValue.setText(themeText);
    }

    /**
     * Set up click listeners for all settings options
     */
    private void setupClickListeners() {
        Log.d(TAG, "Setting up settings click listeners");
        
        // Profile section
        binding.profileSection.setOnClickListener(v -> {
            Log.d(TAG, "Profile section clicked");
            Toast.makeText(requireContext(), "Edit profile clicked", Toast.LENGTH_SHORT).show();
            // Future: Show profile edit dialog
        });
        
        // Theme setting
        binding.themeSetting.setOnClickListener(v -> {
            Log.d(TAG, "Theme setting clicked");
            showThemeSelectionDialog();
        });
        
        
        // Storage setting
        binding.storageSetting.setOnClickListener(v -> {
            Log.d(TAG, "Storage setting clicked");
            Toast.makeText(requireContext(), "Storage setting clicked", Toast.LENGTH_SHORT).show();
            // Future: Show storage location selection dialog
        });
        
        // About
        binding.aboutSetting.setOnClickListener(v -> {
            Log.d(TAG, "About clicked");
            Toast.makeText(requireContext(), "About clicked", Toast.LENGTH_SHORT).show();

        });
        
        // Sign out
        binding.signOutButton.setOnClickListener(v -> {
            Log.d(TAG, "Sign out clicked");
            showSignOutConfirmationDialog();
        });
    }
    
    /**
     * Show confirmation dialog before signing out
     */
    private void showSignOutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out", (dialog, which) -> {
                signOut();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Sign out the current user and navigate to sign in screen
     */
    private void signOut() {
        firebaseAuth.signOut();
        Log.d(TAG, "User signed out");
        Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to sign in activity
        requireActivity().finish();
        startActivity(new android.content.Intent(requireContext(), com.quang.escan.auth.SignInActivity.class));
    }
    
    /**
     * Show dialog for theme selection
     */
    private void showThemeSelectionDialog() {
        final String[] themes = new String[] {"Light", "Dark", "System default"};
        int currentTheme = ThemeUtils.getThemeMode(requireContext());
        int selectedIndex = 0;
        
        switch (currentTheme) {
            case ThemeUtils.MODE_LIGHT:
                selectedIndex = 0;
                break;
            case ThemeUtils.MODE_DARK:
                selectedIndex = 1;
                break;
            case ThemeUtils.MODE_SYSTEM:
                selectedIndex = 2;
                break;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Choose Theme")
               .setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
                   int selectedTheme;
                   switch (which) {
                       case 0:
                           selectedTheme = ThemeUtils.MODE_LIGHT;
                           break;
                       case 1:
                           selectedTheme = ThemeUtils.MODE_DARK;
                           break;
                       case 2:
                       default:
                           selectedTheme = ThemeUtils.MODE_SYSTEM;
                           break;
                   }
                   
                   ThemeUtils.setThemeMode(requireContext(), selectedTheme);
                   updateThemeText();
                   dialog.dismiss();
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        builder.create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user info when fragment becomes visible
        updateUserInfo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleaning up settings fragment");
        binding = null;
    }
} 