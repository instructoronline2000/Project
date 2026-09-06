package com.training.oms.notification;

import com.training.oms.events.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationStore notificationStore;

    public NotificationEventListener(NotificationStore notificationStore) {
        this.notificationStore = notificationStore;
    }

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        send(event.orderId(), "Your order #" + event.orderId() + " has been received and totals "
                + event.totalAmount());
    }

    private void send(Long orderId, String message) {
        // Simulated delivery - a real implementation would call an email/SMS gateway.
        log.info("[NOTIFICATION] -> customer of order {}: {}", orderId, message);
        notificationStore.add(new NotificationRecord(Instant.now(), orderId, "EMAIL", message));
    }
}
