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
    public void disableReplayMode() {
        this.isReplayMode = false;
        this.productsToReplay.clear();
        this.replayIndex = 0;
        System.out.println("🔁 Replay mode disabled");
    }

       public Map<String, Object> getReplayStatus(boolean isRunning) {
        return Map.of(
                "isReplayMode", isReplayMode,
                "isRunning", isRunning,
                "totalProducts", productsToReplay.size(),
                "replayIndex", replayIndex,
                "productsReplayed", replayIndex,
                "productsRemaining", productsToReplay.size() - replayIndex);
    }

   
    public boolean isReplayMode() {
        return isReplayMode;
    }

   
    public List<ProductSnapshot> getProductsToReplay() {
        return productsToReplay;
    }

   
    public int getReplayIndex() {
        return replayIndex;
    }

    /**
     * Increment the replay index.
     */
    public void incrementReplayIndex() {
        replayIndex++;
    }

   
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

   
    public void addRecordedProduct(ProductSnapshot snapshot) {
        recordedProducts.add(snapshot);
    }
}