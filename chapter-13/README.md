# Chapter 13 — Sarah Forgot the Customer

*Every LLM call starts with amnesia: the model itself remembers nothing, so the application must resend the whole conversation transcript on every call.*

Companion code for **Chapter 13** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

A customer tells the shopping assistant "I want black running shoes," then on the next turn says "Only size 8." Without help, the assistant has no idea what product "size 8" refers to, because each call to the `ChatModel` is stateless — it only knows what is inside the prompt it just received. This module fixes that by introducing a `ChatMemoryStore` abstraction, backed in production by `RedisChatMemoryStore`, that stores every turn of a session and hands it back on the next call. `ShoppingAssistantService` loads that history before every `ChatClient` call, rebuilds the full message list (system prompt, then every stored turn, then the newest customer message), and only after the model replies does it append the new user and assistant turns back into the store. The history is capped at a configurable number of turns and expires after a configurable idle TTL, so the "memory" is really just discipline: reassemble the transcript, every time, and bound how much of it you carry.

## Prerequisites

- Java 21
- Maven (or the bundled `mvnw` wrapper from the repository root, if present)
- A running Redis instance for the *application itself* — `spring-boot-starter-data-redis` auto-configures a connection from `spring.data.redis.host`/`port`, and `RedisChatMemoryStore` is the only `ChatMemoryStore` bean wired by `ChatMemoryConfig`, so the app will fail to start (or fail on first use) without Redis reachable at `REDIS_HOST:REDIS_PORT` (default `localhost:6379`)
- Docker, for the test suite only — `RedisChatMemoryStoreTest` starts a real `redis:7-alpine` container via Testcontainers (`@Testcontainers(disabledWithoutDocker = true)`); it does not use an in-memory fake, so it is skipped automatically if Docker is unavailable
- Spring AI's OpenAI starter, `spring-ai-starter-model-openai` (declared in `pom.xml`)
- An OpenAI API key exported as the `OPENAI_API_KEY` environment variable — read via `spring.ai.openai.api-key: ${OPENAI_API_KEY:demo-key}` in `application.yml`; the placeholder lets the app start and lets the unit tests run (they mock `ChatModel`), but any real call to `/api/assistant/...` needs a real key
- Docker (optionally, to use the included `docker-compose.yml` instead of running Redis yourself)

## Run it

A `docker-compose.yml` is included in this module — it starts Redis 7 on the default port `6379` this module's `application.yml` already expects:

```bash
cd volume1_2/code/chapter-13
docker compose up -d

# In another terminal, from this module's directory
OPENAI_API_KEY=sk-your-real-key mvn spring-boot:run
```

The application starts on port `8080`. The caller supplies the `sessionId` in the URL — the same id must be reused across the whole conversation so `ShoppingAssistantService` finds the right history in Redis:

```bash
# Turn 1
curl -X POST http://localhost:8080/api/assistant/sessions/session-42/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "I want black running shoes."}'
# {"sessionId":"session-42","reply":"Sure -- what size are you looking for?"}

# Turn 2, same sessionId
curl -X POST http://localhost:8080/api/assistant/sessions/session-42/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "Only size 8."}'
# {"sessionId":"session-42","reply":"Got it -- black running shoes, size 8."}
```

The second call only ever sends "Only size 8." as the new user message, but the assistant still answers about black running shoes, because `ShoppingAssistantService` loaded turn 1 back out of Redis and put it in front of the new message before calling the model.

## Project structure

```
src/main/java/com/acme/retail/ch13/
├── Ch13Application.java                    # Spring Boot entry point
├── assistant/
│   ├── AssistantController.java            # REST endpoint: POST /api/assistant/sessions/{sessionId}/messages
│   ├── ChatReply.java                      # Response record: sessionId + reply text
│   ├── ChatRequest.java                    # Request record: the customer's newest message
│   └── ShoppingAssistantService.java       # Loads history, rebuilds the prompt, calls the model, appends the new turns
└── chat/
    ├── ChatMemoryConfig.java               # Wires the ChatMemoryStore bean (RedisChatMemoryStore) from Redis + Jackson + properties
    ├── ChatMemoryProperties.java           # chat.memory.* config: max-turns and ttl
    ├── ChatMemoryStore.java                # Storage-agnostic abstraction: load(sessionId) / append(sessionId, message)
    ├── ChatMessage.java                    # One stored turn: role (USER/ASSISTANT), content, timestamp
    └── RedisChatMemoryStore.java           # Redis LIST-backed implementation: push, trim to max-turns, refresh TTL
```

## Examples in this module

### com.acme.retail.ch13.Ch13Application

The `@SpringBootApplication` entry point. Booting it starts a normal Spring MVC web app, Spring AI's OpenAI auto-configuration, and the Redis auto-configuration that `ChatMemoryConfig` depends on. No custom startup logic beyond `SpringApplication.run(...)`.

### com.acme.retail.ch13.chat.ChatMemoryStore

The interface the rest of the code depends on, deliberately kept ignorant of *where* history lives. It declares exactly two operations: `load(sessionId)` returns a session's stored turns oldest-first (or an empty list for an unseen or expired session), and `append(sessionId, message)` adds one turn, with the cap on history length left entirely to the implementation.

```java
public interface ChatMemoryStore {

    List<ChatMessage> load(String sessionId);

    void append(String sessionId, ChatMessage message);
}
```

### com.acme.retail.ch13.chat.ChatMessage

A record for one stored turn: `role` (`USER` or `ASSISTANT`), `content`, and `timestamp`. The system prompt is never stored here — it is added fresh, in code, on every call, so changing the assistant's instructions never requires touching saved sessions.

### com.acme.retail.ch13.chat.RedisChatMemoryStore

The real backing implementation `ChatMemoryStore` is wired to. Each session's turns live in a Redis LIST at key `chat:memory:<sessionId>`, one JSON-encoded `ChatMessage` per element, oldest first. `append` does three things every time: right-pushes the new turn, trims the list down to `chat.memory.max-turns` entries (dropping the oldest), and refreshes the key's TTL so an abandoned session cleans itself up. This is the concrete mapping from a conversation id to stored messages:

```java
private String keyFor(String sessionId) {
    return KEY_PREFIX + sessionId;   // "chat:memory:" + sessionId
}

public void append(String sessionId, ChatMessage message) {
    String key = keyFor(sessionId);
    redisTemplate.opsForList().rightPush(key, writeMessage(message));
    redisTemplate.opsForList().trim(key, -properties.getMaxTurns(), -1);
    redisTemplate.expire(key, properties.getTtl().toSeconds(), TimeUnit.SECONDS);
}
```

### com.acme.retail.ch13.chat.ChatMemoryProperties

Binds `chat.memory.*` from `application.yml` into two settings: `maxTurns` (default `12`) — how many combined user/assistant turns survive per session, and directly how many of them get resent, in full, on every future call — and `ttl` (default `30m`) — how long an idle session's history survives in Redis before expiring, reset on every new turn.

### com.acme.retail.ch13.chat.ChatMemoryConfig

The `@Configuration` class that wires the single `ChatMemoryStore` bean. It combines the auto-configured `StringRedisTemplate` (from `spring-boot-starter-data-redis`) and `ObjectMapper` (from `spring-boot-starter-web`) with `ChatMemoryProperties` to construct a `RedisChatMemoryStore`. This is the only place in the module that decides Redis is the backing store — everything else depends only on the `ChatMemoryStore` interface.

```java
@Bean
public ChatMemoryStore chatMemoryStore(StringRedisTemplate redisTemplate,
                                        ObjectMapper objectMapper,
                                        ChatMemoryProperties properties) {
    return new RedisChatMemoryStore(redisTemplate, objectMapper, properties);
}
```

### com.acme.retail.ch13.assistant.ShoppingAssistantService

Where the chapter's fix actually lives. `reply(sessionId, customerMessage)` loads this session's prior turns from `ChatMemoryStore`, rebuilds the full prompt as system message + every stored turn (converted back into Spring AI `UserMessage`/`AssistantMessage` objects) + the newest customer message, then calls the `ChatClient`. Only after getting a reply does it append both the new user turn and the new assistant turn back into the store — the store never sees a turn until the call it belongs to has completed.

```java
public String reply(String sessionId, String customerMessage) {
    List<ChatMessage> history = memoryStore.load(sessionId);

    List<Message> promptMessages = new ArrayList<>();
    promptMessages.add(new SystemMessage(SYSTEM_PROMPT));
    for (ChatMessage turn : history) {
        promptMessages.add(toSpringAiMessage(turn));
    }
    promptMessages.add(new UserMessage(customerMessage));

    String reply = chatClient.prompt().messages(promptMessages).call().content();

    memoryStore.append(sessionId, new ChatMessage(Role.USER, customerMessage, Instant.now()));
    memoryStore.append(sessionId, new ChatMessage(Role.ASSISTANT, reply, Instant.now()));
    return reply;
}
```

### com.acme.retail.ch13.assistant.AssistantController

The REST entry point: `POST /api/assistant/sessions/{sessionId}/messages`. The `sessionId` is supplied by the caller (the chat widget) rather than generated server-side — it is the correlation key `ShoppingAssistantService` and `RedisChatMemoryStore` both use to find the right history, so the client is responsible for reusing it across a conversation.

### com.acme.retail.ch13.assistant.ChatRequest

The incoming request body record for a message — a single `message` field holding the customer's newest text.

### com.acme.retail.ch13.assistant.ChatReply

The response body record: `sessionId` (echoed back) and `reply` (the assistant's text).

## API Endpoints

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/api/assistant/sessions/{sessionId}/messages` | `ChatRequest` — `{ "message": "<customer text>" }` | `ChatReply` — `{ "sessionId": "...", "reply": "..." }` |

Example, two turns sharing the same `sessionId` path variable:

```bash
curl -X POST http://localhost:8080/api/assistant/sessions/session-42/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "I want black running shoes."}'

curl -X POST http://localhost:8080/api/assistant/sessions/session-42/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "Only size 8."}'
```

## Tests

- `ShoppingAssistantServiceTest` — mocks both `ChatModel` and `ChatMemoryStore`, so it never makes a real network call and never needs an API key or Redis. It captures the `Prompt` actually sent to the model and asserts that stored history turns are injected ahead of the new user message (system + 2 history turns + new user turn = 4 messages), and separately that a brand-new session sends only the system message and the new user turn.
- `RedisChatMemoryStoreTest` — uses a **real Redis** instance via Testcontainers (`redis:7-alpine`, `@Testcontainers(disabledWithoutDocker = true)`), not an in-memory fake, because this test exists specifically to verify real Redis LIST semantics. It verifies turns come back out in append order, that history is capped at the configured `max-turns` (oldest turns fall off first), and that an unknown session returns an empty list.

Run the tests:

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

  ai:
    openai:
      api-key: ${OPENAI_API_KEY:demo-key}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.3

chat:
  memory:
    max-turns: 12
    ttl: 30m
```

- `spring.data.redis.host` / `spring.data.redis.port` — where the app looks for Redis at startup; both are overridable via `REDIS_HOST`/`REDIS_PORT` env vars, and the app needs this connection reachable to serve any request, since `RedisChatMemoryStore` is the only `ChatMemoryStore` bean.
- `spring.ai.openai.api-key: ${OPENAI_API_KEY:demo-key}` — reads the real key from the environment with a placeholder fallback so the app and unit tests can start without one; only real calls to `/api/assistant/...` require a genuine key.
- `spring.ai.chat.options.model: gpt-4o-mini` — the OpenAI chat model the `ChatClient` calls.
- `spring.ai.chat.options.temperature: 0.3` — keeps replies fairly deterministic and on-topic rather than creatively varied, appropriate for a shopping assistant.
- `chat.memory.max-turns: 12` — the memory budget: how many user+assistant turns are kept and resent, in full, on every future call for a session; raising it raises token cost and latency on every request, not just the first.
- `chat.memory.ttl: 30m` — how long an idle session's history survives in Redis before being reclaimed; refreshed on every new turn so an active conversation never expires mid-chat.
