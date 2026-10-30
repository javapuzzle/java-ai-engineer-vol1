package com.acme.retail.ch20.assistant;

import com.acme.retail.ch20.product.Product;
import com.acme.retail.ch20.product.ProductRepository;
import com.acme.retail.ch20.resilience.FeatureFlagService;
import com.acme.retail.ch20.resilience.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Answers a customer's question about one product, safely.
 *
 * <p>This is where every earlier chapter's lesson meets in one place:
 *
 * <ul>
 *   <li>the model is grounded in real inventory data before it answers
 *       (Chapter 11),</li>
 *   <li>a circuit breaker with a fallback protects the call, and now also
 *       trips on answers that are wrong without throwing an exception
 *       (Chapter 16, extended for Chapter 17's "quiet failure" problem),</li>
 *   <li>every outcome is logged with a correlation ID and counted as a
 *       metric (Chapter 17),</li>
 *   <li>the customer ID is never written to the log line in the clear
 *       (Chapter 18),</li>
 *   <li>and the fallback answer is always built from the real database
 *       row, never invented.</li>
 * </ul>
 *
 * <p>{@code AssistantService} never trusts the model's claim about stock
 * on its own. It always compares the claim to the real {@link
 * ProductFact} first, and only forwards the model's words to the customer
 * when they agree.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final ModelProvider modelProvider;
    private final ProductRepository productRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final FeatureFlagService featureFlags;
    private final AssistantMetrics metrics;

    public AssistantService(ModelProvider modelProvider,
                             ProductRepository productRepository,
                             CircuitBreakerRegistry circuitBreakerRegistry,
                             FeatureFlagService featureFlags,
                             AssistantMetrics metrics) {
        this.modelProvider = modelProvider;
        this.productRepository = productRepository;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.featureFlags = featureFlags;
        this.metrics = metrics;
    }

    public AssistantResponse ask(AssistantRequest request) {
        String correlationId = currentCorrelationId();

        if (!featureFlags.isEnabled()) {
            log.warn("event=assistant_call outcome=feature_disabled sku={} correlationId={}",
                    request.sku(), correlationId);
            metrics.increment("feature_disabled");
            return AssistantResponse.disabled(correlationId);
        }

        ProductFact fact = loadFact(request.sku());
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(
                ResilienceConfig.ASSISTANT_PROVIDER_BREAKER);

        try {
            AssistantAnswer answer = breaker.executeSupplier(
                    () -> callAndCheck(request.question(), fact));

            if (answer.mismatched()) {
                log.error("event=assistant_call outcome=grounding_mismatch sku={} "
                                + "providerClaimsInStock={} actualInStock={} provider={} correlationId={}",
                        fact.sku(), answer.claimsInStock(), fact.inStock(), answer.providerId(), correlationId);
                metrics.increment("grounding_mismatch");
                return AssistantResponse.fallback(correlationId, fact, "fallback_mismatch");
            }

            log.info("event=assistant_call outcome=success sku={} grounded=true correlationId={}",
                    fact.sku(), correlationId);
            metrics.increment("success");
            return AssistantResponse.success(correlationId, answer.text());

        } catch (CallNotPermittedException circuitOpen) {
            log.warn("event=assistant_call outcome=circuit_open sku={} correlationId={}",
                    fact.sku(), correlationId);
            metrics.increment("circuit_open");
            return AssistantResponse.fallback(correlationId, fact, "fallback_circuit_open");

        } catch (Exception providerError) {
            log.error("event=assistant_call outcome=provider_error sku={} error={} correlationId={}",
                    fact.sku(), providerError.getMessage(), correlationId);
            metrics.increment("provider_error");
            return AssistantResponse.fallback(correlationId, fact, "fallback_provider_error");
        }
    }

    /**
     * Calls the model provider and immediately attaches whether its
     * answer contradicts the real fact -- before the circuit breaker's
     * {@code recordResultPredicate} ever sees the result.
     */
    private AssistantAnswer callAndCheck(String question, ProductFact fact) {
        AssistantAnswer raw = modelProvider.ask(question, fact);
        boolean mismatched = raw.claimsInStock() != fact.inStock();
        return raw.withMismatch(mismatched);
    }

    private ProductFact loadFact(String sku) {
        Optional<Product> product = productRepository.findBySku(sku);
        return product
                .map(p -> new ProductFact(p.getSku(), p.getName(), p.isInStock(),
                        p.getStockQuantity() == null ? 0 : p.getStockQuantity()))
                .orElse(ProductFact.unknown(sku));
    }

    private String currentCorrelationId() {
        String fromContext = MDC.get(com.acme.retail.ch20.observability.CorrelationIdFilter.MDC_KEY);
        return fromContext != null ? fromContext : UUID.randomUUID().toString();
    }
}
