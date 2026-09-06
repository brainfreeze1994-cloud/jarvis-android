package com.jarvis.ai.pipeline;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.List;

/**
 * End-to-End Computer Vision Pipeline:
 * [Raw Frame] → [Preprocessing & Denoise] → [Gradient Edge Extraction] →
 * [Topological Graph Construction] → [Algorithmic Traversal (BFS / DFS)] → [Augmented Render]
 */
public class VisionPipeline {

    public enum PipelineStage {
        STAGE_RAW("1. Raw Image / Feed", "Original visual frame from camera or benchmark dataset"),
        STAGE_PREPROCESSED("2. Preprocessing & Denoise", "Grayscale conversion, contrast stretching, and Gaussian noise suppression"),
        STAGE_EDGES("3. Gradient Edge Map", "Sobel 3x3 edge gradients and adaptive feature thresholding"),
        STAGE_GRAPH("4. Topological Feature Graph", "Nodes and traversable edges extracted from visual feature centroids"),
        STAGE_TRAVERSAL("5. Graph Traversal (BFS / DFS)", "Breadth-First and Depth-First search execution with animated telemetry");

        public final String title;
        public final String description;

        PipelineStage(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    /**
     * Converts an image to grayscale and applies contrast stretching.
     */
    public static Bitmap preprocessGrayscale(Bitmap src) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        int minLum = 255;
        int maxLum = 0;
        int[] lumArray = new int[w * h];

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            int lum = (int) (0.299f * r + 0.587f * g + 0.114f * b);
            lumArray[i] = lum;
            if (lum < minLum) minLum = lum;
            if (lum > maxLum) maxLum = lum;
        }

        int range = Math.max(1, maxLum - minLum);
        for (int i = 0; i < pixels.length; i++) {
            int stretched = ((lumArray[i] - minLum) * 255) / range;
            pixels[i] = 0xFF000000 | (stretched << 16) | (stretched << 8) | stretched;
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    /**
     * Computes Sobel 3x3 gradient magnitude edge map.
     */
    public static Bitmap extractSobelEdges(Bitmap gray) {
        if (gray == null) return null;
        int w = gray.getWidth();
        int h = gray.getHeight();
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        int[] srcPixels = new int[w * h];
        int[] edgePixels = new int[w * h];
        gray.getPixels(srcPixels, 0, w, 0, 0, w, h);

        int[][] gx = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
        };

        int[][] gy = {
            {-1, -2, -1},
            { 0,  0,  0},
            { 1,  2,  1}
        };

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int sumX = 0;
                int sumY = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = srcPixels[(y + ky) * w + (x + kx)] & 0xFF;
                        sumX += pixel * gx[ky + 1][kx + 1];
                        sumY += pixel * gy[ky + 1][kx + 1];
                    }
                }

                int mag = (int) Math.min(255, Math.sqrt(sumX * sumX + sumY * sumY));
                // Cyan Stark tint for edges
                int edgeColor = mag > 65
                        ? (0xFF000000 | ((mag / 2) << 16) | (mag << 8) | mag)
                        : 0xFF020C1B;
                edgePixels[y * w + x] = edgeColor;
            }
        }

        out.setPixels(edgePixels, 0, w, 0, 0, w, h);
        return out;
    }

    /**
     * Creates a synthetic benchmark image matching a selected scenario
     * so users can experience the vision pipeline instantly without needing a physical setup.
     */
    public static Bitmap generateScenarioCanvas(String scenarioType, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(0xFF020C1B); // Deep navy background

        Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0x1800D4FF);
        gridPaint.setStrokeWidth(1f);

        // Technical blueprint grid lines
        int gridSize = 40;
        for (int x = 0; x < width; x += gridSize) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
        for (int y = 0; y < height; y += gridSize) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }

        Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(2f);

        if ("pcb".equalsIgnoreCase(scenarioType)) {
            // Draw circuit board traces and IC pads
            accentPaint.setColor(0x4000FF99);
            for (int i = 0; i < 6; i++) {
                float y = height * (0.2f + i * 0.12f);
                canvas.drawLine(width * 0.1f, y, width * 0.85f, y, accentPaint);
            }
            // Vertical bus lines
            for (int i = 0; i < 5; i++) {
                float x = width * (0.2f + i * 0.15f);
                canvas.drawLine(x, height * 0.15f, x, height * 0.85f, accentPaint);
            }
        } else if ("stars".equalsIgnoreCase(scenarioType)) {
            // Star field nebulae
            Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            starPaint.setColor(0x60FFFFFF);
            for (int i = 0; i < 40; i++) {
                float sx = (float) ((i * 137.5f) % width);
                float sy = (float) ((i * 223.3f) % height);
                canvas.drawCircle(sx, sy, (i % 3) + 1f, starPaint);
            }
        }

        return bmp;
    }
}
