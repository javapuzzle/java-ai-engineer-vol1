package com.acme.retail.ch11.inventory;

import java.util.List;

/**
 * Turns retrieved {@link InventoryFact} rows into plain text the model can
 * read inside a prompt.
 *
 * <p>This is deliberately its own small class, separate from anything that
 * calls a model. Building the grounding text is a pure function -- given
 * the same facts, it always produces the same string -- so it can be
 * unit-tested completely without touching Spring AI, an API key, or a
 * network call. Sarah keeps this boundary on purpose: the more of the RAG
 * pipeline that is plain, deterministic Java, the more of it she can test
 * without a live model (see {@code GroundingContextBuilderTest}).
 */
public final class GroundingContextBuilder {

    private GroundingContextBuilder() {
    }

    public static String toContext(List<InventoryFact> facts) {
        if (facts.isEmpty()) {
            return "No matching product was found in the Acme Retail catalog.";
        }

        StringBuilder context = new StringBuilder();
        for (InventoryFact fact : facts) {
            context.append("- ")
                    .append(fact.productName())
                    .append(" (SKU ")
                    .append(fact.sku())
                    .append("): ");

            if (fact.available()) {
                context.append(fact.stockQuantity()).append(" units in stock.");
            } else {
                context.append("out of stock (0 units available).");
            }

            context.append(System.lineSeparator());
        }

        return context.toString().stripTrailing();
    }
}
