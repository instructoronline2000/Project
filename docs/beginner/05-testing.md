# Beginner · Lesson 05 — Unit & Integration Testing

## Goal

Learn the two testing styles used throughout this course: fast, isolated
service-layer unit tests with Mockito, and full-stack integration tests with
`@SpringBootTest`.

## Unit testing the service layer

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @InjectMocks private CustomerService customerService;

    @Test
    void create_savesNewCustomer_whenEmailNotTaken() {
        when(customerRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.create(request);

        assertThat(response.name()).isEqualTo("Alice Johnson");
        verify(customerRepository).save(any(Customer.class));
    }
}
```

See [`CustomerServiceTest.java`](../../order-management-system/customer-service/src/test/java/com/training/oms/customer/service/CustomerServiceTest.java).

* `@Mock` creates a fake `CustomerRepository` — no database, no Spring context, milliseconds to run.
* `@InjectMocks` constructs `CustomerService` with the mocks injected (this is exactly why we used **constructor injection** in Lesson 02 — Mockito can only do this cleanly with a constructor).
* `verify(...)` asserts a method *was called*, not just what it returned — useful for testing side effects like "did we actually try to save?"

This style is fast enough to run on every keystroke and should cover the
majority of your business-logic branches (see
[`ProductServiceTest`](../../order-management-system/product-service/src/test/java/com/training/oms/product/service/ProductServiceTest.java)
for the insufficient-stock branch).

## Full-stack integration testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "eureka.client.enabled=false" })
class CustomerControllerTest {

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void createThenFetchCustomer_roundTripsThroughRealHttpAndDatabase() {
        CustomerRequest request = new CustomerRequest("Carol Danvers", "carol@example.com", "555-0199", "1 Hero Way");
        ResponseEntity<CustomerRequest> response = restTemplate.postForEntity("/api/customers", request, CustomerRequest.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

See [`CustomerControllerTest.java`](../../order-management-system/customer-service/src/test/java/com/training/oms/customer/controller/CustomerControllerTest.java).

* `@SpringBootTest(webEnvironment = RANDOM_PORT)` boots the **entire** Spring context, on a real embedded Tomcat, on a random free port.
* `TestRestTemplate` makes a real HTTP call over the loopback interface — this exercises validation, the controller, the service, Hibernate, and the real (in-memory) H2 database together.
* `eureka.client.enabled=false` stops the test from trying (and failing) to register with a Eureka server that isn't running in the test environment.
* Note the class is named `*Test`, not `*IT` — Maven Surefire's default include pattern only picks up `*Test`/`Test*`/`*Tests` classes for the `test` phase. `*IT` is the convention for the **Failsafe** plugin bound to the `integration-test`/`verify` phases (a common intermediate/advanced-level Maven refinement — try adding `maven-failsafe-plugin` yourself and renaming this class back to `*IT`).

## Which style to reach for

| | Unit test (Mockito) | Integration test (`@SpringBootTest`) |
|---|---|---|
| Speed | Milliseconds | Seconds (full context) |
| Scope | One class | Whole module |
| Use for | Business logic branches, edge cases | "Does this all actually wire together and respond over HTTP?" |
| How many | Many | Few, per critical flow |

## Run the tests

```bash
cd order-management-system
mvn test
```

## Try it yourself

Add a unit test to `CustomerServiceTest` for the `delete()` method: verify it
throws `CustomerNotFoundException` when the id doesn't exist, and calls
`deleteById` when it does.

## Key takeaways

* Unit-test business logic with Mockito — fast, no Spring context.
* Integration-test critical end-to-end flows with `@SpringBootTest` + `TestRestTemplate`.
* Constructor injection (Lesson 02) is what makes `@InjectMocks` work cleanly.
* Maven Surefire only picks up `*Test` classes by default — `*IT` needs Failsafe.

You've completed the **Beginner** module. Continue to
[Intermediate Lesson 01 — Microservices Architecture](../intermediate/01-microservices-architecture.md).
