package com.quang.escan.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.quang.escan.MainActivity;
import com.quang.escan.R;
import com.quang.escan.databinding.ActivitySignInBinding;
import com.quang.escan.util.AuthManager;

/**
 * Activity that handles user sign-in with Firebase Authentication
 */
public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    
    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private boolean passwordVisible = false;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        authManager = AuthManager.getInstance(this);
        
        setupClickListeners();
        
        // Check if anonymous usage is exhausted and update UI accordingly
        updateAnonymousButtonVisibility();
    }
    
    /**
     * Update the visibility of the anonymous sign-in button based on usage status
     */
    private void updateAnonymousButtonVisibility() {
        // If anonymous usage is exhausted, hide the anonymous sign-in button
        if (authManager.isAnonymousUsageExhausted()) {
            binding.anonymousSignInButton.setVisibility(View.GONE);
        } else {
            binding.anonymousSignInButton.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + 
                    (currentUser.isAnonymous() ? "Anonymous" : currentUser.getEmail()));
            navigateToMainActivity();
        }
    }

    private void setupClickListeners() {
        // Close button click
        binding.btnClose.setOnClickListener(v -> {
            Log.d(TAG, "Close button clicked, finishing activity");
            finish();
        });
        
        // Sign in button click
        binding.signInButton.setOnClickListener(v -> {
            if (validateInputs()) {
                attemptSignIn();
            }
        });
        
        // Anonymous sign in button click
        binding.anonymousSignInButton.setOnClickListener(v -> {
            Log.d(TAG, "Anonymous sign in button clicked");
            showAnonymousSignInWarning();
        });

        // Forgot password click
        binding.forgotPassword.setOnClickListener(v -> {
            String email = binding.email.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
                binding.email.requestFocus();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
                binding.email.requestFocus();
            } else {
                sendPasswordResetEmail(email);
            }
        });

        // Sign up link click
        binding.signUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
            // Optional: add transition animation
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        
        // Toggle password visibility
        binding.togglePasswordVisibility.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                // Show password
                binding.password.setTransformationMethod(null);
                binding.togglePasswordVisibility.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                // Hide password
                binding.password.setTransformationMethod(new PasswordTransformationMethod());
                binding.togglePasswordVisibility.setImageResource(android.R.drawable.ic_menu_view);
            }
            // Move cursor to the end of the text
            binding.password.setSelection(binding.password.getText().length());
        });
    }
    
    /**
     * Show warning dialog before anonymous sign in
     */
    private void showAnonymousSignInWarning() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Continue as Guest")
                .setMessage("As a guest user, you'll only have limited access to features:\n\n" +
                        "• You can only use Extract Text feature up to 3 times\n" +
                        "• Your scanned documents won't be saved to your account\n\n" +
                        "Create an account to unlock all features!")
                .setPositiveButton("Continue as Guest", (dialog, which) -> {
                    attemptAnonymousSignIn();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Attempt anonymous sign in
     */
    private void attemptAnonymousSignIn() {
        showLoading(true);
        binding.anonymousSignInButton.setEnabled(false);
        binding.anonymousSignInButton.setText("Connecting...");
        
        authManager.signInAnonymously(this, new AuthManager.OnAuthCompleteListener() {
            @Override
            public void onAuthComplete(boolean success, String errorMessage) {
                if (success) {
                    Log.d(TAG, "Anonymous sign in success");
                    navigateToMainActivity();
                } else {
                    Log.w(TAG, "Anonymous sign in failed: " + errorMessage);
                    Toast.makeText(SignInActivity.this, 
                            "Failed to sign in as guest: " + errorMessage, 
                            Toast.LENGTH_LONG).show();
                    binding.anonymousSignInButton.setEnabled(true);
                    binding.anonymousSignInButton.setText("CONTINUE WITHOUT ACCOUNT");
                }
                showLoading(false);
            }
        });
    }

    /**
     * Validates email and password inputs
     * @return true if inputs are valid, false otherwise
     */
    private boolean validateInputs() {
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        boolean isValid = true;

        // Check email
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Check password
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    /**
     * Attempts to sign in the user using Firebase Authentication
     */
    private void attemptSignIn() {
        // Show loading indicator
        showLoading(true);

        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();

        // Use Firebase Auth to sign in
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success
                    Log.d(TAG, "signInWithEmail:success");
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    signInSuccess(user);
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                    handleSignInError(task.getException());
                }
                
                // Hide loading indicator
                showLoading(false);
            });
    }

    private void handleSignInError(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            Toast.makeText(this, "No user found with this email.", Toast.LENGTH_LONG).show();
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(this, "Invalid password.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Authentication failed: " + exception.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void signInSuccess(FirebaseUser user) {
        if (user != null) {
            // Reset usage counts for anonymous users who sign in with an account
            if (authManager.isAnonymousUser()) {
                authManager.resetUsageCounts();
            }
            
            navigateToMainActivity();
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void sendPasswordResetEmail(String email) {
        showLoading(true);
        
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent");
                        Toast.makeText(SignInActivity.this, 
                                "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "Error sending password reset email", task.getException());
                        Toast.makeText(SignInActivity.this, 
                                "Error sending password reset email: " + task.getException().getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                    
                    showLoading(false);
                });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.signInButton.setEnabled(false);
            binding.signInButton.setText("Signing In...");
            binding.anonymousSignInButton.setEnabled(false);
        } else {
            binding.signInButton.setEnabled(true);
            binding.signInButton.setText("LOG IN");
            binding.anonymousSignInButton.setEnabled(true);
        }
    }
    
    /**
     * Handle back button press
     * If anonymous usage is exhausted, show a message to user
     */
    @Override
    public void onBackPressed() {
        if (authManager.isAnonymousUsageExhausted()) {
            // If anonymous usage is exhausted, show a message
            Toast.makeText(this, 
                    "You need to sign in or create an account to continue using the app.", 
                    Toast.LENGTH_LONG).show();
        } else {
            super.onBackPressed();
        }
    }
} 