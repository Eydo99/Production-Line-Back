package com.example.producuctionLine.controller;

import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.service.SimulationManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * REST Controller for Machine operations
 */
@RestController
@RequestMapping("/api/machine")
@CrossOrigin(origins = "http://localhost:4200")
public class MachineController {
    
    private final SimulationManager manager = SimulationManager.getInstance();
    
    /**
     * Create new machine
     * POST /api/machine
     * Body: { "x": 100, "y": 200 }
     */
    @PostMapping
    public ResponseEntity<Machine> createMachine(@RequestBody Map<String, Double> request) {
        double x = request.getOrDefault("x", 100.0);
        double y = request.getOrDefault("y", 100.0);
        
        Machine machine = manager.addMachine(x, y);
        return ResponseEntity.ok(machine);
    }
    
    /**
     * Get all machines
     * GET /api/machine
     */
    @GetMapping
    public ResponseEntity<Collection<Machine>> getAllMachines() {
        return ResponseEntity.ok(manager.getMachines().values());
    }
    
    /**
     * Get machine by ID
     * GET /api/machine/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Machine> getMachine(@PathVariable String id) {
        Machine machine = manager.getMachine(id);
        if (machine == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(machine);
    }
    
    /**
     * Update machine position
     * PUT /api/machine/{id}/position
     * Body: { "x": 150, "y": 250 }
     */
    @PutMapping("/{id}/position")
    public ResponseEntity<Machine> updatePosition(
            @PathVariable String id,
            @RequestBody Map<String, Double> position) {
        
        Machine machine = manager.getMachine(id);
        if (machine == null) {
            return ResponseEntity.notFound().build();
        }
        
        machine.setX(position.getOrDefault("x", machine.getX()));
        machine.setY(position.getOrDefault("y", machine.getY()));
        
        System.out.println("⚙️ Machine " + id + " moved to (" + 
                          machine.getX() + ", " + machine.getY() + ")");
        
        return ResponseEntity.ok(machine);
    }
    
    /**
     * Update machine service time
     * PUT /api/machine/{id}/servicetime
     * Body: { "serviceTime": 3000 }
     */
    @PutMapping("/{id}/servicetime")
    public ResponseEntity<Machine> updateServiceTime(
            @PathVariable String id,
            @RequestBody Map<String, Integer> request) {
        
        Machine machine = manager.getMachine(id);
        if (machine == null) {
            return ResponseEntity.notFound().build();
        }
        
        int serviceTime = request.getOrDefault("serviceTime", machine.getServiceTime());
        machine.setServiceTime(serviceTime);
        
        System.out.println("⏱️ Machine " + id + " service time updated to " + serviceTime + "ms");
        
        return ResponseEntity.ok(machine);
    }
    
    /**
     * Get machine status
     * GET /api/machine/{id}/status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getMachineStatus(@PathVariable String id) {
        Machine machine = manager.getMachine(id);
        if (machine == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "id", machine.getName(),
            "status", machine.getStatus(),
            "isReady", machine.isReady(),
            "color", machine.getColor(),
            "serviceTime", machine.getServiceTime(),
            "hasCurrentProduct", machine.getCurrentProduct() != null
        ));
    }
    
    /**
     * Delete machine
     * DELETE /api/machine/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMachine(@PathVariable String id) {
        Machine machine = manager.getMachine(id);
        if (machine == null) {
            return ResponseEntity.notFound().build();
        }
        
        manager.removeMachine(id);
        return ResponseEntity.ok(Map.of(
            "message", "Machine deleted",
            "id", id
        ));
    }
}