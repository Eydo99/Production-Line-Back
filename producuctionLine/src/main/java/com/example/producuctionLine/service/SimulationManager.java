package com.example.producuctionLine.service;

import com.example.producuctionLine.model.Connection;
import com.example.producuctionLine.model.Queue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton class to manage entire simulation
 * Coordinates all queues, machines, and connections
 */
@Service
public class SimulationManager {
    private static SimulationManager instance;
    
    // Thread-safe collections
    private final Map<String, Queue> queues = new ConcurrentHashMap<>();
    private final List<Connection> connections = new ArrayList<>();
    
    private volatile boolean isRunning = false;
    
    // Private constructor for Singleton
    private SimulationManager() {}
    
    /**
     * Get singleton instance
     */
    public static synchronized SimulationManager getInstance() {
        if (instance == null) {
            instance = new SimulationManager();
        }
        return instance;
    }
    
    // ========== QUEUE MANAGEMENT ==========
    
    /**
     * Add new queue to simulation
     */
    public Queue addQueue(double x, double y) {
        String id = "Q" + (queues.size() + 1);
        Queue queue = new Queue(id, x, y);
        queues.put(id, queue);
        System.out.println("🆕 Queue created: " + id + " at (" + x + ", " + y + ")");
        return queue;
    }
    
    /**
     * Get queue by ID
     */
    public Queue getQueue(String id) {
        return queues.get(id);
    }
    
    /**
     * Get all queues
     */
    public Map<String, Queue> getQueues() {
        return queues;
    }
    
    /**
     * Remove queue
     */
    public void removeQueue(String id) {
        Queue removed = queues.remove(id);
        if (removed != null) {
            System.out.println("🗑️ Queue removed: " + id);
        }
    }
    
    // ========== CONNECTION MANAGEMENT ==========
    
    /**
     * Create connection between nodes
     */
    public Connection createConnection(String fromId, String toId) {
        // Validate connection exists
        if (!queues.containsKey(fromId) && !queues.containsKey(toId)) {
            throw new IllegalArgumentException("Invalid connection nodes");
        }
        
        Connection connection = new Connection(fromId, toId);
        connections.add(connection);
        
        System.out.println("🔗 Connection created: " + fromId + " → " + toId);
        return connection;
    }
    
    /**
     * Get all connections
     */
    public List<Connection> getConnections() {
        return connections;
    }
    
    // ========== SIMULATION CONTROL ==========
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void setRunning(boolean running) {
        this.isRunning = running;
    }
    
    /**
     * Clear all simulation data
     */
    public void clearSimulation() {
        queues.clear();
        connections.clear();
        isRunning = false;
        System.out.println("🧹 Simulation cleared");
    }
}