# Chapter 10 — Sarah Meets AI

*Every request funnels through one box: a Spring AI `ChatClient` call that assembles the customer's message and Acme Retail's system prompt into a single request before anything reaches the model.*

Companion code for **Chapter 10** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

This module is Acme Retail's first real AI-enabled Spring endpoint. It uses Spring AI's `ChatClient` to send a shopping request to an OpenAI-compatible chat model and get back a structured recommendation instead of raw text. The endpoint attaches a system prompt built from `CatalogContextProvider`, so the model knows it is Acme Retail's assistant and which product categories it is allowed to talk about — the "box in the middle" that every user message and every bit of context must pass through before it becomes one outgoing prompt. The response is bound directly to a `ProductRecommendation` record via Spring AI's structured output support, so the controller never has to parse free-form text. A unit test proves the controller's behavior with a mocked `ChatClient`, without ever calling a real model.

## Prerequisites

- Java 21
- Maven (or the bundled `mvnw` wrapper from the repository root, if present)
- Spring AI's OpenAI starter, `spring-ai-starter-model-openai` (declared in `pom.xml`) — this auto-configures a `ChatModel` and `ChatClient.Builder` from `spring.ai.openai.*` properties
- An OpenAI API key exported as the `OPENAI_API_KEY` environment variable — this is the property Spring AI reads via `spring.ai.openai.api-key: ${OPENAI_API_KEY:...}` in `application.yml`; without it the app still starts (using a placeholder value), but any real call to `/api/shopping-assistant/recommend` will fail against OpenAI

## Run it

```bash
OPENAI_API_KEY=sk-your-real-key mvn spring-boot:run
```

The application starts on port `8080`. Send a shopping request to the assistant:

```bash
curl -X POST http://localhost:8080/api/shopping-assistant/recommend \
  -H "Content-Type: application/json" \
  -d '{"message": "I need something for trail running"}'
```

A successful response is a JSON object matching `ProductRecommendation`, for example:

```json
{
  "productName": "Trailblazer GTX",
  "category": "Running Shoes",
  "reason": "A durable trail running shoe suited to off-road terrain."
}
```

## Project structure

```
src/main/java/com/acme/retail/ch10/
├── Chapter10Application.java              # Spring Boot entry point; boots the web app and Spring AI auto-configuration
└── assistant/
    ├── CatalogContextProvider.java         # Hardcoded summary of Acme Retail's catalog categories, injected into the system prompt
    ├── ChatClientConfig.java               # Builds the ChatClient bean from the auto-configured ChatClient.Builder
    ├── ProductRecommendation.java          # Structured output record the model's JSON response is bound to
    ├── ShoppingAssistantController.java    # REST endpoint that assembles the system prompt + user message into one ChatClient call
    └── ShoppingAssistantRequest.java       # Request body record for the customer's message
```

## Examples in this module

### com.acme.retail.ch10.Chapter10Application

The `@SpringBootApplication` entry point. Starting it boots a normal Spring MVC web application plus the Spring AI auto-configuration that wires an OpenAI-backed `ChatClient.Builder` bean from `application.yml`. There is no custom startup logic beyond `SpringApplication.run(...)`.

### com.acme.retail.ch10.assistant.ChatClientConfig

Turns the auto-configured `ChatClient.Builder` into a plain `ChatClient` bean that the controller can depend on. This is the only wiring this chapter needs: Spring AI already builds the `Builder` from `spring.ai.openai.*` properties, so this class just calls `.build()` once and exposes the result, which also makes the controller trivial to unit test with a mock `ChatClient`.

```java
@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient shoppingAssistantChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
```

### com.acme.retail.ch10.assistant.CatalogContextProvider

Supplies the only catalog knowledge the model is allowed to use: a short, hand-written summary of Acme Retail's product categories (Running Shoes, Hiking Boots, Backpacks, Accessories). It is deliberately static, not a live database query — this chapter is about handing a model *any* context at all, not about retrieving live per-product data, which is Chapter 11's retrieval-augmented generation problem. Note that the summary never mentions stock levels; the model is not given data it could guess wrong about.

### com.acme.retail.ch10.assistant.ProductRecommendation

A Java record — `productName`, `category`, `reason` — that the model's JSON response is bound into via Spring AI's structured output support, instead of the controller parsing free-form text. It intentionally omits fields like `inStock` or `stockQuantity`, because the model was never given live inventory data in its system prompt, so asking it to report on stock would only invite it to guess.

### com.acme.retail.ch10.assistant.ShoppingAssistantRequest

The incoming request body record for `POST /api/shopping-assistant/recommend`, holding a single `message` field — the customer's request in their own words.

### com.acme.retail.ch10.assistant.ShoppingAssistantController

The REST controller, and the chapter's "box in the middle" made concrete. It formats a `SYSTEM_PROMPT` template with the catalog summary from `CatalogContextProvider`, then builds one `ChatClient` request that carries both that system prompt and the customer's message, and asks Spring AI to bind the result directly to a `ProductRecommendation`:

```java
@PostMapping("/recommend")
public ProductRecommendation recommend(@RequestBody ShoppingAssistantRequest request) {
    String systemPrompt = SYSTEM_PROMPT.formatted(catalogContext.summary());

    return chatClient.prompt()
            .system(systemPrompt)
            .user(request.message())
            .call()
            .entity(ProductRecommendation.class);
}
```

Nothing about the customer's message reaches the model without also carrying the system prompt through this same `.prompt()...call()` chain — that convergence is the mental model this chapter introduces.

## API Endpoints

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/api/shopping-assistant/recommend` | `ShoppingAssistantRequest` — `{ "message": "<customer text>" }` | `ProductRecommendation` — `{ "productName": "...", "category": "...", "reason": "..." }` |

Example:

```bash
curl -X POST http://localhost:8080/api/shopping-assistant/recommend \
  -H "Content-Type: application/json" \
  -d '{"message": "I need boots for weekend day hikes"}'
```

## Tests

- `CatalogContextProviderTest` — verifies that the catalog summary mentions the expected categories (Running Shoes, Hiking Boots, Backpacks) and, importantly, never mentions stock levels, enforcing the boundary that the model only ever sees category-level descriptions.
- `ShoppingAssistantControllerTest` — does **not** call a real AI provider. It mocks `ChatClient` with Mockito's deep stubs to stub the entire fluent chain (`prompt().system(...).user(...).call().entity(...)`) in one call, then asserts the controller returns exactly the structured `ProductRecommendation` the mocked client produced.

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
      api-key: ${OPENAI_API_KEY:sk-REPLACE-WITH-A-REAL-KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.3
```

- `spring.ai.openai.api-key: ${OPENAI_API_KEY:...}` — reads the real key from the `OPENAI_API_KEY` environment variable at startup, with a placeholder fallback so the app (and its tests) can still start without a real key. Never commit a real key into this file.
- `spring.ai.chat.options.model: gpt-4o-mini` — pins the specific OpenAI chat model the `ChatClient` calls; changing it changes cost and response quality without touching any Java code.
- `spring.ai.chat.options.temperature: 0.3` — keeps recommendations relatively deterministic and on-catalog rather than creatively varied, which matters for a shopping assistant that should stick to the categories it was told about.
- `server.port: 8080` — the port the controller in this README's examples assumes.
