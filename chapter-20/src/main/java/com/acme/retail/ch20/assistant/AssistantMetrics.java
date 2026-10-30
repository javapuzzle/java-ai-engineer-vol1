package com.acme.retail.ch20.assistant;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Wraps the Micrometer {@link MeterRegistry} with the handful of counters
 * this chapter's incident response actually reads.
 *
 * <p>Chapter 17 introduced metrics as one of the three observability
 * signals (alongside logs and traces). Here, {@code grounding_mismatch}
 * and {@code circuit_open} are the counters an on-call dashboard would
 * alert on, and {@code IncidentResponseService.diagnose()} reads the same
 * counters to build its report -- the dashboard and the incident-response
 * code look at the same numbers.
 */
@Component
public class AssistantMetrics {

    private static final String METRIC_NAME = "assistant.calls";
    private static final String OUTCOME_TAG = "outcome";

    private final MeterRegistry registry;

    public AssistantMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Records one call outcome, e.g. {@code "success"}, {@code
     * "grounding_mismatch"}, {@code "circuit_open"}, {@code
     * "provider_error"}, or {@code "feature_disabled"}.
     */
    public void increment(String outcome) {
        Counter.builder(METRIC_NAME)
                .tag(OUTCOME_TAG, outcome)
                .description("AI shopping assistant calls, tagged by outcome")
                .register(registry)
                .increment();
    }

    /**
     * The number of calls recorded for a given outcome so far. Used by
     * tests and by {@code IncidentResponseService.diagnose()}.
     */
    public double getCount(String outcome) {
        Counter counter = registry.find(METRIC_NAME).tag(OUTCOME_TAG, outcome).counter();
        return counter == null ? 0.0 : counter.count();
    }
}
