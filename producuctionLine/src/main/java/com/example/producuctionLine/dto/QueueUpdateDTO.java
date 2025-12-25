package com.example.producuctionLine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueueUpdateDTO {
    private String queueId;
    private int currentSize;
}
