package com.training.oms.payment.messaging;

import com.training.oms.events.OrderCreatedEvent;
import com.training.oms.payment.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final PaymentProcessor paymentProcessor;

    public OrderEventListener(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    @KafkaListener(topics = "order-events", groupId = "payment-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Processing payment for order {} (amount={})", event.orderId(), event.totalAmount());
        paymentProcessor.process(event);
    }
}
