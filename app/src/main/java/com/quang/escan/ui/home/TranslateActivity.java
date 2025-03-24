package com.quang.escan.ui.home;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.quang.escan.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TranslateActivity extends AppCompatActivity {

    private EditText sourceLanguageEt;
    private TextView destinationLanguageTv;
    private MaterialButton sourceLanguageChooseBtn, translateBtn, destinationLanguageChooseBtn;
    private Translator translator;
    private ProgressDialog progressDialog;
    private ArrayList<ModelLanguage> languageArrayList;

    private String sourceLanguageCode = "en";  // Mặc định: Tiếng Anh
    private String sourceLanguageTitle = "English";
    private String destinationLanguageCode = "vi"; // Mặc định: Tiếng Việt
    private String destinationLanguageTitle = "Vietnamese";

    private static final String TAG = "TranslateActivity";
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_translate);

        // Ánh xạ các View từ layout
        sourceLanguageEt = findViewById(R.id.sourceLanguageEt);
        destinationLanguageTv = findViewById(R.id.destinationLanguageTv);
        sourceLanguageChooseBtn = findViewById(R.id.sourceLanguageChooseBtn);
        translateBtn = findViewById(R.id.translateBtn);
        destinationLanguageChooseBtn = findViewById(R.id.destinationLanguageChooseBtn);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        loadAvailableLanguages();

        // Xử lý sự kiện khi bấm chọn ngôn ngữ nguồn
        sourceLanguageChooseBtn.setOnClickListener(v -> chooseSourceLanguage());

        // Xử lý sự kiện khi bấm chọn ngôn ngữ đích
        destinationLanguageChooseBtn.setOnClickListener(v -> chooseDestinationLanguage());

        // Xử lý sự kiện khi bấm dịch
        translateBtn.setOnClickListener(v -> validateAndTranslate());

        //Toolbar
        // Thiết lập Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bật nút "X" để đóng Activity
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void validateAndTranslate() {
        String sourceText = sourceLanguageEt.getText().toString().trim();
        Log.d(TAG, "validateAndTranslate: " + sourceText);

        if (sourceText.isEmpty()) {
            Toast.makeText(this, "Enter text to translate...", Toast.LENGTH_SHORT).show();
        } else {
            startTranslation(sourceText);
        }
    }

    private void startTranslation(String sourceText) {
        progressDialog.setMessage("Downloading language model...");
        progressDialog.show();

        // Tạo tùy chọn dịch với ngôn ngữ đã chọn
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(destinationLanguageCode)
                .build();
        translator = Translation.getClient(options);

        // Điều kiện tải model
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        // Kiểm tra & tải model nếu cần
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Model is ready for translation.");
                    translateText(sourceText);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Model download failed: " + e.getMessage());
                    Toast.makeText(TranslateActivity.this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }

    private void translateText(String sourceText) {
        translator.translate(sourceText)
                .addOnSuccessListener(translatedText -> {
                    Log.d(TAG, "Translated text: " + translatedText);
                    progressDialog.dismiss();
                    destinationLanguageTv.setText(translatedText);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Translation failed: " + e.getMessage());
                    Toast.makeText(TranslateActivity.this, "Translation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void chooseSourceLanguage() {
        PopupMenu popupMenu = new PopupMenu(this, sourceLanguageChooseBtn);
        for (int i = 0; i < languageArrayList.size(); i++) {
            popupMenu.getMenu().add(Menu.NONE, i, i, languageArrayList.get(i).languageTitle);
        }

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int position = item.getItemId();
                sourceLanguageCode = languageArrayList.get(position).languageCode;
                sourceLanguageTitle = languageArrayList.get(position).languageTitle;

                sourceLanguageChooseBtn.setText(sourceLanguageTitle);
                sourceLanguageEt.setHint("Enter " + sourceLanguageTitle);

                Log.d(TAG, "Selected source language: " + sourceLanguageTitle);
                return false;
            }
        });
    }



    private void chooseDestinationLanguage() {
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
            Log.d(TAG, "Selected destination language: " + destinationLanguageCode);
            return false;
        });
    }

    private void loadAvailableLanguages() {
        languageArrayList = new ArrayList<>();
        List<String> languageCodeList = TranslateLanguage.getAllLanguages();

        for (String languageCode : languageCodeList) {
            String languageTitle = new Locale(languageCode).getDisplayLanguage();
            Log.d(TAG, "Loaded language: " + languageCode);
            Log.d(TAG, "Loaded language: " + languageTitle);

            ModelLanguage modelLanguage = new ModelLanguage(languageCode,languageTitle);
            languageArrayList.add(modelLanguage);
        }
    }
}