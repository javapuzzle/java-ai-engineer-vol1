package com.acme.retail.ch20.incident;

import com.acme.retail.ch20.assistant.AssistantMetrics;
import com.acme.retail.ch20.assistant.AssistantRequest;
import com.acme.retail.ch20.assistant.AssistantResponse;
import com.acme.retail.ch20.assistant.AssistantService;
import com.acme.retail.ch20.assistant.SimulatedModelProvider;
import com.acme.retail.ch20.evaluation.EvaluationResult;
import com.acme.retail.ch20.evaluation.GoldenCase;
import com.acme.retail.ch20.evaluation.GoldenDatasetEvaluator;
import com.acme.retail.ch20.product.Product;
import com.acme.retail.ch20.product.ProductRepository;
import com.acme.retail.ch20.resilience.FeatureFlagService;
import com.acme.retail.ch20.resilience.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The launch-day incident, end to end, in one test.
 *
 * <p>This test is the executable version of the chapter's story:
 *
 * <ol>
 *   <li>the assistant goes live and immediately starts giving wrong
 *       availability answers for a sold-out product,</li>
 *   <li>the circuit breaker trips and the fallback engages,</li>
 *   <li>{@code diagnose()} reports what happened using nothing but
 *       metrics and circuit breaker state,</li>
 *   <li>the provider is repaired,</li>
 *   <li>{@code recover()} refuses to re-enable the feature until the
 *       golden-dataset evaluation passes, then does.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class IncidentResponseServiceTest {

    private static final String SOLD_OUT_SKU = "SKU-1001";

    @Mock
    private ProductRepository productRepository;

    @Test
    void fullIncidentLifecycle_fromBadResponseToVerifiedRecovery() {
        Product soldOutShoe = new Product("Trail Running Shoe", "Grippy trail shoe",
                8999, SOLD_OUT_SKU, 0, "Footwear");
        when(productRepository.findBySku(SOLD_OUT_SKU)).thenReturn(Optional.of(soldOutShoe));

        SimulatedModelProvider modelProvider = new SimulatedModelProvider();
        CircuitBreakerRegistry circuitBreakerRegistry = ResilienceConfig.defaultRegistry();
        FeatureFlagService featureFlags = new FeatureFlagService();
        AssistantMetrics metrics = new AssistantMetrics(new SimpleMeterRegistry());

        AssistantService assistantService = new AssistantService(
                modelProvider, productRepository, circuitBreakerRegistry, featureFlags, metrics);
        GoldenDatasetEvaluator evaluator = new GoldenDatasetEvaluator(modelProvider, productRepository);
        IncidentResponseService incidentResponseService = new IncidentResponseService(
                circuitBreakerRegistry, featureFlags, metrics, evaluator);

        // --- Launch day: the provider bug ships. ---
        modelProvider.degrade();
        AssistantRequest question = new AssistantRequest(
                "cust-42", SOLD_OUT_SKU, "Is the trail running shoe in stock?");

        for (int i = 0; i < 4; i++) {
            AssistantResponse response = assistantService.ask(question);
            // The customer never sees the wrong "in stock" claim -- the
            // fallback is always grounded in the real, sold-out row.
            assertThat(response.answer()).contains("sold out");
        }

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(
                ResilienceConfig.ASSISTANT_PROVIDER_BREAKER);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // --- Sarah investigates: diagnose() before guessing at a fix. ---
        IncidentReport report = incidentResponseService.diagnose();
        assertThat(report.circuitState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(report.mismatchCount()).isGreaterThan(0.0);
        assertThat(report.summary()).contains("grounding mismatch");

        // --- Someone (rightly) disables the feature while this is live. ---
        featureFlags.disable("grounding mismatches confirmed by diagnose()");
        assertThat(assistantService.ask(question).outcome()).isEqualTo("feature_disabled");

        // --- The fix is deployed. ---
        modelProvider.repair();

        List<GoldenCase> goldenDataset = List.of(
                new GoldenCase("gc-1", SOLD_OUT_SKU, "Is the trail running shoe in stock?", false));

        // --- Verify before trusting: recover() must run the evaluation. ---
        boolean recovered = incidentResponseService.recover(goldenDataset, 0.9);

        assertThat(recovered).isTrue();
        assertThat(featureFlags.isEnabled()).isTrue();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // --- Confirm: the same question now gets a correct, grounded answer. ---
        AssistantResponse afterRecovery = assistantService.ask(question);
        assertThat(afterRecovery.outcome()).isEqualTo("success");
        assertThat(afterRecovery.answer()).contains("sold out");
    }

    @Test
    void recover_refusesToReEnable_whenEvaluationStillFails() {
        SimulatedModelProvider modelProvider = new SimulatedModelProvider();
        modelProvider.degrade(); // fix was never actually deployed

        CircuitBreakerRegistry circuitBreakerRegistry = ResilienceConfig.defaultRegistry();
        FeatureFlagService featureFlags = new FeatureFlagService();
        featureFlags.disable("incident in progress");
        AssistantMetrics metrics = new AssistantMetrics(new SimpleMeterRegistry());
        GoldenDatasetEvaluator evaluator = new GoldenDatasetEvaluator(modelProvider, productRepository);
        IncidentResponseService incidentResponseService = new IncidentResponseService(
                circuitBreakerRegistry, featureFlags, metrics, evaluator);

        when(productRepository.findBySku(SOLD_OUT_SKU))
                .thenReturn(Optional.of(new Product("Trail Running Shoe", "desc",
                        8999, SOLD_OUT_SKU, 0, "Footwear")));

        List<GoldenCase> goldenDataset = List.of(
                new GoldenCase("gc-1", SOLD_OUT_SKU, "Is the trail running shoe in stock?", false));

        EvaluationResult expected = evaluator.evaluate(goldenDataset);
        assertThat(expected.passRate()).isLessThan(0.9);

        boolean recovered = incidentResponseService.recover(goldenDataset, 0.9);

        assertThat(recovered).isFalse();
        assertThat(featureFlags.isEnabled()).isFalse();
    }
}
