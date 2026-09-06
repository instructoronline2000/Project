package com.training.oms.order.messaging;

import com.training.oms.events.PaymentCompletedEvent;
import com.training.oms.events.PaymentFailedEvent;
import com.training.oms.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Advanced module: Saga (choreography). order-service reacts to events
 * published by payment-service instead of calling it synchronously - each
 * service owns its own local transaction and the overall business
 * transaction is completed via this chain of events.
 *
 * <p>Uses the class-level {@code @KafkaListener} + per-type {@code @KafkaHandler}
 * pattern instead of a single method taking {@code Object}: with a plain
 * {@code Object} parameter, Spring Kafka treats the listener as wanting the raw
 * {@link org.apache.kafka.clients.consumer.ConsumerRecord} (since {@code Object}
 * is assignable from it) and skips payload extraction entirely, so every
 * {@code instanceof} check would silently and permanently fail.
 */
@Component
@KafkaListener(topics = "payment-events", groupId = "order-service")
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderRepository orderRepository;

    public PaymentEventListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaHandler
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            order.markPaid();
            log.info("Order {} marked PAID after payment {}", order.getId(), event.paymentId());
        }, () -> log.warn("Received PaymentCompletedEvent for unknown order {}", event.orderId()));
    }

    @KafkaHandler
    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            order.markFailed();
            log.info("Order {} marked FAILED - payment declined: {}", order.getId(), event.reason());
            // A production Saga would also emit a compensating "ReleaseStockCommand"
            // here so product-service restores the reserved inventory.
        }, () -> log.warn("Received PaymentFailedEvent for unknown order {}", event.orderId()));
    }
}
