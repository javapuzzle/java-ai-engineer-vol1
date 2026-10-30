package com.acme.retail.ch01.product;

/**
 * Thrown when a product ID is requested but no matching row exists.
 *
 * <p>This is a plain, unchecked exception. {@link ProductExceptionHandler}
 * catches it and turns it into an HTTP 404 response, so the service layer
 * itself never needs to think about HTTP status codes.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product not found: " + id);
    }
}
