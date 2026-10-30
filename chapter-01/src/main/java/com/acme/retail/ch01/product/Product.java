package com.acme.retail.ch01.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A product in the Acme Retail catalog.
 *
 * <p>This is a JPA entity: a plain Java class annotated so that Spring Data
 * JPA (and, underneath it, Hibernate) knows how to map it to a row in the
 * {@code products} table. Sarah does not write any SQL for basic CRUD
 * (Create, Read, Update, Delete) operations on this class -- Spring Data JPA
 * generates it from {@link ProductRepository}.
 *
 * <p>Money is stored as {@code priceCents} (an integer number of cents,
 * e.g. 4999 for $49.99) rather than a floating point dollar amount. Floating
 * point numbers cannot represent every decimal fraction exactly, so storing
 * currency as cents avoids rounding bugs.
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
