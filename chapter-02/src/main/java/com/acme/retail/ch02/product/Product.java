package com.acme.retail.ch02.product;

/**
 * A product in the Acme Retail catalog.
 *
 * <p>Field names match the shared Acme Retail domain model used across
 * every chapter of this book, so the story reads as one evolving system
 * even though each chapter's code module is independent.
 */
public record Product(
        Long id,
        String name,
        String description,
        long priceCents,
        String sku,
        int stockQuantity,
        String category) {
}
