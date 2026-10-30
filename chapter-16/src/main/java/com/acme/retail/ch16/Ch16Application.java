package com.acme.retail.ch16;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 16 — "Production Doesn't Care About Your Demo".
 *
 * <p>This module reproduces the incident Sarah walks into: the AI provider
 * that has always answered in a few hundred milliseconds during every demo
 * suddenly gets slow, then stops answering at all. Requests pile up,
 * threads block, and the failure spreads to parts of the application that
 * have nothing to do with AI.
 *
 * <p>The fix is not "write better code" in the usual sense. It is wrapping
 * one already-working call with the resilience patterns every production
 * dependency needs: a timeout, a retry with backoff, a circuit breaker, a
 * bulkhead, and a fallback. See
 * {@link com.acme.retail.ch16.assistant.ShoppingAssistantService} and
 * {@code application.yml} for where each pattern is configured.
 */
@SpringBootApplication
public class Ch16Application {

    public static void main(String[] args) {
        SpringApplication.run(Ch16Application.class, args);
    }
}
