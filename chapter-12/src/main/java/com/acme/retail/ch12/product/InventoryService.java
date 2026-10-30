package com.acme.retail.ch12.product;

import org.springframework.stereotype.Service;

/**
 * Turns raw catalog rows into the short, factual sentences the assistant is
 * allowed to repeat to a customer.
 *
 * <p>This is the same "ground the model in real data" idea Sarah learned in
 * Chapter 11 (The AI Is Wrong): the model never sees the database directly,
 * it only ever sees the sentences this service hands it. Chapter 12 does
 * not change that -- it changes how the model's answer travels back to the
 * customer once the model has it.
 */
@Service
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * One short sentence per product, e.g. "The Trailhead Running Shoe
     * (SHOE-BLK-001) has 4 in stock." The catalog is small enough in this
     * chapter to hand the model every fact every time; a larger catalog
     * would need retrieval (Chapter 11) to pick only the relevant facts.
     */
    public String catalogFacts() {
        StringBuilder facts = new StringBuilder();
        for (Product product : productRepository.findAll()) {
            facts.append("The ").append(product.name())
                    .append(" (").append(product.sku()).append(") has ")
                    .append(product.stockQuantity()).append(" in stock. ");
        }
        return facts.toString().trim();
    }
}
