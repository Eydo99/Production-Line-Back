package com.example.producuctionLine.service;

import com.example.producuctionLine.model.Connection;
import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.model.Queue;
import com.example.producuctionLine.model.snapshot.*;
import com.example.producuctionLine.runner.MachineRunner;
import com.example.producuctionLine.runner.ProductGenerator;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Singleton class to manage entire simulation
 * Coordinates all queues, machines, and connections
 * Implements Concurrency Design Pattern and Memento Pattern (Originator role)
 * Memento Pattern Participants:
 * - Originator: SimulationManager (this class) - creates and restores from
 * snapshots
 * - Memento: SimulationSnapshot - stores the state
 * - Caretaker: SimulationCaretaker - manages snapshot history
 *
 * @author Person 3 - Simulation Control
 */
@Service
public class SimulationManager implements SimulationOriginator {

    // ========== WEBSOCKET BROADCASTER ==========
    private final WebSocketBroadcaster broadcaster;

    // ========== SERVICES (Refactored) ==========
    private final StatisticsService statisticsService;
    private final SnapshotService snapshotService;
    private final ConnectionService connectionService;
    private final ReplayService replayService;
    private final SimulationValidationService validationService;

    // ========== SIMULATION STATE ==========
    @Getter
    private volatile boolean isRunning = false;
    @Getter
    private volatile boolean isPaused = false;

    // ========== DATA COLLECTIONS (Thread-safe) ==========
    @Getter
    private final Map<String, Queue> queues = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Machine> machines = new ConcurrentHashMap<>();

    // ========== THREADING ==========
    private Thread productGeneratorThread;
    private final Map<String, Thread> machineThreads = new ConcurrentHashMap<>();

    // ========== COUNTERS ==========
    private int queueCounter = 0;
    private int machineCounter = 0;

    // ========== RANDOM ==========
    private final Random random = new Random();
    private long randomSeed;

    /**
     * Constructor Injection for all services
     */
   @Autowired
public SimulationManager(
        WebSocketBroadcaster broadcaster,
        StatisticsService statisticsService,
        SnapshotService snapshotService,
        ConnectionService connectionService,
        ReplayService replayService,
        SimulationValidationService validationService) {  // ← ADD THIS PARAMETER
    this.broadcaster = broadcaster;
    this.statisticsService = statisticsService;
    this.snapshotService = snapshotService;
    this.connectionService = connectionService;
    this.replayService = replayService;
    this.validationService = validationService;  // ← ADD THIS LINE
    System.out.println("🏗️ SimulationManager initialized with all services");
}

    // ========================================================================
    // QUEUE MANAGEMENT
    // ========================================================================

    /**
     * Add new queue to simulation
     * 
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

    public Queue getQueue(String id) {
        return queues.get(id);
    }

    public void removeQueue(String id) {
        // Get connections linked to this queue
        List<Connection> toRemove = connectionService.getConnectionsForNode(id);

        // Properly delete each connection to clean up machine references
        for (Connection conn : toRemove) {
            deleteConnection(conn.getFromId(), conn.getToId());
        }

        Queue removed = queues.remove(id);
        if (removed != null) {
            System.out.println("🗑️ Queue removed: " + id);
        }
    }

    // ========================================================================
    // MACHINE MANAGEMENT
    // ========================================================================

    /**
     * Add new machine to simulation
     * 
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

        if (isRunning) {
            startMachineThread(machine);
        }
        return machine;
    }

    public Machine getMachine(String id) {
        return machines.get(id);
    }

    public void removeMachine(String id) {
        Machine removed = machines.remove(id);
        if (removed != null) {
            Thread thread = machineThreads.remove(id);
            if (thread != null) {
                thread.interrupt();
            }

            // Remove all connections involving this machine
            connectionService.removeConnectionsForNode(id);

            // Unregister from queues
            for (Queue queue : removed.getInputQueues()) {
                queue.unregisterObserver(removed);
            }
            System.out.println("🗑️ Machine removed: " + id);
        }
    }

    // ========================================================================
    // CONNECTION MANAGEMENT (Delegated to ConnectionService)
    // ========================================================================

    /**
     * Create connection between nodes
     * Validates Q→M→Q pattern
     * Implements Observer Pattern wiring
     *
     * @param fromId Source node ID (Queue or Machine)
     * @param toId   Target node ID (Queue or Machine)
     * @return Created connection
     * @throws IllegalArgumentException if connection is invalid
     */
    public Connection createConnection(String fromId, String toId) {
        return connectionService.createConnection(fromId, toId, queues, machines);
    }

    public List<Connection> getConnections() {
        return connectionService.getConnections();
    }

    public void deleteConnection(String fromId, String toId) {
        connectionService.deleteConnection(fromId, toId, queues, machines);
    }

    // ========================================================================
    // SIMULATION CONTROL
    // ========================================================================

    /**
     * Start the simulation
     * Implements Concurrency Design Pattern:
     * - Each machine runs on its own thread
     * - Product generator runs on separate thread
     * - Thread-safe operations throughout
     *
     * @throws IllegalStateException if simulation already running or no queues
     *                               exist
     */
    public synchronized void startSimulation() {
    if (isRunning) {
        throw new IllegalStateException("Simulation is already running");
    }

    // ========== VALIDATION PHASE ==========
    System.out.println("🔍 Validating simulation configuration...");
    
    SimulationValidationService.ValidationResult validation = 
        validationService.validateSimulation(queues, machines, connectionService.getConnections());

    if (!validation.isValid()) {
        System.err.println("❌ Validation failed:");
        validation.getErrors().forEach(System.err::println);
        
        // Return detailed error message
        String errorMessage = String.join("\n", validation.getErrors());
        throw new IllegalStateException("Cannot start simulation - configuration errors:\n" + errorMessage);
    }

    // Print warnings if any
    if (!validation.getWarnings().isEmpty()) {
        System.out.println("⚠️ Validation warnings:");
        validation.getWarnings().forEach(System.out::println);
    }

    System.out.println("✅ Validation passed!");
    // ========== END VALIDATION ==========

    // Clear all queues from previous run
    for (Queue queue : queues.values()) {
        queue.getProducts().clear();
    }

    if (queues.isEmpty()) {
        throw new IllegalStateException("Cannot start simulation: No queues exist. Add at least one queue.");
    }

    if (machines.isEmpty()) {
        throw new IllegalStateException("Cannot start simulation: No machines exist. Add at least one machine.");
    }

    if (!connectionService.hasValidPath()) {
        System.out.println("⚠️ Warning: No complete Q→M→Q path exists. Products may get stuck.");
    }

    statisticsService.startSimulation();

    // Reset replay mode and clear recorded products for new simulation
    if (!replayService.isReplayMode()) {
        replayService.clearRecordedProducts();
        // Initialize seed for new run
        randomSeed = System.currentTimeMillis();
        random.setSeed(randomSeed);
        System.out.println("🎲 Randomized seed: " + randomSeed);
    } else {
        // In replay mode, seed should have been restored from snapshot
        random.setSeed(randomSeed);
        System.out.println("🔄 Replay using seed: " + randomSeed);
    }

    isRunning = true;
    isPaused = false;

    for (Machine machine : machines.values()) {
        Thread thread = new Thread(createMachineRunner(machine), machine.getName() + "-Thread");
        machineThreads.put(machine.getName(), thread);
        thread.start();
        System.out.println("🚀 Started thread for " + machine.getName());
    }

    productGeneratorThread = new Thread(createProductGenerator(), "ProductGenerator");
    productGeneratorThread.setDaemon(true);
    productGeneratorThread.start();

    System.out.println("▶️  SIMULATION STARTED");
    System.out.println("   Machines: " + machines.size() + " (each on separate thread)");
    System.out.println("   Queues: " + queues.size());
    System.out.println("   Connections: " + connectionService.getConnectionCount());
}
    private void startMachineThread(Machine machine) {
        Thread thread = new Thread(createMachineRunner(machine), machine.getName() + "-Thread");
        thread.setDaemon(true);
        machineThreads.put(machine.getName(), thread);
        thread.start();
        System.out.println("🚀 Started thread for " + machine.getName());
    }

    /**
     * Factory method to create a MachineRunner with all required dependencies
     */
    private MachineRunner createMachineRunner(Machine machine) {
        return new MachineRunner(
                machine,
                () -> isRunning,
                () -> isPaused,
                random,
                broadcaster,
                statisticsService,
                this::broadcastStatistics,
                this // pauseLock
        );
    }

    /**
     * Factory method to create a ProductGenerator with all required dependencies
     */
    private ProductGenerator createProductGenerator() {
        return new ProductGenerator(
                () -> isRunning,
                () -> isPaused,
                replayService::isReplayMode,
                this::getFirstQueue,
                replayService::getProductsToReplay,
                replayService::getReplayIndex,
                replayService::incrementReplayIndex,
                replayService.getRecordedProducts(),
                statisticsService,
                broadcaster,
                this::broadcastStatistics);
    }

    /**
     * Helper method to reset machine to idle state
     */
    private void resetMachine(Machine machine) {
        machine.setCurrentProduct(null);
        machine.setCurrentTask(null);
        machine.setColor(machine.getDefaultColor());
        machine.setStatus("idle");
        machine.setReady(true);
    }

    public synchronized void stopSimulation() {
        if (!isRunning) {
            System.out.println("⚠️  Simulation is not running");
            return;
        }

        System.out.println("⏹️  Stopping simulation...");

        // Auto-save snapshot before stopping (Memento Pattern)
        if (snapshotService != null) {
            System.out.println("📸 Auto-saving snapshot before stop...");
            createSnapshot();
        }

        // Set flag to stop all threads
        isRunning = false;
        isPaused = false;

        // Disable replay mode when simulation stops
        replayService.disableReplayMode();

        if (productGeneratorThread != null && productGeneratorThread.isAlive()) {
            productGeneratorThread.interrupt();
            try {
                // Wait for generator to finish (crucial for replay consistency)
                productGeneratorThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (Thread thread : machineThreads.values()) {
            thread.interrupt();
        }
        machineThreads.clear();

        for (Machine machine : machines.values()) {
            resetMachine(machine);
        }

        long duration = statisticsService.getSimulationDuration(false);
        System.out.println("⏹️  SIMULATION STOPPED");
        System.out.println("   Duration: " + (duration / 1000) + " seconds");
        System.out.println("   Products Generated: " + statisticsService.getTotalProductsGenerated());
        System.out.println("   Products Processed: " + statisticsService.getTotalProductsProcessed());
    }

    public synchronized void pauseSimulation() {
        if (!isRunning) {
            throw new IllegalStateException("Cannot pause: Simulation is not running");
        }
        if (isPaused) {
            throw new IllegalStateException("Simulation is already paused");
        }

        isPaused = true;
        statisticsService.pauseSimulation();
        System.out.println("⏸️  Simulation PAUSED");

        // CRITICAL: Unregister all machines as observers from queues
        for (Machine machine : machines.values()) {
            for (Queue q : machine.getInputQueues()) {
                q.unregisterObserver(machine);
            }
        }

        System.out.println("⏸️  Simulation PAUSED (all observers unregistered)");
    }

    public synchronized void resumeSimulation() {
        if (!isRunning) {
            throw new IllegalStateException("Cannot resume: Simulation is not running");
        }
        if (!isPaused) {
            throw new IllegalStateException("Simulation is not paused");
        }

        isPaused = false;
        statisticsService.resumeSimulation();

        System.out.println("▶️  Simulation RESUMED");

        // CRITICAL: Re-register all machines as observers
        for (Machine machine : machines.values()) {
            for (Queue q : machine.getInputQueues()) {
                q.registerObserver(machine);
            }
        }

        System.out.println("▶️  Simulation RESUMED (observers re-registered)");
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Queue getFirstQueue() {
        return queues.values().stream()
                .min((q1, q2) -> q1.getId().compareTo(q2.getId()))
                .orElse(null);
    }

    private void broadcastStatistics() {
        if (broadcaster != null) {
            broadcaster.broadcastStatistics(getStatistics());
        }
    }

    // ========================================================================
    // GETTERS & STATE
    // ========================================================================

    public long getSimulationDuration() {
        return statisticsService.getSimulationDuration(isPaused);
    }

    public double getAverageQueueLength() {
        return statisticsService.getAverageQueueLength(queues);
    }

    public Map<String, Object> getStatistics() {
        return Map.of(
                "isRunning", isRunning,
                "isPaused", isPaused,
                "totalGenerated", statisticsService.getTotalProductsGenerated(),
                "totalProcessed", statisticsService.getTotalProductsProcessed(),
                "duration", getSimulationDuration(),
                "avgQueueLength", getAverageQueueLength(),
                "queueCount", queues.size(),
                "machineCount", machines.size(),
                "connectionCount", connectionService.getConnectionCount());
    }

    public synchronized void clearSimulation() {
        if (isRunning) {
            stopSimulation();
        }
        // Auto-save snapshot before clearing (Memento Pattern)
        if (!snapshotService.hasSnapshot() && !queues.isEmpty()) {
            System.out.println("📸 Auto-saving snapshot before clear...");
            createSnapshot();
        }

        queues.clear();
        machines.clear();
        connectionService.clearConnections();
        machineThreads.clear();

        queueCounter = 0;
        machineCounter = 0;
        statisticsService.clearStatistics();

        System.out.println("🧹 Simulation cleared");
    }

    // ========================================================================
    // SNAPSHOT (Memento Pattern) - For Simulation Replay
    // ========================================================================

    /**
     * Create a snapshot of the current simulation state (Memento Pattern)
     * Captures all queues, machines, connections, products, and counters
     * 
     * @return SimulationSnapshot containing the complete simulation state
     */
    public synchronized SimulationSnapshot createSnapshot() {
        SimulationSnapshot snapshot = snapshotService.createSnapshot(
                queues,
                machines,
                connectionService.getConnections(),
                queueCounter,
                machineCounter,
                statisticsService.getTotalProductsGenerated(),
                statisticsService.getTotalProductsProcessed(),
                getSimulationDuration(),
                replayService.getRecordedProducts(),
                randomSeed);

        System.out.println("📸 Snapshot created at " + snapshot.getTimestamp());
        System.out.println("   Queues: " + snapshot.getQueueSnapshots().size());
        System.out.println("   Machines: " + snapshot.getMachineSnapshots().size());
        System.out.println("   Connections: " + snapshot.getConnectionSnapshots().size());

        return snapshot;
    }

    /**
     * Restore simulation state from a snapshot (Memento Pattern)
     * Clears current state and rebuilds from the snapshot
     * 
     * @param snapshot The snapshot to restore from
     * @throws IllegalStateException if simulation is running or snapshot is invalid
     */
    public synchronized void restoreFromSnapshot(SimulationSnapshot snapshot) {
        if (isRunning) {
            throw new IllegalStateException(
                    "Cannot restore snapshot while simulation is running. Stop the simulation first.");
        }

        machineThreads.clear();

        // Use SnapshotService to restore
        SnapshotService.RestoreResult result = snapshotService.restoreFromSnapshot(
                snapshot, queues, machines, connectionService);

        // Apply results
        queueCounter = result.queueCounter;
        machineCounter = result.machineCounter;
        statisticsService.setStates(result.totalProductsGenerated, result.totalProductsProcessed);
        randomSeed = result.randomSeed;

        // Reset statistics for new run but keep counters
        statisticsService.clearStatistics();
        statisticsService.setStates(result.totalProductsGenerated, result.totalProductsProcessed);
    }

    /**
     * Check if a snapshot exists for replay
     * 
     * @return true if a snapshot is available
     */
    public boolean hasSnapshot() {
        return snapshotService.hasSnapshot();
    }

    /**
     * Get the last snapshot from the Caretaker
     * 
     * @return The last saved snapshot, or null if none exists
     */
    public SimulationSnapshot getLastSnapshot() {
        return snapshotService.getLastSnapshot();
    }

    /**
     * Get the Caretaker for advanced snapshot operations (undo/redo)
     * 
     * @return The SimulationCaretaker instance
     */
    public SimulationCaretaker getCaretaker() {
        return snapshotService.getCaretaker();
    }

    // ========================================================================
    // REPLAY MODE (Delegated to ReplayService)
    // ========================================================================

    /**
     * Set up replay mode with products from a snapshot
     * Must be called before startSimulation() for deterministic replay
     * 
     * @param snapshot The snapshot containing recorded products
     */
    public void setupReplayMode(SimulationSnapshot snapshot) {
        replayService.setupReplayMode(snapshot, queues, machines, this::resetMachine);
    }

    public Map<String, Object> getReplayStatus() {
        return replayService.getReplayStatus(isRunning);
    }

    /**
     * Disable replay mode (for normal simulation)
     */
    public void disableReplayMode() {
        replayService.disableReplayMode();
    }
}
