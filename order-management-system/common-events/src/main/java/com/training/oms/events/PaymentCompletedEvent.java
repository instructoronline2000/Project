package com.training.oms.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by payment-service to the "payment-events" Kafka topic when a
 * simulated payment succeeds. order-service and notification-service consume this.
 */
public record PaymentCompletedEvent(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        Instant occurredAt
) {
}
