package com.training.oms.order.service;

import com.training.oms.events.OrderCreatedEvent;
import com.training.oms.order.client.CustomerGateway;
import com.training.oms.order.client.ProductDto;
import com.training.oms.order.client.ProductGateway;
import com.training.oms.order.domain.Order;
import com.training.oms.order.domain.OrderItem;
import com.training.oms.order.dto.OrderRequest;
import com.training.oms.order.dto.OrderResponse;
import com.training.oms.order.exception.OrderNotFoundException;
import com.training.oms.order.messaging.OrderEventProducer;
import com.training.oms.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerGateway customerGateway;
    private final ProductGateway productGateway;
    private final OrderEventProducer orderEventProducer;

    public OrderService(OrderRepository orderRepository, CustomerGateway customerGateway,
                         ProductGateway productGateway, OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.customerGateway = customerGateway;
        this.productGateway = productGateway;
        this.orderEventProducer = orderEventProducer;
    }

    public OrderResponse createOrder(OrderRequest request) {
        // 1. Validate the customer exists (synchronous call, circuit-breaker protected).
        customerGateway.getCustomer(request.customerId());

        // 2. Price each line from the catalog, then reserve stock (deduct inventory).
        List<OrderItem> items = request.items().stream()
                .map(line -> {
                    ProductDto product = productGateway.getProduct(line.productId());
                    productGateway.reserveStock(line.productId(), line.quantity());
                    return new OrderItem(product.id(), line.quantity(), product.price());
                })
                .toList();

        // 3. Persist the order locally - this is the first step of the Saga.
        Order order = orderRepository.save(new Order(request.customerId(), items));

        // 4. Publish an event so payment-service can continue the Saga asynchronously.
        orderEventProducer.publishOrderCreated(new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getItems().stream()
                        .map(i -> new OrderCreatedEvent.OrderLine(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                        .toList(),
                order.getTotalAmount(),
                Instant.now()));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return OrderResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream().map(OrderResponse::from).toList();
    }

    private Order findOrThrow(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
