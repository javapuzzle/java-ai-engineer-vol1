package com.acme.retail.ch20.evaluation;

import com.acme.retail.ch20.assistant.AssistantAnswer;
import com.acme.retail.ch20.assistant.ModelProvider;
import com.acme.retail.ch20.assistant.ProductFact;
import com.acme.retail.ch20.product.Product;
import com.acme.retail.ch20.product.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Grades a model provider against a fixed golden dataset.
 *
 * <p>This deliberately calls {@link ModelProvider} directly -- it does
 * not go through {@code AssistantService}, so it is never affected by the
 * feature flag or the circuit breaker. Chapter 19's lesson was that
 * testing the AI's behavior is a different job from testing the
 * infrastructure around it: this evaluator only answers one question,
 * "does the model itself now give grounded answers?" -- which is exactly
 * what needs to be true before the feature flag should be trusted again.
 */
@Component
public class GoldenDatasetEvaluator {

    private final ModelProvider modelProvider;
    private final ProductRepository productRepository;

    public GoldenDatasetEvaluator(ModelProvider modelProvider, ProductRepository productRepository) {
        this.modelProvider = modelProvider;
        this.productRepository = productRepository;
    }

    public EvaluationResult evaluate(List<GoldenCase> cases) {
        int passed = 0;
        List<String> failedCaseIds = new ArrayList<>();

        for (GoldenCase testCase : cases) {
            ProductFact fact = resolveFact(testCase);
            AssistantAnswer answer = modelProvider.ask(testCase.question(), fact);

            if (answer.claimsInStock() == testCase.expectedInStock()) {
                passed++;
            } else {
                failedCaseIds.add(testCase.id());
            }
        }

        int total = cases.size();
        double passRate = total == 0 ? 0.0 : (double) passed / total;
        return new EvaluationResult(total, passed, passRate, failedCaseIds);
    }

    /**
     * Prefers the live database row when one exists for the case's SKU,
     * falling back to the case's own expected answer as the fact -- a
     * golden case can describe a scenario (e.g. a specific sold-out SKU)
     * that is not necessarily seeded in every environment the evaluator
     * runs in.
     */
    private ProductFact resolveFact(GoldenCase testCase) {
        Optional<Product> product = productRepository.findBySku(testCase.sku());
        return product
                .map(p -> new ProductFact(p.getSku(), p.getName(), p.isInStock(),
                        p.getStockQuantity() == null ? 0 : p.getStockQuantity()))
                .orElse(new ProductFact(testCase.sku(), testCase.sku(), testCase.expectedInStock(),
                        testCase.expectedInStock() ? 1 : 0));
    }
}
