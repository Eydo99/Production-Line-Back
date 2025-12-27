package com.example.producuctionLine.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of a Queue's state (Memento)
 * Used to capture queue state including its products for simulation replay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueSnapshot {
    private String id;
    private double x;
    private double y;
    private List<ProductSnapshot> productSnapshots = new ArrayList<>();
}
