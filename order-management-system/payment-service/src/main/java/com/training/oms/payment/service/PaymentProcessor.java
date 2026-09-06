package com.training.oms.payment.service;

import com.training.oms.events.OrderCreatedEvent;
import com.training.oms.events.PaymentCompletedEvent;
import com.training.oms.events.PaymentFailedEvent;
import com.training.oms.payment.domain.Payment;
import com.training.oms.payment.messaging.PaymentEventProducer;
import com.training.oms.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PaymentProcessor {

    // Simulated fraud/limit rule: orders above this amount are declined so the
    // course can demonstrate the compensating (FAILED) path of the Saga on demand.
    static final BigDecimal AUTO_DECLINE_THRESHOLD = new BigDecimal("1000.00");

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentProcessor(PaymentRepository paymentRepository, PaymentEventProducer paymentEventProducer) {
        this.paymentRepository = paymentRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Transactional
    public void process(OrderCreatedEvent event) {
        boolean approved = event.totalAmount().compareTo(AUTO_DECLINE_THRESHOLD) <= 0;

        if (approved) {
            Payment payment = paymentRepository.save(Payment.approved(event.orderId(), event.totalAmount()));
            paymentEventProducer.publishCompleted(new PaymentCompletedEvent(
                    payment.getId(), event.orderId(), event.totalAmount(), Instant.now()));
        } else {
            String reason = "Amount exceeds auto-approval threshold of " + AUTO_DECLINE_THRESHOLD;
            paymentRepository.save(Payment.declined(event.orderId(), event.totalAmount(), reason));
            paymentEventProducer.publishFailed(new PaymentFailedEvent(
                    event.orderId(), event.totalAmount(), reason, Instant.now()));
        }
    }
}
