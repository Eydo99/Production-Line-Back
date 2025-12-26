package com.example.producuctionLine.Obserevers;
import com.example.producuctionLine.model.Queue;

public interface MachineObserver {
      void onProductAvailable(Queue queue);
}
