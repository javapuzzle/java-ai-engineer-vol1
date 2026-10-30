package com.acme.retail.ch20.assistant;

/**
 * The response body for {@code POST /api/assistant/ask}.
 *
 * <p>{@code correlationId} is always returned, even on a fallback or a
 * disabled response, so Daniel and Sarah can find the exact request in the
 * structured logs (Chapter 17) if a customer reports a problem.
 *
 * @param correlationId the ID that ties this response to its log lines
 * @param answer        the text shown to the customer
 * @param grounded      true when the answer is backed by the real
 *                      inventory table, either because the model agreed
 *                      with it or because a fallback used it directly
 * @param outcome       one of {@code success}, {@code fallback_mismatch},
 *                      {@code fallback_circuit_open}, {@code
 *                      fallback_provider_error}, or {@code feature_disabled}
 */
public record AssistantResponse(String correlationId, String answer, boolean grounded, String outcome) {

    public static AssistantResponse success(String correlationId, String answer) {
        return new AssistantResponse(correlationId, answer, true, "success");
    }

    public static AssistantResponse fallback(String correlationId, ProductFact fact, String outcome) {
        String answer = fact.inStock()
                ? "We show " + fact.stockQuantity() + " units of " + fact.name() + " in stock right now."
                : "We're sorry, " + fact.name() + " is currently sold out.";
        return new AssistantResponse(correlationId, answer, true, outcome);
    }

    public static AssistantResponse disabled(String correlationId) {
        return new AssistantResponse(correlationId,
                "The shopping assistant is temporarily unavailable. Please check the product page directly.",
                false, "feature_disabled");
    }
}
