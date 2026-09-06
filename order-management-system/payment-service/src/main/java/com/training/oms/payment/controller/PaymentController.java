package com.training.oms.payment.controller;

import com.training.oms.payment.dto.PaymentResponse;
import com.training.oms.payment.repository.PaymentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll().stream().map(PaymentResponse::from).toList();
    }
}
