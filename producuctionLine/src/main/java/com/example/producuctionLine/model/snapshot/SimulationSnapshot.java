package com.example.producuctionLine.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSnapshot {

    // Timestamp when snapshot was taken
    private long timestamp;

    // Captured states
    private List<QueueSnapshot> queueSnapshots = new ArrayList<>();
    private List<MachineSnapshot> machineSnapshots = new ArrayList<>();
    private List<ConnectionSnapshot> connectionSnapshots = new ArrayList<>();

    // Counters (for ID generation continuity)
    private int queueCounter;
    private int machineCounter;

    // Statistics
    private int totalProductsGenerated;
    private int totalProductsProcessed;

    // Duration of the simulation run (for replay)
    private long simulationDuration;

    // Record of all products generated (for deterministic replay)
    // Each product stores: id, color, and relative time when it was generated
    private List<ProductSnapshot> generatedProductsRecord = new ArrayList<>();

    // Seed for deterministic replay of random decisions (e.g. routing)
    private long randomSeed;

    /**
     * Check if this snapshot is valid for replay
     */
    public boolean isValid() {
        return timestamp > 0 &&
                queueSnapshots != null &&
                machineSnapshots != null &&
                connectionSnapshots != null;
    }
}
