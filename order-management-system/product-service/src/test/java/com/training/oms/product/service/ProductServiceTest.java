package com.training.oms.product.service;

import com.training.oms.product.domain.Product;
import com.training.oms.product.dto.ProductResponse;
import com.training.oms.product.exception.InsufficientStockException;
import com.training.oms.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void reserveStock_reducesQuantity_whenEnoughStockAvailable() {
        Product product = new Product("Mouse", "desc", BigDecimal.TEN, 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.reserveStock(1L, 4);

        assertThat(response.stockQuantity()).isEqualTo(6);
    }

    @Test
    void reserveStock_throwsInsufficientStockException_whenNotEnoughStock() {
        Product product = new Product("Mouse", "desc", BigDecimal.TEN, 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.reserveStock(1L, 5))
                .isInstanceOf(InsufficientStockException.class);
    }
}
