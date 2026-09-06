package com.jarvis.ai.pipeline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Universal Graph Search Engine implementing Breadth-First Search (BFS)
 * and Depth-First Search (DFS) with real-time telemetry, step recording,
 * and comparative algorithmic benchmarking.
 *
 * Applicable across computer vision graphs, routing networks, component analysis,
 * circuit traces, and topological meshes.
 */
public class GraphSearchEngine {

    /**
     * Snapshot of a single algorithmic step during search traversal.
     */
    public static class SearchStep {
        public final int stepIndex;
        public final int currentNodeId;
        public final List<Integer> frontier; // Queue for BFS, Stack for DFS
        public final Set<Integer> visited;   // All nodes marked as visited so far
        public final Map<Integer, Integer> parentMap; // Child -> Parent for path tracing
        public final List<Integer> currentBranch; // Active exploration branch
        public final String logMessage;
        public final boolean isBacktracking;
        public final int peakFrontierSize;

        public SearchStep(int stepIndex, int currentNodeId, List<Integer> frontier,
                          Set<Integer> visited, Map<Integer, Integer> parentMap,
                          List<Integer> currentBranch, String logMessage,
                          boolean isBacktracking, int peakFrontierSize) {
            this.stepIndex = stepIndex;
            this.currentNodeId = currentNodeId;
            this.frontier = new ArrayList<>(frontier);
            this.visited = new HashSet<>(visited);
            this.parentMap = new HashMap<>(parentMap);
            this.currentBranch = new ArrayList<>(currentBranch);
            this.logMessage = logMessage;
            this.isBacktracking = isBacktracking;
            this.peakFrontierSize = peakFrontierSize;
        }
    }

    /**
     * Complete result of a search execution including performance telemetry.
     */
    public static class SearchResult {
        public final String algorithmName; // "BFS" or "DFS"
        public final boolean foundGoal;
        public final List<Integer> path; // Reconstructed path from start to goal
        public final List<SearchStep> steps; // Recorded animation frames
        public final int totalSteps;
        public final int nodesVisitedCount;
        public final int totalGraphNodes;
        public final float pathDistance;
        public final int peakMemory; // Max items in Queue/Stack
        public final long executionTimeNs;
        public final boolean isShortestPath;
        public final String complexityTime;
        public final String complexitySpace;
        public final String behavioralSummary;

        public SearchResult(String algorithmName, boolean foundGoal, List<Integer> path,
                            List<SearchStep> steps, int totalSteps, int nodesVisitedCount,
                            int totalGraphNodes, float pathDistance, int peakMemory,
                            long executionTimeNs, boolean isShortestPath,
                            String complexityTime, String complexitySpace,
                            String behavioralSummary) {
            this.algorithmName = algorithmName;
            this.foundGoal = foundGoal;
            this.path = path != null ? path : new ArrayList<>();
            this.steps = steps != null ? steps : new ArrayList<>();
            this.totalSteps = totalSteps;
            this.nodesVisitedCount = nodesVisitedCount;
            this.totalGraphNodes = totalGraphNodes;
            this.pathDistance = pathDistance;
            this.peakMemory = peakMemory;
            this.executionTimeNs = executionTimeNs;
            this.isShortestPath = isShortestPath;
            this.complexityTime = complexityTime;
            this.complexitySpace = complexitySpace;
            this.behavioralSummary = behavioralSummary;
        }
    }

    /**
     * Executes Breadth-First Search (BFS).
     *
     * Principles:
     * - Data Structure: FIFO Queue
     * - Traversal: Level-by-level (expanding concentric wave)
     * - Optimality: GUARANTEES shortest path in unweighted networks!
     * - Complexity: Time O(V + E), Space O(V)
     */
    public static SearchResult runBFS(VisionGraph graph, int startId, int goalId) {
        long startTime = System.nanoTime();
        List<SearchStep> steps = new ArrayList<>();
        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> parentMap = new HashMap<>();

        int stepCounter = 0;
        int peakQueue = 0;
        boolean foundGoal = false;

        queue.add(startId);
        visited.add(startId);
        parentMap.put(startId, null);
        peakQueue = Math.max(peakQueue, queue.size());

        // Initial snapshot
        steps.add(new SearchStep(
                stepCounter++,
                startId,
                new ArrayList<>(queue),
                visited,
                parentMap,
                Collections.singletonList(startId),
                "Initialized BFS at Start Node #" + startId + " (" + getNodeLabel(graph, startId) + "). Enqueued root.",
                false,
                peakQueue
        ));

        while (!queue.isEmpty()) {
            int current = queue.poll();

            List<Integer> currentBranch = reconstructPath(parentMap, current);
            String nodeName = getNodeLabel(graph, current);

            if (current == goalId) {
                foundGoal = true;
                steps.add(new SearchStep(
                        stepCounter++,
                        current,
                        new ArrayList<>(queue),
                        visited,
                        parentMap,
                        currentBranch,
                        "TARGET REACHED: Goal Node #" + current + " (" + nodeName + ") dequeued! Shortest path confirmed.",
                        false,
                        peakQueue
                ));
                break;
            }

            List<Integer> neighbors = graph.getNeighbors(current);
            int newlyEnqueued = 0;

            for (int neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                    queue.add(neighbor);
                    newlyEnqueued++;
                }
            }

            peakQueue = Math.max(peakQueue, queue.size());

            steps.add(new SearchStep(
                    stepCounter++,
                    current,
                    new ArrayList<>(queue),
                    visited,
                    parentMap,
                    currentBranch,
                    "Expanded Node #" + current + " (" + nodeName + "): Enqueued " + newlyEnqueued +
                            " unvisited neighbor" + (newlyEnqueued == 1 ? "" : "s") + ". Queue size: " + queue.size(),
                    false,
                    peakQueue
            ));
        }

        long durationNs = System.nanoTime() - startTime;
        List<Integer> finalPath = foundGoal ? reconstructPath(parentMap, goalId) : new ArrayList<>();
        float totalDist = calculatePathDistance(graph, finalPath);

        return new SearchResult(
                "BFS (Breadth-First Search)",
                foundGoal,
                finalPath,
                steps,
                steps.size(),
                visited.size(),
                graph.getNodes().size(),
                totalDist,
                peakQueue,
                durationNs,
                true,
                "O(V + E)",
                "O(V)",
                "BFS expands uniformly outward like a radial wave. It rigorously guarantees the minimal hop count path."
        );
    }

    /**
     * Executes Depth-First Search (DFS).
     *
     * Principles:
     * - Data Structure: LIFO Stack
     * - Traversal: Deep branch exploration with backtracking
     * - Optimality: Does NOT guarantee shortest path (often wanders through sub-optimal detours)
     * - Complexity: Time O(V + E), Space O(h) where h is max branch depth
     */
    public static SearchResult runDFS(VisionGraph graph, int startId, int goalId) {
        long startTime = System.nanoTime();
        List<SearchStep> steps = new ArrayList<>();
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> parentMap = new HashMap<>();

        int stepCounter = 0;
        int peakStack = 0;
        boolean foundGoal = false;

        stack.push(startId);
        peakStack = Math.max(peakStack, stack.size());

        steps.add(new SearchStep(
                stepCounter++,
                startId,
                new ArrayList<>(stack),
                visited,
                parentMap,
                Collections.singletonList(startId),
                "Initialized DFS at Start Node #" + startId + " (" + getNodeLabel(graph, startId) + "). Pushed to stack.",
                false,
                peakStack
        ));

        while (!stack.isEmpty()) {
            int current = stack.pop();

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);
            List<Integer> currentBranch = reconstructPath(parentMap, current);
            String nodeName = getNodeLabel(graph, current);

            if (current == goalId) {
                foundGoal = true;
                steps.add(new SearchStep(
                        stepCounter++,
                        current,
                        new ArrayList<>(stack),
                        visited,
                        parentMap,
                        currentBranch,
                        "TARGET REACHED: Goal Node #" + current + " (" + nodeName + ") popped from stack!",
                        false,
                        peakStack
                ));
                break;
            }

            List<Integer> neighbors = graph.getNeighbors(current);
            // Reverse so lower index neighbors are explored first
            List<Integer> reversedNeighbors = new ArrayList<>(neighbors);
            Collections.reverse(reversedNeighbors);

            int pushedCount = 0;
            for (int neighbor : reversedNeighbors) {
                if (!visited.contains(neighbor)) {
                    parentMap.put(neighbor, current);
                    stack.push(neighbor);
                    pushedCount++;
                }
            }

            peakStack = Math.max(peakStack, stack.size());
            boolean isDeadEnd = (pushedCount == 0 && !stack.isEmpty());

            steps.add(new SearchStep(
                    stepCounter++,
                    current,
                    new ArrayList<>(stack),
                    visited,
                    parentMap,
                    currentBranch,
                    isDeadEnd
                            ? "Dead end at Node #" + current + " (" + nodeName + "). Backtracking to previous branch point."
                            : "Plunged into Node #" + current + " (" + nodeName + "). Pushed " + pushedCount + " branch options to stack.",
                    isDeadEnd,
                    peakStack
            ));
        }

        long durationNs = System.nanoTime() - startTime;
        List<Integer> finalPath = foundGoal ? reconstructPath(parentMap, goalId) : new ArrayList<>();
        float totalDist = calculatePathDistance(graph, finalPath);

        return new SearchResult(
                "DFS (Depth-First Search)",
                foundGoal,
                finalPath,
                steps,
                steps.size(),
                visited.size(),
                graph.getNodes().size(),
                totalDist,
                peakStack,
                durationNs,
                false,
                "O(V + E)",
                "O(h)",
                "DFS follows continuous branch tendrils until blocked, then backtracks. Ideal for topological ordering and cycle detection."
        );
    }

    /**
     * Reconstructs the path from start node to destination using parent backpointers.
     */
    public static List<Integer> reconstructPath(Map<Integer, Integer> parentMap, int targetNode) {
        List<Integer> path = new ArrayList<>();
        Integer curr = targetNode;
        Set<Integer> cycleGuard = new HashSet<>();

        while (curr != null) {
            if (cycleGuard.contains(curr)) break;
            cycleGuard.add(curr);
            path.add(curr);
            curr = parentMap.get(curr);
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * Computes the total Euclidean spatial distance of a reconstructed path.
     */
    public static float calculatePathDistance(VisionGraph graph, List<Integer> path) {
        if (path == null || path.size() < 2) return 0f;
        float total = 0f;
        for (int i = 0; i < path.size() - 1; i++) {
            VisionNode n1 = graph.getNode(path.get(i));
            VisionNode n2 = graph.getNode(path.get(i + 1));
            if (n1 != null && n2 != null) {
                total += n1.distanceTo(n2);
            }
        }
        return total;
    }

    private static String getNodeLabel(VisionGraph g, int id) {
        VisionNode n = g.getNode(id);
        return n != null ? n.label : "Node " + id;
    }

    /**
     * Side-by-Side comparison metrics between BFS and DFS on identical graph topology.
     */
    public static class ComparisonReport {
        public final SearchResult bfs;
        public final SearchResult dfs;
        public final String winnerPathLength;
        public final String winnerMemory;
        public final String analysisText;

        public ComparisonReport(SearchResult bfs, SearchResult dfs) {
            this.bfs = bfs;
            this.dfs = dfs;

            if (bfs.path.size() < dfs.path.size()) {
                winnerPathLength = "BFS Wins (" + (bfs.path.size() - 1) + " hops vs " + (dfs.path.size() - 1) + " hops)";
            } else if (bfs.path.size() > dfs.path.size()) {
                winnerPathLength = "DFS (" + (dfs.path.size() - 1) + " hops vs " + (bfs.path.size() - 1) + " hops)";
            } else {
                winnerPathLength = "Tied (" + (bfs.path.size() - 1) + " hops)";
            }

            if (bfs.peakMemory < dfs.peakMemory) {
                winnerMemory = "BFS (" + bfs.peakMemory + " queue items vs " + dfs.peakMemory + " stack items)";
            } else if (dfs.peakMemory < bfs.peakMemory) {
                winnerMemory = "DFS (" + dfs.peakMemory + " stack items vs " + bfs.peakMemory + " queue items)";
            } else {
                winnerMemory = "Tied (" + bfs.peakMemory + " items)";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("◈ BFS VS DFS ARCHITECTURAL ANALYSIS\n\n");
            sb.append("• Optimality: BFS ").append(bfs.foundGoal ? "guarantees the shortest path of " + (bfs.path.size() - 1) + " hops." : "did not reach goal.")
              .append(" DFS took ").append(dfs.path.size() - 1).append(" hops (often wandering into branch detours).\n");
            sb.append("• Exploration Pattern: BFS explored ").append(bfs.nodesVisitedCount).append("/").append(bfs.totalGraphNodes)
              .append(" nodes radially. DFS explored ").append(dfs.nodesVisitedCount).append("/").append(dfs.totalGraphNodes)
              .append(" nodes via deep tendril recursion.\n");
            sb.append("• Memory Profile: Peak queue size was ").append(bfs.peakMemory).append(" items vs peak stack depth of ")
              .append(dfs.peakMemory).append(" items.\n");
            sb.append("• When to choose BFS: Routing, shortest delivery/transit routes, peer discovery in networks, minimum unweighted paths.\n");
            sb.append("• When to choose DFS: Topological sorting, cycle detection, puzzle backtracking, connected component contour tracing.");

            analysisText = sb.toString();
        }
    }

    public static ComparisonReport compare(VisionGraph graph, int startId, int goalId) {
        SearchResult bfs = runBFS(graph, startId, goalId);
        SearchResult dfs = runDFS(graph, startId, goalId);
        return new ComparisonReport(bfs, dfs);
    }
}
