package com.training.oms.payment.dto;

import com.training.oms.payment.domain.Payment;
import com.training.oms.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id, Long orderId, BigDecimal amount, PaymentStatus status, String declineReason, Instant processedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(),
                payment.getStatus(), payment.getDeclineReason(), payment.getProcessedAt());
    }
}
