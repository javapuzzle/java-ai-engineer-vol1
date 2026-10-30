package com.acme.retail.ch19.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link Product}. Spring Data JPA generates the
 * implementation of {@code findBySku} from the method name -- no SQL is
 * written by hand.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);
}
