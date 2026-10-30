package com.acme.retail.ch03.product;

/**
 * The shape the catalog API and the cache actually return.
 *
 * <p>A DTO (data transfer object), not the JPA entity. Two reasons: the
 * entity carries a lazy Hibernate proxy for {@code category} that has no
 * business leaving this module, and the cache needs a plain, JSON-friendly
 * type it can serialize into Redis without touching Hibernate at all.
 */
public record ProductView(
        Long id,
        String name,
        String sku,
        long priceCents,
        int stockQuantity,
        String categoryName) {

    public static ProductView from(Product product) {
        return new ProductView(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPriceCents(),
                product.getStockQuantity(),
                product.getCategory().getName());
    }
}
