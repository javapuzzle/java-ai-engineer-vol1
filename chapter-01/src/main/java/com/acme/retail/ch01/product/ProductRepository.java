package com.acme.retail.ch01.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * The repository layer -- the only place in this module that "knows" about
 * the database.
 *
 * <p>Sarah does not write a single line of SQL here. {@link JpaRepository}
 * already provides {@code findAll()}, {@code findById(id)}, {@code save(entity)}
 * and {@code deleteById(id)}. Spring Data JPA generates the implementation
 * of this interface at startup -- there is no {@code ProductRepositoryImpl}
 * class anywhere in this project.
 *
 * <p>{@code findBySku} is a "query derivation" method: Spring Data JPA reads
 * the method name, understands that it means
 * {@code SELECT * FROM products WHERE sku = ?}, and generates that query
 * automatically. No annotation and no SQL string are required for a query
 * this simple.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);
}
