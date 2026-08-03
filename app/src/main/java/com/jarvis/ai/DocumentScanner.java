package com.jarvis.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;
import java.util.Locale;

/**
 * Document Scanner — uses ML Kit OCR (on-device, free, no internet needed)
 * to extract text from a captured/gallery image.
 */
public class DocumentScanner {

    public interface Callback {
        void onResult(String text, int lineCount);
        void onError(String reason);
    }

    public static boolean isScanCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("scan document") || lower.contains("scan this") ||
               lower.contains("read document") || lower.contains("ocr") ||
               lower.contains("extract text") || lower.contains("scan text") ||
               lower.contains("read the text") || lower.contains("scan page") ||
               lower.contains("scan receipt") || lower.contains("scan card") ||
               lower.contains("scan qr") || lower.contains("read this image") ||
               lower.contains("what does this say") || lower.contains("what does it say");
    }

    public static void scan(Context ctx, Uri imageUri, Callback cb) {
        new Thread(() -> {
            try {
                InputStream is  = ctx.getContentResolver().openInputStream(imageUri);
                if (is == null) { cb.onError("Cannot open image."); return; }
                Bitmap bmp = BitmapFactory.decodeStream(is); is.close();
                if (bmp == null) { cb.onError("Cannot decode image."); return; }

                // Resize if too large (ML Kit works best ≤ 4MP)
                int w = bmp.getWidth(), h = bmp.getHeight();
                if (w * h > 4_000_000) {
                    float scale = (float) Math.sqrt(4_000_000.0 / (w * h));
                    bmp = Bitmap.createScaledBitmap(bmp, (int)(w * scale), (int)(h * scale), true);
                }

                InputImage image = InputImage.fromBitmap(bmp, 0);
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String text = result.getText().trim();
                        if (text.isEmpty()) {
                            cb.onError("No text found in the image, sir.");
                        } else {
                            int lines = text.split("\n").length;
                            cb.onResult(text, lines);
                        }
                    })
                    .addOnFailureListener(e -> cb.onError("OCR failed: " + e.getMessage()));
            } catch (Exception e) {
                cb.onError("Scanner error: " + e.getMessage());
            }
        }).start();
    }

    public static void scanBitmap(Bitmap bmp, Callback cb) {
        if (bmp == null) { cb.onError("No image provided."); return; }
        InputImage image = InputImage.fromBitmap(bmp, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
            .addOnSuccessListener(result -> {
                String text = result.getText().trim();
                if (text.isEmpty()) cb.onError("No text found in the image, sir.");
                else cb.onResult(text, text.split("\n").length);
            })
            .addOnFailureListener(e -> cb.onError("OCR failed: " + e.getMessage()));
    }
}
