package com.acme.retail.ch03.product;

import com.acme.retail.ch03.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A catalog product, same shape used since Chapter 01, now with a real
 * {@link Category} relationship.
 *
 * <p>The {@code category} association is declared {@code LAZY}. That is a
 * reasonable, common default — the problem in this chapter is never the
 * annotation itself, it is code that reads {@code getCategory()} inside a
 * loop without telling Hibernate to fetch it up front.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected Product() {
        // required by JPA
    }

    public Product(String name, String description, long priceCents, String sku,
                    int stockQuantity, Category category) {
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

    public long getPriceCents() {
        return priceCents;
    }

    public String getSku() {
        return sku;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public Category getCategory() {
        return category;
    }
}
