package com.acme.retail.ch03.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /*
     * findAll() is inherited from JpaRepository. It is not wrong by itself —
     * it runs exactly one query. The bug in this chapter is not this method;
     * it is code that later calls product.getCategory().getName() on every
     * row it returns. See ProductCatalogService.naiveCatalog().
     */

    /**
     * The fix Sarah reaches for first: an explicit JOIN FETCH. Hibernate
     * loads every product and its category in a single SQL statement, so
     * there is nothing left to lazy-load later.
     */
    @Query("select p from Product p join fetch p.category")
    List<Product> findAllWithCategory();

    @Query("select p from Product p join fetch p.category where p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    /**
     * The same fix expressed a different way: an {@code @EntityGraph}
     * annotation instead of hand-written JPQL. Useful when the query itself
     * is already generated from the method name (a "derived query") and
     * you still want to control what gets fetched eagerly.
     */
    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryId(Long categoryId);
}
