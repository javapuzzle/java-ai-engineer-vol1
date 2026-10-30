package com.acme.retail.ch19.inventory;

import com.acme.retail.ch19.product.Product;
import com.acme.retail.ch19.product.ProductNotFoundException;
import com.acme.retail.ch19.product.ProductRepository;
import org.springframework.stereotype.Service;

/**
 * Turns a database row into the grounding facts the assistant is allowed
 * to answer from.
 *
 * <p>This is the same idea Sarah used in Chapter 11 (retrieve real data,
 * then let the model generate from it) -- this chapter is not re-teaching
 * grounding, it is testing it.
 */
@Service
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public String groundingFactsFor(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));

        String availability = product.getStockQuantity() > 0
                ? product.getStockQuantity() + " unit(s) in stock"
                : "sold out, 0 units in stock";

        return "%s (SKU %s, category: %s) -- %s.".formatted(
                product.getName(), product.getSku(), product.getCategory(), availability);
    }
}
