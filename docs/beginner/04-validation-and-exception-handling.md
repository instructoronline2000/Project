# Beginner · Lesson 04 — Validation & Exception Handling

## Goal

Learn Bean Validation and centralized error handling so your controllers stay
free of `try`/`catch` clutter.

## Bean Validation

```java
public record CustomerRequest(
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
        String phone,
        String address
) {}
```

See [`CustomerRequest.java`](../../order-management-system/customer-service/src/main/java/com/training/oms/customer/dto/CustomerRequest.java).
Annotate the DTO's fields with constraints from `jakarta.validation.constraints`,
then add `@Valid` to the controller parameter:

```java
@PostMapping
public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) { ... }
```

If validation fails, Spring throws `MethodArgumentNotValidException`
**before your controller method body ever runs**. You never write an `if
(name == null)` check.

## Custom business exceptions

Business rules that aren't expressible as annotations become plain
exceptions:

```java
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long id) {
        super("Customer not found with id: " + id);
    }
}
```

See [`CustomerNotFoundException`](../../order-management-system/customer-service/src/main/java/com/training/oms/customer/exception/CustomerNotFoundException.java)
and [`DuplicateEmailException`](../../order-management-system/customer-service/src/main/java/com/training/oms/customer/exception/DuplicateEmailException.java).
The service layer just throws them:

```java
customerRepository.findByEmail(request.email()).ifPresent(existing -> {
    throw new DuplicateEmailException(request.email());
});
```

## Centralizing the mapping: `@RestControllerAdvice`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(CustomerNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }
    ...
}
```

See [`GlobalExceptionHandler.java`](../../order-management-system/customer-service/src/main/java/com/training/oms/customer/exception/GlobalExceptionHandler.java).

One class, applied automatically to **every** controller in the module, maps
exceptions to a consistent JSON error shape (`ApiError`): timestamp, status,
error, message, and (for validation) a list of field-level details. No
`try`/`catch` in any controller method anywhere in this codebase.

## Try it yourself

```bash
# Missing required fields -> 400 with field-level detail
curl -X POST http://localhost:8081/api/customers \
  -H "Content-Type: application/json" -d '{"name":""}'

# Duplicate email -> 409
curl -X POST http://localhost:8081/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Dup","email":"alice@example.com"}'

# Unknown id -> 404
curl http://localhost:8081/api/customers/999
```

**Exercise:** `product-service`'s
[`GlobalExceptionHandler`](../../order-management-system/product-service/src/main/java/com/training/oms/product/exception/GlobalExceptionHandler.java)
uses a plain `Map<String,Object>` instead of a shared `ApiError` record.
Extract `ApiError` into `common-events` (or a new `common-web` module) so both
services share one error shape — this is the same "shared kernel" trade-off
you'll revisit with Kafka events in the Advanced module.

## Key takeaways

* Bean Validation (`@Valid` + constraint annotations) rejects bad input before your code runs.
* Model business-rule violations as exceptions, thrown from the service layer.
* `@RestControllerAdvice` + `@ExceptionHandler` centralizes exception-to-HTTP-status mapping across every controller.

Next: [Lesson 05 — Unit & Integration Testing](05-testing.md)
