package com.training.oms.customer.controller;

import com.training.oms.customer.dto.CustomerRequest;
import com.training.oms.customer.dto.CustomerResponse;
import com.training.oms.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "CRUD operations for customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Create a customer")
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
    }

    @Operation(summary = "Get a customer by id")
    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @Operation(summary = "List all customers")
    @GetMapping
    public List<CustomerResponse> getAll() {
        return customerService.getAll();
    }

    @Operation(summary = "Replace a customer's details")
    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a customer")
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }
}
