package com.acme.retail.ch20.assistant;

import com.acme.retail.ch20.resilience.FeatureFlagService;
import com.acme.retail.ch20.resilience.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public entry point to the shopping assistant.
 *
 * <p>{@code /status} is what Sarah actually looks at first during the
 * incident in this chapter: it shows the feature flag state and the
 * circuit breaker state without needing to read a single log line.
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final FeatureFlagService featureFlags;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public AssistantController(AssistantService assistantService,
                                FeatureFlagService featureFlags,
                                CircuitBreakerRegistry circuitBreakerRegistry) {
        this.assistantService = assistantService;
        this.featureFlags = featureFlags;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostMapping("/ask")
    public AssistantResponse ask(@RequestBody AssistantRequest request) {
        return assistantService.ask(request);
    }

    @GetMapping("/status")
    public AssistantStatus status() {
        String state = circuitBreakerRegistry
                .circuitBreaker(ResilienceConfig.ASSISTANT_PROVIDER_BREAKER)
                .getState()
                .name();
        return new AssistantStatus(featureFlags.isEnabled(), state);
    }

    public record AssistantStatus(boolean featureEnabled, String circuitBreakerState) {
    }
}
