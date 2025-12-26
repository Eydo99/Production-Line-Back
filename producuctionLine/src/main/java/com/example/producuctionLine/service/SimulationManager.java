package com.example.producuctionLine.service;

import com.example.producuctionLine.dto.MachineUpdateDTO;
import com.example.producuctionLine.dto.QueueUpdateDTO;
import com.example.producuctionLine.model.Connection;
import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.model.Product;
import com.example.producuctionLine.model.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Singleton class to manage entire simulation
 * Coordinates all queues, machines, and connections
 * Implements Concurrency Design Pattern
 *
 * @author Person 3 - Simulation Control
 */
@Service
public class SimulationManager {

    // ========== SINGLETON INSTANCE ==========
    private static SimulationManager instance;

    // ========== WEBSOCKET BROADCASTER (PERSON 4) ==========
    @Autowired
    private WebSocketBroadcaster broadcaster; // ✅ ADDED - Inject WebSocket broadcaster

    // ========== SIMULATION STATE ==========
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;

    // ========== DATA COLLECTIONS (Thread-safe) ==========
    private final Map<String, Queue> queues = new ConcurrentHashMap<>();
    private final Map<String, Machine> machines = new ConcurrentHashMap<>();
    private final List<Connection> connections = new CopyOnWriteArrayList<>();

    // ========== THREADING ==========
    private ExecutorService machineExecutor;
    private Thread productGeneratorThread;
    private final Map<String, Future<?>> machineFutures = new ConcurrentHashMap<>();

    // ========== STATISTICS ==========
    private int totalProductsGenerated = 0;
    private int totalProductsProcessed = 0;
    private long simulationStartTime = 0;

    // ========== COUNTERS ==========
    private int queueCounter = 0;
    private int machineCounter = 0;

    // ========== CONFIGURATION ==========
    private static final int MIN_PRODUCT_DELAY = 1000; // 1 second
    private static final int MAX_PRODUCT_DELAY = 3000; // 3 seconds
    private static final int THREAD_POOL_SIZE = 10;

    /**
     * Private constructor for Singleton pattern
     */
    private SimulationManager() {
        System.out.println("🏗️ SimulationManager initialized");
    }

    /**
     * Get singleton instance (Thread-safe)
     */
    public static synchronized SimulationManager getInstance() {
        if (instance == null) {
            instance = new SimulationManager();
        }
        return instance;
    }

    // ========================================================================
    // QUEUE MANAGEMENT
    // ========================================================================

    /**
     * Add new queue to simulation
     * @param x X coordinate
     * @param y Y coordinate
     * @return Created queue
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
     * Remove queue and clean up connections
     */
    public void removeQueue(String id) {
        Queue removed = queues.remove(id);
        if (removed != null) {
            // Remove all connections involving this queue
            connections.removeIf(conn ->
                    conn.getFromId().equals(id) || conn.getToId().equals(id)
            );

            System.out.println("🗑️ Queue removed: " + id);
        }
    }

    // ========================================================================
    // MACHINE MANAGEMENT
    // ========================================================================

    /**
     * Add new machine to simulation
     * @param x X coordinate
     * @param y Y coordinate
     * @return Created machine
     */
    public Machine addMachine(double x, double y) {
        machineCounter++;
        String id = "M" + machineCounter;
        Machine machine = new Machine(id, machineCounter, x, y);
        machines.put(id, machine);

        System.out.println("🆕 Machine created: " + id + " at (" + x + ", " + y + ")");

        // If simulation is running, start this machine's thread
        if (isRunning) {
            startMachineThread(machine);
        }

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
     * Remove machine and clean up
     */
    public void removeMachine(String id) {
        Machine removed = machines.remove(id);
        if (removed != null) {
            // Stop machine thread if running
            Future<?> future = machineFutures.remove(id);
            if (future != null) {
                future.cancel(true);
            }

            // Remove all connections involving this machine
            connections.removeIf(conn ->
                    conn.getFromId().equals(id) || conn.getToId().equals(id)
            );

            // Unregister from queues
            if (removed.getInputQueue() != null) {
                removed.getInputQueue().unregisterObserver(removed);
            }

            System.out.println("🗑️ Machine removed: " + id);
        }
    }

    // ========================================================================
    // CONNECTION MANAGEMENT
    // ========================================================================

    /**
     * Create connection between nodes
     * Validates Q→M→Q pattern
     * Implements Observer Pattern wiring
     *
     * @param fromId Source node ID (Queue or Machine)
     * @param toId Target node ID (Queue or Machine)
     * @return Created connection
     * @throws IllegalArgumentException if connection is invalid
     */
    public Connection createConnection(String fromId, String toId) {
        // Validate nodes exist
        boolean fromExists = queues.containsKey(fromId) || machines.containsKey(fromId);
        boolean toExists = queues.containsKey(toId) || machines.containsKey(toId);

        if (!fromExists) {
            throw new IllegalArgumentException("Source node '" + fromId + "' does not exist");
        }
        if (!toExists) {
            throw new IllegalArgumentException("Target node '" + toId + "' does not exist");
        }

        // Validate Q→M or M→Q pattern (no Q→Q or M→M)
        char fromType = fromId.charAt(0);
        char toType = toId.charAt(0);

        if (fromType == toType) {
            throw new IllegalArgumentException(
                    "Invalid connection: Cannot connect " + fromType + " to " + toType +
                            ". Must alternate between Queue and Machine (Q→M or M→Q)"
            );
        }

        // Create connection object
        Connection connection = new Connection(fromId, toId);

        // Check if connection already exists
        boolean exists = connections.stream()
                .anyMatch(c -> c.getFromId().equals(fromId) && c.getToId().equals(toId));

        if (exists) {
            throw new IllegalArgumentException("Connection already exists");
        }

        connections.add(connection);

        // Wire up the actual objects (Observer Pattern)
        if (fromType == 'Q' && toType == 'M') {
            // Queue → Machine
            Queue queue = queues.get(fromId);
            Machine machine = machines.get(toId);

            machine.setInputQueue(queue);
            queue.registerObserver(machine); // Machine observes queue (Observer Pattern)

            System.out.println("🔗 Connected: Queue " + fromId + " → Machine " + toId);
            System.out.println("   Observer Pattern: " + toId + " now observes " + fromId);

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
        return new ArrayList<>(connections);
    }

    /**
     * Delete a connection
     */
    public void deleteConnection(String fromId, String toId) {
        connections.removeIf(conn ->
                conn.getFromId().equals(fromId) && conn.getToId().equals(toId)
        );

        // Unwire objects
        char fromType = fromId.charAt(0);
        char toType = toId.charAt(0);

        if (fromType == 'Q' && toType == 'M') {
            Queue queue = queues.get(fromId);
            Machine machine = machines.get(toId);
            if (queue != null && machine != null) {
                queue.unregisterObserver(machine);
                machine.setInputQueue(null);
            }
        } else if (fromType == 'M' && toType == 'Q') {
            Machine machine = machines.get(fromId);
            if (machine != null) {
                machine.setOutputQueue(null);
            }
        }

        System.out.println("🔌 Connection deleted: " + fromId + " → " + toId);
    }

    // ========================================================================
    // SIMULATION CONTROL (Concurrency Pattern)
    // ========================================================================

    /**
     * Start the simulation
     * Implements Concurrency Design Pattern:
     * - Each machine runs on its own thread
     * - Product generator runs on separate thread
     * - Thread-safe operations throughout
     *
     * @throws IllegalStateException if simulation already running or no queues exist
     */
    public synchronized void startSimulation() {
        if (isRunning) {
            throw new IllegalStateException("Simulation is already running");
        }

        if (queues.isEmpty()) {
            throw new IllegalStateException("Cannot start simulation: No queues exist. Add at least one queue.");
        }

        if (machines.isEmpty()) {
            throw new IllegalStateException("Cannot start simulation: No machines exist. Add at least one machine.");
        }

        // Validate at least one complete path exists
        if (!hasValidPath()) {
            System.out.println("⚠️ Warning: No complete Q→M→Q path exists. Products may get stuck.");
        }

        // Reset statistics
        totalProductsGenerated = 0;
        totalProductsProcessed = 0;
        simulationStartTime = System.currentTimeMillis();

        // Set running flag
        isRunning = true;
        isPaused = false;

        // Create thread pool for machines
        machineExecutor = Executors.newFixedThreadPool(
                Math.max(THREAD_POOL_SIZE, machines.size()),
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "MachineThread-" + (++counter));
                        t.setDaemon(true);
                        return t;
                    }
                }
        );

        // Start all machine threads (Concurrency Pattern)
        for (Machine machine : machines.values()) {
            Future<?> future = machineExecutor.submit(() -> runMachineLoop(machine));
            machineFutures.put(machine.getName(), future);
            System.out.println("🚀 Started thread for " + machine.getName());
        }

        // Start product generator thread
        productGeneratorThread = new Thread(() -> runProductGenerator(), "ProductGenerator");
        productGeneratorThread.setDaemon(true);
        productGeneratorThread.start();

        System.out.println("▶️  SIMULATION STARTED");
        System.out.println("   Machines: " + machines.size() + " (each on separate thread)");
        System.out.println("   Queues: " + queues.size());
        System.out.println("   Connections: " + connections.size());
    }

    /**
     * Machine processing loop (runs on separate thread for each machine)
     * This is the core of the Concurrency Pattern
     */
    private void runMachineLoop(Machine machine) {
        System.out.println("🏁 " + machine.getName() + " thread started");

        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // Skip if paused
                if (isPaused) {
                    Thread.sleep(100);
                    continue;
                }

                // If machine is ready and has input queue, check for products
                if (machine.isReady() && machine.getInputQueue() != null) {
                    Queue inputQueue = machine.getInputQueue();

                    // Try to get a product
                    if (!inputQueue.isEmpty()) {
                        Product product = inputQueue.dequeue();

                        if (product != null) {
                            processProductOnMachine(machine, product);
                        }
                    } else {
                        // No products available, register as observer
                        inputQueue.registerObserver(machine);
                    }
                }

                // Small sleep to prevent busy-waiting
                Thread.sleep(50);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("⏹️  " + machine.getName() + " thread interrupted");
                break;
            } catch (Exception e) {
                System.err.println("❌ Error in " + machine.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("🛑 " + machine.getName() + " thread stopped");
    }

    /**
     * Process product on machine (blocking operation in separate thread)
     */
    private void processProductOnMachine(Machine machine, Product product) {
        try {
            // Update machine state
            machine.setReady(false);
            machine.setStatus("processing");
            machine.setCurrentProduct(product);
            machine.setCurrentTask(product.getId());
            machine.setColor(product.getColor());

            System.out.println("⚙️  " + machine.getName() + " started processing " +
                    product.getId() + " (color: " + product.getColor() + ")");

            // ✅ WEBSOCKET: Broadcast machine started processing
            if (broadcaster != null) {
                broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                        machine.getName(),
                        "processing",
                        product.getColor()
                ));
            }

            // Simulate processing time (BLOCKING - this is intentional)
            Thread.sleep(machine.getServiceTime());

            // Flash effect
            System.out.println("✨ " + machine.getName() + " finished processing " + product.getId());

            // ✅ WEBSOCKET: Broadcast machine flash
            if (broadcaster != null) {
                broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                        machine.getName(),
                        "FLASHING",
                        product.getColor()
                ));
            }

            Thread.sleep(200); // Flash duration

            // Move product to output queue
            if (machine.getOutputQueue() != null) {
                machine.getOutputQueue().enqueue(product);
                totalProductsProcessed++;
                System.out.println("📤 " + machine.getName() + " sent product to " +
                        machine.getOutputQueue().getId());
            } else {
                System.out.println("⚠️  " + machine.getName() + " has no output queue - product completed");
                totalProductsProcessed++;
            }

            // Reset machine state
            machine.setCurrentProduct(null);
            machine.setCurrentTask(null);
            machine.setColor(machine.getDefaultColor());
            machine.setStatus("idle");
            machine.setReady(true);

            // ✅ WEBSOCKET: Broadcast machine back to idle
            if (broadcaster != null) {
                broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                        machine.getName(),
                        "idle",
                        machine.getDefaultColor()
                ));
            }

            // ✅ WEBSOCKET: Broadcast updated statistics
            broadcastStatistics();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            machine.setStatus("error");
            machine.setReady(true);
            System.err.println("❌ " + machine.getName() + " processing interrupted");
        }
    }

    /**
     * Product generator loop (runs on separate thread)
     */
    private void runProductGenerator() {
        Random random = new Random();
        System.out.println("🏭 Product generator started");

        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // Skip if paused
                if (isPaused) {
                    Thread.sleep(100);
                    continue;
                }

                // Random delay between products
                int delay = MIN_PRODUCT_DELAY + random.nextInt(MAX_PRODUCT_DELAY - MIN_PRODUCT_DELAY);
                Thread.sleep(delay);

                // Generate product at first queue
                Queue firstQueue = getFirstQueue();
                if (firstQueue != null) {
                    Product product = new Product();
                    totalProductsGenerated++;

                    firstQueue.enqueue(product);

                    System.out.println("🆕 Generated product #" + totalProductsGenerated +
                            ": " + product.getId() +
                            " (color: " + product.getColor() + ") → " + firstQueue.getId());

                    // ✅ WEBSOCKET: Broadcast statistics update after product generation
                    broadcastStatistics();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("⏹️  Product generator stopped");
                break;
            } catch (Exception e) {
                System.err.println("❌ Error in product generator: " + e.getMessage());
            }
        }

        System.out.println("🛑 Product generator stopped");
    }

    /**
     * Stop the simulation
     */
    public synchronized void stopSimulation() {
        if (!isRunning) {
            System.out.println("⚠️  Simulation is not running");
            return;
        }

        System.out.println("⏹️  Stopping simulation...");

        // Set flag to stop all threads
        isRunning = false;

        // Stop product generator
        if (productGeneratorThread != null && productGeneratorThread.isAlive()) {
            productGeneratorThread.interrupt();
            try {
                productGeneratorThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Cancel all machine futures
        for (Future<?> future : machineFutures.values()) {
            future.cancel(true);
        }
        machineFutures.clear();

        // Shutdown executor
        if (machineExecutor != null) {
            machineExecutor.shutdownNow();
            try {
                if (!machineExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("⚠️  Executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                machineExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Reset all machines to idle
        for (Machine machine : machines.values()) {
            machine.setStatus("idle");
            machine.setReady(true);
            machine.setCurrentProduct(null);
            machine.setColor(machine.getDefaultColor());
        }

        long duration = System.currentTimeMillis() - simulationStartTime;
        System.out.println("⏹️  SIMULATION STOPPED");
        System.out.println("   Duration: " + (duration / 1000) + " seconds");
        System.out.println("   Products Generated: " + totalProductsGenerated);
        System.out.println("   Products Processed: " + totalProductsProcessed);
    }

    /**
     * Pause the simulation
     */
    public synchronized void pauseSimulation() {
        if (!isRunning) {
            throw new IllegalStateException("Cannot pause: Simulation is not running");
        }
        isPaused = true;
        System.out.println("⏸️  Simulation PAUSED");
    }

    /**
     * Resume the simulation
     */
    public synchronized void resumeSimulation() {
        if (!isRunning) {
            throw new IllegalStateException("Cannot resume: Simulation is not running");
        }
        isPaused = false;
        System.out.println("▶️  Simulation RESUMED");
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get the first queue (sorted by ID) for product generation
     */
    private Queue getFirstQueue() {
        return queues.values().stream()
                .min((q1, q2) -> q1.getId().compareTo(q2.getId()))
                .orElse(null);
    }

    /**
     * Check if at least one valid path exists (Q→M→Q)
     */
    private boolean hasValidPath() {
        // Simple check: at least one queue has output connection
        return connections.stream()
                .anyMatch(c -> c.getFromId().startsWith("Q") && c.getToId().startsWith("M"));
    }

    /**
     * Start a machine's thread (used when adding machine during simulation)
     */
    private void startMachineThread(Machine machine) {
        if (machineExecutor != null && !machineExecutor.isShutdown()) {
            Future<?> future = machineExecutor.submit(() -> runMachineLoop(machine));
            machineFutures.put(machine.getName(), future);
            System.out.println("🚀 Started thread for " + machine.getName());
        }
    }

    // ========================================================================
    // GETTERS & STATE
    // ========================================================================

    /**
     * Check if simulation is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Check if simulation is paused
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Get total products generated
     */
    public int getTotalProductsGenerated() {
        return totalProductsGenerated;
    }

    /**
     * Get total products processed
     */
    public int getTotalProductsProcessed() {
        return totalProductsProcessed;
    }

    /**
     * Get simulation duration in milliseconds
     */
    public long getSimulationDuration() {
        if (simulationStartTime == 0) return 0;
        return System.currentTimeMillis() - simulationStartTime;
    }

    /**
     * Get average queue length
     */
    public double getAverageQueueLength() {
        if (queues.isEmpty()) return 0;
        return queues.values().stream()
                .mapToInt(Queue::size)
                .average()
                .orElse(0.0);
    }

    /**
     * Get simulation statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "isRunning", isRunning,
                "isPaused", isPaused,
                "totalGenerated", totalProductsGenerated,
                "totalProcessed", totalProductsProcessed,
                "duration", getSimulationDuration(),
                "avgQueueLength", getAverageQueueLength(),
                "queueCount", queues.size(),
                "machineCount", machines.size(),
                "connectionCount", connections.size()
        );
    }

    /**
     * Clear all simulation data
     */
    public synchronized void clearSimulation() {
        // Stop if running
        if (isRunning) {
            stopSimulation();
        }

        // Clear all data
        queues.clear();
        machines.clear();
        connections.clear();
        machineFutures.clear();

        // Reset counters
        queueCounter = 0;
        machineCounter = 0;
        totalProductsGenerated = 0;
        totalProductsProcessed = 0;
        simulationStartTime = 0;

        System.out.println("🧹 Simulation cleared");
    }

    // ========================================================================
    // WEBSOCKET BROADCASTING
    // ========================================================================

    /**
     * Broadcast current statistics to frontend via WebSocket
     * Called after each product generation or processing
     */
    private void broadcastStatistics() {
        if (broadcaster != null) {
            try {
                Map<String, Object> stats = Map.of(
                        "totalGenerated", totalProductsGenerated,
                        "totalProcessed", totalProductsProcessed,
                        "avgQueueLength", getAverageQueueLength(),
                        "duration", getSimulationDuration() / 1000, // Convert to seconds
                        "timestamp", System.currentTimeMillis()
                );

                // Note: WebSocketBroadcaster doesn't have broadcastStatistics yet
                // Person 4 needs to add this method to WebSocketBroadcaster.java:
                // public void broadcastStatistics(Map<String, Object> stats) {
                //     messagingTemplate.convertAndSend("/topic/statistics", stats);
                // }

                // For now, we'll just track it in console
                // Uncomment below once Person 4 implements broadcastStatistics()
                // broadcaster.broadcastStatistics(stats);

            } catch (Exception e) {
                System.err.println("❌ Error broadcasting statistics: " + e.getMessage());
            }
        }
    }
}