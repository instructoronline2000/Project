package com.training.oms.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound DTO validated with Bean Validation (Beginner lesson 04).
 */
public record CustomerRequest(
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
        String phone,
        String address
) {
}
