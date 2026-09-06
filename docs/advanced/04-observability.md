# Advanced · Lesson 04 — Observability: Actuator, Prometheus & Grafana

## Goal

Understand the three pillars of observability (metrics, health, logs) and
wire up a real metrics pipeline: Micrometer → Prometheus → Grafana.

## Why "it works on my machine" isn't enough

Across five running services communicating over HTTP and Kafka, a slow order
could be caused by the database, a downstream Feign call, a saturated
consumer, or a network blip — and there's no single stack trace to look at.
Observability is what lets you answer "which one?" *without* attaching a
debugger to a running production service.

## Health: Spring Boot Actuator

Every service already exposes:

```bash
curl http://localhost:8083/actuator/health
```

See any service's `application.yml`, e.g.
[`order-service`](../../order-management-system/order-service/src/main/resources/application.yml):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,circuitbreakers,prometheus
  health:
    circuitbreakers:
      enabled: true
```

`health.circuitbreakers.enabled: true` is what surfaces the Resilience4j
circuit breaker states (Intermediate Lesson 05) inside the health payload —
a load balancer or Kubernetes readiness probe can use this to route traffic
away from an instance whose downstream dependencies are all failing.

## Metrics: Micrometer + Prometheus

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

See [`order-service/pom.xml`](../../order-management-system/order-service/pom.xml).
Micrometer is a vendor-neutral metrics facade already built into Actuator;
adding this one registry dependency makes `/actuator/prometheus` emit
metrics in the text format Prometheus expects — no code changes, because
Spring Boot auto-configures HTTP request timers, JVM memory/GC stats,
DataSource pool stats, and (thanks to the dependency already on the
classpath) Resilience4j circuit breaker metrics automatically.

```bash
curl http://localhost:8083/actuator/prometheus | grep resilience4j_circuitbreaker_state
```

## Wiring up the stack

See [`observability/docker-compose.observability.yml`](../../order-management-system/observability/docker-compose.observability.yml)
and [`observability/prometheus.yml`](../../order-management-system/observability/prometheus.yml):

```yaml
scrape_configs:
  - job_name: order-service
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["host.docker.internal:8083"]
```

Prometheus **pulls** metrics on an interval (10s here) rather than services
pushing them — this is why the target is `host.docker.internal:8083`
(reachable from inside the Prometheus container back to your host machine,
where `order-service` is running via `mvn spring-boot:run`).

```bash
cd order-management-system/observability
docker compose -f docker-compose.observability.yml up -d
```

* Prometheus UI: http://localhost:9090 — try the query `http_server_requests_seconds_count`.
* Grafana: http://localhost:3000 (`admin`/`admin`) — add Prometheus (`http://prometheus:9090`) as a data source and build a dashboard.

## Logs

Every listener/service in this project logs at meaningful points (order
created, payment processed, saga transitions) using SLF4J
(`LoggerFactory.getLogger(...)`), e.g.
[`PaymentEventListener`](../../order-management-system/order-service/src/main/java/com/training/oms/order/messaging/PaymentEventListener.java).
In production, these would be shipped to a centralized log store (ELK,
Loki, ...) and correlated across services using a trace ID — a natural next
step is adding `micrometer-tracing` + `spring-cloud-sleuth`-equivalent
propagation so a single order's logs are correlatable across all five
services. Left as an extension exercise for this course.

## Try it yourself

1. Add `micrometer-registry-prometheus` to `payment-service` too, and add a scrape job for it in `prometheus.yml`.
2. In Grafana, build a panel graphing `resilience4j_circuitbreaker_state` for `order-service`, then stop `customer-service` and watch the state change live.

## Key takeaways

* Actuator health endpoints (including Resilience4j state) let orchestrators make automated traffic-routing decisions.
* Micrometer is a facade — adding a registry dependency (Prometheus, here) is the only code change needed to export metrics in that system's format.
* Prometheus scrapes (pulls); design your services to expose a stable metrics endpoint, not to push.

Next: [Lesson 05 — Containerization: Docker & Kubernetes](05-containerization-docker-kubernetes.md)
