package com.example.producuctionLine.model.snapshot;


public interface SimulationOriginator {

    
    SimulationSnapshot createSnapshot();

    
    void restoreFromSnapshot(SimulationSnapshot snapshot);
}
