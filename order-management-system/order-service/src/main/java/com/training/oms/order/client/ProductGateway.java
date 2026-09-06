package com.training.oms.order.client;

import com.training.oms.order.exception.DownstreamServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductGateway {

    private static final Logger log = LoggerFactory.getLogger(ProductGateway.class);

    private final ProductClient productClient;

    public ProductGateway(ProductClient productClient) {
        this.productClient = productClient;
    }

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductDto getProduct(Long productId) {
        return productClient.getProduct(productId);
    }

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "reserveStockFallback")
    public ProductDto reserveStock(Long productId, int quantity) {
        return productClient.reserveStock(productId, new ProductClient.StockReservationRequest(quantity));
    }

    @SuppressWarnings("unused")
    private ProductDto getProductFallback(Long productId, Throwable throwable) {
        log.warn("product-service unavailable while fetching product {}: {}", productId, throwable.toString());
        throw new DownstreamServiceException("product-service is currently unavailable", throwable);
    }

    @SuppressWarnings("unused")
    private ProductDto reserveStockFallback(Long productId, int quantity, Throwable throwable) {
        log.warn("product-service unavailable while reserving stock for product {}: {}", productId, throwable.toString());
        throw new DownstreamServiceException("product-service is currently unavailable", throwable);
    }
}
