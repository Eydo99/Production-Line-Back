package com.example.producuctionLine.Obserevers;


public interface Observable {
    void registerObserver(MachineObserver observer);
    void unregisterObserver(MachineObserver observer);
    void notifyObservers();
}