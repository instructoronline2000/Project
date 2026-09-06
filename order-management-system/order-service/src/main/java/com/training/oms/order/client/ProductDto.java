package com.training.oms.order.client;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, String description, BigDecimal price, int stockQuantity) {
}
