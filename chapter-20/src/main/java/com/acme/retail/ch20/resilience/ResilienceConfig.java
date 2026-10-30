package com.acme.retail.ch20.resilience;

import com.acme.retail.ch20.assistant.AssistantAnswer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures the circuit breaker that protects every call to the AI
 * shopping assistant's model provider.
 *
 * <p>This is Chapter 16's pattern, with one addition this chapter needs:
 * the breaker does not only trip on thrown exceptions (a timeout, a 500
 * response). It also trips on {@link AssistantAnswer#mismatched()} --
 * an answer that came back as a normal, successful call but that
 * contradicts Acme Retail's real inventory. That is exactly the kind of
 * "quiet" failure Chapter 17 warned about: HTTP 200, no exception, still
 * wrong. Without {@code recordResultPredicate}, a circuit breaker would
 * never notice this class of failure at all.
 */
@Configuration
public class ResilienceConfig {

    /**
     * The name this chapter's circuit breaker is registered under. Shared
     * as a constant so the service, the controller, and the incident
     * response code all agree on it.
     */
    public static final String ASSISTANT_PROVIDER_BREAKER = "assistantProvider";

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return defaultRegistry();
    }

    /**
     * Builds the same registry the Spring bean uses, but callable directly
     * from a plain JUnit test with no Spring context -- the same style
     * chapter-01's tests use for {@code ProductService}.
     */
    public static CircuitBreakerRegistry defaultRegistry() {
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

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        // Touch the breaker once so it exists in the registry immediately,
        // rather than only being created lazily on first use.
        registry.circuitBreaker(ASSISTANT_PROVIDER_BREAKER);
        return registry;
    }
}
