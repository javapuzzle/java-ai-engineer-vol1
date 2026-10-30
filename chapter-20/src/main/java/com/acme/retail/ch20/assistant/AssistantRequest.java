package com.acme.retail.ch20.assistant;

/**
 * The incoming request body for {@code POST /api/assistant/ask}.
 *
 * @param customerId an identifier for the customer asking (kept out of logs
 *                   in plain text -- see Chapter 18's data-handling rules)
 * @param sku        the product the customer is asking about
 * @param question   the customer's question, in their own words
 */
public record AssistantRequest(String customerId, String sku, String question) {
}
