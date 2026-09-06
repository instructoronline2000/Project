package com.training.oms.order.messaging;

import com.training.oms.events.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    public static final String TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        // Keying by orderId (as text) keeps every event for the same order on
        // the same partition, preserving per-order ordering guarantees.
        kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCreatedEvent for order {}", event.orderId(), ex);
                    } else {
                        log.info("Published OrderCreatedEvent for order {}", event.orderId());
                    }
                });
    }
}
