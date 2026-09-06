package com.training.oms.product.controller;

import com.training.oms.product.dto.ProductRequest;
import com.training.oms.product.dto.ProductResponse;
import com.training.oms.product.dto.StockReservationRequest;
import com.training.oms.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Catalog and inventory operations")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Create a product")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @Operation(summary = "Get a product by id")
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @Operation(summary = "List all products")
    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.getAll();
    }

    @Operation(summary = "Reserve (deduct) stock for an order line")
    @PatchMapping("/{id}/reserve-stock")
    public ProductResponse reserveStock(@PathVariable Long id, @Valid @RequestBody StockReservationRequest request) {
        return productService.reserveStock(id, request.quantity());
    }
}
