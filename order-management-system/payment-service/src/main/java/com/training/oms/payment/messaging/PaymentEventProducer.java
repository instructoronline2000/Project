package com.training.oms.payment.messaging;

import com.training.oms.events.PaymentCompletedEvent;
import com.training.oms.events.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    public static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCompleted(PaymentCompletedEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event);
        log.info("Published PaymentCompletedEvent for order {}", event.orderId());
    }

    public void publishFailed(PaymentFailedEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event);
        log.info("Published PaymentFailedEvent for order {}: {}", event.orderId(), event.reason());
    }
}
