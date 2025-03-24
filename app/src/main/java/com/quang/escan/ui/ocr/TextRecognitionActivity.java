package com.quang.escan.ui.ocr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quang.escan.R;
import com.quang.escan.databinding.ActivityTextRecognitionBinding;
import com.quang.escan.ml.TextRecognitionHelper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Activity for text recognition with multiple language support
 */
public class TextRecognitionActivity extends AppCompatActivity 
        implements TextRecognitionHelper.TextRecognitionCallback {

    private static final String TAG = "TextRecognitionActivity";
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_FEATURE_TYPE = "feature_type";
    private static final int MAX_DISPLAY_WIDTH = 800;
    private static final int FEATURE_EXTRACT_TEXT = 0;
    private static final int FEATURE_EXTRACT_HANDWRITING = 1;
    
    private ActivityTextRecognitionBinding binding;
    private TextRecognitionHelper textRecognitionHelper;
    private Uri imageUri;
    private Bitmap imageBitmap;
    private TextRecognitionHelper.LanguageModel currentLanguageModel = 
            TextRecognitionHelper.LanguageModel.LATIN;
    private int featureType = FEATURE_EXTRACT_TEXT; // Default to text extraction
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTextRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize text recognition helper
        textRecognitionHelper = new TextRecognitionHelper(this, this);
        
        // Get feature type from intent
        featureType = getIntent().getIntExtra(EXTRA_FEATURE_TYPE, FEATURE_EXTRACT_TEXT);
        Log.d(TAG, "Feature type: " + featureType);
        
        // Setup UI
        setupToolbar();
        setupLanguageSpinner();
        setupClickListeners();
        
        // Get image URI from intent
        handleIntent();
    }
    
    /**
     * Set up the toolbar with appropriate title
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Set title based on feature type
        if (featureType == FEATURE_EXTRACT_HANDWRITING) {
            getSupportActionBar().setTitle(R.string.ink_recognition_title);
        } else {
            getSupportActionBar().setTitle(R.string.text_recognition_title);
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    
    private void setupLanguageSpinner() {
        // Create adapter for language selection
        String[] languages = new String[]{
                "Latin (English, Spanish, etc.)", 
                "Chinese", 
                "Devanagari (Hindi, etc.)", 
                "Japanese", 
                "Korean"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLanguage.setAdapter(adapter);
        
        // Set default selection to Latin
        binding.spinnerLanguage.setSelection(0);
        
        // Handle language selection
        binding.spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Map position to language model
                switch (position) {
                    case 1:
                        currentLanguageModel = TextRecognitionHelper.LanguageModel.CHINESE;
                        break;
                    case 2:
                        currentLanguageModel = TextRecognitionHelper.LanguageModel.DEVANAGARI;
                        break;
                    case 3:
                        currentLanguageModel = TextRecognitionHelper.LanguageModel.JAPANESE;
                        break;
                    case 4:
                        currentLanguageModel = TextRecognitionHelper.LanguageModel.KOREAN;
                        break;
                    case 0:
                    default:
                        currentLanguageModel = TextRecognitionHelper.LanguageModel.LATIN;
                        break;
                }
                
                // If we already have an image, restart recognition with new language
                if (imageUri != null || imageBitmap != null) {
                    recognizeText();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default to Latin if nothing selected
                currentLanguageModel = TextRecognitionHelper.LanguageModel.LATIN;
            }
        });
    }
    
    /**
     * Set up click listeners for UI elements
     */
    private void setupClickListeners() {
        // Recognize text button
        binding.btnRecognize.setOnClickListener(v -> {
            if (imageUri != null) {
                recognizeText();
            } else {
                Toast.makeText(this, "No image loaded", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Copy text button
        binding.btnCopy.setOnClickListener(v -> {
            String text = binding.txtRecognizedText.getText().toString();
            if (!text.isEmpty()) {
                android.content.ClipboardManager clipboard = 
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = 
                        android.content.ClipData.newPlainText("Recognized Text", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Share text button
        binding.btnShare.setOnClickListener(v -> {
            String text = binding.txtRecognizedText.getText().toString();
            if (!text.isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            } else {
                Toast.makeText(this, "No text to share", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Save button
        binding.btnSave.setOnClickListener(v -> {
            String text = binding.txtRecognizedText.getText().toString();
            if (!text.isEmpty() && imageUri != null) {
                // Launch SaveExtractedTextActivity
                Intent intent = new Intent(this, SaveExtractedTextActivity.class);
                intent.putExtra(SaveExtractedTextActivity.EXTRA_IMAGE_URI, imageUri.toString());
                intent.putExtra(SaveExtractedTextActivity.EXTRA_EXTRACTED_TEXT, text);
                // Pass the feature type to save activity
                intent.putExtra(EXTRA_FEATURE_TYPE, featureType);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No text or image to save", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadImage() {
        if (imageUri == null) {
            Toast.makeText(this, "No image to process", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Load image for display
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                // Decode image size first
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);
                
                // Calculate sample size to reduce memory usage
                int sampleSize = 1;
                if (options.outWidth > MAX_DISPLAY_WIDTH) {
                    sampleSize = Math.round((float) options.outWidth / (float) MAX_DISPLAY_WIDTH);
                }
                
                // Decode with sample size
                options = new BitmapFactory.Options();
                options.inSampleSize = sampleSize;
                
                inputStream = getContentResolver().openInputStream(imageUri);
                imageBitmap = BitmapFactory.decodeStream(inputStream, null, options);
                
                // Display image
                binding.imagePreview.setImageBitmap(imageBitmap);
                
                // Show the UI now that we have an image
                binding.layoutControls.setVisibility(View.VISIBLE);
                
                // Start recognition automatically
                recognizeText();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image", e);
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Get image URI from intent and load it
     */
    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_IMAGE_URI)) {
            String uriString = intent.getStringExtra(EXTRA_IMAGE_URI);
            if (uriString != null) {
                imageUri = Uri.parse(uriString);
                loadImage();
            } else {
                // Try to get image URI directly
                imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI);
                if (imageUri != null) {
                    loadImage();
                } else {
                    Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
                    binding.layoutControls.setVisibility(View.GONE);
                }
            }
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
            binding.layoutControls.setVisibility(View.GONE);
        }
    }
    
    private void recognizeText() {
        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.txtRecognizedText.setText("");
        binding.btnCopy.setEnabled(false);
        binding.btnShare.setEnabled(false);
        binding.btnSave.setEnabled(false);
        
        // Recognize text based on what we have
        if (imageBitmap != null) {
            textRecognitionHelper.recognizeText(imageBitmap, currentLanguageModel);
        } else if (imageUri != null) {
            textRecognitionHelper.recognizeText(imageUri, currentLanguageModel);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "No image to process", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onTextRecognized(String text) {
        runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.GONE);
            
            if (text.isEmpty()) {
                binding.txtRecognizedText.setText("No text found in image");
                Toast.makeText(this, "No text was recognized", Toast.LENGTH_SHORT).show();
            } else {
                binding.txtRecognizedText.setText(text);
                binding.btnCopy.setEnabled(true);
                binding.btnShare.setEnabled(true);
                binding.btnSave.setEnabled(true);
            }
        });
    }
    
    @Override
    public void onError(Exception e) {
        runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.txtRecognizedText.setText("Error: " + e.getMessage());
            Toast.makeText(this, "Recognition error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Text recognition error", e);
        });
    }
    
    private void launchSaveActivity() {
        Intent intent = new Intent(this, SaveExtractedTextActivity.class);
        intent.putExtra(SaveExtractedTextActivity.EXTRA_EXTRACTED_TEXT, 
                binding.txtRecognizedText.getText().toString());
        intent.putExtra(SaveExtractedTextActivity.EXTRA_IMAGE_URI, imageUri.toString());
        
        // Pass the feature type to save activity
        intent.putExtra(EXTRA_FEATURE_TYPE, featureType);
        
        startActivity(intent);
    }
} 