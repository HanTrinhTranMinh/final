package com.quang.escan.ui.home;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.quang.escan.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TranslateActivity extends AppCompatActivity {

    private TextInputEditText sourceLanguageEt;
    private TextView destinationLanguageTv;
    private MaterialButton sourceLanguageChooseBtn, translateBtn, destinationLanguageChooseBtn;
    private ImageButton convertLanguageBtn;
    private Button btnCopy, btnShare;
    private MaterialToolbar toolbar;
    private ProgressDialog progressDialog;
    private static ArrayList<ModelLanguage> languageArrayList;
    private Translator translator;

    private String sourceLanguageCode = "und"; // "und" nghĩa là chưa xác định
    private String sourceLanguageTitle = "Auto-detect";
    private String destinationLanguageCode = "vi"; // Mặc định: Tiếng Việt
    private String destinationLanguageTitle = "Tiếng Việt";
    private static final String TAG = "TranslateActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        // Ánh xạ các View từ layout
        sourceLanguageEt = findViewById(R.id.sourceLanguageEt);
        destinationLanguageTv = findViewById(R.id.destinationLanguageTv);
        sourceLanguageChooseBtn = findViewById(R.id.sourceLanguageChooseBtn);
        translateBtn = findViewById(R.id.translateBtn);
        destinationLanguageChooseBtn = findViewById(R.id.destinationLanguageChooseBtn);
        convertLanguageBtn = findViewById(R.id.convertLanguage);
        btnCopy = findViewById(R.id.btnCopy);
        btnShare = findViewById(R.id.btnShare);
        toolbar = findViewById(R.id.toolbar);

        // Thiết lập Toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Khởi tạo ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        // Tải danh sách ngôn ngữ
        loadAvailableLanguages();

        // Cập nhật giao diện ban đầu
        sourceLanguageChooseBtn.setText(sourceLanguageTitle);
        destinationLanguageChooseBtn.setText(destinationLanguageTitle);

        // Nhận văn bản từ Intent và tự động dịch
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("recognized_text")) {
            String recognizedText = intent.getStringExtra("recognized_text");
            sourceLanguageEt.setText(recognizedText);
            detectLanguageAndTranslate(recognizedText);
        }

        // Xử lý sự kiện
        sourceLanguageChooseBtn.setOnClickListener(v -> chooseSourceLanguage());
        destinationLanguageChooseBtn.setOnClickListener(v -> chooseDestinationLanguage());
        translateBtn.setOnClickListener(v -> {
            String sourceText = sourceLanguageEt.getText().toString().trim();
            if (!sourceText.isEmpty()) {
                detectLanguageAndTranslate(sourceText);
            } else {
                Toast.makeText(this, "Enter text to translate...", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý nút hoán đổi ngôn ngữ
        convertLanguageBtn.setOnClickListener(v -> swapLanguages());

        // Thiết lập nút Copy
        btnCopy.setOnClickListener(v -> {
            String translatedText = destinationLanguageTv.getText().toString();
            if (!translatedText.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("translated text", translatedText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No text to copy!", Toast.LENGTH_SHORT).show();
            }
        });

        // Thiết lập nút Share
        btnShare.setOnClickListener(v -> {
            String translatedText = destinationLanguageTv.getText().toString();
            if (!translatedText.isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, translatedText);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            } else {
                Toast.makeText(this, "No text to share!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void swapLanguages() {
        // Hoán đổi mã ngôn ngữ và tiêu đề
        String tempCode = sourceLanguageCode;
        String tempTitle = sourceLanguageTitle;
        sourceLanguageCode = destinationLanguageCode;
        sourceLanguageTitle = destinationLanguageTitle;
        destinationLanguageCode = tempCode;
        destinationLanguageTitle = tempTitle;

        // Cập nhật giao diện
        sourceLanguageChooseBtn.setText(sourceLanguageTitle);
        destinationLanguageChooseBtn.setText(destinationLanguageTitle);

        // Hoán đổi văn bản nếu có
        String sourceText = sourceLanguageEt.getText().toString().trim();
        String destText = destinationLanguageTv.getText().toString().trim();
        if (!sourceText.isEmpty() || !destText.isEmpty()) {
            sourceLanguageEt.setText(destText);
            destinationLanguageTv.setText(sourceText);
        }

        // Tự động dịch lại nếu có văn bản trong sourceLanguageEt
        if (!sourceText.isEmpty() && !sourceLanguageCode.equals("und")) {
            startTranslation(destText.isEmpty() ? sourceText : destText);
        }
    }

    private void detectLanguageAndTranslate(String text) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Detecting language...");
        progressDialog.show();

        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) {
                        Log.d(TAG, "Could not identify language, defaulting to English");
                        sourceLanguageCode = "en";
                        sourceLanguageTitle = "English";
                    } else {
                        sourceLanguageCode = languageCode;
                        sourceLanguageTitle = new Locale(languageCode).getDisplayLanguage();
                    }
                    sourceLanguageChooseBtn.setText(sourceLanguageTitle);
                    Log.d(TAG, "Detected language: " + sourceLanguageTitle + " (" + sourceLanguageCode + ")");
                    startTranslation(text);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Language detection failed: " + e.getMessage());
                    Toast.makeText(this, "Language detection failed, defaulting to English", Toast.LENGTH_SHORT).show();
                    sourceLanguageCode = "en";
                    sourceLanguageTitle = "English";
                    sourceLanguageChooseBtn.setText(sourceLanguageTitle);
                    startTranslation(text);
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void startTranslation(String sourceText) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(destinationLanguageCode)
                .build();
        translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder().build();
        progressDialog.setMessage("Preparing translation model...");
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Model is ready for translation.");
                    translateText(sourceText);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Model download failed: " + e.getMessage());
                    Toast.makeText(this, "Failed to prepare translation model: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void translateText(String sourceText) {
        progressDialog.setMessage("Translating...");
        translator.translate(sourceText)
                .addOnSuccessListener(translatedText -> {
                    Log.d(TAG, "Translated text: " + translatedText);
                    progressDialog.dismiss();
                    destinationLanguageTv.setText(translatedText);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Translation failed: " + e.getMessage());
                    Toast.makeText(this, "Translation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void chooseSourceLanguage() {
        if (languageArrayList == null || languageArrayList.isEmpty()) {
            Toast.makeText(this, "No languages available", Toast.LENGTH_SHORT).show();
            return;
        }
        PopupMenu popupMenu = new PopupMenu(this, sourceLanguageChooseBtn);
        popupMenu.getMenu().add(Menu.NONE, 0, 0, "Auto-detect");
        for (int i = 0; i < languageArrayList.size(); i++) {
            popupMenu.getMenu().add(Menu.NONE, i + 1, i + 1, languageArrayList.get(i).languageTitle);
        }
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(item -> {
            int position = item.getItemId();
            if (position == 0) {
                sourceLanguageCode = "und";
                sourceLanguageTitle = "Auto-detect";
            } else {
                sourceLanguageCode = languageArrayList.get(position - 1).languageCode;
                sourceLanguageTitle = languageArrayList.get(position - 1).languageTitle;
            }
            sourceLanguageChooseBtn.setText(sourceLanguageTitle);
            Log.d(TAG, "Selected source language: " + sourceLanguageTitle);
            String sourceText = sourceLanguageEt.getText().toString().trim();
            if (!sourceText.isEmpty() && !sourceLanguageCode.equals("und")) {
                startTranslation(sourceText);
            }
            return true;
        });
    }

    private void chooseDestinationLanguage() {
        if (languageArrayList == null || languageArrayList.isEmpty()) {
            Toast.makeText(this, "No languages available", Toast.LENGTH_SHORT).show();
            return;
        }
        PopupMenu popupMenu = new PopupMenu(this, destinationLanguageChooseBtn);
        for (int i = 0; i < languageArrayList.size(); i++) {
            popupMenu.getMenu().add(Menu.NONE, i, i, languageArrayList.get(i).languageTitle);
        }
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(item -> {
            int position = item.getItemId();
            destinationLanguageCode = languageArrayList.get(position).languageCode;
            destinationLanguageTitle = languageArrayList.get(position).languageTitle;
            destinationLanguageChooseBtn.setText(destinationLanguageTitle);
            Log.d(TAG, "Selected destination language: " + destinationLanguageTitle);
            String sourceText = sourceLanguageEt.getText().toString().trim();
            if (!sourceText.isEmpty()) {
                detectLanguageAndTranslate(sourceText);
            }
            return true;
        });
    }

    private void loadAvailableLanguages() {
        if (languageArrayList == null || languageArrayList.isEmpty()) {
            languageArrayList = new ArrayList<>();
            List<String> languageCodeList = TranslateLanguage.getAllLanguages();
            for (String languageCode : languageCodeList) {
                String languageTitle = new Locale(languageCode).getDisplayLanguage();
                languageArrayList.add(new ModelLanguage(languageCode, languageTitle));
            }
            Log.d(TAG, "Languages loaded: " + languageArrayList.size());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translator != null) {
            translator.close();
        }
    }
}