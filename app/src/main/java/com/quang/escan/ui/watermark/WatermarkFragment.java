package com.quang.escan.ui.watermark;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.quang.escan.R;
import com.quang.escan.databinding.FragmentWatermarkBinding;
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
    private int color_options = Color.WHITE;
    private int seek_transparency = 150; // Default transparency (0-255)
    private float blurRadius = 0f; // Default blur radius (0 = no blur)
    private float textSize = 50f; // Default text size in pixels
    private Typeface selectedFont = Typeface.DEFAULT; // Default font

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

        // Set up click listeners and Spinner
        setupClickListeners();
        setupFontSpinner();
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
        binding.btnApply.setOnClickListener(v -> applyWatermark());
        binding.btnSave.setOnClickListener(v -> saveWatermarkedImage());

        // Color selection with Toast
        binding.btnColorRed.setOnClickListener(v -> {
            color_options = Color.RED;
            showToast("Selected color: Red");
            applyWatermarkIfTextExists();
        });
        binding.btnColorBlue.setOnClickListener(v -> {
            color_options = Color.BLUE;
            showToast("Selected color: Blue");
            applyWatermarkIfTextExists();
        });
        binding.btnColorBlack.setOnClickListener(v -> {
            color_options = Color.BLACK;
            showToast("Selected color: Black");
            applyWatermarkIfTextExists();
        });
        binding.btnColorWhite.setOnClickListener(v -> {
            color_options = Color.WHITE;
            showToast("Selected color: White");
            applyWatermarkIfTextExists();
        });
        binding.btnColorGreen.setOnClickListener(v -> {
            color_options = Color.GREEN;
            showToast("Selected color: Green");
            applyWatermarkIfTextExists();
        });

        // Transparency with Toast
        binding.seekTransparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seek_transparency = progress;
                showToast("Transparency: " + progress + "/255");
                applyWatermarkIfTextExists();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Blur effect with Toast
        binding.seekBlur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                blurRadius = progress;
                showToast("Blur Radius: " + progress + "/20");
                applyWatermarkIfTextExists();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Text size with Toast
        binding.seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSize = 20f + (progress * 4f); // Minimum 20px, maximum 100px
                showToast("Text Size: " + String.format("%.0f", textSize) + "px");
                applyWatermarkIfTextExists();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Real-time text input update
        binding.editWatermarkText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                applyWatermarkIfTextExists();
            }
        });

        // Radio button change listener
        binding.radioGroupStyle.setOnCheckedChangeListener((group, checkedId) -> applyWatermarkIfTextExists());
    }

    private void setupFontSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.font_families,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFontStyle.setAdapter(adapter);

        binding.spinnerFontStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // Default
                        selectedFont = Typeface.DEFAULT;
                        break;
                    case 1: // Serif
                        selectedFont = Typeface.SERIF;
                        break;
                    case 2: // Sans-Serif
                        selectedFont = Typeface.SANS_SERIF;
                        break;
                    case 3: // Monospace
                        selectedFont = Typeface.MONOSPACE;
                        break;
                    case 4: // Cursive (not directly supported, fallback to default bold)
                        selectedFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
                        break;
                }
                applyWatermarkIfTextExists();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedFont = Typeface.DEFAULT;
            }
        });
    }

    private void applyWatermarkIfTextExists() {
        String watermarkText = binding.editWatermarkText.getText().toString().trim();
        if (!watermarkText.isEmpty()) {
            applyWatermark();
        }
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

            if (selectedId == R.id.radio_diagonal) {
                watermarkedBitmap = WatermarkUtils.addDiagonalTextWatermark(
                        originalBitmap, watermarkText, 45, color_options, seek_transparency, blurRadius, selectedFont, textSize);
            } else if (selectedId == R.id.radio_tiled) {
                watermarkedBitmap = WatermarkUtils.addTiledTextWatermark(
                        originalBitmap, watermarkText, 150, 150, color_options, seek_transparency, blurRadius, selectedFont, textSize);
            } else {
                watermarkedBitmap = WatermarkUtils.addHorizontalTextWatermark(
                        originalBitmap, watermarkText, color_options, seek_transparency, blurRadius, selectedFont, textSize);
            }

            if (watermarkedBitmap != null) {
                binding.imagePreview.setImageBitmap(watermarkedBitmap);
                binding.btnSave.setEnabled(true);
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
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String newFileName = "WATERMARKED_" + timeStamp + ".jpg";

            File storageDir = new File(requireContext().getExternalFilesDir(null), "EScan/Images");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            File outputFile = new File(storageDir, newFileName);

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, newFileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EScan");

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream stream = requireContext().getContentResolver().openOutputStream(uri)) {
                    watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    stream.flush();
                }
            }

            showToast("Image saved: " + outputFile.getAbsolutePath());
            navigateUp();

        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
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