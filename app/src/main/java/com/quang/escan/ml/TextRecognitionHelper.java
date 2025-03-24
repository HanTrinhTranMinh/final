package com.quang.escan.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

/**
 * Helper class for handling text recognition across multiple languages
 */
public class TextRecognitionHelper {
    private static final String TAG = "TextRecognitionHelper";

    // Available language models
    public enum LanguageModel {
        LATIN,      // English and Latin-based languages
        CHINESE,    // Chinese (simplified and traditional)
        DEVANAGARI, // Hindi, Marathi, Sanskrit, etc.
        JAPANESE,   // Japanese
        KOREAN      // Korean
    }

    private Context context;
    private TextRecognitionCallback callback;

    /**
     * Callback interface for text recognition results
     */
    public interface TextRecognitionCallback {
        void onTextRecognized(String text);
        void onError(Exception e);
    }

    /**
     * Constructor
     * 
     * @param context Application context
     * @param callback Callback for recognition results
     */
    public TextRecognitionHelper(Context context, TextRecognitionCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    /**
     * Recognize text from bitmap using the specified language model
     * 
     * @param bitmap Image bitmap
     * @param languageModel Language model to use
     */
    public void recognizeText(Bitmap bitmap, LanguageModel languageModel) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            processTextRecognition(image, languageModel);
        } catch (Exception e) {
            Log.e(TAG, "Error creating input image from bitmap", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    /**
     * Recognize text from an image URI using the specified language model
     * 
     * @param imageUri URI of the image
     * @param languageModel Language model to use
     */
    public void recognizeText(Uri imageUri, LanguageModel languageModel) {
        try {
            InputImage image = InputImage.fromFilePath(context, imageUri);
            processTextRecognition(image, languageModel);
        } catch (IOException e) {
            Log.e(TAG, "Error creating input image from URI", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    /**
     * Process text recognition with appropriate text recognizer
     * 
     * @param image Input image
     * @param languageModel Language model to use
     */
    private void processTextRecognition(InputImage image, LanguageModel languageModel) {
        // Create text recognizer based on selected language model
        TextRecognizer recognizer = getTextRecognizer(languageModel);
        
        // Process the image
        Task<Text> result = recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        String recognizedText = text.getText();
                        Log.d(TAG, "Text recognition successful");
                        
                        if (callback != null) {
                            callback.onTextRecognized(recognizedText);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Text recognition failed", e);
                        
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                });
    }

    /**
     * Get the appropriate text recognizer based on the language model
     * 
     * @param languageModel Language model to use
     * @return TextRecognizer instance for the specified language
     */
    private TextRecognizer getTextRecognizer(LanguageModel languageModel) {
        switch (languageModel) {
            case CHINESE:
                return TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            case DEVANAGARI:
                return TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
            case JAPANESE:
                return TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
            case KOREAN:
                return TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
            case LATIN:
            default:
                return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
    }
} 