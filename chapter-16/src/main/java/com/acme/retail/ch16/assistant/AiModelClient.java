package com.acme.retail.ch16.assistant;

/**
 * A thin abstraction over "ask the AI provider a question."
 *
 * <p>The real work happens in {@link SpringAiModelClient}, which calls
 * Spring AI's {@code ChatClient}. This interface exists so that
 * {@link ShoppingAssistantService} — and its tests — do not depend on the
 * exact fluent {@code ChatClient} call chain. In tests, a fake
 * implementation can throw a {@link java.util.concurrent.TimeoutException}
 * or {@link RuntimeException} on demand to simulate the AI provider being
 * slow or down, without needing a real network call.
 */
public interface AiModelClient {

    /**
     * Sends the system prompt and the customer's question to the AI
     * provider and returns its answer.
     *
     * @throws RuntimeException if the call fails or times out. The exact
     *                          exception type depends on the underlying
     *                          HTTP client; {@link ShoppingAssistantService}
     *                          treats any exception from this method as a
     *                          failure worth retrying / tripping the
     *                          circuit breaker on.
     */
    String complete(String systemPrompt, String question);
}
