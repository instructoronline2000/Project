package com.training.oms.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Published by order-service to the "order-events" Kafka topic once an order
 * has been persisted with status CREATED.
 */
public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        List<OrderLine> items,
        BigDecimal totalAmount,
        Instant occurredAt
) {
    public record OrderLine(Long productId, int quantity, BigDecimal unitPrice) {
    }
}
