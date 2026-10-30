package com.acme.retail.ch03.product;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds the product catalog view.
 *
 * <p>Two methods on purpose: {@link #naiveCatalog()} is Sarah's first
 * version, kept here (and covered by a test) exactly because it is the code
 * this whole chapter is about. {@link #catalog()} is what the controller
 * actually calls today.
 */
@Service
public class ProductCatalogService {

    private final ProductRepository productRepository;

    public ProductCatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * BEFORE. This method runs one query for {@code findAll()}, then one
     * more query the first time each product's {@code category} is
     * touched — because {@code Product.category} is a lazy
     * {@code @ManyToOne}. For a catalog page with 200 products, that is
     * 201 round trips to the database instead of 1. This is the N+1 query
     * pattern.
     */
    public List<ProductView> naiveCatalog() {
        List<Product> products = productRepository.findAll();
        List<ProductView> views = new ArrayList<>();
        for (Product product : products) {
            views.add(new ProductView(
                    product.getId(),
                    product.getName(),
                    product.getSku(),
                    product.getPriceCents(),
                    product.getStockQuantity(),
                    product.getCategory().getName() // <-- one extra query, per product
            ));
        }
        return views;
    }

    /**
     * AFTER. {@code findAllWithCategory()} uses {@code JOIN FETCH}, so
     * Hibernate loads every product and its category in one SQL statement.
     * Reading {@code getCategory().getName()} here costs nothing extra —
     * the category was already loaded.
     */
    public List<ProductView> catalog() {
        return productRepository.findAllWithCategory().stream()
                .map(ProductView::from)
                .toList();
    }
}
