package com.training.oms.payment;

import com.training.oms.events.OrderCreatedEvent;
import com.training.oms.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Advanced lesson 06: a real Kafka broker in a disposable Docker container,
 * driving the *actual* Spring Kafka listener end-to-end - no mocks. Named
 * "*IT" on purpose (see Beginner lesson 05): it needs Docker and is meant to
 * run via `mvn verify` with the Failsafe plugin, not on every `mvn test`.
 */
@Testcontainers
@SpringBootTest(properties = {
        "eureka.client.enabled=false"
})
class PaymentServiceKafkaIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void orderCreatedEvent_isConsumedAndCreatesAnApprovedPayment() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                100L, 1L,
                List.of(new OrderCreatedEvent.OrderLine(1L, 2, new BigDecimal("10.00"))),
                new BigDecimal("20.00"), Instant.now());

        kafkaTemplate.send("order-events", "100", event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(paymentRepository.findAll())
                        .anyMatch(p -> p.getOrderId().equals(100L)));
    }
}
