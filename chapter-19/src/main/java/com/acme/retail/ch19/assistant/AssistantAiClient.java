package com.acme.retail.ch19.assistant;

/**
 * The seam between "our code" and "the AI provider".
 *
 * <p>{@link ShoppingAssistantService} talks only to this interface, never
 * directly to Spring AI's {@code ChatClient}. That one line of indirection
 * is what lets a test replace the real model with a fixed, deterministic
 * stub -- see {@code ShoppingAssistantIntegrationTest} in {@code src/test}
 * -- without touching any production code.
 */
@FunctionalInterface
public interface AssistantAiClient {

    String ask(String systemPrompt, String customerQuestion);
}
