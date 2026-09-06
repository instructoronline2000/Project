# Intermediate · Lesson 04 — Inter-Service Communication with OpenFeign

## Goal

Understand how `order-service` calls `customer-service` and `product-service`
synchronously, using a declarative HTTP client instead of hand-rolled
`RestTemplate`/`WebClient` code.

## The declarative client

```java
@FeignClient(name = "customer-service")
public interface CustomerClient {

    @GetMapping("/api/customers/{id}")
    CustomerDto getCustomer(@PathVariable("id") Long id);
}
```

See [`CustomerClient.java`](../../order-management-system/order-service/src/main/java/com/training/oms/order/client/CustomerClient.java).

You write an **interface** with Spring MVC annotations describing the HTTP
call — no implementation. At startup, OpenFeign generates a dynamic proxy
that:

1. Resolves `"customer-service"` to a live instance via Eureka (same mechanism as `lb://` in Lesson 03).
2. Builds the HTTP request from the method signature.
3. Deserializes the JSON response into `CustomerDto`.

Enable it once, application-wide:

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrderServiceApplication { ... }
```

See [`OrderServiceApplication.java`](../../order-management-system/order-service/src/main/java/com/training/oms/order/OrderServiceApplication.java).

## Using it from the service layer

```java
public OrderResponse createOrder(OrderRequest request) {
    customerGateway.getCustomer(request.customerId());   // throws if not found (404 -> FeignException)

    List<OrderItem> items = request.items().stream()
            .map(line -> {
                ProductDto product = productGateway.getProduct(line.productId());
                productGateway.reserveStock(line.productId(), line.quantity());
                return new OrderItem(product.id(), line.quantity(), product.price());
            })
            .toList();
    ...
}
```

See [`OrderService.java`](../../order-management-system/order-service/src/main/java/com/training/oms/order/service/OrderService.java).
Three remote calls, written like local method calls.

## Why `CustomerGateway`/`ProductGateway` instead of calling the Feign client directly?

You'll notice `OrderService` doesn't call `CustomerClient` directly — it goes
through [`CustomerGateway`](../../order-management-system/order-service/src/main/java/com/training/oms/order/client/CustomerGateway.java):

```java
@Component
public class CustomerGateway {
    private final CustomerClient customerClient;

    @Retry(name = "customerService")
    @CircuitBreaker(name = "customerService", fallbackMethod = "fallback")
    public CustomerDto getCustomer(Long customerId) {
        return customerClient.getCustomer(customerId);
    }
    ...
}
```

That's the Resilience4j policy from the next lesson — kept in its own Spring
bean rather than as a private method on `OrderService`, for a reason you'll
learn there. For this lesson, the takeaway is simpler: **wrap remote calls in
a dedicated component** so retry/circuit-breaker/caching concerns don't
clutter your orchestration logic.

## Timeouts

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 2000
        readTimeout: 2000
```

See [`order-service/application.yml`](../../order-management-system/order-service/src/main/resources/application.yml).
**Always** set explicit timeouts on inter-service calls. An unbounded call to
a hung downstream service will eventually exhaust your own service's thread
pool — this is the single most common cause of cascading failure in
microservices, and exactly the problem Lesson 05's circuit breaker exists to
contain.

## Try it yourself

Stop `customer-service`, then `POST /api/orders` through `order-service`.
Watch the Feign call fail with a connection error after ~2 seconds (the
configured timeout) rather than hanging forever. In the next lesson you'll
add a circuit breaker so repeated failures like this get detected and
short-circuited automatically.

## Key takeaways

* `@FeignClient(name = "...")` + Eureka gives you type-safe, load-balanced HTTP clients with almost no boilerplate.
* The `name` must match the target service's `spring.application.name`.
* Always set explicit connect/read timeouts on inter-service calls.
* Wrap Feign calls in a dedicated gateway component to keep resilience policies (next lesson) separate from business logic.

Next: [Lesson 05 — Resilience4j: Circuit Breaker & Retry](05-resilience4j.md)
