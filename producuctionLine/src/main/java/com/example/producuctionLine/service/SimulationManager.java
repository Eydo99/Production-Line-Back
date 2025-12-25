package com.example.producuctionLine.service;

import com.example.producuctionLine.model.Connection;
import com.example.producuctionLine.model.Machine;
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
    private final Map<String, Machine> machines = new ConcurrentHashMap<>();
    private final List<Connection> connections = new ArrayList<>();
    
    private volatile boolean isRunning = false;
    
    // Counters for IDs
    private int queueCounter = 0;
    private int machineCounter = 0;
    
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
        queueCounter++;
        String id = "Q" + queueCounter;
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
    
    // ========== MACHINE MANAGEMENT ==========
    
    /**
     * Add new machine to simulation
     */
    public Machine addMachine(double x, double y) {
        machineCounter++;
        String id = "M" + machineCounter;
        Machine machine = new Machine(id, machineCounter, x, y);
        machines.put(id, machine);
        System.out.println("🆕 Machine created: " + id + " at (" + x + ", " + y + ")");
        return machine;
    }
    
    /**
     * Get machine by ID
     */
    public Machine getMachine(String id) {
        return machines.get(id);
    }
    
    /**
     * Get all machines
     */
    public Map<String, Machine> getMachines() {
        return machines;
    }
    
    /**
     * Remove machine
     */
    public void removeMachine(String id) {
        Machine removed = machines.remove(id);
        if (removed != null) {
            System.out.println("🗑️ Machine removed: " + id);
        }
    }
    
    // ========== CONNECTION MANAGEMENT ==========
    
    /**
     * Create connection between nodes
     * Validates Q→M→Q pattern
     */
    public Connection createConnection(String fromId, String toId) {
        // Validate nodes exist
        boolean fromExists = queues.containsKey(fromId) || machines.containsKey(fromId);
        boolean toExists = queues.containsKey(toId) || machines.containsKey(toId);
        
        if (!fromExists || !toExists) {
            throw new IllegalArgumentException("One or both nodes do not exist");
        }
        
        // Validate Q→M or M→Q pattern
        char fromType = fromId.charAt(0);
        char toType = toId.charAt(0);
        
        if (!((fromType == 'Q' && toType == 'M') || (fromType == 'M' && toType == 'Q'))) {
            throw new IllegalArgumentException("Invalid connection pattern. Must be Q→M or M→Q");
        }
        
        // Create connection
        Connection connection = new Connection(fromId, toId);
        connections.add(connection);
        
        // Wire up the actual objects
        if (fromType == 'Q' && toType == 'M') {
            // Queue → Machine
            Queue queue = queues.get(fromId);
            Machine machine = machines.get(toId);
            
            machine.setInputQueue(queue);
            queue.registerObserver(machine); // Machine observes queue
            
            System.out.println("🔗 Connected: Queue " + fromId + " → Machine " + toId);
            
        } else if (fromType == 'M' && toType == 'Q') {
            // Machine → Queue
            Machine machine = machines.get(fromId);
            Queue queue = queues.get(toId);
            
            machine.setOutputQueue(queue);
            
            System.out.println("🔗 Connected: Machine " + fromId + " → Queue " + toId);
        }
        
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
        machines.clear();
        connections.clear();
        queueCounter = 0;
        machineCounter = 0;
        isRunning = false;
        System.out.println("🧹 Simulation cleared");
    }
}