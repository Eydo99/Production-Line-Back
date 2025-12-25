package com.example.producuctionLine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketTestController {

    @Autowired
    private com.example.producuctionLine.service.WebSocketBroadcaster broadcaster;

    @MessageMapping("/test/queue")
    public void testQueue() {
        broadcaster.broadcastQueueUpdate(
            new com.example.producuctionLine.dto.QueueUpdateDTO("Q1", 5)
        );
    }

    @MessageMapping("/test/machine")
    public void testMachine() {
        broadcaster.broadcastMachineUpdate(
            new com.example.producuctionLine.dto.MachineUpdateDTO("M1", "PROCESSING", "#FF0000")
        );
    }
}