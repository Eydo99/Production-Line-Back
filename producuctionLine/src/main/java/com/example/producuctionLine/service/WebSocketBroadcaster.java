package com.example.producuctionLine.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.example.producuctionLine.dto.MachineUpdateDTO;
import com.example.producuctionLine.dto.QueueUpdateDTO;
import com.example.producuctionLine.model.Queue;
import java.util.stream.Collectors;

@Service
public class WebSocketBroadcaster {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public WebSocketBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        System.out.println("✅ WebSocketBroadcaster initialized");
    }
    
    
    public void broadcastQueueUpdate(Queue queue) {
        try {
            // Convert ALL products to DTOs with colors
            var productDTOs = queue.getProductList().stream()
                .map(QueueUpdateDTO.ProductDTO::fromProduct)
                .collect(Collectors.toList());
            
            QueueUpdateDTO update = new QueueUpdateDTO(
                queue.getId(),
                queue.size(),
                productDTOs  // ALWAYS include products
            );
            
            System.out.println("📡 Broadcasting Queue Update: " + queue.getId() + 
                             " (size: " + update.getCurrentSize() + 
                             ", products: " + productDTOs.size() + ")");
            
            messagingTemplate.convertAndSend("/topic/queues", update);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to broadcast queue update: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void broadcastMachineUpdate(MachineUpdateDTO update) {
        try {
            System.out.println("📡 Broadcasting Machine Update: " + update);
            messagingTemplate.convertAndSend("/topic/machines", update);
            System.out.println("✅ Machine update sent successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to broadcast machine update: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void broadcastStatistics(java.util.Map<String, Object> stats) {
        try {
            messagingTemplate.convertAndSend("/topic/statistics", (Object) stats);
        } catch (Exception e) {
            System.err.println("❌ Failed to broadcast statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
}