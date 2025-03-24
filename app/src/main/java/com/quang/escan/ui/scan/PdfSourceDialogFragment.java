package com.quang.escan.ui.scan;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.quang.escan.R;
import com.quang.escan.databinding.DialogPdfSourceBinding;

/**
 * Dialog fragment for selecting the PDF source (camera, gallery, or file)
 */
public class PdfSourceDialogFragment extends DialogFragment {

    private static final String TAG = "PdfSourceDialog";
    private static final int REQUEST_FILE_PICK = 3;
    
    private DialogPdfSourceBinding binding;
    private PdfSourceListener listener;
    
    /**
     * Interface for handling the PDF source selection
     */
    public interface PdfSourceListener {
        void onCameraSelected();
        void onGallerySelected();
        void onFileSelected();
    }
    
    /**
     * Create a new instance of the dialog
     */
    public static PdfSourceDialogFragment newInstance() {
        return new PdfSourceDialogFragment();
    }
    
    /**
     * Set the listener for PDF source selection events
     */
    public void setPdfSourceListener(PdfSourceListener listener) {
        this.listener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        binding = DialogPdfSourceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupClickListeners();
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
    
    /**
     * Setup click listeners for the dialog options
     */
    private void setupClickListeners() {
        // Camera option
        binding.btnCamera.setOnClickListener(v -> {
            Log.d(TAG, "Camera option selected");
            dismiss();
            
            if (listener != null) {
                listener.onCameraSelected();
            } else {
                // Default implementation - navigate to scan fragment
                try {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.navigation_scan);
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to scan fragment", e);
                }
            }
        });
        
        // Gallery option
        binding.btnGallery.setOnClickListener(v -> {
            Log.d(TAG, "Gallery option selected");
            dismiss();
            
            if (listener != null) {
                listener.onGallerySelected();
            } else {
                // Default implementation - open gallery
                openGallery();
            }
        });
        
        // File option
        binding.btnFile.setOnClickListener(v -> {
            Log.d(TAG, "File option selected");
            dismiss();
            
            if (listener != null) {
                listener.onFileSelected();
            } else {
                // Default implementation - open file picker
                openFilePicker();
            }
        });
    }
    
    /**
     * Open the gallery to pick an image
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            startActivityForResult(intent, REQUEST_FILE_PICK);
        } catch (Exception e) {
            Log.e(TAG, "Error opening gallery", e);
        }
    }
    
    /**
     * Open file picker to select a document
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        
        try {
            startActivityForResult(intent, REQUEST_FILE_PICK);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker", e);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 