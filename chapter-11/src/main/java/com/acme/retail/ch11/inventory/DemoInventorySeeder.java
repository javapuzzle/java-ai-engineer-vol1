package com.acme.retail.ch11.inventory;

import com.acme.retail.ch11.product.Product;
import com.acme.retail.ch11.product.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a few demo products on startup so this module is runnable without
 * a separate data-loading step.
 *
 * <p>The "Black Running Shoe" row is the one from the chapter's story: it
 * exists in the catalog, but {@code stockQuantity} is zero. Everything
 * else in this chapter exists to make sure the assistant says so.
 */
@Component
public class DemoInventorySeeder implements ApplicationRunner {

    private final ProductRepository productRepository;

    public DemoInventorySeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }

        productRepository.save(new Product(
                "Black Running Shoe", "Lightweight everyday trainer.", 8999,
                "RUN-BLK-42", 0, "Footwear"));

        productRepository.save(new Product(
                "White Running Shoe", "Lightweight everyday trainer.", 8999,
                "RUN-WHT-42", 12, "Footwear"));

        productRepository.save(new Product(
                "Red Running Shoe", "Lightweight everyday trainer.", 8999,
                "RUN-RED-42", 3, "Footwear"));
    }
}
