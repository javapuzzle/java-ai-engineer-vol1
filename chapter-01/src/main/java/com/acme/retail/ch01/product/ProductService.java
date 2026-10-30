package com.acme.retail.ch01.product;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The service layer -- this is where Sarah's business logic lives.
 *
 * <p>Right now the logic is simple: fetch a product, list all products,
 * create a product. But the point of having this class at all -- instead
 * of calling {@link ProductRepository} directly from
 * {@link ProductController} -- is that business rules will grow here over
 * time (for example: "do not allow negative stock," or, in later chapters,
 * "check the cache before hitting the database"), without the controller
 * or the database access code ever needing to change.
 *
 * <p>{@code @Service} marks this class as a Spring-managed bean. Spring
 * creates one instance of it and hands it to anything that asks for a
 * {@code ProductService} in a constructor -- this is dependency injection.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
}
