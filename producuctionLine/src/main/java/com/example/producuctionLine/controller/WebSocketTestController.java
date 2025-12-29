package com.example.producuctionLine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import com.example.producuctionLine.service.WebSocketBroadcaster;
import com.example.producuctionLine.dto.MachineUpdateDTO;
import com.example.producuctionLine.model.Queue;
import com.example.producuctionLine.model.Product;

@Controller
public class WebSocketTestController {
    
    @Autowired
    private WebSocketBroadcaster broadcaster;
    
    @MessageMapping("/test/queue")
    public void testQueue() {
        // Create a test queue with products
        Queue testQueue = new Queue("Q1", 100, 100);
        
        // Add some test products (using default Product constructor)
        Product p1 = new Product();
        p1.setId("P1");
        p1.setColor("#FF0000"); // Red
        testQueue.enqueue(p1);
        
        Product p2 = new Product();
        p2.setId("P2");
        p2.setColor("#00FF00"); // Green
        testQueue.enqueue(p2);
        
        Product p3 = new Product();
        p3.setId("P3");
        p3.setColor("#0000FF"); // Blue
        testQueue.enqueue(p3);
        
        // FIXED: Pass Queue object instead of using DTO constructor
        broadcaster.broadcastQueueUpdate(testQueue);
    }
    
    @MessageMapping("/test/machine")
    public void testMachine() {
        broadcaster.broadcastMachineUpdate(
            new MachineUpdateDTO("M1", "PROCESSING", "#FF0000")
        );
    }
}