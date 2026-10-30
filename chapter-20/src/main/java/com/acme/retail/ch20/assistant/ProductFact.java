package com.acme.retail.ch20.assistant;

/**
 * The facts about one product that Sarah's assistant is allowed to reason
 * from -- pulled directly from the products table, not from the model.
 *
 * <p>This is the same grounding idea from Chapter 11 (Retrieval-Augmented
 * Generation): before the model answers a question about a specific
 * product, the application hands it real facts instead of trusting it to
 * already know Acme Retail's live inventory.
 *
 * @param sku          the product SKU (Stock Keeping Unit -- Acme Retail's
 *                      unique product code)
 * @param name          the product name
 * @param inStock       true when the real inventory table shows at least
 *                      one unit available
 * @param stockQuantity the real quantity on hand
 */
public record ProductFact(String sku, String name, boolean inStock, int stockQuantity) {

    /**
     * A fact for a SKU that does not exist in the catalog at all. The
     * assistant still needs something to reason from when a customer asks
     * about an unknown product.
     */
    public static ProductFact unknown(String sku) {
        return new ProductFact(sku, "unknown product", false, 0);
    }
}
