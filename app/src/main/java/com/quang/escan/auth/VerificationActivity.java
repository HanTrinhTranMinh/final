package com.quang.escan.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.quang.escan.databinding.ActivityVerificationBinding;

/**
 * Activity that shows verification success message to the user
 */
public class VerificationActivity extends AppCompatActivity {

    private static final String TAG = "VerificationActivity";
    private ActivityVerificationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Continue button redirects to sign in
        binding.continueButton.setOnClickListener(v -> {
            Log.d(TAG, "Continue button clicked, navigating to SignInActivity");
            navigateToSignIn();
        });
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(VerificationActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
} 