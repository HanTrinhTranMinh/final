package com.quang.escan.ui.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quang.escan.R;
import com.quang.escan.model.ExtractedDocument;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying documents in the library
 */
public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private Context context;
    private List<ExtractedDocument> documents;
    private DocumentClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    /**
     * Interface for handling document clicks
     */
    public interface DocumentClickListener {
        void onDocumentClick(ExtractedDocument document);
        boolean onDocumentLongClick(ExtractedDocument document);
    }

    public DocumentAdapter(Context context, DocumentClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.documents = new ArrayList<>();
    }

    public void setDocuments(List<ExtractedDocument> documents) {
        this.documents = documents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        ExtractedDocument document = documents.get(position);
        holder.bind(document);
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private ImageView imagePreview;
        private TextView textFileName;
        private TextView textCategory;
        private TextView textDate;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            textFileName = itemView.findViewById(R.id.text_file_name);
            textCategory = itemView.findViewById(R.id.text_category);
            textDate = itemView.findViewById(R.id.text_date);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDocumentClick(documents.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    return listener.onDocumentLongClick(documents.get(position));
                }
                return false;
            });
        }

        public void bind(ExtractedDocument document) {
            // Set text data
            textFileName.setText(document.getFileName());
            textCategory.setText(document.getCategory());
            
            if (document.getCreationDate() != null) {
                textDate.setText(dateFormat.format(document.getCreationDate()));
            } else {
                textDate.setText("Unknown date");
            }

            // Load image if available
            if (document.getImagePath() != null && !document.getImagePath().isEmpty()) {
                File imageFile = new File(document.getImagePath());
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        imagePreview.setImageBitmap(bitmap);
                        return;
                    }
                }
            }
            
            // If no image or error loading, show placeholder
            imagePreview.setImageResource(R.drawable.ic_verified);
        }
    }
} 