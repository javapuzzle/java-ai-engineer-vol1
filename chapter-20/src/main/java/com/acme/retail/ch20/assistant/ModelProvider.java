package com.acme.retail.ch20.assistant;

/**
 * The seam between Sarah's application and whichever AI model answers
 * shopping questions.
 *
 * <p>Kept as a small interface (as in Chapter 10 and Chapter 16) so tests
 * and the golden-dataset evaluator (Chapter 19) can swap in any
 * implementation without a real network call, and so the "bad provider
 * response" at the heart of this chapter's incident can be reproduced on
 * demand with {@link SimulatedModelProvider}.
 */
public interface ModelProvider {

    /**
     * Answers a customer's question about one product.
     *
     * @param question the customer's question
     * @param fact      the real inventory facts for the product being
     *                  asked about -- what the provider is grounded in
     * @return the model's answer, including whether it claims the product
     *         is in stock
     */
    AssistantAnswer ask(String question, ProductFact fact);
}
