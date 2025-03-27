package com.quang.escan.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.quang.escan.R;

import java.io.File;
import java.util.ArrayList;

public class Dashboard extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final String PREFS_NAME = "EScanPrefs";
    private static final String KEY_SCAN_COUNT = "scan_count";
    private static final String KEY_WATERMARK_COUNT = "watermark_count";
    private static final String KEY_TRANSLATE_COUNT = "translate_count";

    private TextView uploadedFilesCount, scannedImagesCount, totalUsageCount;
    private BarChart usageChart;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        uploadedFilesCount = findViewById(R.id.uploaded_files_count);
        scannedImagesCount = findViewById(R.id.scanned_images_count);
        totalUsageCount = findViewById(R.id.total_usage_count);
        usageChart = findViewById(R.id.usage_chart);

        // Load statistics
        loadStatistics();

    }

    private void loadStatistics() {
        // Count files and images in storage
        File storageDir = new File(getExternalFilesDir(null), "EScan/Images");
        int imageCount = 0;
        int fileCount = 0;

        if (storageDir.exists()) {
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        if (file.getName().toLowerCase().endsWith(".jpg") ||
                                file.getName().toLowerCase().endsWith(".png")) {
                            imageCount++;
                        }
                        fileCount++;
                    }
                }
            }
        }

        // Load function usage counts from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int scanCount = prefs.getInt(KEY_SCAN_COUNT, 0);
        int watermarkCount = prefs.getInt(KEY_WATERMARK_COUNT, 0);
        int translateCount = prefs.getInt(KEY_TRANSLATE_COUNT, 0);
        int totalUsage = scanCount + watermarkCount + translateCount;

        // Update UI
        uploadedFilesCount.setText(String.valueOf(fileCount));
        scannedImagesCount.setText(String.valueOf(imageCount));
        totalUsageCount.setText(String.valueOf(totalUsage));

        // Setup BarChart
        setupBarChart(scanCount, watermarkCount, translateCount);

        Log.d(TAG, "Statistics loaded: " + fileCount + " files, " + imageCount + " images, " +
                scanCount + " scans, " + watermarkCount + " watermarks, " + translateCount + " translations");
    }

    private void setupBarChart(int scanCount, int watermarkCount, int translateCount) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, scanCount));
        entries.add(new BarEntry(1, watermarkCount));
        entries.add(new BarEntry(2, translateCount));

        BarDataSet dataSet = new BarDataSet(entries, "Function Usage");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        BarData barData = new BarData(dataSet);
        usageChart.setData(barData);
        usageChart.getDescription().setEnabled(false);
        usageChart.animateY(1000); // Animation when loading
        usageChart.invalidate();
    }


    // Static methods to increment counts
    public static void incrementScanCount(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int count = prefs.getInt(KEY_SCAN_COUNT, 0);
        prefs.edit().putInt(KEY_SCAN_COUNT, count + 1).apply();
    }

    public static void incrementWatermarkCount(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int count = prefs.getInt(KEY_WATERMARK_COUNT, 0);
        prefs.edit().putInt(KEY_WATERMARK_COUNT, count + 1).apply();
    }

    public static void incrementTranslateCount(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int count = prefs.getInt(KEY_TRANSLATE_COUNT, 0);
        prefs.edit().putInt(KEY_TRANSLATE_COUNT, count + 1).apply();
    }
}