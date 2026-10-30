package com.acme.retail.ch19.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A product in the Acme Retail catalog.
 *
 * <p>Same shape as every other chapter's {@code Product} entity -- id,
 * name, description, priceCents, sku, stockQuantity, category. This
 * chapter does not change the domain model. It changes how Sarah proves
 * that code built on top of this model actually behaves correctly.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private Integer priceCents;

    private String sku;

    private Integer stockQuantity;

    private String category;

    protected Product() {
        // Required by JPA/Hibernate to build entities via reflection.
    }

    public Product(String name, String description, Integer priceCents,
                    String sku, Integer stockQuantity, String category) {
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.sku = sku;
        this.stockQuantity = stockQuantity;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getPriceCents() {
        return priceCents;
    }

    public String getSku() {
        return sku;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public String getCategory() {
        return category;
    }
}
