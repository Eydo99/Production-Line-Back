package com.example.producuctionLine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.producuctionLine.Obserevers.MachineObserver;
import com.example.producuctionLine.Obserevers.Observable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

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
    @JsonIgnore // Don't serialize the BlockingQueue directly
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
        System.out.println("❌ Observer unregistered from " + id);
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
    public synchronized void enqueue(Product product) {
        products.offer(product);
        System.out.println("📦 Product " + product.getId() + 
                          " added to " + id + " (size: " + size() + ")");
        notifyObservers(); // Notify machines waiting for products
    }
    
    /**
     * Remove and return product from queue
     */
    public synchronized Product dequeue() {
        Product product = products.poll();
        if (product != null) {
            System.out.println("📤 Product " + product.getId() + 
                              " removed from " + id + " (size: " + size() + ")");
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
    
    /**
     * Get product list for JSON serialization
     * Returns list of products with their colors
     */
    public List<Product> getProductList() {
        return new ArrayList<>(products);
    }
}