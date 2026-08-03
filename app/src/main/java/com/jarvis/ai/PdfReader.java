package com.jarvis.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reads a PDF URI and returns plain text via ML Kit OCR on each page.
 * Max 10 pages to stay within reasonable token limits.
 */
public class PdfReader {

    public interface Callback {
        void onResult(String text, int pages);
        void onError(String reason);
    }

    public static void read(Context ctx, Uri uri, Callback cb) {
        new Thread(() -> {
            try (ParcelFileDescriptor pfd =
                     ctx.getContentResolver().openFileDescriptor(uri, "r")) {
                if (pfd == null) { cb.onError("Cannot open PDF"); return; }

                PdfRenderer renderer = new PdfRenderer(pfd);
                int pageCount = Math.min(renderer.getPageCount(), 10);
                TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                StringBuilder sb = new StringBuilder();
                sb.append("[PDF — ").append(renderer.getPageCount()).append(" page(s)]\n\n");

                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    // Render at 150 DPI equivalent
                    int w = (int)(page.getWidth() * 1.5f);
                    int h = (int)(page.getHeight() * 1.5f);
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();

                    // OCR this page synchronously via CountDownLatch
                    CountDownLatch latch = new CountDownLatch(1);
                    AtomicReference<String> pageText = new AtomicReference<>("");
                    InputImage img = InputImage.fromBitmap(bmp, 0);
                    recognizer.process(img)
                        .addOnSuccessListener(result -> {
                            pageText.set(result.getText());
                            latch.countDown();
                        })
                        .addOnFailureListener(e -> latch.countDown());
                    latch.await();
                    bmp.recycle();

                    sb.append("--- Page ").append(i + 1).append(" ---\n");
                    sb.append(pageText.get()).append("\n\n");
                }
                renderer.close();
                recognizer.close();
                cb.onResult(sb.toString().trim(), pageCount);

            } catch (IOException e) {
                cb.onError("PDF read error: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cb.onError("Interrupted");
            }
        }).start();
    }
}
