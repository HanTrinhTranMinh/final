package com.quang.escan.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.BlurMaskFilter;

public class WatermarkUtils {

    public static Bitmap addHorizontalTextWatermark(Bitmap source, String text, int color, int alpha, float blurRadius, Typeface font, float textSize) {
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setTextSize(textSize); // Sử dụng textSize từ tham số
        paint.setTypeface(font);
        paint.setAntiAlias(true);
        if (blurRadius > 0) {
            paint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
        }

        float x = (canvas.getWidth() - paint.measureText(text)) / 2;
        float y = canvas.getHeight() - 50;
        canvas.drawText(text, x, y, paint);
        return result;
    }

    public static Bitmap addDiagonalTextWatermark(Bitmap source, String text, float angle, int color, int alpha, float blurRadius, Typeface font, float textSize) {
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setTextSize(textSize); // Sử dụng textSize từ tham số
        paint.setTypeface(font);
        paint.setAntiAlias(true);
        if (blurRadius > 0) {
            paint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
        }

        canvas.rotate(angle, canvas.getWidth() / 2f, canvas.getHeight() / 2f);
        float x = (canvas.getWidth() - paint.measureText(text)) / 2;
        float y = canvas.getHeight() / 2;
        canvas.drawText(text, x, y, paint);
        return result;
    }

    public static Bitmap addTiledTextWatermark(Bitmap source, String text, int tileWidth, int tileHeight, int color, int alpha, float blurRadius, Typeface font, float textSize) {
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setTextSize(textSize); // Sử dụng textSize từ tham số
        paint.setTypeface(font);
        paint.setAntiAlias(true);
        if (blurRadius > 0) {
            paint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
        }

        for (int x = 0; x < canvas.getWidth(); x += tileWidth) {
            for (int y = 0; y < canvas.getHeight(); y += tileHeight) {
                canvas.drawText(text, x, y + paint.getTextSize(), paint);
            }
        }
        return result;
    }
}