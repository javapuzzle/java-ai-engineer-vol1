package com.acme.retail.ch13.assistant;

/**
 * Response body for {@code POST /api/assistant/sessions/{sessionId}/messages}.
 */
public record ChatReply(String sessionId, String reply) {
}
