package com.acme.retail.ch20.evaluation;

import com.acme.retail.ch20.assistant.SimulatedModelProvider;
import com.acme.retail.ch20.product.Product;
import com.acme.retail.ch20.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the golden-dataset evaluation gate.
 *
 * <p>The dataset below mixes in-stock and sold-out products on purpose:
 * a provider that always claims "in stock" (the launch-day bug) should
 * pass exactly the in-stock cases and fail exactly the sold-out ones.
 */
@ExtendWith(MockitoExtension.class)
class GoldenDatasetEvaluatorTest {

    @Mock
    private ProductRepository productRepository;

    private final SimulatedModelProvider modelProvider = new SimulatedModelProvider();

    private List<GoldenCase> goldenDataset() {
        return List.of(
                new GoldenCase("case-1", "SKU-1001", "Is the trail shoe in stock?", true),
                new GoldenCase("case-2", "SKU-1002", "Is the hiking boot in stock?", false),
                new GoldenCase("case-3", "SKU-1003", "Is the daypack in stock?", true),
                new GoldenCase("case-4", "SKU-1004", "Is the water bottle in stock?", false));
    }

    private void stubCatalog() {
        when(productRepository.findBySku("SKU-1001"))
                .thenReturn(Optional.of(new Product("Trail Shoe", "desc", 8999, "SKU-1001", 10, "Footwear")));
        when(productRepository.findBySku("SKU-1002"))
                .thenReturn(Optional.of(new Product("Hiking Boot", "desc", 12999, "SKU-1002", 0, "Footwear")));
        when(productRepository.findBySku("SKU-1003"))
                .thenReturn(Optional.of(new Product("Daypack", "desc", 5999, "SKU-1003", 5, "Bags")));
        when(productRepository.findBySku("SKU-1004"))
                .thenReturn(Optional.of(new Product("Water Bottle", "desc", 1999, "SKU-1004", 0, "Accessories")));
    }

    @Test
    void healthyProvider_passesEveryGoldenCase() {
        stubCatalog();
        GoldenDatasetEvaluator evaluator = new GoldenDatasetEvaluator(modelProvider, productRepository);

        EvaluationResult result = evaluator.evaluate(goldenDataset());

        assertThat(result.total()).isEqualTo(4);
        assertThat(result.passed()).isEqualTo(4);
        assertThat(result.passRate()).isEqualTo(1.0);
        assertThat(result.failedCaseIds()).isEmpty();
        assertThat(result.meetsGate(0.9)).isTrue();
    }

    @Test
    void degradedProvider_onlyPassesTheInStockCases() {
        stubCatalog();
        modelProvider.degrade(); // always claims "in stock"
        GoldenDatasetEvaluator evaluator = new GoldenDatasetEvaluator(modelProvider, productRepository);

        EvaluationResult result = evaluator.evaluate(goldenDataset());

        assertThat(result.total()).isEqualTo(4);
        assertThat(result.passed()).isEqualTo(2); // case-1 and case-3 are the in-stock cases
        assertThat(result.passRate()).isEqualTo(0.5);
        assertThat(result.failedCaseIds()).containsExactlyInAnyOrder("case-2", "case-4");
        assertThat(result.meetsGate(0.9)).isFalse();
    }

    @Test
    void repairedProvider_clearsTheGateAgain() {
        stubCatalog();
        modelProvider.degrade();
        GoldenDatasetEvaluator evaluator = new GoldenDatasetEvaluator(modelProvider, productRepository);

        assertThat(evaluator.evaluate(goldenDataset()).meetsGate(0.9)).isFalse();

        modelProvider.repair();

        EvaluationResult afterFix = evaluator.evaluate(goldenDataset());
        assertThat(afterFix.passRate()).isEqualTo(1.0);
        assertThat(afterFix.meetsGate(0.9)).isTrue();
    }
}
