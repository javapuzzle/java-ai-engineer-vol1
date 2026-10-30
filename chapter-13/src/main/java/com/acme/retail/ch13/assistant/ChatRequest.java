package com.acme.retail.ch13.assistant;

/**
 * Request body for {@code POST /api/assistant/sessions/{sessionId}/messages}.
 */
public record ChatRequest(String message) {
}
