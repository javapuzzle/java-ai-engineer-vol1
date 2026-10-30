package com.acme.retail.ch02.product;

/**
 * Thrown when a product does not exist in the catalog.
 *
 * <p>Translated to an HTTP 404 by {@link ProductController}.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product not found: " + id);
    }
}
