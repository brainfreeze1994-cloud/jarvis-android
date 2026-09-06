package com.jarvis.ai.pipeline;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Topological visual graph extracted from visual features, road networks,
 * circuit traces, or astronomical constellations.
 */
public class VisionGraph {

    private final String name;
    private final String description;
    private final List<VisionNode> nodes = new ArrayList<>();
    private final List<VisionEdge> edges = new ArrayList<>();
    private final Map<Integer, List<Integer>> adjacency = new HashMap<>();
    private final Map<String, Float> edgeWeights = new HashMap<>();

    public VisionGraph(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<VisionNode> getNodes() { return nodes; }
    public List<VisionEdge> getEdges() { return edges; }

    public void addNode(VisionNode node) {
        nodes.add(node);
        if (!adjacency.containsKey(node.id)) {
            adjacency.put(node.id, new ArrayList<>());
        }
    }

    public void addEdge(int fromId, int toId, float weight, boolean directed) {
        VisionEdge edge = new VisionEdge(fromId, toId, weight, directed);
        edges.add(edge);

        adjacency.computeIfAbsent(fromId, k -> new ArrayList<>()).add(toId);
        edgeWeights.put(fromId + "->" + toId, weight);

        if (!directed) {
            adjacency.computeIfAbsent(toId, k -> new ArrayList<>()).add(fromId);
            edgeWeights.put(toId + "->" + fromId, weight);
        }
    }

    public void addEdge(int fromId, int toId) {
        VisionNode n1 = getNode(fromId);
        VisionNode n2 = getNode(toId);
        float dist = (n1 != null && n2 != null) ? n1.distanceTo(n2) : 1.0f;
        addEdge(fromId, toId, dist, false);
    }

    public List<Integer> getNeighbors(int nodeId) {
        List<Integer> list = adjacency.get(nodeId);
        return list != null ? list : new ArrayList<>();
    }

    public float getEdgeWeight(int fromId, int toId) {
        Float w = edgeWeights.get(fromId + "->" + toId);
        return w != null ? w : 1.0f;
    }

    public VisionNode getNode(int id) {
        for (VisionNode n : nodes) {
            if (n.id == id) return n;
        }
        return null;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Realistic Real-World Presets (Non-Maze)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Preset 1: Urban Satellite Transit & Delivery Route Network.
     * Demonstrates BFS finding the shortest-hop delivery path across arterial loops
     * vs DFS wandering deep into peripheral suburban cul-de-sacs.
     */
    public static VisionGraph createSatelliteTransitNetwork() {
        VisionGraph g = new VisionGraph("Satellite Transit Grid",
                "Urban logistics and aerial waypoint mesh spanning city terminals, bridges, and bypasses.");

        // Central corridor and arterial hubs
        g.addNode(new VisionNode(0, 0.15f, 0.20f, "North Depot", "terminal", 1.0f));
        g.addNode(new VisionNode(1, 0.40f, 0.18f, "North Expressway Hub", "hub", 0.9f));
        g.addNode(new VisionNode(2, 0.70f, 0.15f, "Aerospace Logistics Port", "terminal", 0.85f));

        g.addNode(new VisionNode(3, 0.20f, 0.45f, "West Industrial Yard", "junction", 0.7f));
        g.addNode(new VisionNode(4, 0.50f, 0.45f, "Central Grand Concourse", "hub", 1.0f));
        g.addNode(new VisionNode(5, 0.80f, 0.42f, "East Harbor Suspension Bridge", "junction", 0.8f));

        g.addNode(new VisionNode(6, 0.12f, 0.75f, "Southwest Rail Terminal", "terminal", 0.75f));
        g.addNode(new VisionNode(7, 0.38f, 0.72f, "South Ring Arterial", "junction", 0.8f));
        g.addNode(new VisionNode(8, 0.65f, 0.78f, "Tech Innovation District", "hub", 0.9f));
        g.addNode(new VisionNode(9, 0.88f, 0.82f, "Oceanic Freight Terminal", "terminal", 1.0f));

        // Peripheral suburban branch nodes (ideal for demonstrating DFS deep branch exploration)
        g.addNode(new VisionNode(10, 0.08f, 0.35f, "Mountain Ridge Outpost", "waypoint", 0.5f));
        g.addNode(new VisionNode(11, 0.28f, 0.32f, "Valley Logistics Sub-Hub", "waypoint", 0.6f));
        g.addNode(new VisionNode(12, 0.62f, 0.30f, "Lakeview Perimeter Waypoint", "waypoint", 0.6f));
        g.addNode(new VisionNode(13, 0.92f, 0.25f, "Coastal Beacon Station", "waypoint", 0.5f));
        g.addNode(new VisionNode(14, 0.50f, 0.92f, "Southern Metro Gateway", "waypoint", 0.65f));

        // Core Arterial Edges
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(0, 3);
        g.addEdge(1, 4);
        g.addEdge(2, 5);
        g.addEdge(3, 4);
        g.addEdge(4, 5);
        g.addEdge(3, 6);
        g.addEdge(4, 7);
        g.addEdge(5, 8);
        g.addEdge(5, 9);
        g.addEdge(6, 7);
        g.addEdge(7, 8);
        g.addEdge(8, 9);
        g.addEdge(7, 14);
        g.addEdge(8, 14);

        // Peripheral branch edges
        g.addEdge(0, 10);
        g.addEdge(10, 3);
        g.addEdge(1, 11);
        g.addEdge(11, 4);
        g.addEdge(2, 12);
        g.addEdge(12, 4);
        g.addEdge(2, 13);
        g.addEdge(13, 5);

        return g;
    }

    /**
     * Preset 2: PCB Microcontroller & Circuit Trace Network.
     * Demonstrates signal propagation, bus topologies, and trace continuity checks.
     */
    public static VisionGraph createPcbCircuitBoardNetwork() {
        VisionGraph g = new VisionGraph("PCB Circuit Trace Bus",
                "High-density circuit traces connecting microprocessor pins, bus transceivers, and filter capacitors.");

        g.addNode(new VisionNode(0, 0.15f, 0.15f, "VCC Power Rail (+3.3V)", "power", 1.0f));
        g.addNode(new VisionNode(1, 0.35f, 0.15f, "Decoupling Cap C1 (100nF)", "filter", 0.8f));
        g.addNode(new VisionNode(2, 0.60f, 0.15f, "Ferrite Bead FB1", "filter", 0.85f));
        g.addNode(new VisionNode(3, 0.85f, 0.15f, "LDO Voltage Regulator", "power", 0.95f));

        g.addNode(new VisionNode(4, 0.20f, 0.38f, "MCU GPIO Pin #4 (SPI_CLK)", "ic_pin", 0.9f));
        g.addNode(new VisionNode(5, 0.50f, 0.38f, "ARM Cortex-M4 Microcontroller Core", "core", 1.0f));
        g.addNode(new VisionNode(6, 0.80f, 0.38f, "Crystal Oscillator (16 MHz)", "clock", 0.9f));

        g.addNode(new VisionNode(7, 0.15f, 0.62f, "Buffer IC Level Shifter", "ic", 0.85f));
        g.addNode(new VisionNode(8, 0.40f, 0.62f, "Pull-up Resistor Array (10kΩ)", "resistor", 0.75f));
        g.addNode(new VisionNode(9, 0.65f, 0.62f, "DMA Hardware Controller", "ic", 0.9f));
        g.addNode(new VisionNode(10, 0.85f, 0.62f, "Flash Memory Module (128MB SPI)", "memory", 0.95f));

        g.addNode(new VisionNode(11, 0.20f, 0.85f, "UART TX/RX Header (JTAG)", "terminal", 0.8f));
        g.addNode(new VisionNode(12, 0.50f, 0.88f, "Digital Ground Plane (GND)", "ground", 1.0f));
        g.addNode(new VisionNode(13, 0.80f, 0.85f, "External Sensor Bus (I2C/SPI)", "sensor", 0.9f));

        // Traces / Bus connections
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(0, 4);
        g.addEdge(1, 5);
        g.addEdge(2, 5);
        g.addEdge(3, 6);
        g.addEdge(4, 5);
        g.addEdge(5, 6);

        g.addEdge(4, 7);
        g.addEdge(5, 8);
        g.addEdge(5, 9);
        g.addEdge(6, 10);
        g.addEdge(7, 8);
        g.addEdge(8, 9);
        g.addEdge(9, 10);

        g.addEdge(7, 11);
        g.addEdge(8, 12);
        g.addEdge(9, 12);
        g.addEdge(10, 13);
        g.addEdge(11, 12);
        g.addEdge(12, 13);

        return g;
    }

    /**
     * Preset 3: Astronomical Constellation & Deep Sky Mesh.
     * Demonstrates visual star pattern connectivity and navigation line-of-sight across the celestial sphere.
     */
    public static VisionGraph createStarConstellationNetwork() {
        VisionGraph g = new VisionGraph("Celestial Stellar Mesh",
                "Navigational stars and visual asterisms connecting Orion, Canis Major, Taurus, and Gemini.");

        g.addNode(new VisionNode(0, 0.25f, 0.18f, "Polaris (North Star)", "nav_star", 1.0f));
        g.addNode(new VisionNode(1, 0.50f, 0.12f, "Capella (Auriga)", "star", 0.85f));
        g.addNode(new VisionNode(2, 0.75f, 0.18f, "Vega (Lyra)", "star", 0.9f));

        g.addNode(new VisionNode(3, 0.18f, 0.40f, "Aldebaran (Taurus Eye)", "star", 0.95f));
        g.addNode(new VisionNode(4, 0.45f, 0.35f, "Betelgeuse (Orion Shoulder)", "supergiant", 1.0f));
        g.addNode(new VisionNode(5, 0.55f, 0.45f, "Alnilam (Orion Belt)", "star", 0.85f));
        g.addNode(new VisionNode(6, 0.78f, 0.40f, "Deneb (Cygnus Swan)", "star", 0.88f));

        g.addNode(new VisionNode(7, 0.32f, 0.60f, "Rigel (Orion Foot)", "supergiant", 0.95f));
        g.addNode(new VisionNode(8, 0.60f, 0.62f, "Sirius (Dog Star - Brightest)", "luminary", 1.0f));
        g.addNode(new VisionNode(9, 0.82f, 0.65f, "Altair (Aquila Eagle)", "star", 0.85f));

        g.addNode(new VisionNode(10, 0.20f, 0.82f, "Canopus (Carina)", "star", 0.9f));
        g.addNode(new VisionNode(11, 0.50f, 0.85f, "Procyon (Canis Minor)", "star", 0.88f));
        g.addNode(new VisionNode(12, 0.80f, 0.88f, "Antares (Scorpius Heart)", "supergiant", 0.95f));

        // Asterism alignment lines
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(0, 3);
        g.addEdge(1, 4);
        g.addEdge(2, 6);
        g.addEdge(3, 4);
        g.addEdge(4, 5);
        g.addEdge(5, 7);
        g.addEdge(5, 8);
        g.addEdge(6, 9);
        g.addEdge(4, 11);
        g.addEdge(7, 10);
        g.addEdge(7, 8);
        g.addEdge(8, 11);
        g.addEdge(8, 12);
        g.addEdge(9, 12);
        g.addEdge(10, 11);
        g.addEdge(11, 12);

        return g;
    }

    /**
     * Preset 4: Biological Neuronal & Cell Cluster Network.
     * Demonstrates signal cascades, synaptic connectivity, and cell cluster reachability.
     */
    public static VisionGraph createBiologicalCellNetwork() {
        VisionGraph g = new VisionGraph("Neuronal Synapse Mesh",
                "Cortical pyramidal neurons, dendritic branches, and synaptic junctions in neural tissue.");

        g.addNode(new VisionNode(0, 0.15f, 0.25f, "Sensory Receptor Neuron S1", "sensory", 1.0f));
        g.addNode(new VisionNode(1, 0.38f, 0.20f, "Interneuron Alpha Junction", "interneuron", 0.8f));
        g.addNode(new VisionNode(2, 0.65f, 0.18f, "Pyramidal Cell Soma A", "pyramidal", 0.95f));
        g.addNode(new VisionNode(3, 0.85f, 0.25f, "Inhibitory GABAergic Interneuron", "inhibitory", 0.85f));

        g.addNode(new VisionNode(4, 0.25f, 0.50f, "Dendritic Spine Cluster D1", "dendrite", 0.75f));
        g.addNode(new VisionNode(5, 0.50f, 0.48f, "Central Synaptic Cleft #1", "synapse", 1.0f));
        g.addNode(new VisionNode(6, 0.75f, 0.52f, "Axon Hillock Action Trigger", "axon", 0.9f));

        g.addNode(new VisionNode(7, 0.18f, 0.78f, "Glial Astrocytic Bridge", "glia", 0.7f));
        g.addNode(new VisionNode(8, 0.42f, 0.75f, "Post-Synaptic Density PSD-95", "post_synaptic", 0.85f));
        g.addNode(new VisionNode(9, 0.68f, 0.78f, "Myelinated Axon Node of Ranvier", "axon_node", 0.9f));
        g.addNode(new VisionNode(10, 0.88f, 0.82f, "Effector Motor Neuron Terminal", "motor", 1.0f));

        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(0, 4);
        g.addEdge(1, 5);
        g.addEdge(2, 5);
        g.addEdge(3, 6);
        g.addEdge(4, 5);
        g.addEdge(5, 6);
        g.addEdge(4, 7);
        g.addEdge(5, 8);
        g.addEdge(6, 9);
        g.addEdge(6, 10);
        g.addEdge(7, 8);
        g.addEdge(8, 9);
        g.addEdge(9, 10);

        return g;
    }

    /**
     * Extracts a topological vision graph from actual image features / keypoint centroids.
     */
    public static VisionGraph fromImageKeypoints(Bitmap bmp, int maxNodes) {
        if (bmp == null) return createSatelliteTransitNetwork();

        VisionGraph g = new VisionGraph("Image Feature Topology",
                "Feature keypoints and spatial proximity edges extracted directly from image luminance.");

        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int stepX = Math.max(1, w / 8);
        int stepY = Math.max(1, h / 8);

        int id = 0;
        // Divide image into spatial cells and pick highest contrast / luminance variation point in each cell
        for (int y = stepY / 2; y < h - stepY / 4 && id < maxNodes; y += stepY) {
            for (int x = stepX / 2; x < w - stepX / 4 && id < maxNodes; x += stepX) {
                int pixel = bmp.getPixel(x, y);
                int r = Color.red(pixel);
                int gVal = Color.green(pixel);
                int b = Color.blue(pixel);
                float lum = (0.299f * r + 0.587f * gVal + 0.114f * b) / 255f;

                float relX = (float) x / w;
                float relY = (float) y / h;
                String label = "P" + id + String.format(" [%.0f%%]", lum * 100f);
                g.addNode(new VisionNode(id++, relX, relY, label, "feature", lum));
            }
        }

        // Connect nearest neighbors within proximity threshold
        List<VisionNode> nodeList = g.getNodes();
        float maxDistanceThreshold = 0.35f;

        for (int i = 0; i < nodeList.size(); i++) {
            VisionNode n1 = nodeList.get(i);
            int connections = 0;
            // Connect to up to 3 closest neighbors
            List<Integer> closest = new ArrayList<>();
            for (int j = 0; j < nodeList.size(); j++) {
                if (i == j) continue;
                VisionNode n2 = nodeList.get(j);
                float dist = n1.distanceTo(n2);
                if (dist <= maxDistanceThreshold) {
                    closest.add(j);
                }
            }

            // Sort by proximity
            closest.sort((a, b) -> Float.compare(n1.distanceTo(nodeList.get(a)), n1.distanceTo(nodeList.get(b))));
            for (int k = 0; k < Math.min(3, closest.size()); k++) {
                int targetIdx = closest.get(k);
                g.addEdge(n1.id, nodeList.get(targetIdx).id);
            }
        }

        return g;
    }
}
