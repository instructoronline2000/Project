# Beginner · Lesson 03 — JPA Persistence

## Goal

Understand entities, repositories, and transactions using
[`product-service`](../../order-management-system/product-service), which also
introduces **optimistic locking** — important once multiple order requests
race to reserve the same stock.

## The entity

```java
@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal price;
    private int stockQuantity;

    @Version
    private Long version;   // optimistic locking column
    ...
}
```

See [`Product.java`](../../order-management-system/product-service/src/main/java/com/training/oms/product/domain/Product.java).

* `@Entity` + `@Table` map the class to a database table.
* `@Id @GeneratedValue(strategy = IDENTITY)` delegates primary key generation to the database (`AUTO_INCREMENT`/`IDENTITY`).
* `@Version` adds **optimistic locking**: every `UPDATE` includes `WHERE version = ?` and increments it. If two transactions read the same row and both try to update it, the second one gets an `OptimisticLockException` instead of silently overwriting the first (a "lost update"). This matters a lot once `order-service` calls `reserve-stock` concurrently for popular products.

## Repositories: Spring Data JPA

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

See [`ProductRepository.java`](../../order-management-system/product-service/src/main/java/com/training/oms/product/repository/ProductRepository.java).
That's it — `JpaRepository<Product, Long>` already gives you `save`,
`findById`, `findAll`, `deleteById`, pagination, and more, with **zero
implementation code**. Spring Data generates a proxy implementation at
startup.

Need a custom query? Just declare a method following the naming convention
and Spring Data derives the SQL — see
[`CustomerRepository.findByEmail`](../../order-management-system/customer-service/src/main/java/com/training/oms/customer/repository/CustomerRepository.java).

## Transactions

```java
@Service
@Transactional
public class ProductService {
    ...
    public ProductResponse reserveStock(Long id, int quantity) {
        Product product = findOrThrow(id);
        if (!product.hasSufficientStock(quantity)) {
            throw new InsufficientStockException(id, quantity, product.getStockQuantity());
        }
        product.reduceStock(quantity);
        return ProductResponse.from(product);
        // no explicit save() call needed - see "dirty checking" below
    }
}
```

See [`ProductService.java`](../../order-management-system/product-service/src/main/java/com/training/oms/product/service/ProductService.java).

* `@Transactional` on the class opens a transaction for every public method and commits on success / rolls back on any `RuntimeException`.
* **Dirty checking**: because `product` is a *managed* entity (loaded inside this transaction via `findOrThrow`), Hibernate automatically detects the field mutation (`reduceStock`) and issues an `UPDATE` at commit time — you never called `.save()`.
* Read-only methods (`getById`, `getAll`) are marked `@Transactional(readOnly = true)` — a hint that lets Hibernate skip dirty-checking overhead for a small performance win.

## H2 in-memory database

Every service in this course uses H2 (`spring.datasource.url: jdbc:h2:mem:...`)
so you can run the whole system with zero external setup. In a real project
you'd point this at PostgreSQL/MySQL — only the connection properties change;
none of your entity/repository/service code would.

`defer-datasource-initialization: true` (see
[`application.yml`](../../order-management-system/product-service/src/main/resources/application.yml))
tells Spring Boot to let Hibernate create the schema **before** running
`data.sql`, so the seed data has somewhere to land.

## Try it yourself

```bash
curl -X PATCH http://localhost:8082/api/products/1/reserve-stock \
  -H "Content-Type: application/json" -d '{"quantity": 5}'

curl http://localhost:8082/api/products/1     # stockQuantity decreased by 5
```

**Exercise:** Write a `@DataJpaTest` for `ProductRepository` that saves a
product, then asserts `findAll()` returns it (see Lesson 05 for the testing
pattern).

## Key takeaways

* `@Entity`/`@Id`/`@GeneratedValue` map classes to tables; `JpaRepository` gives you CRUD for free.
* `@Version` (optimistic locking) protects concurrent updates without pessimistic database locks.
* Managed entities are dirty-checked automatically inside a `@Transactional` method — no explicit `save()` needed for updates.

Next: [Lesson 04 — Validation & Exception Handling](04-validation-and-exception-handling.md)
