package com.acme.retail.ch12.product;

/**
 * A product in the Acme Retail catalog.
 *
 * <p>Kept as a simple immutable record for this chapter -- streaming is the
 * lesson here, not persistence, so this module has no database. See
 * {@link ProductRepository} for the small in-memory catalog it comes from.
 * The field names match the {@code Product} entity from earlier chapters on
 * purpose, so the domain stays recognizable across the book.
 */
public record Product(
        Long id,
        String name,
        String description,
        Integer priceCents,
        String sku,
        Integer stockQuantity,
        String category) {
}
