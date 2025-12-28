package com.example.producuctionLine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionUpdateDTO {
    private String connectionId;
    private String fromId;
    private String toId;
    private double fromX;
    private double fromY;
    private double toX;
    private double toY;
    private String action; // "created", "deleted", "updated"
}