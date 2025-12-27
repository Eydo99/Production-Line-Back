package com.example.producuctionLine.model.snapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Caretaker class for the Memento (Snapshot) Pattern
 * 
 * Responsibilities:
 * - Stores snapshots in an ArrayList (history)
 * - Does NOT modify or inspect snapshot contents
 * - Provides access to stored snapshots for replay
 * 
 * Pattern Participants:
 * - Originator: SimulationManager (creates and restores from snapshots)
 * - Memento: SimulationSnapshot (stores the state)
 * - Caretaker: This class (stores history of snapshots)
 */
public class SimulationCaretaker {

    // ArrayList to store snapshot history
    private final List<SimulationSnapshot> history = new ArrayList<>();

    // Index of current snapshot (for undo/redo functionality)
    private int currentIndex = -1;

    /**
     * Save a snapshot to history
     * 
     * @param snapshot The snapshot to save
     */
    public void saveSnapshot(SimulationSnapshot snapshot) {
        // If we're not at the end of history, remove future snapshots
        if (currentIndex < history.size() - 1) {
            history.subList(currentIndex + 1, history.size()).clear();
        }

        history.add(snapshot);
        currentIndex = history.size() - 1;

        System.out.println("📸 Caretaker: Saved snapshot #" + currentIndex +
                " (total: " + history.size() + ")");
    }

    /**
     * Get the last saved snapshot
     * 
     * @return The most recent snapshot, or null if none exists
     */
    public SimulationSnapshot getLastSnapshot() {
        if (history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1);
    }

    /**
     * Get snapshot at specific index
     * 
     * @param index The index of the snapshot
     * @return The snapshot at that index
     */
    public SimulationSnapshot getSnapshot(int index) {
        if (index < 0 || index >= history.size()) {
            throw new IndexOutOfBoundsException("Snapshot index out of bounds: " + index);
        }
        currentIndex = index;
        return history.get(index);
    }

    /**
     * Get previous snapshot (undo)
     * 
     * @return Previous snapshot, or null if at beginning
     */
    public SimulationSnapshot undo() {
        if (currentIndex > 0) {
            currentIndex--;
            System.out.println("⏪ Caretaker: Undo to snapshot #" + currentIndex);
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * Get next snapshot (redo)
     * 
     * @return Next snapshot, or null if at end
     */
    public SimulationSnapshot redo() {
        if (currentIndex < history.size() - 1) {
            currentIndex++;
            System.out.println("⏩ Caretaker: Redo to snapshot #" + currentIndex);
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * Check if snapshots exist
     * 
     * @return true if at least one snapshot exists
     */
    public boolean hasSnapshots() {
        return !history.isEmpty();
    }

    /**
     * Get total number of snapshots
     * 
     * @return Number of snapshots in history
     */
    public int getSnapshotCount() {
        return history.size();
    }

    /**
     * Get current snapshot index
     * 
     * @return Current index in history
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Get all snapshots (for listing)
     * 
     * @return List of all snapshots
     */
    public List<SimulationSnapshot> getAllSnapshots() {
        return new ArrayList<>(history);
    }

    /**
     * Clear all snapshots
     */
    public void clearHistory() {
        history.clear();
        currentIndex = -1;
        System.out.println("🧹 Caretaker: History cleared");
    }
}
