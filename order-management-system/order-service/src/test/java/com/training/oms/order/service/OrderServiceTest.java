package com.training.oms.order.service;

import com.training.oms.order.client.CustomerDto;
import com.training.oms.order.client.CustomerGateway;
import com.training.oms.order.client.ProductDto;
import com.training.oms.order.client.ProductGateway;
import com.training.oms.order.dto.OrderRequest;
import com.training.oms.order.dto.OrderResponse;
import com.training.oms.order.messaging.OrderEventProducer;
import com.training.oms.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Intermediate lesson 04/05: unit-tests the orchestration logic with the
 * Feign gateways mocked out - no network, no Eureka, no Kafka broker needed.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerGateway customerGateway;
    @Mock
    private ProductGateway productGateway;
    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_reservesStockAndPublishesEvent_whenCustomerAndProductExist() {
        when(customerGateway.getCustomer(1L)).thenReturn(new CustomerDto(1L, "Alice", "a@x.com", null, null));
        ProductDto product = new ProductDto(10L, "Mouse", "desc", new BigDecimal("19.99"), 100);
        when(productGateway.getProduct(10L)).thenReturn(product);
        when(productGateway.reserveStock(10L, 2)).thenReturn(product);
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest(1L, List.of(new OrderRequest.OrderItemRequest(10L, 2)));
        OrderResponse response = orderService.createOrder(request);

        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("39.98"));
        verify(productGateway).reserveStock(10L, 2);
        verify(orderEventProducer).publishOrderCreated(any());
    }
}
