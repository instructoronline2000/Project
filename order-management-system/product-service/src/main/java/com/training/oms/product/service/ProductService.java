package com.training.oms.product.service;

import com.training.oms.product.domain.Product;
import com.training.oms.product.dto.ProductRequest;
import com.training.oms.product.dto.ProductResponse;
import com.training.oms.product.exception.InsufficientStockException;
import com.training.oms.product.exception.ProductNotFoundException;
import com.training.oms.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductRequest request) {
        Product saved = productRepository.save(
                new Product(request.name(), request.description(), request.price(), request.stockQuantity()));
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    /**
     * Reserves (permanently deducts) stock for an order line. Called by
     * order-service through OpenFeign when an order is created.
     */
    public ProductResponse reserveStock(Long id, int quantity) {
        Product product = findOrThrow(id);
        if (!product.hasSufficientStock(quantity)) {
            throw new InsufficientStockException(id, quantity, product.getStockQuantity());
        }
        product.reduceStock(quantity);
        return ProductResponse.from(product);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
