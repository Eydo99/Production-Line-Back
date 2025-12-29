package com.example.producuctionLine.service;

import com.example.producuctionLine.dto.MachineUpdateDTO;
import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.model.Queue;
import com.example.producuctionLine.model.snapshot.ProductSnapshot;
import com.example.producuctionLine.model.snapshot.SimulationSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service to manage simulation replay functionality.
 * Handles replay mode state, product replay tracking, and broadcasting updates.
 * 
 * @author Refactored from SimulationManager
 */
@Service
public class ReplayService {

    private final WebSocketBroadcaster broadcaster;

    private boolean isReplayMode = false;
    private List<ProductSnapshot> productsToReplay = new ArrayList<>();
    private int replayIndex = 0;
    private final List<ProductSnapshot> recordedProducts = new ArrayList<>();

    public ReplayService(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * Set up replay mode with products from a snapshot.
     * Must be called before startSimulation() for deterministic replay.
     *
     * @param snapshot        The snapshot containing recorded products
     * @param queues          Map of all queues to clear
     * @param machines        Map of all machines to reset
     * @param machineResetter Function to reset a machine to idle state
     */
    public void setupReplayMode(SimulationSnapshot snapshot,
            Map<String, Queue> queues,
            Map<String, Machine> machines,
            java.util.function.Consumer<Machine> machineResetter) {
        this.isReplayMode = true;
        this.productsToReplay = new ArrayList<>(snapshot.getGeneratedProductsRecord());
        this.replayIndex = 0;

        // CRITICAL: Clear current state for clean replay
        for (Queue queue : queues.values()) {
            queue.getProducts().clear();
        }

        for (Machine machine : machines.values()) {
            machineResetter.accept(machine);
        }

        // Broadcast the "clear" to frontend so it wipes the canvas visuals
        // FIXED: Use Queue object instead of DTO constructor
        if (broadcaster != null) {
            for (Queue queue : queues.values()) {
                broadcaster.broadcastQueueUpdate(queue);
            }
            for (Machine machine : machines.values()) {
                broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                        machine.getName(), "idle", machine.getDefaultColor()));
            }
        }

        System.out.println("🔁 Replay mode enabled with " + productsToReplay.size() + " recorded products");
    }

    /**
     * Disable replay mode (for normal simulation).
     */
    public void disableReplayMode() {
        this.isReplayMode = false;
        this.productsToReplay.clear();
        this.replayIndex = 0;
        System.out.println("🔁 Replay mode disabled");
    }

    /**
     * Get the current replay status.
     *
     * @param isRunning Whether the simulation is currently running
     * @return Map containing replay status information
     */
    public Map<String, Object> getReplayStatus(boolean isRunning) {
        return Map.of(
                "isReplayMode", isReplayMode,
                "isRunning", isRunning,
                "totalProducts", productsToReplay.size(),
                "replayIndex", replayIndex,
                "productsReplayed", replayIndex,
                "productsRemaining", productsToReplay.size() - replayIndex);
    }

    /**
     * Check if replay mode is enabled.
     *
     * @return true if in replay mode
     */
    public boolean isReplayMode() {
        return isReplayMode;
    }

    /**
     * Get the list of products to replay.
     *
     * @return List of product snapshots to replay
     */
    public List<ProductSnapshot> getProductsToReplay() {
        return productsToReplay;
    }

    /**
     * Get the current replay index.
     *
     * @return Current replay index
     */
    public int getReplayIndex() {
        return replayIndex;
    }

    /**
     * Increment the replay index.
     */
    public void incrementReplayIndex() {
        replayIndex++;
    }

    /**
     * Get the list of recorded products (for snapshot creation).
     *
     * @return List of recorded product snapshots
     */
    public List<ProductSnapshot> getRecordedProducts() {
        return recordedProducts;
    }

    /**
     * Clear recorded products (for new simulation run).
     */
    public void clearRecordedProducts() {
        recordedProducts.clear();
        replayIndex = 0;
    }

    /**
     * Add a recorded product snapshot.
     *
     * @param snapshot The product snapshot to record
     */
    public void addRecordedProduct(ProductSnapshot snapshot) {
        recordedProducts.add(snapshot);
    }
}