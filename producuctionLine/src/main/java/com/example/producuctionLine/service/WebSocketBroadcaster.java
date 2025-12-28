package com.example.producuctionLine.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    // Remove @Autowired - Spring auto-injects with single constructor
    public WebSocketBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastQueueUpdate(com.example.producuctionLine.dto.QueueUpdateDTO update) {
        messagingTemplate.convertAndSend("/topic/queues", update);
    }

    public void broadcastMachineUpdate(com.example.producuctionLine.dto.MachineUpdateDTO update) {
        messagingTemplate.convertAndSend("/topic/machines", update);
    }

    public void broadcastStatistics() {
        // TODO: Implement statistics broadcasting if needed
    }

    public void broadcastConnectionUpdate(com.example.producuctionLine.dto.ConnectionUpdateDTO update) {
    messagingTemplate.convertAndSend("/topic/connections", update);
}

public void broadcastProductMovement(com.example.producuctionLine.dto.ProductMovementDTO movement) {
    messagingTemplate.convertAndSend("/topic/product-movement", movement);
}


}