package com.quang.escan.ui.ocr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quang.escan.R;
import com.quang.escan.databinding.ActivitySaveExtractedTextBinding;
import com.quang.escan.model.ExtractedDocument;
import com.quang.escan.ui.library.LibraryRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.widget.ArrayAdapter;

/**
 * Activity for saving extracted text to the library
 * Allows naming, categorizing, and saving extracted text documents
 */
public class SaveExtractedTextActivity extends AppCompatActivity {

    private static final String TAG = "SaveExtractActivity";
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_EXTRACTED_TEXT = "extra_extracted_text";
    public static final String EXTRA_FEATURE_TYPE = "feature_type";
    private static final int MAX_DISPLAY_WIDTH = 800;
    private static final int FEATURE_EXTRACT_TEXT = 0;
    private static final int FEATURE_EXTRACT_HANDWRITING = 1;
    
    private ActivitySaveExtractedTextBinding binding;
    private LibraryRepository libraryRepository;
    private Uri imageUri;
    private Bitmap imageBitmap;
    private String extractedText;
    private String imageUriString;
    private int featureType = FEATURE_EXTRACT_TEXT; // Default to text extraction
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySaveExtractedTextBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize repository
        libraryRepository = new LibraryRepository(this);
        
        // Get feature type from intent
        featureType = getIntent().getIntExtra(EXTRA_FEATURE_TYPE, FEATURE_EXTRACT_TEXT);
        Log.d(TAG, "Feature type: " + featureType);
        
        // Setup UI
        setupToolbar();
        setupClickListeners();
        
        // Get data from intent
        handleIntent();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            
            // Set title based on feature type
            if (featureType == FEATURE_EXTRACT_HANDWRITING) {
                getSupportActionBar().setTitle(R.string.save_extracted_ink);
            } else {
                getSupportActionBar().setTitle(R.string.save_extracted_text);
            }
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupClickListeners() {
        // Cancel button
        binding.btnCancel.setOnClickListener(v -> navigateBack());
        
        // Save button
        binding.btnSave.setOnClickListener(v -> saveDocument());
    }
    
    /**
     * Get data from intent
     */
    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            // Get extracted text
            extractedText = intent.getStringExtra(EXTRA_EXTRACTED_TEXT);
            if (extractedText != null) {
                binding.txtExtractedText.setText(extractedText);
            }
            
            // Get image URI
            imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI);
            if (imageUriString != null) {
                loadImage();
            }
            
            // Get feature type
            featureType = intent.getIntExtra(EXTRA_FEATURE_TYPE, FEATURE_EXTRACT_TEXT);
        }
        
        // Setup default filename based on feature type
        setupDefaultFilename();
    }
    
    /**
     * Set up category spinner with predefined categories
     */
    private void setupCategorySpinner() {
        String[] categories = {"Personal", "Work", "School"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Using the radio group directly instead of a non-existent spinner
        // The binding.spinnerCategory.setAdapter(adapter); line is removed
    }
    
    /**
     * Generate default filename based on feature type
     */
    private void setupDefaultFilename() {
        // Generate filename with timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        
        // Set filename based on feature type
        if (featureType == FEATURE_EXTRACT_HANDWRITING) {
            binding.editFileName.setText("Handwriting_" + timestamp);
        } else {
            binding.editFileName.setText("Text_" + timestamp);
        }
    }
    
    /**
     * Load and display the image
     */
    private void loadImage() {
        if (imageUriString == null) {
            return;
        }

        try {
            imageUri = Uri.parse(imageUriString);
            Log.d(TAG, "Loading image from URI: " + imageUri);
            
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                if (inputStream != null) {
                    imageBitmap = BitmapFactory.decodeStream(inputStream);
                    
                    if (imageBitmap != null) {
                        // Scale bitmap if too large
                        if (imageBitmap.getWidth() > MAX_DISPLAY_WIDTH) {
                            float scaleFactor = (float) MAX_DISPLAY_WIDTH / imageBitmap.getWidth();
                            int newHeight = (int) (imageBitmap.getHeight() * scaleFactor);
                            
                            imageBitmap = Bitmap.createScaledBitmap(
                                    imageBitmap, MAX_DISPLAY_WIDTH, newHeight, true);
                        }
                        
                        binding.imagePreview.setImageBitmap(imageBitmap);
                    } else {
                        Log.e(TAG, "Failed to decode bitmap from URI");
                        binding.imagePreview.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            binding.imagePreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
    
    private void saveDocument() {
        // Validate input
        String fileName = binding.editFileName.getText().toString().trim();
        if (TextUtils.isEmpty(fileName)) {
            binding.editFileName.setError("Please enter a file name");
            return;
        }
        
        // Determine selected category
        String category = "Personal"; // Default
        int selectedId = binding.radioGroupCategory.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton radioButton = findViewById(selectedId);
            category = radioButton.getText().toString();
        }
        
        // Create the document model
        ExtractedDocument document = new ExtractedDocument();
        document.setFileName(fileName);
        document.setCategory(category);
        document.setExtractedText(extractedText);
        document.setCreationDate(new Date());
        
        // Save the image if available
        if (imageBitmap != null) {
            String imagePath = saveImageToStorage(imageBitmap, fileName);
            document.setImagePath(imagePath);
        }
        
        // Save to repository
        long documentId = libraryRepository.saveDocument(document);
        
        if (documentId > 0) {
            Toast.makeText(this, "Document saved successfully", Toast.LENGTH_SHORT).show();
            
            // Navigate to library fragment
            navigateToLibrary(category);
        } else {
            Toast.makeText(this, "Error saving document", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String saveImageToStorage(Bitmap bitmap, String fileName) {
        File directory = new File(getExternalFilesDir(null), "scans");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        String imageFileName = fileName + ".jpg";
        File imageFile = new File(directory, imageFileName);
        
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }
    
    private void navigateBack() {
        // Navigate back to home
        Intent intent = new Intent(this, com.quang.escan.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
    
    private void navigateToLibrary(String category) {
        // Navigate to library fragment with selected category
        Intent intent = new Intent(this, com.quang.escan.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("navigate_to", "library");
        intent.putExtra("selected_category", category);
        startActivity(intent);
        finish();
    }
} 