package com.example.producuctionLine.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketTestController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.producuctionLine.service.WebSocketBroadcaster broadcaster;

    @MessageMapping("/test/queue")
    public void testQueue() {
        broadcaster.broadcastQueueUpdate(new com.example.producuctionLine.model.dto.QueueUpdateDTO("Q1", 5));
    }

    @MessageMapping("/test/machine")
    public void testMachine() {
        broadcaster.broadcastMachineUpdate(
                new com.example.producuctionLine.model.dto.MachineUpdateDTO("M1", "PROCESSING", "#FF0000"));
    }
}
