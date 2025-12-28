package com.example.producuctionLine.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.example.producuctionLine.dto.MachineUpdateDTO;
import com.example.producuctionLine.dto.QueueUpdateDTO;

@Service
public class WebSocketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        System.out.println("✅ WebSocketBroadcaster initialized");
    }

    public void broadcastQueueUpdate(QueueUpdateDTO update) {
        try {
            System.out.println("📡 Broadcasting Queue Update: " + update);
            messagingTemplate.convertAndSend("/topic/queues", update);
            System.out.println("✅ Queue update sent successfully");
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

    public void broadcastStatistics() {
        // TODO: Implement statistics broadcasting if needed
    }
}