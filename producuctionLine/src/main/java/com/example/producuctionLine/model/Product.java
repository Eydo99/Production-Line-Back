package com.example.producuctionLine.model;

import lombok.Data;
import java.util.UUID;

@Data
public class Product {
    private String id;
    private String color;
    private long createdAt;
    
    // Constructor - don't use @NoArgsConstructor
    public Product() {
        this.id = UUID.randomUUID().toString();
        this.color = generateRandomColor();
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * Generate random color from predefined palette
     */
    private String generateRandomColor() {
        String[] colors = {
            "#2094f3", // Blue
            "#22c55e", // Green
            "#f59e0b", // Orange
            "#ef4444", // Red
            "#8b5cf6", // Purple
            "#ec4899", // Pink
            "#14b8a6", // Teal
            "#f97316"  // Deep Orange
        };
        int index = (int) (Math.random() * colors.length);
        return colors[index];
    }
}