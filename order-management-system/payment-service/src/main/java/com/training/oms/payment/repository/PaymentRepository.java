package com.training.oms.payment.repository;

import com.training.oms.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
