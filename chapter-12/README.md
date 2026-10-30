# Chapter 12 — Customers Don't Want to Wait

*A traditional endpoint makes the customer stare at a blank screen until the entire AI answer is ready; a streaming endpoint shows them the first words almost immediately, even though the model takes exactly as long to finish either way.*

Companion code for **Chapter 12** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

This module puts the same shopping-assistant question behind two endpoints so the difference in perceived latency is visible side by side. `POST /assistant/ask` calls Spring AI's blocking `ChatClient` API (`.call().content()`) and returns one `String` only after the model has produced the complete answer. `POST /assistant/ask/stream` calls the reactive `.stream().content()` API instead, returning a `Flux<String>` of answer chunks as Server-Sent Events, so the browser can render the first token long before the model has finished generating the rest. Both endpoints share the same `AssistantService`, the same system prompt, and the same `InventoryService`-backed catalog facts — the only thing that changes is how the answer travels back to the customer. The controller logs both first-token latency and total latency for the streaming call (and total latency for the blocking call), and the test suite proves, with a deliberately slowed-down fake `ChatModel`, that first-token latency is measurably smaller than total latency — the mechanism behind the chapter's "customers don't want to wait" argument, without inventing any specific millisecond numbers.

## Prerequisites

- Java 21
- Maven (or the bundled `mvnw` wrapper from the repository root, if present)
- Spring AI's OpenAI starter, `spring-ai-starter-model-openai` (declared in `pom.xml`) — auto-configures a `ChatModel` bean from `spring.ai.openai.*` properties, which `AssistantConfig` wraps in a `ChatClient`
- An OpenAI API key exported as the `OPENAI_API_KEY` environment variable — read via `spring.ai.openai.api-key: ${OPENAI_API_KEY:...}` in `application.yml`; without it the app still starts (using a placeholder value), but real calls to either `/assistant/ask` or `/assistant/ask/stream` will fail against OpenAI

## Run it

```bash
OPENAI_API_KEY=sk-your-real-key mvn spring-boot:run
```

The application starts on port `8086`.

Blocking endpoint — the response only arrives once the full answer is ready:

```bash
curl -X POST http://localhost:8086/assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Is the black running shoe available?"}'
```

Streaming endpoint — tokens arrive incrementally as Server-Sent Events:

```bash
curl -N -X POST http://localhost:8086/assistant/ask/stream \
  -H "Content-Type: application/json" \
  -d '{"question": "Is the black running shoe available?"}'
```

Use `curl -N` (disable output buffering) on the streaming call so you actually see each chunk print as it arrives instead of curl holding everything until the connection closes; watch the application log for the `First token after ... ms` and `Stream complete after ... ms` lines to compare first-token latency against total latency for yourself.

## Project structure

```
src/main/java/com/acme/retail/ch12/
├── Chapter12Application.java              # Spring Boot entry point
├── assistant/
│   ├── AssistantController.java           # REST controller exposing the blocking /assistant/ask and streaming /assistant/ask/stream endpoints
│   ├── AssistantService.java              # Calls the shared ChatClient two ways: .call() (blocking) and .stream() (reactive)
│   └── dto/
│       └── AssistantRequest.java          # Request body record carrying the customer's question
├── config/
│   └── AssistantConfig.java               # Builds the ChatClient bean from the auto-configured ChatModel
└── product/
    ├── InventoryService.java              # Turns catalog rows into short factual sentences the assistant may repeat
    ├── Product.java                       # Immutable product record
    └── ProductRepository.java             # In-memory stand-in for the Acme Retail product catalog
```

## Examples in this module

### com.acme.retail.ch12.Chapter12Application

The `@SpringBootApplication` entry point. Booting it starts the same shopping-assistant idea as earlier chapters, now exposed through two endpoints instead of one — a synchronous one that waits for the whole answer, and a streaming one that sends the answer to the browser one token at a time.

### com.acme.retail.ch12.config.AssistantConfig

Wraps the auto-configured `ChatModel` bean (created by the Spring AI OpenAI starter from `spring.ai.openai.*` properties) in a `ChatClient` bean:

```java
@Bean
public ChatClient chatClient(ChatModel chatModel) {
    return ChatClient.builder(chatModel).build();
}
```

This is the same `ChatClient` instance `AssistantService` uses for both the blocking `.call()` path and the reactive `.stream()` path — the fork between "wait" and "show what you have" happens entirely inside `AssistantService`, not in how the client is built.

### com.acme.retail.ch12.assistant.dto.AssistantRequest

A single-field record, `question`, sent as the JSON request body to both `/assistant/ask` and `/assistant/ask/stream`.

### com.acme.retail.ch12.assistant.AssistantService

Holds the two answer strategies side by side, both grounded in the same system prompt and the same `InventoryService.catalogFacts()`:

```java
/** Traditional approach: wait for the complete answer, then return it. */
public String askAndWaitForFullAnswer(String question) {
    return chatClient.prompt()
            .system(SYSTEM_PROMPT.formatted(inventoryService.catalogFacts()))
            .user(question)
            .call()
            .content();
}

/** Modern approach: stream the answer as the model generates it. */
public Flux<String> askAndStreamAnswer(String question) {
    return chatClient.prompt()
            .system(SYSTEM_PROMPT.formatted(inventoryService.catalogFacts()))
            .user(question)
            .stream()
            .content();
}
```

Mechanically, the only difference between the two methods is the terminal call in the fluent chain: `askAndWaitForFullAnswer` ends in `.call().content()`, which blocks the calling thread until Spring AI has assembled the model's complete response into one `String`. `askAndStreamAnswer` ends in `.stream().content()`, which returns immediately with a `Flux<String>` — a reactive publisher that emits one answer chunk (delta) each time the underlying provider sends one, and completes only once the model is done. Everything upstream of that terminal call (the system prompt, the user question, the `ChatClient` instance) is identical.

### com.acme.retail.ch12.assistant.AssistantController

Exposes both `AssistantService` methods as HTTP endpoints and instruments each with `Instant`-based timing so the latency difference is visible in the logs, not just in the code:

```java
@PostMapping("/ask")
public String ask(@RequestBody AssistantRequest request) {
    Instant start = Instant.now();
    String answer = assistantService.askAndWaitForFullAnswer(request.question());
    log.info("Synchronous answer ready after {} ms (total latency)",
            Duration.between(start, Instant.now()).toMillis());
    return answer;
}

@PostMapping(path = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> askStream(@RequestBody AssistantRequest request) {
    Instant start = Instant.now();
    AtomicReference<Instant> firstTokenAt = new AtomicReference<>();

    return assistantService.askAndStreamAnswer(request.question())
            .doOnNext(chunk -> {
                if (firstTokenAt.compareAndSet(null, Instant.now())) {
                    log.info("First token after {} ms (first-token latency)",
                            Duration.between(start, firstTokenAt.get()).toMillis());
                }
            })
            .doOnComplete(() -> log.info("Stream complete after {} ms (total latency)",
                    Duration.between(start, Instant.now()).toMillis()));
}
```

`ask` returns a plain `String` from a `@PostMapping`, so Spring MVC cannot write anything to the response body until the method returns — the customer's connection sits idle for the entire generation time. `askStream` declares `produces = MediaType.TEXT_EVENT_STREAM_VALUE` and returns `Flux<String>`; Spring MVC recognizes the reactive return type and dispatches the request asynchronously, writing each emitted chunk to the response as a Server-Sent Event the moment `doOnNext` sees it, rather than waiting for `doOnComplete`. The `AtomicReference<Instant>` and `compareAndSet` exist purely to capture the timestamp of the *first* emitted chunk exactly once, so the first-token-latency log line fires only on the first token, not on every one.

### com.acme.retail.ch12.product.Product

An immutable record (`id`, `name`, `description`, `priceCents`, `sku`, `stockQuantity`, `category`) representing one catalog item. Field names intentionally match the `Product` entity from earlier, database-backed chapters so the domain stays recognizable across the book.

### com.acme.retail.ch12.product.ProductRepository

An in-memory stand-in for the catalog: a `LinkedHashMap<Long, Product>` preloaded in the constructor with three seed products (two running-shoe variants and a rain jacket), exposing `findAll()` and `findById(Long)`. Earlier chapters backed this same class name and method signatures with a real PostgreSQL-backed `JpaRepository`; this chapter keeps persistence out of the way because streaming, not storage, is the lesson.

### com.acme.retail.ch12.product.InventoryService

Converts each `Product` into one short factual sentence (for example, "The Trailhead Running Shoe (SHOE-BLK-001) has 4 in stock.") via `catalogFacts()`, and hands the whole concatenated string to `AssistantService` as the system prompt's grounding data. This is the same "ground the model in real data" discipline introduced in Chapter 11 — Chapter 12 does not change what the model is allowed to know, only how its answer is delivered once it has decided what to say.

## API Endpoints

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/assistant/ask` | `AssistantRequest` — `{ "question": "<customer text>" }` | `text/plain` body — the complete answer, sent only after generation finishes |
| POST | `/assistant/ask/stream` | `AssistantRequest` — `{ "question": "<customer text>" }` | `text/event-stream` — one chunk of the answer per SSE event, as the model generates it |

Blocking endpoint:

```bash
curl -X POST http://localhost:8086/assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Is the black running shoe available?"}'
```

Streaming endpoint:

```bash
curl -N -X POST http://localhost:8086/assistant/ask/stream \
  -H "Content-Type: application/json" \
  -d '{"question": "Is the black running shoe available?"}'
```

## Tests

- `InventoryServiceTest` — verifies `catalogFacts()` lists every seeded product with its SKU and correct stock count (`SHOE-BLK-001` with 4 in stock, `SHOE-WHT-001` with 0 in stock, `JKT-RAIN-002` present).
- `AssistantServiceTest` — exercises `AssistantService` against a hand-written fake `ChatModel` (`FakeStreamingChatModel`) that "generates" a fixed answer one token at a time with a small delay between tokens. Verifies `askAndWaitForFullAnswer` returns the complete concatenated answer, `askAndStreamAnswer` emits one chunk per token via `StepVerifier`, and — the chapter's central claim — that first-token latency is strictly less than total latency when streaming.
- `AssistantControllerTest` — a `@WebMvcTest` that posts to `/assistant/ask/stream` and asserts the servlet container starts an async dispatch (`request().asyncStarted()`), and that the completed response has a `text/event-stream`-compatible content type containing the expected text. Uses a second hand-written fake `ChatModel` (`FixedAnswerChatModel`) rather than a mocking framework, so the whole module's tests need no `OPENAI_API_KEY` and no Mockito.

Run the tests:

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:demo-key-set-a-real-key-to-run-this}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.3

server:
  port: 8086
```

- `spring.ai.openai.api-key: ${OPENAI_API_KEY:...}` — reads the real key from the `OPENAI_API_KEY` environment variable at startup, falling back to a placeholder so the app and its tests can still start without one; the placeholder is enough for `mvn test` (fake `ChatModel`s only) but not for real calls to either endpoint.
- `spring.ai.chat.options.model: gpt-4o-mini` — the OpenAI chat model both the blocking `.call()` and streaming `.stream()` calls use; the same `ChatClient` and model back both endpoints, so this setting affects them equally.
- `spring.ai.chat.options.temperature: 0.3` — keeps answers relatively deterministic and on-catalog, which matters here since streamed partial answers are shown to the customer as they are generated, with no chance to discard a bad completion before display.
- `server.port: 8086` — the port the controller in this README's examples assumes.
