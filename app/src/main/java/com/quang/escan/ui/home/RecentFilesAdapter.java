package com.quang.escan.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quang.escan.R;

import java.util.List;

/**
 * Adapter for displaying recent files in a RecyclerView
 */
public class RecentFilesAdapter extends RecyclerView.Adapter<RecentFilesAdapter.FileViewHolder> {

    private List<RecentFile> recentFiles;
    private OnItemClickListener listener;

    /**
     * Interface for handling item clicks
     */
    public interface OnItemClickListener {
        void onItemClick(RecentFile file);
        void onShareClick(RecentFile file);
        void onMoreClick(RecentFile file);
    }

    public RecentFilesAdapter(List<RecentFile> recentFiles) {
        this.recentFiles = recentFiles;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void updateRecentFiles(List<RecentFile> newFiles) {
        this.recentFiles.clear();
        this.recentFiles.addAll(newFiles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        RecentFile file = recentFiles.get(position);
        
        // Set file name and date
        holder.fileName.setText(file.getFileName());
        holder.fileDate.setText(file.getDateModified());
        
        // Set thumbnail if available
        if (file.getThumbnail() != null) {
            holder.fileThumbnail.setImageBitmap(file.getThumbnail());
        } else {
            // Set a default thumbnail for the file type (PDF, image, etc.)
            holder.fileThumbnail.setImageResource(R.drawable.ic_library);
        }
        
        // Set click listeners for buttons
        holder.shareButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareClick(file);
            }
        });
        
        holder.moreButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreClick(file);
            }
        });
        
        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(file);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recentFiles.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileThumbnail;
        TextView fileName;
        TextView fileDate;
        ImageButton shareButton;
        ImageButton moreButton;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileThumbnail = itemView.findViewById(R.id.image_file_thumbnail);
            fileName = itemView.findViewById(R.id.text_file_name);
            fileDate = itemView.findViewById(R.id.text_file_date);
            shareButton = itemView.findViewById(R.id.btn_share);
        }
    }
} 