package com.acme.retail.ch10.assistant;

/**
 * A shopping assistant's structured recommendation for one product.
 *
 * <p>Before this chapter, an AI response was just a String of free text.
 * That is fine for a chat window, but it is awkward for code: if the
 * frontend wants to show a "product name" field and a "why we picked this"
 * field, someone has to parse the model's sentences to find them, and that
 * parsing breaks the moment the model phrases its answer differently.
 *
 * <p>Spring AI's structured output support solves this by asking the model
 * to return JSON that matches this record's fields, and converting that
 * JSON into a real {@code ProductRecommendation} object for us. See
 * {@link ShoppingAssistantController#recommend} for where this happens.
 *
 * <p>Notice that this record does <strong>not</strong> have a field like
 * {@code inStock} or {@code stockQuantity}. That is deliberate: the model
 * only knows what Sarah put in the system prompt (a short category
 * summary), not Acme Retail's live inventory. Asking it to report stock
 * status here would invite it to guess. Chapter 11 explores exactly what
 * goes wrong when an AI assistant is expected to know live application
 * data it was never given.
 *
 * @param productName the specific product or product type being recommended
 * @param category the catalog category the recommendation falls under
 * @param reason a short, one-or-two-sentence explanation of why this fits
 *               the customer's message
 */
public record ProductRecommendation(String productName, String category, String reason) {
}
