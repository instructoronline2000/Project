# Advanced · Lesson 06 — Testing Strategies with Testcontainers

## Goal

Round out the testing pyramid from the Beginner module (Lesson 05) with a
third style: full-stack tests against a **real Kafka broker** running in a
disposable Docker container, so async event flows are actually verified, not
mocked away.

## The testing pyramid, revisited

| Style | Example | Speed | What it proves |
|---|---|---|---|
| Unit (Mockito) | [`OrderServiceTest`](../../order-management-system/order-service/src/test/java/com/training/oms/order/service/OrderServiceTest.java) | ms | Business logic, in isolation |
| Integration (`@SpringBootTest` + H2) | [`CustomerControllerTest`](../../order-management-system/customer-service/src/test/java/com/training/oms/customer/controller/CustomerControllerTest.java) | ~seconds | HTTP + JPA wiring, real (in-memory) DB |
| **Testcontainers** | [`PaymentServiceKafkaIT`](../../order-management-system/payment-service/src/test/java/com/training/oms/payment/PaymentServiceKafkaIT.java) | seconds–minutes | Real infrastructure (a real Kafka broker), real `@KafkaListener` wiring |

Mocking Kafka (e.g. asserting `verify(kafkaTemplate).send(...)`) proves your
code *tried* to publish something — it proves nothing about whether a real
consumer would actually receive and process it correctly, especially with
JSON (de)serialization and consumer group configuration in the mix
([Advanced Lesson 01](01-event-driven-kafka.md)). Testcontainers closes that
gap by giving the test a real, disposable Kafka broker in a Docker container.

## The test

```java
@Testcontainers
@SpringBootTest(properties = { "eureka.client.enabled=false" })
class PaymentServiceKafkaIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired private PaymentRepository paymentRepository;

    @Test
    void orderCreatedEvent_isConsumedAndCreatesAnApprovedPayment() {
        kafkaTemplate.send("order-events", "100", new OrderCreatedEvent(...));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(paymentRepository.findAll()).anyMatch(p -> p.getOrderId().equals(100L)));
    }
}
```

See the full file:
[`PaymentServiceKafkaIT.java`](../../order-management-system/payment-service/src/test/java/com/training/oms/payment/PaymentServiceKafkaIT.java).

* `@Testcontainers` + `@Container` start a real, throwaway Kafka broker in Docker before the test class runs, and tear it down after.
* `@DynamicPropertySource` overrides `spring.kafka.bootstrap-servers` at runtime to point at whatever random port Testcontainers assigned — you never hardcode a port.
* The test publishes an event with the real `KafkaTemplate` bean from the Spring context (the exact same bean `order-service` uses in production) and then polls (`Awaitility.await()`) until the *real* `@KafkaListener` (`OrderEventListener` → `PaymentProcessor`) has asynchronously processed it and written a row. **Never `Thread.sleep()`** in async tests — poll with a timeout instead, or the test becomes flaky and slow.
* Notice the class is named `PaymentServiceKafkaIT`, not `*Test` — deliberately, per the convention introduced in [Beginner Lesson 05](../beginner/05-testing.md): it needs Docker running and takes longer, so it's meant for `mvn verify` via the Failsafe plugin, not every `mvn test`.

## Wiring it into the build (exercise)

This project intentionally does **not** bind Failsafe by default (to keep
`mvn test` fast and Docker-free for every earlier lesson). To actually run
`PaymentServiceKafkaIT`:

```bash
# Requires Docker running locally
cd order-management-system/payment-service
mvn test -Dtest=PaymentServiceKafkaIT
```

**Exercise:** add `maven-failsafe-plugin` to the parent POM's
`pluginManagement`, bind it to the `integration-test`/`verify` phases with
the default `**/*IT.java` include pattern, and confirm `mvn verify` now runs
this test automatically while `mvn test` still skips it.

## Key takeaways

* Unit tests for logic, `@SpringBootTest`+H2 for wiring, Testcontainers for real infrastructure (brokers, databases, anything you can't/shouldn't fake).
* `@DynamicPropertySource` wires a container's randomly-assigned port into Spring's `Environment` before context startup.
* Poll (`Awaitility`) for asynchronous outcomes; never `Thread.sleep()`.
* Keep expensive, Docker-dependent tests on the `*IT` + Failsafe track, separate from the fast `*Test` + Surefire track.

You've completed the **Advanced** module — and the full course. From here,
the natural next steps are the exercises scattered through each lesson:
centralized configuration (Spring Cloud Config), distributed tracing
(Micrometer Tracing), a real OAuth2/OIDC provider, and orchestration-style
Sagas.
