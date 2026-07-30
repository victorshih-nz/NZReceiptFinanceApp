package com.example.nzreceiptapp.data.ocr;

import android.content.Context;
import android.net.Uri;

import com.example.nzreceiptapp.domain.service.IOCRService;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

/**
 * 基於 Google ML Kit 的 OCR 實作 (Data Layer)
 */
public class MLKitOCRService implements IOCRService {

    private final Context context;
    private final TextRecognizer recognizer;

    public MLKitOCRService(Context context) {
        this.context = context.getApplicationContext();
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    @Override
    public void extractText(String imagePath, OnOCRCompleteListener listener) {
        try {
            InputImage image = InputImage.fromFilePath(context, Uri.parse(imagePath));
            
            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        listener.onSuccess(text.getText());
                    })
                    .addOnFailureListener(e -> {
                        listener.onFailure(e);
                    });
                    
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }
}
