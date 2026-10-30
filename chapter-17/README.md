# Chapter 17 — Something Is Wrong, But Nobody Knows What

*One correlation ID, assigned at the front door of a request, ties that request's logs, metrics, and trace back together on demand.*

Companion code for **Chapter 17** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

A customer reports a wrong answer from the shopping assistant, the response was a 200 OK, and there is nothing to look at afterwards — no request-level logging, no metrics, no way to reconstruct what the AI actually said. This module fixes that without touching the AI's behavior itself: a `CorrelationIdFilter` assigns (or reuses) one correlation ID per incoming HTTP request and puts it in SLF4J's MDC so every log line for that request carries it automatically; `AiCallLogger` wraps every call to the AI client and writes structured JSON log lines (prompt, response, latency, token counts) around it; and `AiCallMetrics` records the same call's latency and token usage in Micrometer, tagged only with low-cardinality values (`outcome`, `type`) — never the correlation ID itself, which would blow up the metric's cardinality. Together, the same correlation ID that shows up in the response header lets Sarah or Alex pull the matching log line and the aggregate metrics for one specific customer's request back into one story.

## Prerequisites

- Java 21
- Maven 3.9+ (or the wrapper, if added to this module)
- No external AI provider or API key — `AcmeAiClient` is an in-process stub that simulates latency and reproduces Chapter 11's "confidently wrong" stock answer, so the chapter's story stays about observability, not about model correctness
- `spring-boot-starter-actuator` and `micrometer-registry-prometheus` (already declared in `pom.xml`) for the `/actuator/metrics` and `/actuator/prometheus` endpoints
- `logstash-logback-encoder` (already declared in `pom.xml`) for structured JSON console logging

## Run it

```bash
cd volume1_2/code/chapter-17
mvn spring-boot:run
```

The application starts on port `8090`. Call the assistant endpoint with an explicit correlation ID (simulating a support agent re-playing a customer's ticket number):

```bash
curl -i -X POST http://localhost:8090/api/assistant/messages \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: ticket-4821" \
  -d '{"customerId":"cust-1","message":"Is this in stock?"}'
```

The response echoes the same value back in an `X-Correlation-Id` response header, and the response body's `correlationId` field carries it too. If the caller does not send the header, `CorrelationIdFilter` generates a random UUID and returns that instead — either way, the ID in the response is the one to hand to support to look up this exact request.

While the call is in flight, watch the console for a single JSON log line tagged with `correlationId=ticket-4821`, `aiModel`, `aiLatencyMs`, `aiPromptTokens`, and `aiCompletionTokens`. To see the aggregate metrics instead of one request, query Actuator:

```bash
curl http://localhost:8090/actuator/metrics/acme.ai.call.latency
curl http://localhost:8090/actuator/metrics/acme.ai.call.tokens
curl http://localhost:8090/actuator/prometheus | grep acme_ai_call
```

## Project structure

```
src/main/java/com/acme/retail/ch17/
├── Chapter17Application.java             # Spring Boot entry point
├── assistant/
│   ├── AcmeAiClient.java                  # Stub AI client (simulated latency + Chapter 11-style wrong answer)
│   ├── AiResponse.java                    # AI call result: content, model, prompt/completion token counts
│   ├── ChatController.java                # POST /api/assistant/messages -- delegates observability to AiCallLogger
│   ├── ChatRequest.java                   # Inbound request record: customerId, message
│   └── ChatResponse.java                  # Outbound response record: correlationId, reply
├── correlation/
│   └── CorrelationIdFilter.java           # Assigns/reuses a correlation ID; puts it in MDC and a response header
└── observability/
    ├── AiCallLogger.java                  # Times every AI call and writes structured success/failure log lines
    └── AiCallMetrics.java                 # Records AI call latency and token counts in Micrometer
```

## Examples in this module

### com.acme.retail.ch17.Chapter17Application

Standard `@SpringBootApplication` entry point. No chapter-specific logic; it exists so the module boots as a self-contained Spring Boot application, independent of every other chapter's build.

### com.acme.retail.ch17.correlation.CorrelationIdFilter

A `OncePerRequestFilter` that runs on every incoming request. If the caller already sent an `X-Correlation-Id` header — for example, a support tool re-playing a customer's ticket number — that value is reused; otherwise a new random UUID is generated. The ID is stored under a fixed MDC key so every log line written anywhere while handling this request automatically includes it, and it is echoed back as a response header so the client can quote it back later. The MDC entry is removed in a `finally` block so a reused thread from the servlet container's thread pool can never leak one request's correlation ID into the next, unrelated request:

```java
String correlationId = request.getHeader(HEADER_NAME);
if (correlationId == null || correlationId.isBlank()) {
    correlationId = UUID.randomUUID().toString();
}

MDC.put(MDC_KEY, correlationId);
response.setHeader(HEADER_NAME, correlationId);

try {
    filterChain.doFilter(request, response);
} finally {
    MDC.remove(MDC_KEY);
}
```

`HEADER_NAME` (`X-Correlation-Id`) and `MDC_KEY` (`correlationId`) are public constants so `ChatController` and the test suite can reference the same values instead of duplicating string literals.

### com.acme.retail.ch17.observability.AiCallLogger

Wraps every call to the AI client with timing, structured logging, and metrics recording, so any controller that calls it gets identical observability without repeating the plumbing. It logs the prompt, the response, the latency, and the token usage as structured MDC fields — not as a free-text sentence — which is what makes them individually searchable and filterable in a log platform. Because `CorrelationIdFilter` already put the correlation ID into the MDC before this code runs, the resulting log line carries that same correlation ID automatically:

```java
public AiResponse call(String prompt, Function<String, AiResponse> aiCall) {
    Instant start = Instant.now();
    try {
        AiResponse response = aiCall.apply(prompt);
        Duration latency = Duration.between(start, Instant.now());

        logSuccess(prompt, response, latency);
        metrics.recordLatency("success", latency);
        metrics.recordTokens("prompt", response.promptTokens());
        metrics.recordTokens("completion", response.completionTokens());

        return response;
    } catch (RuntimeException ex) {
        Duration latency = Duration.between(start, Instant.now());
        logFailure(prompt, ex, latency);
        metrics.recordLatency("error", latency);
        throw ex;
    }
}
```

On success it adds `aiModel`, `aiLatencyMs`, `aiPromptTokens`, and `aiCompletionTokens` to the MDC for the duration of one log statement, then removes them again so they don't leak into unrelated log lines later in the same request; on failure it logs `aiLatencyMs` and the exception message instead. A private `sanitize` helper strips quotes and newlines from the prompt/response text so a multi-line or quote-containing AI response cannot break the log line or the JSON encoder.

### com.acme.retail.ch17.observability.AiCallMetrics

Records AI-specific metrics with Micrometer. Logs answer "what happened on this one request"; metrics answer "what is happening across thousands of requests" (is latency trending up, are errors climbing). The `outcome` and `type` tags are deliberately small, fixed-value enumerations (`success`/`error`, `prompt`/`completion`) rather than anything request-specific — critically, the correlation ID is never used as a tag, because Micrometer creates one time series per unique tag combination, and tagging by a value that is different on every single request (a raw correlation ID, a customer ID, a raw prompt) would create unbounded cardinality and can crash or bankrupt a metrics backend:

```java
public void recordLatency(String outcome, Duration latency) {
    Timer.builder("acme.ai.call.latency")
            .description("Latency of calls to the Acme AI assistant")
            .tag("outcome", outcome)
            .register(registry)
            .record(latency);
}

public void recordTokens(String tokenType, int count) {
    registry.counter("acme.ai.call.tokens", "type", tokenType).increment(count);
}
```

If a request needs to be traced individually rather than aggregated, that is what the correlation ID in the logs (and, in a full trace setup, a distributed trace) is for — metrics stay aggregate by design.

### com.acme.retail.ch17.assistant.AcmeAiClient

Stands in for the real call to Acme Retail's AI provider (see Chapter 10). This chapter is not about making the AI more correct — that is Chapter 11's problem (grounding and RAG) — so this client still reproduces Chapter 11's confidently wrong "yes, that's in stock" answer for stock/availability questions, to keep the underlying story visible while the chapter's actual subject (observability) is layered on top. It simulates network latency with a random `Thread.sleep` and estimates token counts from word counts rather than calling a real tokenizer.

### com.acme.retail.ch17.assistant.AiResponse

Record of what the AI actually returned for one call: `content`, `model`, `promptTokens`, `completionTokens`, plus a `totalTokens()` convenience method. Tokens are the small chunks of text a language model reads and writes; providers bill and rate-limit by token count, so tracking them here is part of observability, not just cost reporting.

### com.acme.retail.ch17.assistant.ChatController

The endpoint the storefront calls to talk to the shopping assistant. Notice what it deliberately does *not* do: it never logs the prompt or response itself and never touches Micrometer directly — that responsibility belongs entirely to `AiCallLogger`, so every AI call in the application, from any controller, is observed the same way:

```java
@PostMapping("/messages")
public ChatResponse handleMessage(@RequestBody ChatRequest request) {
    AiResponse aiResponse = aiCallLogger.call(request.message(), aiClient::ask);
    String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
    return new ChatResponse(correlationId, aiResponse.content());
}
```

It reads the current request's correlation ID straight out of the MDC (populated earlier by `CorrelationIdFilter`) and includes it in the response body, in addition to the value `CorrelationIdFilter` already set as a response header.

### com.acme.retail.ch17.assistant.ChatRequest

Inbound request record: `customerId` and `message` — the text a customer sends to the shopping assistant.

### com.acme.retail.ch17.assistant.ChatResponse

Outbound response record: `correlationId` and `reply`. Always carries the correlation ID used for this request, so if the customer complains again, support can jump straight to the matching logs, metrics, and trace.

## API Endpoints

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `/api/assistant/messages` | JSON body: `{ "customerId": string, "message": string }`, optional `X-Correlation-Id` header | JSON `ChatResponse`: `{ "correlationId": string, "reply": string }`, plus an `X-Correlation-Id` response header |

```bash
curl -i -X POST http://localhost:8090/api/assistant/messages \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: ticket-4821" \
  -d '{"customerId":"cust-1","message":"Is this in stock?"}'
```

## Tests

- `ChatControllerTest` — full-stack (`@SpringBootTest`, random port) tests that a caller-supplied `X-Correlation-Id` is echoed back on both the response header and the `ChatResponse.correlationId` body field, and that a caller who sends no header still gets a non-blank generated correlation ID back.
- `CorrelationIdFilterTest` — unit tests against the filter directly: generates an ID when the caller sends none, reuses the caller's ID verbatim when one is sent, and — critically — asserts the MDC entry is `null` after the filter chain completes, proving the cleanup in the `finally` block prevents correlation IDs from leaking across requests on a reused thread.
- `AiCallLoggerTest` — using a real `SimpleMeterRegistry`, verifies that a successful call records exactly one `acme.ai.call.latency` timer count tagged `outcome=success` and the correct `acme.ai.call.tokens` counts tagged `type=prompt` and `type=completion`, and that a call whose `aiCall` function throws records one timer count tagged `outcome=error` instead (and still propagates the exception).

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

- `server.port: 8090` — the HTTP port the controller listens on.
- `management.endpoints.web.exposure.include: health,prometheus,metrics` — exposes the Actuator `/actuator/health`, `/actuator/prometheus`, and `/actuator/metrics` endpoints; without this, Actuator's default configuration only exposes `/actuator/health`, and the metrics this chapter records would be invisible from outside the JVM.
- `management.metrics.tags.application: ${spring.application.name}` — stamps every Micrometer metric emitted by this application with a fixed `application` tag, so metrics from this module can be told apart from other services in a shared dashboard without adding per-request cardinality.
- `logging.level.com.acme.retail.ch17: INFO` — keeps this module's own log lines (including `AiCallLogger`'s `ai_call_completed` / `ai_call_failed` lines) visible at the default level.

From `src/main/resources/logback-spring.xml`:

- The `STDOUT` appender uses `net.logstash.logback.encoder.LogstashEncoder` so every log line is written as one JSON object instead of a free-text sentence.
- `includeMdcKeyName` is declared for `correlationId`, `aiModel`, `aiLatencyMs`, `aiPromptTokens`, and `aiCompletionTokens` — this is what promotes those MDC fields set by `CorrelationIdFilter` and `AiCallLogger` into top-level, individually searchable/filterable JSON fields in a log platform such as Splunk, Datadog, or the ELK stack, rather than leaving them buried in an unstructured message string.
