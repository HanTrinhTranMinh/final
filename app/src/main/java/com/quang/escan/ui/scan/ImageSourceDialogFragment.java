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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.quang.escan.R;
import com.quang.escan.databinding.DialogImageSourceBinding;

/**
 * Dialog fragment for selecting the image source (camera or gallery)
 */
public class ImageSourceDialogFragment extends DialogFragment {

    private static final String TAG = "ImageSourceDialog";
    private static final int REQUEST_IMAGE_PICK = 2;
    
    private DialogImageSourceBinding binding;
    private ImageSourceListener listener;
    
    /**
     * Interface for handling the image source selection
     */
    public interface ImageSourceListener {
        void onCameraSelected();
        void onGallerySelected();
    }
    
    /**
     * Create a new instance of the dialog
     */
    public static ImageSourceDialogFragment newInstance() {
        return new ImageSourceDialogFragment();
    }
    
    /**
     * Set the listener for image source selection events
     */
    public void setImageSourceListener(ImageSourceListener listener) {
        this.listener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        binding = DialogImageSourceBinding.inflate(inflater, container, false);
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
    }
    
    /**
     * Open the gallery to pick an image
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        } catch (Exception e) {
            Log.e(TAG, "Error opening gallery", e);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 