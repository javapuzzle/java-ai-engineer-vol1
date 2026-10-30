package com.acme.retail.ch11.inventory;

import com.acme.retail.ch11.product.Product;
import com.acme.retail.ch11.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests the retrieval step in isolation, with the repository mocked. This
 * proves the mapping logic (stock quantity zero -&gt; not available) without
 * needing a real PostgreSQL database.
 */
@ExtendWith(MockitoExtension.class)
class InventoryLookupServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Test
    void soldOutProductIsMarkedUnavailable() {
        Product blackShoe = new Product(
                "Black Running Shoe", "Lightweight trainer.", 8999, "RUN-BLK-42", 0, "Footwear");
        when(productRepository.findByNameContainingIgnoreCase("running shoe"))
                .thenReturn(List.of(blackShoe));

        InventoryLookupService service = new InventoryLookupService(productRepository);
        List<InventoryFact> facts = service.findFacts("running shoe");

        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).available()).isFalse();
        assertThat(facts.get(0).stockQuantity()).isZero();
    }

    @Test
    void inStockProductIsMarkedAvailable() {
        Product whiteShoe = new Product(
                "White Running Shoe", "Lightweight trainer.", 8999, "RUN-WHT-42", 12, "Footwear");
        when(productRepository.findByNameContainingIgnoreCase("white"))
                .thenReturn(List.of(whiteShoe));

        InventoryLookupService service = new InventoryLookupService(productRepository);
        List<InventoryFact> facts = service.findFacts("white");

        assertThat(facts.get(0).available()).isTrue();
        assertThat(facts.get(0).stockQuantity()).isEqualTo(12);
    }

    @Test
    void unknownProductReturnsNoFacts() {
        when(productRepository.findByNameContainingIgnoreCase("hoverboard"))
                .thenReturn(List.of());

        InventoryLookupService service = new InventoryLookupService(productRepository);
        List<InventoryFact> facts = service.findFacts("hoverboard");

        assertThat(facts).isEmpty();
    }
}
