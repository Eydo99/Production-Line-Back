package com.example.producuctionLine.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Snapshot of a Connection's state (Memento)
 * Used to capture connection state for simulation replay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionSnapshot {
    private String id;
    private String fromId;
    private String toId;
}
