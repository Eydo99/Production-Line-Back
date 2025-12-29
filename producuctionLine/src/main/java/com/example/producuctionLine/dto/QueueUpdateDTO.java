package com.example.producuctionLine.dto;

import com.example.producuctionLine.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueueUpdateDTO {
    private String queueId;
    private int currentSize;
    private List<ProductDTO> products; // Include product list with colors

    // Constructor for backward compatibility (without products)
    public QueueUpdateDTO(String queueId, int currentSize) {
        this.queueId = queueId;
        this.currentSize = currentSize;
        this.products = List.of();
    }

    /**
     * DTO for Product information
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductDTO {
        private String id;
        private String color;
        private long createdAt;

        public static ProductDTO fromProduct(Product product) {
            return new ProductDTO(
                product.getId(),
                product.getColor(),
                product.getCreatedAt()
            );
        }
    }
}