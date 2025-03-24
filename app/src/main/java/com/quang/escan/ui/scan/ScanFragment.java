package com.quang.escan.ui.scan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.quang.escan.R;
import com.quang.escan.databinding.FragmentScanBinding;
import com.quang.escan.ui.scan.ImageSourceDialogFragment;
import com.quang.escan.util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment for the document scanning functionality
 * Handles camera preview, photo capture, and scan options
 */
public class ScanFragment extends Fragment {

    private static final String TAG = "ScanFragment";
    private FragmentScanBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private boolean flashEnabled = false;
    private boolean forTextRecognition = false;
    private boolean forQrScan = false;
    private int featureType = -1;
    private String currentPhotoPath;
    private BarcodeScanner barcodeScanner;
    private ImageAnalysis imageAnalysis;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating scan fragment view");
        binding = FragmentScanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Setting up scan fragment");

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Set up UI interactions
        setupClickListeners();

        // Request camera permissions and start camera
        if (allPermissionsGranted()) {
            Log.d(TAG, "Permissions already granted, starting camera");
            startCamera();
        } else {
            Log.d(TAG, "Requesting camera permissions");
            ActivityCompat.requestPermissions(
                    requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Extract arguments
        if (getArguments() != null) {
            forTextRecognition = getArguments().getBoolean("for_text_recognition", false);
            forQrScan = getArguments().getBoolean("for_qr_scan", false);
            featureType = getArguments().getInt("feature_type", -1);
            Log.d(TAG, "Arguments: forTextRecognition=" + forTextRecognition + 
                       ", forQrScan=" + forQrScan +
                       ", featureType=" + featureType);
        }
        
        // Initialize barcode scanner if needed
        if (forQrScan) {
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build();
            barcodeScanner = BarcodeScanning.getClient(options);
        }
    }

    /**
     * Set up all UI interaction listeners
     */
    private void setupClickListeners() {
        try {
            Log.d(TAG, "Setting up click listeners");
            
            // Capture button
            binding.btnCapture.setOnClickListener(v -> {
                Log.d(TAG, "Capture button clicked");
                takePhoto();
            });

            // Back navigation
            binding.toolbar.setNavigationOnClickListener(v -> {
                Log.d(TAG, "Navigation back clicked");
                Navigation.findNavController(requireView()).navigateUp();
            });
            
            // Flash toggle
            binding.btnFlash.setOnClickListener(v -> {
                Log.d(TAG, "Flash button clicked");
                flashEnabled = !flashEnabled;
                Toast.makeText(requireContext(), 
                        flashEnabled ? "Flash enabled" : "Flash disabled", 
                        Toast.LENGTH_SHORT).show();

                // Flash implementation would be here
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    /**
     * Check if this fragment was launched for text recognition
     */
    private boolean isForTextRecognition() {
        Bundle args = getArguments();
        return args != null && args.getBoolean("for_text_recognition", false);
    }

    /**
     * Check if this fragment was launched for ink/handwriting recognition
     */
    private boolean isForInkRecognition() {
        Bundle args = getArguments();
        return args != null && args.getBoolean("for_ink_recognition", false);
    }

    /**
     * Capture a photo using CameraX
     */
    private void takePhoto() {
        File photoFile = createImageFile();
        currentPhotoPath = photoFile.getAbsolutePath();
        
        try {
            // Get URI for the created file
            Uri photoURI = FileHelper.getFileProviderUri(requireContext(), photoFile);
            
            if (photoURI == null) {
                Toast.makeText(requireContext(), "Failed to create file for photo", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Take the picture
            imageCapture.takePicture(
                    new ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                    ContextCompat.getMainExecutor(requireContext()),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            Log.d(TAG, "Image saved successfully: " + currentPhotoPath);
                            
                            if (forQrScan) {
                                // If in QR scan mode, process the image directly for QR codes
                                processImageFileForQrCode(photoFile);
                            } else if (getArguments() != null && getArguments().getBoolean("for_watermark", false)) {
                                // If for watermark, navigate directly to watermark fragment
                                Bundle args = new Bundle();
                                args.putString("imagePath", currentPhotoPath);
                                
                                // Navigate to watermark screen
                                Navigation.findNavController(requireView()).navigate(
                                        R.id.navigation_watermark, args);
                            } else {
                                // For regular document scanning, navigate to image edit screen
                                Bundle args = new Bundle();
                                args.putString("imagePath", currentPhotoPath);
                                args.putBoolean("for_text_recognition", forTextRecognition);
                                args.putInt("feature_type", featureType);
                                
                                // Navigate to image edit screen
                                Navigation.findNavController(requireView()).navigate(R.id.action_scan_to_image_edit, args);
                            }
                        }
                        
                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Error capturing image", exception);
                            Toast.makeText(requireContext(), 
                                    "Error taking photo: " + exception.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error taking photo", e);
            Toast.makeText(requireContext(), 
                    "Error taking photo: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Launch TextRecognitionActivity with the captured image
     */
    private void launchTextRecognition(File photoFile) {
        if (getContext() != null) {
            try {
                android.net.Uri imageUri = android.net.Uri.fromFile(photoFile);
                Intent intent = new Intent(requireContext(), com.quang.escan.ui.ocr.TextRecognitionActivity.class);
                intent.putExtra(com.quang.escan.ui.ocr.TextRecognitionActivity.EXTRA_IMAGE_URI, imageUri.toString());
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching text recognition", e);
                showToast("Error launching text recognition: " + e.getMessage());
            }
        }
    }

    /**
     * Initialize and start the camera
     */
    private void startCamera() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "Context is null, cannot start camera");
            return;
        }
        
        try {
            // Get the camera provider
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                    ProcessCameraProvider.getInstance(context);

            cameraProviderFuture.addListener(() -> {
                try {
                    // Check if fragment is still attached
                    if (!isAdded() || getContext() == null) {
                        Log.d(TAG, "Fragment not attached, aborting camera start");
                        return;
                    }
                    
                    // Get camera provider
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    // Create preview use case
                    Preview preview = new Preview.Builder().build();
                    
                    if (binding.previewView == null) {
                        Log.e(TAG, "Preview view is null");
                        return;
                    }
                    
                    preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                    // Create image capture use case
                    imageCapture = new ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build();

                    // Select back camera
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    // Unbind any existing use cases
                    cameraProvider.unbindAll();

                    // Set up QR scanning if needed
                    if (forQrScan) {
                        // Set up image analysis use case for QR scanning
                        imageAnalysis = new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();
                        
                        imageAnalysis.setAnalyzer(cameraExecutor, this::processImageForQrCode);
                        
                        // Bind use cases to camera
                        cameraProvider.bindToLifecycle(
                                (LifecycleOwner) this, cameraSelector, preview, imageCapture, imageAnalysis);
                        
                        // Change UI for QR scanning
                        binding.btnCapture.setVisibility(View.VISIBLE);
                        binding.toolbar.setTitle("Scan QR Code");
                    } else {
                        // Bind use cases to camera without analysis
                        cameraProvider.bindToLifecycle(
                                (LifecycleOwner) this, cameraSelector, preview, imageCapture);
                    }
                    
                    Log.d(TAG, "Camera successfully initialized");

                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Error starting camera", e);
                    showToast("Error starting camera: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Camera error", e);
                    showToast("Camera error: " + e.getMessage());
                }
            }, ContextCompat.getMainExecutor(context));
        } catch (Exception e) {
            Log.e(TAG, "Error accessing camera", e);
            showToast("Error accessing camera: " + e.getMessage());
        }
    }

    /**
     * Check if all required permissions are granted
     */
    private boolean allPermissionsGranted() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "Context is null, cannot check permissions");
            return false;
        }
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted, starting camera");
                startCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
                showToast("Camera permission is required for scanning documents");
            }
        }
    }
    
    /**
     * Helper method for showing toasts only when fragment is attached
     */
    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleaning up resources");
        cameraExecutor.shutdown();
        binding = null;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Scan fragment resumed");
        if (allPermissionsGranted() && imageCapture == null) {
            Log.d(TAG, "Starting camera on resume");
            startCamera();
        }
    }

    /**
     * Create a temporary file to store the image
     */
    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "SCAN_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(null);
        
        try {
            File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
            );
            return image;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create image file", e);
            // If file creation fails, use a fallback approach
            return new File(storageDir, imageFileName + ".jpg");
        }
    }

    /**
     * Process image frame for QR code detection
     */
    private void processImageForQrCode(ImageProxy imageProxy) {
        if (!isAdded() || getContext() == null) {
            imageProxy.close();
            return;
        }
        
        @SuppressLint("UnsafeOptInUsageError")
        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes.size() > 0) {
                        Barcode barcode = barcodes.get(0);
                        String qrValue = barcode.getRawValue();
                        Log.d(TAG, "QR code detected: " + qrValue);
                        
                        // Stop camera analysis to prevent multiple detections
                        if (imageAnalysis != null) {
                            imageAnalysis.clearAnalyzer();
                        }
                        
                        // Launch QR result activity
                        launchQrResultActivity(qrValue);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error detecting QR code", e);
                })
                .addOnCompleteListener(task -> {
                    imageProxy.close();
                });
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
                Navigation.findNavController(requireView()).navigateUp();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching QR result activity", e);
            showToast("Error: " + e.getMessage());
        }
    }

    /**
     * Process a saved image file for QR code detection
     */
    private void processImageFileForQrCode(File imageFile) {
        try {
            InputImage inputImage = InputImage.fromFilePath(requireContext(), Uri.fromFile(imageFile));
            
            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.size() > 0) {
                            Barcode barcode = barcodes.get(0);
                            String qrValue = barcode.getRawValue();
                            Log.d(TAG, "QR code detected in image: " + qrValue);
                            
                            // Launch QR result activity
                            launchQrResultActivity(qrValue);
                        } else {
                            // No QR code found in the image
                            Toast.makeText(requireContext(), 
                                    "No QR code found in the image", 
                                    Toast.LENGTH_SHORT).show();
                            // Navigate back
                            Navigation.findNavController(requireView()).navigateUp();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error processing image for QR code", e);
                        Toast.makeText(requireContext(), 
                                "Error processing image: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        // Navigate back
                        Navigation.findNavController(requireView()).navigateUp();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error creating input image", e);
            Toast.makeText(requireContext(), 
                    "Error processing image: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            // Navigate back
            Navigation.findNavController(requireView()).navigateUp();
        }
    }
}