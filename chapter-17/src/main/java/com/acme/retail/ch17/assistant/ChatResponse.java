package com.acme.retail.ch17.assistant;

/**
 * The reply sent back to the customer. It always carries the correlation ID
 * that was used for this request, so if the customer complains again, the
 * support team can jump straight to the right logs, metrics and trace.
 */
public record ChatResponse(String correlationId, String reply) {
}
