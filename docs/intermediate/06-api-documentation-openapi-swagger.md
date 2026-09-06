# Intermediate · Lesson 06 — API Documentation with OpenAPI & Swagger UI

## Goal

Give every service (and the gateway) a live, browsable, always-up-to-date API
contract — generated from the code itself, not hand-maintained separately.

## Why generated docs, not a hand-written spec?

A hand-written Postman collection or Word doc goes stale the moment a
controller changes. **springdoc-openapi** instead inspects your running
Spring MVC/WebFlux controllers, DTOs and Bean Validation annotations at
startup and generates the OpenAPI 3 spec from them — the docs can never drift
from the actual code, because they *are* the code, reflected.

## Adding it to a service

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

See e.g. [`customer-service/pom.xml`](../../order-management-system/customer-service/pom.xml).
That's the entire integration — no controller changes required. Start the
service and two endpoints exist automatically:

* `http://localhost:8081/v3/api-docs` — the raw OpenAPI 3 JSON spec.
* `http://localhost:8081/swagger-ui.html` — an interactive UI to browse and **try out** every endpoint.

## Customizing the spec

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customerServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Customer Service API")
                .description("Beginner module: CRUD for customers.")
                .version("v1"));
    }
}
```

See [`OpenApiConfig.java`](../../order-management-system/customer-service/src/main/java/com/training/oms/customer/config/OpenApiConfig.java)
(one per service). This is what puts a proper title/description on the
Swagger UI page instead of a generic default.

## Documenting individual endpoints

```java
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order orchestration: validates customer/stock, then drives the payment Saga")
public class OrderController {

    @Operation(summary = "Place a new order",
               description = "Calls customer-service and product-service via Feign, then publishes OrderCreatedEvent to Kafka")
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) { ... }
}
```

See [`OrderController.java`](../../order-management-system/order-service/src/main/java/com/training/oms/order/controller/OrderController.java)
(also applied to
[`CustomerController`](../../order-management-system/customer-service/src/main/java/com/training/oms/customer/controller/CustomerController.java)
and
[`ProductController`](../../order-management-system/product-service/src/main/java/com/training/oms/product/controller/ProductController.java)).

* `@Tag` groups every endpoint in the controller under one named section in Swagger UI.
* `@Operation(summary, description)` documents intent that isn't obvious from the method signature alone — *why* this endpoint calls two other services and publishes a Kafka event, not just *what* HTTP verb/path it uses.
* springdoc also reads your existing `@Valid`/`@NotBlank`/`@Email` Bean Validation annotations (Beginner Lesson 04) and request/response DTOs automatically — request/response schemas in Swagger UI need no extra annotations at all.

## Aggregating every service behind the gateway

Asking students to remember five different ports (`8081`–`8085`) for five
different Swagger UIs doesn't scale. Instead, the gateway hosts **one**
Swagger UI that lists every service, using the exact routing mechanism from
[Lesson 03](03-api-gateway.md):

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: customer-service-docs
          uri: lb://customer-service
          predicates:
            - Path=/docs/customer-service/v3/api-docs
          filters:
            - RewritePath=/docs/customer-service/v3/api-docs, /v3/api-docs
        # ...one such route per service

springdoc:
  swagger-ui:
    urls:
      - name: customer-service
        url: /docs/customer-service/v3/api-docs
      - name: product-service
        url: /docs/product-service/v3/api-docs
      # ...
```

See [`api-gateway/application.yml`](../../order-management-system/api-gateway/src/main/resources/application.yml).

* Each `*-docs` route proxies `/docs/{service}/v3/api-docs` through to that service's real `/v3/api-docs`, load-balanced via Eureka (`lb://`) exactly like the API routes.
* `springdoc.swagger-ui.urls` tells the gateway's own Swagger UI about each spec URL — the UI renders a dropdown to switch between services.
* The gateway itself needs `springdoc-openapi-starter-webflux-ui` (not `-webmvc-ui`) because Spring Cloud Gateway runs on WebFlux, not Spring MVC — see [`api-gateway/pom.xml`](../../order-management-system/api-gateway/pom.xml).

## Don't forget: exempt docs from authentication

[Advanced Lesson 03](../advanced/03-security-jwt.md) locks every route behind
a JWT except an explicit allow-list. Swagger UI's own assets and every
`/v3/api-docs`/`/docs/**` path must be on that allow-list, or students get a
confusing `401` just trying to *view* the API before they've even logged in:

```java
private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/login", "/actuator",
        "/swagger-ui", "/v3/api-docs", "/docs/", "/webjars");
```

See [`JwtAuthenticationFilter.java`](../../order-management-system/api-gateway/src/main/java/com/training/oms/gateway/security/JwtAuthenticationFilter.java).

## Run it

```bash
# per-service (direct)
curl http://localhost:8081/v3/api-docs | jq .info
open http://localhost:8081/swagger-ui.html

# aggregated, through the gateway (needs eureka-server + every service + api-gateway running)
open http://localhost:8080/swagger-ui.html
```

## Try it yourself

1. Add `@Parameter(description = "...")` to the `@PathVariable Long id` parameters in `CustomerController` and observe the extra description appear next to that field in Swagger UI.
2. Add a `payment-service-docs` style route + `swagger-ui.urls` entry for `notification-service`, matching the existing pattern.

## Key takeaways

* springdoc-openapi generates OpenAPI 3 docs (and an interactive Swagger UI) directly from your controllers/DTOs — no hand-maintained spec to go stale.
* Use WebMVC's starter for servlet-based services, WebFlux's starter for Spring Cloud Gateway.
* An API Gateway can aggregate every downstream service's spec into one Swagger UI using the same `lb://` routing already used for real traffic.
* Always allow-list documentation endpoints in any authentication filter, or the docs become unreachable before anyone can log in to use them.

Next: [Advanced Lesson 01 — Event-Driven Architecture with Kafka](../advanced/01-event-driven-kafka.md)
