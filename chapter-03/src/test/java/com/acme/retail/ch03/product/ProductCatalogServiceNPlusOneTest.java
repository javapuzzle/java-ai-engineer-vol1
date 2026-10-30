package com.acme.retail.ch03.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.retail.ch03.category.Category;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

/**
 * Proves the N+1 query pattern with real numbers instead of a claim in a
 * paragraph.
 *
 * <p>{@code @DataJpaTest} boots just the JPA slice of the app against an
 * in-memory H2 database (see the {@code h2} test dependency in
 * {@code pom.xml}) — no PostgreSQL, no Redis, no full application context.
 *
 * <p>Five products are seeded, each in its own category, then the
 * persistence context is cleared so the next query has to build fresh
 * (genuinely lazy) proxies — otherwise Hibernate would just hand back the
 * already-loaded categories from the previous step and hide the bug.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class ProductCatalogServiceNPlusOneTest {

    private static final int PRODUCT_COUNT = 5;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private ProductCatalogService catalogService;

    private Statistics statistics;

    @BeforeEach
    void seedCatalog() {
        catalogService = new ProductCatalogService(productRepository);

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        for (int i = 1; i <= PRODUCT_COUNT; i++) {
            Category category = new Category("Category " + i);
            entityManager.persist(category);
            entityManager.persist(new Product(
                    "Product " + i, "description", 1000L + i, "SKU-" + i, 10, category));
        }

        // Flush the inserts, then clear the persistence context. Without
        // this, the products and categories created above stay attached
        // to this same session, and a later getCategory() call would
        // return the already-loaded object instead of a lazy proxy.
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void naiveCatalogRunsOneQueryPerProductInAdditionToTheInitialFind() {
        statistics.clear();

        List<ProductView> views = catalogService.naiveCatalog();

        assertThat(views).hasSize(PRODUCT_COUNT);
        // 1 query for findAll() + 1 lazy-load query per product's category,
        // because every product is in a different category.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(PRODUCT_COUNT + 1L);
    }

    @Test
    void fixedCatalogRunsExactlyOneQueryRegardlessOfProductCount() {
        statistics.clear();

        List<ProductView> views = catalogService.catalog();

        assertThat(views).hasSize(PRODUCT_COUNT);
        // JOIN FETCH loads every product and its category in one statement.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }
}
