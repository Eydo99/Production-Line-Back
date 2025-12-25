package com.example.producuctionLine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MachineUpdateDTO {
    private String machineId;
    private String status; // "PROCESSING", "IDLE", "FLASHING"
    private String productColor; // Hex code or name
}
