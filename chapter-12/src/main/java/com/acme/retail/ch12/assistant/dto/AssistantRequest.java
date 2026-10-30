package com.acme.retail.ch12.assistant.dto;

/**
 * The customer's question, sent as JSON to both the synchronous
 * {@code /assistant/ask} endpoint and the streaming
 * {@code /assistant/ask/stream} endpoint.
 */
public record AssistantRequest(String question) {
}
