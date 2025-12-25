package com.example.producuctionLine.Obserevers;
import com.example.producuctionLine.model.Queue;
import com.example.producuctionLine.model.Machine;

public interface MachineObserver {
    void onMachineReady(Machine machine);


      void onProductAvailable(Queue queue);
}
