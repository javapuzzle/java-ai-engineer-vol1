package com.acme.retail.ch20.assistant;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A fake model provider that stands in for the real AI provider behind
 * Acme Retail's shopping assistant.
 *
 * <p>This class is the one place in this module that plays the role of
 * "the bad provider response" from the chapter's story. In its degraded
 * state it behaves exactly like the provider did on launch day: it always
 * answers that a product is in stock, no matter what the real inventory
 * says. It is confidently wrong, and its HTTP response would still be a
 * normal 200 -- there is no exception to catch, which is exactly the
 * Chapter 17 problem ("something is wrong, but nobody knows what") this
 * chapter builds on.
 *
 * <p>This module wires {@code SimulatedModelProvider} in as the only
 * {@link ModelProvider} bean rather than calling a real, billed AI
 * provider (the pattern earlier chapters, e.g. Chapter 10's {@code
 * ChatClient}, used for a live model). That keeps this capstone runnable
 * offline and deterministic -- what matters here is the incident-response
 * flow around the provider, not the provider's own intelligence.
 *
 * <p>{@link #repair()} represents Sarah and Daniel's actual fix -- in a
 * real incident this would be a provider configuration change, a prompt
 * fix, or a provider failover, not a boolean flag. The flag keeps the
 * lesson visible in code: the fix is a deliberate, named action, not a
 * mystery.
 */
@Component
public class SimulatedModelProvider implements ModelProvider {

    private final AtomicBoolean healthy = new AtomicBoolean(true);

    @Override
    public AssistantAnswer ask(String question, ProductFact fact) {
        if (healthy.get()) {
            boolean inStock = fact.inStock();
            String text = inStock
                    ? "Yes, " + fact.name() + " is in stock -- we have " + fact.stockQuantity() + " units."
                    : "I'm sorry, " + fact.name() + " is currently sold out.";
            return new AssistantAnswer(text, inStock, false, "acme-shopping-model-v2");
        }

        // The launch-day bug: the degraded provider ignores the real
        // inventory fact entirely and always claims the product is
        // available. This is what Sarah's investigation eventually finds.
        String text = "Yes, " + fact.name() + " is in stock and ready to ship today!";
        return new AssistantAnswer(text, true, false, "acme-shopping-model-v2");
    }

    /**
     * Simulates the launch-day provider bug: every answer claims the
     * product is in stock, regardless of the real fact it was given.
     */
    public void degrade() {
        healthy.set(false);
    }

    /**
     * Simulates deploying the fix: the provider goes back to grounding
     * its answer in the real fact it was given.
     */
    public void repair() {
        healthy.set(true);
    }

    public boolean isHealthy() {
        return healthy.get();
    }
}
