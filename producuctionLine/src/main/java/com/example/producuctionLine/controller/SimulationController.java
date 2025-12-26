package com.example.producuctionLine.controller;

import com.example.producuctionLine.service.SimulationManager;
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
                    "isRunning", true
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Internal server error: " + e.getMessage(),
                    "status", "error"
            ));
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
                    "isRunning", false
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"
            ));
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
                    "message", "Simulation paused successfully"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"
            ));
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
                    "message", "Simulation resumed successfully"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "status", "error"
            ));
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
                    "error", e.getMessage()
            ));
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
                    "message", "Simulation cleared successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
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
                    "isPaused", manager.isPaused()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}