package com.example.nzreceiptapp.data.ocr;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;

import com.example.nzreceiptapp.domain.service.IOCRService;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基於 Google ML Kit 的 OCR 實作 (Data Layer)
 */
public class MLKitOCRService implements IOCRService {

    private final Context context;
    private final TextRecognizer recognizer;
    private final OcrTextLayoutBuilder layoutBuilder;

    public MLKitOCRService(Context context) {
        this.context = context.getApplicationContext();
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.layoutBuilder = new OcrTextLayoutBuilder();
    }

    @Override
    public void extractText(String imagePath, OnOCRCompleteListener listener) {
        try {
            InputImage image = InputImage.fromFilePath(context, Uri.parse(imagePath));

            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String layoutText = rebuildReceiptRows(text);
                        listener.onSuccess(layoutText.isEmpty() ? text.getText() : layoutText);
                    })
                    .addOnFailureListener(e -> {
                        listener.onFailure(e);
                    });

        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    private String rebuildReceiptRows(Text text) {
        List<OcrTextLayoutBuilder.Fragment> fragments = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect box = line.getBoundingBox();
                if (box == null) continue;
                fragments.add(new OcrTextLayoutBuilder.Fragment(
                        line.getText(), box.left, box.top, box.right, box.bottom));
            }
        }
        return layoutBuilder.build(fragments);
    }
}
