package com.acme.retail.ch19.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a customer or the evaluation harness asks about a SKU
 * (Stock Keeping Unit -- a retailer's unique code for one product) that
 * does not exist in the catalog.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String sku) {
        super("No product found for SKU " + sku);
    }
}
