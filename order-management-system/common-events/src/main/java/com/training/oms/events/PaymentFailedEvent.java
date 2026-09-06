package com.training.oms.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by payment-service to the "payment-events" Kafka topic when a
 * simulated payment fails. This drives the compensating action in the Saga
 * (order-service marks the order as FAILED / releases stock).
 */
public record PaymentFailedEvent(
        Long orderId,
        BigDecimal amount,
        String reason,
        Instant occurredAt
) {
}
