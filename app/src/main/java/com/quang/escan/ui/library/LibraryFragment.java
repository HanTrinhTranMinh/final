package com.quang.escan.ui.library;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibraryFragment extends Fragment implements DocumentAdapter.DocumentClickListener {

    private static final String TAG = "LibraryFragment";
    private FragmentLibraryBinding binding;
    private NavController navController;
    private LibraryRepository repository;
    private DocumentAdapter adapter;
    private String[] categories = {"Personal", "Work", "School", "Others"};
    private int currentSortMode = 0; // 0: default, 1: alphabet, 2: date

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
        setupSortButton();

        checkForCategorySelection();
    }

    private void setupSortButton() {
        binding.sortButton.setOnClickListener(v -> showSortPopupMenu(v));
    }

    private void showSortPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.sort_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_sort_alphabet) {
                currentSortMode = 1;
                sortAndRefreshDocuments();
                return true;
            } else if (itemId == R.id.action_sort_date) {
                currentSortMode = 2;
                sortAndRefreshDocuments();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void sortAndRefreshDocuments() {
        int selectedTab = binding.tabLayout.getSelectedTabPosition();
        if (selectedTab >= 0 && selectedTab < categories.length) {
            filterDocumentsByCategory(selectedTab);
        }
    }

    private void filterDocumentsByCategory(int tabPosition) {
        Log.d(TAG, "Filtering documents by category: " + categories[tabPosition]);

        List<ExtractedDocument> documents = repository.getDocumentsByCategory(categories[tabPosition]);

        if (documents != null) {
            switch (currentSortMode) {
                case 1: // Alphabetical
                    Collections.sort(documents, (doc1, doc2) ->
                            doc1.getFileName().compareToIgnoreCase(doc2.getFileName()));
                    break;
                case 2: // Date
                    Collections.sort(documents, (doc1, doc2) -> {
                        if (doc1.getCreationDate() == null || doc2.getCreationDate() == null) {
                            return 0;
                        }
                        return doc2.getCreationDate().compareTo(doc1.getCreationDate()); // Mới nhất trước
                    });
                    break;
                default: // Default sorting (by creation date DESC from database)
                    break;
            }

            adapter.setDocuments(documents);
            checkIfEmpty(documents.isEmpty());
        } else {
            adapter.setDocuments(new ArrayList<>());
            checkIfEmpty(true);
        }
    }

    private void setupTabLayout() {
        Log.d(TAG, "Setting up tab layout");
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab selected: " + tab.getPosition());
                filterDocumentsByCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up recycler view");

        adapter = new DocumentAdapter(requireContext(), this);
        binding.recyclerDocuments.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerDocuments.setAdapter(adapter);

        filterDocumentsByCategory(0); // Load "Personal" category first
    }

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

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
    }

    private void checkForCategorySelection() {
        if (getActivity() != null && getActivity().getIntent() != null) {
            String selectedCategory = getActivity().getIntent().getStringExtra("selected_category");
            if (selectedCategory != null && !selectedCategory.isEmpty()) {
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equals(selectedCategory)) {
                        binding.tabLayout.getTabAt(i).select();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onDocumentClick(ExtractedDocument document) {
        Bundle args = new Bundle();
        args.putLong("document_id", document.getId());
        navController.navigate(R.id.navigation_document_viewer, args);
    }

    @Override
    public boolean onDocumentLongClick(ExtractedDocument document) {
        showDocumentOptionsDialog(document);
        return true;
    }

    private void showDocumentOptionsDialog(ExtractedDocument document) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(document.getFileName());

        String[] options = {"View", "Share", "Delete"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    onDocumentClick(document);
                    break;
                case 1:
                    shareDocument(document);
                    break;
                case 2:
                    confirmDocumentDeletion(document);
                    break;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void shareDocument(ExtractedDocument document) {
        DocumentViewerFragment fragment = DocumentViewerFragment.newInstance(document.getId());
        onDocumentClick(document);
        Toast.makeText(requireContext(), "Opening document for sharing...", Toast.LENGTH_SHORT).show();
    }

    private void confirmDocumentDeletion(ExtractedDocument document) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Document");
        builder.setMessage("Are you sure you want to delete '" + document.getFileName() + "'?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            boolean deleted = repository.deleteDocument(document.getId());
            if (deleted) {
                Toast.makeText(requireContext(), "Document deleted", Toast.LENGTH_SHORT).show();
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