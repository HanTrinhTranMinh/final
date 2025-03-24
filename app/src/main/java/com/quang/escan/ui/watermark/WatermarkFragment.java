package com.quang.escan.ui.watermark;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.quang.escan.R;
import com.quang.escan.databinding.FragmentWatermarkBinding;
import com.quang.escan.model.ExtractedDocument;
import com.quang.escan.ui.library.LibraryRepository;
import com.quang.escan.util.FileHelper;
import com.quang.escan.util.WatermarkUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WatermarkFragment extends Fragment {
    private static final String TAG = "WatermarkFragment";
    private static final String ARG_IMAGE_PATH = "imagePath";

    private FragmentWatermarkBinding binding;
    private NavController navController;
    private String imagePath;
    private Bitmap originalBitmap;
    private Bitmap watermarkedBitmap;
    private int selectedColor = Color.WHITE;
    private int transparency = 150; // Default transparency (0-255)

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
            Log.d(TAG, "Received image path: " + imagePath);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        binding = FragmentWatermarkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        navController = Navigation.findNavController(view);
        
        // Set up toolbar
        binding.toolbar.setNavigationOnClickListener(v -> navigateUp());
        
        // Load and display the image
        if (imagePath != null) {
            loadImage();
        } else {
            showToast("No image provided");
            navigateUp();
        }
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void loadImage() {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: " + imagePath);
                showToast("Error: Image file not found");
                navigateUp();
                return;
            }
            
            // Load bitmap from file
            originalBitmap = BitmapFactory.decodeFile(imagePath);
            
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from: " + imagePath);
                showToast("Error: Could not load image");
                navigateUp();
                return;
            }

            binding.imagePreview.setImageBitmap(originalBitmap);
            Log.d(TAG, "Image loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            showToast("Error loading image: " + e.getMessage());
            navigateUp();
        }
    }
    
    private void setupClickListeners() {
        // Apply watermark button
        binding.btnApply.setOnClickListener(v -> applyWatermark());
        // Save button
        binding.btnSave.setOnClickListener(v -> saveWatermarkedImage());
    }
    
    private void applyWatermark() {
        if (originalBitmap == null) {
            showToast("No image to watermark");
            return;
        }

        String watermarkText = binding.editWatermarkText.getText().toString().trim();
        if (watermarkText.isEmpty()) {
            showToast("Please enter watermark text");
            return;
        }

        try {
            int selectedId = binding.radioGroupStyle.getCheckedRadioButtonId();

            if (selectedId == R.id.radio_horizontal) {  // ✅ Đúng ID từ XML
                watermarkedBitmap = WatermarkUtils.addHorizontalTextWatermark(originalBitmap, watermarkText);
            } else if (selectedId == R.id.radio_vertical) {
                watermarkedBitmap = WatermarkUtils.addVerticalTextWatermark(originalBitmap, watermarkText);
            } else if (selectedId == R.id.radio_diagonal) {
                watermarkedBitmap = WatermarkUtils.addDiagonalTextWatermark(originalBitmap, watermarkText, 45);
            } else if (selectedId == R.id.radio_tiled) {
                watermarkedBitmap = WatermarkUtils.addTiledTextWatermark(originalBitmap, watermarkText, 150, 150);
            } else {
                // Default to horizontal
                watermarkedBitmap = WatermarkUtils.addHorizontalTextWatermark(originalBitmap, watermarkText);
            }

            if (watermarkedBitmap != null) {
                binding.imagePreview.setImageBitmap(watermarkedBitmap);
                binding.btnSave.setEnabled(true);
                showToast("Watermark applied");
            } else {
                showToast("Failed to apply watermark");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying watermark", e);
            showToast("Error applying watermark: " + e.getMessage());
        }
    }

    private void saveWatermarkedImage() {
        if (watermarkedBitmap == null) {
            showToast("Apply watermark first");
            return;
        }
        
        try {
            // Create a new file name with timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String newFileName = "WATERMARKED_" + timeStamp + ".jpg";
            
            // Get directory for saved images
            File storageDir = new File(requireContext().getExternalFilesDir(null), "EScan/Images");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            File outputFile = new File(storageDir, newFileName);
            
            // Save the watermarked bitmap
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
            }
            
            showToast("Image saved: " + outputFile.getAbsolutePath());
            
            // Return to the home screen
            if (getActivity() != null) {
                navigateUp();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving watermarked image", e);
            showToast("Error saving image: " + e.getMessage());
        }
    }
    
    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void navigateUp() {
        if (navController != null) {
            navController.navigateUp();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up bitmaps to avoid memory leaks
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
            originalBitmap = null;
        }
        
        if (watermarkedBitmap != null && !watermarkedBitmap.isRecycled()) {
            watermarkedBitmap.recycle();
            watermarkedBitmap = null;
        }
        
        binding = null;
    }
} 