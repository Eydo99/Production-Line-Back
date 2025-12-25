package com.example.producuctionLine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastQueueUpdate(com.example.producuctionLine.model.dto.QueueUpdateDTO update) {
        messagingTemplate.convertAndSend("/topic/queues", update);
    }

    public void broadcastMachineUpdate(com.example.producuctionLine.model.dto.MachineUpdateDTO update) {
        messagingTemplate.convertAndSend("/topic/machines", update);
    }

    public void broadcastStatistics() {
        // TODO: Implement statistics broadcasting if needed
    }
}
