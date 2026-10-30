package com.acme.retail.ch20.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * The only place in this module that "knows" about the products table.
 *
 * <p>{@link #findBySku} is what {@link com.acme.retail.ch20.assistant.AssistantService}
 * calls before it lets any answer reach a customer -- it is the source of
 * ground truth that the model's claims get checked against.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);
}
