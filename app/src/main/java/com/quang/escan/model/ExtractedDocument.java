package com.quang.escan.model;

import java.util.Date;

/**
 * Model class representing an extracted document in the library
 */
public class ExtractedDocument {
    private long id;
    private String fileName;
    private String category;
    private String extractedText;
    private String imagePath;
    private Date creationDate;

    public ExtractedDocument() {
        // Default constructor
    }

    public ExtractedDocument(long id, String fileName, String category, String extractedText, 
                            String imagePath, Date creationDate) {
        this.id = id;
        this.fileName = fileName;
        this.category = category;
        this.extractedText = extractedText;
        this.imagePath = imagePath;
        this.creationDate = creationDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String toString() {
        return "ExtractedDocument{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", category='" + category + '\'' +
                ", creationDate=" + creationDate +
                '}';
    }
} 