# Chapter 16 — Production Doesn't Care About Your Demo

*A circuit breaker has three states — CLOSED lets calls through, OPEN fails every call instantly with a fallback before it ever reaches the AI provider, and HALF-OPEN lets a handful of test calls decide whether to go back to CLOSED or straight back to OPEN.*

Companion code for **Chapter 16** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

The AI provider behind the earlier chapters' shopping assistant was always fast in the demo, so nothing in the code ever asked "what if it isn't?" This module reproduces that incident: a slow, then failing, AI provider with no timeout, retry, circuit breaker, or bulkhead in front of it will hang every calling thread indefinitely, and it fixes it by layering four Resilience4j patterns — a read timeout on the HTTP client, `@Retry` with exponential backoff, `@CircuitBreaker`, and `@Bulkhead` — around one already-working `ChatClient` call, plus a fallback that returns a safe canned answer. The three circuit-breaker states map directly onto the code: CLOSED is the normal path where `askAssistant` calls the AI provider, OPEN is what happens once 50% of the last 10 calls fail and every subsequent call gets `fallbackAnswer` without ever reaching the provider, and HALF-OPEN is the 3 probe calls Resilience4j lets through after the wait duration elapses to decide whether to close the circuit again or reopen it. The module also demonstrates the decorator-ordering trap directly: the fallback method is wired onto `@Retry`, the outermost annotation, not onto `@CircuitBreaker`, because attaching it to an inner layer would swallow failures before `@Retry` ever got a chance to retry them.

## Prerequisites

- Java 21
- Maven 3.9+ (or the wrapper, if added to this module)
- `io.github.resilience4j:resilience4j-spring-boot3` (version `2.2.0`, declared in `pom.xml`) — provides the `@Retry`, `@CircuitBreaker`, and `@Bulkhead` annotations plus the Spring Boot AOP integration that turns them into working proxies
- `org.springframework.ai:spring-ai-starter-model-openai` — the OpenAI chat starter used by `SpringAiModelClient`
- An `OPENAI_API_KEY` environment variable set to a valid OpenAI API key (falls back to a non-functional `demo-key` placeholder if unset, per `application.yml`)

## Run it

```bash
export OPENAI_API_KEY=sk-...
cd volume1_2/code/chapter-16
mvn spring-boot:run
```

The application starts on port `8080`.

```bash
curl -X POST http://localhost:8080/api/assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Are the black running shoes back in stock?"}'
```

A healthy call returns the model's real answer with `"degraded": false`. If the AI provider is slow past the 2.5-second read timeout, or fails repeatedly and trips the circuit breaker, the same endpoint instead returns the canned fallback message with `"degraded": true` — the controller's contract never changes.

## Project structure

```
src/main/java/com/acme/retail/ch16/
├── Ch16Application.java                  # Spring Boot entry point
├── config/
│   └── AiClientConfig.java               # Read/connect timeout on the AI provider's RestClient
└── assistant/
    ├── AiModelClient.java                # Abstraction over "ask the AI provider a question"
    ├── SpringAiModelClient.java          # Real implementation, calls Spring AI's ChatClient
    ├── ShoppingAssistantService.java     # Retry + CircuitBreaker + Bulkhead + fallback, in that order
    ├── AssistantController.java          # POST /api/assistant/ask -- stays simple on purpose
    ├── AssistantRequest.java             # Inbound request record: question
    └── AssistantAnswer.java              # Outbound response record: message, degraded
```

## Examples in this module

### com.acme.retail.ch16.Ch16Application

Standard `@SpringBootApplication` entry point. No chapter-specific logic; it exists so the module boots as a self-contained Spring Boot application, independent of every other chapter's build.

### com.acme.retail.ch16.config.AiClientConfig

Declares a `RestClientCustomizer` bean that sets a 1-second connect timeout and a 2.5-second read timeout on the `RestClient` Spring AI's OpenAI client uses under the hood:

```java
@Bean
public RestClientCustomizer aiProviderTimeoutCustomizer() {
    ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(1))
            .withReadTimeout(Duration.ofMillis(2500));

    return restClientBuilder ->
            restClientBuilder.requestFactory(ClientHttpRequestFactories.get(settings));
}
```

This is the one resilience pattern in the chapter that lives outside Resilience4j's annotations, and it has to exist first: without a read timeout, a provider that accepts the connection but never responds never throws anything, so `@Retry` and `@CircuitBreaker` never even get a chance to run.

### com.acme.retail.ch16.assistant.AiModelClient

The interface `ShoppingAssistantService` depends on: `String complete(String systemPrompt, String question)`. It exists specifically so tests can supply a fake implementation that throws `TimeoutException`/`RuntimeException` on demand to simulate a slow or down AI provider, without needing a real network call or a real Spring AI `ChatClient` call chain.

### com.acme.retail.ch16.assistant.SpringAiModelClient

The production `AiModelClient` implementation. It knows nothing about timeouts, retries, or circuit breakers — it is a plain wrapper around Spring AI's `ChatClient`:

```java
@Override
public String complete(String systemPrompt, String question) {
    return chatClient.prompt()
            .system(systemPrompt)
            .user(question)
            .call()
            .content();
}
```

Keeping this class resilience-free is deliberate: it is the abstraction being protected, not the place doing the protecting. All resilience is applied one layer up, in `ShoppingAssistantService`.

### com.acme.retail.ch16.assistant.ShoppingAssistantService

The core of the chapter. `askAssistant` is wrapped in three Resilience4j annotations, applied outside-in in Resilience4j's fixed order (`Retry`, then `CircuitBreaker`, then `Bulkhead`, then the method itself):

```java
@Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackAnswer")
@CircuitBreaker(name = RESILIENCE_INSTANCE)
@Bulkhead(name = RESILIENCE_INSTANCE)
public AssistantAnswer askAssistant(String question) {
    String reply = aiModelClient.complete(SYSTEM_PROMPT, question);
    return AssistantAnswer.fromModel(reply);
}

private AssistantAnswer fallbackAnswer(String question, Throwable exception) {
    log.warn("Shopping assistant fallback triggered for question '{}': {}",
            question, exception.toString());
    return AssistantAnswer.fallback();
}
```

The fallback method (`fallbackAnswer`) is attached to `@Retry`, the outermost annotation here — not to `@CircuitBreaker` or `@Bulkhead`. That placement is the whole point of the chapter's decorator-ordering lesson: Resilience4j applies these three annotations from the outside in, so `@Retry` is the last layer to see a failure, only after every retry attempt (including any instantly-failed attempt from an already-open circuit) has been exhausted. If `fallbackMethod` were declared on `@CircuitBreaker` instead, it would intercept the very first failed attempt and return a value immediately, and `@Retry` — seeing what looks like a normal successful return — would never retry at all. `@Bulkhead` caps concurrent in-flight calls to the AI provider so a slow provider cannot exhaust every request thread in the application; `@CircuitBreaker` stops sending calls to a provider that is clearly unhealthy instead of piling up more doomed attempts.

### com.acme.retail.ch16.assistant.AssistantController

The customer-facing `POST /api/assistant/ask` endpoint. It contains no `try/catch`, no health check, no branching on whether the AI provider is up — it just calls `shoppingAssistantService.askAssistant(request.question())` and returns the result. All resilience is declarative and lives entirely in `ShoppingAssistantService`, so the controller never needs to know the AI provider can fail.

### com.acme.retail.ch16.assistant.AssistantRequest

Inbound request record: `question` (free-text customer question), deserialized from the POST body.

### com.acme.retail.ch16.assistant.AssistantAnswer

Outbound response record: `message` (the customer-facing text) and `degraded` (`true` when `message` is the canned fallback rather than a real model answer). `AssistantAnswer.fromModel(String)` builds a normal answer; `AssistantAnswer.fallback()` builds the degraded one with the fixed `FALLBACK_MESSAGE` — `"Our assistant is temporarily unavailable, please browse manually."`

## API Endpoints

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `/api/assistant/ask` | JSON body: `{ "question": string }` | JSON `AssistantAnswer`: `{ "message": string, "degraded": boolean }` |

```bash
curl -X POST http://localhost:8080/api/assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Is the blue jacket available in size M?"}'
```

## Tests

- `ShoppingAssistantServiceTest` — a `@SpringBootTest` (a plain `new ShoppingAssistantService(...)` would skip Spring's AOP proxy entirely and the annotations would never run) that mocks `AiModelClient` and exercises all three resilience outcomes: a call that succeeds outright (`degraded` false, `AiModelClient.complete` invoked once); a call that fails once with a `RuntimeException` and then succeeds, proving `@Retry` recovers a transient failure without the customer noticing (`AiModelClient.complete` invoked exactly twice); and a call that fails every time with a `RuntimeException` wrapping a `TimeoutException`, proving retries are exhausted and the fallback message is returned with `degraded` true (`AiModelClient.complete` invoked at least 3 times). Yes — it explicitly simulates both a transient failing call and a persistently failing/slow AI call to prove the resilience behavior end to end, rather than just asserting on configuration.

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml` (test overrides in `src/test/resources/application-test.yml` use the same keys with much shorter durations so the test suite doesn't spend real seconds waiting):

- `resilience4j.circuitbreaker.instances.acmeAiAssistant.sliding-window-type: COUNT_BASED` / `sliding-window-size: 10` — health is judged over the last 10 calls rather than the last N seconds; this is what the circuit breaker's CLOSED-state bookkeeping watches.
- `resilience4j.circuitbreaker.instances.acmeAiAssistant.failure-rate-threshold: 50` — the circuit trips from CLOSED to OPEN once 50% of those last 10 calls failed.
- `resilience4j.circuitbreaker.instances.acmeAiAssistant.wait-duration-in-open-state: 15s` — how long the circuit stays OPEN, failing every call instantly via the fallback with nothing reaching the AI provider, before it is willing to test the waters again.
- `resilience4j.circuitbreaker.instances.acmeAiAssistant.permitted-number-of-calls-in-half-open-state: 3` — exactly the HALF-OPEN state from the mental model: 3 probe calls are let through after the wait duration; if they succeed the circuit returns to CLOSED, if they fail it reopens.
- `resilience4j.circuitbreaker.instances.acmeAiAssistant.automatic-transition-from-open-to-closed-enabled: false` — the transition out of OPEN only happens by evaluating real half-open probe calls, not on a timer alone.
- `resilience4j.retry.instances.acmeAiAssistant.max-attempts: 3` — up to 3 total attempts (1 original call plus 2 retries) before `@Retry` gives up and hands off to `fallbackAnswer`.
- `resilience4j.retry.instances.acmeAiAssistant.wait-duration: 200ms` with `enable-exponential-backoff: true` and `exponential-backoff-multiplier: 2` — waits double each time (200ms, then 400ms), so many clients retrying at once don't hit a recovering provider in a synchronized wave right when it is most fragile.
- `resilience4j.bulkhead.instances.acmeAiAssistant.max-concurrent-calls: 10` — at most 10 requests may be waiting on the AI provider at once; the 11th fails fast instead of queuing behind ten others each waiting up to the read timeout, which is what stops a slow AI dependency from starving every other endpoint of threads.
- `server` timeouts are not set here — the read timeout that actually matters is `AiClientConfig`'s 2.5-second `RestClient` read timeout, since that is what turns a hang into an exception the CLOSED-state retry/circuit-breaker logic can see in the first place.
