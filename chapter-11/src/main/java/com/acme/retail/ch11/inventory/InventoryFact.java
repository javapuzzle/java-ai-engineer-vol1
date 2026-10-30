package com.acme.retail.ch11.inventory;

/**
 * One retrieved fact about a product's real stock level.
 *
 * <p>This is the "retrieval" half of RAG (Retrieval-Augmented Generation).
 * Before the model generates anything, Sarah's application retrieves facts
 * like this one from the real {@code products} table and hands them to the
 * model as grounding context -- text the model can read and quote, instead
 * of a live database connection it does not have.
 *
 * @param productName   the product's name as stored in Acme Retail's catalog
 * @param sku           the stock keeping unit, Acme Retail's internal product code
 * @param stockQuantity the real number of units currently in stock
 * @param available     {@code true} when {@code stockQuantity > 0}
 */
public record InventoryFact(String productName, String sku, int stockQuantity, boolean available) {
}
