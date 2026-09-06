package com.training.oms.product.dto;

import com.training.oms.product.domain.Product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal price, int stockQuantity) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStockQuantity());
    }
}
