package com.training.oms.notification;

import com.training.oms.events.PaymentCompletedEvent;
import com.training.oms.events.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Uses the class-level {@code @KafkaListener} + per-type {@code @KafkaHandler}
 * pattern instead of a single method taking {@code Object}: with a plain
 * {@code Object} parameter, Spring Kafka treats the listener as wanting the raw
 * {@link org.apache.kafka.clients.consumer.ConsumerRecord} (since {@code Object}
 * is assignable from it) and skips payload extraction entirely, so every
 * {@code instanceof} check would silently and permanently fail.
 */
@Component
@KafkaListener(topics = "payment-events", groupId = "notification-service")
public class PaymentEventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventNotificationListener.class);

    private final NotificationStore notificationStore;

    public PaymentEventNotificationListener(NotificationStore notificationStore) {
        this.notificationStore = notificationStore;
    }

    @KafkaHandler
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        send(event.orderId(), "Payment confirmed for order #" + event.orderId() + ". Thank you for your purchase!");
    }

    @KafkaHandler
    public void onPaymentFailed(PaymentFailedEvent event) {
        send(event.orderId(), "Payment for order #" + event.orderId() + " could not be processed: " + event.reason());
    }

    private void send(Long orderId, String message) {
        // Simulated delivery - a real implementation would call an email/SMS gateway.
        log.info("[NOTIFICATION] -> customer of order {}: {}", orderId, message);
        notificationStore.add(new NotificationRecord(Instant.now(), orderId, "EMAIL", message));
    }
}
