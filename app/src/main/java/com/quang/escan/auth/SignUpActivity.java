package com.quang.escan.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.quang.escan.R;
import com.quang.escan.databinding.ActivitySignUpBinding;

/**
 * Activity for creating a new user account with Firebase Authentication
 */
public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    
    private ActivitySignUpBinding binding;
    private FirebaseAuth firebaseAuth;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Close button click
        binding.btnClose.setOnClickListener(v -> {
            Log.d(TAG, "Close button clicked, finishing activity");
            finish();
        });
        
        // Sign up button click
        binding.signUpButton.setOnClickListener(v -> {
            if (validateInputs()) {
                createAccount();
            }
        });
        
        // Sign in link click
        binding.signInLink.setOnClickListener(v -> {
            Log.d(TAG, "Sign in link clicked, finishing activity");
            finish();
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
     * Validates all user inputs
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateInputs() {
        String name = binding.name.getText().toString().trim();
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        String confirmPassword = binding.confirmPassword.getText().toString().trim();
        boolean isValid = true;
        
        // Validate name
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            binding.name.requestFocus();
            isValid = false;
        }
        
        // Validate email
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, getString(R.string.email_required), Toast.LENGTH_SHORT).show();
            binding.email.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            binding.email.requestFocus();
            isValid = false;
        }
        
        // Validate password
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.password_required), Toast.LENGTH_SHORT).show();
            binding.password.requestFocus();
            isValid = false;
        } else if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show();
            binding.password.requestFocus();
            isValid = false;
        }
        
        // Validate password confirmation
        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            binding.confirmPassword.requestFocus();
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
            binding.confirmPassword.requestFocus();
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Creates a new user account using Firebase Authentication
     */
    private void createAccount() {
        // Show loading indicator
        showLoading(true);
        
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        
        // Create account with Firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            updateUserProfile(user);
                        } else {
                            // Sign up failed
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            handleSignUpError(task.getException());
                            
                            // Hide loading indicator
                            showLoading(false);
                        }
                    }
                });
    }
    
    /**
     * Updates the user's profile with additional information
     */
    private void updateUserProfile(FirebaseUser user) {
        if (user != null) {
            // Send email verification
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email verification sent");
                            // Navigate to verification screen
                            navigateToVerification();
                        } else {
                            Log.w(TAG, "Failed to send verification email", task.getException());
                            Toast.makeText(SignUpActivity.this, 
                                    "Account created but verification email failed to send", 
                                    Toast.LENGTH_LONG).show();
                            navigateToSignIn();
                        }
                        
                        // Hide loading indicator
                        showLoading(false);
                    });
        }
    }
    
    /**
     * Handles sign up errors
     */
    private void handleSignUpError(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            Toast.makeText(this, getString(R.string.email_already_in_use), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Sign up failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Navigates to the verification screen
     */
    private void navigateToVerification() {
        Intent intent = new Intent(SignUpActivity.this, VerificationActivity.class);
        startActivity(intent);
        finish();
    }
    
    /**
     * Navigates back to the sign in screen
     */
    private void navigateToSignIn() {
        finish();
    }
    
    /**
     * Shows or hides the loading indicator
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.signUpButton.setEnabled(false);
            binding.signUpButton.setText("Creating Account...");
        } else {
            binding.signUpButton.setEnabled(true);
            binding.signUpButton.setText("SIGN UP");
        }
    }
} 