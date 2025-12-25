package com.example.producuctionLine.controller;

import com.example.producuctionLine.model.Queue;
import com.example.producuctionLine.service.SimulationManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * REST Controller for Queue operations
 */
@RestController
@RequestMapping("/api/queue")
@CrossOrigin(origins = "http://localhost:4200")
public class QueueController {
    
    private final SimulationManager manager = SimulationManager.getInstance();
    
    /**
     * Create new queue
     * POST /api/queue
     * Body: { "x": 100, "y": 200 }
     */
    @PostMapping
    public ResponseEntity<Queue> createQueue(@RequestBody Map<String, Double> request) {
        double x = request.getOrDefault("x", 100.0);
        double y = request.getOrDefault("y", 100.0);
        
        Queue queue = manager.addQueue(x, y);
        return ResponseEntity.ok(queue);
    }
    
    /**
     * Get all queues
     * GET /api/queue
     */
    @GetMapping
    public ResponseEntity<Collection<Queue>> getAllQueues() {
        return ResponseEntity.ok(manager.getQueues().values());
    }
    
    /**
     * Get queue by ID
     * GET /api/queue/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Queue> getQueue(@PathVariable String id) {
        Queue queue = manager.getQueue(id);
        if (queue == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(queue);
    }
    
    /**
     * Update queue position
     * PUT /api/queue/{id}/position
     * Body: { "x": 150, "y": 250 }
     */
    @PutMapping("/{id}/position")
    public ResponseEntity<Queue> updatePosition(
            @PathVariable String id,
            @RequestBody Map<String, Double> position) {
        
        Queue queue = manager.getQueue(id);
        if (queue == null) {
            return ResponseEntity.notFound().build();
        }
        
        queue.setX(position.getOrDefault("x", queue.getX()));
        queue.setY(position.getOrDefault("y", queue.getY()));
        
        System.out.println("📍 Queue " + id + " moved to (" + 
                          queue.getX() + ", " + queue.getY() + ")");
        
        return ResponseEntity.ok(queue);
    }
    
    /**
     * Delete queue
     * DELETE /api/queue/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteQueue(@PathVariable String id) {
        manager.removeQueue(id);
        return ResponseEntity.ok(Map.of(
            "message", "Queue deleted",
            "id", id
        ));
    }
}