package com.acme.retail.ch02.product;

import org.springframework.stereotype.Service;

/**
 * Application logic for reading products.
 *
 * <p>Nothing in this class changed between Sarah's "raise the thread
 * pool" attempt and the "enable virtual threads" fix. That is the whole
 * point of this chapter: the fix for a blocking-I/O problem usually does
 * not require rewriting business logic, only changing how the platform
 * runs it.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id);
    }
}
