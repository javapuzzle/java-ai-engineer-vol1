package com.acme.retail.ch12.product;

import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory stand-in for the Acme Retail product catalog.
 *
 * <p>Chapters 01-03 gave this catalog a real PostgreSQL table behind a
 * {@code ProductRepository} extending {@code JpaRepository}. This chapter is
 * about streaming the assistant's answer, not about persistence, so the
 * catalog here is a small preloaded map instead. The class name and method
 * names stay identical to the earlier chapters on purpose -- a reader who
 * has followed the story should recognize this immediately.
 */
@Repository
public class ProductRepository {

    private final Map<Long, Product> products = new LinkedHashMap<>();

    public ProductRepository() {
        AtomicLong ids = new AtomicLong(1);
        seed(new Product(ids.getAndIncrement(), "Trailhead Running Shoe",
                "Lightweight everyday running shoe, black upper with reflective trim.",
                8999, "SHOE-BLK-001", 4, "Footwear"));
        seed(new Product(ids.getAndIncrement(), "Trailhead Running Shoe",
                "Lightweight everyday running shoe, white and grey upper.",
                8999, "SHOE-WHT-001", 0, "Footwear"));
        seed(new Product(ids.getAndIncrement(), "Summit Rain Jacket",
                "Packable waterproof shell, unisex sizing.",
                12999, "JKT-RAIN-002", 15, "Outerwear"));
    }

    private void seed(Product product) {
        products.put(product.id(), product);
    }

    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }
}
