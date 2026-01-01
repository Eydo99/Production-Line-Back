package com.example.producuctionLine.model.snapshot;

import java.util.ArrayList;
import java.util.List;


public class SimulationCaretaker {

    // ArrayList to store snapshot history
    private final List<SimulationSnapshot> history = new ArrayList<>();

    // Index of current snapshot (for undo/redo functionality)
    private int currentIndex = -1;

    
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

    
    public SimulationSnapshot getLastSnapshot() {
        if (history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1);
    }

    
    public SimulationSnapshot getSnapshot(int index) {
        if (index < 0 || index >= history.size()) {
            throw new IndexOutOfBoundsException("Snapshot index out of bounds: " + index);
        }
        currentIndex = index;
        return history.get(index);
    }

    
    public SimulationSnapshot undo() {
        if (currentIndex > 0) {
            currentIndex--;
            System.out.println("⏪ Caretaker: Undo to snapshot #" + currentIndex);
            return history.get(currentIndex);
        }
        return null;
    }


    public SimulationSnapshot redo() {
        if (currentIndex < history.size() - 1) {
            currentIndex++;
            System.out.println("⏩ Caretaker: Redo to snapshot #" + currentIndex);
            return history.get(currentIndex);
        }
        return null;
    }

    
    public boolean hasSnapshots() {
        return !history.isEmpty();
    }

    
    public int getSnapshotCount() {
        return history.size();
    }

    
    public int getCurrentIndex() {
        return currentIndex;
    }

    
    public List<SimulationSnapshot> getAllSnapshots() {
        return new ArrayList<>(history);
    }

    
    public void clearHistory() {
        history.clear();
        currentIndex = -1;
        System.out.println("🧹 Caretaker: History cleared");
    }
}
