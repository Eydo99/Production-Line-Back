package com.example.producuctionLine.service;

import com.example.producuctionLine.model.Connection;
import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.model.Product;
import com.example.producuctionLine.model.Queue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    
    
    // Counters for IDs
    private int queueCounter = 0;
    private int machineCounter = 0;
    
    // Private constructor for Singleton
    private SimulationManager() {}
    
    /**
     * Get singleton instance
     */


    private ExecutorService machineExecutor;
private Thread productGeneratorThread;
private volatile boolean isRunning = false;

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



    public void startSimulation() {
    if (isRunning) {
        throw new IllegalStateException("Simulation already running");
    }
    
    if (queues.isEmpty()) {
        throw new IllegalStateException("Add at least one queue before starting");
    }
    
    isRunning = true;
    
    // Start all machines as threads
    machineExecutor = Executors.newCachedThreadPool();
    for (Machine machine : machines.values()) {
        machineExecutor.submit(() -> {
            while (isRunning) {
                try {
                    if (machine.isReady() && machine.getInputQueue() != null) {
                        Product product = machine.getInputQueue().dequeue();
                        if (product != null) {
                            processMachineProduct(machine, product);
                        }
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    // Start product generator
    productGeneratorThread = new Thread(() -> {
        Random random = new Random();
        while (isRunning) {
            try {
                Thread.sleep(1000 + random.nextInt(2000)); // 1-3 seconds
                
                // Generate product at first queue
                if (!queues.isEmpty()) {
                    Queue firstQueue = queues.values().iterator().next();
                    Product product = new Product();
                    firstQueue.enqueue(product);
                    System.out.println("🆕 Generated product: " + product.getId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    });
    productGeneratorThread.start();
    
    System.out.println("▶️ Simulation STARTED");
}

public void stopSimulation() {
    isRunning = false;
    
    if (machineExecutor != null) {
        machineExecutor.shutdownNow();
    }
    
    if (productGeneratorThread != null) {
        productGeneratorThread.interrupt();
    }
    
    System.out.println("⏸️ Simulation STOPPED");
}

private void processMachineProduct(Machine machine, Product product) {
    try {
        machine.setReady(false);
        machine.setColor(product.getColor());
        machine.setCurrentProduct(product);
        
        System.out.println("⚙️ " + machine.getName() + " processing " + product.getId());
        
        // Broadcast machine status
        // Person 4 will implement WebSocket here
        
        Thread.sleep(machine.getServiceTime());
        
        // Flash effect
        System.out.println("✨ " + machine.getName() + " finished!");
        
        // Move to output queue
        if (machine.getOutputQueue() != null) {
            machine.getOutputQueue().enqueue(product);
        }
        
        machine.setCurrentProduct(null);
        machine.setColor(machine.getDefaultColor());
        machine.setReady(true);
        
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
}