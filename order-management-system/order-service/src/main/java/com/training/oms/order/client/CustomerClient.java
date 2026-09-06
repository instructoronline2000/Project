package com.training.oms.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative HTTP client resolved through Eureka (Intermediate lesson 04:
 * Inter-service communication with OpenFeign). "customer-service" is the
 * spring.application.name registered by the target service - no hardcoded URL.
 */
@FeignClient(name = "customer-service")
public interface CustomerClient {

    @GetMapping("/api/customers/{id}")
    CustomerDto getCustomer(@PathVariable("id") Long id);
}
