package com.example.producuctionLine.Obserevers;

/**
 * Observable interface for subjects (Queues)
 * Part of Observer Design Pattern
 */
public interface Observable {
    void registerObserver(MachineObserver observer);
    void unregisterObserver(MachineObserver observer);
    void notifyObservers();
}