package com.example.producuctionLine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Connection entity - represents links between queues and machines
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Connection {
    private String id;
    private String fromId; // Source node (Queue or Machine)
    private String toId;   // Target node (Queue or Machine)
    
    public Connection(String fromId, String toId) {
        this.id = fromId + "-" + toId;
        this.fromId = fromId;
        this.toId = toId;
    }
}