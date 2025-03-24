package com.quang.escan.ui.library;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.quang.escan.R;
import com.quang.escan.databinding.FragmentDocumentViewerBinding;
import com.quang.escan.model.ExtractedDocument;
import com.quang.escan.util.FileHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for viewing document details
 * Shows the saved document with its image and extracted text
 */
public class DocumentViewerFragment extends Fragment {

    private static final String TAG = "DocumentViewerFragment";
    private static final String ARG_DOCUMENT_ID = "document_id";
    
    private FragmentDocumentViewerBinding binding;
    private LibraryRepository repository;
    private ExtractedDocument document;
    private long documentId;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    
    // Available categories for documents - matching the Library fragment defaults
    private final String[] categories = {"Personal", "Work", "School", "Others"};
    
    /**
     * Create a new instance of the fragment with document ID
     */
    public static DocumentViewerFragment newInstance(long documentId) {
        DocumentViewerFragment fragment = new DocumentViewerFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_DOCUMENT_ID, documentId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get document ID from arguments
        if (getArguments() != null) {
            documentId = getArguments().getLong(ARG_DOCUMENT_ID, -1);
            if (documentId == -1) {
                documentId = getArguments().getLong("document_id", -1);
            }
        }
        
        // Initialize repository
        repository = new LibraryRepository(requireContext());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDocumentViewerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup toolbar
        setupToolbar();
        
        // Load document data
        loadDocument();
        
        // Setup button listeners
        setupClickListeners();
    }
    
    private void setupToolbar() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material); // Use system back arrow
        binding.toolbar.setNavigationOnClickListener(v -> navigateBack());
        
        // More options menu
        binding.btnMoreOptions.setOnClickListener(v -> showOptionsMenu());
    }
    
    private void loadDocument() {
        if (documentId == -1) {
            showError("Invalid document ID");
            return;
        }
        
        // Load document from repository
        document = repository.getDocumentById(documentId);
        
        if (document == null) {
            showError("Document not found");
            return;
        }
        
        // Display document info
        displayDocumentInfo();
        
        // Load and display image
        loadDocumentImage();
        
        // Display text content
        binding.textContent.setText(document.getExtractedText());
    }
    
    private void displayDocumentInfo() {
        // Set file name
        binding.textFileName.setText(document.getFileName());
        
        // Set category
        binding.textCategory.setText(document.getCategory());
        
        // Set date
        if (document.getCreationDate() != null) {
            binding.textDate.setText(dateFormat.format(document.getCreationDate()));
        } else {
            binding.textDate.setText("Unknown date");
        }
        
        // Set toolbar title to file name
        binding.toolbar.setTitle(document.getFileName());
    }
    
    private void loadDocumentImage() {
        String imagePath = document.getImagePath();
        
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                
                if (bitmap != null) {
                    binding.imagePreview.setImageBitmap(bitmap);
                    return;
                }
            }
        }
        
        // If no image or error loading, show placeholder
        binding.imagePreview.setImageResource(R.drawable.ic_verified);
    }
    
    private void setupClickListeners() {
        // Delete button
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        
        // Copy button
        binding.btnCopy.setOnClickListener(v -> copyToClipboard());
        
        // Edit button
        binding.btnEdit.setOnClickListener(v -> showEditOptions());
        
        // Share button
        binding.btnShare.setOnClickListener(v -> shareDocument());
    }
    
    /**
     * Copy text content to clipboard
     */
    private void copyToClipboard() {
        if (document == null || document.getExtractedText() == null) {
            Toast.makeText(requireContext(), "No text to copy", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) 
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Extracted Text", document.getExtractedText());
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(requireContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show edit options dialog
     */
    private void showEditOptions() {
        if (document == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Document");
        
        String[] options = {"Rename Document", "Change Category"};
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showRenameDialog();
                    break;
                case 1:
                    showChangeCategoryDialog();
                    break;
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show dialog to rename document
     */
    private void showRenameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rename Document");
        
        // Create edit text for new name
        EditText input = new EditText(requireContext());
        input.setText(document.getFileName());
        input.setSelectAllOnFocus(true);
        builder.setView(input);
        
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            
            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            document.setFileName(newName);
            updateDocument("Document renamed");
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show dialog to change document category
     */
    private void showChangeCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Category");
        
        // Create spinner for category selection
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_spinner, null);
        Spinner spinner = view.findViewById(R.id.spinner);
        
        // Create adapter for spinner with categories
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), 
                android.R.layout.simple_spinner_item,
                categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        // Set current category as selected
        int currentPos = Arrays.asList(categories).indexOf(document.getCategory());
        if (currentPos >= 0) {
            spinner.setSelection(currentPos);
        } else {
            // If current category is not in our list, add it temporarily to avoid losing data
            List<String> expandedCategories = new ArrayList<>(Arrays.asList(categories));
            expandedCategories.add(document.getCategory());
            
            adapter = new ArrayAdapter<>(
                    requireContext(), 
                    android.R.layout.simple_spinner_item,
                    expandedCategories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(expandedCategories.size() - 1);
            
            // Show notice about changing to standard category
            Toast.makeText(requireContext(), 
                    "Current category will be changed to one of the standard categories", 
                    Toast.LENGTH_SHORT).show();
        }
        
        builder.setView(view);
        
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newCategory = (String) spinner.getSelectedItem();
            
            if (!newCategory.equals(document.getCategory())) {
                document.setCategory(newCategory);
                updateDocument("Category updated to " + newCategory);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Update document in repository
     */
    private void updateDocument(String successMessage) {
        long result = repository.updateDocument(document);
        
        if (result > 0) {
            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();
            // Refresh UI
            displayDocumentInfo();
        } else {
            Toast.makeText(requireContext(), "Error updating document", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Share document content
     */
    private void shareDocument() {
        if (document == null) return;
        
        try {
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
            
            String shareDescription = hasImage ? 
                    "Sharing document with text and image..." : 
                    "Sharing document text...";
            Toast.makeText(requireContext(), shareDescription, Toast.LENGTH_SHORT).show();
            
            startActivity(Intent.createChooser(shareIntent, "Share '" + document.getFileName() + "' via"));
        } catch (Exception e) {
            // In case of errors (like FileProvider issues)
            Toast.makeText(requireContext(), 
                    "Error sharing document: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error sharing document", e);
        }
    }
    
    /**
     * Confirm document deletion
     */
    private void confirmDelete() {
        if (document == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Document");
        builder.setMessage("Are you sure you want to delete '" + document.getFileName() + "'?");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Delete from repository
            boolean deleted = repository.deleteDocument(document.getId());
            
            if (deleted) {
                // Also delete the image file if it exists
                if (document.getImagePath() != null && !document.getImagePath().isEmpty()) {
                    File imageFile = new File(document.getImagePath());
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }
                }
                
                Toast.makeText(requireContext(), "Document deleted", Toast.LENGTH_SHORT).show();
                
                // Navigate back to library
                navigateBack();
            } else {
                Toast.makeText(requireContext(), "Error deleting document", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show options menu
     */
    private void showOptionsMenu() {
        if (document == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(document.getFileName());
        
        List<String> optionsList = new ArrayList<>();
        optionsList.add("Print Document");
        optionsList.add("Add to Favorites");
        optionsList.add("View Document Info");
        
        builder.setItems(optionsList.toArray(new String[0]), (dialog, which) -> {
            switch (which) {
                case 0:
                    // Print functionality
                    Toast.makeText(requireContext(), "Print functionality coming soon", 
                            Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    // Add to favorites
                    Toast.makeText(requireContext(), "Added to favorites", 
                            Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    // View document info
                    showDocumentInfoDialog();
                    break;
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show document info dialog
     */
    private void showDocumentInfoDialog() {
        if (document == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Document Information");
        
        String textLength = document.getExtractedText() != null ? 
                String.valueOf(document.getExtractedText().length()) : "0";
        
        StringBuilder info = new StringBuilder();
        info.append("File name: ").append(document.getFileName()).append("\n\n");
        info.append("Category: ").append(document.getCategory()).append("\n\n");
        info.append("Created: ").append(dateFormat.format(document.getCreationDate())).append("\n\n");
        info.append("Text length: ").append(textLength).append(" characters").append("\n\n");
        
        if (document.getImagePath() != null && !document.getImagePath().isEmpty()) {
            File imageFile = new File(document.getImagePath());
            if (imageFile.exists()) {
                info.append("Image size: ").append(imageFile.length() / 1024).append(" KB");
            }
        }
        
        builder.setMessage(info.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    private void navigateBack() {
        Navigation.findNavController(requireView()).navigateUp();
    }
    
    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        navigateBack();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 