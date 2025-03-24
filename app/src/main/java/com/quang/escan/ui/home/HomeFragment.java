package com.quang.escan.ui.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.quang.escan.R;
import com.quang.escan.auth.SignInActivity;
import com.quang.escan.databinding.FragmentHomeBinding;
import com.quang.escan.model.ExtractedDocument;
import com.quang.escan.ui.library.DocumentViewerFragment;
import com.quang.escan.ui.library.LibraryRepository;
import com.quang.escan.ui.scan.ImageSourceDialogFragment;
import com.quang.escan.util.AuthManager;
import com.quang.escan.util.FileHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Home screen fragment - the main landing page of the application
 */
public class HomeFragment extends Fragment implements 
        ImageSourceDialogFragment.ImageSourceListener {

    private static final String TAG = "HomeFragment";
    private static final int REQUEST_IMAGE_PICK = 1;
    
    // Feature identifiers
    private static final int FEATURE_EXTRACT_TEXT = 1;
    private static final int FEATURE_EXTRACT_HANDWRITING = 2;
    private static final int FEATURE_WATERMARK = 3;
    private static final int FEATURE_QR_SCAN = 4;
    
    private FragmentHomeBinding binding;
    private NavController navController;
    private int lastClickedFeature;
    private AuthManager authManager;
    
    private LibraryRepository libraryRepository;
    private RecentFilesAdapter recentFilesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating home fragment view");
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Setting up home fragment");
        
        navController = Navigation.findNavController(view);
        authManager = AuthManager.getInstance(requireContext());
        libraryRepository = new LibraryRepository(requireContext());
        
        setupRecentFiles();
        setupClickListeners();
        loadRecentDocuments();
        
        // Show feature usage dialog for anonymous users
        if (authManager.isAnonymousUser()) {
            authManager.showAnonymousLimitDialog(requireContext());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh documents when returning to this fragment
        loadRecentDocuments();
    }

    private void setupRecentFiles() {
        // Set up RecyclerView
        binding.recyclerviewRecentFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Create and set adapter with empty list
        recentFilesAdapter = new RecentFilesAdapter(new ArrayList<>());
        recentFilesAdapter.setOnItemClickListener(new RecentFilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecentFile file) {
                // Navigate to document viewer
                if (file.getDocumentId() > 0) {
                    Bundle args = new Bundle();
                    args.putLong("document_id", file.getDocumentId());
                    navController.navigate(R.id.navigation_document_viewer, args);
                }
            }

            @Override
            public void onShareClick(RecentFile file) {
                shareDocument(file);
            }

            @Override
            public void onMoreClick(RecentFile file) {
                showFileOptions(file);
            }
        });
        
        binding.recyclerviewRecentFiles.setAdapter(recentFilesAdapter);
    }
    
    /**
     * Load recent documents from the library
     */
    private void loadRecentDocuments() {
        List<ExtractedDocument> documents = libraryRepository.getAllDocuments();
        List<RecentFile> recentFiles = new ArrayList<>();
        
        for (ExtractedDocument document : documents) {
            // Convert ExtractedDocument to RecentFile
            Bitmap thumbnail = null;
            if (document.getImagePath() != null && !document.getImagePath().isEmpty()) {
                File imageFile = new File(document.getImagePath());
                if (imageFile.exists()) {
                    // Load a downsampled thumbnail to save memory
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4; // Scale down to 1/4 the original size
                    thumbnail = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
                }
            }
            
            // Format date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);
            String formattedDate = document.getCreationDate() != null ? 
                    dateFormat.format(document.getCreationDate()) : "Unknown";
            
            RecentFile recentFile = new RecentFile(
                    document.getFileName(),
                    formattedDate,
                    thumbnail
            );
            
            // Store document ID for later use
            recentFile.setDocumentId(document.getId());
            
            recentFiles.add(recentFile);
        }
        
        // Update UI based on data
        if (recentFiles.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.recyclerviewRecentFiles.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.recyclerviewRecentFiles.setVisibility(View.VISIBLE);
            recentFilesAdapter.updateRecentFiles(recentFiles);
        }
    }

    private void setupClickListeners() {
        // Feature buttons
        setupFeatureButtons();
        
        // Collapse recent files
        binding.btnCollapseRecent.setOnClickListener(v -> {
            Log.d(TAG, "Collapse Recent Files clicked");
            toggleRecentFilesVisibility();
        });
        
        // Contact Us button
        binding.fabContactUs.setOnClickListener(v -> {
            Log.d(TAG, "Contact Us clicked");
            showContactDialog();
        });
    }

    /**
     * Set up feature buttons
     */
    private void setupFeatureButtons() {
        // Extract Text feature
        binding.featureExtractText.setOnClickListener(v -> {
            Log.d(TAG, "Extract Text button clicked");
            lastClickedFeature = FEATURE_EXTRACT_TEXT;
            
            // Check if anonymous user can use this feature
            if (authManager.isAnonymousUser()) {
                if (!authManager.canUseExtractFeature(requireContext())) {
                    authManager.showLimitReachedDialog(requireContext());
                    return;
                }
            }
            
            showImageSourceDialog();
        });
        
        // Extract Handwriting feature
        binding.featureExtractHandwriting.setOnClickListener(v -> {
            Log.d(TAG, "Extract Handwriting button clicked");
            
            // For anonymous users, restrict features other than Extract Text
            if (authManager.isAnonymousUser()) {
                showFeatureRestrictedDialog();
                return;
            }
            
            lastClickedFeature = FEATURE_EXTRACT_HANDWRITING;
            showImageSourceDialog();
        });
        //Translate Frature
        binding.featureTranslate.setOnClickListener(v -> {
            Log.d(TAG, "Translate clicked");
            // Translation feature implementation
            Intent intent = new Intent(requireContext(), TranslateActivity.class);
            startActivity(intent);
        });
        // Watermark feature
        binding.featureWatermark.setOnClickListener(v -> {
            Log.d(TAG, "Watermark button clicked");
            
            // For anonymous users, restrict features other than Extract Text
            if (authManager.isAnonymousUser()) {
                showFeatureRestrictedDialog();
                return;
            }
            
            lastClickedFeature = FEATURE_WATERMARK;
            showImageSourceDialog();
        });
        
        // QR Scan feature
        binding.featureQrScan.setOnClickListener(v -> {
            Log.d(TAG, "QR Scan button clicked");
            
            // For anonymous users, restrict features other than Extract Text
            if (authManager.isAnonymousUser()) {
                showFeatureRestrictedDialog();
                return;
            }
            
            lastClickedFeature = FEATURE_QR_SCAN;
            // Navigate directly to dedicated QR scan fragment
            navController.navigate(R.id.navigation_qr_scan);
        });
    }
    
    /**
     * Show dialog that explains feature restriction for anonymous users
     */
    private void showFeatureRestrictedDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Feature Restricted")
                .setMessage("This feature is only available for registered users.\n\n" +
                        "As a guest user, you can only use the Extract Text feature, and only up to 3 times.\n\n" +
                        "Would you like to sign in or create an account now?")
                .setPositiveButton("Sign In", (dialog, which) -> {
                    // Sign out before navigating to sign in screen
                    authManager.signOut();
                    Intent intent = new Intent(requireContext(), SignInActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }
    
    /**
     * Share a document from recent files
     */
    private void shareDocument(RecentFile file) {
        if (file.getDocumentId() <= 0) {
            Toast.makeText(requireContext(), "Cannot share this file", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Get document from repository
            ExtractedDocument document = libraryRepository.getDocumentById(file.getDocumentId());
            if (document == null) {
                Toast.makeText(requireContext(), "Document not found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create intent to share text
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, document.getFileName());
            
            // Add the document's content as text
            String textContent = document.getExtractedText();
            if (textContent == null || textContent.trim().isEmpty()) {
                textContent = "(No text content available)";
            }
            shareIntent.putExtra(Intent.EXTRA_TEXT, textContent);
            
            boolean hasImage = false;
            
            // If there's an image, add it as well
            if (document.getImagePath() != null && !document.getImagePath().isEmpty()) {
                File imageFile = new File(document.getImagePath());
                
                if (imageFile.exists()) {
                    Uri imageUri = FileHelper.getFileProviderUri(requireContext(), imageFile);
                    
                    if (imageUri != null) {
                        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                        shareIntent.setType("image/jpeg");
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        hasImage = true;
                    }
                }
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share '" + document.getFileName() + "' via"));
        } catch (Exception e) {
            // Log and show error message
            Log.e(TAG, "Error sharing document", e);
            Toast.makeText(requireContext(), 
                    "Error sharing document: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show options for a file (view, delete, rename)
     */
    private void showFileOptions(RecentFile file) {
        if (file.getDocumentId() <= 0) {
            Toast.makeText(requireContext(), "Invalid file", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(file.getFileName());
        
        String[] options = {"View", "Rename", "Delete"};
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // View
                    // Navigate to document viewer
                    Bundle args = new Bundle();
                    args.putLong("document_id", file.getDocumentId());
                    navController.navigate(R.id.navigation_document_viewer, args);
                    break;
                    
                case 1: // Rename
                    showRenameDialog(file);
                    break;
                    
                case 2: // Delete
                    confirmDeleteFile(file);
                    break;
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show dialog to rename a file
     */
    private void showRenameDialog(RecentFile file) {
        // Get document from repository
        ExtractedDocument document = libraryRepository.getDocumentById(file.getDocumentId());
        if (document == null) {
            Toast.makeText(requireContext(), "Document not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create edit text for new name
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setText(document.getFileName());
        input.setSelectAllOnFocus(true);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rename Document");
        builder.setView(input);
        
        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            
            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Update document name
            document.setFileName(newName);
            long result = libraryRepository.saveDocument(document);
            
            if (result > 0) {
                // Refresh list without toast message
                loadRecentDocuments();
            } else {
                Toast.makeText(requireContext(), "Error renaming document", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Confirm deletion of a file
     */
    private void confirmDeleteFile(RecentFile file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Document");
        builder.setMessage("Are you sure you want to delete '" + file.getFileName() + "'?");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Get document to access the image path
            ExtractedDocument document = libraryRepository.getDocumentById(file.getDocumentId());
            
            // Delete from repository
            boolean deleted = libraryRepository.deleteDocument(file.getDocumentId());
            
            if (deleted) {
                // Also delete the image file if it exists
                if (document != null && document.getImagePath() != null && !document.getImagePath().isEmpty()) {
                    File imageFile = new File(document.getImagePath());
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }
                }
                
                // Refresh list without toast message
                loadRecentDocuments();
            } else {
                Toast.makeText(requireContext(), "Error deleting document", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Shows the image source selection dialog
     */
    private void showImageSourceDialog() {
        ImageSourceDialogFragment dialog = new ImageSourceDialogFragment();
        dialog.setImageSourceListener(this);
        dialog.show(getChildFragmentManager(), "ImageSourceDialog");
    }
    
    /**
     * Opens the gallery for image selection
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    
    /**
     * Handle camera selection from dialog
     */
    @Override
    public void onCameraSelected() {
        Log.d(TAG, "Camera option selected");
        
        // For anonymous users, increment usage count for Extract Text
        if (authManager.isAnonymousUser() && lastClickedFeature == FEATURE_EXTRACT_TEXT) {
            authManager.incrementExtractFeatureCount();
        }
        
        // Navigate to scan fragment with appropriate flag
        Bundle args = new Bundle();
        
        if (lastClickedFeature == FEATURE_QR_SCAN) {
            // For QR scanning, use dedicated fragment
            navController.navigate(R.id.navigation_qr_scan);
            return;
        }
        
        // For both text and handwriting recognition, use for_text_recognition flag
        if (lastClickedFeature == FEATURE_EXTRACT_TEXT || lastClickedFeature == FEATURE_EXTRACT_HANDWRITING) {
            args.putBoolean("for_text_recognition", true);
            // Store the feature type to distinguish between text and handwriting later
            args.putInt("feature_type", lastClickedFeature);
        }
        else if (lastClickedFeature == FEATURE_WATERMARK) {
            // For watermark, we'll take the photo first and then go to watermark
            args.putBoolean("for_watermark", true);
            args.putInt("feature_type", lastClickedFeature);
        }
        
        navController.navigate(R.id.navigation_scan, args);
    }
    
    /**
     * Handle gallery selection from dialog
     */
    @Override
    public void onGallerySelected() {
        Log.d(TAG, "Gallery option selected");
        openGallery();
    }

    /**
     * Toggles the visibility of the recent files section
     */
    private void toggleRecentFilesVisibility() {
        if (binding.recyclerviewRecentFiles.getVisibility() == View.VISIBLE) {
            binding.recyclerviewRecentFiles.setVisibility(View.GONE);
            binding.btnCollapseRecent.setRotation(0); // Right arrow
        } else {
            binding.recyclerviewRecentFiles.setVisibility(View.VISIBLE);
            binding.btnCollapseRecent.setRotation(90); // Down arrow
        }
    }
    
    /**
     * Shows the contact dialog with support information
     */
    private void showContactDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Contact Us")
            .setMessage("Need help with the app? Contact our support team:\n\nEmail: support@escan.com\nPhone: +1-800-123-4567")
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Handle activity result for gallery image picking
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        try {
            if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
                Uri selectedImageUri = data.getData();
                
                if (selectedImageUri != null) {
                    Log.d(TAG, "Image picked from gallery: " + selectedImageUri);
                    
                    // For anonymous users, increment usage count for Extract Text
                    if (authManager.isAnonymousUser() && lastClickedFeature == FEATURE_EXTRACT_TEXT) {
                        authManager.incrementExtractFeatureCount();
                    }
                    
                    // Navigate to the image edit fragment with appropriate flags
                    Bundle args = new Bundle();
                    args.putString("imagePath", selectedImageUri.toString());
                    
                    // For both text and handwriting recognition, use for_text_recognition flag
                    if (lastClickedFeature == FEATURE_EXTRACT_TEXT || lastClickedFeature == FEATURE_EXTRACT_HANDWRITING) {
                        args.putBoolean("for_text_recognition", true);
                        // Store the feature type to distinguish between text and handwriting later
                        args.putInt("feature_type", lastClickedFeature);
                    }
                    // For watermark feature
                    else if (lastClickedFeature == FEATURE_WATERMARK) {
                        // We'll get the file path from URI
                        String imagePath = getPathFromUri(selectedImageUri);
                        Bundle watermarkArgs = new Bundle();
                        watermarkArgs.putString("imagePath", imagePath);
                        navController.navigate(R.id.navigation_watermark, watermarkArgs);
                        return;
                    }
                    // For QR scan
                    else if (lastClickedFeature == FEATURE_QR_SCAN) {
                        // For QR scan, we'll use the path to create a File in the QrScanFragment
                        // and process it directly
                        String imagePath = getPathFromUri(selectedImageUri);
                        Bundle qrArgs = new Bundle();
                        qrArgs.putString("image_uri", selectedImageUri.toString());
                        navController.navigate(R.id.navigation_qr_scan, qrArgs);
                        return;
                    }
                    
                    Log.d(TAG, "Navigating to image edit with args: " + args);
                    navController.navigate(R.id.navigation_image_edit, args);
                } else {
                    Log.e(TAG, "Selected image URI is null");
                    Toast.makeText(requireContext(), "Failed to get selected image", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onActivityResult", e);
            Toast.makeText(requireContext(), "Error processing selected image: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Get file path from URI
     */
    private String getPathFromUri(Uri uri) {
        try {
            if ("content".equals(uri.getScheme())) {
                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = requireActivity().getContentResolver().query(
                        uri, projection, null, null, null);
                
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    String path = cursor.getString(columnIndex);
                    cursor.close();
                    return path;
                }
            } else if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file path from URI", e);
        }
        
        // Return the URI string as a fallback
        return uri.toString();
    }
    
    // Define inner interface for image source dialog listeners
    public interface ImageSourceListener {
        void onCameraSelected();
        void onGallerySelected();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleaning up home fragment");
        binding = null;
    }
} 