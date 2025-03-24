package com.quang.escan.ui.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.quang.escan.model.ExtractedDocument;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Repository for managing document storage and retrieval
 */
public class LibraryRepository {
    private static final String TAG = "LibraryRepository";
    private static final String DATABASE_NAME = "escan_documents.db";
    private static final int DATABASE_VERSION = 1;

    // Database tables and columns
    private static final String TABLE_DOCUMENTS = "documents";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FILE_NAME = "file_name";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_EXTRACTED_TEXT = "extracted_text";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_CREATION_DATE = "creation_date";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private final DatabaseHelper dbHelper;

    public LibraryRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * Save a document to the database
     * @param document The document to save
     * @return The ID of the saved document, or -1 if there was an error
     */
    public long saveDocument(ExtractedDocument document) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_FILE_NAME, document.getFileName());
        values.put(COLUMN_CATEGORY, document.getCategory());
        values.put(COLUMN_EXTRACTED_TEXT, document.getExtractedText());
        values.put(COLUMN_IMAGE_PATH, document.getImagePath());
        values.put(COLUMN_CREATION_DATE, dateFormat.format(document.getCreationDate()));

        long id = db.insert(TABLE_DOCUMENTS, null, values);
        Log.d(TAG, "Document saved with ID: " + id);
        return id;
    }
    
    /**
     * Update an existing document in the database
     * @param document The document to update
     * @return The number of rows affected (should be 1 if successful)
     */
    public long updateDocument(ExtractedDocument document) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_FILE_NAME, document.getFileName());
        values.put(COLUMN_CATEGORY, document.getCategory());
        values.put(COLUMN_EXTRACTED_TEXT, document.getExtractedText());
        values.put(COLUMN_IMAGE_PATH, document.getImagePath());
        
        // Don't update creation date - it should remain as is

        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(document.getId())};
        
        int rowsAffected = db.update(TABLE_DOCUMENTS, values, whereClause, whereArgs);
        Log.d(TAG, "Document updated, rows affected: " + rowsAffected);
        return rowsAffected;
    }

    /**
     * Get a document by its ID
     * @param documentId The ID of the document to get
     * @return The document, or null if not found
     */
    public ExtractedDocument getDocumentById(long documentId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(documentId)};
        
        try (Cursor cursor = db.query(
                TABLE_DOCUMENTS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToDocument(cursor);
            }
        }
        
        return null;
    }

    /**
     * Get all documents
     * @return A list of all documents
     */
    public List<ExtractedDocument> getAllDocuments() {
        return getDocumentsByCategory(null);
    }

    /**
     * Get documents by category
     * @param category The category to filter by, or null for all documents
     * @return A list of documents in the specified category
     */
    public List<ExtractedDocument> getDocumentsByCategory(String category) {
        List<ExtractedDocument> documents = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = null;
        String[] selectionArgs = null;

        if (category != null && !category.isEmpty()) {
            selection = COLUMN_CATEGORY + " = ?";
            selectionArgs = new String[]{category};
        }

        try (Cursor cursor = db.query(
                TABLE_DOCUMENTS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                COLUMN_CREATION_DATE + " DESC")) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ExtractedDocument document = cursorToDocument(cursor);
                    documents.add(document);
                } while (cursor.moveToNext());
            }
        }

        return documents;
    }

    /**
     * Delete a document
     * @param documentId The ID of the document to delete
     * @return True if the document was successfully deleted
     */
    public boolean deleteDocument(long documentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete(
                TABLE_DOCUMENTS,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(documentId)});
        return rowsDeleted > 0;
    }

    /**
     * Convert a database cursor to a document object
     * @param cursor The cursor to convert
     * @return The document object
     */
    private ExtractedDocument cursorToDocument(Cursor cursor) {
        ExtractedDocument document = new ExtractedDocument();

        document.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        document.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_NAME)));
        document.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
        document.setExtractedText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXTRACTED_TEXT)));
        document.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));

        String dateString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATION_DATE));
        try {
            document.setCreationDate(dateFormat.parse(dateString));
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            document.setCreationDate(new Date());
        }

        return document;
    }

    /**
     * Database helper class
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTableQuery = "CREATE TABLE " + TABLE_DOCUMENTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_FILE_NAME + " TEXT NOT NULL, " +
                    COLUMN_CATEGORY + " TEXT NOT NULL, " +
                    COLUMN_EXTRACTED_TEXT + " TEXT, " +
                    COLUMN_IMAGE_PATH + " TEXT, " +
                    COLUMN_CREATION_DATE + " TEXT NOT NULL);";

            db.execSQL(createTableQuery);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Handle database upgrades if needed
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOCUMENTS);
            onCreate(db);
        }
    }
} 