package com.acme.retail.ch11.inventory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the pure grounding-text logic directly -- no database, no model,
 * no Spring context. This is the part of the RAG pipeline that never
 * changes behavior between test runs, so it gets a plain, fast unit test.
 */
class GroundingContextBuilderTest {

    @Test
    void reportsOutOfStockProductHonestly() {
        InventoryFact soldOut = new InventoryFact("Black Running Shoe", "RUN-BLK-42", 0, false);

        String context = GroundingContextBuilder.toContext(List.of(soldOut));

        assertThat(context).contains("Black Running Shoe");
        assertThat(context).contains("RUN-BLK-42");
        assertThat(context).contains("out of stock (0 units available)");
    }

    @Test
    void reportsInStockProductWithQuantity() {
        InventoryFact inStock = new InventoryFact("White Running Shoe", "RUN-WHT-42", 12, true);

        String context = GroundingContextBuilder.toContext(List.of(inStock));

        assertThat(context).contains("12 units in stock");
    }

    @Test
    void noMatchesProducesAnHonestNotFoundFact() {
        String context = GroundingContextBuilder.toContext(List.of());

        assertThat(context).contains("No matching product was found");
    }
}
