package com.acme.retail.ch12.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryServiceTest {

    private final InventoryService inventoryService = new InventoryService(new ProductRepository());

    @Test
    void catalogFacts_listsEveryProductWithSkuAndStockCount() {
        String facts = inventoryService.catalogFacts();

        assertThat(facts)
                .contains("SHOE-BLK-001")
                .contains("4 in stock")
                .contains("SHOE-WHT-001")
                .contains("0 in stock")
                .contains("JKT-RAIN-002");
    }
}
