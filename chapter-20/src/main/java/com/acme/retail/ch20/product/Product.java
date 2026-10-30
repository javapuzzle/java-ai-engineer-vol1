package com.acme.retail.ch20.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A product in the Acme Retail catalog.
 *
 * <p>This is the same shape used throughout the book (see the character
 * bible's Acme Retail domain model): {@code id}, {@code name},
 * {@code description}, {@code priceCents}, {@code sku},
 * {@code stockQuantity}, {@code category}. In this chapter, {@code
 * stockQuantity} is the ground truth that the AI shopping assistant's
 * answers are checked against. When the model claims a product is in stock
 * but this table says otherwise, that mismatch is the incident.
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

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getCategory() {
        return category;
    }

    /**
     * True when there is at least one unit available to sell.
     */
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }
}
