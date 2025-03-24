package com.quang.escan.ui.qr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.quang.escan.R;
import com.quang.escan.databinding.ActivityQrResultBinding;

public class QrResultActivity extends AppCompatActivity {
    public static final String EXTRA_QR_VALUE = "extra_qr_value";
    private ActivityQrResultBinding binding;
    private String qrContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Get QR content from intent
        qrContent = getIntent().getStringExtra(EXTRA_QR_VALUE);
        if (qrContent == null || qrContent.isEmpty()) {
            Toast.makeText(this, "No QR content found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Display QR content
        binding.tvQrContent.setText(qrContent);
        
        // Set up copy button
        binding.btnCopy.setOnClickListener(v -> copyToClipboard(qrContent));
        
        // Set up open URL button - only show if content is a URL
        if (isValidUrl(qrContent)) {
            binding.btnOpenUrl.setVisibility(View.VISIBLE);
            binding.btnOpenUrl.setOnClickListener(v -> openUrl(qrContent));
        } else {
            binding.btnOpenUrl.setVisibility(View.GONE);
        }
    }
    
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QR Content", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open URL", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean isValidUrl(String url) {
        return Patterns.WEB_URL.matcher(url).matches();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
