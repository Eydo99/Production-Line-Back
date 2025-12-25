package com.example.producuctionLine.model;

import java.util.ArrayList;
import java.util.List;

import com.example.producuctionLine.Obserevers.MachineObserver;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Machine implements MachineObserver {
    
    private String name;
    private int id;
    
    // Position on canvas
    private double x;
    private double y;
    
    @JsonIgnore
    private List<MachineQueue> next = new ArrayList<>();
    
    @JsonIgnore
    private List<MachineObserver> observers = new ArrayList<>();
    
    private int serviceTime = 2000; // Default 2 seconds
    private String color;
    private String defaultColor = "#3b82f6"; // Blue
    
    @JsonIgnore
    private Product currentProduct;
    
    private boolean isReady = true;
    
    // Input/Output queues for connections
    @JsonIgnore
    private Queue inputQueue;
    
    @JsonIgnore
    private Queue outputQueue;
    
    /**
     * Constructor with position
     */
    public Machine(String name, int id, double x, double y) {
        this.name = name;
        this.id = id;
        this.x = x;
        this.y = y;
        this.color = defaultColor;
    }
    
    // Register an observer (input queue)
    public void addObserver(MachineObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    // Remove an observer
    public void removeObserver(MachineObserver observer) {
        observers.remove(observer);
    }
    
    // Notify all observers that machine is ready
    public void notifyReady() {
        for (MachineObserver observer : observers) {
            observer.onMachineReady(this);
        }
    }
    
    @Override
    public void onMachineReady(Machine machine) {
        // Implementation if needed
    }
    
    @Override
    public void onProductAvailable(Queue queue) {
        if (isReady && queue == inputQueue && !queue.isEmpty()) {
            // Pull product from queue and process
            Product product = queue.dequeue();
            if (product != null) {
                processProduct(product);
            }
        }
    }
    
    /**
     * Process a product (simplified for now)
     */
    private void processProduct(Product product) {
        this.isReady = false;
        this.currentProduct = product;
        this.color = product.getColor();
        
        System.out.println("⚙️ Machine " + name + " processing product " + product.getId());
        
        // Person 3 will implement threading here
    }
    
    /**
     * Finish processing and move product to output
     */
    public void finishProcessing() {
        if (currentProduct != null && outputQueue != null) {
            outputQueue.enqueue(currentProduct);
            System.out.println("✅ Machine " + name + " finished processing");
        }
        
        this.currentProduct = null;
        this.color = defaultColor;
        this.isReady = true;
        notifyReady();
    }
}