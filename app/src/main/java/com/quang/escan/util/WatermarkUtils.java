package com.quang.escan.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

/**
 * Utility class for adding watermarks to images
 */
public class WatermarkUtils {

    /**
     * Adds a horizontal text watermark at the bottom center of the image
     *
     * @param source Source bitmap to watermark
     * @param watermarkText Text to use as watermark
     * @param color Color of the watermark text (including alpha for transparency)
     * @return A new bitmap with the watermark applied
     */
    public static Bitmap addHorizontalTextWatermark(Bitmap source, String watermarkText, int color) {
        if (source == null) {
            throw new IllegalArgumentException("Source bitmap cannot be null");
        }
        
        if (watermarkText == null || watermarkText.isEmpty()) {
            return source;
        }
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        // Create a new bitmap with the same dimensions
        Bitmap result = Bitmap.createBitmap(width, height, source.getConfig());
        
        // Create canvas and draw the original bitmap
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(source, 0, 0, null);
        
        // Setup paint for text
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        // Calculate text size based on bitmap width (about 5% of image width)
        float textSize = width * 0.05f;
        paint.setTextSize(textSize);
        
        // Calculate position (bottom center, with padding)
        int padding = height / 30;
        float xPos = width / 2f;
        float yPos = height - padding;
        
        // Draw the text
        canvas.drawText(watermarkText, xPos, yPos, paint);
        
        return result;
    }
    
    /**
     * Adds a horizontal text watermark with default white color
     *
     * @param source Source bitmap to watermark
     * @param watermarkText Text to use as watermark
     * @return A new bitmap with the watermark applied
     */
    public static Bitmap addHorizontalTextWatermark(Bitmap source, String watermarkText) {
        return addHorizontalTextWatermark(source, watermarkText, Color.argb(150, 255, 255, 255));
    }
    
    /**
     * Adds a vertical text watermark on the right side of the image
     *
     * @param source Source bitmap to watermark
     * @param watermarkText Text to use as watermark
     * @param color Color of the watermark text (including alpha for transparency)
     * @return A new bitmap with the watermark applied
     */
    public static Bitmap addVerticalTextWatermark(Bitmap source, String watermarkText, int color) {
        if (source == null) {
            throw new IllegalArgumentException("Source bitmap cannot be null");
        }
        
        if (watermarkText == null || watermarkText.isEmpty()) {
            return source;
        }
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        // Create a new bitmap with the same dimensions
        Bitmap result = Bitmap.createBitmap(width, height, source.getConfig());
        
        // Create canvas and draw the original bitmap
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(source, 0, 0, null);
        
        // Setup paint for text
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        // Calculate text size based on bitmap height (about 5% of image height)
        float textSize = height * 0.04f;
        paint.setTextSize(textSize);
        
        // Calculate position (right side, vertical)
        int padding = width / 30;
        float xPos = width - padding;
        
        // Save the current matrix
        canvas.save();
        
        // Rotate canvas 90 degrees at the point where we want to draw
        canvas.rotate(90, xPos, height / 2f);
        
        // Draw the text
        canvas.drawText(watermarkText, xPos, height / 2f, paint);
        
        // Restore to the previous matrix
        canvas.restore();
        
        return result;
    }
    
    /**
     * Adds a vertical text watermark with default white color
     *
     * @param source Source bitmap to watermark
     * @param watermarkText Text to use as watermark
     * @return A new bitmap with the watermark applied
     */
    public static Bitmap addVerticalTextWatermark(Bitmap source, String watermarkText) {
        return addVerticalTextWatermark(source, watermarkText, Color.argb(150, 255, 255, 255));
    }
    
    /**
     * Adds a diagonal text watermark across the image
     *
     * @param source Source bitmap to watermark
     * @param watermarkText Text to use as watermark
     * @param angle Angle of the watermark in degrees
     * @param color Color of the watermark text (including alpha for transparency)
     * @return A new bitmap with the watermark applied
     */
    public static Bitmap addDiagonalTextWatermark(Bitmap source, String watermarkText, 
                                                 float angle, int color) {
        if (source == null) {
            throw new IllegalArgumentException("Source bitmap cannot be null");
        }
        
        if (watermarkText == null || watermarkText.isEmpty()) {
            return source;
        }
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        // Create a new bitmap with the same dimensions
        Bitmap result = Bitmap.createBitmap(width, height, source.getConfig());
        
        // Create canvas and draw the original bitmap
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(source, 0, 0, null);
        
        // Setup paint for text
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        // Calculate text size based on image dimensions
        float textSize = Math.min(width, height) * 0.06f;
        paint.setTextSize(textSize);
        
        // Calculate the center of the image
        float centerX = width / 2f;
        float centerY = height / 2f;
        
        // Save canvas state
        canvas.save();
        
        // Rotate the canvas around the center
        canvas.rotate(angle, centerX, centerY);
        
        // Draw the text
        canvas.drawText(watermarkText, centerX, centerY, paint);
        
        // Restore canvas state
        canvas.restore();
        
        return result;
    }
    
    /**
     * Adds a diagonal text watermark with default white color
     *
     * @param source Source bitmap to watermark
     * @param watermarkText Text to use as watermark
     * @param angle Angle of the watermark in degrees
     * @return A new bitmap with the watermark applied
     */
    public static Bitmap addDiagonalTextWatermark(Bitmap source, String watermarkText, float angle) {
        return addDiagonalTextWatermark(source, watermarkText, angle, Color.argb(150, 255, 255, 255));
    }
    
    /**
     * Adds a tiled text watermark pattern across the entire image
     *
     * @param source Source bitmap to watermark
     * @param watermarkText Text to use as watermark
     * @param horizontalSpacing Horizontal spacing between watermarks
     * @param verticalSpacing Vertical spacing between watermarks
     * @param color Color of the watermark text (including alpha for transparency)
     * @return A new bitmap with the watermark applied
     */
    public static Bitmap addTiledTextWatermark(Bitmap source, String watermarkText, 
                                             int horizontalSpacing, int verticalSpacing, int color) {
        if (source == null) {
            throw new IllegalArgumentException("Source bitmap cannot be null");
        }
        
        if (watermarkText == null || watermarkText.isEmpty()) {
            return source;
        }
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        // Create a new bitmap with the same dimensions
        Bitmap result = Bitmap.createBitmap(width, height, source.getConfig());
        
        // Create canvas and draw the original bitmap
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(source, 0, 0, null);
        
        // Setup paint for text
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        // Calculate text size
        float textSize = Math.min(width, height) * 0.04f;
        paint.setTextSize(textSize);
        
        // Get text bounds
        Rect bounds = new Rect();
        paint.getTextBounds(watermarkText, 0, watermarkText.length(), bounds);
        int textWidth = bounds.width();
        int textHeight = bounds.height();
        
        // Calculate rotation for the text (diagonal)
        float angle = -30;
        
        // Create a temporary bitmap for rotated text
        Bitmap textBitmap = Bitmap.createBitmap(textWidth + 20, textHeight * 2, Bitmap.Config.ARGB_8888);
        Canvas textCanvas = new Canvas(textBitmap);
        textCanvas.drawColor(Color.TRANSPARENT); // Transparent background
        
        // Draw text onto the temporary bitmap
        textCanvas.drawText(watermarkText, 10, textHeight + 5, paint);
        
        // Create a rotated matrix
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        
        // Create rotated bitmap
        Bitmap rotatedTextBitmap = Bitmap.createBitmap(textBitmap, 0, 0,
                textBitmap.getWidth(), textBitmap.getHeight(), matrix, true);
        
        // Tile the watermark across the image
        for (int y = 0; y < height; y += verticalSpacing) {
            for (int x = 0; x < width; x += horizontalSpacing) {
                canvas.drawBitmap(rotatedTextBitmap, x, y, null);
            }
        }
        
        // Recycle the temporary bitmaps to free up memory
        textBitmap.recycle();
        rotatedTextBitmap.recycle();
        
        return result;
    }
    
    /**
     * Adds a tiled text watermark with default white color
     *
     * @param source Source bitmap to watermark
     * @param watermarkText Text to use as watermark
     * @param horizontalSpacing Horizontal spacing between watermarks
     * @param verticalSpacing Vertical spacing between watermarks
     * @return A new bitmap with the watermark applied
     */
    public static Bitmap addTiledTextWatermark(Bitmap source, String watermarkText, 
                                           int horizontalSpacing, int verticalSpacing) {
        return addTiledTextWatermark(source, watermarkText, horizontalSpacing, verticalSpacing, 
                                  Color.argb(150, 255, 255, 255));
    }
    
    /**
     * Adds an image watermark to the source bitmap
     *
     * @param source Source bitmap to watermark
     * @param watermarkImage Image to use as a watermark
     * @param padding Padding from the edge of the source image
     * @return A new bitmap with the watermark applied
     */
    public static Bitmap addImageWatermark(Bitmap source, Bitmap watermarkImage, int padding) {
        if (source == null) {
            throw new IllegalArgumentException("Source bitmap cannot be null");
        }
        
        if (watermarkImage == null) {
            return source;
        }
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        // Create a new bitmap with the same dimensions
        Bitmap result = Bitmap.createBitmap(width, height, source.getConfig());
        
        // Create canvas and draw the original bitmap
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(source, 0, 0, null);
        
        // Calculate watermark size (max 20% of the source width)
        int maxWatermarkWidth = width / 5;
        float scale = (float) maxWatermarkWidth / watermarkImage.getWidth();
        
        // If the watermark is smaller than maxWatermarkWidth, don't upscale it
        if (scale > 1) {
            scale = 1;
        }
        
        int scaledWidth = (int) (watermarkImage.getWidth() * scale);
        int scaledHeight = (int) (watermarkImage.getHeight() * scale);
        
        // Create scaled bitmap
        Bitmap scaledWatermark = Bitmap.createScaledBitmap(
                watermarkImage, scaledWidth, scaledHeight, true);
        
        // Calculate position (bottom right with padding)
        int left = width - scaledWidth - padding;
        int top = height - scaledHeight - padding;
        
        // Create a paint with translucent effect
        Paint paint = new Paint();
        paint.setAlpha(128); // 50% opacity
        
        // Draw the watermark image
        canvas.drawBitmap(scaledWatermark, left, top, paint);
        
        // Recycle the scaled bitmap
        scaledWatermark.recycle();
        
        return result;
    }
} 