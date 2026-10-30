# Chapter 15 — Sarah Builds an Escape Hatch

*Provider portability is a promise Spring AI's `ChatClient` abstraction makes in theory, but it only becomes true once a real fallback path is coded, wired, and tested against a real failure.*

Companion code for **Chapter 15** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

Chapter 09 established the principle that application code should depend on Spring AI's portable `ChatClient` interface rather than a vendor SDK, so that swapping providers is a configuration change, not a rewrite. This module is where that principle is actually put to the test. Two provider starters — OpenAI and Anthropic — sit on the classpath at the same time, and each is exposed as its own explicitly named `ChatClient` bean rather than the single implicit bean every earlier chapter relied on. A thin resilience layer, `ResilientAssistantService`, calls the primary (OpenAI) `ChatClient` first and, if that call throws for any reason — outage, timeout, rate limit — falls back to the secondary (Anthropic) `ChatClient` automatically, at runtime, with no redeploy. The response reports back which provider actually produced the answer, and a unit test proves the failover branch executes, not just the happy path.

## Prerequisites

- Java 21
- Maven (or the wrapper, if present in your environment)
- An OpenAI API key exported as `OPENAI_API_KEY` (primary provider)
- An Anthropic API key exported as `ANTHROPIC_API_KEY` (fallback provider)

Both keys default to a placeholder value (`demo-key`) in `application.yml`, so the app will start without them, but any real call to `/api/resilient-assistant/ask` will fail against both providers unless at least the primary key is valid.

## Run it

```bash
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...

mvn spring-boot:run
```

The app starts on port `8080`. Send a request to the resilient assistant endpoint:

```bash
curl -X POST http://localhost:8080/api/resilient-assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Are trail running shoes in stock?"}'
```

The JSON response includes a `providerUsed` field (`PRIMARY` or `FALLBACK`) that reports which provider actually answered the question — `PRIMARY` under normal conditions, `FALLBACK` only when the OpenAI call failed and Anthropic answered instead.

## Project structure

```
src/main/java/com/acme/retail/ch15/
├── Chapter15Application.java              # Spring Boot entry point
└── assistant/
    ├── AssistantAnswer.java               # Response record: answer text + which provider produced it
    ├── ProviderChatClientConfig.java       # Declares the two named ChatClient beans (primary/fallback)
    ├── ProviderUsed.java                   # Enum: PRIMARY or FALLBACK
    ├── ResilientAssistantController.java  # REST endpoint that delegates to the service
    └── ResilientAssistantService.java      # The try-primary/catch/fallback failover logic
```

## Examples in this module

### com.acme.retail.ch15.Chapter15Application

The standard `@SpringBootApplication` entry point. It has no chapter-specific logic; its only role is bootstrapping component scanning and auto-configuration so both provider auto-configurations (OpenAI and Anthropic) and the `assistant` package are picked up.

### com.acme.retail.ch15.assistant.ProviderChatClientConfig

Declares the two distinct, explicitly named `ChatClient` beans that make dual-provider configuration possible in the same application context. With both the `spring-ai-starter-model-openai` and `spring-ai-starter-model-anthropic` starters on the classpath, Spring Boot auto-configures one `ChatModel` per provider (`OpenAiChatModel`, `AnthropicChatModel`); this class wraps each in its own named bean, `primaryChatClient` and `fallbackChatClient`, so nothing is left for Spring to guess:

```java
@Bean
ChatClient primaryChatClient(OpenAiChatModel openAiChatModel) {
    return ChatClient.builder(openAiChatModel).build();
}

@Bean
ChatClient fallbackChatClient(AnthropicChatModel anthropicChatModel) {
    return ChatClient.builder(anthropicChatModel).build();
}
```

### com.acme.retail.ch15.assistant.ProviderUsed

A two-value enum, `PRIMARY` and `FALLBACK`, that records which provider actually handled a given request. It exists so an incident review — or a test — can prove the failover path really executed, rather than only asserting that it theoretically could.

### com.acme.retail.ch15.assistant.AssistantAnswer

A response record combining the generated `text` with the `providerUsed` value, so every answer returned by the API is self-describing about which provider produced it.

### com.acme.retail.ch15.assistant.ResilientAssistantService

The core of the chapter: a `@Service` that tries the primary `ChatClient` first and falls back to the secondary one on any `RuntimeException`. Because `ProviderChatClientConfig` exposes two beans of the same `ChatClient` type, the constructor uses `@Qualifier("primaryChatClient")` and `@Qualifier("fallbackChatClient")` to tell Spring unambiguously which bean fills which parameter — without these qualifiers, Spring cannot disambiguate the two `ChatClient` beans by parameter name alone. The failover logic itself:

```java
public AssistantAnswer ask(String question) {
    try {
        String answer = primaryChatClient.prompt()
                .user(question)
                .call()
                .content();
        return new AssistantAnswer(answer, ProviderUsed.PRIMARY);
    } catch (RuntimeException primaryFailure) {
        log.warn("Primary provider failed, falling back to secondary provider", primaryFailure);

        String answer = fallbackChatClient.prompt()
                .user(question)
                .call()
                .content();
        return new AssistantAnswer(answer, ProviderUsed.FALLBACK);
    }
}
```

### com.acme.retail.ch15.assistant.ResilientAssistantController

A `@RestController` exposing a single `POST /api/resilient-assistant/ask` endpoint. It accepts a validated `AskRequest` (a `@NotBlank` question) and delegates directly to `ResilientAssistantService.ask(...)`, returning the resulting `AssistantAnswer` as JSON.

## API Endpoints

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/api/resilient-assistant/ask` | `{"question": "<non-blank string>"}` | `{"text": "<answer>", "providerUsed": "PRIMARY" \| "FALLBACK"}` |

```bash
curl -X POST http://localhost:8080/api/resilient-assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Do you have any waterproof hiking boots?"}'
```

## Tests

- `ResilientAssistantServiceTest` — two tests covering the failover policy directly against mocked `ChatClient` instances (no real API keys or network calls required):
  - `ask_usesPrimaryProvider_whenPrimarySucceeds` verifies that when the primary `ChatClient` mock returns content normally, the service returns `ProviderUsed.PRIMARY` and never touches the fallback mock.
  - `ask_fallsBackToSecondaryProvider_whenPrimaryThrows` configures the primary `ChatClient` mock to throw a `RuntimeException` on any invocation, simulating a real primary-provider outage, and asserts the service catches it and returns an answer from the fallback mock with `ProviderUsed.FALLBACK` — proving the fallback branch actually executes, not just that it compiles.

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

- `spring.ai.openai.api-key: ${OPENAI_API_KEY:demo-key}` — the primary provider's credential; must be a real key for `/ask` to succeed against OpenAI, since the placeholder `demo-key` will only trigger the fallback path.
- `spring.ai.openai.chat.options.model: gpt-4o-mini` — pins the primary model so the chapter's cost and latency characteristics stay predictable and reproducible.
- `spring.ai.anthropic.api-key: ${ANTHROPIC_API_KEY:demo-key}` — the fallback provider's credential; this is the key that actually gets exercised when the primary call fails.
- `spring.ai.anthropic.chat.options.model: claude-3-5-sonnet-latest` — pins the fallback model so a failover produces a consistent, comparable answer rather than drifting with provider defaults.
- `server.port: 8080` — the HTTP port the module listens on.
- `logging.level.org.springframework.ai: INFO` — keeps Spring AI's request/response logging at a level useful for observing which provider is being called without full DEBUG noise.
