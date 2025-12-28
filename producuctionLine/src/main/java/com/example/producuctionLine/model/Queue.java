package com.example.producuctionLine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.producuctionLine.Obserevers.MachineObserver;
import com.example.producuctionLine.Obserevers.Observable;
import com.example.producuctionLine.service.WebSocketBroadcaster;

import lombok.Data;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Queue entity - holds products and notifies observers
 * Implements Observable pattern (Subject role)
 */
@Data
public class Queue implements Observable {
    private String id;
    private double x;
    private double y;
    
    // Thread-safe queue for products
    @JsonIgnore // Don't serialize in REST responses
    private BlockingQueue<Product> products;
    
    // Thread-safe list of observers
    @JsonIgnore
    private List<MachineObserver> observers;
    
    // Connection to next node
    private Connection outputConnection;
    
    public Queue(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.products = new LinkedBlockingQueue<>();
        this.observers = new CopyOnWriteArrayList<>();
    }
    
    // ========== OBSERVER PATTERN METHODS ==========
    
    @Override
    public void registerObserver(MachineObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("✅ Observer registered to " + id);
        }
    }
    
    @Override
    public void unregisterObserver(MachineObserver observer) {
        observers.remove(observer);
        System.out.println("âŒ Observer unregistered from " + id);
    }
    
    @Override
    public void notifyObservers() {
        for (MachineObserver observer : observers) {
            observer.onProductAvailable(this);
        }
    }
    
    // ========== QUEUE OPERATIONS ==========
    
    /**
     * Add product to queue and notify observers
     */
 // Add WebSocketBroadcaster field at the top of the class (after line 18, before the "id" field)
@JsonIgnore
private WebSocketBroadcaster broadcaster;

// Add setter method (after the constructor, around line 35)
public void setBroadcaster(WebSocketBroadcaster broadcaster) {
    this.broadcaster = broadcaster;
}

// Replace enqueue method
public synchronized void enqueue(Product product) {
    products.offer(product);
    System.out.println("📦 Product " + product.getId() + 
                      " added to " + id + " (size: " + size() + ")");
    
    // ✅ Broadcast queue update
    if (broadcaster != null) {
        broadcaster.broadcastQueueUpdate(
            new com.example.producuctionLine.dto.QueueUpdateDTO(id, size())
        );
    }
    
    notifyObservers();
}

// Replace dequeue method
public synchronized Product dequeue() {
    Product product = products.poll();
    if (product != null) {
        System.out.println("📤 Product " + product.getId() + 
                          " removed from " + id + " (size: " + size() + ")");
        
        // ✅ Broadcast queue update
        if (broadcaster != null) {
            broadcaster.broadcastQueueUpdate(
                new com.example.producuctionLine.dto.QueueUpdateDTO(id, size())
            );
        }
    }
    return product;
}
    /**
     * Get current queue size
     */
    public int size() {
        return products.size();
    }
    
    /**
     * Check if queue is empty
     */
    public boolean isEmpty() {
        return products.isEmpty();
    }
    
    /**
     * Get queue size for JSON serialization
     */
    public int getCurrentSize() {
        return size();
    }
}