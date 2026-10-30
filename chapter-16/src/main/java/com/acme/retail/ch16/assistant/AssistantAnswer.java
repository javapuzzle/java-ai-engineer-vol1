package com.acme.retail.ch16.assistant;

/**
 * The assistant's answer, returned to the customer-facing web page.
 *
 * @param message  the text shown to the customer. Either the AI provider's
 *                  real answer, or the canned fallback message.
 * @param degraded {@code true} when {@code message} is the fallback
 *                  response rather than a real answer from the AI
 *                  provider. The frontend can use this to show a small
 *                  "assistant is running in limited mode" hint instead of
 *                  pretending nothing happened.
 */
public record AssistantAnswer(String message, boolean degraded) {

    public static final String FALLBACK_MESSAGE =
            "Our assistant is temporarily unavailable, please browse manually.";

    public static AssistantAnswer fromModel(String message) {
        return new AssistantAnswer(message, false);
    }

    public static AssistantAnswer fallback() {
        return new AssistantAnswer(FALLBACK_MESSAGE, true);
    }
}
