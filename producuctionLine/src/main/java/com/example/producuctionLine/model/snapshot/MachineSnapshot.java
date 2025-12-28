package com.example.producuctionLine.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Snapshot of a Machine's state (Memento)
 * Used to capture machine state for simulation replay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineSnapshot {
    private String name;
    private int machineNumber;
    private double x;
    private double y;
    private String status;
    private String color;
    private String defaultColor;
    private int serviceTime;
    private boolean ready;

    // Queue references (by ID for reconstruction)
    private java.util.List<String> inputQueueIds;
    private java.util.List<String> outputQueueIds;

    // Current product if processing
    private ProductSnapshot currentProductSnapshot;
}
