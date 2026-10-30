package com.acme.retail.ch11.assistant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the grounded system prompt actually carries the retrieved
 * facts and the "don't guess" instruction.
 *
 * <p>Note what this test does <em>not</em> do: it never calls a real
 * model. Whether the model obeys the instruction is a separate concern --
 * evaluating model behavior is covered properly in Chapter 19 with mock AI
 * providers and golden datasets. What this test can prove today, cheaply
 * and deterministically, is that the correct facts are actually in the
 * prompt the model will see.
 */
class GroundedPromptFactoryTest {

    @Test
    void systemPromptContainsTheRetrievedFacts() {
        String groundingContext = "- Black Running Shoe (SKU RUN-BLK-42): out of stock (0 units available).";

        String systemPrompt = GroundedPromptFactory.systemPrompt(groundingContext);

        assertThat(systemPrompt).contains("Black Running Shoe");
        assertThat(systemPrompt).contains("out of stock");
    }

    @Test
    void systemPromptInstructsTheModelNotToGuess() {
        String systemPrompt = GroundedPromptFactory.systemPrompt("- Example Product: 5 units in stock.");

        assertThat(systemPrompt).contains("do not guess");
        assertThat(systemPrompt).contains("ONLY the inventory");
    }
}
