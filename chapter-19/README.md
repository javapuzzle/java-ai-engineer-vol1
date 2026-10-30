# Chapter 19 — Can We Trust Our Tests?

*A deterministic test with a mocked AI answers "did my code produce exactly the output I expect?"; an AI evaluation against a real model answers a different question entirely — "is the model's real answer actually correct and on-topic?" — and a green suite of the first kind proves nothing about the second.*

Companion code for **Chapter 19** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

Sarah's 42 deterministic tests are all green, yet a customer was told an out-of-stock shoe was available. This module shows why those two facts do not contradict each other, by building the same shopping-assistant feature behind one seam — `AssistantAiClient` — and exercising it with two structurally different tests. The first, `ShoppingAssistantIntegrationTest`, boots a real Spring context against a real disposable PostgreSQL database (via Testcontainers) but replaces the AI provider with a fixed stub, so the HTTP response can be asserted with an exact string match. The second, `ShoppingAssistantEvaluationTest`, uses the exact same service and database wiring but calls the real OpenAI model, then scores each answer against a small golden dataset of known inventory facts using `GroundednessScorer` instead of comparing strings. Neither test replaces the other: the first proves the retrieval-and-prompt-assembly code path is wired correctly; the second is the only one that can say whether the model's real wording is actually true.

## Prerequisites

- Java 21
- Maven (the module uses the Maven Wrapper conventions of `spring-boot-starter-parent`; any Maven 3.9+ install also works)
- Docker, running and reachable from the local Maven build — both `ShoppingAssistantIntegrationTest` and `ShoppingAssistantEvaluationTest` start a real `postgres:16-alpine` container via Testcontainers (the integration test is additionally annotated `@Testcontainers(disabledWithoutDocker = true)`, so it skips cleanly rather than failing if Docker is unavailable)
- An `OPENAI_API_KEY` environment variable with a real, funded OpenAI API key — **only** required to actually execute `ShoppingAssistantEvaluationTest`. Every other test, and the application itself, runs fine with no key set (`application.yml` falls back to a placeholder `demo-key`)

## Run it

```bash
# 1. Fast suite: the deterministic integration test plus the pure-logic
#    scorer unit tests. ShoppingAssistantEvaluationTest is still picked up
#    by Surefire here, but JUnit 5 skips it automatically (not a failure)
#    because @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", ...)
#    is not satisfied.
mvn test

# 2. Golden-dataset evaluation: set a real key and target the evaluation
#    test directly so it actually runs against the live model.
OPENAI_API_KEY=sk-your-real-key mvn test -Dtest=ShoppingAssistantEvaluationTest -DfailIfNoTests=false
```

There is no Maven profile or `@Tag` gating the evaluation test — the gate is entirely the JUnit 5 `@EnabledIfEnvironmentVariable` condition on the test class. That means `mvn test` alone is always safe to run (no network call, no API cost), and the only way to run the evaluation for real is to have `OPENAI_API_KEY` set in the environment that invokes Maven.

Both test classes start their own disposable Postgres container via Testcontainers, so nothing above needs Docker running manually beyond having the Docker daemon available. Running the application itself with `mvn spring-boot:run` is different: `application.yml` points `spring.datasource` at a real, already-running `jdbc:postgresql://localhost:5432/acme_retail`, not at Testcontainers. A `docker-compose.yml` is included in this module for that case (pre-configured with the exact `acme_retail`/`acme`/`acme` database/user/password already expected):

```bash
cd volume1_2/code/chapter-19
docker compose up -d
OPENAI_API_KEY=sk-your-real-key mvn spring-boot:run
```

## Project structure

```
src/main/java/com/acme/retail/ch19/
├── Chapter19Application.java              # Spring Boot entry point
├── assistant/
│   ├── AskQuestionRequest.java            # HTTP request body: { "question": "..." }
│   ├── AssistantAiClient.java             # Seam interface between service code and the AI provider
│   ├── AssistantAnswer.java               # HTTP response body, including the grounding facts used
│   ├── OpenAiAssistantClient.java         # Production AssistantAiClient backed by Spring AI's ChatClient
│   ├── ShoppingAssistantController.java   # POST /assistant/products/{sku}/ask
│   └── ShoppingAssistantService.java      # Builds the grounded prompt, calls the AI client
├── eval/
│   ├── EvaluationResult.java              # Scored outcome: grounded / relevant, per answer
│   ├── GoldenDataset.java                 # Hand-curated products + golden questions for evaluation
│   ├── GoldenQuestion.java                # One golden-dataset fixture: question + known-correct phrases
│   └── GroundednessScorer.java            # Pure text-matching logic that scores an answer
├── inventory/
│   └── InventoryService.java              # Turns a Product row into grounding facts for the prompt
└── product/
    ├── Product.java                       # JPA entity: name, sku, stockQuantity, category, etc.
    ├── ProductNotFoundException.java      # 404 when a SKU is not in the catalog
    └── ProductRepository.java             # Spring Data JPA repository, findBySku
```

## Examples in this module

### com.acme.retail.ch19.Chapter19Application

The standard `@SpringBootApplication` entry point. Boots the same shopping-assistant slice used elsewhere in the book; the chapter's point is not a new feature but a new way of proving the existing feature works.

### com.acme.retail.ch19.assistant.AskQuestionRequest

A one-field record, `question`, that is the JSON body of `POST /assistant/products/{sku}/ask`. Nothing chapter-specific here beyond being the smallest possible request shape both tests can send.

### com.acme.retail.ch19.assistant.AssistantAiClient

The seam this whole chapter turns on: a `@FunctionalInterface` with a single method, `String ask(String systemPrompt, String customerQuestion)`. `ShoppingAssistantService` depends only on this interface, never on Spring AI's `ChatClient` directly. `ShoppingAssistantIntegrationTest` swaps in a `@Primary` stub bean that always returns the same fixed string; `ShoppingAssistantEvaluationTest` leaves the real `OpenAiAssistantClient` wired in. Same production code path, two different implementations behind the seam — that indirection is what makes both kinds of test possible without touching `ShoppingAssistantService` itself.

### com.acme.retail.ch19.assistant.AssistantAnswer

The response record: `sku`, `question`, `answer`, and `groundingContext` — the exact inventory-facts string the model was given. Including the grounding context in the response (not just the answer) is what lets both a test and a human reading logs see precisely what the model was, and was not, told.

### com.acme.retail.ch19.assistant.OpenAiAssistantClient

The production `AssistantAiClient` implementation. Wraps a Spring AI `ChatClient` built from `ChatClient.Builder`, sends the system prompt and customer question, and returns `content()` verbatim — no parsing, no post-processing. This class is never invoked by `ShoppingAssistantIntegrationTest` (the stub bean takes its place); it is the class exercised by `ShoppingAssistantEvaluationTest`, which is exactly why that test cannot use an exact-match assertion.

### com.acme.retail.ch19.assistant.ShoppingAssistantController

A single-endpoint `@RestController` mapped at `/assistant/products`. It is deliberately thin — no logic beyond delegating to `ShoppingAssistantService` — so that both the deterministic test and the evaluation test exercise identical controller wiring.

### com.acme.retail.ch19.assistant.ShoppingAssistantService

Assembles the system prompt from real inventory facts (via `InventoryService`) and delegates to whatever `AssistantAiClient` is currently wired in. It has no idea whether it is talking to a stub or a real model — that is the design point of the whole chapter: identical production code runs under both test styles.

### com.acme.retail.ch19.eval.EvaluationResult

The scored outcome of checking one answer: `questionId`, `answer`, and two independent booleans, `grounded` and `relevant`, plus a `passed()` convenience method (`grounded && relevant`). The two booleans are kept separate on purpose — an answer can name the right product (relevant) while stating the wrong stock fact (not grounded), which is precisely the shape of bug that opened this chapter.

```java
public record EvaluationResult(String questionId, String answer, boolean grounded, boolean relevant) {

    public boolean passed() {
        return grounded && relevant;
    }
}
```

### com.acme.retail.ch19.eval.GoldenDataset

A small, hand-curated fixture: three products (one sold out, one comfortably in stock, one low stock) and three matching `GoldenQuestion`s, each with phrases that would prove an answer is grounded and phrases that would prove it is not. This is the "known-correct answer" half of a golden-dataset evaluation — the AI-evaluation equivalent of a fixture in an ordinary deterministic test.

```java
public static List<GoldenQuestion> questions() {
    return List.of(
            new GoldenQuestion(
                    "Q1-sold-out",
                    "Is the black trail running shoe in stock?",
                    "SHOE-BLK-42",
                    "Trail Runner 42",
                    List.of("sold out", "out of stock", "no longer available", "0 units"),
                    List.of("is in stock", "we have plenty", "available for purchase")),
            // ... Q2-in-stock, Q3-low-stock
    );
}
```

### com.acme.retail.ch19.eval.GoldenQuestion

The shape of one golden-dataset entry: an `id`, the customer-style `question`, the `productSku`/`productName` it is about, a list of `acceptablePhrases` (any one of which, present in the answer, counts as grounded) and a list of `contradictingPhrases` (any one of which fails the answer outright, even if an acceptable phrase is also present).

### com.acme.retail.ch19.eval.GroundednessScorer

The scoring logic itself — a plain static function with no Spring dependency and no network call, which is what makes it separately unit-testable with an ordinary deterministic test (`GroundednessScorerTest`). It lower-cases the answer, then computes two independent checks: `grounded` is true only when the answer contains at least one acceptable phrase **and** none of the contradicting phrases; `relevant` is true when the answer mentions the product's name or its SKU. Note that `grounded` and `relevant` are computed independently — an answer can be relevant without being grounded (names the product, states the wrong fact), which is exactly the bug class this scorer exists to catch.

```java
public static EvaluationResult score(GoldenQuestion question, String answer) {
    String normalized = answer == null ? "" : answer.toLowerCase();

    boolean grounded = containsAny(normalized, question.acceptablePhrases())
            && !containsAny(normalized, question.contradictingPhrases());

    boolean relevant = normalized.contains(question.productName().toLowerCase())
            || normalized.contains(question.productSku().toLowerCase());

    return new EvaluationResult(question.id(), answer, grounded, relevant);
}
```

### com.acme.retail.ch19.inventory.InventoryService

Loads a `Product` by SKU and formats it into the grounding-facts string the assistant's system prompt is built from, e.g. `"Trail Runner 42 (SKU SHOE-BLK-42, category: Footwear) -- sold out, 0 units in stock."`. Throws `ProductNotFoundException` when the SKU does not exist. This is the same retrieve-then-generate grounding pattern used earlier in the book; this chapter tests it rather than introducing it.

### com.acme.retail.ch19.product.Product

The JPA entity backing the catalog: `id`, `name`, `description`, `priceCents`, `sku`, `stockQuantity`, `category`. Same shape as the `Product` entity in other chapters — this chapter does not change the domain model, only how its behavior is verified.

### com.acme.retail.ch19.product.ProductNotFoundException

A `@ResponseStatus(HttpStatus.NOT_FOUND)` runtime exception thrown when a SKU (Stock Keeping Unit — a retailer's unique code for one product) is not found in the catalog, whether the caller is a real customer or the evaluation harness.

### com.acme.retail.ch19.product.ProductRepository

A `JpaRepository<Product, Long>` with one derived query method, `findBySku(String sku)`, implemented by Spring Data JPA from the method name alone.

## API Endpoints

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `/assistant/products/{sku}/ask` | `AskQuestionRequest` — `{ "question": "string" }` | `AssistantAnswer` — `{ "sku", "question", "answer", "groundingContext" }` |

```bash
curl -X POST http://localhost:8091/assistant/products/SHOE-BLK-42/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Is the black trail running shoe in stock?"}'
```

## Tests

- **`ShoppingAssistantIntegrationTest`** (`assistant` package) — deterministic. Starts a real `postgres:16-alpine` Testcontainer, seeds one known `Product`, and replaces `AssistantAiClient` with a `@Primary` stub bean that always returns a fixed string. Because both the database contents and the AI output are fixed, the test asserts an exact match on the HTTP response body and the grounding context. It proves the controller/service/repository/database wiring is correct; it proves nothing about whether a real model answers well. Annotated `@Testcontainers(disabledWithoutDocker = true)`, so it skips (rather than fails) if Docker is not available. Runs as part of `mvn test`.

- **`GroundednessScorerTest`** (`eval` package) — a classic deterministic unit test of pure logic: fixed `GoldenQuestion` and fixed answer text in, fixed `grounded`/`relevant` booleans out. No Spring context, no database, no network call. Verifies the scorer correctly marks an answer grounded-and-relevant when it matches the known fact, relevant-but-not-grounded when it names the right product but states the wrong fact (the chapter's motivating bug, reproduced deliberately), and not-relevant when it never addresses the product at all. Runs as part of `mvn test`.

- **`ShoppingAssistantEvaluationTest`** (`eval` package) — the golden-dataset evaluation test. Starts the same kind of Testcontainers PostgreSQL instance, seeds all three `GoldenDataset.products()`, and calls the REAL `ShoppingAssistantService` (backed by `OpenAiAssistantClient`, not a stub) for each of `GoldenDataset.questions()`. Each answer is scored with `GroundednessScorer` instead of being string-matched; the test fails only if any answer comes back ungrounded or irrelevant. Gated by `@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")` — no Maven profile, no `@Tag`, no Surefire exclusion. Run it specifically with:

  ```bash
  OPENAI_API_KEY=sk-your-real-key mvn test -Dtest=ShoppingAssistantEvaluationTest -DfailIfNoTests=false
  ```

  Without a real key present, `mvn test` still collects this test but JUnit 5 reports it as skipped, not failed or absent.

## Configuration

From `src/main/resources/application.yml`:

- `spring.ai.openai.api-key: ${OPENAI_API_KEY:demo-key}` — reads the key from the environment; the `demo-key` fallback exists only so the application context can still start (and the deterministic tests can still run) with no real credential present.
- `spring.ai.openai.chat.options.model: gpt-4o-mini` — the chat model used by `OpenAiAssistantClient` whenever the evaluation test actually runs.
- `spring.ai.openai.chat.options.temperature: 0.1` — a low temperature makes answers more repeatable, but the file's own comment is explicit that "more repeatable" is not "deterministic" — which is the reason this chapter needs a scored evaluation test rather than just a lower temperature setting.
- `spring.datasource.url: jdbc:postgresql://localhost:5432/acme_retail` (with `username`/`password: acme`) — the default local datasource, entirely overridden in both test classes by `@DynamicPropertySource`, which points Spring at the Testcontainers-managed `postgres:16-alpine` instance instead.
- `spring.jpa.hibernate.ddl-auto: update` — lets Hibernate create/update the `products` table automatically, convenient for a teaching module, not something to carry into a real production configuration unchanged.
- `server.port: 8091` — this chapter's module listens on its own port so it can run side by side with other chapters' modules.
