# Chapter 03 — When the Database Says No

*The pool sits between your code and the database: a small fixed set of reusable connections, and when every one of them is checked out, the next request simply waits — and can time out.*

Companion code for **Chapter 03** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

Sarah's product catalog endpoint looks innocent — one repository call, one loop — but under load it exhausts the HikariCP connection pool because it issues one SQL query per product in addition to the initial `findAll()`. This is the N+1 query pattern: `Product.category` is a lazy `@ManyToOne`, so touching `getCategory().getName()` inside a loop forces one extra round trip to Postgres per row. `ProductCatalogService` keeps both the broken version (`naiveCatalog()`) and the fixed version (`catalog()`, using `JOIN FETCH`/`@EntityGraph`) side by side so the difference is visible in code, not just in prose. A second, unrelated pressure on the same pool — the same few hot products being read thousands of times a minute — is addressed separately with a Redis cache-aside layer (`ProductCacheService`) that answers single-product reads without touching the database at all on a cache hit.

## Prerequisites

- Java 21
- Maven (or the wrapper, if one is added to the repo root)
- PostgreSQL reachable at `jdbc:postgresql://localhost:5432/acme_retail` with user/password `acme`/`acme` (see `src/main/resources/application.yml`) — required to run the application; not required to run the tests, which use an in-memory H2 database instead
- Redis reachable at `localhost:6379` — required to run the application (`ProductCacheService` and `RedisConfig` depend on a live `RedisConnectionFactory`); not required to run the tests, which mock `RedisTemplate`
- HikariCP pool configured with `maximum-pool-size: 10` and a `connection-timeout` of 3000 ms — this is the pool the chapter's story is about; both values are read directly from `application.yml`
- Docker (only if you use the included `docker-compose.yml` to start Postgres and Redis instead of running them yourself)

## Run it

A `docker-compose.yml` is included in this module — it starts PostgreSQL 16 (pre-configured with the `acme_retail`/`acme`/`acme` database/user/password this module's `application.yml` already expects) and Redis 7 together:

```bash
cd volume1_2/code/chapter-03
docker compose up -d
mvn spring-boot:run
```

The app listens on port 8080.

```bash
# Catalog page — the fixed, single-query version
curl http://localhost:8080/api/products

# A single product — cache-aside through Redis
curl http://localhost:8080/api/products/1
```

Tests do not need either service running — `ProductCatalogServiceNPlusOneTest` uses `@DataJpaTest` against an embedded H2 database, and `ProductCacheServiceTest` mocks `RedisTemplate` directly:

```bash
mvn test
```

## Project structure

```
src/main/java/com/acme/retail/ch03/
├── Chapter03Application.java          # Spring Boot entry point
├── category/
│   ├── Category.java                  # Simple JPA entity: id + unique name
│   └── CategoryRepository.java        # Plain JpaRepository, no custom queries
├── config/
│   └── RedisConfig.java               # RedisTemplate<String, ProductView> bean, JSON-serialized
└── product/
    ├── Product.java                   # JPA entity with a LAZY @ManyToOne to Category
    ├── ProductRepository.java         # findAll() (N+1-prone) vs JOIN FETCH / @EntityGraph fixes
    ├── ProductCatalogService.java     # naiveCatalog() (the bug) vs catalog() (the fix)
    ├── ProductCacheService.java       # Cache-aside read-through layer backed by Redis
    ├── ProductView.java               # DTO returned by the API and stored in the cache
    └── ProductController.java         # REST endpoints: GET /api/products, GET /api/products/{id}
```

## Examples in this module

### com.acme.retail.ch03.Chapter03Application

Standard `@SpringBootApplication` entry point. Its Javadoc frames the whole chapter: the Java service itself is no longer the bottleneck (that was Chapter 02); under real load, the database becomes the bottleneck through a maxed-out HikariCP pool and an N+1 query pattern.

```java
@SpringBootApplication
public class Chapter03Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter03Application.class, args);
    }
}
```

### com.acme.retail.ch03.category.Category

A deliberately small JPA entity — `id` and a unique `name`. It exists purely so `Product` has something to lazily load; the interesting behavior lives in how `Product` and `ProductRepository` fetch it, not in this class.

```java
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
    ...
}
```

### com.acme.retail.ch03.category.CategoryRepository

A bare `JpaRepository<Category, Long>` with no custom methods. It is only referenced from test setup code that seeds categories directly through the `EntityManager`.

```java
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
```

### com.acme.retail.ch03.config.RedisConfig

Defines the `RedisTemplate<String, ProductView>` bean used by `ProductCacheService`. Keys are plain strings (`product:42`) via `StringRedisSerializer`; values are serialized as JSON via `GenericJackson2JsonRedisSerializer` specifically so a developer can inspect what is cached with `redis-cli GET product:42` instead of staring at opaque Java-serialized bytes.

```java
@Bean
public RedisTemplate<String, ProductView> productRedisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, ProductView> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
}
```

### com.acme.retail.ch03.product.Product

The catalog entity, carried forward from earlier chapters, now with a real `category` relationship declared `@ManyToOne(fetch = FetchType.LAZY)`. The class Javadoc is explicit that `LAZY` is a reasonable default and not the bug — the bug is code that reads `getCategory()` inside a loop without telling Hibernate to fetch it up front.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = false)
private Category category;
```

### com.acme.retail.ch03.product.ProductRepository

Holds the fix in two equivalent forms. `findAllWithCategory()` and `findByIdWithCategory(id)` use hand-written JPQL with `JOIN FETCH` so Hibernate loads each product and its category in a single SQL statement. `findByCategoryId(Long)` achieves the same result with `@EntityGraph(attributePaths = "category")` instead — useful when the query is already a Spring Data "derived query" from the method name and you still want to force an eager fetch. The inherited `findAll()` is called out in a comment as *not* the bug by itself: it runs exactly one query, and only becomes a problem once calling code lazily touches `category` on every row.

```java
@Query("select p from Product p join fetch p.category")
List<Product> findAllWithCategory();

@Query("select p from Product p join fetch p.category where p.id = :id")
Optional<Product> findByIdWithCategory(@Param("id") Long id);

@EntityGraph(attributePaths = "category")
List<Product> findByCategoryId(Long categoryId);
```

### com.acme.retail.ch03.product.ProductCatalogService

The heart of the chapter. `naiveCatalog()` is Sarah's original code: it calls `findAll()` (one query), then calls `product.getCategory().getName()` inside the loop, which triggers one additional lazy-load query per product — 201 round trips for a 200-product catalog instead of 1. `catalog()` is the fix actually wired into the controller: it calls `findAllWithCategory()`, so every category is already loaded and reading `getCategory().getName()` costs nothing extra.

```java
public List<ProductView> naiveCatalog() {
    List<Product> products = productRepository.findAll();
    List<ProductView> views = new ArrayList<>();
    for (Product product : products) {
        views.add(new ProductView(
                product.getId(), product.getName(), product.getSku(),
                product.getPriceCents(), product.getStockQuantity(),
                product.getCategory().getName() // <-- one extra query, per product
        ));
    }
    return views;
}
```

### com.acme.retail.ch03.product.ProductCacheService

A cache-aside layer for single-product reads, addressing a different pressure on the same connection pool: a small set of hot products (best sellers, flash-sale items) being requested far more often than the catalog changes. `getProductById(id)` checks Redis first (`product:{id}`); on a hit it returns without touching Postgres at all; on a miss it loads via `ProductRepository.findByIdWithCategory(id)`, converts to a `ProductView`, and writes it into Redis with a 10-minute TTL (`CACHE_TTL`). `evict(id)` deletes the cache entry so a later write to the database does not leave Redis serving a stale value until the TTL expires.

```java
public Optional<ProductView> getProductById(Long id) {
    String key = KEY_PREFIX + id;
    ProductView cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        return Optional.of(cached);
    }
    Optional<Product> product = productRepository.findByIdWithCategory(id);
    if (product.isEmpty()) {
        return Optional.empty();
    }
    ProductView view = ProductView.from(product.get());
    redisTemplate.opsForValue().set(key, view, CACHE_TTL);
    return Optional.of(view);
}
```

### com.acme.retail.ch03.product.ProductView

The DTO returned by both the catalog and single-product endpoints, and the exact type stored in Redis. It exists for two reasons: the `Product` entity carries a lazy Hibernate proxy for `category` that should never leave this module, and the cache needs a plain, JSON-friendly type it can serialize without touching Hibernate.

```java
public record ProductView(
        Long id, String name, String sku, long priceCents,
        int stockQuantity, String categoryName) {

    public static ProductView from(Product product) {
        return new ProductView(
                product.getId(), product.getName(), product.getSku(),
                product.getPriceCents(), product.getStockQuantity(),
                product.getCategory().getName());
    }
}
```

### com.acme.retail.ch03.product.ProductController

Two endpoints, deliberately calling two different services so the two fixes in this chapter stay visually distinct: `list()` calls `ProductCatalogService.catalog()` (the `JOIN FETCH` fix), and `getById(id)` calls `ProductCacheService.getProductById(id)` (the Redis cache-aside fix).

```java
@GetMapping
public List<ProductView> list() {
    return catalogService.catalog();
}

@GetMapping("/{id}")
public ResponseEntity<ProductView> getById(@PathVariable Long id) {
    Optional<ProductView> product = cacheService.getProductById(id);
    return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
}
```

## API Endpoints

| Method | Path                  | Request       | Response                                  |
|--------|------------------------|---------------|--------------------------------------------|
| GET    | `/api/products`        | none          | `200 OK` — JSON array of `ProductView`     |
| GET    | `/api/products/{id}`   | path variable `id` (Long) | `200 OK` with `ProductView`, or `404 Not Found` if no such product |

```bash
curl http://localhost:8080/api/products

curl http://localhost:8080/api/products/1
```

## Tests

- `ProductCacheServiceTest` — pure unit tests with `RedisTemplate` and `ValueOperations` mocked (no Spring context, no real Redis, no database). Verifies: a cache hit returns the cached value and never calls `ProductRepository` (`verifyNoInteractions`); a cache miss loads from the repository, maps to a `ProductView`, and writes it back into Redis with `ProductCacheService.CACHE_TTL`; a missing product returns `Optional.empty()` and caches nothing; `evict(id)` deletes the `product:{id}` key.
- `ProductCatalogServiceNPlusOneTest` — proves the N+1 pattern with real query counts rather than a claim in prose. It boots the JPA slice only (`@DataJpaTest` against an in-memory H2 database, with `hibernate.generate_statistics=true`), seeds 5 products, each in its own category, then flushes and clears the persistence context so the next reads must build genuinely lazy proxies instead of reusing already-loaded entities. It then reads `SessionFactory.getStatistics().getPrepareStatementCount()` before and after each catalog method: `naiveCatalogRunsOneQueryPerProductInAdditionToTheInitialFind()` asserts the count is exactly `PRODUCT_COUNT + 1` (one `findAll()` plus one lazy-load per product), while `fixedCatalogRunsExactlyOneQueryRegardlessOfProductCount()` asserts the count is exactly `1` for the `JOIN FETCH` version. This is a direct, numeric proof of the N+1 bug and its fix, not an inference from behavior.

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

- `spring.datasource.hikari.maximum-pool-size: 10` — the ceiling on connections HikariCP will ever hand out at once; this is the exact number Daniel points at on the dashboard when the pool exhausts under the N+1 load in the chapter's story.
- `spring.datasource.hikari.minimum-idle: 2` — the floor of connections kept open even when idle, so the pool does not have to reconnect from zero the moment traffic resumes.
- `spring.datasource.hikari.connection-timeout: 3000` — how long (ms) a request will wait for a free connection before failing; this is the timeout that actually surfaces as errors when the pool is exhausted.
- `spring.datasource.hikari.idle-timeout: 300000` / `max-lifetime: 1800000` — how long idle connections are kept, and the hard cap on a connection's total lifetime, both standard HikariCP hygiene settings.
- `spring.jpa.open-in-view: false` — disables the Open Session in View anti-pattern so lazy-loading failures surface where they actually happen (the service layer) instead of being silently papered over in the view layer.
- `spring.data.redis.host` / `port` (`localhost:6379`) — the Redis instance `ProductCacheService` reads and writes through; the application will fail to start cleanly if nothing is listening here.
- `logging.level.org.hibernate.SQL: info` (with a commented-out `debug` / `trace` pair) — Sarah's second diagnostic move in the chapter: flipping this to `debug` reveals every SQL statement Hibernate sends, which is how the repeating `select category where id = ?` query becomes visible.
- `management.endpoints.web.exposure.include: health,metrics` — exposes `/actuator/metrics/hikaricp.connections.active` and `hikaricp.connections.pending`, the numbers read on the connection-pool dashboard during the investigation.
