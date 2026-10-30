package com.acme.retail.ch11.inventory;

import com.acme.retail.ch11.product.Product;
import com.acme.retail.ch11.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieves real inventory facts from Acme Retail's own database.
 *
 * <p>This is the piece that was missing before this chapter. Sarah's first
 * version of the shopping assistant (Chapter 10) sent the customer's
 * question straight to the model. The model answered fluently because
 * that is what language models are trained to do -- but it had never been
 * shown Acme Retail's stock table, so its confident "Yes" was a guess, not
 * a fact. This service is the fix: look up the real row before anyone asks
 * the model to say anything about it.
 */
@Service
public class InventoryLookupService {

    private final ProductRepository productRepository;

    public InventoryLookupService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Finds every product whose name matches the customer's query and
     * converts each one into a fact the model can be grounded on.
     *
     * <p>Returns an empty list when nothing matches -- this is a fact too:
     * it tells the prompt-assembly step "the catalog does not know this
     * product," which is very different from "the product exists and is in
     * stock." Losing that distinction is exactly how hallucinations happen.
     */
    public List<InventoryFact> findFacts(String productQuery) {
        List<Product> matches = productRepository.findByNameContainingIgnoreCase(productQuery);

        return matches.stream()
                .map(InventoryLookupService::toFact)
                .toList();
    }

    private static InventoryFact toFact(Product product) {
        int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        boolean available = stockQuantity > 0;
        return new InventoryFact(product.getName(), product.getSku(), stockQuantity, available);
    }
}
