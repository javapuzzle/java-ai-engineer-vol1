# Chapter 01 — Sarah's First Day at Acme Retail

*A real API is not just a method: it sits at the top of a chain — Client → Controller → Service → Repository → Database — and every layer trusts only the layer directly below it.*

Companion code for **Chapter 01** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

This module is the product catalog REST API Sarah builds on her first day at Acme Retail: a `ProductController` that only turns HTTP requests into calls on a `ProductService`, a `ProductService` that holds business logic and talks only to a `ProductRepository`, and a `ProductRepository` that is the single place in the codebase that "knows" about the database. Errors are translated at the edge by a `@RestControllerAdvice`, so the service layer never has to think about HTTP status codes. There is no shortcut from the controller to the database anywhere in this code — that separation is the entire point of the mental model this chapter teaches. The module also shows the two different ways a layered architecture gets tested: a fast, Spring-free unit test for the service layer, and a web-layer test that boots only the MVC machinery.

## Prerequisites

- Java 21 (matches `<java.version>` in `pom.xml`)
- Maven (no wrapper script is checked into this module or elsewhere in the repository; use a locally installed `mvn`)
- A running PostgreSQL instance reachable at `jdbc:postgresql://localhost:5432/acme_retail`, with a database/role that matches `spring.datasource.username`/`password` in `application.yml` (`acme` / `acme`) — required only to run the app itself; the test suite does not need it (`ProductServiceTest` mocks the repository and `ProductControllerTest` uses `@WebMvcTest`, neither starts a real database)
- Port `8080` free locally (set by `server.port`)
- Docker (only if you use the included `docker-compose.yml` to start that Postgres instance instead of running one yourself)

## Run it

A `docker-compose.yml` is included in this module — it starts a PostgreSQL 16 container pre-configured with the exact database/user/password (`acme_retail` / `acme` / `acme`) this module's `application.yml` already expects:

```bash
cd volume1_2/code/chapter-01
docker compose up -d
mvn spring-boot:run
```

The application starts on **port 8080**. Once it is up, list all products with:

```bash
curl http://localhost:8080/api/products
```

## Project structure

```
src/main/java/com/acme/retail/ch01/
├── Chapter01Application.java          # Spring Boot entry point; boots the layered app
└── product/
    ├── Product.java                   # JPA entity mapped to the "products" table
    ├── ProductController.java         # HTTP layer: routes requests to ProductService
    ├── ProductService.java            # Business logic layer; the only caller of ProductRepository
    ├── ProductRepository.java         # Spring Data JPA repository; the only layer that knows about the database
    ├── ProductNotFoundException.java  # Unchecked exception thrown when a product ID has no matching row
    └── ProductExceptionHandler.java   # @RestControllerAdvice; maps exceptions to HTTP responses
```

## Examples in this module

### com.acme.retail.ch01.Chapter01Application

The `@SpringBootApplication` entry point that boots the layered app (`ProductController` -> `ProductService` -> `ProductRepository` -> PostgreSQL); nothing to configure beyond `main`.

### com.acme.retail.ch01.product.Product

The JPA entity mapped to the `products` table via `@Entity`/`@Table`. Sarah writes no SQL for basic CRUD against this class because Spring Data JPA generates it from `ProductRepository`. Money is deliberately stored as an integer number of cents rather than a floating-point dollar amount, to avoid rounding bugs:

```java
private Integer priceCents;
```

as noted in the class Javadoc: "Floating point numbers cannot represent every decimal fraction exactly, so storing currency as cents avoids rounding bugs." The no-arg constructor is `protected` because JPA/Hibernate needs it to build entities via reflection; application code uses the six-argument constructor instead.

### com.acme.retail.ch01.product.ProductController

Sarah's first Spring Boot class, and the top layer in the chain. It is deliberately thin — it never talks to a database directly, only to `ProductService`:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<Product> listProducts() {
        return productService.listProducts();
    }
```

Spring turns the returned `Product` objects into JSON automatically; the controller never constructs a response body by hand.

### com.acme.retail.ch01.product.ProductService

The business-logic layer, and the only class in the module allowed to call `ProductRepository`. Today the logic is simple (fetch one, list all, create one), but the class exists precisely so that future rules — the Javadoc gives the examples "do not allow negative stock" or, in later chapters, "check the cache before hitting the database" — have one obvious home, without the controller or the repository ever needing to change. `@Service` marks it as a Spring-managed bean, injected into `ProductController` via constructor injection.

### com.acme.retail.ch01.product.ProductRepository

The repository layer — the only place in the module that "knows" about the database. It extends `JpaRepository<Product, Long>`, so `findAll()`, `findById(id)`, `save(entity)`, and `deleteById(id)` come for free; there is no `ProductRepositoryImpl` class anywhere in the project. `findBySku` demonstrates Spring Data JPA query derivation — the method name alone is enough for Spring to generate `SELECT * FROM products WHERE sku = ?`, with no annotation and no SQL string:

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);
}
```

### com.acme.retail.ch01.product.ProductNotFoundException

A plain unchecked `RuntimeException`, thrown by `ProductService.getProduct` when a requested ID has no matching row. It carries no HTTP knowledge itself — the message is just `"Product not found: " + id`.

### com.acme.retail.ch01.product.ProductExceptionHandler

A `@RestControllerAdvice` that centralizes exception-to-HTTP-response translation for every `@RestController` in the module. Catching `ProductNotFoundException` here and turning it into a 404 keeps `ProductService` focused purely on business logic:

```java
@ExceptionHandler(ProductNotFoundException.class)
public ResponseEntity<Map<String, String>> handleNotFound(ProductNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
}
```

## API Endpoints

| Method | Path                | Request                                              | Response                          |
|--------|----------------------|-------------------------------------------------------|------------------------------------|
| GET    | `/api/products`      | —                                                       | `200 OK`, JSON array of products   |
| GET    | `/api/products/{id}` | —                                                       | `200 OK` with product, or `404 Not Found` |
| POST   | `/api/products`      | JSON body (validated `Product`)                        | `201 Created`, JSON of created product |

```bash
# List all products
curl http://localhost:8080/api/products

# Get one product by id
curl http://localhost:8080/api/products/1

# Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Insulated Water Bottle",
        "description": "Keeps drinks cold for 24 hours",
        "priceCents": 2499,
        "sku": "SKU-2002",
        "stockQuantity": 100,
        "category": "Outdoor"
      }'
```

A `GET /api/products/{id}` for an ID that does not exist returns `404 Not Found` with a body like `{"error": "Product not found: 999"}`, produced by `ProductExceptionHandler`.

## Tests

- `ProductServiceTest.getProduct_returnsProduct_whenItExists` — verifies `ProductService.getProduct` returns the product when the mocked `ProductRepository.findById` finds a row.
- `ProductServiceTest.getProduct_throwsProductNotFoundException_whenIdDoesNotExist` — verifies a missing ID causes `ProductService` to throw `ProductNotFoundException` containing the ID in its message.
- `ProductServiceTest.createProduct_delegatesToRepositorySave` — verifies `createProduct` calls `ProductRepository.save` and returns what the repository returns.
- `ProductServiceTest.listProducts_returnsEveryProductFromRepository` — verifies `listProducts` returns every product the repository provides.
- `ProductControllerTest.getProduct_returns200WithProductJson_whenProductExists` — verifies `GET /api/products/1` returns `200 OK` with the expected `name`, `sku`, and `priceCents` fields in the JSON body.
- `ProductControllerTest.getProduct_returns404_whenProductDoesNotExist` — verifies `GET /api/products/999` returns `404 Not Found` with an `error` field, when the mocked service throws `ProductNotFoundException`.
- `ProductControllerTest.listProducts_returns200WithJsonArray` — verifies `GET /api/products` returns `200 OK` with a JSON array whose first element's `sku` matches.
- `ProductControllerTest.createProduct_returns201_whenRequestBodyIsValid` — verifies `POST /api/products` with a valid JSON body returns `201 Created` with the created product's `sku` in the response.

`ProductServiceTest` uses Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`) and never starts Spring or touches a database. `ProductControllerTest` uses `@WebMvcTest(ProductController.class)`, which starts only the web layer (controllers, JSON serialization, exception handlers) and replaces `ProductService` with a `@MockBean`.

Run the suite with:

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

- `spring.datasource.url: jdbc:postgresql://localhost:5432/acme_retail` (with `username: acme` / `password: acme`) — this is what makes `ProductRepository` real: the repository layer's whole reason to exist is that it is the only code in the module that knows this URL and these credentials.
- `spring.jpa.hibernate.ddl-auto: update` — lets Hibernate create/update the `products` table from the `Product` entity automatically, so the chapter's example needs no separate schema migration script.
- `spring.jpa.open-in-view: false` — disables the Open Session In View pattern, so database access cannot leak past the repository layer into the controller by accident (a good default given this chapter's message about layers not skipping past each other).
- `server.port: 8080` — the port used in the "Run it" and "API Endpoints" sections above.

The file's own header comment is explicit that this is local-only configuration: "In production, connection details come from environment variables / a secrets manager, not from a file committed to source control."
