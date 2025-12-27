package com.example.producuctionLine.service;

import com.example.producuctionLine.dto.MachineUpdateDTO;
import com.example.producuctionLine.model.Connection;
import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.model.Product;
import com.example.producuctionLine.model.Queue;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
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
    private WebSocketBroadcaster broadcaster;

    @Getter
    private volatile boolean isRunning = false;
    @Getter
    private volatile boolean isPaused = false;

    @Getter
    private final Map<String, Queue> queues = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Machine> machines = new ConcurrentHashMap<>();
    private final List<Connection> connections = new CopyOnWriteArrayList<>();

    // ========== THREADING ==========
    private ExecutorService machineExecutor;
    private Thread productGeneratorThread;
    private final Map<String, Thread> machineThreads = new ConcurrentHashMap<>();

    @Getter
    private int totalProductsGenerated = 0;
    @Getter
    private int totalProductsProcessed = 0;
    private long simulationStartTime = 0;
    private long totalPausedTime = 0;
    private long pauseStartTime = 0;

    // ========== COUNTERS ==========
    private int queueCounter = 0;
    private int machineCounter = 0;

    // ========== CONFIGURATION ==========
    private static final int MIN_PRODUCT_DELAY = 1000;
    private static final int MAX_PRODUCT_DELAY = 3000;
    private static final int THREAD_POOL_SIZE = 10;

    private SimulationManager() {
        System.out.println("🏗️ SimulationManager initialized");
    }

    public static synchronized SimulationManager getInstance() {
        if (instance == null) {
            instance = new SimulationManager();
        }
        return instance;
    }

    // ========================================================================
    // QUEUE MANAGEMENT
    // ========================================================================

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
        Queue removed = queues.remove(id);
        if (removed != null) {
            connections.removeIf(conn ->
                    conn.getFromId().equals(id) || conn.getToId().equals(id)
            );
            System.out.println("🗑️ Queue removed: " + id);
        }
    }

    // ========================================================================
    // MACHINE MANAGEMENT
    // ========================================================================

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
            connections.removeIf(conn ->
                    conn.getFromId().equals(id) || conn.getToId().equals(id)
            );
            if (removed.getInputQueue() != null) {
                removed.getInputQueue().unregisterObserver(removed);
            }
            System.out.println("🗑️ Machine removed: " + id);
        }
    }

    // ========================================================================
    // CONNECTION MANAGEMENT
    // ========================================================================

    public Connection createConnection(String fromId, String toId) {
        boolean fromExists = queues.containsKey(fromId) || machines.containsKey(fromId);
        boolean toExists = queues.containsKey(toId) || machines.containsKey(toId);

        if (!fromExists) {
            throw new IllegalArgumentException("Source node '" + fromId + "' does not exist");
        }
        if (!toExists) {
            throw new IllegalArgumentException("Target node '" + toId + "' does not exist");
        }

        char fromType = fromId.charAt(0);
        char toType = toId.charAt(0);

        if (fromType == toType) {
            throw new IllegalArgumentException(
                    "Invalid connection: Cannot connect " + fromType + " to " + toType +
                            ". Must alternate between Queue and Machine (Q→M or M→Q)"
            );
        }

        Connection connection = new Connection(fromId, toId);

        boolean exists = connections.stream()
                .anyMatch(c -> c.getFromId().equals(fromId) && c.getToId().equals(toId));

        if (exists) {
            throw new IllegalArgumentException("Connection already exists");
        }

        connections.add(connection);

        if (fromType == 'Q' && toType == 'M') {
            Queue queue = queues.get(fromId);
            Machine machine = machines.get(toId);
            machine.setInputQueue(queue);
            queue.registerObserver(machine);
            System.out.println("🔗 Connected: Queue " + fromId + " → Machine " + toId);
            System.out.println("   Observer Pattern: " + toId + " now observes " + fromId);
        } else if (fromType == 'M' && toType == 'Q') {
            Machine machine = machines.get(fromId);
            Queue queue = queues.get(toId);
            machine.setOutputQueue(queue);
            System.out.println("🔗 Connected: Machine " + fromId + " → Queue " + toId);
        }

        return connection;
    }

    public List<Connection> getConnections() {
        return new ArrayList<>(connections);
    }

    public void deleteConnection(String fromId, String toId) {
        connections.removeIf(conn ->
                conn.getFromId().equals(fromId) && conn.getToId().equals(toId)
        );

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
    // SIMULATION CONTROL
    // ========================================================================

    public synchronized void startSimulation() {
        if (isRunning) {
            throw new IllegalStateException("Simulation is already running");
        }
        // ADD THIS:
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

        if (!hasValidPath()) {
            System.out.println("⚠️ Warning: No complete Q→M→Q path exists. Products may get stuck.");
        }

        totalProductsGenerated = 0;
        totalProductsProcessed = 0;
        simulationStartTime = System.currentTimeMillis();
        totalPausedTime = 0;
        pauseStartTime = 0;

        isRunning = true;
        isPaused = false;

        machineExecutor = Executors.newFixedThreadPool(
                Math.max(THREAD_POOL_SIZE, machines.size()),
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(@NonNull Runnable r) {
                        Thread t = new Thread(r, "MachineThread-" + (++counter));
                        t.setDaemon(true);
                        return t;
                    }
                }
        );

        for (Machine machine : machines.values()) {
            Thread thread = new Thread(() -> runMachineLoop(machine), machine.getName() + "-Thread");
            thread.setDaemon(true);
            machineThreads.put(machine.getName(), thread);
            thread.start();
            System.out.println("🚀 Started thread for " + machine.getName());
        }

        productGeneratorThread = new Thread(this::runProductGenerator, "ProductGenerator");
        productGeneratorThread.setDaemon(true);
        productGeneratorThread.start();

        System.out.println("▶️  SIMULATION STARTED");
        System.out.println("   Machines: " + machines.size() + " (each on separate thread)");
        System.out.println("   Queues: " + queues.size());
        System.out.println("   Connections: " + connections.size());
    }

    /**
     * Machine processing loop - now properly handles pause/stop during processing
     */
    private void runMachineLoop(Machine machine) {
        System.out.println("🏁 " + machine.getName() + " thread started");

        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // Wait while paused
                while (isPaused) {
                    if (!isRunning) break;
                    Thread.sleep(50);
                }

                // Check if stopped while paused
                if (!isRunning) break;

                // If machine is ready and has input queue, check for products
                if (machine.isReady() && machine.getInputQueue() != null) {
                    Queue inputQueue = machine.getInputQueue();
                    if (!inputQueue.isEmpty()) {

// 🚫 DO NOT take product if paused or stopped - with synchronized check
                        synchronized (this) {
                            if (isPaused || !isRunning) {
                                Thread.sleep(50);
                                continue;
                            }
                        }

// Double-check to prevent race condition
                        if (isPaused || !isRunning) {
                            Thread.sleep(50);
                            continue;
                        }

                        Product product = inputQueue.dequeue();

                        if (product != null) {
                            processProductOnMachine(machine, product);

                            // ⛔ stop immediately if simulation ended mid-processing
                            if (!isRunning || Thread.currentThread().isInterrupted()) {
                                break;
                            }
                        }
                    }
else {
                        inputQueue.registerObserver(machine);
                    }
                }

                Thread.sleep(50);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("⏹️  " + machine.getName() + " thread interrupted");
                break;
            }
            catch (Exception e) {
                System.err.println("❌ Error in " + machine.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("🛑 " + machine.getName() + " thread stopped");
    }

    /**
     * Process product on machine - now checks for pause/stop during sleep
     */
    private void processProductOnMachine(Machine machine, Product product) {

        // ⏸️ IMMEDIATE PAUSE CHECK - If paused, return product and exit
        if (isPaused) {
            // Return product to input queue
            if (machine.getInputQueue() != null && product != null) {
                machine.getInputQueue().enqueue(product);
                System.out.println("⏸️  " + machine.getName() + " returned product to queue (paused)");
            }
            // Reset machine state
            resetMachine(machine);
            return;
        }

        // Check if simulation stopped
        if (!isRunning) {
            if (machine.getInputQueue() != null && product != null) {
                machine.getInputQueue().enqueue(product);
            }
            resetMachine(machine);
            return;
        }
        try {
            machine.setReady(false);
            machine.setStatus("processing");
            machine.setCurrentProduct(product);
            machine.setCurrentTask(product.getId());
            machine.setColor(product.getColor());

            System.out.println("⚙️  " + machine.getName() + " started processing " +
                    product.getId() + " (color: " + product.getColor() + ")");

            if (broadcaster != null) {
                broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                        machine.getName(),
                        "processing",
                        product.getColor()
                ));
            }

            // Sleep in small chunks so we can respond to pause/stop quickly
            int serviceTime = machine.getServiceTime();
            int elapsed = 0;
            int sleepChunk = 100; // Check every 100ms

            while (elapsed < serviceTime) {
                // Check if stopped or paused BEFORE sleeping
                if (!isRunning) {
                    System.out.println("⏹️  " + machine.getName() + " processing aborted (simulation stopped)");
                    resetMachine(machine);
                    return;
                }

// Wait while paused - MORE RESPONSIVE VERSION
                while (isPaused) {
                    // Check every 10ms instead of 50ms for faster response
                    Thread.sleep(10);

                    // If simulation stopped while paused
                    if (!isRunning) {
                        // 🔁 return product to input queue
                        if (machine.getInputQueue() != null && product != null) {
                            machine.getInputQueue().enqueue(product);
                            System.out.println("⏹️  " + machine.getName() + " returned product (stopped while paused)");
                        }
                        resetMachine(machine);
                        return;
                    }
                }

                // Sleep for a chunk
                int remainingTime = serviceTime - elapsed;
                int timeToSleep = Math.min(sleepChunk, remainingTime);
                Thread.sleep(timeToSleep);
                elapsed += timeToSleep;
            }
            // Only continue if simulation is still running
            if (!isRunning) {
                // 🔁 return product to input queue
                if (machine.getInputQueue() != null && product != null) {
                    machine.getInputQueue().enqueue(product);
                }
                resetMachine(machine);
                return;
            }
            // Flash effect
            System.out.println("✨ " + machine.getName() + " finished processing " + product.getId());

            if (broadcaster != null) {
                broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                        machine.getName(),
                        "FLASHING",
                        product.getColor()
                ));
            }

            Thread.sleep(200); // Flash duration

            // Move product to output queue - WITH PROPER PAUSE CHECKING

            // Check pause/stop state with synchronization
            synchronized (this) {
                if (!isRunning || isPaused) {
                    // Return product to input queue if paused/stopped
                    if (machine.getInputQueue() != null) {
                        machine.getInputQueue().enqueue(product);
                        System.out.println("⏸️  " + machine.getName() + " returned product (paused/stopped at output)");
                    }
                    resetMachine(machine);
                    return;
                }
            }

            // Only proceed if not paused and running
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
            resetMachine(machine);

            if (broadcaster != null) {
                broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                        machine.getName(),
                        "idle",
                        machine.getDefaultColor()
                ));
            }

            broadcastStatistics();

        } catch (InterruptedException e) {

            // 🔁 return product safely
            if (machine.getInputQueue() != null && product != null) {
                machine.getInputQueue().enqueue(product);
            }

            resetMachine(machine);
            Thread.currentThread().interrupt();
        }

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

    /**
     * Product generator loop
     */
    private void runProductGenerator() {
        Random random = new Random();
        System.out.println("🏭 Product generator started");

        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // Wait while paused
                while (isPaused && isRunning) {
                    Thread.sleep(100);
                }

                // Check if stopped while paused
                if (!isRunning) break;

                int delay = MIN_PRODUCT_DELAY + random.nextInt(MAX_PRODUCT_DELAY - MIN_PRODUCT_DELAY);
                Thread.sleep(delay);

                // Double-check still running after sleep
                if (!isRunning) break;

                Queue firstQueue = getFirstQueue();
                if (firstQueue != null) {
                    Product product = new Product();
                    totalProductsGenerated++;

                    firstQueue.enqueue(product);

                    System.out.println("🆕 Generated product #" + totalProductsGenerated +
                            ": " + product.getId() +
                            " (color: " + product.getColor() + ") → " + firstQueue.getId());

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

    public synchronized void stopSimulation() {
        if (!isRunning) {
            System.out.println("⚠️  Simulation is not running");
            return;
        }

        System.out.println("⏹️  Stopping simulation...");

        isRunning = false;
        isPaused = false;
        pauseStartTime = 0;

        if (productGeneratorThread != null && productGeneratorThread.isAlive()) {
            productGeneratorThread.interrupt();
            try {
                productGeneratorThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (Thread thread : machineThreads.values()) {
            thread.interrupt();
        }
        machineThreads.clear();

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

        for (Machine machine : machines.values()) {
            resetMachine(machine);
        }

        long duration = System.currentTimeMillis() - simulationStartTime;
        System.out.println("⏹️  SIMULATION STOPPED");
        System.out.println("   Duration: " + (duration / 1000) + " seconds");
        System.out.println("   Products Generated: " + totalProductsGenerated);
        System.out.println("   Products Processed: " + totalProductsProcessed);
    }

    public synchronized void pauseSimulation() {
        if (!isRunning) {
            throw new IllegalStateException("Cannot pause: Simulation is not running");
        }
        if (isPaused) {
            throw new IllegalStateException("Simulation is already paused");
        }

        isPaused = true;
        pauseStartTime = System.currentTimeMillis();
        System.out.println("⏸️  Simulation PAUSED");
        // 🆕 CRITICAL: Unregister all machines as observers from queues
        for (Machine machine : machines.values()) {
            if (machine.getInputQueue() != null) {
                machine.getInputQueue().unregisterObserver(machine);
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

        if (pauseStartTime > 0) {
            totalPausedTime += (System.currentTimeMillis() - pauseStartTime);
            pauseStartTime = 0;
        }

        System.out.println("▶️  Simulation RESUMED");

        // 🆕 CRITICAL: Re-register all machines as observers
        for (Machine machine : machines.values()) {
            if (machine.getInputQueue() != null) {
                machine.getInputQueue().registerObserver(machine);
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

    private boolean hasValidPath() {
        return connections.stream()
                .anyMatch(c -> c.getFromId().startsWith("Q") && c.getToId().startsWith("M"));
    }

    private void startMachineThread(Machine machine) {
        Thread thread = new Thread(() -> runMachineLoop(machine), machine.getName() + "-Thread");
        thread.setDaemon(true);
        machineThreads.put(machine.getName(), thread);
        thread.start();
        System.out.println("🚀 Started thread for " + machine.getName());
    }

    // ========================================================================
    // GETTERS & STATE
    // ========================================================================

    public long getSimulationDuration() {
        if (simulationStartTime == 0) return 0;

        long currentTime = System.currentTimeMillis();
        long totalElapsed = currentTime - simulationStartTime;

        long currentPauseDuration = 0;
        if (isPaused && pauseStartTime > 0) {
            currentPauseDuration = currentTime - pauseStartTime;
        }

        return totalElapsed - totalPausedTime - currentPauseDuration;
    }

    public double getAverageQueueLength() {
        if (queues.isEmpty()) return 0;
        return queues.values().stream()
                .mapToInt(Queue::size)
                .average()
                .orElse(0.0);
    }

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

    public synchronized void clearSimulation() {
        if (isRunning) {
            stopSimulation();
        }

        queues.clear();
        machines.clear();
        connections.clear();
        machineThreads.clear();

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

    private void broadcastStatistics() {
        if (broadcaster != null) {
            try {
                Map<String, Object> stats = Map.of(
                        "totalGenerated", totalProductsGenerated,
                        "totalProcessed", totalProductsProcessed,
                        "avgQueueLength", getAverageQueueLength(),
                        "duration", getSimulationDuration() / 1000,
                        "timestamp", System.currentTimeMillis()
                );
            } catch (Exception e) {
                System.err.println("❌ Error broadcasting statistics: " + e.getMessage());
            }
        }
    }
}