package com.acme.retail.ch18.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Order}.
 *
 * <p>{@link #findByOrderCode(String)} is the only lookup
 * {@code OrderStatusLookupTool} is allowed to use. There is no
 * {@code findAll()} call anywhere in the tool, and no method here lets
 * the caller list every order for a customer -- the assistant can look up
 * exactly one order at a time, by the code the customer typed.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);
}
