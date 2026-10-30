# Chapter 02 — It Works, But It's Slow

*With a fixed-size platform-thread pool, every thread blocked waiting on a slow database call is a thread that can't pick up a new request, even though the CPU is nearly idle.*

Companion code for **Chapter 02** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

This module reproduces the same `GET /api/products/{id}` endpoint from Chapter 01, but wires every request to block for roughly 80ms inside `ProductRepository`, standing in for a real JDBC round trip to a moderately loaded database. `application.yml` documents both stops on Sarah's investigation: a commented-out "naive fix" that simply raises Tomcat's platform-thread pool ceiling (`server.tomcat.threads.max: 800`), and the fix that is actually enabled, `spring.threads.virtual.enabled: true`. `ThreadInfoController` gives the tests (and you) a way to prove, from an HTTP response, whether a request was served on an expensive platform thread or a cheap virtual thread. No business logic — `ProductService`, `ProductController`, `Product` — changes between the two fixes; only how the platform runs blocking code changes, which is the entire point of the chapter's mental model.

## Prerequisites

- **Java 21** or later — this is a hard requirement, not just a recommendation: virtual threads (`Thread.isVirtual()`, `Executors.newVirtualThreadPerTaskExecutor()`, and Spring Boot's `spring.threads.virtual.enabled` switch) are a Java 21 feature and the module will not build or behave correctly on an older JDK.
- **Maven** (the module uses the standard `spring-boot-starter-parent` 3.3.4 lifecycle; no separate Maven install is required beyond the wrapper/toolchain already on your machine).
- No database, container, or external service — `ProductRepository` is in-memory and only simulates I/O latency with `Thread.sleep`.

## Run it

```bash
# From code/chapter-02
mvn spring-boot:run
```

The application starts on port `8080`. Example requests once it is running:

```bash
# A known product
curl http://localhost:8080/api/products/1

# An unknown product -> 404
curl -i http://localhost:8080/api/products/999

# Diagnostics: which kind of thread served this request?
curl http://localhost:8080/api/diagnostics/thread-info
```

With `spring.threads.virtual.enabled: true` (the current setting), the diagnostics endpoint should report `"virtual":true`.

## Project structure

```
src/main/java/com/acme/retail/ch02/
├── Ch02Application.java                 # Spring Boot entry point; the fix lives in application.yml, not here
├── diagnostics/
│   └── ThreadInfoController.java        # Reports whether the current request is on a virtual or platform thread
└── product/
    ├── Product.java                     # Immutable record for a catalog item, shared shape across the book
    ├── ProductRepository.java           # In-memory "database" whose findById() blocks for ~80ms per call
    ├── ProductService.java              # Thin application-logic layer, unchanged by the virtual-threads fix
    ├── ProductController.java           # GET /api/products/{id}, translates ProductNotFoundException to 404
    └── ProductNotFoundException.java    # Signals a missing product id
```

## Examples in this module

### com.acme.retail.ch02.Ch02Application

Standard `@SpringBootApplication` bootstrap class. Its Javadoc is the clarifying point of the whole module: the behavioral fix for this chapter is a one-line configuration change in `application.yml`, not a code change in this class or anywhere else.

```java
/**
 * <p>The fix that matters in this chapter lives in {@code application.yml}
 * ({@code spring.threads.virtual.enabled: true}), not in this class.
 */
@SpringBootApplication
public class Ch02Application {

    public static void main(String[] args) {
        SpringApplication.run(Ch02Application.class, args);
    }
}
```

### com.acme.retail.ch02.product.Product

An immutable Java record describing a catalog item (id, name, description, price in cents, SKU, stock quantity, category). It carries no behavior — it exists purely so field names stay consistent with the shared Acme Retail domain model used across every chapter's independent code module.

### com.acme.retail.ch02.product.ProductRepository

This is the class that actually causes the chapter's problem. It backs a tiny `ConcurrentHashMap` of three seed products, but every lookup routes through `simulateBlockingDatabaseCall()`, which calls `Thread.sleep(80)`. That sleep is doing exactly what a real blocking JDBC driver does: parking the calling thread — consuming no CPU, but making that thread unavailable for any other work — while it waits on a slow external resource.

```java
static final long SIMULATED_DATABASE_LATENCY_MS = 80;

public Product findById(Long id) {
    simulateBlockingDatabaseCall();
    Product product = products.get(id);
    if (product == null) {
        throw new ProductNotFoundException(id);
    }
    return product;
}

/**
 * {@link Thread#sleep(long)} blocks the current thread — it does not
 * consume CPU, but the thread cannot do anything else until the sleep
 * finishes. A real blocking JDBC call behaves the same way.
 */
private void simulateBlockingDatabaseCall() {
    try {
        Thread.sleep(SIMULATED_DATABASE_LATENCY_MS);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting on the database", e);
    }
}
```

### com.acme.retail.ch02.product.ProductService

A thin pass-through to `ProductRepository.findById(Long)`. Its Javadoc is the second half of the chapter's core lesson: this class is byte-for-byte identical whether the app is running with the default fixed-size platform-thread pool, the "raise the pool size" naive fix, or the virtual-threads fix — the request-handling model changed underneath it, not the business logic.

### com.acme.retail.ch02.product.ProductController

Exposes `GET /api/products/{id}`, delegating to `ProductService`, and maps `ProductNotFoundException` to an HTTP 404 with an empty body via `@ExceptionHandler`/`@ResponseStatus`. Functionally identical to the Chapter 01 controller — what differs in this chapter is the traffic it has to survive, not its code.

### com.acme.retail.ch02.product.ProductNotFoundException

An unchecked `RuntimeException` carrying the missing product id in its message, thrown by `ProductRepository.findById` and translated to a 404 by `ProductController`.

### com.acme.retail.ch02.diagnostics.ThreadInfoController

Exposes `GET /api/diagnostics/thread-info`, a stand-in for a real thread dump / profiler (`jstack`, JFR, an APM tool). It reports the current thread's name, whether `Thread.isVirtual()` is true, and its thread id — the minimum information needed to prove, from a single HTTP response, whether Tomcat handed the request to a virtual thread or to a platform thread from its fixed-size pool.

```java
@GetMapping("/api/diagnostics/thread-info")
public ThreadInfo threadInfo() {
    Thread current = Thread.currentThread();
    return new ThreadInfo(current.getName(), current.isVirtual(), current.threadId());
}

public record ThreadInfo(String threadName, boolean virtual, long threadId) {
}
```

## API Endpoints

| Method | Path | Request | Response |
|---|---|---|---|
| GET | `/api/products/{id}` | Path variable `id` (Long) | `200 OK` with a `Product` JSON body, or `404 Not Found` (empty body) if no product with that id exists |
| GET | `/api/diagnostics/thread-info` | None | `200 OK` with a `ThreadInfo` JSON body: `threadName`, `virtual`, `threadId` |

```bash
curl http://localhost:8080/api/products/1
# {"id":1,"name":"Trail Running Shoe","description":"Grippy trail shoe, black/orange","priceCents":8999,"sku":"SHOE-TRAIL-01","stockQuantity":42,"category":"Footwear"}

curl http://localhost:8080/api/diagnostics/thread-info
# {"threadName":"...","virtual":true,"threadId":...}
```

## Tests

- `ProductControllerTest` — functional correctness only: `GET /api/products/1` returns `200` with the expected SKU, and `GET /api/products/999` returns `404`. These pass regardless of whether virtual threads are enabled, because correctness is not what this chapter is about.
- `ConcurrentRequestLoadTest` — the chapter's central proof, turned into an automated regression test. It fires `150` concurrent requests at `/api/products/1` from a client-side virtual-thread-per-task executor, where each request blocks for ~80ms server-side in `ProductRepository`. It asserts that the whole batch completes in **under half** of the fully-sequential worst case (`150 × 80ms`), which is only possible if the blocking calls are running concurrently rather than being queued one behind another by an exhausted thread pool. A second test hits `/api/diagnostics/thread-info` and asserts `virtual()` is `true`, directly confirming that Tomcat is dispatching requests to virtual threads (per `spring.threads.virtual.enabled: true` in `application.yml`) rather than to platform threads from its fixed-size pool — which is *why* the first test's throughput is possible at all.

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

```yaml
server:
  port: 8080
```
Standard HTTP port for the module; no special tuning needed once virtual threads are enabled.

```yaml
# NAIVE FIX (commented out, kept for reference)
# tomcat:
#   threads:
#     max: 800   # up from the Spring Boot default of 200
```
Sarah's first instinct — raise the platform-thread pool ceiling. Left in as a comment because it is the wrong fix: each extra thread costs about 1 MB of stack and more context-switching, and requests still queue once the (larger) pool is full, because the underlying problem is blocked I/O, not insufficient CPU-bound capacity.

```yaml
spring:
  threads:
    virtual:
      enabled: true
```
The actual fix for this chapter. Tells Spring Boot to dispatch each request to a cheap virtual thread instead of a platform thread, so a blocking call parks the virtual thread almost for free and frees the underlying OS carrier thread to serve other requests — without changing any business logic.

```yaml
logging:
  level:
    root: INFO
    com.acme.retail.ch02: INFO
```
Baseline logging; nothing chapter-specific.
