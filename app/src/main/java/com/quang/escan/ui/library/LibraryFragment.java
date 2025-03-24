package com.quang.escan.ui.library;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.quang.escan.R;
import com.quang.escan.databinding.FragmentLibraryBinding;
import com.quang.escan.model.ExtractedDocument;

import java.util.List;

/**
 * Fragment for displaying and managing scanned documents
 * Provides filtering and organization of documents by category
 */
public class LibraryFragment extends Fragment implements DocumentAdapter.DocumentClickListener {

    private static final String TAG = "LibraryFragment";
    private FragmentLibraryBinding binding;
    private NavController navController;
    private LibraryRepository repository;
    private DocumentAdapter adapter;
    private String[] categories = {"Personal", "Work", "School", "Others"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating library fragment view");
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Setting up library fragment");
        
        navController = Navigation.findNavController(view);
        repository = new LibraryRepository(requireContext());
        
        setupTabLayout();
        setupRecyclerView();
        setupClickListeners();
        
        // Check if we need to select a specific category
        checkForCategorySelection();
    }
    
    /**
     * Check if the activity was started with a specific category to select
     */
    private void checkForCategorySelection() {
        if (getActivity() != null && getActivity().getIntent() != null) {
            String selectedCategory = getActivity().getIntent().getStringExtra("selected_category");
            if (selectedCategory != null && !selectedCategory.isEmpty()) {
                // Find the appropriate tab and select it
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equals(selectedCategory)) {
                        binding.tabLayout.getTabAt(i).select();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Set up tab layout for document filtering by category
     */
    private void setupTabLayout() {
        Log.d(TAG, "Setting up tab layout");
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab selected: " + tab.getPosition());
                // Filter documents based on selected category
                filterDocumentsByCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    /**
     * Filter documents based on the selected category
     * @param tabPosition The position of the selected tab
     */
    private void filterDocumentsByCategory(int tabPosition) {
        Log.d(TAG, "Filtering documents by category: " + categories[tabPosition]);
        
        // Get documents from repository
        List<ExtractedDocument> documents = repository.getDocumentsByCategory(categories[tabPosition]);
        
        // Update adapter
        adapter.setDocuments(documents);
        
        // Show empty state if needed
        checkIfEmpty(documents.isEmpty());
    }

    /**
     * Set up the recycler view for displaying documents
     */
    private void setupRecyclerView() {
        Log.d(TAG, "Setting up recycler view");
        
        // Initialize adapter
        adapter = new DocumentAdapter(requireContext(), this);
        
        // Set up recycler view
        binding.recyclerDocuments.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerDocuments.setAdapter(adapter);
        
        // Load initial data (Personal category)
        List<ExtractedDocument> documents = repository.getDocumentsByCategory("Personal");
        adapter.setDocuments(documents);
        
        // Check if empty for initial state
        checkIfEmpty(documents.isEmpty());
    }

    /**
     * Show empty state or document list based on whether documents exist
     * @param isEmpty Whether there are no documents to display
     */
    private void checkIfEmpty(boolean isEmpty) {
        Log.d(TAG, "Checking if document list is empty: " + isEmpty);
        if (isEmpty) {
            binding.textEmpty.setVisibility(View.VISIBLE);
            binding.recyclerDocuments.setVisibility(View.GONE);
        } else {
            binding.textEmpty.setVisibility(View.GONE);
            binding.recyclerDocuments.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Set up click listeners for UI interactions
     */
    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
    }
    
    /**
     * Handle document click
     */
    @Override
    public void onDocumentClick(ExtractedDocument document) {
        // Navigate to document viewer fragment
        Log.d(TAG, "Opening document: " + document.getFileName());
        
        // Create bundle with document ID
        Bundle args = new Bundle();
        args.putLong("document_id", document.getId());
        
        // Navigate to document viewer
        navController.navigate(R.id.navigation_document_viewer, args);
    }
    
    /**
     * Handle document long click
     */
    @Override
    public boolean onDocumentLongClick(ExtractedDocument document) {
        // Show options dialog (delete, etc.)
        showDocumentOptionsDialog(document);
        return true;
    }
    
    /**
     * Show dialog with document options
     */
    private void showDocumentOptionsDialog(ExtractedDocument document) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(document.getFileName());
        
        String[] options = {"View", "Share", "Delete"};
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // View document
                    onDocumentClick(document);
                    break;
                case 1:
                    // Share document
                    shareDocument(document);
                    break;
                case 2:
                    // Delete document
                    confirmDocumentDeletion(document);
                    break;
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Share document content and image
     */
    private void shareDocument(ExtractedDocument document) {
        // Create document viewer fragment to handle sharing
        DocumentViewerFragment fragment = DocumentViewerFragment.newInstance(document.getId());
        
        // We can't directly call methods on the fragment without attaching it,
        // so we'll navigate to it to view and share from there
        onDocumentClick(document);
        
        // Show toast instruction
        Toast.makeText(requireContext(), "Opening document for sharing...", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Confirm document deletion
     */
    private void confirmDocumentDeletion(ExtractedDocument document) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Document");
        builder.setMessage("Are you sure you want to delete '" + document.getFileName() + "'?");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Delete from repository
            boolean deleted = repository.deleteDocument(document.getId());
            
            if (deleted) {
                Toast.makeText(requireContext(), "Document deleted", Toast.LENGTH_SHORT).show();
                
                // Refresh the current category view
                int selectedTab = binding.tabLayout.getSelectedTabPosition();
                if (selectedTab < categories.length) {
                    filterDocumentsByCategory(selectedTab);
                }
            } else {
                Toast.makeText(requireContext(), "Error deleting document", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleaning up library fragment");
        binding = null;
    }
} 