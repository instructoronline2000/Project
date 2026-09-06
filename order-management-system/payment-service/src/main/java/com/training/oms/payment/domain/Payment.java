package com.training.oms.payment.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String declineReason;
    private Instant processedAt;

    protected Payment() {
    }

    public static Payment approved(Long orderId, BigDecimal amount) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.amount = amount;
        payment.status = PaymentStatus.APPROVED;
        payment.processedAt = Instant.now();
        return payment;
    }

    public static Payment declined(Long orderId, BigDecimal amount, String reason) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.amount = amount;
        payment.status = PaymentStatus.DECLINED;
        payment.declineReason = reason;
        payment.processedAt = Instant.now();
        return payment;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
