package com.example.producuctionLine.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.example.producuctionLine.Obserevers.MachineObserver;

import lombok.Data;

@Data
public class MachineQueue implements MachineObserver {
    private Queue<Product> products = new LinkedList<>();
    private List<Machine> nextMachines;
    
    public synchronized void add(Product p) {
        products.add(p);
    }

    public synchronized Product poll() {
        return products.poll();
    }

    public synchronized boolean isEmpty() {
        return products.isEmpty();
    }

    @Override
    public void onMachineReady(Machine machine) {
        if (!products.isEmpty()) {
            Product nextProduct = products.poll();
            // Send to machine service to process
            machine.setReady(false);
            // This should trigger MachineService.processProduct()
        }
    }

    @Override
    public void onProductAvailable(com.example.producuctionLine.model.Queue queue) {
        // When a product is available in the queue, try to process it
        if (!queue.isEmpty()) {
            Product product = queue.dequeue();
            if (product != null) {
                add(product);
                // Notify machines that a product is available
                if (nextMachines != null) {
                    for (Machine machine : nextMachines) {
                        if (machine.isReady()) {
                            machine.setReady(false);
                            // Process the product
                        }
                    }
                }
            }
        }
    }
}