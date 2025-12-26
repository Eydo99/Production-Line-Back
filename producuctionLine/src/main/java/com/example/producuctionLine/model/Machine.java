package com.example.producuctionLine.model;

import java.util.Random;

import com.example.producuctionLine.Obserevers.MachineObserver;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Machine implements MachineObserver,Runnable {
    
    // ========== IDENTIFICATION ==========
    private String name;              // M1, M2, etc. (for frontend)
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

    @JsonIgnore
    private volatile boolean isRunning = false;

    @JsonIgnore
    private Thread machineThread;
    
    /**
     * Constructor with position
     */
    public Machine(String id, int machineNumber, double x, double y) {
        this.name = id;
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
        this.ready = false;
        this.status = "processing";
        this.currentProduct = product;
        this.currentTask = product.getId();
        this.color = product.getColor();

        System.out.println("⚙️ " + name + " started processing " + product.getId());

        // UNCOMMENT THIS - Process in background thread
        new Thread(() -> {
            try {
                Thread.sleep(serviceTime);
                finishProcessing(product);
            } catch (InterruptedException e) {
                handleError(e);
            }
        }).start();
    }
    
    /**
     * Finish processing and move product to output queue
     */
    private void finishProcessing(Product product) {
        System.out.println("✅ " + name + " finished processing " + product.getId());
        
        // Move product to output queue (triggers next machine!)
        if (outputQueue != null) {
            outputQueue.enqueue(product);
        } else {
            System.out.println("⚠️ " + name + " has no output queue - product discarded");
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


    @Override
    public void run() {
        isRunning = true;
        System.out.println("🏁 " + name + " thread started");

        while (isRunning) {
            try {
                // Register to input queue if ready and idle
                if (ready && inputQueue != null && !inputQueue.isEmpty()) {
                    inputQueue.registerObserver(this);
                }

                Thread.sleep(100); // Check every 100ms

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("⏹️ " + name + " thread interrupted");
                break;
            }
        }

        System.out.println("🛑 " + name + " thread stopped");
    }

    public void stopThread() {
        isRunning = false;
        if (machineThread != null) {
            machineThread.interrupt();
        }
    }


    public void startThread() {
        if (machineThread == null || !machineThread.isAlive()) {
            machineThread = new Thread(this, name + "-Thread");
            machineThread.start();
        }
    }


    private void handleError(Exception e) {
        System.err.println("❌ " + name + " error: " + e.getMessage());
        this.status = "error";
        this.ready = true;
        this.currentProduct = null;
        this.color = defaultColor;
    }

}