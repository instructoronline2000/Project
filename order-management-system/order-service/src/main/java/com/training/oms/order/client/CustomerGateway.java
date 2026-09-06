package com.training.oms.order.client;

import com.training.oms.order.exception.DownstreamServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Wraps {@link CustomerClient} with Resilience4j policies (Intermediate
 * lesson 05: Circuit Breaker). Kept as its own Spring bean - not a private
 * method on OrderService - because Resilience4j's annotations rely on a
 * Spring AOP proxy, which is bypassed on self-invocation (this.method()).
 */
@Component
public class CustomerGateway {

    private static final Logger log = LoggerFactory.getLogger(CustomerGateway.class);

    private final CustomerClient customerClient;

    public CustomerGateway(CustomerClient customerClient) {
        this.customerClient = customerClient;
    }

    @Retry(name = "customerService")
    @CircuitBreaker(name = "customerService", fallbackMethod = "fallback")
    public CustomerDto getCustomer(Long customerId) {
        return customerClient.getCustomer(customerId);
    }

    @SuppressWarnings("unused") // invoked reflectively by Resilience4j
    private CustomerDto fallback(Long customerId, Throwable throwable) {
        log.warn("customer-service unavailable while fetching customer {}: {}", customerId, throwable.toString());
        throw new DownstreamServiceException("customer-service is currently unavailable", throwable);
    }
}
