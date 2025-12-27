package com.example.producuctionLine.controller;

import com.example.producuctionLine.service.SimulationManager;
import com.example.producuctionLine.model.snapshot.SimulationSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Simulation control
 * Handles start, stop, pause, resume, status endpoints
 */
@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "http://localhost:4200")
public class SimulationController {

    private final SimulationManager manager = SimulationManager.getInstance();

    /**
     * Start the simulation
     * POST /api/simulation/start
     */
    @PostMapping("/start")
    public ResponseEntity<?> startSimulation() {
        try {
            manager.startSimulation();
            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "message", "Simulation started successfully",
                    "isRunning", true));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Internal server error: " + e.getMessage(),
                    "status", "error"));
        }
    }

    /**
     * Stop the simulation
     * POST /api/simulation/stop
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopSimulation() {
        try {
            manager.stopSimulation();
            return ResponseEntity.ok(Map.of(
                    "status", "stopped",
                    "message", "Simulation stopped successfully",
                    "isRunning", false));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"));
        }
    }

    /**
     * Pause the simulation
     * POST /api/simulation/pause
     */
    @PostMapping("/pause")
    public ResponseEntity<?> pauseSimulation() {
        try {
            manager.pauseSimulation();
            return ResponseEntity.ok(Map.of(
                    "status", "paused",
                    "message", "Simulation paused successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"));
        }
    }

    /**
     * Resume the simulation
     * POST /api/simulation/resume
     */
    @PostMapping("/resume")
    public ResponseEntity<?> resumeSimulation() {
        try {
            manager.resumeSimulation();
            return ResponseEntity.ok(Map.of(
                    "status", "resumed",
                    "message", "Simulation resumed successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"));
        }
    }

    /**
     * Get simulation status and statistics
     * GET /api/simulation/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            Map<String, Object> stats = manager.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }

    /**
     * Clear/Reset simulation (stops and clears all data)
     * POST /api/simulation/clear
     */
    @PostMapping("/clear")
    public ResponseEntity<?> clearSimulation() {
        try {
            manager.clearSimulation();
            return ResponseEntity.ok(Map.of(
                    "status", "cleared",
                    "message", "Simulation cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }

    /**
     * Get simulation statistics
     * GET /api/simulation/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            return ResponseEntity.ok(Map.of(
                    "totalGenerated", manager.getTotalProductsGenerated(),
                    "totalProcessed", manager.getTotalProductsProcessed(),
                    "duration", manager.getSimulationDuration(),
                    "avgQueueLength", manager.getAverageQueueLength(),
                    "isRunning", manager.isRunning(),
                    "isPaused", manager.isPaused()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }

    // ========== SNAPSHOT (Memento Pattern) ENDPOINTS ==========

    /**
     * Save current simulation state as a snapshot
     * POST /api/simulation/snapshot
     */
    @PostMapping("/snapshot")
    public ResponseEntity<?> saveSnapshot() {
        try {
            var snapshot = manager.createSnapshot();
            return ResponseEntity.ok(Map.of(
                    "status", "saved",
                    "message", "Snapshot saved successfully",
                    "timestamp", snapshot.getTimestamp(),
                    "queues", snapshot.getQueueSnapshots().size(),
                    "machines", snapshot.getMachineSnapshots().size(),
                    "connections", snapshot.getConnectionSnapshots().size()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"));
        }
    }

    /**
     * Check if a snapshot exists for replay
     * GET /api/simulation/snapshot
     */
    @GetMapping("/snapshot")
    public ResponseEntity<?> hasSnapshot() {
        try {
            boolean hasSnapshot = manager.hasSnapshot();
            SimulationSnapshot snapshot = manager.getLastSnapshot();

            if (hasSnapshot && snapshot != null) {
                return ResponseEntity.ok(Map.of(
                        "hasSnapshot", true,
                        "timestamp", snapshot.getTimestamp(),
                        "queues", snapshot.getQueueSnapshots().size(),
                        "machines", snapshot.getMachineSnapshots().size(),
                        "connections", snapshot.getConnectionSnapshots().size()));
            } else {
                return ResponseEntity.ok(Map.of(
                        "hasSnapshot", false,
                        "message", "No snapshot available"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }

    /**
     * Replay simulation from the last saved snapshot
     * Restores state, starts simulation, and auto-stops after the same duration
     * POST /api/simulation/replay
     */
    @PostMapping("/replay")
    public ResponseEntity<?> replaySimulation() {
        try {
            if (!manager.hasSnapshot()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No snapshot available for replay",
                        "status", "error"));
            }

            SimulationSnapshot snapshot = manager.getLastSnapshot();
            long duration = snapshot.getSimulationDuration();

            // Restore from snapshot
            manager.restoreFromSnapshot(snapshot);

            // Set up deterministic replay mode
            manager.setupReplayMode(snapshot);

            // Start the simulation
            manager.startSimulation();

            // Schedule auto-stop after the same duration
            if (duration > 0) {
                new Thread(() -> {
                    try {
                        System.out.println("⏱️ Replay will auto-stop in " + (duration / 1000) + " seconds");
                        Thread.sleep(duration);
                        if (manager.isRunning()) {
                            manager.stopSimulation();
                            System.out.println("⏹️ Replay auto-stopped after " + (duration / 1000) + " seconds");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "Replay-AutoStop").start();
            }

            return ResponseEntity.ok(Map.of(
                    "status", "replaying",
                    "message", "Simulation replay started. Will auto-stop after " + (duration / 1000) + " seconds.",
                    "duration", duration,
                    "queues", manager.getQueues().size(),
                    "machines", manager.getMachines().size()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"));
        }
    }
}
