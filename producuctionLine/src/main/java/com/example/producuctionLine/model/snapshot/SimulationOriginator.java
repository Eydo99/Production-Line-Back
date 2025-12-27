package com.example.producuctionLine.model.snapshot;

/**
 * Originator Interface for the Memento (Snapshot) Pattern
 * 
 * The Originator creates snapshots of its state and can restore from them.
 * In this pattern:
 * - Originator: Classes implementing this interface (e.g., SimulationManager)
 * - Memento: SimulationSnapshot (stores the state)
 * - Caretaker: SimulationCaretaker (manages snapshot history)
 */
public interface SimulationOriginator {

    /**
     * Create a snapshot (memento) of the current state
     * 
     * @return SimulationSnapshot containing the current state
     */
    SimulationSnapshot createSnapshot();

    /**
     * Restore state from a snapshot (memento)
     * 
     * @param snapshot The snapshot to restore from
     */
    void restoreFromSnapshot(SimulationSnapshot snapshot);
}
