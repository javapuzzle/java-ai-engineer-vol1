package com.acme.retail.ch14.visualsearch;

/**
 * The model's structured answer to a visual search question.
 *
 * <p>Deliberately does not include a specific SKU, price, or stock count --
 * the vision-capable model can describe what it sees and suggest a likely
 * category and color, but it cannot confirm what Acme Retail actually has
 * in stock. That confirmation belongs to the grounded, RAG-backed assistant
 * from Chapter 11, not to this endpoint.
 */
public record VisualMatchResult(
        String visualDescription,
        String likelyCategory,
        String likelyColor,
        String suggestionForCustomer) {
}
