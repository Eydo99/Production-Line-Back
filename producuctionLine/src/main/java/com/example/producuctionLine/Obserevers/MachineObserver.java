package com.example.producuctionLine.Obserevers;

import com.example.producuctionLine.model.dto.Machine;

public interface MachineObserver {
    void onMachineReady(Machine machine);
}
