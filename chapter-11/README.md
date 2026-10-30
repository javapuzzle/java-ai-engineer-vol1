# Chapter 11 — The AI Is Wrong

*Question flows through retrieval before it ever reaches the model: Question → Retrieval → Knowledge → LLM → Answer, so the model is grounded in facts fetched right now from a source Sarah controls, not left to guess from training data.*

Companion code for **Chapter 11** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

This module puts an ungrounded assistant and a grounded assistant side by side behind the same controller so the difference is directly observable. `ShoppingAssistantService.askUngrounded` sends the customer's question straight to the model with no system prompt and no data — the same bug Sarah shipped in Chapter 10, which lets the model confidently claim a sold-out shoe is in stock because it has no way to know otherwise. `askGrounded` fixes it with a lightweight, manual Retrieval-Augmented Generation (RAG) pipeline: `InventoryLookupService` retrieves real rows from the `products` table, `GroundingContextBuilder` turns those rows into plain-text facts, and `GroundedPromptFactory` wraps that text in a system prompt instructing the model to answer only from it. There is no vector database or embedding search here — retrieval is a plain Spring Data JPA keyword query (`findByNameContainingIgnoreCase`) against a real relational table, which is enough for Acme Retail's small, well-controlled catalog. `DemoInventorySeeder` seeds the exact "Black Running Shoe, zero units" row the chapter's story is about, so the bug and the fix can both be reproduced on a fresh database.

## Prerequisites

- Java 21
- Maven (or the bundled `mvnw` wrapper from the repository root, if present)
- A running PostgreSQL instance reachable at `jdbc:postgresql://localhost:5432/acme_retail` with credentials `acme` / `acme` (see `application.yml`) — `spring.jpa.hibernate.ddl-auto: update` creates the `products` table automatically on startup
- Spring AI's OpenAI starter, `spring-ai-starter-model-openai` (declared in `pom.xml`) — this auto-configures a `ChatModel` and `ChatClient.Builder` from `spring.ai.openai.*` properties
- An OpenAI API key exported as the `OPENAI_API_KEY` environment variable — read via `spring.ai.openai.api-key: ${OPENAI_API_KEY:demo-key}` in `application.yml`; without it the app still starts (using the `demo-key` placeholder), but any real call to `/assistant/ask` or `/assistant/ask-ungrounded` will fail against OpenAI
- No manual seeding step is required: `DemoInventorySeeder` inserts three demo products (Black/White/Red Running Shoe) on first startup, only if the `products` table is empty
- Docker (only if you use the included `docker-compose.yml` to start that Postgres instance instead of running one yourself)

## Run it

A `docker-compose.yml` is included in this module — it starts a PostgreSQL 16 container pre-configured with the exact database/user/password (`acme_retail` / `acme` / `acme`) this module's `application.yml` already expects:

```bash
cd volume1_2/code/chapter-11
docker compose up -d
OPENAI_API_KEY=sk-your-real-key mvn spring-boot:run
```

The application starts on port `8085`. Compare the two endpoints against the same seeded data.

Ungrounded (reproduces the chapter's bug — the model has no inventory data and may guess):

```bash
curl -X POST http://localhost:8085/assistant/ask-ungrounded \
  -H "Content-Type: application/json" \
  -d '{"question": "Do you have the Black Running Shoe in stock?"}'
```

Grounded (the fix — retrieval runs before generation):

```bash
curl -X POST http://localhost:8085/assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Do you have the Black Running Shoe in stock?", "productQuery": "Black Running Shoe"}'
```

Once grounding is working, `/assistant/ask` should answer that the Black Running Shoe (SKU `RUN-BLK-42`) is out of stock with 0 units available, because that is the fact `InventoryLookupService` retrieved from the real `products` table — not a guess.

## Project structure

```
src/main/java/com/acme/retail/ch11/
├── Chapter11Application.java               # Spring Boot entry point; boots the web app, JPA, and Spring AI auto-configuration
├── assistant/
│   ├── AssistantController.java            # REST endpoints exposing both the ungrounded and grounded assistant
│   ├── GroundedPromptFactory.java           # Wraps retrieved facts in a "do not guess" system prompt
│   └── ShoppingAssistantService.java        # The "before" (askUngrounded) and "after" (askGrounded) of this chapter
├── inventory/
│   ├── DemoInventorySeeder.java             # Seeds demo products, including the sold-out shoe from the story
│   ├── GroundingContextBuilder.java         # Turns retrieved InventoryFact rows into plain-text prompt context
│   ├── InventoryFact.java                   # The shape of one retrieved, grounded fact about a product
│   └── InventoryLookupService.java          # Retrieval step: queries the real products table for facts
└── product/
    ├── Product.java                         # JPA entity mapped to the products table, Acme Retail's source of truth
    └── ProductRepository.java               # Spring Data JPA repository with the keyword-match retrieval query
```

## Examples in this module

### com.acme.retail.ch11.Chapter11Application

The `@SpringBootApplication` entry point. Starting it boots a normal Spring MVC web application, Spring Data JPA against PostgreSQL, and the Spring AI auto-configuration that wires an OpenAI-backed `ChatClient.Builder` bean from `application.yml`. There is no custom startup logic beyond `SpringApplication.run(...)`.

### com.acme.retail.ch11.assistant.AssistantController

Exposes both versions of the assistant on the same controller so they can be compared directly: `POST /assistant/ask-ungrounded` reproduces the chapter's bug and `POST /assistant/ask` is the grounded endpoint Sarah ships. The `grounded` flag on the response records which path answered.

```java
@PostMapping("/ask-ungrounded")
public AskResponse askUngrounded(@RequestBody AskRequest request) {
    String answer = shoppingAssistantService.askUngrounded(request.question());
    return new AskResponse(answer, false);
}

@PostMapping("/ask")
public AskResponse ask(@RequestBody AskRequest request) {
    String answer = shoppingAssistantService.askGrounded(request.question(), request.productQuery());
    return new AskResponse(answer, true);
}
```

### com.acme.retail.ch11.assistant.ShoppingAssistantService

Holds the "before" and "after" side by side. `askUngrounded` sends the customer's question straight to the model with no system message and no data — the model was never shown Acme Retail's `products` table, so any confident claim about stock is a hallucination, not a lie. `askGrounded` runs the retrieve-then-generate pipeline this chapter is about: retrieve facts with `InventoryLookupService`, turn them into text with `GroundingContextBuilder`, wrap that text in an instruction with `GroundedPromptFactory`, and only then call the model.

```java
public String askGrounded(String question, String productQuery) {
    List<InventoryFact> facts = inventoryLookupService.findFacts(productQuery);
    String groundingContext = GroundingContextBuilder.toContext(facts);
    String systemPrompt = GroundedPromptFactory.systemPrompt(groundingContext);

    return chatClient.prompt()
            .system(systemPrompt)
            .user(question)
            .call()
            .content();
}
```

### com.acme.retail.ch11.assistant.GroundedPromptFactory

A pure, stateless factory for the system prompt that turns retrieved facts into an enforceable boundary. It instructs the model to answer only from the facts it is given, and — critically — to say "not sure" when the facts don't cover the question, rather than keep guessing. Being a pure function of text to text (no Spring AI, no network call) means it is fully unit-testable, as `GroundedPromptFactoryTest` shows.

```java
public static String systemPrompt(String groundingContext) {
    return """
            You are the Acme Retail shopping assistant.

            Answer the customer's question using ONLY the inventory
            facts listed below. Do not use any other knowledge about
            this product, and do not guess.

            If the facts below do not mention the product the customer
            is asking about, say that you are not sure and offer to
            check with a human associate.

            Inventory facts:
            %s
            """.formatted(groundingContext);
}
```

### com.acme.retail.ch11.inventory.InventoryLookupService

The retrieval step of RAG, made concrete: it queries the real `ProductRepository` for products whose name matches the customer's `productQuery` and maps each matching row into an `InventoryFact`. Returning an empty list when nothing matches is itself a meaningful fact — "the catalog does not know this product" is different from "the product exists and is in stock," and collapsing that distinction is exactly how hallucinations happen.

```java
public List<InventoryFact> findFacts(String productQuery) {
    List<Product> matches = productRepository.findByNameContainingIgnoreCase(productQuery);

    return matches.stream()
            .map(InventoryLookupService::toFact)
            .toList();
}

private static InventoryFact toFact(Product product) {
    int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
    boolean available = stockQuantity > 0;
    return new InventoryFact(product.getName(), product.getSku(), stockQuantity, available);
}
```

### com.acme.retail.ch11.inventory.InventoryFact

The shape of one retrieved, grounded fact: `productName`, `sku`, `stockQuantity`, and `available` (`true` when `stockQuantity > 0`). This record is the hand-off point in the pipeline — text the model can read and quote, standing in for the live database connection it does not have.

```java
public record InventoryFact(String productName, String sku, int stockQuantity, boolean available) {
}
```

### com.acme.retail.ch11.inventory.GroundingContextBuilder

Turns a `List<InventoryFact>` into the plain-text block that gets embedded in the prompt, formatting each fact as a bullet line (`- <name> (SKU <sku>): <n> units in stock.` or `out of stock (0 units available).`) and falling back to an honest "No matching product was found in the Acme Retail catalog." when the list is empty. It is a pure function — same facts in, same string out — so `GroundingContextBuilderTest` exercises it with no database and no model call.

```java
public static String toContext(List<InventoryFact> facts) {
    if (facts.isEmpty()) {
        return "No matching product was found in the Acme Retail catalog.";
    }

    StringBuilder context = new StringBuilder();
    for (InventoryFact fact : facts) {
        context.append("- ").append(fact.productName()).append(" (SKU ").append(fact.sku()).append("): ");
        if (fact.available()) {
            context.append(fact.stockQuantity()).append(" units in stock.");
        } else {
            context.append("out of stock (0 units available).");
        }
        context.append(System.lineSeparator());
    }
    return context.toString().stripTrailing();
}
```

### com.acme.retail.ch11.inventory.DemoInventorySeeder

An `ApplicationRunner` that seeds three demo products on startup, but only when the `products` table is empty, so the module is runnable on a fresh database with no separate data-loading step. The "Black Running Shoe" row (`stockQuantity` = 0) is the exact product from the chapter's story — everything else in this chapter exists to make sure the assistant reports that honestly instead of guessing.

```java
productRepository.save(new Product(
        "Black Running Shoe", "Lightweight everyday trainer.", 8999,
        "RUN-BLK-42", 0, "Footwear"));
```

### com.acme.retail.ch11.product.Product

The JPA entity mapped to the `products` table — Acme Retail's single source of truth for what is actually in stock. It is the same domain shape used since Chapter 01, and it is the "application data" this chapter is about: data the language model has never seen and cannot know unless the application shows it.

### com.acme.retail.ch11.product.ProductRepository

The only place in this module that "knows" about the database. `findByNameContainingIgnoreCase` is a Spring Data JPA query-derivation method — Spring Data reads the method name and generates `SELECT * FROM products WHERE LOWER(name) LIKE LOWER('%...%')` for us. This is a deliberately simple keyword match, not semantic search: Acme Retail's product names are short and well controlled, so there is no need for embeddings or a vector store here.

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String nameFragment);
}
```

## API Endpoints

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/assistant/ask-ungrounded` | `AskRequest` — `{ "question": "<customer text>" }` | `AskResponse` — `{ "answer": "...", "grounded": false }` |
| POST | `/assistant/ask` | `AskRequest` — `{ "question": "<customer text>", "productQuery": "<keyword to match against product name>" }` | `AskResponse` — `{ "answer": "...", "grounded": true }` |

Example:

```bash
curl -X POST http://localhost:8085/assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Is the red running shoe available?", "productQuery": "Red Running Shoe"}'
```

## Tests

- `GroundedPromptFactoryTest` — asserts the assembled system prompt actually contains the retrieved facts passed into it (for example, `"Black Running Shoe"` and `"out of stock"`), and that it carries the `"do not guess"` / `"ONLY the inventory"` instructions that make grounding enforceable rather than just informational. It never calls a real model.
- `GroundingContextBuilderTest` — verifies the pure text-formatting logic: an out-of-stock fact renders as `"out of stock (0 units available)"`, an in-stock fact renders its quantity (`"12 units in stock"`), and an empty fact list renders the honest `"No matching product was found"` fallback.
- `InventoryLookupServiceTest` — mocks `ProductRepository` (via Mockito) to prove the retrieval-to-fact mapping in isolation: a product with `stockQuantity` 0 maps to `available = false`, a product with `stockQuantity` 12 maps to `available = true`, and a query with no repository matches returns an empty fact list — all without a real PostgreSQL database.

Run the tests:

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/acme_retail
    username: acme
    password: acme

  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false

  ai:
    openai:
      api-key: ${OPENAI_API_KEY:demo-key}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.2

server:
  port: 8085
```

- `spring.datasource.url` / `username` / `password` — points the app at a local PostgreSQL database named `acme_retail`; this is the real `products` table `InventoryLookupService` retrieves from, so without a reachable database the retrieval step (and the app itself) cannot start.
- `spring.jpa.hibernate.ddl-auto: update` — lets Hibernate create/update the `products` table schema automatically, so this module needs no manual migration step to run.
- `spring.ai.openai.api-key: ${OPENAI_API_KEY:demo-key}` — reads the real key from the `OPENAI_API_KEY` environment variable at startup, falling back to the placeholder `demo-key` so the app (and its tests) can still start without a real key; never commit a real key into this file.
- `spring.ai.chat.options.model: gpt-4o-mini` — pins the specific OpenAI chat model the `ChatClient` calls.
- `spring.ai.chat.options.temperature: 0.2` — keeps answers close to deterministic, which matters for an assistant that is supposed to report facts rather than improvise.
- `server.port: 8085` — the port the controller in this README's examples assumes.
