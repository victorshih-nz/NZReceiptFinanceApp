package com.example.nzreceiptapp.data.ocr;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import com.example.nzreceiptapp.domain.service.IOCRService;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MLKitOCRService implements IOCRService {
    private static final String TAG = "ReceiptOCR";
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
                        logRawLayout(text);
                        String layoutText = rebuildReceiptRows(text);
                        String finalText = layoutText.isEmpty() ? text.getText() : layoutText;
                        Log.d(TAG, "===== RECONSTRUCTED START =====");
                        String[] lines = finalText.split("\\n", -1);
                        for (int i = 0; i < lines.length; i++) {
                            Log.d(TAG, String.format(Locale.US, "R%03d | %s", i + 1, lines[i]));
                        }
                        Log.d(TAG, "===== RECONSTRUCTED END =====");
                        listener.onSuccess(finalText);
                    })
                    .addOnFailureListener(listener::onFailure);
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    private void logRawLayout(Text text) {
        Log.d(TAG, "===== MLKIT RAW START =====");
        int bi = 0, gli = 0;
        for (Text.TextBlock block : text.getTextBlocks()) {
            bi++;
            Log.d(TAG, String.format(Locale.US, "BLOCK %03d box=%s text=%s",
                    bi, rect(block.getBoundingBox()), oneLine(block.getText())));
            int li = 0;
            for (Text.Line line : block.getLines()) {
                li++; gli++;
                Rect b = line.getBoundingBox();
                Log.d(TAG, String.format(Locale.US,
                        "L%03d B%03d.%02d box=%s h=%d cy=%.1f | %s",
                        gli, bi, li, rect(b), b == null ? -1 : b.height(),
                        b == null ? -1.0 : b.exactCenterY(), line.getText()));
                int ei = 0;
                for (Text.Element e : line.getElements()) {
                    ei++;
                    Log.d(TAG, String.format(Locale.US, "  E%02d box=%s | %s",
                            ei, rect(e.getBoundingBox()), e.getText()));
                }
            }
        }
        Log.d(TAG, "===== MLKIT RAW END =====");
    }

    private String rebuildReceiptRows(Text text) {
        List<OcrTextLayoutBuilder.Fragment> fragments = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect b = line.getBoundingBox();
                if (b != null) fragments.add(new OcrTextLayoutBuilder.Fragment(
                        line.getText(), b.left, b.top, b.right, b.bottom));
            }
        }
        return layoutBuilder.build(fragments);
    }

    private String rect(Rect r) {
        return r == null ? "null" : "["+r.left+","+r.top+","+r.right+","+r.bottom+"]";
    }

    private String oneLine(String s) {
        return s == null ? "" : s.replace('\n',' ').replace('\r',' ').trim();
    }
}
