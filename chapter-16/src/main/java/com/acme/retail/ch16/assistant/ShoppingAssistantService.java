package com.acme.retail.ch16.assistant;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Wraps the call to the AI provider with the resilience patterns
 * production traffic needs.
 *
 * <p>During Sarah's incident, one slow AI provider turned into hundreds of
 * blocked request threads, which turned into a slow product API, which
 * turned into a page from Daniel. None of that had anything to do with a
 * bug in Sarah's Java code — it was the total absence of any resilience
 * around a network call to a dependency Sarah did not control.
 *
 * <p>Five patterns work together here:
 * <ul>
 *   <li><b>Timeout</b> — configured on the underlying HTTP client (see
 *       {@code AiClientConfig}), not here. A call that never comes back is
 *       worse than one that fails fast.</li>
 *   <li><b>Retry with backoff</b> — {@link Retry}, configured in
 *       {@code application.yml} with exponential backoff. A short, one-off
 *       blip is retried automatically instead of failing the customer's
 *       request.</li>
 *   <li><b>Circuit breaker</b> — {@link CircuitBreaker}. If the AI
 *       provider keeps failing, stop calling it for a while instead of
 *       piling up more slow, doomed requests.</li>
 *   <li><b>Bulkhead</b> — {@link Bulkhead}. Caps how many requests can be
 *       waiting on the AI provider at once, so a slow AI provider cannot
 *       exhaust every thread in the application.</li>
 *   <li><b>Fallback</b> — {@code fallbackAnswer}, invoked once retries are
 *       exhausted or the circuit is open. The customer gets a useful
 *       message instead of a spinner or an error page.</li>
 * </ul>
 *
 * <p>The annotation order matters, and it is a common mistake to get
 * wrong. Resilience4j's Spring Boot integration applies these annotations
 * from the outside in, in this fixed order: {@code Retry}, then
 * {@code CircuitBreaker}, then {@code Bulkhead}, then the method itself.
 * That means {@link Retry} is the <b>outermost</b> layer here — it is the
 * one that sees the final, retries-exhausted failure. That is why
 * {@code fallbackMethod} is declared on {@link Retry} below, not on
 * {@link CircuitBreaker}. If the fallback were attached to
 * {@code CircuitBreaker} instead, it would catch the very first failed
 * attempt and return a value immediately — and {@code Retry}, seeing what
 * looks like a successful call, would never retry at all.
 */
@Service
public class ShoppingAssistantService {

    private static final Logger log = LoggerFactory.getLogger(ShoppingAssistantService.class);

    private static final String RESILIENCE_INSTANCE = "acmeAiAssistant";

    private static final String SYSTEM_PROMPT = """
            You are the Acme Retail shopping assistant. Answer questions
            about products, orders, and store policy in two sentences or
            fewer. If you are not sure, say so instead of guessing.
            """;

    private final AiModelClient aiModelClient;

    public ShoppingAssistantService(AiModelClient aiModelClient) {
        this.aiModelClient = aiModelClient;
    }

    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackAnswer")
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    @Bulkhead(name = RESILIENCE_INSTANCE)
    public AssistantAnswer askAssistant(String question) {
        String reply = aiModelClient.complete(SYSTEM_PROMPT, question);
        return AssistantAnswer.fromModel(reply);
    }

    /**
     * Invoked instead of {@link #askAssistant} once every retry attempt
     * has failed, or immediately if the circuit breaker's circuit is
     * already open (in which case each "attempt" fails instantly with a
     * {@code CallNotPermittedException} instead of reaching the AI
     * provider at all).
     *
     * <p>The fallback method signature must match {@code askAssistant}'s
     * parameters plus one extra parameter for the exception that
     * triggered it — that is how Resilience4j finds it.
     *
     * <p>This method must not call the AI provider. Its entire job is to
     * return quickly with something safe to show the customer.
     */
    private AssistantAnswer fallbackAnswer(String question, Throwable exception) {
        log.warn("Shopping assistant fallback triggered for question '{}': {}",
                question, exception.toString());
        return AssistantAnswer.fallback();
    }
}
