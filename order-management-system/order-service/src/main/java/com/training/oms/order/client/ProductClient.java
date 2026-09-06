package com.training.oms.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);

    @PatchMapping("/api/products/{id}/reserve-stock")
    ProductDto reserveStock(@PathVariable("id") Long id, @RequestBody StockReservationRequest request);

    record StockReservationRequest(int quantity) {
    }
}
