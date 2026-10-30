package com.acme.retail.ch20.incident;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * A snapshot of what is currently known about the incident -- the kind of
 * thing Sarah would read straight off a dashboard, or return from a
 * status page during the outage.
 *
 * @param circuitState   the assistant provider circuit breaker's current
 *                        state
 * @param mismatchCount  how many grounding mismatches have been recorded
 * @param circuitOpenCount how many calls were rejected while the circuit
 *                        was open
 * @param fallbackCount  total fallback responses served (mismatch +
 *                        circuit-open + provider-error)
 * @param summary        a short, human-readable diagnosis
 */
public record IncidentReport(CircuitBreaker.State circuitState,
                              double mismatchCount,
                              double circuitOpenCount,
                              double fallbackCount,
                              String summary) {
}
