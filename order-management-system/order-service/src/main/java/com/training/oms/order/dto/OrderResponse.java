package com.training.oms.order.dto;

import com.training.oms.order.domain.Order;
import com.training.oms.order.domain.OrderItem;
import com.training.oms.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderItemResponse> items
) {
    public record OrderItemResponse(Long productId, int quantity, BigDecimal unitPrice) {
        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getProductId(), item.getQuantity(), item.getUnitPrice());
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(), order.getCustomerId(), order.getStatus(), order.getTotalAmount(),
                order.getCreatedAt(), order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
