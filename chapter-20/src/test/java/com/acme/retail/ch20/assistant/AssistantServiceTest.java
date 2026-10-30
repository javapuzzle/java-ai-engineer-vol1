package com.acme.retail.ch20.assistant;

import com.acme.retail.ch20.product.Product;
import com.acme.retail.ch20.product.ProductRepository;
import com.acme.retail.ch20.resilience.FeatureFlagService;
import com.acme.retail.ch20.resilience.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the core incident-response flow.
 *
 * <p>None of these tests start Spring or touch a real database --
 * {@link ProductRepository} is mocked, and the {@link CircuitBreakerRegistry}
 * and {@link AssistantMetrics} are built directly, exactly like a real
 * request would use them.
 */
@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    private static final String SKU = "SKU-1001";

    @Mock
    private ProductRepository productRepository;

    private SimulatedModelProvider modelProvider;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private FeatureFlagService featureFlags;
    private AssistantMetrics metrics;
    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        modelProvider = new SimulatedModelProvider();
        circuitBreakerRegistry = ResilienceConfig.defaultRegistry();
        featureFlags = new FeatureFlagService();
        metrics = new AssistantMetrics(new SimpleMeterRegistry());
        assistantService = new AssistantService(
                modelProvider, productRepository, circuitBreakerRegistry, featureFlags, metrics);
    }

    private void soldOutProductInDatabase() {
        Product product = new Product("Trail Running Shoe", "Grippy trail shoe",
                8999, SKU, 0, "Footwear");
        when(productRepository.findBySku(SKU)).thenReturn(Optional.of(product));
    }

    private void inStockProductInDatabase() {
        Product product = new Product("Trail Running Shoe", "Grippy trail shoe",
                8999, SKU, 42, "Footwear");
        when(productRepository.findBySku(SKU)).thenReturn(Optional.of(product));
    }

    @Test
    void healthyProviderAndInStockProduct_returnsSuccess() {
        inStockProductInDatabase();

        AssistantResponse response = assistantService.ask(
                new AssistantRequest("cust-1", SKU, "Is this in stock?"));

        assertThat(response.outcome()).isEqualTo("success");
        assertThat(response.grounded()).isTrue();
        assertThat(metrics.getCount("success")).isEqualTo(1.0);
    }

    @Test
    void degradedProvider_fallsBackOnMismatch_andEventuallyOpensCircuit() {
        soldOutProductInDatabase();
        modelProvider.degrade(); // reproduces the launch-day bug

        AssistantRequest request = new AssistantRequest("cust-1", SKU, "Is this in stock?");

        // Sliding window size is 4 with a 50% failure threshold, so the
        // 4th mismatched call should trip the breaker to OPEN.
        for (int i = 0; i < 4; i++) {
            AssistantResponse response = assistantService.ask(request);
            assertThat(response.outcome()).isIn("fallback_mismatch", "fallback_circuit_open");
            assertThat(response.grounded()).isTrue(); // fallback is always DB-grounded
        }

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(
                ResilienceConfig.ASSISTANT_PROVIDER_BREAKER);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(metrics.getCount("grounding_mismatch")).isGreaterThan(0.0);

        // While OPEN, further calls should short-circuit without ever
        // reaching the (still-broken) provider.
        AssistantResponse whileOpen = assistantService.ask(request);
        assertThat(whileOpen.outcome()).isEqualTo("fallback_circuit_open");
    }

    @Test
    void disabledFeatureFlag_shortCircuitsBeforeCallingProvider() {
        featureFlags.disable("manual kill switch during incident");

        AssistantResponse response = assistantService.ask(
                new AssistantRequest("cust-1", SKU, "Is this in stock?"));

        assertThat(response.outcome()).isEqualTo("feature_disabled");
        assertThat(metrics.getCount("feature_disabled")).isEqualTo(1.0);
        verifyNoInteractions(productRepository);
    }

    @Test
    void unknownSku_stillGetsASafeGroundedFallbackOnMismatch() {
        when(productRepository.findBySku("SKU-GHOST")).thenReturn(Optional.empty());
        modelProvider.degrade();

        AssistantResponse response = assistantService.ask(
                new AssistantRequest("cust-1", "SKU-GHOST", "Is this in stock?"));

        // ProductFact.unknown() reports inStock=false; the degraded
        // provider claims inStock=true -- that is still a mismatch.
        assertThat(response.outcome()).isEqualTo("fallback_mismatch");
    }

    @Test
    void repairedProvider_afterMismatches_returnsToSuccessOnceCircuitClosedAgain() {
        soldOutProductInDatabase();
        modelProvider.degrade();

        AssistantRequest request = new AssistantRequest("cust-1", SKU, "Is this in stock?");
        for (int i = 0; i < 4; i++) {
            assistantService.ask(request);
        }

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(
                ResilienceConfig.ASSISTANT_PROVIDER_BREAKER);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // The fix: repair the provider, then manually close the breaker --
        // this is exactly what IncidentResponseService.recover() automates
        // once the golden-dataset evaluation passes.
        modelProvider.repair();
        breaker.transitionToClosedState();

        AssistantResponse afterFix = assistantService.ask(request);
        assertThat(afterFix.outcome()).isEqualTo("success");
        assertThat(afterFix.answer()).contains("sold out");
    }
}
