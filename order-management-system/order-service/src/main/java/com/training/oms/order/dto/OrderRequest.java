package com.training.oms.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotNull Long customerId,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
    public record OrderItemRequest(@NotNull Long productId, @Min(1) int quantity) {
    }
}
