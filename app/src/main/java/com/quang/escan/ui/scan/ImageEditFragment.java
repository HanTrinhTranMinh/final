package com.quang.escan.ui.scan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.quang.escan.R;
import com.quang.escan.databinding.FragmentImageEditBinding;

import java.io.File;
import java.io.InputStream;

/**
 * Fragment for editing a captured image
 * Provides features like rotation, cropping, and navigation
 */
public class ImageEditFragment extends Fragment {

    private static final String TAG = "ImageEditFragment";
    private static final String ARG_IMAGE_PATH = "imagePath";
    private static final String ARG_FOR_TEXT_RECOGNITION = "for_text_recognition";
    private static final String ARG_FOR_QR_SCAN = "for_qr_scan";
    private static final String ARG_FEATURE_TYPE = "feature_type";

    private FragmentImageEditBinding binding;
    private NavController navController;
    private String imagePath;
    private Bitmap originalBitmap;
    private Bitmap currentBitmap;
    private int rotationDegrees = 0;
    private boolean isForTextRecognition = false;
    private boolean isForQrScan = false;
    private int featureType = -1;

    // Activity result launcher for cropping
    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri croppedUri = result.getData().getData();
                    try {
                        InputStream stream = requireContext().getContentResolver().openInputStream(croppedUri);
                        currentBitmap = BitmapFactory.decodeStream(stream);
                        binding.imagePreview.setImageBitmap(currentBitmap);
                        stream.close();
                        Log.d(TAG, "Image cropped successfully");

                        // Sau khi cắt ảnh, nếu là chế độ nhận diện text, tiến hành nhận diện ngay
                        if (isForTextRecognition) {
                            launchTextRecognitionAfterCrop();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading cropped image", e);
                        showToast("Error loading cropped image: " + e.getMessage());
                    }
                } else {
                    Log.d(TAG, "Crop operation cancelled or failed");
                }
            });


    /**
     * Create a new instance of the fragment with image path as argument
     */
    public static ImageEditFragment newInstance(String imagePath, boolean forTextRecognition, int featureType) {
        ImageEditFragment fragment = new ImageEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        args.putBoolean(ARG_FOR_TEXT_RECOGNITION, forTextRecognition);
        args.putInt(ARG_FEATURE_TYPE, featureType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
            isForTextRecognition = getArguments().getBoolean(ARG_FOR_TEXT_RECOGNITION, false);
            isForQrScan = getArguments().getBoolean(ARG_FOR_QR_SCAN, false);
            featureType = getArguments().getInt(ARG_FEATURE_TYPE, -1);
            Log.d(TAG, "Received image path: " + imagePath +
                       ", forTextRecognition: " + isForTextRecognition +
                       ", forQrScan: " + isForQrScan +
                       ", featureType: " + featureType);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating image edit fragment view");
        binding = FragmentImageEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Setting up image edit fragment");

        navController = Navigation.findNavController(view);

        // Set up toolbar
        binding.toolbar.setNavigationOnClickListener(v -> navigateUp());

        // Get arguments
        Bundle args = getArguments();
        if (args != null) {
            imagePath = args.getString("imagePath");
            isForTextRecognition = args.getBoolean("for_text_recognition", false);
            isForQrScan = args.getBoolean("for_qr_scan", false);
            featureType = args.getInt("feature_type", -1);
        }

        // Load and display the image
        if (imagePath != null) {
            loadImage();

            // If this is for QR scanning, scan the image immediately
            if (isForQrScan && originalBitmap != null) {
                scanQrCode(originalBitmap);
            }
        } else {
            showToast("No image provided");
            navigateUp();
        }

        // Set up click listeners
        setupClickListeners();
    }

    /**
     * Load image from path
     */
    private void loadImage() {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.e(TAG, "No image path provided");
            showToast("Error: No image to edit");
            navController.navigateUp();
            return;
        }

        try {
            // First try to parse as a URI
            Uri imageUri = Uri.parse(imagePath);

            // Check if it's a content URI
            if (imageUri.getScheme() != null && (imageUri.getScheme().equals("content") ||
                                                 imageUri.getScheme().equals("file"))) {
                Log.d(TAG, "Loading image from URI: " + imageUri);

                // Load bitmap from content resolver
                try (InputStream stream = requireContext().getContentResolver().openInputStream(imageUri)) {
                    if (stream != null) {
                        originalBitmap = BitmapFactory.decodeStream(stream);
                    }
                }
            } else {
                // Treat as a file path
                File imageFile = new File(imagePath);
                if (!imageFile.exists()) {
                    Log.e(TAG, "Image file does not exist: " + imagePath);
                    showToast("Error: Image file not found");
                    navController.navigateUp();
                    return;
                }

                // Load bitmap from file
                originalBitmap = BitmapFactory.decodeFile(imagePath);
            }

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from: " + imagePath);
                showToast("Error: Could not load image");
                navController.navigateUp();
                return;
            }

            currentBitmap = originalBitmap;
            binding.imagePreview.setImageBitmap(currentBitmap);
            Log.d(TAG, "Image loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            showToast("Error loading image: " + e.getMessage());
            navController.navigateUp();
        }
    }

    /**
     * Setup click listeners for UI elements
     */
    private void setupClickListeners() {
        // Rotate image button
        binding.btnRotate.setOnClickListener(v -> rotateImage());

        // Crop image button
        binding.btnCrop.setOnClickListener(v -> cropImage());

        // Set click listener for the back button
        binding.btnBack.setOnClickListener(v -> navigateUp());

        // Set click listener for the next button
        binding.btnNext.setOnClickListener(v -> {
            if (isForTextRecognition) {
                launchTextRecognitionAfterCrop(); // Sử dụng ảnh đã cắt
            } else if (isForQrScan) {
                if (currentBitmap != null) {
                    scanQrCode(currentBitmap);
                } else {
                    showToast("Failed to process image");
                }
            } else {
                navigateUp();
            }
        });
    }

    /**
     * Rotate the image by 90 degrees clockwise
     */
    private void rotateImage() {
        if (currentBitmap == null) {
            Log.e(TAG, "Cannot rotate null bitmap");
            return;
        }

        try {
            rotationDegrees = (rotationDegrees + 90) % 360;
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            currentBitmap = Bitmap.createBitmap(
                    currentBitmap,
                    0,
                    0,
                    currentBitmap.getWidth(),
                    currentBitmap.getHeight(),
                    matrix,
                    true);

            binding.imagePreview.setImageBitmap(currentBitmap);
            Log.d(TAG, "Image rotated to " + rotationDegrees + " degrees");
        } catch (Exception e) {
            Log.e(TAG, "Error rotating image", e);
            showToast("Error rotating image: " + e.getMessage());
        }
    }

    /**
     * Crop the image
     */
    private void cropImage() {
        if (currentBitmap == null) {
            Log.e(TAG, "Cannot crop null bitmap");
            showToast("No image to crop");
            return;
        }

        try {
            // Lưu bitmap hiện tại thành file tạm thời
            File tempFile = new File(requireContext().getCacheDir(), "temp_image.jpg");
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, new java.io.FileOutputStream(tempFile));

            // Sử dụng FileProvider để tạo URI an toàn
            Uri imageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.quang.escan.fileprovider", // Thay bằng authority của bạn
                    tempFile
            );

            // Tạo Intent crop
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(imageUri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", 500);
            cropIntent.putExtra("outputY", 500);
            cropIntent.putExtra("return-data", false);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            cropImageLauncher.launch(cropIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error initiating crop", e);
            showToast("Error initiating crop: " + e.getMessage());
        }
    }



    /**
     * Launch the TextRecognitionActivity to process the image
     */
    private void launchTextRecognition() {
        try {
            Uri imageUri;
            if (imagePath.startsWith("content:") || imagePath.startsWith("file:")) {
                // Already a URI string
                imageUri = Uri.parse(imagePath);
            } else {
                // Convert file path to URI
                imageUri = Uri.fromFile(new File(imagePath));
            }

            Intent intent = new Intent(requireContext(), com.quang.escan.ui.ocr.TextRecognitionActivity.class);
            intent.putExtra(com.quang.escan.ui.ocr.TextRecognitionActivity.EXTRA_IMAGE_URI, imageUri.toString());
            // Pass feature type to distinguish between text and handwriting recognition
            intent.putExtra("feature_type", featureType);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching recognition", e);
            showToast("Error launching recognition: " + e.getMessage());
        }
    }

    /**
     * Launch text recognition with the cropped bitmap
     */
    private void launchTextRecognitionAfterCrop() {
        if (currentBitmap == null) {
            Log.e(TAG, "No cropped bitmap available for text recognition");
            showToast("No image available for recognition");
            return;
        }

        try {
            // Lưu currentBitmap thành file tạm thời
            File tempFile = new File(requireContext().getCacheDir(), "cropped_image.jpg");
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, new java.io.FileOutputStream(tempFile));

            // Tạo Uri từ file tạm thời
            Uri croppedImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.quang.escan.fileprovider", // Đảm bảo authority khớp với manifest
                    tempFile
            );

            // Khởi chạy TextRecognitionActivity với Uri của ảnh đã cắt
            Intent intent = new Intent(requireContext(), com.quang.escan.ui.ocr.TextRecognitionActivity.class);
            intent.putExtra(com.quang.escan.ui.ocr.TextRecognitionActivity.EXTRA_IMAGE_URI, croppedImageUri.toString());
            intent.putExtra("feature_type", featureType);
            startActivity(intent);

            Log.d(TAG, "Launched text recognition with cropped image");
        } catch (Exception e) {
            Log.e(TAG, "Error launching text recognition with cropped image", e);
            showToast("Error launching recognition: " + e.getMessage());
        }
    }

    /**
     * Scan QR code from bitmap
     */
    private void scanQrCode(Bitmap bitmap) {
        if (bitmap == null) {
            showToast("Failed to load image for QR scanning");
            return;
        }

        // Show loading state
        binding.btnNext.setEnabled(false);
        showToast("Scanning for QR codes...");

        try {
            // Create barcode scanner
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build();
            BarcodeScanner scanner = BarcodeScanning.getClient(options);

            // Create input image from bitmap
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            // Process the image
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.size() > 0) {
                            Barcode barcode = barcodes.get(0);
                            String qrValue = barcode.getRawValue();
                            Log.d(TAG, "QR code detected: " + qrValue);

                            // Launch QR result activity
                            launchQrResultActivity(qrValue);
                        } else {
                            showToast("No QR code found in the image");
                            binding.btnNext.setEnabled(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error detecting QR code", e);
                        showToast("Error scanning QR code: " + e.getMessage());
                        binding.btnNext.setEnabled(true);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up QR scanner", e);
            showToast("Error setting up QR scanner: " + e.getMessage());
            binding.btnNext.setEnabled(true);
        }
    }

    /**
     * Launch QR result activity with the detected QR code value
     */
    private void launchQrResultActivity(String qrValue) {
        try {
            if (getActivity() != null) {
                Intent intent = new Intent(requireContext(), com.quang.escan.ui.qr.QrResultActivity.class);
                intent.putExtra(com.quang.escan.ui.qr.QrResultActivity.EXTRA_QR_VALUE, qrValue);
                startActivity(intent);

                // Go back to home after launching the result activity
                navigateUp();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching QR result activity", e);
            showToast("Error: " + e.getMessage());
        }
    }

    /**
     * Show a toast message
     */
    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigate back to the previous screen
     */
    private void navigateUp() {
        if (navController != null) {
            navController.navigateUp();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleaning up resources");

        // Clean up bitmaps to avoid memory leaks
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
            originalBitmap = null;
        }

        if (currentBitmap != null && !currentBitmap.isRecycled() && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
            currentBitmap = null;
        }

        binding = null;
    }
}