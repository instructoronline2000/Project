package com.training.oms.product.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super("Product %d has insufficient stock: requested=%d, available=%d"
                .formatted(productId, requested, available));
    }
}
