package com.acme.retail.ch20.incident;

import com.acme.retail.ch20.assistant.AssistantMetrics;
import com.acme.retail.ch20.evaluation.EvaluationResult;
import com.acme.retail.ch20.evaluation.GoldenCase;
import com.acme.retail.ch20.evaluation.GoldenDatasetEvaluator;
import com.acme.retail.ch20.resilience.FeatureFlagService;
import com.acme.retail.ch20.resilience.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sarah's launch-day playbook, expressed as code.
 *
 * <p>This class does not introduce a single new idea -- every method here
 * is a composition of Chapter 16 (circuit breaker), Chapter 17 (metrics as
 * evidence), and Chapter 19 (golden-dataset evaluation as a release gate).
 * What is new is the order they run in, and the fact that step 3 never
 * runs before step 2 finishes: Sarah does not re-enable the feature
 * because the fix "looks right." She re-enables it because the evaluation
 * says so.
 */
@Service
public class IncidentResponseService {

    private static final Logger log = LoggerFactory.getLogger(IncidentResponseService.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final FeatureFlagService featureFlags;
    private final AssistantMetrics metrics;
    private final GoldenDatasetEvaluator evaluator;

    public IncidentResponseService(CircuitBreakerRegistry circuitBreakerRegistry,
                                    FeatureFlagService featureFlags,
                                    AssistantMetrics metrics,
                                    GoldenDatasetEvaluator evaluator) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.featureFlags = featureFlags;
        this.metrics = metrics;
        this.evaluator = evaluator;
    }

    /**
     * Step 1: gather the evidence. Reads the same circuit breaker state
     * and metric counters an on-call dashboard would show -- no guessing.
     */
    public IncidentReport diagnose() {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(
                ResilienceConfig.ASSISTANT_PROVIDER_BREAKER);
        CircuitBreaker.State state = breaker.getState();

        double mismatchCount = metrics.getCount("grounding_mismatch");
        double circuitOpenCount = metrics.getCount("circuit_open");
        double fallbackCount = mismatchCount + circuitOpenCount + metrics.getCount("provider_error");

        String summary = mismatchCount > 0
                ? "Circuit breaker is " + state + " after " + (int) mismatchCount
                        + " grounding mismatch(es) from the model provider -- the provider is "
                        + "returning answers that contradict live inventory."
                : "Circuit breaker is " + state + ". No grounding mismatches recorded.";

        log.info("event=incident_diagnose circuitState={} mismatchCount={} circuitOpenCount={}",
                state, mismatchCount, circuitOpenCount);

        return new IncidentReport(state, mismatchCount, circuitOpenCount, fallbackCount, summary);
    }

    /**
     * Step 2: verify the fix against the golden dataset before trusting
     * it. This is the gate the brief calls "verify with evaluation" --
     * it runs against the live model provider, independent of the feature
     * flag or the circuit breaker's current state.
     */
    public EvaluationResult verifyFix(List<GoldenCase> goldenDataset) {
        return evaluator.evaluate(goldenDataset);
    }

    /**
     * Step 3: only if the evaluation clears the gate, close the circuit
     * breaker and re-enable the feature flag. Otherwise, leave the
     * assistant disabled and say why.
     *
     * @return true if the assistant was recovered and re-enabled
     */
    public boolean recover(List<GoldenCase> goldenDataset, double passRateGate) {
        EvaluationResult result = verifyFix(goldenDataset);

        if (result.meetsGate(passRateGate)) {
            circuitBreakerRegistry.circuitBreaker(ResilienceConfig.ASSISTANT_PROVIDER_BREAKER)
                    .transitionToClosedState();
            featureFlags.enable();
            log.info("event=incident_recovery outcome=recovered passRate={} passed={} total={}",
                    result.passRate(), result.passed(), result.total());
            return true;
        }

        log.warn("event=incident_recovery outcome=blocked passRate={} passed={} total={} failedCases={}",
                result.passRate(), result.passed(), result.total(), result.failedCaseIds());
        return false;
    }
}
