package com.acme.retail.ch18.customer;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Customer}.
 *
 * <p>Notice what does NOT depend on this repository: {@code
 * OrderStatusLookupTool}, the class that gives the AI assistant a
 * function it can call. The assistant is never handed this repository,
 * so it has no path to read a customer's email or phone number, even by
 * accident. That is least privilege in practice -- see the chapter's
 * "Traditional vs Modern" section.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
