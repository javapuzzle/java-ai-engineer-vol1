package com.acme.retail.ch11.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * The repository layer -- the only place in this module that "knows" about
 * the database.
 *
 * <p>{@code findByNameContainingIgnoreCase} is a query-derivation method:
 * Spring Data JPA reads the method name and generates
 * {@code SELECT * FROM products WHERE LOWER(name) LIKE LOWER('%...%')} for
 * us. This is the real query the grounding step in this chapter runs
 * against the live catalog -- it is deliberately simple (a keyword match,
 * not semantic search) because Acme Retail's product names are short and
 * well controlled. Semantic search over unstructured text is what vector
 * search and embeddings are for, introduced later in this chapter's theory
 * section.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String nameFragment);
}
