package com.example.producuctionLine.model;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.producuctionLine.Obserevers.MachineObserver;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Machine implements MachineObserver, Runnable {

    
    private String name; // M1, M2, etc. (for frontend)
    private int machineNumber; // 1, 2, 3 (numeric ID)

    
    private double x;
    private double y;

    
    private String status = "idle"; // "idle", "processing", "error"
    private String currentTask; // Display current product ID
    
    private boolean ready = true; // Is machine ready for next product

    
    private String color;
    private String defaultColor = "#3b82f6"; // Blue

    
    private int serviceTime; // Random processing time (ms)

    @JsonIgnore
    private Product currentProduct;

    
    @JsonIgnore
    private List<Queue> inputQueues = new CopyOnWriteArrayList<>(); // Where products come from

    @JsonIgnore
    private List<Queue> outputQueues = new CopyOnWriteArrayList<>(); // Where products go to

    public void addInputQueue(Queue queue) {
        if (!inputQueues.contains(queue)) {
            inputQueues.add(queue);
        }
    }

    public void removeInputQueue(Queue queue) {
        inputQueues.remove(queue);
    }

    public void addOutputQueue(Queue queue) {
        if (!outputQueues.contains(queue)) {
            outputQueues.add(queue);
        }
    }

    public void removeOutputQueue(Queue queue) {
        outputQueues.remove(queue);
    }

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

    
    @Override
    public void onProductAvailable(Queue queue) {
        // Just notify that products are available
        // The SimulationManager's machine thread will handle the actual processing
        System.out.println("📢 " + name + " notified: products available in " + queue.getId());
    }

    

    
    private int generateServiceTime() {
        return new Random().nextInt(4000) + 1000;
    }

    @Override
    public void run() {
        isRunning = true;
        System.out.println("🏭 " + name + " thread started");

        while (isRunning) {
            try {
                // Register to input queue if ready and idle
                if (ready && !inputQueues.isEmpty()) {
                    for (Queue q : inputQueues) {
                        if (!q.isEmpty()) {
                            q.registerObserver(this);
                        }
                    }
                }

                Thread.sleep(100); // Check every 100ms

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("ℹ️ " + name + " thread interrupted");
                break;
            }
        }

        System.out.println("🛑 " + name + " thread stopped");
    }

    private void handleError(Exception e) {
        System.err.println("❌ " + name + " error: " + e.getMessage());
        this.status = "error";
        this.ready = true;
        this.currentProduct = null;
        this.color = defaultColor;
    }
}