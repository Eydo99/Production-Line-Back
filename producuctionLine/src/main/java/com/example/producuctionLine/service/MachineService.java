package com.example.producuctionLine.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.model.Product;

@Service
public class MachineService {
    private final Random random = new Random();
    
    @Autowired
    private WebSocketBroadcaster wsService;
    
    public void processProduct(Machine machine, Product product) {
        // Update machine state
        machine.setReady(false);
        machine.setStatus("processing");
       
        machine.setCurrentProduct(product);
        machine.setColor(product.getColor());
        
        // Broadcast update
        // wsService.broadcastMachineUpdate(machine);
        
        // Process asynchronously
        // CompletableFuture.runAsync(() -> {
        //     try {
        //         Thread.sleep(random.nextInt(4000) + 1000);
        //         finishProcessing(machine, product);
        //     } catch (InterruptedException e) {
        //         machine.setStatus("error");
        //         wsService.broadcastMachineUpdate(machine);
        //     }
        // });
    }
    
    public void finishProcessing(Machine machine, Product product) {
        // Push to output queue
        if (machine.getOutputQueue() != null) {
            machine.getOutputQueue().enqueue(product);
        }
        
        // Reset machine
        machine.setCurrentProduct(null);
        machine.setColor(machine.getDefaultColor());
        machine.setStatus("idle");
        machine.setReady(true);
        
        // Broadcast update
        // wsService.broadcastMachineUpdate(machine);
    }
}