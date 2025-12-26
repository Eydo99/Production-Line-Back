package com.example.producuctionLine.model;

import java.util.Random;

import com.example.producuctionLine.Obserevers.MachineObserver;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Machine implements MachineObserver {
    
    // ========== IDENTIFICATION ==========
    private String id;              // M1, M2, etc. (for frontend)
    private int machineNumber;      // 1, 2, 3 (numeric ID)
    
    // ========== POSITION ==========
    private double x;
    private double y;
    
    // ========== STATUS ==========
    private String status = "idle"; // "idle", "processing", "error"
    private String currentTask;     // Display current product ID
    private boolean ready = true;   // Is machine ready for next product
    
    // ========== APPEARANCE ==========
    private String color;
    private String defaultColor = "#3b82f6"; // Blue
    
    // ========== PROCESSING ==========
    private int serviceTime;        // Random processing time (ms)
    
    @JsonIgnore
    private Product currentProduct;
    
    // ========== CONNECTIONS ==========
    @JsonIgnore
    private Queue inputQueue;       // Where products come from
    
    @JsonIgnore
    private Queue outputQueue;      // Where products go to
    
    /**
     * Constructor with position
     */
    public Machine(String id, int machineNumber, double x, double y) {
        this.id = id;
        this.machineNumber = machineNumber;
        this.x = x;
        this.y = y;
        this.color = defaultColor;
        this.serviceTime = generateServiceTime();
    }
    
    // ========== OBSERVER PATTERN ==========
    
    /**
     * Called when input queue has products available
     */
    @Override
    public void onProductAvailable(Queue queue) {
        // Only react if: ready, it's my input queue, and queue has products
        if (ready && queue == inputQueue && !queue.isEmpty()) {
            Product product = queue.dequeue();
            if (product != null) {
                processProduct(product);
            }
        }
    }
    
    // ========== PROCESSING LOGIC ==========
    
    /**
     * Process a product asynchronously
     */
    private void processProduct(Product product) {
        // Update state
        this.ready = false;
        this.status = "processing";
        this.currentProduct = product;
        this.currentTask = product.getId();
        this.color = product.getColor();
        
        System.out.println("⚙️ " + id + " started processing " + product.getId());
        
        // TODO: Person 4 - Add WebSocket broadcast here
        // wsService.broadcastMachineUpdate(this);
        
        // // Process in background thread
        // CompletableFuture.runAsync(() -> {
        //     try {
        //         Thread.sleep(serviceTime);
        //         finishProcessing(product);
        //     } catch (InterruptedException e) {
        //         handleError(e);
        //     }
        // });
    }
    
    /**
     * Finish processing and move product to output queue
     */
    private void finishProcessing(Product product) {
        System.out.println("✅ " + id + " finished processing " + product.getId());
        
        // Move product to output queue (triggers next machine!)
        if (outputQueue != null) {
            outputQueue.enqueue(product);
        } else {
            System.out.println("⚠️ " + id + " has no output queue - product discarded");
        }
        
        // Reset machine state
        this.currentProduct = null;
        this.currentTask = null;
        this.color = defaultColor;
        this.status = "idle";
        this.ready = true;
        
        // TODO: Person 4 - Add WebSocket broadcast here
        // wsService.broadcastMachineUpdate(this);
        
        // Check if more products waiting in input queue
        if (inputQueue != null && !inputQueue.isEmpty()) {
            inputQueue.notifyObservers();
        }
    }
    
    
    
    // ========== HELPER METHODS ==========
    
    /**
     * Generate random service time between 1-5 seconds
     */
    private int generateServiceTime() {
        return new Random().nextInt(4000) + 1000;
    }
    
    /**
     * Get status for JSON serialization
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Check if machine is ready
     */
    public boolean isReady() {
        return ready;
    }
}