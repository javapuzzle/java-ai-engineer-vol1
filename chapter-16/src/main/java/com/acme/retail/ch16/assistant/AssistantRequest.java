package com.acme.retail.ch16.assistant;

/**
 * The customer's question to the Acme Retail shopping assistant.
 *
 * @param question free-text question, e.g. "Are the black running shoes
 *                  back in stock?"
 */
public record AssistantRequest(String question) {
}
