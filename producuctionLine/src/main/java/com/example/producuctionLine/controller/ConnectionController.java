package com.example.producuctionLine.controller;

import com.example.producuctionLine.model.Connection;
import com.example.producuctionLine.service.SimulationManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Connection operations
 */
@RestController
@RequestMapping("/api/connection")
@CrossOrigin(origins = "http://localhost:4200")
public class ConnectionController {
    
    private final SimulationManager manager = SimulationManager.getInstance();
    
    /**
     * Create connection between nodes
     * POST /api/connection
     * Body: { "fromId": "Q1", "toId": "M1" }
     */
   @PostMapping
public ResponseEntity<?> createConnection(@RequestBody Map<String, String> request) {
    try {
        String fromId = request.get("fromId");
        String toId = request.get("toId");
        
        if (fromId == null || toId == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Missing fromId or toId")
            );
        }
        
        Connection connection = manager.createConnection(fromId, toId);
        
        // ✅ Broadcast connection creation immediately
        if (manager.getBroadcaster() != null) {
            var fromNode = manager.getNodePosition(fromId);
            var toNode = manager.getNodePosition(toId);
            
            manager.getBroadcaster().broadcastConnectionUpdate(
                new com.example.producuctionLine.dto.ConnectionUpdateDTO(
                    connection.getId(),
                    fromId,
                    toId,
                    fromNode.get("x"),
                    fromNode.get("y"),
                    toNode.get("x"),
                    toNode.get("y"),
                    "created"
                )
            );
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Connection created",
            "connection", connection
        ));
        
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(
            Map.of("error", e.getMessage())
        );
    }
}
    
    /**
     * Get all connections
     * GET /api/connection
     */
    @GetMapping
    public ResponseEntity<List<Connection>> getAllConnections() {
        return ResponseEntity.ok(manager.getConnections());
    }
}