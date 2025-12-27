package com.example.producuctionLine.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Snapshot of a Product's state (Memento)
 * Used to capture product state for simulation replay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSnapshot {
    private String id;
    private String color;
    private long createdAt;
}
