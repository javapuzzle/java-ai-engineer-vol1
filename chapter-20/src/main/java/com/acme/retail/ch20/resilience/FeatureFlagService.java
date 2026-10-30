package com.acme.retail.ch20.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A manual kill switch for the AI shopping assistant feature.
 *
 * <p>This is the fastest fallback available during an incident: turn the
 * whole feature off before you even know the root cause. Chapter 16
 * introduced graceful degradation through the circuit breaker; this flag
 * is the coarser, human-operated version of the same idea -- Sarah or
 * Daniel can disable the assistant in one call, and {@code
 * IncidentResponseService} only re-enables it after the fix is verified.
 */
@Component
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public boolean isEnabled() {
        return enabled.get();
    }

    public void disable(String reason) {
        enabled.set(false);
        log.warn("event=feature_flag flag=ai_assistant outcome=disabled reason={}", reason);
    }

    public void enable() {
        enabled.set(true);
        log.info("event=feature_flag flag=ai_assistant outcome=enabled");
    }
}
