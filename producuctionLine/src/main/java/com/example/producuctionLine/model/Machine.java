package com.example.producuctionLine.model;

import java.util.Random;

import com.example.producuctionLine.Obserevers.MachineObserver;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Machine implements MachineObserver, Runnable {

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
     * NOTE: This does NOT process the product - it just wakes up the machine
     * The actual processing is done by SimulationManager's machine threads
     */
    @Override
    public void onProductAvailable(Queue queue) {
        // Just notify that products are available
        // The SimulationManager's machine thread will handle the actual processing
        System.out.println("📢 " + name + " notified: products available in " + queue.getId());
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
        System.out.println("🏭 " + name + " thread started");

        while (isRunning) {
            try {
                // Register to input queue if ready and idle
                if (ready && inputQueue != null && !inputQueue.isEmpty()) {
                    inputQueue.registerObserver(this);
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