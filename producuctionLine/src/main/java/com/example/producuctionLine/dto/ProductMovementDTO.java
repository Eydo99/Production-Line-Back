package com.example.producuctionLine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductMovementDTO {
    private String productId;
    private String connectionId;
    private String fromId;
    private String toId;
    private String color;
    private double progress; // 0.0 to 1.0
    private long timestamp;
}