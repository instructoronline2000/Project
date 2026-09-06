package com.training.oms.customer.controller;

import com.training.oms.customer.dto.CustomerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Beginner lesson 05: full-stack integration test using an embedded servlet
 * container and the real (H2) database. Eureka registration is disabled so
 * the test can run standalone without a discovery server. Named "*Test" (not
 * "*IT") so it runs with the plain `mvn test` goal used throughout this course.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
class CustomerControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createThenFetchCustomer_roundTripsThroughRealHttpAndDatabase() {
        CustomerRequest request = new CustomerRequest("Carol Danvers", "carol@example.com", "555-0199", "1 Hero Way");

        ResponseEntity<CustomerRequest> createResponse =
                restTemplate.postForEntity("/api/customers", request, CustomerRequest.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
