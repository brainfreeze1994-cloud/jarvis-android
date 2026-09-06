package com.jarvis.ai.pipeline;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interactive Computer Vision Pipeline View rendering the topological graph,
 * real-time BFS/DFS traversal frontiers, edge networks, and live telemetry HUD.
 */
public class VisionPipelineView extends View {

    public interface OnStepChangeListener {
        void onStepChanged(GraphSearchEngine.SearchStep step, GraphSearchEngine.SearchResult result, int stepIndex, int totalSteps);
    }

    private VisionGraph graph;
    private GraphSearchEngine.SearchResult searchResult;
    private int currentStepIndex = 0;
    private boolean isPlaying = false;
    private int speedDelayMs = 450; // 1x default

    private int startNodeId = 0;
    private int goalNodeId = 9;
    private String currentAlgorithm = "BFS"; // "BFS" or "DFS"

    // Visual layers
    private Bitmap backgroundBitmap;
    private int displayMode = 0; // 0 = Holographic Graph, 1 = Raw, 2 = Grayscale, 3 = Sobel Edges

    // Animation & pulse
    private float pulsePhase = 0f;
    private ValueAnimator pulseAnimator;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private final Runnable playbackRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying && searchResult != null && !searchResult.steps.isEmpty()) {
                if (currentStepIndex < searchResult.steps.size() - 1) {
                    currentStepIndex++;
                    notifyStepChanged();
                    invalidate();
                    playbackHandler.postDelayed(this, speedDelayMs);
                } else {
                    isPlaying = false;
                    invalidate();
                }
            }
        }
    };

    private OnStepChangeListener stepChangeListener;

    // Paints
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pathEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodeBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Touch interaction
    private VisionNode draggedNode = null;
    private float touchStartX, touchStartY;
    private long touchStartTime;

    public VisionPipelineView(Context context) {
        super(context);
        init();
    }

    public VisionPipelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VisionPipelineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        edgePaint.setColor(0x3500D4FF);
        edgePaint.setStrokeWidth(3f);
        edgePaint.setStyle(Paint.Style.STROKE);

        activeEdgePaint.setColor(0xFF00FF99);
        activeEdgePaint.setStrokeWidth(5f);
        activeEdgePaint.setStyle(Paint.Style.STROKE);

        pathEdgePaint.setColor(0xFFFFDD00);
        pathEdgePaint.setStrokeWidth(8f);
        pathEdgePaint.setStyle(Paint.Style.STROKE);

        nodePaint.setStyle(Paint.Style.FILL);
        nodeBorderPaint.setStyle(Paint.Style.STROKE);
        nodeBorderPaint.setStrokeWidth(3f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(26f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        subTextPaint.setColor(0xFF90CAF9);
        subTextPaint.setTextSize(18f);
        subTextPaint.setTextAlign(Paint.Align.CENTER);

        hudBgPaint.setColor(0xCC040F1D);
        hudBgPaint.setStyle(Paint.Style.FILL);

        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(3f);

        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(anim -> {
            pulsePhase = (float) anim.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();

        // Default graph
        setGraph(VisionGraph.createSatelliteTransitNetwork());
    }

    public void setOnStepChangeListener(OnStepChangeListener listener) {
        this.stepChangeListener = listener;
    }

    public void setGraph(VisionGraph newGraph) {
        this.graph = newGraph;
        if (graph != null && !graph.getNodes().isEmpty()) {
            startNodeId = graph.getNodes().get(0).id;
            goalNodeId = graph.getNodes().get(graph.getNodes().size() - 1).id;
        }
        recomputeSearch();
    }

    public VisionGraph getGraph() {
        return graph;
    }

    public void setAlgorithm(String algorithm) {
        this.currentAlgorithm = algorithm;
        recomputeSearch();
    }

    public String getAlgorithm() {
        return currentAlgorithm;
    }

    public void setStartAndGoal(int startId, int goalId) {
        this.startNodeId = startId;
        this.goalNodeId = goalId;
        recomputeSearch();
    }

    public int getStartNodeId() { return startNodeId; }
    public int getGoalNodeId() { return goalNodeId; }

    public void setBackgroundBitmap(Bitmap bmp, int mode) {
        this.backgroundBitmap = bmp;
        this.displayMode = mode;
        invalidate();
    }

    public void setDisplayMode(int mode) {
        this.displayMode = mode;
        invalidate();
    }

    public void setSpeedMultiplier(float mult) {
        this.speedDelayMs = Math.max(80, (int) (450f / mult));
    }

    public void play() {
        if (searchResult == null || searchResult.steps.isEmpty()) return;
        if (currentStepIndex >= searchResult.steps.size() - 1) {
            currentStepIndex = 0;
        }
        isPlaying = true;
        playbackHandler.removeCallbacks(playbackRunnable);
        playbackHandler.post(playbackRunnable);
        invalidate();
    }

    public void pause() {
        isPlaying = false;
        playbackHandler.removeCallbacks(playbackRunnable);
        invalidate();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void stepForward() {
        pause();
        if (searchResult != null && currentStepIndex < searchResult.steps.size() - 1) {
            currentStepIndex++;
            notifyStepChanged();
            invalidate();
        }
    }

    public void stepBackward() {
        pause();
        if (searchResult != null && currentStepIndex > 0) {
            currentStepIndex--;
            notifyStepChanged();
            invalidate();
        }
    }

    public void reset() {
        pause();
        currentStepIndex = 0;
        notifyStepChanged();
        invalidate();
    }

    public void jumpToStep(int step) {
        if (searchResult != null && step >= 0 && step < searchResult.steps.size()) {
            currentStepIndex = step;
            notifyStepChanged();
            invalidate();
        }
    }

    public GraphSearchEngine.SearchResult getSearchResult() {
        return searchResult;
    }

    public void recomputeSearch() {
        pause();
        if (graph == null) return;
        if ("DFS".equalsIgnoreCase(currentAlgorithm)) {
            searchResult = GraphSearchEngine.runDFS(graph, startNodeId, goalNodeId);
        } else {
            searchResult = GraphSearchEngine.runBFS(graph, startNodeId, goalNodeId);
        }
        currentStepIndex = 0;
        notifyStepChanged();
        invalidate();
    }

    private void notifyStepChanged() {
        if (stepChangeListener != null && searchResult != null && !searchResult.steps.isEmpty()) {
            GraphSearchEngine.SearchStep step = searchResult.steps.get(
                    Math.min(currentStepIndex, searchResult.steps.size() - 1));
            stepChangeListener.onStepChanged(step, searchResult, currentStepIndex, searchResult.steps.size());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Draw Background
        if (backgroundBitmap != null && displayMode > 0) {
            canvas.drawBitmap(backgroundBitmap, null, new RectF(0, 0, w, h), null);
        } else {
            // Draw holographic deep background
            canvas.drawColor(0xFF020C1B);
            drawHolographicGrid(canvas, w, h);
        }

        if (graph == null) return;

        // Current search state snapshot
        GraphSearchEngine.SearchStep currentStep = null;
        Set<Integer> visitedSet = new HashSet<>();
        List<Integer> frontierList = new ArrayList<>();
        List<Integer> activeBranch = new ArrayList<>();
        int activeNode = -1;

        if (searchResult != null && !searchResult.steps.isEmpty()) {
            int idx = Math.min(currentStepIndex, searchResult.steps.size() - 1);
            currentStep = searchResult.steps.get(idx);
            visitedSet = currentStep.visited;
            frontierList = currentStep.frontier;
            activeBranch = currentStep.currentBranch;
            activeNode = currentStep.currentNodeId;
        }

        boolean reachedGoal = (searchResult != null && searchResult.foundGoal &&
                currentStepIndex >= searchResult.steps.size() - 1);

        // 2. Draw Edges
        for (VisionEdge edge : graph.getEdges()) {
            VisionNode n1 = graph.getNode(edge.fromId);
            VisionNode n2 = graph.getNode(edge.toId);
            if (n1 == null || n2 == null) continue;

            float x1 = n1.x * w;
            float y1 = n1.y * h;
            float x2 = n2.x * w;
            float y2 = n2.y * h;

            // Check if part of final path
            boolean inFinalPath = reachedGoal && isEdgeInPath(searchResult.path, edge.fromId, edge.toId);
            boolean inActiveBranch = isEdgeInPath(activeBranch, edge.fromId, edge.toId);

            if (inFinalPath) {
                pathEdgePaint.setColor(0xFFFFDD00);
                canvas.drawLine(x1, y1, x2, y2, pathEdgePaint);
            } else if (inActiveBranch) {
                activeEdgePaint.setColor("DFS".equalsIgnoreCase(currentAlgorithm) ? 0xFFFF9900 : 0xFF00FF99);
                canvas.drawLine(x1, y1, x2, y2, activeEdgePaint);
            } else {
                edgePaint.setColor(0x3000D4FF);
                canvas.drawLine(x1, y1, x2, y2, edgePaint);
            }
        }

        // 3. Draw Nodes
        float baseRadius = Math.min(w, h) * 0.038f;

        for (VisionNode node : graph.getNodes()) {
            float nx = node.x * w;
            float ny = node.y * h;

            boolean isStart = (node.id == startNodeId);
            boolean isGoal = (node.id == goalNodeId);
            boolean isCurrent = (node.id == activeNode);
            boolean isVisited = visitedSet.contains(node.id);
            boolean inFrontier = frontierList.contains(node.id);
            boolean inPath = reachedGoal && searchResult.path.contains(node.id);

            // Pulsing rings for frontier / active node
            if (isCurrent) {
                float waveRad = baseRadius * (1.2f + 0.8f * pulsePhase);
                int waveAlpha = (int) (220 * (1f - pulsePhase));
                wavePaint.setColor("DFS".equalsIgnoreCase(currentAlgorithm)
                        ? (0x00FF9900 | (waveAlpha << 24))
                        : (0x0000D4FF | (waveAlpha << 24)));
                canvas.drawCircle(nx, ny, waveRad, wavePaint);
            }

            // Node fill color
            int fillColor;
            if (inPath) {
                fillColor = 0xFFFFDD00; // Gold path
            } else if (isStart) {
                fillColor = 0xFF00E676; // Bright Green
            } else if (isGoal) {
                fillColor = 0xFFFF1744; // Bright Red
            } else if (isCurrent) {
                fillColor = "DFS".equalsIgnoreCase(currentAlgorithm) ? 0xFFFF9900 : 0xFF00D4FF;
            } else if (inFrontier) {
                fillColor = "DFS".equalsIgnoreCase(currentAlgorithm) ? 0xFFBA68C8 : 0xFF29B6F6; // Purple for Stack, Light Blue for Queue
            } else if (isVisited) {
                fillColor = 0xFF37474F; // Slate gray visited
            } else {
                fillColor = 0xFF102A43; // Deep navy unvisited
            }

            nodePaint.setColor(fillColor);
            canvas.drawCircle(nx, ny, baseRadius, nodePaint);

            // Border
            int borderColor = (isStart || isGoal || isCurrent || inPath) ? Color.WHITE : 0xFF00D4FF;
            nodeBorderPaint.setColor(borderColor);
            canvas.drawCircle(nx, ny, baseRadius, nodeBorderPaint);

            // ID Text inside node
            textPaint.setColor((fillColor == 0xFFFFDD00 || fillColor == 0xFF00E676) ? Color.BLACK : Color.WHITE);
            canvas.drawText(String.valueOf(node.id), nx, ny + 8, textPaint);

            // Label text below node
            subTextPaint.setColor(isStart ? 0xFF00E676 : (isGoal ? 0xFFFF5252 : 0xFFC0E8FF));
            String badge = isStart ? "★ START" : (isGoal ? "🎯 GOAL" : node.label);
            canvas.drawText(badge, nx, ny + baseRadius + 18, subTextPaint);
        }

        // 4. In-Canvas Live HUD Banner
        drawHudOverlay(canvas, w, h, currentStep, reachedGoal);
    }

    private boolean isEdgeInPath(List<Integer> path, int u, int v) {
        if (path == null || path.size() < 2) return false;
        for (int i = 0; i < path.size() - 1; i++) {
            int p1 = path.get(i);
            int p2 = path.get(i + 1);
            if ((p1 == u && p2 == v) || (p1 == v && p2 == u)) return true;
        }
        return false;
    }

    private void drawHolographicGrid(Canvas canvas, int w, int h) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0x1200D4FF);
        p.setStrokeWidth(1f);
        int step = 45;
        for (int x = 0; x < w; x += step) canvas.drawLine(x, 0, x, h, p);
        for (int y = 0; y < h; y += step) canvas.drawLine(0, y, w, y, p);
    }

    private void drawHudOverlay(Canvas canvas, int w, int h, GraphSearchEngine.SearchStep step, boolean reachedGoal) {
        float hudHeight = 56f;
        RectF hudRect = new RectF(16, 16, w - 16, 16 + hudHeight);
        hudBgPaint.setColor(0xDD040F1D);
        canvas.drawRoundRect(hudRect, 12, 12, hudBgPaint);

        Paint borderP = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderP.setStyle(Paint.Style.STROKE);
        borderP.setStrokeWidth(1.5f);
        borderP.setColor("DFS".equalsIgnoreCase(currentAlgorithm) ? 0x88FF9900 : 0x8800D4FF);
        canvas.drawRoundRect(hudRect, 12, 12, borderP);

        Paint hudText = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudText.setColor(Color.WHITE);
        hudText.setTextSize(20f);
        hudText.setFakeBoldText(true);

        String algoTag = "DFS".equalsIgnoreCase(currentAlgorithm) ? "◈ DEPTH-FIRST SEARCH (STACK)" : "◈ BREADTH-FIRST SEARCH (QUEUE)";
        canvas.drawText(algoTag, 32, 40, hudText);

        Paint statusP = new Paint(Paint.ANTI_ALIAS_FLAG);
        statusP.setTextSize(16f);
        statusP.setColor(reachedGoal ? 0xFFFFDD00 : 0xFF00FF99);

        String stats = (step != null)
                ? String.format("Step %d/%d | Visited: %d | %s: %d",
                        currentStepIndex + 1,
                        (searchResult != null ? searchResult.steps.size() : 1),
                        step.visited.size(),
                        "DFS".equalsIgnoreCase(currentAlgorithm) ? "Stack" : "Queue",
                        step.frontier.size())
                : "Ready to run";

        if (reachedGoal) {
            stats += " ★ PATH FOUND: " + (searchResult.path.size() - 1) + " HOPS";
        }
        canvas.drawText(stats, 32, 62, statusP);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Touch Gestures: Select Start/Goal or Drag Nodes
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (graph == null) return super.onTouchEvent(event);
        float ex = event.getX();
        float ey = event.getY();
        int w = getWidth();
        int h = getHeight();
        float hitRadius = Math.min(w, h) * 0.08f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = ex;
                touchStartY = ey;
                touchStartTime = System.currentTimeMillis();
                draggedNode = findNodeNear(ex, ey, w, h, hitRadius);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (draggedNode != null) {
                    draggedNode.x = Math.max(0.05f, Math.min(0.95f, ex / w));
                    draggedNode.y = Math.max(0.08f, Math.min(0.92f, ey / h));
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                long duration = System.currentTimeMillis() - touchStartTime;
                float distMoved = (float) Math.hypot(ex - touchStartX, ey - touchStartY);

                // If short tap without movement -> Select Start or Goal
                if (duration < 350 && distMoved < 20f) {
                    VisionNode tappedNode = findNodeNear(ex, ey, w, h, hitRadius);
                    if (tappedNode != null) {
                        handleNodeTap(tappedNode.id);
                    }
                } else if (draggedNode != null) {
                    recomputeSearch();
                }
                draggedNode = null;
                return true;
        }

        return super.onTouchEvent(event);
    }

    private VisionNode findNodeNear(float px, float py, int w, int h, float radius) {
        for (VisionNode n : graph.getNodes()) {
            float nx = n.x * w;
            float ny = n.y * h;
            if (Math.hypot(px - nx, py - ny) <= radius) {
                return n;
            }
        }
        return null;
    }

    private void handleNodeTap(int tappedId) {
        if (tappedId == startNodeId) {
            // Already start node
            return;
        }
        if (tappedId == goalNodeId) {
            // Swap start and goal
            int temp = startNodeId;
            startNodeId = goalNodeId;
            goalNodeId = temp;
        } else {
            // Set new goal node
            goalNodeId = tappedId;
        }
        recomputeSearch();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        playbackHandler.removeCallbacks(playbackRunnable);
        if (pulseAnimator != null) pulseAnimator.cancel();
    }
}
