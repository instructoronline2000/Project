package com.training.oms.product.dto;

import jakarta.validation.constraints.Min;

/** Called by order-service (via OpenFeign) when an order is placed. */
public record StockReservationRequest(@Min(1) int quantity) {
}
