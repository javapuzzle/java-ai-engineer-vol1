package com.acme.retail.ch11.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A product in the Acme Retail catalog.
 *
 * <p>This is the same domain shape used since Chapter 01: a plain JPA
 * entity mapped to the {@code products} table. This table is Acme Retail's
 * single source of truth for what is actually in stock right now. It is
 * the "application data" this chapter is about -- data the language model
 * has never seen and cannot know unless the application shows it.
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

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(Integer priceCents) {
        this.priceCents = priceCents;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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

    public void setCategory(String category) {
        this.category = category;
    }
}
