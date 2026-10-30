package com.acme.retail.ch10.assistant;

/**
 * The incoming request body for {@code POST /api/shopping-assistant/recommend}.
 *
 * @param message the customer's message, in their own words, e.g.
 *                {@code "I need something for trail running"}
 */
public record ShoppingAssistantRequest(String message) {
}
