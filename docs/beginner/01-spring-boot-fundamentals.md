# Beginner · Lesson 01 — Spring Boot Fundamentals

Prerequisites:

Knowledge of Java langauge programming basics (Available as seperate courses)

Installation of required software
1. JDK 21
2. Maven 3.3.9
3. VS Code
4. Docker for Desktop
5. PostgreSQL DB
6. Git
7. github account


## Goal

To Understand what is Java Spring Boot.

## What is Spring Boot?

Spring Boot is a set of conventions and auto-configuration on top of the Spring Framework. Instead of hand-wiring beans in XML, you:

1. Add a **starter** dependency (e.g. `spring-boot-starter-web`) which pulls in everything needed for that concern.
2. Annotate a class with `@SpringBootApplication`.
3. Run `main()`. Spring Boot starts an embedded Tomcat server, scans your classes for components, and wires them together.

## Your first service: `customer-service`

Open [`CustomerServiceApplication.java`](../../order-management-system/customer-service/src/main/java/com/training/oms/customer/CustomerServiceApplication.java):

```java
@SpringBootApplication
@EnableDiscoveryClient
public class CustomerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
```

* `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`. It tells Spring "scan this package (and sub-packages) for `@Component`/`@Service`/`@Repository`/`@RestController` classes, and auto-configure beans based on what's on the classpath."
* `@EnableDiscoveryClient` is a Spring Cloud annotation you'll fully understand in the Intermediate module — for now, ignore it (Eureka just won't be reachable, and the app logs a harmless warning).

## Anatomy of a Spring Boot module

Look at [`customer-service/pom.xml`](../../order-management-system/customer-service/pom.xml). The key starters:

| Starter | Gives you |
|---|---|
| `spring-boot-starter-web` | Embedded Tomcat, Spring MVC, Jackson (JSON) |
| `spring-boot-starter-data-jpa` | Hibernate, Spring Data repositories |
| `spring-boot-starter-validation` | Bean Validation (`@NotBlank`, `@Email`, ...) |
| `spring-boot-starter-actuator` | `/actuator/health`, `/actuator/metrics` out of the box |

## Run it

```bash
cd order-management-system/customer-service
mvn spring-boot:run
```

Then in another terminal:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/api/customers
```

You should see the two seeded customers from
[`data.sql`](../../order-management-system/customer-service/src/main/resources/data.sql).

## Configuration: `application.yml`

Open [`application.yml`](../../order-management-system/customer-service/src/main/resources/application.yml).
Spring Boot binds YAML/properties keys straight onto auto-configured beans:

* `server.port` → the embedded Tomcat's port.
* `spring.datasource.*` → the `DataSource` bean.
* `spring.jpa.hibernate.ddl-auto: update` → Hibernate creates/updates tables from your `@Entity` classes automatically (fine for learning; in real projects you'd use Flyway/Liquibase migrations instead — see the "Try it yourself" below).

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

This one line turns on **Java 21 virtual threads** for the embedded Tomcat's
request-handling threads. Every incoming HTTP request now runs on a cheap
virtual thread instead of a pooled platform thread — huge scalability win for
I/O-bound services (like ours, which spend most of their time waiting on the
database or other services) with zero code changes.

## Try it yourself

1. Change `server.port` to `9081`, restart, confirm the service now listens there.
2. Add a new field (e.g. `loyaltyTier`) to `application.yml` under a custom `app.*` prefix, then read it in code with `@Value("${app.loyalty-tier:STANDARD}")` injected into `CustomerController`.
3. Replace `ddl-auto: update` with `validate` and observe the startup failure — this is why real projects use migration tools instead of `update`.

## Key takeaways

* `@SpringBootApplication` = auto-configuration + component scanning.
* Starters decide what auto-configuration kicks in — add/remove a starter, and behavior changes without code.
* `application.yml` is how you configure auto-configured beans without writing `@Configuration` classes.
* `spring.threads.virtual.enabled=true` is a one-line, Java 21-native scalability upgrade.

Next: [Lesson 02 — REST APIs & Layered Architecture](02-rest-api-and-layered-architecture.md)
