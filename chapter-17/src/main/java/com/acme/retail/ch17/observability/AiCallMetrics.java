package com.acme.retail.ch17.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Records AI-specific metrics with Micrometer, Spring Boot's metrics
 * facade. Metrics answer a different question than logs do: logs tell you
 * what happened on one request; metrics tell you what is happening across
 * thousands of requests (is latency trending up? are errors climbing?).
 *
 * <p>The "outcome" and "type" tags let a dashboard break latency down by
 * success/error, and token usage down by prompt/completion, without needing
 * a new metric name for every combination.
 */
@Component
public class AiCallMetrics {

    private final MeterRegistry registry;

    public AiCallMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

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
}
