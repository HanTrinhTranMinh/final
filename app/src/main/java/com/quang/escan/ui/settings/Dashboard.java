package com.quang.escan.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.quang.escan.R;

import java.io.File;
import java.util.ArrayList;

public class Dashboard extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final String PREFS_NAME = "EScanPrefs";
    private static final String KEY_SCAN_COUNT = "scan_count";
    private static final String KEY_WATERMARK_COUNT = "watermark_count";
    private static final String KEY_TRANSLATE_COUNT = "translate_count";

    // Khai báo các biến TextView với tên đúng theo chuẩn camelCase
    private TextView UploadedFilesCount; // Sửa tên biến để nhất quán
    private TextView ScannedImagesCount;
    private TextView TotalUsageCount;
    private BarChart UsageChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Khởi tạo các view với ID từ XML
        UploadedFilesCount = findViewById(R.id.uploaded);
        ScannedImagesCount = findViewById(R.id.scanned);
        TotalUsageCount = findViewById(R.id.total);
        UsageChart = findViewById(R.id.usage);

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

        // Update UI với tên biến đúng
        UploadedFilesCount.setText(String.valueOf(fileCount));
        ScannedImagesCount.setText(String.valueOf(imageCount));
        TotalUsageCount.setText(String.valueOf(totalUsage));

        // Setup BarChart
        setupBarChart(scanCount, watermarkCount, translateCount);

        Log.d(TAG, "Statistics loaded: " + fileCount + " files, " + imageCount + " images, " +
                scanCount + " scans, " + watermarkCount + " watermarks, " + translateCount + " translations");
    }

    private void setupBarChart(int scanCount, int watermarkCount, int translateCount) {
        // Dữ liệu cho biểu đồ
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, scanCount));
        entries.add(new BarEntry(1f, watermarkCount));
        entries.add(new BarEntry(2f, translateCount));

        // Tạo dataset và đặt màu sắc phù hợp với legend trong layout
        BarDataSet dataSet = new BarDataSet(entries, "Function Usage");
        dataSet.setColors(
                0xFFFF5722, // Scan - Orange
                0xFF3F51B5, // Watermark - Blue
                0xFF4CAF50  // Translate - Green
        );
        dataSet.setValueTextSize(12f); // Kích thước chữ giá trị trên cột
        dataSet.setDrawValues(true); // Hiển thị giá trị trên cột

        // Tạo BarData
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f); // Độ rộng cột

        // Cấu hình trục X
        XAxis xAxis = UsageChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                switch ((int) value) {
                    case 0:
                        return "Scan";
                    case 1:
                        return "Watermark";
                    case 2:
                        return "Translate";
                    default:
                        return "";
                }
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(3);
        xAxis.setTextSize(12f);

        // Tắt lưới và mô tả để giao diện gọn gàng hơn
        UsageChart.getAxisLeft().setAxisMinimum(0f); // Giá trị nhỏ nhất trục Y
        UsageChart.getAxisRight().setEnabled(false); // Tắt trục Y bên phải
        UsageChart.getLegend().setEnabled(false); // Tắt legend mặc định
        UsageChart.getDescription().setEnabled(false);

        // Gán dữ liệu và làm mới
        UsageChart.setData(barData);
        UsageChart.setFitBars(true);
        UsageChart.animateY(1000);
        UsageChart.invalidate();

        Log.d(TAG, "BarChart setup: Scan=" + scanCount + ", Watermark=" + watermarkCount + ", Translate=" + translateCount);
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