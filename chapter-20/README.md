# Chapter 20 — Launch Day

*An alert fires, evidence comes from logs, traces, and metrics rather than guesses, a hypothesis is verified against a golden dataset instead of "looks fixed," and only then is the fix confirmed and the feature re-enabled — closing the loop until the next alert.*

Companion code for **Chapter 20** of *Java AI Engineer — Sarah's Journey at Acme Retail* — the capstone chapter, combining resilience, observability, and evaluation into one incident-response story.

## What this module demonstrates

On launch day, Acme Retail's AI shopping assistant starts confidently telling customers a sold-out product is in stock — a normal HTTP 200, no exception, nothing an ordinary health check would catch. This module wires together everything the book built one chapter at a time into a single incident-response loop: a Resilience4j circuit breaker (Chapter 16) trips not just on exceptions but on answers that contradict real inventory, a `FeatureFlagService` gives Sarah a coarser, human-operated kill switch, a `CorrelationIdFilter` (Chapter 17) ties every log line for one request together, and a `GoldenDatasetEvaluator` (Chapter 19) grades a fix objectively before `IncidentResponseService` is allowed to re-enable the assistant. `SimulatedModelProvider` is a deliberately deterministic fake `ModelProvider` — there is no real Spring AI `ChatClient` or external AI provider call anywhere in this module — so that the grounding-mismatch incident at the heart of the story (and its fix) reproduces exactly, on demand, every time, both for the narrative and for the test suite. The whole point of the chapter is the order operations happen in: diagnose with evidence, verify with evaluation, only then recover — never the reverse.

## Prerequisites

- Java 21
- Maven 3.9+ (or the wrapper, if added to this module)
- PostgreSQL reachable at the URL in `application.yml` (`jdbc:postgresql://localhost:5432/acme_retail`) if you want `ProductRepository` to resolve real catalog rows; without it the app still starts, but `AssistantService.loadFact` falls back to `ProductFact.unknown(sku)` for any SKU it cannot find
- No AI provider API key of any kind — the only `ModelProvider` bean in this module is `SimulatedModelProvider`, an in-process deterministic fake. There is no Spring AI `ChatClient`, no HTTP call to any model, and no network dependency for the "AI" part of this chapter.
- Docker (optionally, to use the included `docker-compose.yml` instead of running Postgres yourself)

## Run it

A `docker-compose.yml` is included in this module — it starts a PostgreSQL 16 container pre-configured with the exact database/user/password (`acme_retail` / `acme` / `acme`) this module's `application.yml` already expects, so `ProductRepository` can resolve real catalog rows instead of falling back to `ProductFact.unknown(sku)`:

```bash
cd volume1_2/code/chapter-20
docker compose up -d
mvn spring-boot:run
```

The application starts on port `8095`. Ask the assistant about a product:

```bash
curl -s -X POST http://localhost:8095/api/assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-1","sku":"SKU-1001","question":"Is this in stock?"}'
```

Check the feature flag and circuit breaker state at a glance — this is the first thing Sarah looks at during the incident, before reading a single log line:

```bash
curl -s http://localhost:8095/api/assistant/status
```

There is no HTTP endpoint that triggers the launch-day bug — `SimulatedModelProvider.degrade()` and `.repair()` are plain Java methods on the bean, not exposed through the controller. To reproduce the incident from a running application you would need to reach the bean directly (for example, from an `@SpringBootTest`, a debugger, or a small admin hook you add yourself); the shipped `IncidentResponseServiceTest.fullIncidentLifecycle_fromBadResponseToVerifiedRecovery` test does exactly this end to end without starting a servlet container at all — call `degrade()`, drive four requests through `AssistantService.ask(...)` to trip the breaker, call `diagnose()`, disable the feature flag, call `repair()`, then call `recover(goldenDataset, passRateGate)` to verify and re-enable. That test is the most direct way to see the whole loop run.

## Project structure

```
src/main/java/com/acme/retail/ch20/
├── Chapter20Application.java              # Spring Boot entry point
├── assistant/
│   ├── AssistantAnswer.java                # Model's answer + a mismatched flag the service (not the model) computes
│   ├── AssistantController.java            # POST /api/assistant/ask, GET /api/assistant/status
│   ├── AssistantMetrics.java                # Micrometer counters (assistant.calls, tagged by outcome) read by dashboards and diagnose()
│   ├── AssistantRequest.java                # Inbound record: customerId, sku, question
│   ├── AssistantResponse.java               # Outbound record: correlationId, answer, grounded, outcome
│   ├── AssistantService.java                # Grounds, calls the provider through the circuit breaker, logs, meters, falls back
│   ├── ModelProvider.java                   # The seam between the app and any model implementation
│   ├── ProductFact.java                     # Real inventory facts the model is grounded in and graded against
│   └── SimulatedModelProvider.java          # Deterministic fake ModelProvider; degrade()/repair() reproduce the launch-day bug on demand
├── evaluation/
│   ├── EvaluationResult.java                # total/passed/passRate + meetsGate(threshold)
│   ├── GoldenCase.java                      # One golden question/expected-answer row
│   └── GoldenDatasetEvaluator.java          # Grades the live ModelProvider against a fixed golden dataset
├── incident/
│   ├── IncidentReport.java                  # Circuit state + mismatch/fallback counts + a human-readable summary
│   └── IncidentResponseService.java        # diagnose() -> verifyFix() -> recover(): the incident-response loop as code
├── observability/
│   └── CorrelationIdFilter.java            # Assigns/reuses a correlation ID per request via SLF4J MDC
├── product/
│   ├── Product.java                        # JPA entity: the products table, the ground truth for stock
│   └── ProductRepository.java              # Spring Data JPA repository; findBySku is the only source of truth
└── resilience/
    ├── FeatureFlagService.java             # Manual, human-operated kill switch for the whole assistant feature
    └── ResilienceConfig.java               # Circuit breaker config -- trips on exceptions AND on grounding mismatches
```

## Examples in this module

### com.acme.retail.ch20.Chapter20Application

Standard `@SpringBootApplication` entry point. It exists purely so this chapter's module boots as a self-contained, independently runnable Spring Boot application; it does not introduce any chapter-specific logic itself.

### com.acme.retail.ch20.assistant.AssistantAnswer

A record of what the model provider said (`text`, `claimsInStock`, `providerId`) plus a `mismatched` flag. The Javadoc is explicit that `mismatched` is set by `AssistantService`, never by `ModelProvider` itself — the provider is never trusted to grade its own answer. `withMismatch(boolean)` returns a new immutable copy rather than mutating in place, so the call site reads as "the provider produced this, the service decided whether it disagrees with reality."

### com.acme.retail.ch20.assistant.AssistantController

The public REST entry point. `POST /ask` delegates straight to `AssistantService`. `GET /status` is what Sarah checks first during the incident, before reading any log line — it reports the feature flag state and the circuit breaker's current state (`CLOSED`/`OPEN`/`HALF_OPEN`) in one call:

```java
@GetMapping("/status")
public AssistantStatus status() {
    String state = circuitBreakerRegistry
            .circuitBreaker(ResilienceConfig.ASSISTANT_PROVIDER_BREAKER)
            .getState()
            .name();
    return new AssistantStatus(featureFlags.isEnabled(), state);
}
```

### com.acme.retail.ch20.assistant.AssistantMetrics

Wraps Micrometer's `MeterRegistry` with the handful of counters this chapter's incident response actually reads: one `assistant.calls` counter tagged by `outcome` (`success`, `grounding_mismatch`, `circuit_open`, `provider_error`, `feature_disabled`). The same counters an on-call dashboard would alert on are the ones `IncidentResponseService.diagnose()` reads to build its report — the dashboard and the incident-response code look at identical numbers, not separate approximations of the truth.

### com.acme.retail.ch20.assistant.AssistantRequest

Inbound record for `POST /api/assistant/ask`: `customerId`, `sku`, `question`. The Javadoc notes `customerId` is deliberately kept out of plaintext logs, per Chapter 18's data-handling rules.

### com.acme.retail.ch20.assistant.AssistantResponse

Outbound record: `correlationId`, `answer`, `grounded`, `outcome`. Three static factories build every possible shape of response — `success(...)`, `fallback(correlationId, fact, outcome)` (which always constructs its answer text from the real `ProductFact`, never from the model), and `disabled(correlationId)`. `correlationId` is always populated, even on a fallback or disabled response, so a reported problem can always be traced back to its exact log lines.

### com.acme.retail.ch20.assistant.AssistantService

The class where every earlier chapter's lesson meets. `ask(request)` checks the feature flag first (short-circuiting before any provider call), loads the real `ProductFact` from the database, then calls the model provider *through* the circuit breaker via `executeSupplier`. The mismatch check happens inside `callAndCheck`, before the breaker's `recordResultPredicate` ever inspects the result:

```java
public AssistantResponse ask(AssistantRequest request) {
    String correlationId = currentCorrelationId();

    if (!featureFlags.isEnabled()) {
        metrics.increment("feature_disabled");
        return AssistantResponse.disabled(correlationId);
    }

    ProductFact fact = loadFact(request.sku());
    CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(
            ResilienceConfig.ASSISTANT_PROVIDER_BREAKER);

    try {
        AssistantAnswer answer = breaker.executeSupplier(
                () -> callAndCheck(request.question(), fact));

        if (answer.mismatched()) {
            metrics.increment("grounding_mismatch");
            return AssistantResponse.fallback(correlationId, fact, "fallback_mismatch");
        }
        metrics.increment("success");
        return AssistantResponse.success(correlationId, answer.text());

    } catch (CallNotPermittedException circuitOpen) {
        metrics.increment("circuit_open");
        return AssistantResponse.fallback(correlationId, fact, "fallback_circuit_open");
    } catch (Exception providerError) {
        metrics.increment("provider_error");
        return AssistantResponse.fallback(correlationId, fact, "fallback_provider_error");
    }
}
```

Every branch logs a structured line (`event=assistant_call outcome=... correlationId=...`) and increments a matching metric, so nothing that happens is silent. `currentCorrelationId()` reads from the MDC key `CorrelationIdFilter.MDC_KEY` populated by the servlet filter, falling back to a fresh random UUID for callers outside the servlet pipeline (such as direct unit tests).

### com.acme.retail.ch20.assistant.ModelProvider

A one-method interface (`AssistantAnswer ask(String question, ProductFact fact)`) that is the seam between the application and any AI model implementation. Kept small so tests and `GoldenDatasetEvaluator` can substitute any implementation without a real network call, and so the "bad provider response" driving this chapter's incident can be reproduced deterministically via `SimulatedModelProvider`.

### com.acme.retail.ch20.assistant.ProductFact

A record (`sku`, `name`, `inStock`, `stockQuantity`) of the facts about one product pulled straight from the `products` table — the same grounding idea as Chapter 11's RAG pattern, applied here as the source of truth a model's claim is checked against, never trusted from the model itself. `ProductFact.unknown(sku)` supplies a safe default (`inStock=false`) when a customer asks about a SKU that does not exist in the catalog.

### com.acme.retail.ch20.assistant.SimulatedModelProvider

The one class in this module standing in for a real, billed AI provider — the deliberate design choice that keeps this capstone runnable fully offline and its incident scenario perfectly reproducible. It holds a single `AtomicBoolean healthy` flag. When healthy, `ask(...)` builds its answer directly from the real `ProductFact` it was handed, so `claimsInStock` always agrees with `fact.inStock()`. When degraded (`healthy == false`), it ignores the fact entirely and always claims the product is in stock — that is the exact launch-day bug the chapter's story is built around, reproduced on demand rather than left to chance:

```java
@Override
public AssistantAnswer ask(String question, ProductFact fact) {
    if (healthy.get()) {
        boolean inStock = fact.inStock();
        String text = inStock
                ? "Yes, " + fact.name() + " is in stock -- we have " + fact.stockQuantity() + " units."
                : "I'm sorry, " + fact.name() + " is currently sold out.";
        return new AssistantAnswer(text, inStock, false, "acme-shopping-model-v2");
    }

    // The launch-day bug: the degraded provider ignores the real
    // inventory fact entirely and always claims the product is
    // available. This is what Sarah's investigation eventually finds.
    String text = "Yes, " + fact.name() + " is in stock and ready to ship today!";
    return new AssistantAnswer(text, true, false, "acme-shopping-model-v2");
}
```

`degrade()` flips the flag to simulate the bug shipping; `repair()` flips it back to simulate Sarah and Daniel's actual fix — kept as a named, deliberate action rather than a mystery, exactly as the Javadoc states. `isHealthy()` exposes the current state for tests and diagnostics. Note the `mismatched` field passed into `AssistantAnswer` here is always `false` — `SimulatedModelProvider` never grades its own output; that grading happens one layer up, in `AssistantService.callAndCheck`.

### com.acme.retail.ch20.evaluation.EvaluationResult

A record (`total`, `passed`, `passRate`, `failedCaseIds`) reporting how a golden dataset run went. `meetsGate(double passRateThreshold)` is the single method `IncidentResponseService` calls before it will trust a fix — the concrete expression of "verify with evaluation, not looks fixed."

### com.acme.retail.ch20.evaluation.GoldenCase

A record (`id`, `sku`, `question`, `expectedInStock`) — one row of a small, fixed golden dataset whose correct answer is known in advance, so a fix can be graded automatically instead of by eyeballing a handful of manual prompts (Chapter 19's idea, applied to this chapter's incident).

### com.acme.retail.ch20.evaluation.GoldenDatasetEvaluator

Grades a `ModelProvider` against a fixed list of `GoldenCase`s. It deliberately calls `ModelProvider` directly rather than going through `AssistantService`, so its result is never affected by the feature flag or the circuit breaker's current state — it answers exactly one question: does the model itself now give grounded answers?

```java
public EvaluationResult evaluate(List<GoldenCase> cases) {
    int passed = 0;
    List<String> failedCaseIds = new ArrayList<>();

    for (GoldenCase testCase : cases) {
        ProductFact fact = resolveFact(testCase);
        AssistantAnswer answer = modelProvider.ask(testCase.question(), fact);

        if (answer.claimsInStock() == testCase.expectedInStock()) {
            passed++;
        } else {
            failedCaseIds.add(testCase.id());
        }
    }

    int total = cases.size();
    double passRate = total == 0 ? 0.0 : (double) passed / total;
    return new EvaluationResult(total, passed, passRate, failedCaseIds);
}
```

`resolveFact` prefers a live database row for the case's SKU when one exists, falling back to a fact built from the case's own `expectedInStock` value — a golden case can describe a scenario (a specific sold-out SKU) that is not necessarily seeded in every environment the evaluator runs in.

### com.acme.retail.ch20.incident.IncidentReport

A record (`circuitState`, `mismatchCount`, `circuitOpenCount`, `fallbackCount`, `summary`) — the snapshot Sarah would read straight off a dashboard or a status page during the outage, produced entirely from circuit breaker state and metric counters that already existed for other reasons.

### com.acme.retail.ch20.incident.IncidentResponseService

Sarah's launch-day playbook, expressed as code — and the class that most literally is the chapter's mental model. It introduces no new idea of its own; every method composes Chapter 16 (circuit breaker), Chapter 17 (metrics as evidence), and Chapter 19 (golden-dataset evaluation as a release gate), in a fixed order that step 3 is never allowed to skip ahead of:

```java
public IncidentReport diagnose() {
    CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(
            ResilienceConfig.ASSISTANT_PROVIDER_BREAKER);
    CircuitBreaker.State state = breaker.getState();
    double mismatchCount = metrics.getCount("grounding_mismatch");
    double circuitOpenCount = metrics.getCount("circuit_open");
    double fallbackCount = mismatchCount + circuitOpenCount + metrics.getCount("provider_error");
    // ... builds a human-readable summary from the same numbers
    return new IncidentReport(state, mismatchCount, circuitOpenCount, fallbackCount, summary);
}

public EvaluationResult verifyFix(List<GoldenCase> goldenDataset) {
    return evaluator.evaluate(goldenDataset);
}

public boolean recover(List<GoldenCase> goldenDataset, double passRateGate) {
    EvaluationResult result = verifyFix(goldenDataset);
    if (result.meetsGate(passRateGate)) {
        circuitBreakerRegistry.circuitBreaker(ResilienceConfig.ASSISTANT_PROVIDER_BREAKER)
                .transitionToClosedState();
        featureFlags.enable();
        return true;
    }
    return false; // leave the assistant disabled and say why, in the log line
}
```

`diagnose()` is step 1: gather evidence from the same signals a dashboard shows — no guessing at a cause. `verifyFix()` / `recover()` is steps 2 and 3 fused: the feature flag is only re-enabled and the breaker only closed if `EvaluationResult.meetsGate(passRateGate)` says the fix actually works, never because a fix merely "looks right."

### com.acme.retail.ch20.observability.CorrelationIdFilter

A `GenericFilterBean` run on every incoming request (Chapter 17's pattern). It reads `X-Correlation-Id` from the request if an upstream caller already supplied one, otherwise generates a new UUID; either way it stores the value under a fixed MDC key (`MDC_KEY = "correlationId"`) so every log line for the rest of the request automatically carries it, echoes the same value back as a response header, and removes the MDC entry in a `finally` block so a reused servlet container thread never leaks one request's correlation ID into the next.

### com.acme.retail.ch20.product.Product

A JPA `@Entity` mapped to the `products` table: `id`, `name`, `description`, `priceCents`, `sku`, `stockQuantity`, `category`. `isInStock()` returns true only when `stockQuantity` is non-null and greater than zero. This table's `stockQuantity` column is the ground truth every AI answer in this module is checked against — when the model claims "in stock" but this row disagrees, that disagreement is the incident.

### com.acme.retail.ch20.product.ProductRepository

A `JpaRepository<Product, Long>` with one query method, `findBySku(String sku)`. It is the only place in the module that "knows about" the products table, and it is what `AssistantService` and `GoldenDatasetEvaluator` both call before trusting or grading any answer.

### com.acme.retail.ch20.resilience.FeatureFlagService

A manual, human-operated kill switch for the entire AI assistant feature, backed by an `AtomicBoolean`. It is the fastest fallback available during an incident: turn the whole feature off before you even know the root cause. `disable(String reason)` requires a reason and logs it (`event=feature_flag flag=ai_assistant outcome=disabled reason=...`); `enable()` re-opens it. `IncidentResponseService.recover(...)` is the only caller of `enable()` in this module's incident-response path, and only after the evaluation gate passes.

```java
public void disable(String reason) {
    enabled.set(false);
    log.warn("event=feature_flag flag=ai_assistant outcome=disabled reason={}", reason);
}

public void enable() {
    enabled.set(true);
    log.info("event=feature_flag flag=ai_assistant outcome=enabled");
}
```

### com.acme.retail.ch20.resilience.ResilienceConfig

Configures the Resilience4j `CircuitBreakerRegistry` that protects every call to the model provider. Beyond Chapter 16's baseline pattern, this config's `recordResultPredicate` also trips the breaker on a *successful* call whose `AssistantAnswer` is `mismatched()` — a normal 200 response that is nonetheless wrong, exactly the "quiet failure" Chapter 17 warned about that a breaker configured only for thrown exceptions would never notice:

```java
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(4)
        .minimumNumberOfCalls(4)
        .failureRateThreshold(50.0f)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .permittedNumberOfCallsInHalfOpenState(2)
        .recordExceptions(Exception.class)
        .recordResult(result ->
                result instanceof AssistantAnswer answer && answer.mismatched())
        .build();
```

`ASSISTANT_PROVIDER_BREAKER` is a shared public constant name so the service, the controller, and the incident-response code all agree on which breaker they mean. `defaultRegistry()` is a static factory that builds the identical registry without a Spring context, used directly by all three test classes.

## API Endpoints

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `/api/assistant/ask` | JSON body: `{ "customerId": string, "sku": string, "question": string }` | JSON `AssistantResponse`: `{ "correlationId": string, "answer": string, "grounded": boolean, "outcome": string }` |
| GET | `/api/assistant/status` | none | JSON `AssistantStatus`: `{ "featureEnabled": boolean, "circuitBreakerState": string }` |

```bash
curl -s -X POST http://localhost:8095/api/assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-1","sku":"SKU-1001","question":"Is this in stock?"}'

curl -s http://localhost:8095/api/assistant/status
```

Actuator also exposes circuit breaker state directly (enabled in `application.yml`), useful for the same "check evidence, don't guess" habit outside of this module's own `/status` endpoint:

```bash
curl -s http://localhost:8095/actuator/health
curl -s http://localhost:8095/actuator/circuitbreakers
curl -s http://localhost:8095/actuator/circuitbreakerevents
```

## Tests

- `AssistantServiceTest` — unit tests (Mockito-mocked `ProductRepository`, real `CircuitBreakerRegistry` and `AssistantMetrics`) covering: a healthy provider with an in-stock product returns `success`; a degraded provider on a sold-out product returns grounded fallbacks and trips the breaker to `OPEN` after four mismatched calls (matching the sliding-window size of 4 and 50% failure threshold), after which further calls short-circuit with `fallback_circuit_open` without ever reaching the provider; a disabled feature flag short-circuits before the repository is even touched (`verifyNoInteractions`); an unknown SKU still gets a safe grounded fallback when the degraded provider's "in stock" claim disagrees with `ProductFact.unknown()`'s `inStock=false`; and repairing the provider plus manually closing the breaker returns the assistant to `success` responses.
- `GoldenDatasetEvaluatorTest` — unit tests against a four-case golden dataset mixing in-stock and sold-out products. A healthy provider passes all four cases (pass rate `1.0`, gate of `0.9` met); a degraded provider (always claims "in stock") passes only the two already-in-stock cases and fails the two sold-out ones by name (`case-2`, `case-4`), landing at a `0.5` pass rate that fails the `0.9` gate; repairing the provider restores a `1.0` pass rate and clears the gate again.
- `IncidentResponseServiceTest` — the launch-day incident end to end. `fullIncidentLifecycle_fromBadResponseToVerifiedRecovery` degrades the provider for a sold-out SKU, drives four requests to confirm the customer always sees a grounded "sold out" answer (never the model's wrong claim) and that the breaker trips `OPEN`; calls `diagnose()` and asserts the resulting `IncidentReport` reflects the `OPEN` state and a nonzero mismatch count with a summary mentioning "grounding mismatch"; disables the feature flag; repairs the provider; then calls `recover(goldenDataset, 0.9)` and asserts it returns `true`, the feature flag is re-enabled, the breaker is `CLOSED` again, and the very next request now returns `success` with the correct "sold out" answer. A second test, `recover_refusesToReEnable_whenEvaluationStillFails`, degrades the provider and never repairs it, then asserts `recover(...)` returns `false` and the feature flag stays disabled — proving the gate actually blocks a bad fix rather than only demonstrating the happy path.

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

- `server.port: 8095` — the HTTP port the controller and Actuator endpoints listen on.
- `spring.datasource.url: jdbc:postgresql://localhost:5432/acme_retail` (with `username`/`password: acme`) — local development connection details; the file's own header comment notes that in production these come from environment variables or a secrets manager, not from a file committed to source control.
- `spring.jpa.hibernate.ddl-auto: update` — lets Hibernate keep the `products` table schema in sync automatically for local development; not a production migration strategy.
- `management.endpoints.web.exposure.include: health, metrics, circuitbreakers, circuitbreakerevents` — exposes the circuit breaker's state and its event history over Actuator, which is exactly the evidence `IncidentResponseService.diagnose()` and a human on-call would both want visible from outside the JVM.
- `management.health.circuitbreakers.enabled: true` — folds the assistant provider breaker's health into `/actuator/health`, so an unhealthy (open) breaker is visible in the standard health check, not only in a breaker-specific endpoint.
- `logging.pattern.console` includes `[%X{correlationId}]` — pulls the correlation ID that `CorrelationIdFilter` puts into SLF4J's MDC into every console log line automatically, without any individual log statement having to pass it explicitly.
