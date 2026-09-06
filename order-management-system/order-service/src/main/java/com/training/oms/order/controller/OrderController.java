package com.training.oms.order.controller;

import com.training.oms.order.dto.OrderRequest;
import com.training.oms.order.dto.OrderResponse;
import com.training.oms.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order orchestration: validates customer/stock, then drives the payment Saga")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Place a new order", description = "Calls customer-service and product-service via Feign, then publishes OrderCreatedEvent to Kafka")
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
    }

    @Operation(summary = "Get an order by id")
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @Operation(summary = "List all orders")
    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.getAll();
    }

    @Operation(summary = "List all orders for a customer")
    @GetMapping("/customer/{customerId}")
    public List<OrderResponse> getByCustomer(@PathVariable Long customerId) {
        return orderService.getByCustomerId(customerId);
    }
}
